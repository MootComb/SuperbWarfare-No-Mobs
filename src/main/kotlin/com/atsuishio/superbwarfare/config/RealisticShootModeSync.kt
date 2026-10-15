package com.atsuishio.superbwarfare.config

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.network.message.receive.RealisticShootModeSyncMessage
import com.atsuishio.superbwarfare.tools.sendPacketTo
import com.atsuishio.superbwarfare.tools.sendPacketToAll
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.event.config.ModConfigEvent
import net.minecraftforge.server.ServerLifecycleHooks

/** Forge 的 SERVER 配置只在登录握手时同步；运行期变更需要通知已经在线的玩家。 */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID)
object RealisticShootModeSync {

    fun broadcast() {
        if (ServerLifecycleHooks.getCurrentServer() == null || !SERVER_CONFIG.isLoaded) return
        sendPacketToAll(RealisticShootModeSyncMessage(MiscConfig.REALISTIC_SHOOT_MODE.get()))
    }

    @SubscribeEvent
    fun onPlayerLoggedIn(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        if (!SERVER_CONFIG.isLoaded) return
        // 握手后再发送当前值，覆盖握手期间管理员切换配置的情况。
        sendPacketTo(player, RealisticShootModeSyncMessage(MiscConfig.REALISTIC_SHOOT_MODE.get()))
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(
        modid = Mod.MODID,
        bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD
    )
    object ConfigEvents {
        @SubscribeEvent
        fun onReload(event: ModConfigEvent.Reloading) {
            if (event.config.getSpec<ForgeConfigSpec>() !== SERVER_CONFIG) return
            val server = ServerLifecycleHooks.getCurrentServer() ?: return
            // 文件监视器可能从其它线程触发重载，发包与取值都交回服务端线程。
            server.execute { broadcast() }
        }
    }
}
