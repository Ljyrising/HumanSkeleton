package com.example.humanskeleton.editor

import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.humanskeleton.art.ArtCmd
import com.example.humanskeleton.art.ArtStyle
import com.example.humanskeleton.art.SkeletonArt
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.Mx
import com.example.humanskeleton.model.SkeletonDef
import com.example.humanskeleton.model.Tool
import com.example.humanskeleton.model.rigSegment
import com.example.humanskeleton.ui.Spec

/** Mx → android.graphics.Matrix（3×3 行主序） */
fun Mx.toAndroid(): Matrix = Matrix().apply {
    setValues(floatArrayOf(a, c, e, b, d, f, 0f, 0f, 1f))
}

private data class StylePair(val fillPaint: Paint, val strokePaint: Paint)
private val paintCache = HashMap<String, StylePair>()

/** 按设计稿的浅色 / 深色画布 / 部件归属着色三种变体解析最终颜色 */
private fun resolveColors(s: ArtStyle, groupId: String, dark: Boolean, parts: Boolean): Triple<Int, Int, Float> {
    var stroke = s.strokeColor.toInt()
    var fill = s.fillColor.toInt()
    var alpha = s.alpha
    if (parts) {
        when (s.role) {
            "tube", "thin" -> SkeletonArt.partTint[groupId]?.let { stroke = it.toInt() }
            "tube-in", "thin-in" -> if (groupId.startsWith("b-arm")) fill = 0xFFF7F3EC.toInt()
            "skull", "socket" -> { stroke = 0xFF7D5260.toInt(); fill = 0xFF7D5260.toInt() }
        }
        if (s.role == "thin" && groupId == "b-ribs") alpha = 0.75f
    }
    if (dark) {
        when (s.role) {
            "tube", "thin", "skull-d", "socket", "joint" ->
                SkeletonArt.darkTint[s.role]?.let { stroke = it.toInt(); fill = it.toInt() }
            "tube-in", "thin-in" -> { stroke = SkeletonArt.darkInner.toInt(); fill = SkeletonArt.darkInner.toInt() }
            "skull" -> { stroke = 0xFF57525C.toInt(); fill = SkeletonArt.darkInner.toInt() }
        }
        if (s.role == "socket") alpha = 0.7f
        if (s.role == "joint") fill = SkeletonArt.darkInner.toInt()
    }
    return Triple(stroke, fill, alpha)
}

private fun makePaint(color: Int, width: Float, alpha: Float, fill: Boolean, dashed: Boolean = false) =
    Paint().apply {
        isAntiAlias = true
        style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        this.color = color
        this.alpha = (alpha * 255f).toInt().coerceIn(0, 255)
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (dashed) pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
    }

private fun stylePaints(s: ArtStyle, groupId: String, dark: Boolean, parts: Boolean): StylePair {
    val key = "${s.role}|${s.strokeColor}|${s.fillColor}|${s.width}|${s.alpha}|${s.filled}|${s.stroked}|$groupId|$dark|$parts"
    return paintCache.getOrPut(key) {
        val (stroke, fillColor, alpha) = resolveColors(s, groupId, dark, parts)
        StylePair(
            makePaint(fillColor, s.width, alpha, true),
            makePaint(stroke, s.width, alpha, false)
        )
    }
}

private fun DrawScope.native(block: (android.graphics.Canvas) -> Unit) {
    drawIntoCanvas { block(it.nativeCanvas) }
}

/** 绘制设计稿 SVG 的一条指令 */
private fun drawCmd(
    nc: android.graphics.Canvas, cmd: ArtCmd,
    groupId: String, dark: Boolean, parts: Boolean
) {
    val s = cmd.style
    val (fp, sp) = stylePaints(s, groupId, dark, parts)
    when (cmd) {
        is ArtCmd.PathData -> {
            if (s.filled) nc.drawPath(cmd.path, fp)
            if (s.stroked) nc.drawPath(cmd.path, sp)
        }
        is ArtCmd.Ellipse -> {
            val r = RectF(cmd.cx - cmd.rx, cmd.cy - cmd.ry, cmd.cx + cmd.rx, cmd.cy + cmd.ry)
            if (s.filled) nc.drawOval(r, fp)
            if (s.stroked) nc.drawOval(r, sp)
        }
        is ArtCmd.Circle -> {
            if (s.filled) nc.drawCircle(cmd.cx, cmd.cy, cmd.r, fp)
            if (s.stroked) nc.drawCircle(cmd.cx, cmd.cy, cmd.r, sp)
        }
        is ArtCmd.Line -> nc.drawLine(cmd.x1, cmd.y1, cmd.x2, cmd.y2, sp)
    }
}

private fun linePaint(color: Int, width: Float, alpha: Float, dashed: Boolean = false) =
    makePaint(color, width, alpha, false, dashed)

/** 设计稿 RIG 的 15 个关节锚点按顺序对应的骨骼（用于随 FK 变换） */
private val nodeBones = listOf(
    "pelvis", "spine", "head",
    "lArmU", "rArmU", "lArmL", "rArmL", "lHand", "rHand",
    "lThigh", "rThigh", "lShin", "rShin", "lFoot", "rFoot"
)

/**
 * 画布主绘制。
 * viewMatrix：设计稿 SVG 坐标 → 屏幕像素；transforms：每根骨骼在 SVG 坐标中的 FK 变换。
 */
fun DrawScope.drawEditorArtwork(
    state: EditorState,
    viewMatrix: Mx,
    transforms: Map<String, Mx>,
    onionTransforms: List<Map<String, Mx>> = emptyList()
) {
    val dark = state.darkCanvas
    val parts = state.partsView
    val preview = state.preview

    drawRect(if (dark) Spec.CanvasDark else Spec.Surface)

    // ① 点阵网格（设计稿 16dp 点阵，透明度 ≤30%）
    if (state.showGrid && !preview) {
        val dot = if (dark) Color(0xFF322F35) else Spec.OutlineVar
        val step = 16f * density
        var y = step / 2f
        while (y < size.height) {
            var x = step / 2f
            while (x < size.width) {
                drawCircle(dot.copy(alpha = 0.30f), 1f * density, Offset(x, y))
                x += step
            }
            y += step
        }
    }

    val rigColor = if (dark) SkeletonArt.darkRig.toInt() else Spec.Primary.toArgb()
    val nodeFill = if (dark) Spec.CanvasDark.toArgb() else Color.White.toArgb()
    val anchorFill = if (dark) SkeletonArt.darkRig.toInt() else Spec.Primary.toArgb()
    val tertiary = Spec.Tertiary.toArgb()

    native { nc ->
        nc.save()
        nc.concat(viewMatrix.toAndroid())

        // ② 贴图层（绑定到部件的切片图；三层分离，位于骨架层之下）
        if (state.showArtwork && !preview) {
            for ((id, bmp) in state.bindImages) {
                val m = transforms[id] ?: continue
                val b = state.bone(id) ?: continue
                val c = m.map((b.pivot.x + b.end.x) / 2f, (b.pivot.y + b.end.y) / 2f)
                val halfW = (b.restLength * 0.28f).coerceIn(14f, 34f)
                val halfH = (b.restLength * 0.34f).coerceIn(18f, 42f)
                val dst = RectF(c.x - halfW, c.y - halfH, c.x + halfW, c.y + halfH)
                nc.drawBitmap(bmp.asAndroidBitmap(), null, dst, Paint().apply {
                    isAntiAlias = true; alpha = 210
                })
            }
        }

        // ③ 洋葱皮：只画「该帧与主帧姿态不同」的骨骼残影。
        // 若无差别也照画，静止骨骼的残影会与主骨骼链完全重合，
        // 把 primary 线反复描粗，视觉上就是一堆叠在一起的线条。
        if (state.onionSkin && !preview && state.showRig) {
            for (ghost in onionTransforms) {
                for (b in state.bones) {
                    if (!b.hasRigLine) continue
                    if (ghost[b.id] == transforms[b.id]) continue
                    val seg = rigSegment(b, ghost) ?: continue
                    nc.drawLine(seg.first.x, seg.first.y, seg.second.x, seg.second.y,
                        linePaint(rigColor, 2.6f, 0.22f))
                }
            }
        }

        // ④ 素材层：设计稿 SKEL，逐部件逐路径绘制
        if (state.showArtwork) {
            for (seg in SkeletonDef.segments) {
                val group = SkeletonArt.groups.firstOrNull { it.id == seg.groupId } ?: continue
                val m = seg.boneId?.let { transforms[it] } ?: Mx.Identity
                nc.save()
                nc.concat(m.toAndroid())
                for (i in seg.from..seg.to) {
                    val cmd = group.cmds.getOrNull(i) ?: break
                    drawCmd(nc, cmd, seg.groupId, dark, parts)
                }
                nc.restore()
            }
            // 设计稿 b-joints：12 个关节圆点各自跟随对应骨骼
            val joints = SkeletonArt.groups.firstOrNull { it.id == "b-joints" }
            joints?.cmds?.forEachIndexed { idx, cmd ->
                val boneId = SkeletonDef.jointBones.getOrNull(idx)
                val m = boneId?.let { transforms[it] } ?: Mx.Identity
                nc.save()
                nc.concat(m.toAndroid())
                drawCmd(nc, cmd, "b-joints", dark, parts)
                nc.restore()
            }
        }

        // ⑤ 骨架层：primary 骨骼链 + 关节锚点 + 控制层
        if (state.showRig && !preview) {
            for (hl in SkeletonArt.rigHighlights) {
                nc.drawLine(hl.x1, hl.y1, hl.x2, hl.y2, linePaint(rigColor, 9f, 0.20f))
            }
            val sel = state.selected
            val selSeg = sel?.let { state.bone(it) }?.let { rigSegment(it, transforms) }
            if (selSeg != null) nc.drawLine(
                selSeg.first.x, selSeg.first.y, selSeg.second.x, selSeg.second.y,
                linePaint(rigColor, 9f, 0.20f)
            )
            for (b in state.bones) {
                if (!b.hasRigLine) continue
                val seg = rigSegment(b, transforms) ?: continue
                nc.drawLine(seg.first.x, seg.first.y, seg.second.x, seg.second.y,
                    linePaint(rigColor, 3.2f, 0.92f))
            }
            // 旋转弧（tertiary 1.6dp / 4-4 虚线）与 halo 引导线
            if (selSeg != null && state.tool == Tool.SELECT) {
                val p = selSeg.first
                val arc = Path().apply {
                    addArc(RectF(p.x - 20f, p.y - 20f, p.x + 20f, p.y + 20f), 200f, 90f)
                }
                nc.drawPath(arc, linePaint(tertiary, 1.6f, 0.9f, dashed = true))
                nc.drawLine(p.x - 26f, p.y - 12f, p.x, p.y, linePaint(rigColor, 1.5f, 0.4f))
            }
            // IK 目标：双环 14dp（外环虚线 / 内环实心）
            for (id in state.ikTargets) {
                val b = state.bone(id) ?: continue
                val seg = rigSegment(b, transforms) ?: continue
                val p = seg.second
                nc.drawCircle(p.x, p.y, 7f, linePaint(rigColor, 1.4f, 0.9f, dashed = true))
                nc.drawCircle(p.x, p.y, 3.6f, makePaint(rigColor, 0f, 1f, true))
            }
            // 关节锚点：白填充 + 2.4dp 描边；根节点实心 primary 加白描边
            SkeletonArt.rigNodes.forEachIndexed { idx, n ->
                val boneId = nodeBones.getOrNull(idx) ?: return@forEachIndexed
                val m = transforms[boneId] ?: Mx.Identity
                val p = m.map(n.x, n.y)
                nc.drawCircle(p.x, p.y, n.r, makePaint(
                    if (n.anchor) anchorFill else nodeFill, 0f, 1f, true
                ))
                nc.drawCircle(p.x, p.y, n.r, linePaint(
                    if (n.anchor) Color.White.toArgb() else rigColor, 2.4f, 1f
                ))
            }
        }
        nc.restore()
    }
}

/** 命中测试用的屏幕关节点 */
fun jointScreenPoints(state: EditorState, transforms: Map<String, Mx>, viewMatrix: Mx) =
    state.bones.filter { it.hasRigLine || it.id == "root" }.mapNotNull { b ->
        val m = transforms[b.id] ?: return@mapNotNull null
        val local = m.map(b.pivot.x, b.pivot.y)
        b.id to viewMatrix.map(local.x, local.y)
    }
