package com.atsuishio.superbwarfare.network.message.receive

import com.atsuishio.superbwarfare.config.SERVER_CONFIG
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.ClientPacketPayload
import com.atsuishio.superbwarfare.network.PayloadContext
import kotlinx.serialization.Serializable

@RegisterPacket
@Serializable
data class RealisticShootModeSyncMessage(val enabled: Boolean) : ClientPacketPayload() {
    override fun PayloadContext.handler() {
        // 单人客户端与内置服务端共享配置对象，无需用排队的旧包覆盖服务端的新值。
        if (get().networkManager.isMemoryConnection || !SERVER_CONFIG.isLoaded) return

        // 只更新 Forge 已加载的服务器配置副本，不保存到客户端磁盘。
        // 断开连接时由 Forge 卸载，下一次连接会重新接收目标服务器的配置。
        MiscConfig.REALISTIC_SHOOT_MODE.set(enabled)
    }
}
