package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.model.EditorState

/** 设计稿平板 header：菜单 / 项目名 / 撤销 / 重做 / 网格 / 预览 / 导出 */
@Composable
fun EditorTopBar(state: EditorState, onMenu: () -> Unit, onExport: () -> Unit, onUndoHint: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).background(Spec.Surface)
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SpecIconButton("menu", onMenu, hitSize = 40)
        Column(Modifier.padding(start = 6.dp)) {
            Text("走路循环", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurface)
            Text("角色_骨架_v3 · 24 帧 / 30fps", fontSize = 11.sp, color = Spec.OnSurfaceVar)
        }
        Spacer(Modifier.weight(1f))
        SpecIconButton("undo", {
            if (state.canUndo) state.undo() else onUndoHint()
        }, enabled = state.canUndo, hitSize = 40)
        SpecIconButton("redo", {
            if (state.canRedo) state.redo() else onUndoHint()
        }, enabled = state.canRedo, hitSize = 40)
        SpecIconButton("grid", {
            state.showGrid = !state.showGrid
            state.snackbar = if (state.showGrid) "画布网格已显示" else "画布网格已隐藏"
        }, on = state.showGrid, hitSize = 40)
        Spacer(Modifier.width(8.dp))
        SpecTextButton("预览", {
            state.preview = !state.preview
            state.playing = state.preview
            state.snackbar = if (state.preview) "预览模式：仅播放骨架动画" else "已退出预览"
        })
        SpecFilledButton("导出", onExport, icon = "export")
    }
}

/** 设计稿手机 pbar：菜单 / 标题 / 撤销 / 更多 */
@Composable
fun PhoneTopBar(state: EditorState, onMenu: () -> Unit, onMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).background(Spec.Surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SpecIconButton("menu", onMenu, hitSize = 36, iconSize = 22)
        Column(Modifier.padding(start = 4.dp)) {
            Text("走路循环", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurface)
            Text("第 ${state.frame.toInt()} 帧", fontSize = 11.sp, color = Spec.OnSurfaceVar)
        }
        Spacer(Modifier.weight(1f))
        SpecIconButton("undo", { if (state.canUndo) state.undo() }, enabled = state.canUndo, hitSize = 36, iconSize = 22)
        SpecIconButton("more", onMore, hitSize = 36, iconSize = 22)
    }
}
