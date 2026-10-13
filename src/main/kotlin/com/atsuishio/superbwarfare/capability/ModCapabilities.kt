package com.atsuishio.superbwarfare.capability

import com.atsuishio.superbwarfare.capability.ModCapabilities.register
import com.atsuishio.superbwarfare.capability.ModCapabilities.registered
import com.atsuishio.superbwarfare.capability.entity.InfiniteAmmoCapability
import com.atsuishio.superbwarfare.capability.living.PhosphorusFireCapability
import com.atsuishio.superbwarfare.capability.living.RadiationCapability
import com.atsuishio.superbwarfare.capability.player.PlayerDeployState
import com.atsuishio.superbwarfare.capability.player.PlayerVariable
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken

/**
 * 本 mod 挂在实体上的 Capability 一览。
 *
 * 数据类只描述"有哪些字段"，落盘与同步由 [CapabilityWrapper] 统一实现，挂载与同步登记由
 * [ModCapability] 自动完成。新增一个 capability 只要：写数据类（里面带一个空的包装子类），
 * 然后在下面加一行 [register]。
 */
object ModCapabilities {

    /** 已登记的 capability，[register] 会往里追加 */
    private val registered = ArrayList<ModCapability<*, *>>()

    /**
     * 登记一个 capability。
     *
     * `T` / `H` 都用 reified 拿到 `KClass`，于是「默认值」和「每个实体的包装实例」都能直接
     * 交给无参构造函数（和 `ModItems` 一样的写法），调用点不需要再写任何 supplier。
     *
     * `token` 必须由调用点给出：Forge 的 `CapabilityManager` 以**实现类的全限定名**为键，
     * 而那个名字是编译期写进 `CapabilityToken` 子类签名里的，没法用 `T::class` 在运行时推出来。
     */
    private inline fun <reified T : Any, reified H : CapabilityWrapper<T>> register(
        id: ResourceLocation,
        target: AttachTarget,
        token: CapabilityToken<H>,
    ): ModCapability<T, H> {
        val handle = ModCapability(id, target, T::class, H::class, CapabilityManager.get(token))

        registered += handle
        return handle
    }

    /** 无限弹药：所有实体（含假人），创造模式弹药盒 / 命令都会改它 */
    @JvmField
    val INFINITE_AMMO = register<InfiniteAmmoCapability, InfiniteAmmoCapability.Holder>(
        id = InfiniteAmmoCapability.ID,
        target = AttachTarget.ENTITY,
        token = object : CapabilityToken<InfiniteAmmoCapability.Holder>() {},
    )

    /** 白磷燃烧状态：只对生物有意义 */
    @JvmField
    val PHOSPHORUS_FIRE = register<PhosphorusFireCapability, PhosphorusFireCapability.Holder>(
        id = PhosphorusFireCapability.ID,
        target = AttachTarget.LIVING,
        token = object : CapabilityToken<PhosphorusFireCapability.Holder>() {},
    )

    /** 辐射剂量：只对生物有意义 */
    @JvmField
    val RADIATION = register<RadiationCapability, RadiationCapability.Holder>(
        id = RadiationCapability.ID,
        target = AttachTarget.LIVING,
        token = object : CapabilityToken<RadiationCapability.Holder>() {},
    )

    /** 玩家弹药 / 热成像：只对本人有意义，同步只发给持有者自己（见 `PlayerVariable.Holder`） */
    @JvmField
    val PLAYER_VARIABLE = register<PlayerVariable, PlayerVariable.Holder>(
        id = PlayerVariable.ID,
        target = AttachTarget.PLAYER,
        token = object : CapabilityToken<PlayerVariable.Holder>() {},
    )

    /** 重武器架设状态：不落盘，只存在于服务端内存并随同步下发 */
    @JvmField
    val PLAYER_DEPLOY_STATE = register<PlayerDeployState, PlayerDeployState.Holder>(
        id = PlayerDeployState.ID,
        target = AttachTarget.PLAYER,
        token = object : CapabilityToken<PlayerDeployState.Holder>() {},
    )

    /**
     * 挂载与同步登记都按这个列表自动进行。
     *
     * 它和 [registered] 是同一个列表对象，所以上面每加一行 [register] 都会自动出现，
     * 不需要（也不应该）手动维护。
     */
    @JvmField
    val ALL: List<ModCapability<*, *>> = registered
}
