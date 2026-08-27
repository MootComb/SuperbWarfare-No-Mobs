package com.atsuishio.superbwarfare.network

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.network.message.receive.*
import com.atsuishio.superbwarfare.network.message.send.*
import com.atsuishio.superbwarfare.serialization.ByteBufDecoder
import com.atsuishio.superbwarfare.serialization.ByteBufEncoder
import com.atsuishio.superbwarfare.tools.camelToSnake
import com.atsuishio.superbwarfare.tools.createStreamCodec
import kotlinx.serialization.serializer
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent
import net.neoforged.neoforge.network.handling.IPayloadHandler
import net.neoforged.neoforge.network.registration.PayloadRegistrar

val payloadTypeMap = mutableMapOf<Class<*>, CustomPacketPayload.Type<*>>()

inline fun <reified T> encodeTo(output: FriendlyByteBuf, value: T) {
    ByteBufEncoder(output).encodeSerializableValue(serializer(), value)
}

inline fun <reified T> decodeFrom(input: FriendlyByteBuf): T {
    return ByteBufDecoder(input).decodeSerializableValue(serializer())
}

private inline fun <reified T : PacketPayload> playTo(reg: (CustomPacketPayload.Type<T>, StreamCodec<in RegistryFriendlyByteBuf, T>, IPayloadHandler<T>) -> Unit) {

    val codec = createStreamCodec<T>()
    val className = T::class.java.simpleName.substringBeforeLast("Message")

    val name = className.camelToSnake()

    val type = CustomPacketPayload.Type<T>(loc(name))
    payloadTypeMap[T::class.java] = type

    reg(type, codec) { msg, context -> with(msg) { context.handler() } }
}

private inline fun <reified T : ServerPacketPayload> playToServer() {
    playTo<T> { type, codec, handler ->
        registrar!!.playToServer<T>(type, codec, handler)
    }
}

private inline fun <reified T : ClientPacketPayload> playToClient() {
    playTo<T> { type, codec, handler ->
        registrar!!.playToClient<T>(type, codec, handler)
    }
}

private var registrar: PayloadRegistrar? = null

fun initializeNetwork(event: RegisterPayloadHandlersEvent) {
    registrar = event.registrar("1")
    registerPayloads()
}

private fun registerPayloads() {
    playToClient<ClientIndicatorMessage>()
    playToClient<ClientSetMotionMessage>()
    playToClient<DataSyncMessage>()
    playToClient<ClientMotionSyncMessage>()
    playToClient<ClientPhosphorusFireMessage>()
    playToClient<ContainerDataMessage>()
    playToClient<DrawClientMessage>()
    playToClient<FinishAssemblingVehicleMessage>()
    playToClient<LivingGunKillMessage>()
    playToClient<PlayerVariablesSyncMessage>()
    playToClient<RadarMenuCloseMessage>()
    playToClient<RadarMenuOpenMessage>()
    playToClient<ResetCameraTypeMessage>()
    playToClient<BeyondVisualEntitySyncMessage>()
    playToClient<ExplosionParticleMessage>()
    playToClient<MissileTrailParticleMessage>()
    playToClient<ShakeClientMessage>()
    playToClient<ShootClientMessage>()
    playToClient<SoundClientMessage>()
    playToClient<VehicleShootClientMessage>()
    playToClient<TDMSyncMessage>()
    playToClient<EntityRelationSyncMessage>()
    playToClient<PlayerInfoSyncMessage>()
    playToClient<RadarSyncMessage>()
    playToClient<ClientVehicleItemMessage>()
    playToClient<OpenVehicleSkinScreenMessage>()
    playToClient<OpenTacticalMapScreenMessage>()

    playToServer<AdjustMortarAngleMessage>()
    playToServer<AdjustZoomFovMessage>()
    playToServer<AimVillagerMessage>()
    playToServer<AssembleVehicleMessage>()
    playToServer<ChangeVehicleSeatMessage>()
    playToServer<ArtilleryIndicatorFireMessage>()
    playToServer<DogTagFinishEditMessage>()
    playToServer<DoubleJumpMessage>()
    playToServer<DroneFireMessage>()
    playToServer<EditMessage>()
    playToServer<FireKeyMessage>()
    playToServer<FireModeMessage>()
    playToServer<FiringParametersEditMessage>()
    playToServer<GunReforgeMessage>()
    playToServer<InteractMessage>()
    playToServer<LaserShootMessage>()
    playToServer<LungeMineAttackMessage>()
    playToServer<MeleeAttackMessage>()
    playToServer<MouseMoveMessage>()
    playToServer<ParachuteMessage>()
    playToServer<PlayerStopRidingMessage>()
    playToServer<RadarChangeModeMessage>()
    playToServer<RadarSetPosMessage>()
    playToServer<RadarSetTargetMessage>()
    playToServer<RadarSetParametersMessage>()
    playToServer<ReloadMessage>()
    playToServer<SeekingWeaponWarningMessage>()
    playToServer<SensitivityMessage>()
    playToServer<SetFiringParametersMessage>()
    playToServer<SetVehicleSkinMessage>()
    playToServer<SetPerkLevelMessage>()
    playToServer<ShootMessage>()
    playToServer<ShowChargingRangeMessage>()
    playToServer<SwitchScopeMessage>()
    playToServer<SwitchVehicleWeaponMessage>()
    playToServer<UnloadMessage>()
    playToServer<VehicleFireMessage>()
    playToServer<VehicleMovementMessage>()
    playToServer<WeaponZoomingMessage>()
    playToServer<ZoomMessage>()
    playToServer<BlueprintCraftMessage>()
    playToServer<BlueprintSetIndexMessage>()
    playToServer<LoiterConfigMessage>()
    playToServer<LoiterOverrideMessage>()
    playToServer<VehicleUnloadPassengersMessage>()
    playToServer<VehicleDisconnectTowingMessage>()
    playToServer<EntityClearMessage>()
    playToServer<EntityAreaClearMessage>()
}