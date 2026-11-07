package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.art.SpecIcon
import com.example.humanskeleton.model.Easing
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.FRAME_COUNT
import com.example.humanskeleton.model.Track
import kotlin.math.roundToInt

/** 设计稿底部时间轴（184dp）：播放控制 + 录制 + chips + 轨道 + 菱形关键帧 + 播放头 */
@Composable
fun TimelinePanel(state: EditorState, modifier: Modifier = Modifier) {
    val tracks = state.visibleTracks()
    val pxPerDp = LocalDensity.current.density
    var trackWidth by remember { mutableFloatStateOf(1f) }
    var dragging by remember { mutableStateOf<Pair<String, Int>?>(null) }

    Column(modifier.background(Spec.S1)) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecIconButton(
                if (state.playing) "pause" else "play",
                { state.playing = !state.playing }, on = state.playing, hitSize = 36, iconSize = 18
            )
            SpecIconButton("prev", { state.stepFrame(-1) }, hitSize = 36, iconSize = 18)
            SpecIconButton("next", { state.stepFrame(1) }, hitSize = 36, iconSize = 18)
            Box(
                Modifier.clip(RoundedCornerShape(6.dp)).background(Spec.Surface)
                    .border(1.dp, Spec.OutlineVar, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("帧 ${state.frame.toInt()} / $FRAME_COUNT", fontFamily = Mono,
                    fontSize = 12.sp, color = Spec.OnSurface)
            }
            if (state.recording) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Spec.Tertiary))
                Text("录制中", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.Tertiary)
            }
            Spacer(Modifier.weight(1f))
            SpecChip("洋葱皮", { state.onionSkin = !state.onionSkin }, icon = "onion", on = state.onionSkin)
            SpecChip("循环", { state.loop = !state.loop }, on = state.loop)
            SpecChip("关键帧", {
                state.insertKeyframe(state.selected ?: "lArmU")
            }, icon = "add")
        }
        HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)

        Row(Modifier.fillMaxSize()) {
            Column(Modifier.width(150.dp).fillMaxHeight().background(Spec.S1)) {
                Box(Modifier.fillMaxWidth().height(22.dp).background(Spec.S1))
                HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)
                tracks.forEach { t ->
                    val sel = state.selected == t.boneId
                    Row(
                        Modifier.fillMaxWidth().height(30.dp)
                            .background(if (sel) Spec.Primary.copy(alpha = 0.06f) else Spec.S1)
                            .clickable { state.selected = t.boneId }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SpecIcon("bone", iconSize = 13.dp,
                            tint = if (sel) Spec.Primary else Spec.OnSurfaceVar)
                        Text(
                            state.bone(t.boneId)?.let { state.labelOf(it) } ?: t.boneId,
                            fontSize = 11.5.sp,
                            fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                            color = if (sel) Spec.Primary else Spec.OnSurfaceVar,
                            maxLines = 1
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1ECF4))
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(Spec.OutlineVar))

            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth().height(22.dp).background(Spec.S1)
                            .pointerInput(Unit) {
                                detectTapGestures { p ->
                                    state.seek((p.x / trackWidth * FRAME_COUNT).coerceIn(0f, 24f))
                                }
                            }
                    ) {
                        for (f in listOf(0, 4, 8, 12, 16, 20, 24)) {
                            val left = (f.toFloat() / FRAME_COUNT).coerceAtMost(0.98f)
                            Text(
                                "$f", fontSize = 9.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar,
                                modifier = Modifier.offset(
                                    x = (trackWidth * left / pxPerDp).dp - 6.dp, y = 4.dp
                                )
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)
                    tracks.forEach { t ->
                        TrackRow(state, t, trackWidth) { boneId, frame ->
                            dragging = if (boneId.isEmpty() || frame < 0) null else boneId to frame
                        }
                    }
                }
                val x = (state.frame / FRAME_COUNT).coerceIn(0f, 1f)
                Box(
                    Modifier.offset(x = (trackWidth * x / pxPerDp).dp, y = 0.dp)
                        .width(1.5.dp).fillMaxHeight().background(Spec.Tertiary)
                )
                Box(
                    Modifier.offset(x = (trackWidth * x / pxPerDp).dp - 6.dp, y = 0.dp)
                        .size(12.dp, 8.dp).background(Spec.Tertiary)
                )
                dragging?.let { (_, f) ->
                    Box(
                        Modifier.offset(
                            x = (trackWidth * f / FRAME_COUNT / pxPerDp).dp - 16.dp, y = 26.dp
                        ).clip(RoundedCornerShape(8.dp)).background(Spec.InverseSurface)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("帧 $f", fontSize = 10.5.sp, fontFamily = Mono, color = Spec.InverseOnSurface)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    state: EditorState,
    track: Track,
    widthPx: Float,
    onDragHint: (String, Int) -> Unit
) {
    val keys = track.keys.sortedBy { it.frame }
    val pxPerDp = LocalDensity.current.density
    Box(
        Modifier.fillMaxWidth().height(30.dp)
            .pointerInput(track.boneId) {
                detectTapGestures(onDoubleTap = { p ->
                    val f = (p.x / widthPx * FRAME_COUNT).roundToInt().coerceIn(0, FRAME_COUNT)
                    state.insertKeyframe(track.boneId, f)
                })
            }
            .pointerInput(track.boneId) {
                detectDragGestures(
                    onDragStart = { p ->
                        val near = keys.minByOrNull {
                            kotlin.math.abs(it.frame.toFloat() / FRAME_COUNT * widthPx - p.x)
                        }
                        if (near != null && kotlin.math.abs(near.frame.toFloat() / FRAME_COUNT * widthPx - p.x) < 18f) {
                            state.dragOrigin = near.frame
                            state.dragBone = track.boneId
                            state.dragTarget = near.frame
                            onDragHint(track.boneId, near.frame)
                        } else state.dragBone = null
                    },
                    onDrag = { change, _ ->
                        if (state.dragBone == track.boneId) {
                            val f = (change.position.x / widthPx * FRAME_COUNT).roundToInt()
                                .coerceIn(0, FRAME_COUNT)
                            state.dragTarget = f
                            onDragHint(track.boneId, f)
                        }
                    },
                    onDragEnd = {
                        val bone = state.dragBone
                        val from = state.dragOrigin
                        val to = state.dragTarget
                        if (bone != null && from != null && to != null) state.moveKeyframe(bone, from, to)
                        state.dragBone = null; state.dragOrigin = null; state.dragTarget = null
                        onDragHint("", -1)
                    },
                    onDragCancel = {
                        state.dragBone = null; state.dragOrigin = null; state.dragTarget = null
                        onDragHint("", -1)
                    }
                )
            }
    ) {
        Box(Modifier.offset(y = 14.dp).fillMaxWidth().height(2.dp).background(Color(0xFFE7E0EC)))
        keys.forEach { k ->
            val kx = (k.frame.toFloat() / FRAME_COUNT).coerceAtMost(0.985f)
            val eased = k.easing != Easing.LINEAR && !k.endFrame
            Box(
                Modifier.offset(x = (widthPx * kx / pxPerDp).dp - 5.5.dp, y = 9.5.dp)
                    .size(11.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        when {
                            k.endFrame -> Spec.Tertiary
                            eased -> Color.White
                            else -> Spec.Primary
                        }
                    )
                    .border(
                        1.5.dp,
                        when {
                            eased -> Spec.Primary
                            else -> Color.White
                        },
                        RoundedCornerShape(2.dp)
                    )
                    .pointerInput(k.frame) {
                        detectTapGestures(
                            onDoubleTap = { state.cycleEasing(track.boneId, k.frame) },
                            onLongPress = { state.removeKeyframe(track.boneId, k.frame) }
                        )
                    }
            )
        }
    }
    HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1ECF4))
}

/** 设计稿手机：时间轴收进 150dp bottom sheet */
@Composable
fun PhoneTimelineSheet(state: EditorState, modifier: Modifier = Modifier) {
    val pxPerDp = LocalDensity.current.density
    var widthPx by remember { mutableFloatStateOf(1f) }
    Column(
        modifier.height(150.dp)
            .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
            .background(Spec.S1)
    ) {
        Box(
            Modifier.size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                .background(Spec.OutlineVar).align(Alignment.CenterHorizontally)
                .offset(y = 10.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("时间轴", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurface)
            Box(
                Modifier.clip(RoundedCornerShape(6.dp)).background(Spec.Surface)
                    .border(1.dp, Spec.OutlineVar, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text("${state.frame.toInt()} / $FRAME_COUNT", fontFamily = Mono,
                    fontSize = 11.sp, color = Spec.OnSurface)
            }
            Spacer(Modifier.weight(1f))
            SpecChip("洋葱皮", { state.onionSkin = !state.onionSkin },
                icon = "onion", on = state.onionSkin, small = true)
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(10.dp)).background(Spec.Surface)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectTapGestures { p ->
                        state.seek((p.x / widthPx * FRAME_COUNT).coerceIn(0f, 24f))
                    }
                }
        ) {
            Row(Modifier.fillMaxWidth().height(16.dp)) {
                Text("0", fontSize = 8.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar,
                    modifier = Modifier.padding(start = 2.dp))
                Spacer(Modifier.weight(1f))
                Text("12", fontSize = 8.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar)
                Spacer(Modifier.weight(1f))
                Text("24", fontSize = 8.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar,
                    modifier = Modifier.padding(end = 2.dp))
            }
            Box(Modifier.offset(y = 26.dp).fillMaxWidth().height(2.dp)
                .background(Color(0xFFE7E0EC)))
            state.visibleTracks().firstOrNull()?.keys?.forEach { k ->
                val x = (k.frame.toFloat() / FRAME_COUNT).coerceAtMost(0.985f)
                Box(
                    Modifier.offset(x = (widthPx * x / pxPerDp).dp - 4.5.dp, y = 22.dp)
                        .size(9.dp).graphicsLayer { rotationZ = 45f }
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (k.endFrame) Spec.Tertiary else Spec.Primary)
                        .border(1.dp, Color.White, RoundedCornerShape(2.dp))
                )
            }
            Box(
                Modifier.offset(x = (widthPx * state.frame / FRAME_COUNT / pxPerDp).dp)
                    .width(1.5.dp).fillMaxHeight().background(Spec.Tertiary)
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
