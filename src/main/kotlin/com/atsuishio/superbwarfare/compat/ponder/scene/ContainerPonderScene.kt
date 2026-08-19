package com.atsuishio.superbwarfare.compat.ponder.scene

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.compat.ponder.GeneratedPonderSupport
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3

object ContainerPonderScene {
    fun register(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        helper.forComponents(loc("container"))
            .addStoryBoard("basic_5x5", ContainerPonderScene::introScene)
            .addStoryBoard("basic_7x7", ContainerPonderScene::type63Scene)
            .addStoryBoard("basic_7x7", ContainerPonderScene::firingProcessScene)
    }

    private fun introScene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("container_intro", "Container Introduction")
        val context = GeneratedPonderSupport.Context()
        scene.addKeyframe()
        GeneratedPonderSupport.showStructure(scene, context, null, null, null, null)
        GeneratedPonderSupport.setBlock(
            scene, context, "superbwarfare:container",
            null,
            BlockPos(2, 1, 2),
            null,
            null,
            immediateDisplay = false,
            spawnParticles = false,
            entranceAnimation = "none",
            entranceDuration = null,
            entranceInterval = null,
            smartDisplay = null,
            linkId = null,
            direction = null
        )
        scene.idle(20)
        GeneratedPonderSupport.showText(
            scene,
            "Vehicles from this mod are stored in containers",
            Vec3(2.5, 1.0, 2.5),
            40,
            null,
            true
        )
        scene.idle(50)
        GeneratedPonderSupport.showText(
            scene,
            "Use a crowbar to right-click the container to open it",
            Vec3(2.5, 1.0, 2.5),
            40,
            null,
            true
        )
        GeneratedPonderSupport.showControls(
            scene, Vec3(2.5, 1.0, 2.0), "right", 40, "right", "superbwarfare:crowbar", null,
            whileSneaking = false,
            whileCtrl = false
        )
        scene.idle(50)
        scene.idle(10)
        scene.addKeyframe()
        GeneratedPonderSupport.setBlock(
            scene, context, "minecraft:stone",
            null,
            BlockPos(3, 1, 3),
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
        scene.idle(20)
        GeneratedPonderSupport.showText(
            scene,
            "The container needs some space to open",
            Vec3(2.5, 1.0, 2.5),
            40,
            null,
            true
        )
        scene.idle(50)
        GeneratedPonderSupport.showText(
            scene,
            "If there are obstacles around it, the container cannot open",
            Vec3(3.5, 2.0, 3.5),
            40,
            "red",
            true
        )
        scene.idle(50)
        GeneratedPonderSupport.destroyBlock(scene, context, BlockPos(3, 1, 3), BlockPos(2, 1, 2), false)
        scene.idle(10)
        scene.addKeyframe()
        GeneratedPonderSupport.createEntity(
            scene,
            context,
            "superbwarfare:wheel_chair",
            Vec3(2.5, 1.0, 2.5),
            null,
            null,
            null,
            null,
            "1",
            "simultaneous",
            null,
            "north"
        )
        scene.idle(20)
        GeneratedPonderSupport.showText(
            scene,
            "Sneak + right-click a vehicle with a crowbar to return it to its container",
            Vec3(2.5, 1.0, 2.5),
            40,
            null,
            true
        )
        GeneratedPonderSupport.showControls(
            scene, Vec3(2.5, 1.0, 2.0), "right", 40, "right", "superbwarfare:crowbar", null,
            whileSneaking = true,
            whileCtrl = false
        )
        scene.idle(50)
        GeneratedPonderSupport.clearEntities(
            scene, context, false, "superbwarfare:wheel_chair", "1", null, null, "simultaneous", null, "north"
        )
    }

    private fun type63Scene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("container_type63", "Type-63 107mm MLRS Introduction")
        val context = GeneratedPonderSupport.Context()
        scene.addKeyframe()
        GeneratedPonderSupport.showStructure(scene, context, null, null, null, null)
        scene.idle(20)
        GeneratedPonderSupport.createEntity(
            scene,
            context,
            "superbwarfare:type_63",
            Vec3(3.5, 1.0, 3.5),
            Vec3(3.5, 2.0, 0.5),
            null,
            null,
            "{CanUpdate:1b,ChargeProgress:0.0f,DecoyCount:0,DecoyReloadCoolDown:500,DogTagIcon:[[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]],ForgeData:{},GearRot:0.0f,GearUp:0b,GunXRot:0.0f,GunYRot:0.0f,Health:100.0f,HoverMode:0b,Inventory:{Items:[],Size:12},Invulnerable:0b,IsWreck:0b,LastAttacker:\"undefined\",LastDriver:\"undefined\",LeftWheelDamaged:0b,LeftWheelHealth:50.0f,Locked:0b,LoiterActive:0b,LoiterR:400.0f,LoiterX:0.0f,LoiterY:318.0f,LoiterZ:0.0f,MainEngineDamaged:0b,MainEngineHealth:50.0f,Pitch:0.0f,Power:0.0f,PropellerRot:0.0f,RightWheelDamaged:0b,RightWheelHealth:50.0f,ServerPitch:0.0f,ServerYaw:90.0f,SubEngineDamaged:0b,SubEngineHealth:50.0f,SympatheticDetonated:0b,TowedByUUID:\"\",TowingUUID:\"\",TowingUUIDs:[],TurretBurnTimer:0,TurretBurned:0b,TurretDamaged:0b,TurretHealth:50.0f,TurretXRot:0.0f,TurretYRot:0.0f,TurretYRotLock:0.0f,WeaponState:{AP:{tag:{Attachments:{},GunData:{},Perks:{}}},CM:{tag:{Attachments:{},GunData:{},Perks:{}}},HE:{tag:{Attachments:{},GunData:{},Perks:{}}},Main:{tag:{Attachments:{},GunData:{},Perks:{}}}},Yaw:0.0f}",
            null,
            "simultaneous",
            null,
            "north"
        )
        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "This is the Type-63 107mm multiple rocket launcher. There may be a conflict between certain mod and Ponder if you can't see it",
            Vec3(3.5, 2.0, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.rotateCameraY(scene, -90.0f, 0f, 20)
        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "There is an adjustment dial on top of the tire, roughly at this position",
            Vec3(2.5, 2.3, 4.3),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(2.5, 2.3, 4.3), "right", 60, "right", null, null,
            whileSneaking = false,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Hold right-click and rotate the crank clockwise to raise the barrel; sneak + right-click does the opposite",
            Vec3(2.5, 2.3, 4.3),
            60,
            null,
            true
        )
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "There is a small dial protruding from the right side of the tire, roughly at this position",
            Vec3(3.5, 1.0, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(3.5, 1.0, 3.5), "right", 60, "right", null, null,
            whileSneaking = false,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Hold right-click and rotate the crank counter-clockwise to slightly reduce the turret yaw; sneak + right-click does the opposite",
            Vec3(3.5, 1.0, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "You can also aim at the left or right handle on the turret to adjust its yaw",
            Vec3(2.5, 1.3, 4.7),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(2.5, 1.3, 4.7), "right", 60, "right", null, null, true,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Sneak + right-click and push the left handle to greatly increase yaw and turn the turret right",
            Vec3(2.5, 1.3, 4.7),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.rotateCameraY(scene, -90.0f, 0f, 20)
        scene.idle(20)
        GeneratedPonderSupport.showControls(
            scene, Vec3(4.4, 1.3, 4.7), "right", 60, "right", null, null,
            whileSneaking = true,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Sneak + right-click and push the right handle to greatly reduce yaw and turn the turret left",
            Vec3(4.4, 1.3, 4.7),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(3.5, 2.0, 3.5), "right", 60, null, "superbwarfare:firing_parameters", null,
            whileSneaking = false,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Holding firing data shows the target values of the turret",
            Vec3(3.5, 2.0, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
    }

    private fun firingProcessScene(scene: SceneBuilder, util: SceneBuildingUtil) {
        scene.title("container_firing_process", "Rocket Launch Process")
        val context = GeneratedPonderSupport.Context()
        GeneratedPonderSupport.showStructure(scene, context, null, null, null, null)
        scene.idle(20)
        GeneratedPonderSupport.createEntity(
            scene,
            context,
            "superbwarfare:type_63",
            Vec3(3.5, 1.0, 3.5),
            Vec3(3.5, 2.0, 0.5),
            null,
            null,
            "{CanUpdate:1b,ChargeProgress:0.0f,DecoyCount:0,DecoyReloadCoolDown:500,DogTagIcon:[[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1],[I;-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1]],ForgeData:{},GearRot:0.0f,GearUp:0b,GunXRot:0.0f,GunYRot:0.0f,Health:100.0f,HoverMode:0b,Inventory:{Items:[],Size:12},Invulnerable:0b,IsWreck:0b,LastAttacker:\"undefined\",LastDriver:\"undefined\",LeftWheelDamaged:0b,LeftWheelHealth:50.0f,Locked:0b,LoiterActive:0b,LoiterR:400.0f,LoiterX:0.0f,LoiterY:318.0f,LoiterZ:0.0f,MainEngineDamaged:0b,MainEngineHealth:50.0f,Pitch:0.0f,Power:0.0f,PropellerRot:0.0f,RightWheelDamaged:0b,RightWheelHealth:50.0f,ServerPitch:0.0f,ServerYaw:90.0f,SubEngineDamaged:0b,SubEngineHealth:50.0f,SympatheticDetonated:0b,TowedByUUID:\"\",TowingUUID:\"\",TowingUUIDs:[],TurretBurnTimer:0,TurretBurned:0b,TurretDamaged:0b,TurretHealth:50.0f,TurretXRot:0.0f,TurretYRot:0.0f,TurretYRotLock:0.0f,WeaponState:{AP:{tag:{Attachments:{},GunData:{},Perks:{}}},CM:{tag:{Attachments:{},GunData:{},Perks:{}}},HE:{tag:{Attachments:{},GunData:{},Perks:{}}},Main:{tag:{Attachments:{},GunData:{},Perks:{}}}},Yaw:0.0f}",
            null,
            "simultaneous",
            null,
            "north"
        )
        scene.idle(20)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "The launcher currently uses three types of medium-caliber rockets",
            Vec3(3.5, 2.0, 3.5),
            60,
            null,
            true
        )
        GeneratedPonderSupport.createItemEntity(
            scene,
            context,
            "superbwarfare:medium_rocket_cm",
            1,
            Vec3(1.5, 1.0, 0.5),
            Vec3(0.0, 0.0, 0.0),
            null,
            null
        )
        GeneratedPonderSupport.createItemEntity(
            scene,
            context,
            "superbwarfare:medium_rocket_he",
            1,
            Vec3(3.5, 1.0, 0.5),
            Vec3(0.0, 0.0, 0.0),
            null,
            null
        )
        GeneratedPonderSupport.createItemEntity(
            scene,
            context,
            "superbwarfare:medium_rocket_ap",
            1,
            Vec3(5.5, 1.0, 0.5),
            Vec3(0.0, 0.0, 0.0),
            null,
            null
        )
        scene.idle(80)
        GeneratedPonderSupport.rotateCameraY(scene, -145.5f, 0f, 20)
        scene.idle(20)
        GeneratedPonderSupport.clearItemEntities(scene, context, true, null, null, null, null)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Rockets can be placed into an “empty” barrel pointed at by the crosshair",
            Vec3(3.5, 1.3, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(3.5, 1.3, 3.5), "right", 60, "right", "superbwarfare:medium_rocket_ap", null,
            whileSneaking = false,
            whileCtrl = false
        )
        scene.idle(80)
        GeneratedPonderSupport.showControls(
            scene, Vec3(3.5, 1.3, 3.5), "right", 60, "right", "superbwarfare:crowbar", "{Damage:0}",
            whileSneaking = false,
            whileCtrl = false
        )
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "Right-click the turret with the crowbar from this mod to fire barrels in order",
            Vec3(3.5, 1.3, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(scene, "A flint and steel also works", Vec3(3.5, 1.3, 3.5), 60, null, true)
        scene.idle(80)
        scene.addKeyframe()
        GeneratedPonderSupport.showText(
            scene,
            "If you do not want sequential fire, aim at a loaded barrel and use the crowbar on it to fire",
            Vec3(3.3, 1.3, 3.5),
            60,
            null,
            true
        )
        scene.idle(80)
    }

}

