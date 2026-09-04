package com.atsuishio.superbwarfare.api.event

import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.Event
import org.jetbrains.annotations.ApiStatus

@ApiStatus.AvailableSince("0.8.10")
open class ClientGunFireEvent(
    val shooter: Entity,
    val stack: ItemStack,
    val hand: InteractionHand = InteractionHand.MAIN_HAND
) : Event()
