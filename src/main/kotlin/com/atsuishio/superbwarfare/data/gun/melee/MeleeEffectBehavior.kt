package com.atsuishio.superbwarfare.data.gun.melee

import com.atsuishio.superbwarfare.data.gun.GunData
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * 一次近战效果触发的上下文。
 *
 * 所有行为都**只在服务端**执行（概率 `Chance` 用的是服务端权威的 `level.random`），
 * 所以这里直接给 [ServerLevel] / [Player]，行为里不必再判端。
 *
 * @param level 服务端世界
 * @param attacker 攻击者
 * @param data 结算用的枪械数据（主武器或副武器；冷却表写在它身上）
 * @param actionIndex 本次锁存的连招下标（调试与 Perk 上下文用）
 * @param action 本段动作（完全展开）
 * @param effect 本次要执行的效果（已展开预设与内联覆盖）
 * @param target 触发目标；`Swing` 触发、或目标是载具这类非生物时为 `null`
 * @param position 效果的作用点：命中点 ?: 目标位置 ?: 攻击者视线前方
 * @param damaged 这次攻击是否真的对这个目标造成了伤害（`Hit`/`Kill` 才有意义）
 * @param killed 这个目标是否被这一击打死（`Kill` 才有意义）
 */
class MeleeEffectContext(
    val level: ServerLevel,
    val attacker: Player,
    val data: GunData,
    val actionIndex: Int,
    val action: ResolvedMeleeAction,
    val effect: ResolvedMeleeEffect,
    val target: LivingEntity?,
    val position: Vec3,
    val damaged: Boolean = false,
    val killed: Boolean = false,
)

/**
 * 一种近战额外行为
 */
interface MeleeEffectBehavior {
    val id: String

    fun apply(context: MeleeEffectContext)

    fun validate(effect: ResolvedMeleeEffect): String? = null
}
