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
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

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
     * 生成当前帧的步态（角度单位：度）。
     * 另有三类特殊键：root.x / root.y 重心位移、"&lt;boneId&gt;.len" 沿骨轴缩放、
     * "depth.l" / "depth.r" 手臂前后深度（供渲染层做近实远虚）。
     *
     * 依据参考视频《人体步态·第一课》的四条规律：
     *  ①「左腿迈出，右臂就向前」—— 左腿 ↔ 右臂同相、右腿 ↔ 左臂同相（对侧同步交替）；
     *  ②「腿带着骨盆转，肩膀就反着转」—— 骨盆与肩带反向扭转，抵消迈步带来的扭矩；
     *  ③「固定双臂反而更费力」—— 摆臂由躯干带动、是放松的被动摆动，不是两臂一起开合；
     *  ④「放松肩膀，让手臂自然摆动」—— 幅度克制、略带滞后，前臂与手随动。
     *
     * 实现要点：
     *  - 统一相位 φ = 2π·frame / 24，全身共用一个时钟，四肢相位关系才严格成立；
     *    设计稿的 4 条轨道与菱形帧位保留不变，其峰值角度作为幅度系数（仍可编辑）。
     *  - 所有分量都由 sinφ / sin2φ / (1-cos2φ)/2 构成，在 φ=0 与 φ=π（第 0、12 帧）
     *    恒为 0 —— 打开 app 仍是设计稿 rest 原样，循环首尾也无缝。
     *  - **手臂在矢状面（前后）摆动**，而设计稿骨架是纯正面 2D、没有深度轴：
     *    绕肩旋转在屏幕上只会产生横向位移，无论怎么配相位看起来都是「左右甩臂」。
     *    因此手臂改用三个正面投影线索表达前后：前摆侧屈肘让手收向中线并抬高、
     *    沿骨轴透视缩短（".len" 键）、渲染层按 depth 做近实远虚；上臂横向旋转
     *    只保留 7° 的贴身/外展辅助，不再是主运动。
     *  - 腿仍在正面可见的平面内运动，左右镜像下**同号旋转才是反相摆动**（异号会变成
     *    开合跳）；摆动腿的大腿与小腿角度反向配比，让脚留在原位并离地，支撑腿保持伸直，
     *    配合重心侧移与上下起伏，看起来才是迈步而不是原地抬腿。
     */
    fun animatedAt(f: Float = frame): Map<String, Float> {
        val fn = ((f % FRAME_COUNT) + FRAME_COUNT) % FRAME_COUNT
        val phi = 2f * PI.toFloat() * fn / FRAME_COUNT.toFloat()
        val s = sin(phi)
        val s2 = sin(2f * phi)
        val hump = (1f - cos(2f * phi)) * 0.5f   // 双支撑期(0/12帧)为 0，单支撑期最高

        /** 轨道峰值 → 幅度系数：用户在时间轴改关键帧角度，整体幅度会跟着变 */
        fun peak(id: String, fallback: Float): Float {
            val keys = tracks.firstOrNull { it.boneId == id }?.keys ?: return fallback
            return keys.maxOf { abs(it.angle) }.takeIf { it > 0.01f } ?: fallback
        }
        val legK = peak("rThigh", 8f) / 8f
        val armK = peak("lArmU", 12f) / 12f
        val hipK = peak("pelvis", 1.8f) / 1.8f

        // 摆动相：sinφ 决定哪条腿在前；sin2φ 让「蹬地 → 前摆 → 落地」节奏不对称，
        // 屈膝峰值比抬腿峰值稍晚，形成先抬大腿、再收小腿的折叠感。
        val swingL = maxOf(0f, s + 0.30f * s2)
        val swingR = maxOf(0f, -s - 0.30f * s2)
        val kneeL = maxOf(0f, s - 0.28f * s2)
        val kneeR = maxOf(0f, -s + 0.28f * s2)

        val out = HashMap<String, Float>()

        // ① 腿：摆动腿抬大腿 + 屈膝，两者角度按 12° : 27° 配比，
        //    使脚在正面视角下保持原位并明显离地（比例失衡会让脚越过中线交叉，或看不出抬腿）；
        //    支撑腿伸直，仅极轻微承重屈膝。
        val thighL = 12f * legK * swingL
        val thighR = -12f * legK * swingR
        val shinL = -27f * legK * kneeL - 1.4f * legK * hump
        val shinR = 27f * legK * kneeR + 1.4f * legK * hump
        out["lThigh"] = thighL; out["rThigh"] = thighR
        out["lShin"] = shinL;   out["rShin"] = shinR
        // 脚踝补偿 + 摆动相轻微翻转，表现脚跟离地 / 落脚
        out["lFoot"] = -(thighL + shinL) * 0.35f + 6f * legK * swingL
        out["rFoot"] = -(thighR + shinR) * 0.35f + 6f * legK * swingR

        // ② 手臂：真人走路是在矢状面「前后」摆。φ>0（左腿前迈）→ 右臂前摆 d>0；
        //    d<0 则左臂前摆，两侧严格交替。深度键交给渲染层做「近实远虚」。
        val d = (armK * (s + 0.16f * s2)).coerceIn(-1f, 1f)
        val frontR = maxOf(0f, d)
        val frontL = maxOf(0f, -d)
        val ad = abs(d)
        out["depth.r"] = d
        out["depth.l"] = -d
        // 上臂仅 7°：前摆侧贴身内收、后摆侧微外展，只作辅助而非主运动
        out["lArmU"] = 7f * d
        out["rArmU"] = 7f * d
        // 屈肘：前摆侧 20° 把前臂折向身体中线、手抬到腰腹前；后摆侧反向 5° 微展、
        // 接近伸直。镜像几何下异号 = 两侧都「向中线收」，但只有一侧在屈，不会开合跳。
        val elbowL = -20f * frontL + 5f * frontR
        val elbowR = 20f * frontR - 5f * frontL
        out["lArmL"] = elbowL
        out["rArmL"] = elbowR
        out["lHand"] = -0.3f * elbowL           // 手掌略滞后，保持松弛
        out["rHand"] = -0.3f * elbowR
        out["lClav"] = 1.0f * hipK * s         // 肩带随胸廓反向扭转
        out["rClav"] = 1.0f * hipK * s
        // 沿骨轴透视缩短：前后摆都会略短，前摆侧因屈肘再缩短、肩点前送。
        // 缩放绕各骨骼 pivot 进行，子骨骼继承父变换，肘/腕关节不会断开。
        val upper = 1f - 0.06f * ad
        for ((side, ds, front) in listOf(
            Triple("l", -d, frontL), Triple("r", d, frontR)
        )) {
            val fore = upper - 0.14f * front
            out["${side}Clav.len"] = 1f - 0.06f * ds
            out["${side}ArmU.len"] = upper
            out["${side}ArmL.len"] = fore
            out["${side}Hand.len"] = fore
        }

        // ③ 骨盆随腿转，肩带反向转，头回正保持视线稳定
        out["pelvis"] = -1.3f * hipK * s
        out["spine"] = 2.6f * hipK * s         // 叠加后胸廓 ≈ +1.3°，与骨盆反相
        out["chest"] = 0f
        out["neck"] = 0f
        out["head"] = -1.3f * hipK * s

        // ④ 重心：向支撑侧平移（φ>0 时右腿支撑 → 右移），单支撑期略升高
        out["root.x"] = 2.6f * hipK * s
        out["root.y"] = -3.0f * hipK * hump

        // 默认 4 条轨道的峰值已作为上面的幅度系数；用户在时间轴上给其它骨骼
        // 新增 / 拖动的关键帧仍按采样值叠加，保证关键帧编辑对任意骨骼都有实际效果。
        val clipDriven = setOf("lArmU", "lArmL", "pelvis", "rThigh")
        for (t in tracks) {
            if (t.boneId in clipDriven) continue
            out[t.boneId] = (out[t.boneId] ?: 0f) + sampleTrack(t.keys, f)
        }
        return out
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
