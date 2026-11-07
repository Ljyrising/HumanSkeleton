package com.example.humanskeleton.model

import androidx.compose.ui.geometry.Offset
import com.example.humanskeleton.art.ArtGroup
import com.example.humanskeleton.art.SkeletonArt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

const val FRAME_COUNT = 24
const val FPS = 30

enum class BoneGroup { CORE, ARM, LEG }

/** 一根骨骼：pivot / end 均取自设计稿 RIG 骨骼链的关节点坐标（viewBox 300×560） */
data class BoneSpec(
    val id: String,
    val parent: String?,
    val label: String,
    val pivot: Offset,
    val end: Offset,
    val group: BoneGroup,
    val hasRigLine: Boolean = true
) {
    val restLength: Float get() = hypot(end.x - pivot.x, end.y - pivot.y)
    val restAngle: Float get() = atan2(end.y - pivot.y, end.x - pivot.x)
}

object SkeletonDef {
    private fun b(
        id: String, parent: String?, label: String,
        px: Float, py: Float, ex: Float, ey: Float,
        group: BoneGroup, line: Boolean = true
    ) = BoneSpec(id, parent, label, Offset(px, py), Offset(ex, ey), group, line)

    /** 层级与设计稿「骨骼层级」面板一致，关节坐标与 RIG 骨骼链一一对应 */
    val bones: List<BoneSpec> = listOf(
        b("root",   null,     "骨架 root", 150f, 300f, 150f, 300f, BoneGroup.CORE, false),
        b("pelvis", "root",   "骨盆",      150f, 300f, 150f, 300f, BoneGroup.CORE, false),
        b("spine",  "pelvis", "脊柱",      150f, 300f, 150f, 142f, BoneGroup.CORE),
        b("chest",  "spine",  "胸廓",      150f, 142f, 150f, 142f, BoneGroup.CORE, false),
        b("neck",   "spine",  "颈",        150f, 142f, 150f, 142f, BoneGroup.CORE, false),
        b("head",   "neck",   "头骨",      150f, 142f, 150f, 100f, BoneGroup.CORE),

        b("lClav",  "spine",  "锁骨 · 左", 150f, 142f, 107f, 145f, BoneGroup.ARM),
        b("lArmU",  "lClav",  "左上臂",    107f, 145f,  94f, 216f, BoneGroup.ARM),
        b("lArmL",  "lArmU",  "左前臂",     94f, 216f,  86f, 286f, BoneGroup.ARM),
        b("lHand",  "lArmL",  "左手",       86f, 286f,  81f, 316f, BoneGroup.ARM),

        b("rClav",  "spine",  "锁骨 · 右", 150f, 142f, 193f, 145f, BoneGroup.ARM),
        b("rArmU",  "rClav",  "右上臂",    193f, 145f, 206f, 216f, BoneGroup.ARM),
        b("rArmL",  "rArmU",  "右前臂",    206f, 216f, 214f, 286f, BoneGroup.ARM),
        b("rHand",  "rArmL",  "右手",      214f, 286f, 219f, 316f, BoneGroup.ARM),

        b("lHip",   "pelvis", "髋 · 左",   150f, 300f, 129f, 306f, BoneGroup.LEG, false),
        b("lThigh", "lHip",   "左大腿",    129f, 306f, 122f, 392f, BoneGroup.LEG),
        b("lShin",  "lThigh", "左小腿",    122f, 392f, 118f, 474f, BoneGroup.LEG),
        b("lFoot",  "lShin",  "左脚",      118f, 474f, 100f, 498f, BoneGroup.LEG),

        b("rHip",   "pelvis", "髋 · 右",   150f, 300f, 171f, 306f, BoneGroup.LEG, false),
        b("rThigh", "rHip",   "右大腿",    171f, 306f, 178f, 392f, BoneGroup.LEG),
        b("rShin",  "rThigh", "右小腿",    178f, 392f, 182f, 474f, BoneGroup.LEG),
        b("rFoot",  "rShin",  "右脚",      182f, 474f, 200f, 498f, BoneGroup.LEG)
    )

    val byId: Map<String, BoneSpec> = bones.associateBy { it.id }

    fun depth(id: String): Int {
        var d = 0
        var p = byId[id]?.parent
        while (p != null) { d++; p = byId[p]?.parent }
        return d
    }

    /** 设计稿部件分组 → 骨骼；一个组可拆成多段，分别绑定不同骨骼 */
    val segments: List<ArtSegment> = listOf(
        ArtSegment("ground-g", 0, 0, null),
        ArtSegment("b-head",   0, 4, "head"),
        ArtSegment("b-spine",  0, 3, "spine"),
        ArtSegment("b-ribs",   0, 9, "chest"),
        ArtSegment("b-clav",   0, 1, "lClav"),
        ArtSegment("b-clav",   2, 3, "rClav"),
        ArtSegment("b-armL",   0, 1, "lArmU"),
        ArtSegment("b-armL",   2, 3, "lArmL"),
        ArtSegment("b-armL",   4, 6, "lHand"),
        ArtSegment("b-armR",   0, 1, "rArmU"),
        ArtSegment("b-armR",   2, 3, "rArmL"),
        ArtSegment("b-armR",   4, 6, "rHand"),
        ArtSegment("b-pelvis", 0, 4, "pelvis"),
        ArtSegment("b-legL",   0, 1, "lThigh"),
        ArtSegment("b-legL",   2, 5, "lShin"),
        ArtSegment("b-legL",   6, 8, "lFoot"),
        ArtSegment("b-legR",   0, 1, "rThigh"),
        ArtSegment("b-legR",   2, 5, "rShin"),
        ArtSegment("b-legR",   6, 8, "rFoot")
    )

    /** 设计稿 b-joints 的 12 个关节圆点，按坐标跟随对应骨骼 */
    val jointBones: List<String> = listOf(
        "lClav", "rClav",       // 107,145 / 193,145
        "lArmL", "rArmL",       // 94,216 / 206,216
        "lHand", "rHand",       // 86,286 / 214,286
        "lThigh", "rThigh",     // 129,306 / 171,306
        "lShin", "rShin",       // 122,392 / 178,392
        "lFoot", "rFoot"        // 118,474 / 182,474
    )

    fun groupOf(gid: String): ArtGroup = SkeletonArt.groups.first { it.id == gid }
}

/** 部件分组内的一段绘制区间 */
data class ArtSegment(val groupId: String, val from: Int, val to: Int, val boneId: String?)

// ---------------- 关键帧动画 ----------------

enum class Easing { LINEAR, EASE, HOLD }

/** endFrame = 设计稿的末帧菱形（tertiary）；easing = 缓动菱形（白底描边） */
data class Keyframe(
    val frame: Int,
    val angle: Float,
    val easing: Easing = Easing.LINEAR,
    val endFrame: Boolean = false
)

data class Track(val boneId: String, val keys: List<Keyframe>)

object DefaultClip {
    /**
     * 走路循环：保持设计稿时间轴四条轨道与菱形帧位不变
     * （左上臂 0/8/16/24 · 左前臂 0/12/24 · 骨盆 0/4e/8/12e/16/20e · 右大腿 0/6/12/18）。
     * 角度改成 24 帧完整正弦周期：0 与 24 相等、12 帧回到 rest（打开 app 即设计稿原样）。
     * 对侧四肢由 EditorState.animatedAt() 自动反相，形成协调步态，轨道数量不增加。
     */
    val tracks: List<Track> = listOf(
        Track("lArmU", listOf(
            Keyframe(0, 0f), Keyframe(8, 12f), Keyframe(16, -12f), Keyframe(24, 0f, endFrame = true)
        )),
        Track("lArmL", listOf(
            Keyframe(0, 4f), Keyframe(12, 0f), Keyframe(24, 4f, endFrame = true)
        )),
        Track("pelvis", listOf(
            Keyframe(0, 0f), Keyframe(4, 1.8f, Easing.EASE), Keyframe(8, -1.8f),
            Keyframe(12, 0f, Easing.EASE), Keyframe(16, 1.8f), Keyframe(20, -1.8f, Easing.EASE)
        )),
        Track("rThigh", listOf(
            Keyframe(0, 0f), Keyframe(6, 8f), Keyframe(12, 0f), Keyframe(18, -8f)
        ))
    )
}

fun sampleTrack(keys: List<Keyframe>, frame: Float): Float {
    if (keys.isEmpty()) return 0f
    val f = ((frame % FRAME_COUNT) + FRAME_COUNT) % FRAME_COUNT
    val sorted = keys.sortedBy { it.frame }
    if (f <= sorted.first().frame) return sorted.first().angle
    val last = sorted.last()
    if (f >= last.frame) {
        // 末帧之后平滑插值回首帧（frame 24 ≡ 0），保证循环播放无缝。
        // 若直接返回末帧角度，18→24 会一直卡住、到 24 帧瞬间跳回首帧，播放时一顿一顿。
        val span = (FRAME_COUNT - last.frame + sorted.first().frame).coerceAtLeast(1)
        val t = ((f - last.frame) / span).coerceIn(0f, 1f)
        return last.angle + (sorted.first().angle - last.angle) * t
    }
    for (i in 0 until sorted.size - 1) {
        val a = sorted[i]; val b = sorted[i + 1]
        if (f >= a.frame && f <= b.frame) {
            val span = (b.frame - a.frame).coerceAtLeast(1)
            var t = (f - a.frame) / span
            t = when (b.easing) {
                Easing.HOLD -> 0f
                Easing.EASE -> t * t * (3f - 2f * t)
                Easing.LINEAR -> t
            }
            return a.angle + (b.angle - a.angle) * t
        }
    }
    return last.angle
}

// ---------------- FK 变换 ----------------

/** 用户手动编辑值（属性面板） */
data class BonePose(
    val rotationDeg: Float = 0f,
    val x: Float = 0f,
    val y: Float = 0f,
    val length: Float = 0f,      // 0 = 使用 rest 长度
    val easingName: String = "Ease",
    val order: Int = 3
)

const val DEG2RAD = (PI / 180.0).toFloat()

/** 沿骨轴方向 (pivot→end) 缩放 s，垂直方向保持不变 */
fun lengthMatrix(pivot: Offset, restAngle: Float, s: Float): Mx {
    if (abs(s - 1f) < 0.0001f) return Mx.Identity
    val r = restAngle + PI.toFloat() / 2f
    return Mx.Identity
        .translate(pivot.x, pivot.y)
        .rotate(r)
        .scale(1f, s)
        .rotate(-r)
        .translate(-pivot.x, -pivot.y)
}

/** 绕 pivot 旋转 rad */
fun rotationAbout(pivot: Offset, rad: Float): Mx =
    Mx.Identity.translate(pivot.x, pivot.y).rotate(rad).translate(-pivot.x, -pivot.y)

/**
 * 计算每根骨骼在「设计稿 SVG 坐标系」中的变换。
 * 父变换自动带动子关节，实现与 RIG 骨骼链一致的前向动力学。
 */
fun computePartTransforms(
    pose: Map<String, BonePose>,
    animated: Map<String, Float>,
    bones: List<BoneSpec> = SkeletonDef.bones
): Map<String, Mx> {
    val out = LinkedHashMap<String, Mx>()
    for (b in bones) {
        val parentM = b.parent?.let { out[it] } ?: Mx.Identity
        val p = pose[b.id]
        // animated（关键帧采样）与 pose.rotationDeg 都是「度」，必须整体转弧度。
        // 之前只把 pose 转了、animated 直接当弧度用：14° 变成 14rad≈802°，播放时疯狂打转。
        val rad = ((animated[b.id] ?: 0f) + (p?.rotationDeg ?: 0f)) * DEG2RAD
        // 只有用户显式设置了「长度」且该骨骼本身有长度时才缩放。
        // root / pelvis / chest / neck 是 pivot==end 的结构节点，restLength=0；
        // 若按 len/restLength 计算会得到 0→coerce 到 0.25，沿父链层层叠加
        // 会把整副骨架在水平方向压扁成一条竖线（head 仅剩 0.0156 倍）。
        val rest = b.restLength
        val s = if (p != null && p.length > 0f && rest >= 1f)
            (p.length / rest).coerceIn(0.25f, 3f)
        else 1f
        val local = rotationAbout(b.pivot, rad).concat(lengthMatrix(b.pivot, b.restAngle, s))
        out[b.id] = parentM
            .translate(p?.x ?: 0f, p?.y ?: 0f)
            .concat(local)
    }
    return out
}

/** 骨骼链端点经变换后的 SVG 坐标（用于 rig 线、锚点、命中测试） */
fun rigSegment(b: BoneSpec, t: Map<String, Mx>): Pair<Offset, Offset>? {
    val m = t[b.id] ?: return null
    return m.map(b.pivot.x, b.pivot.y) to m.map(b.end.x, b.end.y)
}

/** 两段解析 IK：返回 (上段角度增量, 下段相对上段的附加角度)，单位弧度 */
fun solveTwoBoneIK(
    upper: BoneSpec, lower: BoneSpec,
    target: Offset,
    parentM: Mx = Mx.Identity
): Pair<Float, Float>? {
    val inv = parentM.invert() ?: return null
    val p0 = inv.map(target.x, target.y)
    val a = upper.restLength
    val b = lower.restLength
    if (a < 1f || b < 1f) return null
    val dx = p0.x - upper.pivot.x
    val dy = p0.y - upper.pivot.y
    val d = hypot(dx, dy).coerceIn(abs(a - b) + 0.01f, a + b - 0.01f)
    val base = atan2(dy, dx)
    val cosA = ((a * a + d * d - b * b) / (2f * a * d)).coerceIn(-1f, 1f)
    val cosB = ((a * a + b * b - d * d) / (2f * a * b)).coerceIn(-1f, 1f)
    val alpha = kotlin.math.acos(cosA)
    val bend = PI.toFloat() - kotlin.math.acos(cosB)
    fun norm(v: Float): Float {
        var x = v % (2f * PI.toFloat())
        if (x > PI.toFloat()) x -= 2f * PI.toFloat()
        if (x < -PI.toFloat()) x += 2f * PI.toFloat()
        return x
    }
    val candidates = listOf(
        run {
            val t1 = base - alpha
            val t2 = t1 + bend
            val r1 = norm(t1 - upper.restAngle)
            norm(r1) to norm(t2 - lower.restAngle - r1)
        },
        run {
            val t1 = base + alpha
            val t2 = t1 - bend
            val r1 = norm(t1 - upper.restAngle)
            norm(r1) to norm(t2 - lower.restAngle - r1)
        }
    )
    return candidates.minByOrNull { abs(it.first) + abs(it.second) }
}
