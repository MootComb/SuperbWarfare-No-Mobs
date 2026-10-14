package com.atsuishio.superbwarfare.client.animation.gun

import com.atsuishio.superbwarfare.Mod
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.GsonUtil
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.AnimationKeyframes
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationFile
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.BedrockAnimationPOJO
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.GsonHelper
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos

// 第一人称奔跑与行走摆动的程序性动画。关键帧读自 animations/bedrock/gun/gun_move_simple.animation.json
// （`sprint_simple` / `walk_simple`），采样规则与引擎（MAE 的 ArrayInterpolatableChannel + Vector3fCubicSplineInterpolator）逐字对齐。
object GunSprintAnimation {

    // 样张里驱动第一人称奔跑的那段循环片段
    private const val LOOP_CLIP = "sprint_simple"

    // 行走摆动：一条纯偏移片段（两端归零），叠在静止姿势上
    private const val WALK_CLIP = "walk_simple"

    // 进出奔跑的过渡片段，它们的曲线就是缓入缓出的曲线
    private const val IN_CLIP = "sprint_in_simple"
    private const val OUT_CLIP = "sprint_out_simple"

    // 手枪类型的枪（`GunType` = Handgun）淡出用另一支片段
    private const val HANDGUN_OUT_CLIP = "handgun_sprint_out_simple"

    // 过渡曲线的采样点数
    private const val CURVE_SAMPLES = 64

    // 样张不可用时的兜底循环时长（秒）
    private const val DEFAULT_LENGTH = 0.8f

    // 允许开火的窗口：姿势偏离归位不超过这个角度（度）
    const val FIRE_READY_ANGLE = 10f

    // 反算 [FIRE_READY_WEIGHT] 时的相位采样数
    private const val PHASES = 64

    private const val MEAN_SAMPLES = 128
    private const val BONE = "root"
    private const val SPLINE = "catmullrom"
    private const val DEG_TO_RAD = (PI / 180.0).toFloat()
    private const val RAD_TO_DEG = (180.0 / PI).toFloat()

    // 一整个循环的时长（秒）
    var LENGTH = DEFAULT_LENGTH
        private set

    // 行走样张一整个循环的时长（秒），行走相位的模数
    var WALK_LENGTH = DEFAULT_LENGTH
        private set

    // 循环接缝（第 0 帧）= 旋转的滞空姿势
    var BASE_ROTATION = Vector3f()
        private set

    // 整圈均值 = 去掉摆动后剩下的那副姿势，位移的滞空姿势停在它上面
    var BASE_POSITION = Vector3f()
        private set

    // 姿态收到这个权重以下才允许开火（由样张反算：整圈最偏的那一帧也偏不过 [FIRE_READY_ANGLE] 度）
    var FIRE_READY_WEIGHT = 1.0
        private set

    // 淡入的权重曲线（相位 → 权重）
    var IN_ROTATION = Curve.LINEAR_IN
        private set
    var IN_POSITION = Curve.LINEAR_IN
        private set

    private var rotationTrack: Track? = null
    private var positionTrack: Track? = null

    private var walkRotationTrack: Track? = null
    private var walkPositionTrack: Track? = null

    private var inRotationTrack: Track? = null
    private var inPositionTrack: Track? = null
    private var inLength = 0f

    // 淡出片段的两份：默认、手枪
    private var outDefault = OutClip(null)
    private var outHandgun = OutClip(null)

    // [animations] 传 null 表示样张不可用，姿势一律归零
    fun load(animations: Map<String, BedrockAnimationPOJO>?) {
        val clip = animations?.get(LOOP_CLIP)
        val bone = clip?.bones?.get(BONE)
        rotationTrack = bone?.rotation?.toTrack()
        positionTrack = bone?.position?.toTrack()

        val length = clip?.animationLength?.toFloat() ?: 0f
        val end = maxOf(rotationTrack?.end ?: 0f, positionTrack?.end ?: 0f)
        if (length > 0f) {
            LENGTH = length
        } else if (end > 0f) {
            LENGTH = end
        }

        BASE_ROTATION = Vector3f(rotationAt(0f, Vector3f()))
        BASE_POSITION = meanPosition()
        FIRE_READY_WEIGHT = fireReadyWeight()

        val walk = animations?.get(WALK_CLIP)
        walkRotationTrack = trackOf(walk, true)
        walkPositionTrack = trackOf(walk, false)
        // 行走相位拿它当模数，样张缺了也得是个正数
        WALK_LENGTH = lengthOf(walk, walkRotationTrack ?: walkPositionTrack).let { if (it > 0f) it else DEFAULT_LENGTH }

        val incoming = animations?.get(IN_CLIP)
        inRotationTrack = trackOf(incoming, true)
        inPositionTrack = trackOf(incoming, false)
        inLength = lengthOf(incoming, inRotationTrack ?: inPositionTrack)
        IN_ROTATION = curve(incoming, true, true)
        IN_POSITION = curve(incoming, false, true)
        outDefault = OutClip(animations?.get(OUT_CLIP))
        // 缺手枪片段（老样张 / 资源包换掉了样张）时退回默认那份，别让手枪连淡出姿势都没了
        outHandgun = OutClip(animations?.get(HANDGUN_OUT_CLIP) ?: animations?.get(OUT_CLIP))
    }

    // 淡出的权重曲线：[handgun] = 枪型是手枪，取样张里手枪那段淡出的曲线
    fun outRotation(handgun: Boolean): Curve = (if (handgun) outHandgun else outDefault).rotation

    fun outPosition(handgun: Boolean): Curve = (if (handgun) outHandgun else outDefault).position

    // 过渡片段自己的姿势：进/出期间直接用它，返回 false = 样张里没有这条通道，调用方该退回「循环姿势 × 权重」
    @JvmStatic
    fun transitionRotation(incoming: Boolean, handgun: Boolean, progress: Float, dest: Vector3f): Boolean {
        val out = if (handgun) outHandgun else outDefault
        val track = if (incoming) inRotationTrack else out.rotationTrack
        if (track == null) return false
        val length = if (incoming) inLength else out.length
        toInternal(track.sample(length * progress.coerceIn(0f, 1f), dest), dest)
        return true
    }

    @JvmStatic
    fun transitionPosition(incoming: Boolean, handgun: Boolean, progress: Float, dest: Vector3f): Boolean {
        val out = if (handgun) outHandgun else outDefault
        val track = if (incoming) inPositionTrack else out.positionTrack
        if (track == null) return false
        val length = if (incoming) inLength else out.length
        track.sample(length * progress.coerceIn(0f, 1f), dest)
        return true
    }

    private fun trackOf(clip: BedrockAnimationPOJO?, rotation: Boolean): Track? {
        val bone = clip?.bones?.get(BONE) ?: return null
        return (if (rotation) bone.rotation else bone.position)?.toTrack()
    }

    private fun lengthOf(clip: BedrockAnimationPOJO?, track: Track?): Float {
        val length = clip?.animationLength?.toFloat() ?: 0f
        if (length > 0f) return length
        return track?.end ?: 0f
    }

    // 把片段一个通道的向量长度归一化成权重曲线：参照取"归位"那一端（进片段在末尾、出片段在开头）
    private fun curve(clip: BedrockAnimationPOJO?, rotation: Boolean, incoming: Boolean): Curve {
        val fallback = if (incoming) Curve.LINEAR_IN else Curve.LINEAR_OUT
        val track = trackOf(clip, rotation) ?: return fallback
        val length = lengthOf(clip, track)
        if (length <= 0f) return fallback

        val dest = Vector3f()
        val knots = FloatArray(CURVE_SAMPLES + 1) {
            track.sample(length * it / CURVE_SAMPLES, dest).length()
        }
        val ref = knots[if (incoming) CURVE_SAMPLES else 0]
        if (ref <= 1e-4f) return fallback
        for (i in knots.indices) {
            knots[i] = (knots[i] / ref).coerceIn(0f, 1.2f)
        }
        return Curve(knots)
    }

    // 配 `rotateZYX(z, y, x)`；位移那侧不用再转，调用方的 `translate` 本来就带 X 取反
    fun rotationAt(t: Float, dest: Vector3f): Vector3f {
        val track = rotationTrack ?: return dest.set(0f, 0f, 0f)
        return toInternal(track.sample(t, dest), dest)
    }

    fun positionAt(t: Float, dest: Vector3f): Vector3f {
        val track = positionTrack ?: return dest.set(0f, 0f, 0f)
        return track.sample(t, dest)
    }

    fun walkRotationAt(t: Float, dest: Vector3f): Vector3f {
        val track = walkRotationTrack ?: return dest.set(0f, 0f, 0f)
        return toInternal(track.sample(t, dest), dest)
    }

    fun walkPositionAt(t: Float, dest: Vector3f): Vector3f {
        val track = walkPositionTrack ?: return dest.set(0f, 0f, 0f)
        return track.sample(t, dest)
    }

    private fun toInternal(channel: Vector3f, dest: Vector3f): Vector3f =
        dest.set(-channel.x * DEG_TO_RAD, -channel.y * DEG_TO_RAD, channel.z * DEG_TO_RAD)

    private fun meanPosition(): Vector3f {
        val dest = Vector3f()
        val sum = Vector3f()
        for (i in 0 until MEAN_SAMPLES) {
            sum.add(positionAt(LENGTH * i / MEAN_SAMPLES, dest))
        }
        return sum.div(MEAN_SAMPLES.toFloat())
    }

    // 二分：让整圈最偏的那一帧的姿势总转角正好收到 [FIRE_READY_ANGLE]
    private fun fireReadyWeight(): Double {
        if (rotationTrack == null) return 1.0
        var lo = 0.0
        var hi = 1.0
        repeat(60) {
            val mid = (lo + hi) * 0.5
            if (maxPoseAngle(mid) < FIRE_READY_ANGLE) lo = mid else hi = mid
        }
        return lo
    }

    // 权重缩到 [weight] 时整圈里姿势最偏的那一帧转了多少度（枪口偏差的上界）
    private fun maxPoseAngle(weight: Double): Float {
        val dest = Vector3f()
        var max = 0f
        for (i in 0 until PHASES) {
            val v = rotationAt(LENGTH * i / PHASES, dest)
            val q = Quaternionf().rotateZYX(
                (v.z * weight).toFloat(), (v.y * weight).toFloat(), (v.x * weight).toFloat()
            ).normalize()
            val angle = 2.0 * acos(abs(q.w).toDouble().coerceAtMost(1.0))
            max = maxOf(max, (angle * RAD_TO_DEG).toFloat())
        }
        return max
    }

    private fun AnimationKeyframes.toTrack(): Track? {
        val frames = keyframes
        if (frames == null || frames.isEmpty()) return null

        val size = frames.size
        val time = FloatArray(size)
        val pre = Array(size) { Vector3f() }
        val post = Array(size) { Vector3f() }
        val spline = BooleanArray(size)
        var i = 0
        for (entry in frames.double2ObjectEntrySet()) {
            val frame = entry.value
            // 与 BedrockAnimation.parseKeyframe 同序：`data`（数组简写）优先，其次 pre/post 互相补位
            val first = frame.data ?: frame.pre ?: frame.post ?: Vector3f()
            val second = frame.data ?: frame.post ?: frame.pre ?: Vector3f()
            time[i] = entry.doubleKey.toFloat()
            pre[i].set(first)
            post[i].set(second)
            spline[i] = frame.lerpMode == SPLINE
            i++
        }
        return Track(time, pre, post, spline)
    }

    // 淡出片段的一套东西：两条姿势轨道 + 时长 + 两条权重曲线（样张里缺这条片段时全退兜底）
    private class OutClip(clip: BedrockAnimationPOJO?) {
        val rotationTrack = trackOf(clip, true)
        val positionTrack = trackOf(clip, false)
        val length = lengthOf(clip, rotationTrack ?: positionTrack)
        val rotation = curve(clip, true, false)
        val position = curve(clip, false, false)
    }

    // 过渡曲线：相位 0..1 → 权重 0..1（样张的归一化向量长度，可以略微过冲）
    class Curve(private val knots: FloatArray) {

        // 走到 90% 的相位，用来把样张的时长换算成相位速率
        val span: Float = run {
            val from = knots[0]
            val travel = knots[knots.size - 1] - from
            var i = 1
            while (i < knots.size - 1 && abs(knots[i] - from) < 0.9f * abs(travel)) i++
            i.toFloat() / (knots.size - 1)
        }

        fun at(phase: Float): Float {
            if (phase <= 0f) return knots[0]
            if (phase >= 1f) return knots[knots.size - 1]
            val x = phase * (knots.size - 1)
            val i = x.toInt()
            return knots[i] + (knots[i + 1] - knots[i]) * (x - i)
        }

        // 曲线的尾巴不单调（样张末几帧的残留，0.00→0.01→0.02→0.00），乘在姿势上就是归位前多抖两下：钳成单调
        fun atMonotone(phase: Float, previous: Float): Float {
            val value = at(phase)
            return if (knots[knots.size - 1] >= knots[0]) maxOf(value, previous) else minOf(value, previous)
        }

        // 反查相位：进出中途掉头时用它接上当前权重
        fun phaseFor(value: Float): Float {
            val rising = knots[knots.size - 1] >= knots[0]
            for (i in 0 until knots.size - 1) {
                val a = knots[i]
                val b = knots[i + 1]
                if ((rising && a <= value && b >= value) || (!rising && a >= value && b <= value)) {
                    val f = if (b == a) 0f else (value - a) / (b - a)
                    return ((i + f) / (knots.size - 1)).coerceIn(0f, 1f)
                }
            }
            return if (rising) 1f else 0f
        }

        companion object {
            val LINEAR_IN = Curve(floatArrayOf(0f, 1f))
            val LINEAR_OUT = Curve(floatArrayOf(1f, 0f))
        }
    }

    // 一条通道：每个关键帧存 pre/post 与它自己的插值方式（catmullrom 的优先级压过 linear）
    private class Track(
        private val time: FloatArray,
        private val pre: Array<Vector3f>,
        private val post: Array<Vector3f>,
        private val spline: BooleanArray
    ) {
        val end: Float get() = if (time.isEmpty()) 0f else time[time.size - 1]

        fun sample(t: Float, dest: Vector3f): Vector3f {
            if (time.isEmpty()) return dest.set(0f, 0f, 0f)
            val last = time.size - 1
            if (t <= time[0]) return dest.set(if (spline[0]) pre[0] else post[0])
            if (t >= time[last]) return dest.set(if (spline[last]) pre[last] else post[last])

            var i = 0
            while (time[i + 1] < t) i++
            val j = i + 1
            val p = if (i == 0) 0 else i - 1
            val n = if (j == last) j else j + 1
            val f = (t - time[i]) / (time[j] - time[i])
            return if (spline[i] || spline[j]) {
                dest.set(
                    cubic(post[p].x, pre[i].x, pre[j].x, pre[n].x, f),
                    cubic(post[p].y, pre[i].y, pre[j].y, pre[n].y, f),
                    cubic(post[p].z, pre[i].z, pre[j].z, pre[n].z, f),
                )
            } else {
                dest.set(post[i]).lerp(pre[j], f)
            }
        }

        private fun cubic(p: Float, x: Float, y: Float, n: Float, f: Float): Float {
            val v0 = (y - p) * 0.5f
            val v1 = (n - x) * 0.5f
            val f2 = f * f
            val f3 = f * f2
            return (2f * f3 - 3f * f2 + 1f) * x +
                    (3f * f2 - 2f * f3) * y +
                    (f3 - 2f * f2 + f) * v0 +
                    (f3 - f2) * v1
        }
    }
}

// 样张的加载入口，注册在 [com.atsuishio.superbwarfare.resource.BedrockModelLoader]
object GunSprintAnimationLoader : ResourceManagerReloadListener {

    // 样张文件
    private val LOCATION = Mod.loc("animations/bedrock/gun/gun_move_simple.animation.json")

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        GunSprintAnimation.load(read(resourceManager))
    }

    private fun read(resourceManager: ResourceManager): Map<String, BedrockAnimationPOJO>? =
        try {
            resourceManager.getResource(LOCATION).orElseThrow().openAsReader().use {
                GsonHelper.fromJson(GsonUtil.CLIENT_GSON, it, BedrockAnimationFile::class.java)
            }.animations
        } catch (e: Exception) {
            Mod.LOGGER.error("Failed to load gun move animations from $LOCATION", e)
            null
        }
}
