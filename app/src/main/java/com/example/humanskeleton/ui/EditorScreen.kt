package com.example.humanskeleton.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.view.Choreographer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.art.SpecIcon
import com.example.humanskeleton.editor.SkeletonCanvas
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.FPS
import com.example.humanskeleton.model.FRAME_COUNT
import kotlinx.coroutines.delay
import java.io.File

private fun loadPart(ctx: Context, uri: Uri): Pair<String, ImageBitmap?> {
    val name = uri.lastPathSegment?.substringAfterLast('/') ?: "部件.png"
    return try {
        val stream = ctx.contentResolver.openInputStream(uri) ?: return name to null
        val bmp = BitmapFactory.decodeStream(stream)
        stream.close()
        name to bmp?.asImageBitmap()
    } catch (e: Exception) {
        name to null
    }
}

private fun exportToDocuments(ctx: Context, json: String): String {
    val dir = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: ctx.filesDir
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "walk_cycle.json")
    file.writeText(json)
    return file.absolutePath
}

/** 设计稿主界面：平板 1280×800 五区布局 / 手机 390×844 竖屏适配 */
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val state = remember { EditorState() }
    var pendingBind by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val id = pendingBind
        if (uri != null && id != null) {
            val (name, bmp) = loadPart(context, uri)
            state.bind(id, uri.toString(), name, bmp)
        }
        pendingBind = null
    }

    fun doExport() {
        val path = exportToDocuments(context, state.exportJson())
        state.snackbar = "已导出：$path"
    }
    fun pickImage(boneId: String) {
        pendingBind = boneId
        picker.launch("image/*")
    }

    // 原生 Choreographer 对齐 Vsync：计算帧号，Canvas 只读状态
    DisposableEffect(state.playing, state.loop) {
        if (!state.playing) {
            onDispose { }
        } else {
            val choreographer = Choreographer.getInstance()
            var last = 0L
            val cb = object : Choreographer.FrameCallback {
                override fun doFrame(t: Long) {
                    if (last == 0L) last = t
                    val dt = (t - last) / 1_000_000_000f
                    last = t
                    var f = state.frame + dt * FPS
                    if (f >= FRAME_COUNT) {
                        if (state.loop) f %= FRAME_COUNT.toFloat()
                        else { f = FRAME_COUNT.toFloat(); state.playing = false }
                    }
                    state.frame = f
                    if (state.playing) choreographer.postFrameCallback(this)
                }
            }
            choreographer.postFrameCallback(cb)
            onDispose { choreographer.removeFrameCallback(cb) }
        }
    }

    // Snackbar 自动消失（设计稿：150ms in / 75ms out，带撤销动作）
    val snack = state.snackbar
    LaunchedEffect(snack) {
        if (snack != null) {
            delay(3200)
            state.snackbar = null
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            PhoneLayout(state, ::doExport, ::pickImage)
        } else {
            TabletLayout(state, ::doExport, ::pickImage)
        }
        if (snack != null) {
            Box(
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 20.dp)
                    .width(if (compact) maxWidth - 32.dp else 520.dp)
            ) {
                SpecSnackbar(snack, "撤销") {
                    state.undo(); state.snackbar = null
                }
            }
        }
    }
}

/** 平板：顶栏 / rail / 骨骼树 / 画布 / 属性 / 时间轴（rail 跨两行） */
@Composable
private fun TabletLayout(
    state: EditorState,
    onExport: () -> Unit,
    onPickImage: (String) -> Unit
) {
    var treeVisible by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(Spec.Surface)) {
        EditorTopBar(state, onMenu = {
            treeVisible = !treeVisible
            state.snackbar = if (treeVisible) "骨骼层级已显示" else "骨骼层级已隐藏"
        }, onExport = onExport, onUndoHint = { state.snackbar = "没有可撤销的操作" })
        Row(Modifier.weight(1f)) {
            NavRail(state, onNewBone = {
                val p = state.bone(state.selected)?.end ?: Offset(150f, 200f)
                state.addBone(state.selected, Offset(p.x + 24f, p.y - 30f))
            })
            Column(Modifier.weight(1f)) {
                Row(Modifier.weight(1f)) {
                    if (treeVisible) BoneTreePanel(state, Modifier.width(214.dp))
                    Box(Modifier.weight(1f)) {
                        SkeletonCanvas(
                            state, onLongPressJoint = { id, pos -> state.longPressMenu = id to pos }
                        )
                        CanvasOverlays(state)
                    }
                    PropertiesPanel(
                        state, Modifier.width(266.dp), onPickImage,
                        onMore = {
                            state.longPressMenu = (state.selected ?: "lArmU") to Offset(400f, 200f)
                        }
                    )
                }
                // 设计稿「动画」目的地：底部时间轴上滑展开
                TimelinePanel(
                    state,
                    Modifier.fillMaxWidth().height(
                        if (state.nav == com.example.humanskeleton.model.NavDest.ANIMATE) 300.dp else 184.dp
                    )
                )
            }
        }
    }
}

/** 手机：画布为主 + 播放 FAB + 时间轴抽屉 + 属性半屏抽屉 + 底部导航 */
@Composable
private fun PhoneLayout(
    state: EditorState,
    onExport: () -> Unit,
    onPickImage: (String) -> Unit
) {
    var showProps by remember { mutableStateOf(false) }
    var showTree by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Spec.Surface)) {
        Column(Modifier.fillMaxSize()) {
            PhoneTopBar(state, onMenu = { showTree = true }, onMore = { showMore = true })
            Box(Modifier.weight(1f).fillMaxWidth()) {
                SkeletonCanvas(
                    state,
                    onLongPressJoint = { id, pos -> state.longPressMenu = id to pos },
                    onSelect = { if (it != null) showProps = true }
                )
                CanvasOverlays(state, compact = true)
                // 播放 FAB（右下，primary 实底 56dp）
                Box(
                    Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = (-14).dp)
                        .size(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(Spec.Primary)
                        .clickable { state.playing = !state.playing },
                    contentAlignment = Alignment.Center
                ) {
                    SpecIcon(if (state.playing) "pause" else "play", iconSize = 24.dp, tint = Color.White)
                }
            }
            PhoneNavBar(state)
            PhoneTimelineSheet(state)
        }

        // 顶栏 more 菜单：预览 / 导出 / 设置
        DropdownMenu(
            expanded = showMore, onDismissRequest = { showMore = false },
            modifier = Modifier.align(Alignment.TopEnd).background(Spec.S3)
        ) {
            DropdownMenuItem(text = { Text("预览", fontSize = 12.5.sp) }, onClick = {
                state.preview = !state.preview; state.playing = state.preview; showMore = false
            })
            DropdownMenuItem(text = { Text("导出", fontSize = 12.5.sp) }, onClick = {
                onExport(); showMore = false
            })
            DropdownMenuItem(text = { Text("设置", fontSize = 12.5.sp) }, onClick = {
                state.nav = com.example.humanskeleton.model.NavDest.SETTINGS
                showProps = true; showMore = false
            })
        }

        // 属性半屏抽屉
        if (showProps) {
            SheetScrim { showProps = false; showTree = false }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.62f)
                    .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                    .background(Spec.S1)
            ) {
                PhonePropertiesSheet(
                    state, onDone = { showProps = false }, onPickImage = onPickImage,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 层级全屏 bottom sheet
        if (showTree) {
            SheetScrim { showTree = false }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.75f)
                    .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                    .background(Spec.S1)
                    .padding(top = 12.dp)
            ) {
                Column {
                    Box(
                        Modifier.size(32.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                            .background(Spec.OutlineVar).align(Alignment.CenterHorizontally)
                    )
                    BoneTreePanel(state, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SheetScrim(onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.28f))
            .clickable { onClose() }
    )
}
