package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.init.ModEntities
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class MiniBombEntity : GunGrenadeEntity {
    constructor(type: EntityType<out MiniBombEntity>, level: Level) : super(type, level)

    constructor(owner: Entity?, level: Level) : super(ModEntities.MINI_BOMB.get(), level) {
        this.owner = owner
    }

    override fun canSplit() = false

    override fun getHiddenTicks() = 0
}
