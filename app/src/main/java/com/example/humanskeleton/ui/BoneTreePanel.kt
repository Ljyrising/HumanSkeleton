package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.art.SpecIcon
import com.example.humanskeleton.model.EditorState

/** 设计稿左栏「骨骼层级」：16dp/级缩进、dot、关键帧计数、显示贴图/锁定 chip */
@Composable
fun BoneTreePanel(state: EditorState, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val byId = state.bones.associateBy { it.id }
    fun depth(id: String): Int {
        var d = 0
        var p = byId[id]?.parent
        while (p != null) { d++; p = byId[p]?.parent }
        return d
    }

    Column(modifier = modifier.fillMaxHeight().background(Spec.S1)) {
        PanelHeader("骨骼层级", onMore = {
            expanded = !expanded
            state.snackbar = if (expanded) "已展开骨骼树" else "已折叠骨骼树"
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            state.bones.forEachIndexed { idx, b ->
                val d = depth(b.id).coerceAtMost(5)
                if (!expanded && d >= 3) return@forEachIndexed
                val sel = state.selected == b.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (sel) Spec.SecondaryContainer else Spec.S1)
                        .clickable { state.selected = b.id }
                        .padding(start = (8 + d * 16).dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (idx == 0) {
                        SpecIcon(
                            "chevron", iconSize = 14.dp, tint = Spec.OnSurfaceVar,
                            modifier = Modifier.size(14.dp)
                        )
                    } else Spacer(Modifier.width(14.dp))
                    Box(
                        Modifier.size(7.dp).clip(CircleShape)
                            .background(if (b.id in state.binds) Spec.Primary else Spec.Outline)
                    )
                    Text(
                        state.labelOf(b), modifier = Modifier.weight(1f),
                        maxLines = 1, fontSize = 12.5.sp,
                        fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                        color = if (sel) Spec.OnSecondaryContainer else Spec.OnSurface
                    )
                    val k = state.keyframesOf(b.id).size
                    if (k > 0) Text("$k", fontSize = 10.sp, fontFamily = Mono, color = Spec.OnSurfaceVar)
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Spec.OutlineVar)
        Row(
            Modifier.fillMaxWidth().padding(10.dp, 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpecChip("显示贴图", { state.showArtwork = !state.showArtwork }, icon = "eye", on = state.showArtwork)
            SpecChip("锁定", {
                state.locked = !state.locked
                state.snackbar = if (state.locked) "骨骼已锁定，无法拖动" else "已解锁"
            }, icon = "lock", on = state.locked)
        }
    }
}
