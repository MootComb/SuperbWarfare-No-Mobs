package com.atsuishio.superbwarfare.compat.ponder.scene

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport
import com.atsuishio.superbwarfare.entity.living.DPSGeneratorEntity
import com.atsuishio.superbwarfare.init.ModEntities
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

object DpsGeneratorDeployerPonderScene {
    fun register(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        helper.forComponents(loc("dps_generator_deployer"))
            .addStoryBoard("basic_5x5", DpsGeneratorDeployerPonderScene::introScene)
            .addStoryBoard("basic_5x5", DpsGeneratorDeployerPonderScene::exportEnergyScene)
    }

    private fun introScene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("dps_generator_deployer_intro", "DPS Generator Introduction")
        val context = GeneratedPonderSupport.Context()
        scene.addKeyframe()
        GeneratedPonderSupport.showStructure(scene, context, null, null, null, null)

        val centerPos = util.grid().at(2, 0, 2)
        val pos = util.vector().topOf(centerPos)
        val entity = scene.world().createEntity {
            val target = ModEntities.DPS_GENERATOR.get().create(it) ?: return@createEntity null
            target.setPosRaw(pos.x, pos.y, pos.z)
            target.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3(pos.x, pos.y, pos.z - 1))
            target.xRot = 0f
            target.xRotO = 0f
            target
        }

        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "The DPS Generator is a target that generates and stores FE energy when damaged", Vec3(2.5, 2.0, 2.5), 60, null, true)
        scene.addKeyframe()
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "Attacking the target lets it store FE energy", Vec3(2.5, 2.0, 2.5), 60, null, true)
        scene.idle(80)
        GeneratedPonderSupport.showControls(scene, Vec3(2.5, 2.0, 2.5), "right", 60, "left", "superbwarfare:beast", null, false, false)
        scene.idle(20)
        scene.world().modifyEntity(entity) {
            val target = it as? DPSGeneratorEntity ?: return@modifyEntity
            target.downTime = 40
        }
        scene.idle(40)
        scene.world().modifyEntity(entity) {
            val target = it as? DPSGeneratorEntity ?: return@modifyEntity
            target.generatorLevel = 2
        }
        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "It regenerates quickly. If the player knocks it down before it recovers, its generator level increases by one, up to level 7", Vec3(2.5, 2.0, 2.5), 60, null, true)
        scene.idle(20)
        scene.world().modifyEntity(entity) {
            val target = it as? DPSGeneratorEntity ?: return@modifyEntity
            target.generatorLevel = 3
        }
        scene.idle(20)
        scene.world().modifyEntity(entity) {
            val target = it as? DPSGeneratorEntity ?: return@modifyEntity
            target.generatorLevel = 4
        }
        scene.idle(60)
    }

    private fun exportEnergyScene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("dps_generator_deployer_export_energy", "Exporting FE Energy")
        val context = GeneratedPonderSupport.Context()
        GeneratedPonderSupport.preScanBounds(scene, BlockPos(2, 1, 2), BlockPos(2, 1, 2))
        GeneratedPonderSupport.showStructure(scene, context, null, null, null, null)
        scene.idle(20)
        GeneratedPonderSupport.createEntity(scene, context, "superbwarfare:dps_generator", Vec3(2.5, 2.0, 2.5), Vec3(2.5, 0.0, 0.5), null, null, "{AbsorptionAmount:0.0f,Attributes:[{Base:0.0d,Name:\"forge:step_height_addition\"},{Base:0.08d,Name:\"forge:entity_gravity\"},{Base:0.0d,Name:\"minecraft:generic.movement_speed\"}],Brain:{memories:{}},CanUpdate:1b,Energy:0,FallFlying:0b,ForgeCaps:{\"curios:inventory\":{Curios:[]},\"superbwarfare:phosphorus_fire_capability\":{SbwPhosphorusFire:0b}},Health:40.0f,HurtByTimestamp:0,Invulnerable:0b,Level:0,NoGravity:1b}", null, "simultaneous", null, "down")
        GeneratedPonderSupport.setBlock(scene, context, "superbwarfare:charging_station", mapOf("show_range" to "false"), BlockPos(2, 1, 2), null, "{Energy:0,FuelTick:0,Items:[],MaxFuelTick:1600,ShowRange:0b}", false, false, "down", 20, 1, false, null, "down")
        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "Place any block with FE storage directly below the DPS Generator to export energy", Vec3(2.5, 3.0, 2.5), 60, null, true)
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "This example uses the Charging Station from this mod", Vec3(2.5, 1.5, 2.0), 60, null, true)
        scene.idle(70)
    }
}
