package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity

interface BasicGeoProjectileEntity {
    fun getAnimationInstance(): BasicProjectileAnimationInstance<*>? = null

    fun getEmissiveTexture(): ResourceLocation? = null

    fun getHiddenTicks(): Int = 0

    fun getFlareHiddenTicks(): Int = 3

    fun getModelInstance(): BakedModelInstance? {
        val entity = this as Entity
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ProjectileModelReloadListener.getModel(
            ResourceLocation.fromNamespaceAndPath(namespace, "models/bedrock/projectile/$id.geo.json")
        )?.createInstance()
    }
}