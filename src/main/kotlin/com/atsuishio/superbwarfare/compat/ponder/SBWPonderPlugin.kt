package com.atsuishio.superbwarfare.compat.ponder

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.compat.ponder.scene.*
import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.minecraft.resources.ResourceLocation

object SBWPonderPlugin : PonderPlugin {
    override fun getModId(): String {
        return Mod.MODID
    }

    override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        AircraftCatapultPonderScene.register(helper)
        BiogasGeneratorPonderScene.register(helper)
        ChargingStationPonderScene.register(helper)
        ContainerPonderScene.register(helper)
        CreativeChargingStationPonderScene.register(helper)
        DpsGeneratorDeployerPonderScene.register(helper)
        DronePonderScene.register(helper)
        JumpPadPonderScene.register(helper)
        TargetDeployerPonderScene.register(helper)
        VehicleAssemblingTablePonderScene.register(helper)
    }
}
