package com.example.humanskeleton.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import com.example.humanskeleton.art.SkeletonArt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

enum class NavDest { ASSEMBLE, ANIMATE, PARTS, WEIGHTS, SETTINGS }
enum class Tool { SELECT, BONE, BIND, IK }

/** 可撤销的编辑快照（不含纯视图状态） */
private data class Snapshot(
    val bones: List<BoneSpec>,
    val pose: Map<String, BonePose>,
    val tracks: List<Track>,
    val binds: Map<String, String>,
    val bindNames: Map<String, String>,
    val bindImages: Map<String, ImageBitmap>,
    val labels: Map<String, String>,
    val ikTargets: Set<String>
)

class EditorState {
    // ---- 视图状态 ----
    var frame by mutableFloatStateOf(12f)
    var playing by mutableStateOf(false)
    var loop by mutableStateOf(true)   // 点播放即持续循环走路，避免 24 帧播完 0.8 秒就停住
    var recording by mutableStateOf(true)
    var onionSkin by mutableStateOf(true)
    var showGrid by mutableStateOf(true)
    var showArtwork by mutableStateOf(true)
    var showRig by mutableStateOf(true)
    var locked by mutableStateOf(false)
    var darkCanvas by mutableStateOf(false)
    var partsView by mutableStateOf(false)
    var preview by mutableStateOf(false)
    var selected by mutableStateOf<String?>("lArmU")
    var tool by mutableStateOf(Tool.SELECT)
    var nav by mutableStateOf(NavDest.ASSEMBLE)
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    var cursor by mutableStateOf(Offset(148f, 302f))
    var snackbar by mutableStateOf<String?>(null)
    var canvasSize by mutableStateOf(androidx.compose.ui.unit.IntSize.Zero)
    // 时间轴拖动关键帧时的临时状态
    var dragBone by mutableStateOf<String?>(null)
    var dragOrigin by mutableStateOf<Int?>(null)
    var dragTarget by mutableStateOf<Int?>(null)
    var longPressMenu by mutableStateOf<Pair<String, Offset>?>(null)
    var renameTarget by mutableStateOf<String?>(null)

    /** 由当前状态实时计算 FK 变换（手势与绘制共用同一份结果） */
    fun currentTransforms(): Map<String, Mx> =
        computePartTransforms(pose, animatedAt(), bones)

    /** SVG 坐标 → 屏幕像素的视图矩阵 */
    fun viewMatrix(): Mx {
        val s = baseScale(canvasSize) * zoom
        val cx = canvasSize.width / 2f + pan.x
        val cy = canvasSize.height / 2f + pan.y
        return Mx(s, 0f, 0f, s, cx - s * SkeletonArt.VB_W / 2f, cy - s * SkeletonArt.VB_H / 2f)
    }

    // ---- 可编辑数据 ----
    val bones = mutableStateListOf<BoneSpec>().also { it.addAll(SkeletonDef.bones) }
    // 初始为设计稿 rest 姿态：不施加任何手工偏移，保证打开即与设计稿骨架完全一致
    var pose by mutableStateOf(emptyMap<String, BonePose>())
    var tracks by mutableStateOf(DefaultClip.tracks)
    val binds = mutableStateMapOf<String, String>()          // boneId -> uri
    val bindNames = mutableStateMapOf<String, String>()      // boneId -> 显示名
    val bindImages = mutableStateMapOf<String, ImageBitmap>()
    val labels = mutableStateMapOf<String, String>()
    val ikTargets = mutableStateListOf<String>()

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var lastCommit = 0L

    val canUndo get() = undoStack.isNotEmpty()
    val canRedo get() = redoStack.isNotEmpty()

    private fun snapshot() = Snapshot(
        bones.toList(), pose, tracks, binds.toMap(), bindNames.toMap(),
        bindImages.toMap(), labels.toMap(), ikTargets.toSet()
    )

    private fun restore(s: Snapshot) {
        bones.clear(); bones.addAll(s.bones)
        pose = s.pose; tracks = s.tracks
        binds.clear(); binds.putAll(s.binds)
        bindNames.clear(); bindNames.putAll(s.bindNames)
        bindImages.clear(); bindImages.putAll(s.bindImages)
        labels.clear(); labels.putAll(s.labels)
        ikTargets.clear(); ikTargets.addAll(s.ikTargets)
    }

    /** 每次离散编辑动作前记录一次；连续拖动用节流避免刷爆栈 */
    fun commit(throttle: Boolean = false) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (throttle && now - lastCommit < 320L) return
        lastCommit = now
        undoStack.addLast(snapshot())
        if (undoStack.size > 80) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(snapshot())
        restore(undoStack.removeLast())
        if (selected != null && !bones.any { it.id == selected }) selected = null
        snackbar = "已撤销"
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(snapshot())
        restore(redoStack.removeLast())
        snackbar = "已重做"
    }

    // ---- 骨骼属性 ----
    fun bone(id: String?): BoneSpec? = id?.let { bones.firstOrNull { it.id == id } }
    fun labelOf(b: BoneSpec): String = labels[b.id] ?: b.label

    fun poseOf(id: String): BonePose = pose[id] ?: BonePose()
    fun setPose(id: String, change: BonePose.() -> BonePose, throttled: Boolean = false) {
        commit(throttled)
        val cur = poseOf(id)
        pose = pose + (id to cur.change())
    }

    fun rotationOf(id: String): Float = poseOf(id).rotationDeg
    fun setRotation(id: String, deg: Float) = setPose(id, { copy(rotationDeg = deg) }, true)
    fun setX(id: String, v: Float) = setPose(id, { copy(x = v) }, true)
    fun setY(id: String, v: Float) = setPose(id, { copy(y = v) }, true)
    fun lengthOf(id: String): Float = poseOf(id).length.takeIf { it > 0f } ?: bone(id)?.restLength ?: 86f
    fun setLength(id: String, v: Float) = setPose(id, { copy(length = v) }, true)

    fun resetSelected() {
        val id = selected ?: return
        commit()
        pose = pose - id
        snackbar = "已重置「${bone(id)?.let { labelOf(it) } ?: id}」"
    }

    /** 属性面板「镜像」：把左右两侧骨骼的姿态对调并取反 */
    fun mirrorSelected() {
        val id = selected ?: return
        val counterpart = when {
            id.startsWith("l") -> "r" + id.removePrefix("l")
            id.startsWith("r") -> "l" + id.removePrefix("r")
            else -> null
        } ?: run { snackbar = "该骨骼没有镜像对象"; return }
        val a = poseOf(id); val b = poseOf(counterpart)
        commit()
        pose = pose + (counterpart to a.copy(rotationDeg = -a.rotationDeg, x = -a.x)) +
                (id to b.copy(rotationDeg = -b.rotationDeg, x = -b.x))
        snackbar = "已镜像 ${labelOf(bones.first { it.id == id })}"
    }

    fun rename(id: String, newName: String) {
        commit()
        labels[id] = newName.trim().ifEmpty { bone(id)?.label ?: id }
        snackbar = "已重命名为「${labels[id]}」"
    }

    // ---- 关键帧 ----
    /**
     * 采样当前帧的动画角度（单位：度）。
     * 设计稿时间轴只列出 4 条轨道（左上臂 / 左前臂 / 骨盆 / 右大腿），
     * 对侧四肢在这里自动反相，形成左右交替的 24 帧走路循环；轨道 UI 不增加。
     * computePartTransforms 会统一把这里的度数转成弧度。
     */
    fun animatedAt(f: Float = frame): Map<String, Float> {
        val base = tracks.associate { it.boneId to sampleTrack(it.keys, f) }.toMutableMap()
        val armL = base["lArmU"] ?: 0f
        val forearmL = base["lArmL"] ?: 0f
        // 「右大腿」轨道的采样值作为步态相位（0 / +8 / 0 / -8）。
        // 设计稿骨架是正面视角：左右腿简单反相会在同一帧「同时外张 / 同时内合」，像开合跳。
        // 用半波整流让两条腿交替抬起——每帧只有一条腿屈膝外摆，另一条直立支撑。
        val phase = base["rThigh"] ?: 0f
        val leftLift = maxOf(0f, -phase)
        val rightLift = maxOf(0f, phase)
        val thighL = leftLift * 1.3f
        val thighR = -rightLift * 1.3f
        val shinL = -leftLift * 2.2f      // 抬起侧屈膝，小腿往回收
        val shinR = rightLift * 2.2f
        base["rArmU"] = -armL             // 手臂与对侧腿协调，连续小幅摆动
        base["rArmL"] = -forearmL
        base["lThigh"] = thighL
        base["rThigh"] = thighR
        base["lShin"] = shinL
        base["rShin"] = shinR
        base["lFoot"] = -(thighL + shinL) * 0.35f
        base["rFoot"] = -(thighR + shinR) * 0.35f
        return base
    }

    fun keyframesOf(id: String): List<Keyframe> = tracks.firstOrNull { it.boneId == id }?.keys ?: emptyList()

    /** 时间轴「+关键帧」/ 双击轨道 / 录制时写入当前帧 */
    fun insertKeyframe(id: String, at: Int = frame.toInt(), angle: Float? = null, showTip: Boolean = true) {
        commit()
        val f = at.coerceIn(0, FRAME_COUNT)
        val value = angle ?: (poseOf(id).rotationDeg + (animatedAt(f.toFloat())[id] ?: 0f))
        val list = tracks.toMutableList()
        val idx = list.indexOfFirst { it.boneId == id }
        if (idx < 0) {
            list.add(Track(id, listOf(Keyframe(f, value))))
        } else {
            val keys = list[idx].keys.filterNot { it.frame == f }.toMutableList()
            keys.add(Keyframe(f, value))
            list[idx] = list[idx].copy(keys = keys.sortedBy { it.frame })
        }
        tracks = list
        if (showTip) snackbar = "已在第 $f 帧插入关键帧"
    }

    fun removeKeyframe(id: String, f: Int) {
        commit()
        tracks = tracks.map { if (it.boneId == id) it.copy(keys = it.keys.filterNot { k -> k.frame == f }) else it }
                .filter { it.keys.isNotEmpty() }
    }

    fun moveKeyframe(id: String, from: Int, to: Int) {
        if (from == to) return
        commit()
        tracks = tracks.map { t ->
            if (t.boneId != id) t
            else t.copy(keys = t.keys.filterNot { it.frame == to }
                .map { if (it.frame == from) it.copy(frame = to) else it }.sortedBy { it.frame })
        }
    }

    fun cycleEasing(id: String, f: Int) {
        commit()
        tracks = tracks.map { t ->
            if (t.boneId != id) t else t.copy(keys = t.keys.map { k ->
                if (k.frame != f) k else k.copy(
                    easing = when (k.easing) {
                        Easing.LINEAR -> Easing.EASE
                        Easing.EASE -> Easing.HOLD
                        Easing.HOLD -> Easing.LINEAR
                    }
                )
            })
        }
    }

    fun seek(f: Float) {
        playing = false
        frame = f.coerceIn(0f, FRAME_COUNT.toFloat())
    }

    fun stepFrame(delta: Int) = seek((frame.toInt() + delta).coerceIn(0, FRAME_COUNT).toFloat())

    /** 轨道列表：设计稿展示「有关键帧的骨骼」，新增关键帧自动多一行 */
    fun visibleTracks(): List<Track> {
        val listed = tracks.filter { it.keys.isNotEmpty() }.sortedBy { t ->
            DefaultClip.tracks.indexOfFirst { it.boneId == t.boneId }.let { if (it < 0) 99 else it }
        }
        return listed
    }

    // ---- 绑定部件 ----
    fun bind(id: String, uri: String, name: String, image: ImageBitmap?) {
        commit()
        binds[id] = uri; bindNames[id] = name
        if (image != null) bindImages[id] = image else bindImages.remove(id)
        snackbar = "已将「${bone(id)?.let { labelOf(it) } ?: id}」绑定到 $name"
    }

    fun unbind(id: String) {
        commit()
        binds.remove(id); bindNames.remove(id); bindImages.remove(id)
        snackbar = "已解绑「${bone(id)?.let { labelOf(it) } ?: id}」"
    }

    // ---- IK ----
    fun toggleIK(id: String) {
        commit()
        if (ikTargets.contains(id)) {
            ikTargets.remove(id); snackbar = "已取消 IK 目标"
        } else {
            ikTargets.add(id); tool = Tool.IK; snackbar = "已设为 IK 目标"
        }
    }

    /** 拖 IK 目标：取该骨骼与其父骨骼组成两段链，解析求解写回 pose */
    fun dragIK(id: String, targetSvg: Offset) {
        val lower = bone(id) ?: return
        val upper = bone(lower.parent) ?: return
        val parentM = computePartTransforms(pose, animatedAt())[upper.parent] ?: Mx.Identity
        val sol = solveTwoBoneIK(upper, lower, targetSvg, parentM) ?: return
        val r1 = sol.first / DEG2RAD
        val r2 = sol.second / DEG2RAD
        setPose(upper.id, { copy(rotationDeg = r1) }, true)
        setPose(lower.id, { copy(rotationDeg = r2) }, true)
    }

    // ---- 新建骨骼（rail FAB / 「新建骨骼」按钮） ----
    fun addBone(parentId: String?, svgPoint: Offset) {
        val parent = bone(parentId) ?: bones.first()
        val id = "bone_${(bones.size + 1).toString().padStart(2, '0')}"
        val end = Offset(svgPoint.x, svgPoint.y)
        val len = hypot(end.x - parent.end.x, end.y - parent.end.y)
        val pivot = parent.end
        val newBone = BoneSpec(
            id, parent.id, "新骨骼 ${bones.size - SkeletonDef.bones.size + 1}",
            pivot, if (len < 8f) Offset(pivot.x, pivot.y - 40f) else end,
            BoneGroup.ARM
        )
        commit()
        bones.add(newBone)
        selected = newBone.id
        snackbar = "已新建骨骼：${newBone.label}"
    }

    fun deleteBone(id: String) {
        if (id in listOf("root", "pelvis", "spine")) { snackbar = "根骨骼不可删除"; return }
        commit()
        bones.removeAll { it.id == id || it.parent == id }
        pose = pose - id; binds.remove(id); bindImages.remove(id); ikTargets.remove(id)
        selected = null
        snackbar = "已删除骨骼"
    }

    // ---- 导出 ----
    fun exportJson(): String {
        val sb = StringBuilder()
        sb.append("{\n  \"name\": \"走路循环\",\n  \"asset\": \"角色_骨架_v3\",\n")
        sb.append("  \"frameCount\": $FRAME_COUNT,\n  \"fps\": $FPS,\n  \"bones\": [\n")
        bones.forEachIndexed { i, b ->
            val p = poseOf(b.id)
            sb.append("    {\"id\":\"${b.id}\",\"parent\":${b.parent?.let { "\"$it\"" } ?: "null"},\"label\":\"${labelOf(b)}\",")
            sb.append("\"pivot\":[${b.pivot.x},${b.pivot.y}],\"end\":[${b.end.x},${b.end.y}],")
            sb.append("\"rotation\":${p.rotationDeg},\"x\":${p.x},\"y\":${p.y},\"length\":${lengthOf(b.id)}")
            sb.append(if (i == bones.lastIndex) "}\n" else "},\n")
        }
        sb.append("  ],\n  \"tracks\": [\n")
        tracks.forEachIndexed { ti, t ->
            sb.append("    {\"boneId\":\"${t.boneId}\",\"keys\":[")
            t.keys.sortedBy { it.frame }.forEachIndexed { ki, k ->
                if (ki > 0) sb.append(",")
                sb.append("{\"frame\":${k.frame},\"angle\":${k.angle},\"easing\":\"${k.easing.name}\"")
                sb.append(",\"endFrame\":${k.endFrame}}")
            }
            sb.append("]}")
            sb.append(if (ti == tracks.lastIndex) "\n" else ",\n")
        }
        sb.append("  ]\n}")
        return sb.toString()
    }

    /** 长按菜单的 IK / 解绑 / 重命名 三项共用入口 */
    fun applyLongPressAction(action: String, id: String, newName: String? = null) {
        when (action) {
            "ik" -> toggleIK(id)
            "unbind" -> unbind(id)
            "rename" -> rename(id, newName ?: "")
        }
    }

    /** 把屏幕点换算到设计稿 SVG 坐标 */
    fun screenToSvg(p: Offset, canvasSize: androidx.compose.ui.unit.IntSize, baseScale: Float): Offset? {
        val cx = canvasSize.width / 2f + pan.x
        val cy = canvasSize.height / 2f + pan.y
        val s = baseScale * zoom
        if (s <= 0f) return null
        return Offset(
            (p.x - cx) / s + SkeletonArt.VB_W / 2f,
            (p.y - cy) / s + SkeletonArt.VB_H / 2f
        )
    }

    fun svgToScreen(p: Offset, canvasSize: androidx.compose.ui.unit.IntSize, baseScale: Float): Offset {
        val cx = canvasSize.width / 2f + pan.x
        val cy = canvasSize.height / 2f + pan.y
        val s = baseScale * zoom
        return Offset(
            cx + (p.x - SkeletonArt.VB_W / 2f) * s,
            cy + (p.y - SkeletonArt.VB_H / 2f) * s
        )
    }

    fun baseScale(canvasSize: androidx.compose.ui.unit.IntSize): Float {
        val w = canvasSize.width.toFloat(); val h = canvasSize.height.toFloat()
        if (w <= 0f || h <= 0f) return 1f
        return (h * 0.92f / SkeletonArt.VB_H).coerceAtMost(w * 0.8f / SkeletonArt.VB_W)
    }
}

/** 角度归一化到 [-π, π] */
fun normAngle(a: Float): Float {
    var x = a % (2f * PI.toFloat())
    if (x > PI.toFloat()) x -= 2f * PI.toFloat()
    if (x < -PI.toFloat()) x += 2f * PI.toFloat()
    return x
}

/** 命中测试：点到骨骼段（SVG 坐标）的距离 */
fun pointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val vx = b.x - a.x; val vy = b.y - a.y
    val wx = p.x - a.x; val wy = p.y - a.y
    val l2 = vx * vx + vy * vy
    val t = if (l2 < 1e-6f) 0f else ((wx * vx + wy * vy) / l2).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * vx), p.y - (a.y + t * vy))
}

/** 由拖拽点求骨骼旋转增量（设计稿：单指拖动关节旋转骨骼） */
fun rotationFromDrag(b: BoneSpec, pivot: Offset, from: Offset, to: Offset): Float {
    val a0 = atan2(from.y - pivot.y, from.x - pivot.x)
    val a1 = atan2(to.y - pivot.y, to.x - pivot.x)
    return normAngle(a1 - a0)
}
