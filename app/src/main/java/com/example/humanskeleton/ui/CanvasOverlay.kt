package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.Tool

/** 设计稿画布内的浮层：顶部分段按钮、左侧竖排工具、提示气泡、HUD、长按菜单、重命名对话框 */
@Composable
fun BoxScope.CanvasOverlays(state: EditorState, compact: Boolean = false) {
    val tools = listOf(
        "select" to Tool.SELECT, "bone" to Tool.BONE, "link" to Tool.BIND
    )

    if (!state.preview) {
        // ① 顶部分段按钮（手机上不显示，改由左侧工具栏承担）
        if (!compact) {
            SpecSeg(
                items = listOf(
                    "选择" to "select", "骨骼" to "bone", "绑定" to "link", "IK" to "ik"
                ),
                selectedIndex = when (state.tool) {
                    Tool.SELECT -> 0; Tool.BONE -> 1; Tool.BIND -> 2; Tool.IK -> 3
                },
                onSelect = {
                    state.tool = when (it) {
                        0 -> Tool.SELECT; 1 -> Tool.BONE; 2 -> Tool.BIND; else -> Tool.IK
                    }
                    state.snackbar = when (state.tool) {
                        Tool.SELECT -> "选择：拖动关节旋转骨骼"
                        Tool.BONE -> "骨骼：轻点空白处新建骨骼"
                        Tool.BIND -> "绑定：选中骨骼后在属性面板添加部件"
                        Tool.IK -> "IK：拖动末端骨骼求解"
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter).offset(y = 12.dp)
            )
        }

        // ② 左侧竖排工具栏
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = if (compact) 16.dp else 64.dp)
                .clip(RoundedCornerShape(if (compact) 14.dp else 16.dp))
                .background(Spec.S2)
                .padding(if (compact) 5.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            tools.forEach { (icon, tool) ->
                SpecIconButton(
                    icon, { state.tool = tool }, on = state.tool == tool,
                    hitSize = if (compact) 34 else 36, iconSize = if (compact) 19 else 20
                )
            }
            if (!compact) {
                SpecIconButton("mirror", {
                    state.mirrorSelected()
                }, hitSize = 36, iconSize = 20)
            }
            SpecIconButton("zoom", {
                state.zoom = 1f
                state.pan = androidx.compose.ui.geometry.Offset.Zero
                state.snackbar = "画布缩放已复位 100%"
            }, hitSize = if (compact) 34 else 36, iconSize = if (compact) 19 else 20)
        }

        // ③ 提示气泡：「左上臂 · 旋转 24°」
        val b = state.bone(state.selected)
        if (b != null && !compact) {
            Row(
                modifier = Modifier.align(Alignment.TopStart)
                    .offset(x = 300.dp, y = 150.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(8.dp).graphicsLayer { rotationZ = 45f }
                        .background(Spec.InverseSurface)
                )
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Spec.InverseSurface)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${state.labelOf(b)} · 旋转 ${"%.1f".format(state.rotationOf(b.id))}°",
                        fontSize = 11.sp, color = Spec.InverseOnSurface
                    )
                }
            }
        }
    }

    // ④ HUD：100% | x 148 · y 302
    if (!compact) {
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-14).dp, y = (-12).dp)
                .clip(RoundedCornerShape(100.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("${(state.zoom * 100).toInt()}%", fontSize = 11.sp,
                fontWeight = FontWeight.Medium, color = Spec.OnSurface)
            Text("|", fontSize = 11.sp, color = Spec.OnSurfaceVar.copy(alpha = 0.5f))
            Text("x ${state.cursor.x.toInt()} · y ${state.cursor.y.toInt()}",
                fontSize = 11.sp, fontFamily = Mono, color = Spec.OnSurfaceVar)
        }
    }

    // ⑤ 长按关节浮层：设为 IK / 解绑 / 重命名
    val menu = state.longPressMenu
    if (menu != null) {
        val (boneId, pos) = menu
        DropdownMenu(
            expanded = true,
            onDismissRequest = { state.longPressMenu = null },
            offset = DpOffset((pos.x / 2f).dp, (pos.y / 2f).dp),
            modifier = Modifier.background(Spec.S3)
        ) {
            val isIK = boneId in state.ikTargets
            DropdownMenuItem(
                text = { Text(if (isIK) "取消 IK" else "设为 IK", fontSize = 12.5.sp) },
                onClick = { state.toggleIK(boneId); state.longPressMenu = null }
            )
            DropdownMenuItem(
                text = { Text("解绑", fontSize = 12.5.sp) },
                onClick = { state.unbind(boneId); state.longPressMenu = null }
            )
            DropdownMenuItem(
                text = { Text("重命名", fontSize = 12.5.sp) },
                onClick = { state.renameTarget = boneId; state.longPressMenu = null }
            )
        }
    }

    // ⑥ 重命名对话框
    state.renameTarget?.let { id ->
        var text by remember(id) { mutableStateOf(state.bone(id)?.let { state.labelOf(it) } ?: "") }
        AlertDialog(
            onDismissRequest = { state.renameTarget = null },
            title = { Text("重命名骨骼", fontSize = 15.sp) },
            text = {
                TextField(
                    value = text, onValueChange = { text = it },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    state.rename(id, text); state.renameTarget = null
                }) { Text("确定", color = Spec.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { state.renameTarget = null }) { Text("取消", color = Spec.Primary) }
            }
        )
    }
}
