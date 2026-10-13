package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.capability.sync.CapabilitySync
import com.atsuishio.superbwarfare.capability.sync.SyncedCapability
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilitySerializable
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.common.util.LazyOptional
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/** [ModCapability] 要挂到哪些实体上，对应旧代码里 `AttachCapabilitiesEvent` 中的那几层 `if`。 */
enum class AttachTarget {
    /** 所有实体 */
    ENTITY,

    /** 所有生物 */
    LIVING,

    /** 真实玩家（不含假人） */
    PLAYER;

    fun accepts(entity: Entity): Boolean = when (this) {
        ENTITY -> true
        LIVING -> entity is LivingEntity
        PLAYER -> entity is Player && entity !is FakePlayer
    }
}

/**
 * 一个「数据类 Capability」的句柄：把注册、挂载、读写、同步登记收在一处。
 *
 * 实例的创建全部走 `KClass`（和 `ModItems` 一样）：默认值由数据类的无参构造函数给出，
 * 每个实体的包装实例也由包装类的无参构造函数给出，不需要再手写 supplier。
 *
 * 新增一个 capability 只要两步，其余全部自动完成（挂载、存档读写、同步登记、发包时机）：
 *
 * 1. 写一个 `@Serializable data class`，在里面放一个空的 [CapabilityWrapper] 子类
 *    （Forge 要求 capability 的实现类唯一，原因见 [CapabilityWrapper]）；
 * 2. 在 [ModCapabilities] 里加一行 `register`。
 *
 * @param T 数据类
 * @param H 该数据类对应的 [CapabilityWrapper] 子类，Forge 拿它当 capability 的唯一键
 */
class ModCapability<T : Any, H : CapabilityWrapper<T>>(
    /** `AttachCapabilitiesEvent` 与 [CapabilitySync] 使用的 id */
    @JvmField val id: ResourceLocation,
    /** 该 capability 挂到哪些实体上 */
    @JvmField val target: AttachTarget,
    private val type: KClass<T>,
    private val holderType: KClass<H>,
    private val capability: Capability<H>,
) {
    private fun holder(entity: Entity): H? = entity.getCapability(capability).resolve().orElse(null)

    /** 读取实体上的数据；没有挂载时返回数据类的默认值（与 1.21 的 `getData` 语义一致） */
    fun get(entity: Entity?): T = entity?.let { holder(it)?.value } ?: type.newInstance()

    /** 读取实体上的数据；没有挂载时返回 `null` */
    fun getOrNull(entity: Entity?): T? = entity?.let { holder(it)?.value }

    /** 写入实体上的数据；值没有变化时不会发包，没有挂载时什么也不做 */
    fun set(entity: Entity?, value: T) {
        val holder = entity?.let { holder(it) } ?: return
        if (holder.value == value) return

        holder.set(value)
        markDirty(entity)
    }

    /** 读出、变换、写回；结果与原来相等时不会发包 */
    fun update(entity: Entity?, block: (T) -> T) {
        set(entity, block(get(entity)))
    }

    /**
     * 手动标记同步。
     *
     * 数据在服务端被**就地修改**（[CapabilityWrapper.value] 是可变的）时用它，
     * 例如 `PlayerVariable` 的 `modify`。
     */
    fun markDirty(entity: Entity?) {
        CapabilitySync.markDirty(entity, id)
    }

    /** [CapabilitySync] 的取值器：从实体上取已挂载的包装实例 */
    fun syncedHolder(entity: Entity): SyncedCapability? = holder(entity)

    fun createProvider(): ICapabilitySerializable<CompoundTag> =
        CapabilityProvider(capability, LazyOptional.of { holderType.newInstance() })
}

/**
 * 用无参构造函数创建实例，失败时给出明确信息 —— 与 `ModItems.getInstanceOrCreate` 一致。
 *
 * 数据类只要所有字段都有默认值，Kotlin 就会额外生成一个无参构造函数；
 * [CapabilityWrapper] 子类则天然带有无参构造函数。
 */
private fun <X : Any> KClass<X>.newInstance(): X = try {
    createInstance()
} catch (e: Exception) {
    throw IllegalStateException(
        "Cannot create an instance of $qualifiedName for capability registration: " +
                "the class must have a public no-arg constructor.",
        e
    )
}

/** 把 [CapabilityWrapper] 适配成 Forge 的 `ICapabilityProvider` + 存档读写入口。 */
private class CapabilityProvider<T : Any, H : CapabilityWrapper<T>>(
    private val capability: Capability<H>,
    private val instance: LazyOptional<H>,
) : ICapabilitySerializable<CompoundTag> {

    override fun <C> getCapability(cap: Capability<C>, side: Direction?) =
        capability.orEmpty(cap, instance.cast())

    override fun serializeNBT(): CompoundTag =
        instance.orElseThrow { NullPointerException() }.serializeNBT()

    override fun deserializeNBT(nbt: CompoundTag) {
        instance.orElseThrow { NullPointerException() }.deserializeNBT(nbt)
    }
}
