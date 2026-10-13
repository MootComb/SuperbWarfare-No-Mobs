package com.atsuishio.superbwarfare.capability.player

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.capability.CapabilityWrapper
import com.atsuishio.superbwarfare.capability.ModCapabilities
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedBlockPos
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedUUID
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraftforge.common.capabilities.AutoRegisterCapability

/**
 * 重武器架设状态：架在三脚架上的位置，以及被架的是哪把枪（按枪自己的 uuid 认）。
 *
 * 数据完全由世界状态派生（三脚架还在不在、手里还是不是那把枪），重登后一定能重新推导出来，
 * 因此 [ModCapabilities.PLAYER_DEPLOY_STATE] 被声明为不落盘：下线 / 死亡即解除。
 *
 * 只描述数据；网络同步由 [CapabilityWrapper] 代劳。
 */
@Serializable
data class PlayerDeployState(
    val tripodPos: SerializedBlockPos? = null,
    val gunUuid: SerializedUUID? = null,
) {
    companion object {
        val ID: ResourceLocation = loc("player_deploy_state")

        @JvmStatic
        fun getOrDefault(entity: Entity): PlayerDeployState = ModCapabilities.PLAYER_DEPLOY_STATE.get(entity)

        /** 变换并写回；结果与原来相等时不会发包 */
        @JvmStatic
        fun modify(entity: Entity, mapper: (PlayerDeployState) -> PlayerDeployState) {
            ModCapabilities.PLAYER_DEPLOY_STATE.update(entity, mapper)
        }
    }

    /** 只为了让 Forge 有一个唯一的 capability 实现类而存在，见 [CapabilityWrapper]。 */
    @AutoRegisterCapability
    class Holder : CapabilityWrapper<PlayerDeployState>(
        serializer(),
        PlayerDeployState(),
        persistent = false,
    )
}
