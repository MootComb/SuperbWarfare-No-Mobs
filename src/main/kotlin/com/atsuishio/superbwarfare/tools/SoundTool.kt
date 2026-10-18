package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.network.message.receive.SoundClientMessage
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.*

fun Player?.playLocalSound(sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
    SoundTool.playLocalSound(this, sound, volume, pitch)
}

fun ServerPlayer.playLocalSound(
    sound: SoundEvent,
    source: SoundSource = SoundSource.PLAYERS,
    volume: Float = 1f,
    pitch: Float = 1f
) {
    SoundTool.playLocalSound(this, sound, source, volume, pitch)
}

fun ServerPlayer.stopSound(sound: ResourceLocation?, source: SoundSource? = SoundSource.PLAYERS) {
    SoundTool.stopSound(this, sound, source)
}

object SoundTool {
    @JvmStatic
    @JvmOverloads
    fun playLocalSound(player: Player?, sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
        if (player is ServerPlayer) {
            SoundTool.playLocalSound(player, sound, volume, pitch)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun playLocalSound(player: ServerPlayer, sound: SoundEvent, volume: Float = 1f, pitch: Float = 1f) {
        playLocalSound(player, sound, SoundSource.PLAYERS, volume, pitch)
    }

    @JvmStatic
    fun playLocalSound(player: ServerPlayer, sound: SoundEvent, source: SoundSource, volume: Float, pitch: Float) {
        sendPacketTo(
            player, ClientboundSoundPacket(
                Holder.Direct(sound),
                source, player.x, player.y, player.z, volume, pitch, player.level().random.nextLong()
            )
        )
    }

    @JvmStatic
    @JvmOverloads
    fun stopSound(player: ServerPlayer, sound: ResourceLocation?, source: SoundSource? = SoundSource.PLAYERS) {
        sendPacketTo(player, ClientboundStopSoundPacket(sound, source))
    }

    /**
     * 播放「远程延迟音效」：只发给 [radius] × 16 格内的玩家，其余全部交给客户端按距离延迟播放
     * （约 17 格/tick，即音速），用于远处的枪声、爆炸声。
     *
     * [radius] 既是音量也是收听半径的倍率，和原版 `Level.playSound` 的口径一致；
     * 客户端侧的处理见 [SoundClientMessage]。
     *
     * @param sender 发射者 UUID，客户端据此区分「自己打的」和「别人打的」
     * @param exclude 这个玩家**完全收不到**这条音效。例如射手本人不该听到自己的第三人称枪声
     *        （他只该听客户端本地播的第一人称音效），这里直接不给他发包
     */
    @JvmStatic
    fun playDistantSound(
        serverLevel: ServerLevel,
        soundEvent: SoundEvent,
        pos: Vec3,
        radius: Float,
        pitch: Float,
        sender: Entity?,
        exclude: ServerPlayer? = null
    ) {
        val excludeUuid = exclude?.uuid
        val maxDistanceSqr = radius.toDouble() * radius * 256
        val players = serverLevel.getPlayers {
            (excludeUuid == null || it.uuid != excludeUuid) && it.distanceToSqr(pos) < maxDistanceSqr
        }
        if (players.isEmpty()) return

        // 同一个包发给所有收件人：sender 为 null 时用一次性 UUID，反正不会和任何玩家匹配
        val message = SoundClientMessage(
            soundEvent.location,
            pos.x,
            pos.y,
            pos.z,
            radius,
            pitch,
            sender?.uuid ?: UUID.randomUUID()
        )
        for (serverPlayer in players) {
            sendPacketTo(serverPlayer, message)
        }
    }
}
