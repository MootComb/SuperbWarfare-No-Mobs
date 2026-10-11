package com.atsuishio.superbwarfare.client.charm

import com.atsuishio.superbwarfare.client.charm.CharmRuntime.apply
import com.atsuishio.superbwarfare.client.charm.CharmRuntime.clampBodyCone
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.attachment.CharmInfo
import com.atsuishio.superbwarfare.event.ClientEventHandler
import net.minecraft.client.Minecraft
import net.minecraft.world.InteractionHand
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.*

object CharmRuntime {

    /** 一个 (手, 配件, 挂点) 的摆动状态 */
    private class CharmState {
        val solver = CharmSolver()
        val params = CharmParams()

        /** 本帧的相机运动量；一帧算一次、各个子步共用 */
        val frame = CharmFrame()

        /** 数据侧给定的响应系数（开镜时会被压小，所以要留一份原始值） */
        var baseResponse = 0.15f
        var baseTurnResponse = 0.3f
        var aimResponse = 0.08f

        /** 上一次配置用的是哪份数据 / 哪个模型；变了就重算参数 */
        var configuredInfo: CharmInfo? = null
        var configuredLength = -1f

        var lastNanos = 0L

        /** 固定步长累加器（秒） */
        var accumulator = 0f

        /** 上一**帧**的相机世界坐标；相机线加速度是它的帧间差分 */
        var camX = 0.0
        var camY = 0.0
        var camZ = 0.0

        /**
         * 相机世界速度（方块/秒）的一阶低通与上一帧的值
         */
        val camVelocity = DoubleArray(3)
        val camVelocityPrev = DoubleArray(3)
        var hasCamera = false

        /**
         * 「视图 ← 世界」旋转（[apply] 收到的那个矩阵的逆）
         */
        val viewFromWorld = Matrix4f()

        /** 上一帧的 [viewFromWorld]，用来差分角速度 */
        val rotationPrev = Matrix4f()
        var hasRotation = false

        /** 低通之后的角速度（视图空间）与上一帧的值，后者用来差分出角加速度 */
        val omega = Vector3f()
        val omegaPrev = Vector3f()

        /** 上一次**解算**时用的悬挂点；子步之间线性插值到本帧采样值 */
        val anchorStep = Vector3f()
        val anchorInterpolated = Vector3f()
        var hasAnchor = false

        /**
         * 低通之后的悬挂点（**视图空间**）
         */
        val anchorFiltered = Vector3f()

        /** 最近两次解算出的摆角，渲染时在它们之间插值 */
        val previousSwing = Quaternionf()
        val currentSwing = Quaternionf()
        val outputSwing = Quaternionf()
        var hasSwing = false

        val anchorView = Vector3f()
        val direction = Vector3f()

        /** 模型局部 +X → 视图空间 的草稿（软限位的侧别参考轴） */
        val sideAxisLocal = Vector3f()

        val swing = Quaternionf()
        val rawOmega = Vector3f()
        val snapshot = CharmSnapshot()
    }

    private data class CharmKey(
        val hand: InteractionHand,
        val attachment: String,
        val mountBone: String,
    )

    private val states = HashMap<CharmKey, CharmState>()

    /**
     * 解算并把摆动姿态写进模型
     */
    @JvmStatic
    fun apply(
        model: BedrockAttachmentModel,
        definition: AttachmentDefinition,
        mountBone: String,
        modelToView: Matrix4f,
        worldFromView: Matrix4f,
        hand: InteractionHand,
    ): CharmSnapshot? {
        val rig = model.charmRig() ?: return null

        val key = CharmKey(hand, definition.getId(), mountBone)
        val state = states.getOrPut(key) { CharmState() }

        val info = definition.charmInfo()
        if (state.configuredInfo !== info || state.configuredLength != rig.length) {
            configure(state, info, rig.length)
        }

        val mc = Minecraft.getInstance()

        // 距上次推进太久（切 F5、开 GUI、暂停、换手）→ 重新归位，别让质点带着旧速度回来
        val now = System.nanoTime()
        if (now - state.lastNanos > RESET_GAP_NANOS) {
            state.solver.invalidate()
            state.accumulator = 0f
            state.hasCamera = false
            state.hasRotation = false
            state.hasAnchor = false
            // ⚠ 速度基线也要一起清掉：留着一整段"没渲染的时间"之前的速度去差分，
            // 下一帧会算出一个巨大的加速度（走了两步、参考点却停在原地）。
            state.camVelocity.fill(0.0)
            state.camVelocityPrev.fill(0.0)
            state.omega.set(0f, 0f, 0f)
            state.omegaPrev.set(0f, 0f, 0f)
        }
        state.lastNanos = now

        // 悬挂点：模型局部（摆点）→ 视图空间。每帧只采一次、子步共用：
        // 一帧之内枪的姿态不会变，重复采样只会白算几遍矩阵。
        // 「视图 ← 世界」也在这里备好，重力/角速度/相机加速度都要用它换基。
        state.viewFromWorld.set(worldFromView).invert()
        modelToView.transformPosition(rig.pivot, state.anchorView)

        // 模型局部 +X（= 用 [CharmInfo.lateralAngle] 声明"枪身侧"时用的那条尺子）转到视图空间，
        // 交给解算器区分"枪身侧/无遮挡侧"（见 `CharmSolver.swingLimit`）。
        // ⚠ 必须归一化：挂点变换里带着缩放（手臂锚点就是这么带 scale 的）。
        // ⚠ 和 [clampLateral] 用的是**同一个** `modelToView`，两边对"左右"的定义天然一致。
        modelToView.transformDirection(1f, 0f, 0f, state.sideAxisLocal)
        if (state.sideAxisLocal.lengthSquared() < 1.0e-8f) {
            state.frame.sideAxis.set(1f, 0f, 0f)
        } else {
            state.frame.sideAxis.set(state.sideAxisLocal).normalize()
        }

        val frameDt = (mc.deltaFrameTime / 20f).coerceIn(MIN_FRAME_DT, MAX_FRAME_DT)
        if (!state.hasAnchor) {
            state.anchorFiltered.set(state.anchorView)
            state.anchorStep.set(state.anchorView)
            state.hasAnchor = true
        } else {
            // 一阶低通：滤掉 tick 量化出来的台阶（走路/飞行时的高频抖动来源）
            val tau = state.params.smoothing
            val k = if (tau > 0f) 1f - exp(-frameDt / tau) else 1f
            state.anchorFiltered.lerp(state.anchorView, k)
        }

        // 开镜时把响应压下去：瞄准时吊坠还在乱晃会干扰视线
        val zoom = ClientEventHandler.zoomTime.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse * (1f - zoom) + state.aimResponse * zoom
        state.params.turnResponse = state.baseTurnResponse * (1f - zoom) + state.aimResponse * zoom

        val cameraPos = mc.gameRenderer.mainCamera.position
        if (!state.hasCamera) {
            state.camX = cameraPos.x
            state.camY = cameraPos.y
            state.camZ = cameraPos.z
            state.hasCamera = true
        }
        updateFrame(state, frameDt, cameraPos.x, cameraPos.y, cameraPos.z)

        state.accumulator = (state.accumulator + frameDt).coerceAtMost(MAX_ACCUMULATOR)
        val steps = (state.accumulator / SIM_STEP).toInt().coerceAtMost(MAX_STEPS)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            state.anchorInterpolated.set(state.anchorStep).lerp(state.anchorFiltered, t)

            state.solver.update(state.anchorInterpolated, state.frame, state.params, SIM_STEP)
            state.accumulator -= SIM_STEP

            // 解算结果 → 模型局部空间的旋转；两次解算之间由渲染侧插值
            state.previousSwing.set(state.currentSwing)
            if (solveSwing(state, rig, modelToView)) {
                state.currentSwing.set(state.swing)
                state.hasSwing = true
            }
        }

        if (steps > 0) {
            state.camX = cameraPos.x
            state.camY = cameraPos.y
            state.camZ = cameraPos.z
            state.anchorStep.set(state.anchorFiltered)
        }

        // 还没跑过任何一步（刚装上 / 刚切回第一人称）：这一帧先按绑定姿态画
        if (!state.hasSwing) return null

        val alpha = (state.accumulator / SIM_STEP).coerceIn(0f, 1f)
        state.outputSwing.set(state.previousSwing).slerp(state.currentSwing, alpha)
        rig.apply(model.instance, state.outputSwing, state.snapshot)
        return state.snapshot
    }

    /**
     * 采集本帧的相机运动量（[CharmFrame]）。每帧**一次**，各个子步共用
     */
    private fun updateFrame(state: CharmState, frameDt: Float, camX: Double, camY: Double, camZ: Double) {
        val frame = state.frame
        val p = state.params
        val tau = p.smoothing
        val k = if (tau > 0f) 1f - exp(-frameDt / tau) else 1f

        // 重力（视图空间）
        frame.gravity.set(0f, -p.gravity, 0f)
        state.viewFromWorld.transformDirection(frame.gravity)

        // 相机线加速度（世界轴）→ 视图空间
        if (state.hasCamera) {
            state.camVelocity[0] += ((camX - state.camX) / frameDt - state.camVelocity[0]) * k
            state.camVelocity[1] += ((camY - state.camY) / frameDt - state.camVelocity[1]) * k
            state.camVelocity[2] += ((camZ - state.camZ) / frameDt - state.camVelocity[2]) * k
            frame.camAccel.set(
                ((state.camVelocity[0] - state.camVelocityPrev[0]) / frameDt).toFloat(),
                ((state.camVelocity[1] - state.camVelocityPrev[1]) / frameDt).toFloat(),
                ((state.camVelocity[2] - state.camVelocityPrev[2]) / frameDt).toFloat(),
            )
            state.camVelocityPrev[0] = state.camVelocity[0]
            state.camVelocityPrev[1] = state.camVelocity[1]
            state.camVelocityPrev[2] = state.camVelocity[2]
        } else {
            state.camVelocity[0] = (camX - state.camX) / frameDt
            state.camVelocity[1] = (camY - state.camY) / frameDt
            state.camVelocity[2] = (camZ - state.camZ) / frameDt
            state.camVelocityPrev[0] = state.camVelocity[0]
            state.camVelocityPrev[1] = state.camVelocity[1]
            state.camVelocityPrev[2] = state.camVelocity[2]
            frame.camAccel.set(0f, 0f, 0f)
            state.hasCamera = true
        }

        if (frame.camAccel.lengthSquared() > MAX_CAM_ACCEL * MAX_CAM_ACCEL) {
            frame.camAccel.normalize(MAX_CAM_ACCEL)
        }
        state.viewFromWorld.transformDirection(frame.camAccel)
        state.camX = camX
        state.camY = camY
        state.camZ = camZ

        // 相机角速度（视图空间）→ 低通 → 角加速度
        if (state.hasRotation) {
            CharmFrameMath.angularVelocity(state.rotationPrev, state.viewFromWorld, frameDt, state.rawOmega)
            state.omega.lerp(state.rawOmega, k)
            frame.alpha.set(state.omega).sub(state.omegaPrev).mul(1f / frameDt)
            state.omegaPrev.set(state.omega)
        } else {
            state.omega.set(0f, 0f, 0f)
            state.omegaPrev.set(0f, 0f, 0f)
            frame.alpha.set(0f, 0f, 0f)
            state.hasRotation = true
        }
        frame.omega.set(state.omega)
        state.rotationPrev.set(state.viewFromWorld)
    }

    /** 还原 [apply] 写进骨骼的摆动姿态（**必须**在画完之后调用） */
    @JvmStatic
    fun revert(model: BedrockAttachmentModel, snapshot: CharmSnapshot?) {
        val rig = model.charmRig() ?: return
        if (snapshot == null) return
        rig.restore(model.instance, snapshot)
    }

    /**
     * 解算出来的绳方向 → 模型局部空间的旋转，结果放在 `state.swing`
     */
    private fun solveSwing(state: CharmState, rig: CharmRig, modelToView: Matrix4f): Boolean {
        state.direction.set(state.solver.rope)
        MODEL_FROM_VIEW.set(modelToView).invert().transformDirection(state.direction)
        if (state.direction.lengthSquared() < 1.0e-8f) return false
        state.direction.normalize()

        // ⚠ 顺序有意义：先圆锥、后横向墙，让现有的横向墙保持最终裁决权 ——
        // 这样锥没咬住时，所有现有吊坠的输出与加这项之前逐位一致（见 [clampBodyCone]）。
        clampBodyCone(state.direction, rig.restDir, state.params.bodyMaxAngle)
        clampLateral(state.direction, state.params.lateralLimitSin)

        // ⚠ 最短弧旋转在 `direction ≈ −restDir` 处退化（叉积为零，"最短弧"没有定义）：
        // 方向在它附近来回抖就会逐帧翻面。开侧上限放宽到 90° 之后，硬夹逼**不再是**充分的保护 ——
        // 夹逼锥的轴在模型局部是"挂点变换求逆后的世界下方向"，而挂点会被收枪/冲刺/开镜动画转走，
        // 余量（`180° − β − 上限`）会缩水。这里干脆放弃这一帧的更新、保持上一次的结果，
        // 比翻面好看得多，而且方向一旦离开就会自己接上。
        if (state.direction.dot(rig.restDir) < -0.999f) return false

        // 从"绑定垂下方向"转到"解算方向"的最短弧旋转，就是两根分组骨骼要绕摆点转的角度
        state.swing.rotationTo(rig.restDir, state.direction)
        return true
    }

    private fun clampBodyCone(direction: Vector3f, restDir: Vector3f, limit: Float) {
        if (limit <= 0f) return

        // ⚠ 两个向量都在**模型局部**空间里（调用方已经换过基），所以这个夹角只跟挂点（枪身）走：
        // 世界重力、相机朝向、枪的动画姿态全都进不来 —— 拿重力方向来量就退化成 `MaxAngle` 那套重力圆锥了。
        val angleCos = direction.x * restDir.x + direction.y * restDir.y + direction.z * restDir.z
        if (angleCos >= cos(limit)) return

        // 轴 = direction × restDir：绕它正转 excess 就是沿最短弧朝 restDir 收
        var ax = direction.y * restDir.z - direction.z * restDir.y
        var ay = direction.z * restDir.x - direction.x * restDir.z
        var az = direction.x * restDir.y - direction.y * restDir.x
        val axisLen = sqrt(ax * ax + ay * ay + az * az)
        if (axisLen < 1.0e-8f) {
            // 方向几乎正对/背对绑定姿态：正对时上面已经返回，背对时（上限接近 180° 才可能）轴任取一个与 restDir 垂直的方向
            ax = 0f
            ay = restDir.z
            az = -restDir.y
            val l = sqrt(ax * ax + ay * ay + az * az)
            if (l < 1.0e-8f) {
                ax = restDir.z
                ay = 0f
                az = -restDir.x
            } else {
                ax /= l
                ay /= l
                az /= l
            }
        } else {
            ax /= axisLen
            ay /= axisLen
            az /= axisLen
        }

        val excess = acos(angleCos.coerceIn(-1f, 1f)) - limit
        if (excess <= 0f) return

        CONE_ROTATION.fromAxisAngleRad(ax, ay, az, excess).transform(direction)
    }

    /**
     * 侧向限位：把绳方向在模型局部 X 上的分量钳到 [limitSin]，Y/Z 等比缩放保持单位长度
     */
    private fun clampLateral(direction: Vector3f, limitSin: Float) {
        if (limitSin == 0f) return
        val x = direction.x
        if (limitSin > 0f) {
            if (x <= limitSin) return
        } else {
            if (x >= limitSin) return
        }

        val rest = sqrt((1f - limitSin * limitSin).coerceAtLeast(0f))
        val yz = sqrt(direction.y * direction.y + direction.z * direction.z)
        if (yz > 1.0e-6f) {
            val k = rest / yz
            direction.y *= k
            direction.z *= k
        } else {
            // 退化：本来几乎就是正上/正下方，直接给个正下方
            direction.y = -rest
            direction.z = 0f
        }
        direction.x = limitSin
    }

    /** 数据或模型换了：把 [CharmInfo] 摊平成解算用的标量 */
    private fun configure(state: CharmState, info: CharmInfo, length: Float) {
        state.configuredInfo = info
        state.configuredLength = length

        val params = state.params
        params.length = length
        val gravity = info.gravity
        if (gravity != null) {
            // 显式指定了等效重力就用它（想要"真实重力"的写法）
            params.gravity = gravity.toFloat()
        } else {
            params.applyFrequency(info.frequency)
        }
        params.damping = info.damping.coerceAtLeast(0.0).toFloat()
        // ⚠ 顺序有意义：开侧上限要钳到"不小于枪身侧"，所以先设枪身侧
        params.setMaxAngleDegrees(info.maxAngle)
        params.setMaxAngleFreeDegrees(info.maxAngleFree)
        params.limitStiffness = info.limitStiffness.coerceIn(0.0, MAX_LIMIT_STIFFNESS).toFloat()
        params.idleSway = Math.toRadians(info.idleSway).toFloat()
        params.smoothing = info.smoothing.coerceAtLeast(0.0).toFloat()

        val lateral = info.lateralAngle.coerceIn(-89.0, 89.0)
        params.lateralLimitSin = if (lateral == 0.0) 0f else sin(Math.toRadians(lateral)).toFloat()
        // 侧别跟着 [lateralLimitSin] 的符号走：0 = 没声明，开侧上限不生效（见 `CharmSolver.swingLimit`）
        params.blockedAxisSign = when {
            params.lateralLimitSin > 0f -> 1f
            params.lateralLimitSin < 0f -> -1f
            else -> 0f
        }

        // 挂件的内部锥形限位（枪身参考系，全方位）：0 = 关闭；非 0 钳到 [1°, 179°] ——
        // 0° 会把挂件钉死在绑定姿态上、没有意义，所以把 0 留给"关闭"
        val bodyLimit = info.bodyMaxAngle.coerceIn(0.0, 179.0)
        params.bodyMaxAngle = if (bodyLimit < 1.0) 0f else Math.toRadians(bodyLimit).toFloat()

        state.baseResponse = info.response.coerceIn(0.0, 1.0).toFloat()
        state.baseTurnResponse = info.turnResponse.coerceIn(0.0, 1.0).toFloat()
        state.aimResponse = info.aimResponseScale.coerceIn(0.0, 1.0).toFloat()
        state.params.response = state.baseResponse
        state.params.turnResponse = state.baseTurnResponse

        // 参数变了（尤其是摆长）就重新归位，免得旧速度配上新摆长直接甩飞
        state.solver.invalidate()
        state.accumulator = 0f
        state.hasAnchor = false
    }

    /** 模型局部 → 视图空间 的逆矩阵，复用它避免每帧分配 */
    private val MODEL_FROM_VIEW = Matrix4f()

    /** [clampBodyCone] 的草稿四元数，同上，避免每个子步都新建一个 */
    private val CONE_ROTATION = Quaternionf()

    /** 固定物理步长（秒）。30Hz 对一两赫兹的摆锤绰绰有余 */
    private const val SIM_STEP = 1f / 30f

    /** 单帧时长下限/上限（秒）：卡顿或调试停帧时不让一步跨太大 */
    private const val MIN_FRAME_DT = 1f / 240f
    private const val MAX_FRAME_DT = 0.1f

    /** 累加器上限（秒）：一次卡顿最多补三步，不做死亡螺旋 */
    private const val MAX_ACCUMULATOR = 0.1f

    /** 单帧最多跑几步 */
    private const val MAX_STEPS = 3

    /** 相机线加速度上限（方块/秒²，约 5g）：只用来挡住传送这类异常的帧间跳变 */
    private const val MAX_CAM_ACCEL = 50f

    /**
     * 软限位强度上限。再大，壁面附近的等效频率 `ω_eff ≈ sqrt(g/L · 2k/上限)` 就逼近
     * Verlet 的稳定边界（`ω·dt < 2`），30Hz 下摆动会开始发颤。
     */
    private const val MAX_LIMIT_STIFFNESS = 8.0

    /** 两帧间隔超过它就重新归位（纳秒） */
    private const val RESET_GAP_NANOS = 250_000_000L
}
