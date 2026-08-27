package com.atsuishio.superbwarfare.compat.ponder.scene

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.createEntity
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.modifyBlockEntity
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.preScanBounds
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.setBlock
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.showStructure
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport.showText
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

object BiogasGeneratorPonderScene {
    fun register(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        helper.forComponents(loc("biogas_generator"))
            .addStoryBoard("basic_7x7", BiogasGeneratorPonderScene::introScene)
    }

    private fun introScene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("biogas_generator_intro", "Biogas Generator Introduction")
        val context = GeneratedPonderSupport.Context()
        preScanBounds(scene, BlockPos(1, 1, 3), BlockPos(3, 2, 4))
        scene.addKeyframe()
        showStructure(scene, context, null, null, null, null)
        scene.idle(20)
        setBlock(
            scene,
            context,
            "superbwarfare:biogas_generator",
            null,
            BlockPos(3, 1, 3),
            null,
            "{Power:0.0f}",
            false,
            spawnParticles = false,
            entranceAnimation = "down",
            entranceDuration = 20,
            entranceInterval = 1,
            smartDisplay = false,
            linkId = null,
            direction = "down"
        )
        scene.idle(20)
        scene.addKeyframe()
        showText(scene, "This is a very distinctive power-generating device", Vec3(3.5, 1.0, 3.5), 60, null, true)
        scene.idle(80)
        setBlock(
            scene,
            context,
            "minecraft:composter",
            mapOf("level" to "0"),
            BlockPos(3, 2, 3),
            null,
            null,
            false,
            spawnParticles = false,
            entranceAnimation = "down",
            entranceDuration = 20,
            entranceInterval = 1,
            smartDisplay = false,
            linkId = null,
            direction = "down"
        )
        scene.addKeyframe()
        showText(
            scene,
            "It detects the number of specific entities inside the composter placed above it. More entities increase power generation efficiency",
            Vec3(3.5, 1.0, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        setBlock(
            scene,
            context,
            "superbwarfare:charging_station",
            mapOf(
                "show_range" to "false",
                "facing" to "south"
            ),
            BlockPos(3, 1, 4),
            null,
            "{Energy:0,FuelTick:0,Items:[],MaxFuelTick:1600,ShowRange:0b}",
            false,
            spawnParticles = false,
            entranceAnimation = "down",
            entranceDuration = 20,
            entranceInterval = 1,
            smartDisplay = false,
            linkId = null,
            direction = "down"
        )
        setBlock(
            scene,
            context,
            "minecraft:comparator",
            mapOf(
                "mode" to "compare",
                "facing" to "east"
            ),
            BlockPos(2, 1, 4),
            null,
            "{OutputSignal:11}",
            false,
            spawnParticles = false,
            entranceAnimation = "down",
            entranceDuration = 20,
            entranceInterval = 1,
            smartDisplay = false,
            linkId = null,
            direction = "down"
        )
        setBlock(
            scene,
            context,
            "minecraft:redstone_lamp",
            null,
            BlockPos(1, 1, 4),
            null,
            null,
            immediateDisplay = false,
            spawnParticles = false,
            entranceAnimation = "down",
            entranceDuration = 20,
            entranceInterval = 1,
            smartDisplay = false,
            linkId = null,
            direction = "down"
        )
        scene.addKeyframe()
        showText(scene, "This is the simplest detection setup", Vec3(2.0, 1.5, 4.5), 60, null, true)
        scene.idle(80)
        createEntity(
            scene,
            context,
            "superbwarfare:senpai",
            Vec3(3.5, 2.0, 3.5),
            null,
            null,
            null,
            "{AbsorptionAmount:0.0f,ArmorDropChances:[0.085f,0.085f,0.085f,0.085f],ArmorItems:[{},{},{},{}],Attributes:[{Base:0.08d,Name:\"neoforge:entity_gravity\"},{Base:0.0d,Name:\"forge:step_height_addition\"},{Base:0.23d,Name:\"minecraft:generic.movement_speed\"}],Brain:{memories:{}},CanPickUpLoot:0b,CanUpdate:1b,FallFlying:0b,ForgeCaps:{\"curios:inventory\":{Curios:[]},\"superbwarfare:phosphorus_fire_capability\":{SbwPhosphorusFire:0b}},ForgeData:{},HandDropChances:[0.085f,0.085f],HandItems:[{},{}],Health:24.0f,HurtByTimestamp:0,Invulnerable:0b,LeftHanded:1b,PersistenceRequired:0b,Runner:0b}",
            null,
            "simultaneous",
            null,
            "down"
        )
        scene.addKeyframe()
        showText(scene, "Place an entity to generate power", Vec3(3.5, 2.0, 3.5), 60, null, true)
        modifyBlockEntity(
            scene,
            mapOf("show_range" to "false"),
            "{Energy:4000000,FuelTick:0,Items:[],MaxFuelTick:1600,ShowRange:0b}",
            true,
            BlockPos(3, 1, 4),
            null
        )
        modifyBlockEntity(
            scene,
            mapOf(
                "mode" to "compare",
                "powered" to "true",
                "facing" to "east"
            ),
            "{OutputSignal:14}",
            true,
            BlockPos(2, 1, 4),
            null
        )
        modifyBlockEntity(
            scene,
            mapOf("lit" to "true"),
            null,
            true,
            BlockPos(1, 1, 4),
            null
        )
        scene.idle(80)
    }
}
