package com.example.humanskeleton.ui

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.humanskeleton.gesture.HAND_CONNECTIONS
import com.example.humanskeleton.gesture.HandGesture
import com.example.humanskeleton.gesture.HandTrackingController
import com.example.humanskeleton.model.EditorState

/** ComponentActivity 同时是 LifecycleOwner；沿 ContextWrapper 链向上找，避免引入额外依赖 */
private fun Context.lifecycleActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.lifecycleActivity()
    else -> null
}

/**
 * 画布右上角的体感手势区：开关 → 相机小窗（含 21 点叠加）→ 实时 HUD → 说明表。
 * 只在 BoxScope 内使用，平板与手机共用，仅尺寸不同。
 */
@Composable
fun BoxScope.GestureOverlay(state: EditorState, compact: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        granted = ok
        if (!ok) {
            state.setGestureActive(false)
            state.snackbar = "没有相机权限，无法开启体感手势"
        }
    }

    LaunchedEffect(state.gestureEnabled) {
        if (state.gestureEnabled && !granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val previewW = if (compact) 132.dp else 196.dp
    val previewH = if (compact) 100.dp else 148.dp

    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = (-12).dp, y = if (compact) 12.dp else 12.dp)
            .width(previewW),
        horizontalAlignment = Alignment.End
    ) {
        // ① 开关 + 说明
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (state.gestureEnabled) Spec.PrimaryContainer else Spec.S2)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .clickable {
                        val next = !state.gestureEnabled
                        state.setGestureActive(next)
                        state.snackbar = if (next) "体感手势已开启，先点「?」查看手势表"
                        else "体感手势已关闭，骨架恢复编辑姿态"
                    }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Rounded.Videocam, null,
                    modifier = Modifier.size(16.dp),
                    tint = if (state.gestureEnabled) Spec.OnPrimaryContainer else Spec.OnSurfaceVar
                )
                Text(
                    "手势",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (state.gestureEnabled) Spec.OnPrimaryContainer else Spec.OnSurfaceVar
                )
            }
            Box(
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(50.dp))
                    .clickable { state.gestureShowGuide = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, modifier = Modifier.size(15.dp),
                    tint = Spec.OnSurfaceVar)
            }
        }

        if (state.gestureEnabled && granted) {
            Spacer(Modifier.height(8.dp))
            GesturePreviewCard(state, previewW, previewH)
            Spacer(Modifier.height(6.dp))
            GestureHud(state)
        } else if (state.gestureEnabled) {
            Spacer(Modifier.height(6.dp))
            Text(
                state.gestureStatus,
                fontSize = 10.5.sp, color = Spec.Error,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    if (state.gestureShowGuide) GestureGuideDialog(state)
}

@Composable
private fun GesturePreviewCard(state: EditorState, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(
        modifier = Modifier
            .size(w, h)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF211F24))
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView = it }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 21 个关键点叠加：与分析帧同一套归一化坐标
        val frame = state.gestureFrame
        Canvas(Modifier.fillMaxSize()) {
            val pts = frame.points
            if (pts.size >= 21) {
                val line = Color(0xFFB79BFF)
                for ((a, b) in HAND_CONNECTIONS) {
                    drawLine(
                        line,
                        Offset(pts[a].x * size.width, pts[a].y * size.height),
                        Offset(pts[b].x * size.width, pts[b].y * size.height),
                        strokeWidth = 1.6.dp.toPx()
                    )
                }
                pts.forEach { p ->
                    drawCircle(
                        Color.White,
                        radius = 2.1.dp.toPx(),
                        center = Offset(p.x * size.width, p.y * size.height)
                    )
                }
            } else {
                // 未检测到手时画一个虚线取景框，提示把手放进来
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val inset = 14.dp.toPx()
                val tl = Offset(inset, inset)
                val tr = Offset(size.width - inset, inset)
                val br = Offset(size.width - inset, size.height - inset)
                val bl = Offset(inset, size.height - inset)
                val c = Color.White.copy(alpha = 0.45f)
                drawLine(c, tl, tr, strokeWidth = 1.dp.toPx(), pathEffect = dash)
                drawLine(c, tr, br, strokeWidth = 1.dp.toPx(), pathEffect = dash)
                drawLine(c, br, bl, strokeWidth = 1.dp.toPx(), pathEffect = dash)
                drawLine(c, bl, tl, strokeWidth = 1.dp.toPx(), pathEffect = dash)
            }
        }

        // 底部状态条
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                frame.gesture.label,
                fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium
            )
            Text(
                if (frame.hasHand) "${(frame.confidence * 100).toInt()}%" else "—",
                fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = Mono
            )
        }
    }

    val owner = context.lifecycleActivity()
    DisposableEffect(previewView, state.gestureEnabled, owner) {
        val pv = previewView
        if (pv != null && owner != null && state.gestureEnabled) {
            val controller = HandTrackingController(
                context = context,
                lifecycleOwner = owner,
                previewView = pv,
                onFrame = { state.onGestureFrame(it) },
                onDiscrete = { state.onGestureAction(it) },
                onStatus = { state.gestureStatus = it }
            )
            controller.start()
            onDispose { controller.stop() }
        } else {
            onDispose { }
        }
    }
}

@Composable
private fun GestureHud(state: EditorState) {
    val f = state.gestureFrame
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Spec.S2)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            if (f.hasHand) f.gesture.action else state.gestureStatus,
            fontSize = 10.sp, color = Spec.OnSurface, fontWeight = FontWeight.Medium,
            maxLines = 2
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { f.confidence },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Spec.Primary,
            trackColor = Spec.OutlineVar
        )
        if (f.hasHand) {
            Spacer(Modifier.height(3.dp))
            Text(
                "${f.handedness.ifEmpty { "Hand" }} · 张开 ${(f.openness * 100).toInt()}% · 捏合 ${"%.2f".format(f.pinch)}",
                fontSize = 9.sp, color = Spec.OnSurfaceVar, fontFamily = Mono
            )
        }
    }
}

/** 手势词汇表：直接由 HandGesture.guide 生成，保证与代码里的映射定义一致 */
@Composable
private fun GestureGuideDialog(state: EditorState) {
    AlertDialog(
        onDismissRequest = { state.gestureShowGuide = false },
        title = { Text("体感手势表", fontSize = 16.sp, fontWeight = FontWeight.Medium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "手掌正对前置摄像头、距离 30–60 cm。连续手势实时驱动骨骼；" +
                            "离散手势需稳定保持约 0.4 秒，两次触发间隔 0.9 秒。",
                    fontSize = 11.sp, color = Spec.OnSurfaceVar, lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Text("连续操控", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Spec.Primary)
                HandGesture.guide.filter { it.continuous }.forEach { GuideRow(it) }
                Spacer(Modifier.height(8.dp))
                Text("离散动作", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Spec.Primary)
                HandGesture.guide.filter { !it.continuous }.forEach { GuideRow(it) }
                Spacer(Modifier.height(8.dp))
                Text(
                    "开启手势时会自动暂停走路循环；V 字手势可随时播放 / 暂停。" +
                            "OK 复位或关闭开关即可清除全部手势效果。",
                    fontSize = 10.5.sp, color = Spec.OnSurfaceVar, lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { state.gestureShowGuide = false }) {
                Text("知道了", color = Spec.Primary)
            }
        }
    )
}

@Composable
private fun GuideRow(g: HandGesture) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(g.label, fontSize = 11.sp, color = Spec.OnSurface,
            fontWeight = FontWeight.Medium, modifier = Modifier.width(72.dp))
        Column(Modifier.weight(1f)) {
            Text(g.action, fontSize = 10.5.sp, color = Spec.OnSurfaceVar)
            Text(g.hint, fontSize = 9.5.sp, color = Spec.OnSurfaceVar.copy(alpha = 0.7f))
        }
    }
}
