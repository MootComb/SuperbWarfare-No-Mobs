package com.atsuishio.superbwarfare.client.overlay.ammo

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.data.gun.AmmoSource
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.gun.ammo_consumer_strategy.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
object AmmoHudResources {
    sealed interface Icon {
        data class Item(val stack: ItemStack) : Icon
        data class Texture(val location: ResourceLocation) : Icon
    }

    private val icons = linkedMapOf<Class<out AmmoConsumeStrategy>, (AmmoSource) -> Icon>()

    fun registerIcon(strategy: Class<out AmmoConsumeStrategy>, provider: (AmmoSource) -> Icon) {
        icons[strategy] = provider
    }

    init {
        // Stable 16x16 resource-pack replacement points; see the README beside these textures.
        registerTexture(EnergyAmmoStrategy::class.java, "energy")
        registerTexture(HealthAmmoStrategy::class.java, "health")
        registerTexture(HungerAmmoStrategy::class.java, "hunger")
        registerTexture(ExpAmmoStrategy::class.java, "experience")
        registerTexture(InfiniteAmmoStrategy::class.java, "infinite")
    }

    private fun registerTexture(strategy: Class<out AmmoConsumeStrategy>, name: String) {
        registerIcon(strategy) { Icon.Texture(loc("textures/overlay/ammo_bar/resource/$name.png")) }
    }

    private fun icon(source: AmmoSource): Icon = icons[source.strategy.javaClass]?.invoke(source)
        ?: Icon.Item(source.stack().takeUnless { it.isEmpty } ?: source.playerAmmoType?.itemStack ?: ItemStack.EMPTY)

    // 副能源直接消耗 FE，不能使用主来源的弹匣或百分比语义。
    private fun low(data: GunData, source: AmmoSource, index: Int, player: Player): Boolean {
        if (data.hasInfiniteBackupAmmo(player)) return false
        if (index == 0) {
            val cost = data.get(GunProp.AMMO_COST_PER_SHOOT).coerceAtLeast(1)
            if (!data.useBackpackAmmo() && data.ammo.get() >= cost) return false
            // Primary prefixes convert one item into rounds; they are not per-shot item costs.
            return data.countBackupAmmo(player) < cost
        }
        if (source.strategy === EnergyAmmoStrategy) {
            return data.getEnergyProvider(player).map { it.energyStored < source.loadAmount }.orElseGet { true }
        }
        return !source.hasEnough(data, player)
    }

    data class Resource(val icon: Icon, val low: Boolean)

    fun group(data: GunData, player: Player): List<Resource> =
        data.selectedAmmoConsumer().sources.mapIndexed { index, source ->
            // Keep a snapshot for the outgoing animation; never retain a mutable inventory stack.
            val image = when (val image = icon(source)) {
                is Icon.Item -> Icon.Item(image.stack.copy())
                is Icon.Texture -> image
            }
            Resource(image, low(data, source, index, player))
        }
}
