package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.NavDest

/** 设计稿右栏「属性 · 左上臂」；部件 / 设置导航时切换内容 */
@Composable
fun PropertiesPanel(
    state: EditorState,
    modifier: Modifier = Modifier,
    onPickImage: (String) -> Unit,
    onMore: () -> Unit
) {
    Column(modifier = modifier.fillMaxHeight().background(Spec.S1)) {
        when (state.nav) {
            NavDest.PARTS -> PartsPage(state, onPickImage)
            NavDest.SETTINGS -> SettingsPage(state)
            NavDest.ANIMATE -> AnimatePage(state)
            else -> PropPage(state, onPickImage, onMore)
        }
    }
}

@Composable
private fun PropPage(state: EditorState, onPickImage: (String) -> Unit, onMore: () -> Unit) {
    var numberEdit by remember { mutableStateOf<Triple<String, Float, (Float) -> Unit>?>(null) }
    val id = state.selected
    val b = state.bone(id)
    PanelHeader("属性 · ${b?.let { state.labelOf(it) } ?: "未选中"}", onMore)
    if (b == null) {
        Text(
            "点选画布中的骨骼以编辑属性", fontSize = 12.5.sp,
            color = Spec.OnSurfaceVar, modifier = Modifier.padding(16.dp)
        )
        return
    }
    Column(
        Modifier.padding(horizontal = 12.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SpecField("旋转", "%.1f°".format(state.rotationOf(b.id)), Modifier.fillMaxWidth())
        SpecSliderRow(
            "旋转", state.rotationOf(b.id), -180f..180f,
            { state.setRotation(b.id, it) },
            Modifier.fillMaxWidth(), leftLabel = "−180°", rightLabel = "+180°"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecField("x", "%.0f".format(state.poseOf(b.id).x), Modifier.weight(1f)
                .clickable {
                    numberEdit = Triple("X", state.poseOf(b.id).x) { state.setX(b.id, it) }
                })
            SpecField("y", "%.0f".format(state.poseOf(b.id).y), Modifier.weight(1f)
                .clickable {
                    numberEdit = Triple("Y", state.poseOf(b.id).y) { state.setY(b.id, it) }
                })
        }
        SpecSliderRow(
            "长度", state.lengthOf(b.id), 10f..160f,
            { state.setLength(b.id, it) }, Modifier.fillMaxWidth(),
            display = "${state.lengthOf(b.id).toInt()} dp"
        )
        HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)
        Text("绑定到贴图", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurfaceVar)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecChip(
                state.bindNames[b.id] ?: "未绑定",
                { state.snackbar = state.bindNames[b.id]?.let { "已绑定 $it" } ?: "尚未绑定部件" },
                icon = "image", on = b.id in state.binds
            )
            SpecChip("添加部件", { onPickImage(b.id) }, icon = "add")
        }
        Text("动画属性", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurfaceVar)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecField("缓动", state.poseOf(b.id).easingName, Modifier.weight(1f)
                .clickable {
                    val next = when (state.poseOf(b.id).easingName) {
                        "Ease" -> "Linear"; "Linear" -> "Hold"; else -> "Ease"
                    }
                    state.setPose(b.id, { copy(easingName = next) })
                })
            SpecField("排序", state.poseOf(b.id).order.toString(), Modifier.weight(1f)
                .clickable { state.setPose(b.id, { copy(order = order % 9 + 1) }) })
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecTonalButton("重置", { state.resetSelected() }, Modifier.weight(1f))
            SpecOutlinedButton("镜像", { state.mirrorSelected() }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
    }
    numberEdit?.let { (title, initial, apply) ->
        var text by remember(title, initial) { mutableStateOf("%.0f".format(initial)) }
        AlertDialog(
            onDismissRequest = { numberEdit = null },
            title = { Text("$title 位移", fontSize = 15.sp) },
            text = {
                TextField(value = text, onValueChange = { text = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    text.toFloatOrNull()?.let(apply)
                    numberEdit = null
                }) { Text("确定", color = Spec.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { numberEdit = null }) { Text("取消", color = Spec.Primary) }
            }
        )
    }
}

/** 设计稿「动画」目的地：当前骨骼的动画属性与关键帧列表 */
@Composable
private fun AnimatePage(state: EditorState) {
    val id = state.selected
    val b = state.bone(id)
    PanelHeader("动画属性 · ${b?.let { state.labelOf(it) } ?: "未选中"}")
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("帧 ${state.frame.toInt()} / 24 · 30fps", fontSize = 11.5.sp,
            fontFamily = Mono, color = Spec.OnSurfaceVar)
        if (b != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecField("缓动", state.poseOf(b.id).easingName, Modifier.weight(1f)
                    .clickable {
                        val next = when (state.poseOf(b.id).easingName) {
                            "Ease" -> "Linear"; "Linear" -> "Hold"; else -> "Ease"
                        }
                        state.setPose(b.id, { copy(easingName = next) })
                    })
                SpecField("排序", state.poseOf(b.id).order.toString(), Modifier.weight(1f)
                    .clickable { state.setPose(b.id, { copy(order = order % 9 + 1) }) })
            }
            Text("关键帧", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurfaceVar)
            val keys = state.keyframesOf(b.id).sortedBy { it.frame }
            if (keys.isEmpty()) Text("暂无关键帧，可在时间轴双击轨道添加",
                fontSize = 11.sp, color = Spec.OnSurfaceVar)
            keys.forEach { k ->
                Row(Modifier.fillMaxWidth().clickable { state.seek(k.frame.toFloat()) },
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("帧 ${k.frame}", fontSize = 11.5.sp, fontFamily = Mono, color = Spec.OnSurface)
                    Text("%.1f° · ${k.easing.name}".format(k.angle),
                        fontSize = 11.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar)
                }
            }
            Spacer(Modifier.height(8.dp))
            SpecChip("关键帧", { state.insertKeyframe(b.id) }, icon = "add")
        }
        Text("底部时间轴已展开，可双击轨道插入、拖动菱形吸附整帧。",
            fontSize = 11.sp, color = Spec.OnSurfaceVar)
    }
}

/** 设计稿「部件」目的地：显示每个骨骼绑定的贴图部件 */
@Composable
private fun PartsPage(state: EditorState, onPickImage: (String) -> Unit) {
    PanelHeader("部件", onMore = { state.snackbar = "部件按切图分组，绑定后随骨骼变换" })
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "贴图层与骨架层分离，绑定部件不会被烘焙进控制层。",
            fontSize = 11.5.sp, color = Spec.OnSurfaceVar
        )
        state.bones.filter { it.id in state.binds }.forEach { b ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpecChip(state.bindNames[b.id] ?: "部件", { state.unbind(b.id) }, icon = "image", on = true)
            }
        }
        if (state.binds.isEmpty()) {
            Text("暂无绑定部件", fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
        }
        Spacer(Modifier.height(8.dp))
        SpecChip("添加部件", {
            onPickImage(state.selected ?: "lArmU")
        }, icon = "add", on = false)
        Text(
            "提示：先选中骨骼，再添加部件；点击已绑定 chip 可解绑。",
            fontSize = 11.sp, color = Spec.OnSurfaceVar
        )
    }
}

/** 设置目的地：设计稿里出现的各类显示开关与深色画布 */
@Composable
private fun SettingsPage(state: EditorState) {
    PanelHeader("设置", onMore = { state.snackbar = "设置项即时生效" })
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("画布", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurfaceVar)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecChip("网格", { state.showGrid = !state.showGrid }, icon = "grid", on = state.showGrid)
            SpecChip("深色画布", {
                state.darkCanvas = !state.darkCanvas
                state.snackbar = if (state.darkCanvas) "画布使用 #1D1B20" else "画布恢复浅色"
            }, on = state.darkCanvas)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecChip("贴图层", { state.showArtwork = !state.showArtwork }, icon = "eye", on = state.showArtwork)
            SpecChip("骨骼链", { state.showRig = !state.showRig }, icon = "bone", on = state.showRig)
        }
        HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)
        Text("编辑", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurfaceVar)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecChip("洋葱皮", { state.onionSkin = !state.onionSkin }, icon = "onion", on = state.onionSkin)
            SpecChip("录制", { state.recording = !state.recording }, on = state.recording)
            SpecChip("锁定", { state.locked = !state.locked }, icon = "lock", on = state.locked)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SpecChip("部件着色", { state.partsView = !state.partsView }, icon = "weight", on = state.partsView)
            SpecChip("缩放复位", {
                state.zoom = 1f; state.pan = androidx.compose.ui.geometry.Offset.Zero
            }, icon = "zoom")
        }
    }
}

/** 设计稿手机：点击骨骼弹出的半屏属性抽屉 */
@Composable
fun PhonePropertiesSheet(
    state: EditorState,
    onDone: () -> Unit,
    onPickImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val id = state.selected
    val b = state.bone(id)
    val name = b?.let { state.labelOf(it) } ?: "未选中"
    Column(modifier.background(Spec.S1).padding(20.dp, 16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            SpecIconButton("chevron", onDone, hitSize = 36, iconSize = 22)
            Column(Modifier.padding(start = 6.dp)) {
                Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurface)
                Text("绑定与动画属性", fontSize = 11.sp, color = Spec.OnSurfaceVar)
            }
            Spacer(Modifier.weight(1f))
            SpecIconButton("more", { state.snackbar = "$name · 长按画布关节可重命名" }, hitSize = 36)
        }
        Spacer(Modifier.height(18.dp))
        if (b != null) {
            Text("旋转", fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text("%.1f".format(state.rotationOf(b.id)), fontSize = 26.sp, fontFamily = Mono, color = Spec.Primary)
                Text(" 度", fontSize = 13.sp, color = Spec.OnSurfaceVar)
            }
            SpecSliderRow("旋转", state.rotationOf(b.id), -180f..180f,
                { state.setRotation(b.id, it) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SpecField("X", "%.0f".format(state.poseOf(b.id).x), Modifier.weight(1f))
                SpecField("Y", "%.0f".format(state.poseOf(b.id).y), Modifier.weight(1f))
                SpecField("长度", state.lengthOf(b.id).toInt().toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("绑定贴图", fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpecChip(state.bindNames[b.id] ?: "未绑定", {
                    state.snackbar = "点击「添加」选择部件图片"
                }, icon = "image", on = b.id in state.binds)
                SpecChip("添加", { onPickImage(b.id) }, icon = "add")
            }
        }
        Spacer(Modifier.weight(1f))
        SpecFilledButton("完成", onDone, modifier = Modifier.fillMaxWidth())
    }
}
