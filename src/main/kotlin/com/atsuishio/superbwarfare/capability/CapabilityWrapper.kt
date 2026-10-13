package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.capability.sync.SyncTarget
import com.atsuishio.superbwarfare.capability.sync.SyncedCapability
import com.atsuishio.superbwarfare.serialization.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import net.minecraft.nbt.CompoundTag
import net.minecraftforge.common.util.INBTSerializable

/**
 * 把一个数据类包装成 Forge Capability 的载荷。
 *
 * 数据类只需要用 `@Serializable` 描述"有哪些字段"，落盘与同步全部由这里统一完成：
 *
 * - **落盘**：直接调用 [encodeToCompoundTag] / [decodeFromCompoundTag]，不再需要手写
 *   `serializeNBT` / `deserializeNBT`；
 * - **同步**：同一份序列化器写进 [ByteBufEncoder] / [ByteBufDecoder]，不再需要手写
 *   `writeSync` / `readSync`；
 * - **向前兼容**：反序列化按数据类声明的字段取值，NBT 里多出来的键天然被忽略；
 *   写回时以"上次读到的那个 tag"为底再合并当前值，那些键因此不会在存档时被抹掉；
 * - **向后兼容**：解析失败时保留既有值并打日志，而不是让整张存档加载失败。
 *
 * 数据类的每个字段都会写出去，因此全量与增量没有区别：这里的 [writeSync] / [readSync]
 * 忽略 `full`，收包方拿到的永远是一份完整快照。
 *
 * Forge 要求每个 Capability 对应一个**独立的实现类**（`CapabilityManager` 以类的内部名为键，
 * 见 `CapabilityTokenSubclass`），所以每个数据类里面仍然要写一个空的 `Holder` 子类，
 * 写法见 `RadiationCapability.Holder`。这是本方案唯一无法消除的样板，且与字段增删无关。
 *
 * @param T 数据类
 */
open class CapabilityWrapper<T : Any>(
    private val serializer: KSerializer<T>,
    initial: T,
    override val syncTarget: SyncTarget = SyncTarget.TRACKING,
    /** `false` 表示这份数据只存在于内存并随同步下发，不写进存档（重登即回到默认值） */
    private val persistent: Boolean = true,
) : INBTSerializable<CompoundTag>, SyncedCapability {

    var value: T = initial
        private set

    /**
     * 最近一次从存档读到的 tag。写回时以它为底，这样"当前版本不认识的键"
     * ——别的 mod、或者更新过的版本写下的——不会在保存时被抹掉。
     */
    private var retained: CompoundTag = CompoundTag()

    fun set(newValue: T) {
        value = newValue
    }

    override fun serializeNBT(): CompoundTag {
        if (!persistent) return CompoundTag()

        return retained.copy().merge(encodeToCompoundTag(serializer, value, serializersModule))
    }

    override fun deserializeNBT(nbt: CompoundTag) {
        if (!persistent) return

        retained = nbt.copy()

        value = try {
            decodeFromCompoundTag(serializer, nbt, serializersModule)
        } catch (e: Exception) {
            @OptIn(ExperimentalSerializationApi::class)
            val name = serializer.descriptor.serialName

            Mod.LOGGER.warn("Failed to read capability $name, keeping the current value", e)
            value
        }
    }

    override fun writeSync(encoder: ByteBufEncoder, full: Boolean) {
        encoder.encodeSerializableValue(serializer, value)
    }

    override fun readSync(decoder: ByteBufDecoder, full: Boolean) {
        value = decoder.decodeSerializableValue(serializer)
    }
}
