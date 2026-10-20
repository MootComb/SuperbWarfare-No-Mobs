package com.atsuishio.superbwarfare.client.sound

import com.atsuishio.superbwarfare.entity.projectile.ReforgingLightningBallEntity
import com.atsuishio.superbwarfare.init.ModSounds
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class LightningBallFlySoundInstance(private val entity: ReforgingLightningBallEntity) :
    AbstractTickableSoundInstance(
        ModSounds.LIGHTNING_BALL_FLY.get(),
        SoundSource.AMBIENT,
        SoundInstance.createUnseededRandom()
    ) {

    private val client = Minecraft.getInstance()

    init {
        looping = true
        delay = 0
        volume = 1f
        x = entity.x
        y = entity.y
        z = entity.z
    }

    override fun tick() {
        if (entity.isRemoved || client.player == null) {
            stop()
            return
        }

        x = entity.x
        y = entity.y
        z = entity.z
    }
}
