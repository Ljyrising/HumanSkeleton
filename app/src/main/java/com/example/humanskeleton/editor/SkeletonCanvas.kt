package com.example.humanskeleton.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.humanskeleton.model.DEG2RAD
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.Tool
import com.example.humanskeleton.model.computePartTransforms
import com.example.humanskeleton.model.pointToSegment
import com.example.humanskeleton.model.rigSegment
import com.example.humanskeleton.model.rotationFromDrag
import kotlin.math.hypot

/**
 * 设计稿的画布：单指拖动关节旋转骨骼、双指缩放平移、长按关节弹出浮层。
 * 所有绘制来自 drawEditorArtwork（设计稿 SVG 逐路径）。
 */
@Composable
fun SkeletonCanvas(
    state: EditorState,
    modifier: Modifier = Modifier,
    onLongPressJoint: (String, Offset) -> Unit = { _, _ -> },
    onSelect: (String?) -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.canvasSize = it }
            .pointerInput(Unit) {
                val scope = this
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                    val start = down.position
                    val sizeNow = IntSize(scope.size.width, scope.size.height)
                    val t0 = computePartTransforms(state.pose, state.animatedAt(), state.bones)
                    val vm0 = state.viewMatrix()

                    // 命中关节（半径 22px）/ 骨骼段（阈值 14px）
                    val hitJoint = jointScreenPoints(state, t0, vm0)
                        .minByOrNull { (_, p) -> hypot(p.x - start.x, p.y - start.y) }
                        ?.takeIf { (_, p) -> hypot(p.x - start.x, p.y - start.y) <= 22f * density }
                    var hitBone: String? = hitJoint?.first
                    if (hitBone == null) {
                        val svg = state.screenToSvg(start, sizeNow, state.baseScale(sizeNow))
                        if (svg != null) {
                            hitBone = state.bones.filter { it.hasRigLine }.mapNotNull { b ->
                                val seg = rigSegment(b, t0) ?: return@mapNotNull null
                                val d = pointToSegment(svg, seg.first, seg.second)
                                b.id to d
                            }.filter { it.second <= 12f }.minByOrNull { it.second }?.first
                        }
                    }

                    var last = start
                    var dragged = false
                    var longFired = false
                    var panPair: Pair<Offset, Float>? = null
                    var rotateId: String? = null
                    var ikId: String? = null

                    if (hitBone != null) {
                        when {
                            state.tool == Tool.SELECT && !state.locked && hitJoint != null -> rotateId = hitBone
                            state.tool == Tool.IK -> ikId = hitBone
                        }
                    }

                    while (true) {
                        val ev = if (!longFired && hitBone != null && !dragged) {
                            withTimeoutOrNull(420L) { awaitPointerEvent(PointerEventPass.Main) }
                        } else awaitPointerEvent(PointerEventPass.Main)

                        if (ev == null) {
                            // 静止超过 420ms 且未移动 → 长按关节
                            if (hitBone != null && !dragged && !longFired) {
                                longFired = true
                                onLongPressJoint(hitBone, start)
                            }
                            continue
                        }
                        val pressed = ev.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        if (pressed.size >= 2) {
                            // 双指缩放 + 平移画布（设计稿交互要点）
                            val a = pressed[0].position; val b = pressed[1].position
                            val dist = hypot(b.x - a.x, b.y - a.y).toFloat()
                            val center = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                            val prev = panPair
                            if (prev == null) {
                                panPair = center to dist
                            } else {
                                if (prev.second > 1f && dist > 1f) {
                                    state.zoom = (state.zoom * (dist / prev.second)).coerceIn(0.4f, 4f)
                                }
                                state.pan += Offset(center.x - prev.first.x, center.y - prev.first.y)
                                panPair = center to dist
                            }
                            dragged = true
                            ev.changes.forEach { it.consume() }
                            continue
                        }
                        panPair = null

                        val ch = pressed.last()
                        val pos = ch.position
                        val delta = pos - last
                        if (hypot(delta.x, delta.y) > 3f) dragged = true
                        last = pos
                        val svg = state.screenToSvg(pos, IntSize(scope.size.width, scope.size.height),
                            state.baseScale(IntSize(scope.size.width, scope.size.height)))
                        if (svg != null) state.cursor = svg

                        if (dragged && !longFired) {
                            when {
                                rotateId != null -> {
                                    val bone = state.bone(rotateId) ?: continue
                                    val t = computePartTransforms(state.pose, state.animatedAt(), state.bones)
                                    val m = t[bone.id] ?: MxIdentity()
                                    val pivot = m.map(bone.pivot.x, bone.pivot.y)
                                    val from = state.screenToSvg(
                                        pos - delta, IntSize(scope.size.width, scope.size.height),
                                        state.baseScale(IntSize(scope.size.width, scope.size.height))
                                    ) ?: continue
                                    val inc = rotationFromDrag(bone, pivot, from, svg ?: from)
                                    state.setRotation(rotateId, state.rotationOf(rotateId) + inc / DEG2RAD)
                                    if (state.recording) state.insertKeyframe(rotateId, showTip = false)
                                }
                                ikId != null -> state.dragIK(ikId, svg ?: continue)
                                hitBone == null && state.tool == Tool.SELECT -> {
                                    // 空白处单指平移画布
                                    state.pan += delta
                                }
                            }
                        }
                        ev.changes.forEach { if (it.pressed) it.consume() }
                    }

                    if (!dragged && !longFired) {
                        // 轻点
                        val pos = start
                        val svg = state.screenToSvg(pos, IntSize(scope.size.width, scope.size.height),
                            state.baseScale(IntSize(scope.size.width, scope.size.height)))
                        when (state.tool) {
                            Tool.BONE -> if (hitBone == null && svg != null && !state.locked) {
                                state.addBone(state.selected, svg)
                            } else if (hitBone != null) state.selected = hitBone
                            Tool.BIND -> {
                                state.selected = hitBone
                                state.snackbar = if (hitBone != null)
                                    "已选中「${state.bone(hitBone)?.let { state.labelOf(it) }}」，在属性面板添加部件"
                                else "请点选要绑定的骨骼"
                            }
                            Tool.IK -> state.selected = hitBone
                            Tool.SELECT -> state.selected = hitBone
                        }
                        onSelect(hitBone)
                    }
                }
            }
    ) {
        val t = state.currentTransforms()
        // 洋葱皮是编辑辅助：播放时不画残影，否则前后帧骨骼链叠在动画上像一堆乱线
        val ghosts = if (state.onionSkin && !state.playing) listOf(
            computePartTransforms(state.pose, state.animatedAt(state.frame - 4f), state.bones),
            computePartTransforms(state.pose, state.animatedAt(state.frame + 4f), state.bones)
        ) else emptyList()
        drawEditorArtwork(state, state.viewMatrix(), t, ghosts)
    }
}

private fun MxIdentity() = com.example.humanskeleton.model.Mx.Identity
