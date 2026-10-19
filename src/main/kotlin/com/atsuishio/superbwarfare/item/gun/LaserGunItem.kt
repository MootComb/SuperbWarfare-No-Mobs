package com.atsuishio.superbwarfare.item.gun

import com.atsuishio.superbwarfare.client.renderer.gun.LaserGunRenderer
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ShootParameters
import com.atsuishio.superbwarfare.entity.projectile.LaserBeamEntity
import net.minecraft.world.entity.Entity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

open class LaserGunItem(properties: Properties) : GeoGunItem(properties) {
    @OnlyIn(Dist.CLIENT)
    override fun createRenderer() = LaserGunRenderer()

    override fun onProjectileCreated(parameters: ShootParameters, entity: Entity) {
        super.onProjectileCreated(parameters, entity)
        if (entity is LaserBeamEntity) {
            entity.prepareShot(parameters.data.get(GunProp.RANGE).toDouble(), parameters.power)
        }
    }
}
