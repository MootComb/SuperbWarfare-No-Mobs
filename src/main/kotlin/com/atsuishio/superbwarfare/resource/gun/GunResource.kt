package com.atsuishio.superbwarfare.resource.gun

import com.atsuishio.superbwarfare.data.CustomData
import com.atsuishio.superbwarfare.data.DefaultDataSupplier
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.item.gun.EmptyGunItem
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class GunResource private constructor(stack: ItemStack) : DefaultDataSupplier<DefaultGunResource> {
    val stack: ItemStack
    val item: GunItem
    val id: String

    private var cache: DefaultGunResource? = null

    init {
        val item = stack.item
        val gunItem = item as? GunItem
        val useEmpty = gunItem == null || stack.isEmpty
        this.item = if (useEmpty) ModItems.EMPTY_GUN.get() as GunItem else gunItem
        this.stack = stack
        this.id = if (useEmpty) EmptyGunItem.EMPTY_GUN_ID else getRegistryId(stack.item)
    }

    fun compute(): DefaultGunResource {
        if (cache != null) return cache!!

        val defaultResource = getDefault().copy()

        // TODO 正确实现属性计算
        cache = defaultResource

        return defaultResource
    }

    fun update() {
        this.cache = null
    }

    override fun getDefault(): DefaultGunResource {
        return CustomData.GUN_RESOURCE.getOrElseGet(id) { DefaultGunResource() }
    }

    companion object {
        val RESOURCE_CACHE: LoadingCache<ItemStack, GunResource> = CacheBuilder.newBuilder()
            .weakKeys()
            .weakValues()
            .build(object : CacheLoader<ItemStack, GunResource>() {
                override fun load(stack: ItemStack): GunResource {
                    return GunResource(stack)
                }
            })

        @JvmStatic
        fun compute(stack: ItemStack): DefaultGunResource {
            return from(stack).compute()
        }

        @JvmStatic
        fun getDefault(id: String?): DefaultGunResource {
            return CustomData.GUN_RESOURCE.getOrElseGet(id) { DefaultGunResource() }
        }

        @JvmStatic
        fun getDefault(stack: ItemStack): DefaultGunResource {
            return getDefault(stack.item)
        }

        @JvmStatic
        fun getDefault(item: Item): DefaultGunResource {
            return getDefault(getRegistryId(item))
        }

        @JvmStatic
        fun create(item: Item): GunResource {
            return from(ItemStack(item))
        }

        @JvmStatic
        fun from(stack: ItemStack): GunResource {
            return RESOURCE_CACHE.getUnchecked(stack)
        }

        @JvmStatic
        fun getRegistryId(item: Item): String {
            var id = item.descriptionId
            id = id.substring(id.indexOf(".") + 1).replace('.', ':')
            return id
        }
    }
}
