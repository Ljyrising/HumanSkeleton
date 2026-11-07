package com.example.humanskeleton.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.humanskeleton.art.SpecIcon

/** 设计稿固定语义色板（seed #6750A4），界面只用语义角色 */
object Spec {
    val Primary = Color(0xFF6750A4)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFEADDFF)
    val OnPrimaryContainer = Color(0xFF21005D)
    val Secondary = Color(0xFF625B71)
    val SecondaryContainer = Color(0xFFE8DEF8)
    val OnSecondaryContainer = Color(0xFF1D192B)
    val Tertiary = Color(0xFF7D5260)
    val TertiaryContainer = Color(0xFFFFD8E4)
    val Error = Color(0xFFB3261E)
    val Surface = Color(0xFFFEF7FF)
    val S1 = Color(0xFFF7F2FA)
    val S2 = Color(0xFFF3EDF7)
    val S3 = Color(0xFFECE6F0)
    val OnSurface = Color(0xFF1D1B20)
    val OnSurfaceVar = Color(0xFF49454F)
    val Outline = Color(0xFF79747E)
    val OutlineVar = Color(0xFFCAC4D0)
    val InverseSurface = Color(0xFF322F35)
    val InverseOnSurface = Color(0xFFF5EFF7)
    val Bone = Color(0xFFF4F1EA)
    val BoneLine = Color(0xFFB4B2A9)
    val CanvasDark = Color(0xFF1D1B20)
}

val Mono = FontFamily.Monospace

@Composable
fun SpecIconButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    enabled: Boolean = true,
    iconSize: Int = 24,
    hitSize: Int = 40,
    description: String = icon
) {
    Box(
        modifier = modifier
            .size(hitSize.dp)
            .clip(CircleShape)
            .background(if (on) Spec.Primary else Color.Transparent)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        SpecIcon(
            icon,
            iconSize = iconSize.dp,
            tint = when {
                !enabled -> Spec.OnSurfaceVar.copy(alpha = 0.38f)
                on -> Color.White
                else -> Spec.OnSurfaceVar
            }
        )
    }
}

@Composable
fun SpecFilledButton(
    text: String,
    onClick: () -> Unit,
    icon: String? = null,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Spec.Primary, contentColor = Color.White)
    ) {
        if (icon != null) { SpecIcon(icon, iconSize = 18.dp, tint = Color.White); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SpecTonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledTonalButton(
        onClick = onClick, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Spec.SecondaryContainer, contentColor = Spec.OnSecondaryContainer
        )
    ) { Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun SpecOutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick, modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Spec.Primary)
    ) { Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun SpecTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.height(40.dp)) {
        Text(text, color = Spec.Primary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
    }
}

/** 设计稿 chip-s：小号状态 chip，选中为 secondary-container */
@Composable
fun SpecChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    on: Boolean = false,
    small: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) Spec.SecondaryContainer else Color.Transparent)
            .then(if (!on) Modifier.border(1.dp, Spec.OutlineVar, RoundedCornerShape(8.dp)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 3.dp else 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) SpecIcon(
            icon, iconSize = 13.dp,
            tint = if (on) Spec.OnSecondaryContainer else Spec.OnSurfaceVar
        )
        Text(
            text,
            fontSize = if (small) 10.5.sp else 11.5.sp,
            fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
            color = if (on) Spec.OnSecondaryContainer else Spec.OnSurfaceVar
        )
    }
}

/** 设计稿 seg：分段按钮，选中项 primary 实底 */
@Composable
fun SpecSeg(
    items: List<Pair<String, String?>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Spec.S2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { i, (text, icon) ->
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (i == selectedIndex) Spec.Primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icon != null) SpecIcon(
                    icon, iconSize = 16.dp,
                    tint = if (i == selectedIndex) Color.White else Spec.OnSurfaceVar
                )
                Text(
                    text, fontSize = 12.sp,
                    fontWeight = if (i == selectedIndex) FontWeight.Medium else FontWeight.Normal,
                    color = if (i == selectedIndex) Color.White else Spec.OnSurfaceVar
                )
            }
        }
    }
}

/** 设计稿 field：1dp outline 圆角 8dp 的数值显示框 */
@Composable
fun SpecField(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Spec.Surface)
            .border(1.dp, Spec.Outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Spec.OnSurface)
        Text(value, fontSize = 12.5.sp, fontFamily = Mono, color = Spec.OnSurfaceVar)
    }
}

@Composable
fun SpecSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    display: String = "",
    leftLabel: String? = null,
    rightLabel: String? = null
) {
    Column(modifier = modifier) {
        if (leftLabel != null || rightLabel != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(leftLabel ?: "", fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
                Text(rightLabel ?: "", fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontSize = 11.5.sp, color = Spec.OnSurfaceVar)
                Text(display, fontSize = 11.5.sp, fontFamily = Mono, color = Spec.OnSurface)
            }
        }
        Slider(
            value = value, onValueChange = onValueChange, valueRange = range,
            modifier = Modifier.height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = Spec.Primary, activeTrackColor = Spec.Primary,
                inactiveTrackColor = Color(0xFFE7E0EC)
            )
        )
    }
}

@Composable
fun PanelHeader(title: String, onMore: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(48.dp).padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = Spec.OnSurface)
        SpecIconButton("more", onMore, iconSize = 20, hitSize = 32)
    }
}

/** 设计稿 Snackbar：inverse-surface 底 + primary 动作文字 */
@Composable
fun SpecSnackbar(message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Spec.InverseSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(message, fontSize = 12.5.sp, color = Spec.InverseOnSurface)
        if (action != null && onAction != null) {
            Text(
                action, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                color = Color(0xFFD0BCFF),
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}
