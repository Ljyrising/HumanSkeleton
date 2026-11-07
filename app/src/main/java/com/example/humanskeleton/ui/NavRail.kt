package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.art.SpecIcon
import com.example.humanskeleton.model.EditorState
import com.example.humanskeleton.model.NavDest

private val items = listOf(
    NavDest.ASSEMBLE to "装配" to "bone",
    NavDest.ANIMATE to "动画" to "keyframe",
    NavDest.PARTS to "部件" to "image",
    NavDest.WEIGHTS to "权重" to "weight"
)

/** 设计稿左侧 Navigation rail（80dp） */
@Composable
fun NavRail(state: EditorState, onNewBone: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(80.dp).fillMaxHeight().background(Spec.Surface)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (item in items) {
            val (pair, ic) = item
            val (d, name) = pair
            val on = state.nav == d
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        state.nav = d
                        state.partsView = d == NavDest.WEIGHTS
                        if (d == NavDest.WEIGHTS) state.snackbar = "部件归属着色视图"
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(56.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) Spec.SecondaryContainer else Spec.Surface),
                    contentAlignment = Alignment.Center
                ) {
                    SpecIcon(
                        ic, iconSize = 24.dp,
                        tint = if (on) Spec.OnSecondaryContainer else Spec.OnSurfaceVar
                    )
                }
                Text(
                    name,
                    fontSize = 10.5.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    color = if (on) Spec.OnSurface else Spec.OnSurfaceVar
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        // rail-fab：新建骨骼
        Box(
            modifier = Modifier.size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Spec.PrimaryContainer)
                .clickable { onNewBone() },
            contentAlignment = Alignment.Center
        ) {
            SpecIcon("add", iconSize = 26.dp, tint = Spec.OnPrimaryContainer)
        }
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier.width(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { state.nav = NavDest.SETTINGS }
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (state.nav == NavDest.SETTINGS) Spec.SecondaryContainer else Spec.Surface),
                contentAlignment = Alignment.Center
            ) { SpecIcon("settings", iconSize = 24.dp, tint = Spec.OnSurfaceVar) }
            Text("设置", fontSize = 10.5.sp, color = Spec.OnSurfaceVar)
        }
    }
}

/** 设计稿手机：一级导航降级为底部导航栏 */
@Composable
fun PhoneNavBar(state: EditorState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(64.dp).background(Spec.S1),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((pair, icon) in items) {
            val (dest, name) = pair
            val on = state.nav == dest
            Column(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .clickable {
                        state.nav = dest
                        state.partsView = dest == NavDest.WEIGHTS
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(48.dp, 30.dp).clip(RoundedCornerShape(15.dp))
                        .background(if (on) Spec.SecondaryContainer else Spec.S1),
                    contentAlignment = Alignment.Center
                ) {
                    SpecIcon(
                        icon, iconSize = 20.dp,
                        tint = if (on) Spec.OnSecondaryContainer else Spec.OnSurfaceVar
                    )
                }
                Text(name, fontSize = 10.5.sp, color = if (on) Spec.OnSurface else Spec.OnSurfaceVar)
            }
        }
    }
}
