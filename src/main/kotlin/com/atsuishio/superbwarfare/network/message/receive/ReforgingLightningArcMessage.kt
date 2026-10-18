package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.entity.projectile.ReforgingLightningBallEntity
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.tools.localPlayer
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@RegisterPacket
@Serializable
data class ReforgingLightningArcMessage(
    val entityId: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val seed: Long,
) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        val ball = localPlayer?.level()?.getEntity(entityId) as? ReforgingLightningBallEntity ?: return
        ball.addLightningArc(Vec3(x, y, z), seed)
    }
}
