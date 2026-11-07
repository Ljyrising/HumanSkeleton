package com.example.humanskeleton.art

import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas

/** 把设计稿的一条图标路径变成 android.graphics.Paint */
private fun makePaint(spec: IconPathSpec, colorInt: Int): Paint = Paint().apply {
    isAntiAlias = true
    style = if (spec.filled) Paint.Style.FILL else Paint.Style.STROKE
    color = colorInt
    alpha = (spec.alpha * 255f).toInt().coerceIn(0, 255)
    strokeWidth = spec.width
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
    if (!spec.filled && spec.dashOn > 0f && spec.dashOff > 0f) {
        pathEffect = DashPathEffect(floatArrayOf(spec.dashOn, spec.dashOff), 0f)
    }
}

/**
 * 设计稿 ICONS 中的图标：24dp 画板、1.8dp 描边、圆头圆角、单色。
 * 逐路径绘制，保证与设计稿图标完全一致（不改用 Material 自带图标）。
 */
@Composable
fun SpecIcon(
    name: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = LocalContentColor.current
) {
    val colorInt = tint.toArgb()
    val pairs = remember(name, colorInt) {
        (SpecIcons.all[name] ?: emptyList()).map { it to makePaint(it, colorInt) }
    }
    Canvas(modifier.then(Modifier.size(iconSize))) {
        if (pairs.isEmpty()) return@Canvas
        val s = size.minDimension / 24f
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            nc.save()
            nc.scale(s, s)
            for ((spec, paint) in pairs) nc.drawPath(spec.path, paint)
            nc.restore()
        }
    }
}

/** 在 DrawScope 内绘制一个 24 单位画板的设计稿图标（用于画布上的手柄/标记） */
fun DrawScope.drawSpecIcon(
    name: String,
    centerX: Float,
    centerY: Float,
    sizePx: Float,
    color: Color,
    paintCache: MutableMap<String, List<Pair<IconPathSpec, Paint>>>
) {
    val key = "$name-${color.toArgb()}"
    val pairs = paintCache.getOrPut(key) {
        (SpecIcons.all[name] ?: emptyList()).map { it to makePaint(it, color.toArgb()) }
    }
    val s = sizePx / 24f
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.translate(centerX - sizePx / 2f, centerY - sizePx / 2f)
        nc.scale(s, s)
        for ((spec, paint) in pairs) nc.drawPath(spec.path, paint)
        nc.restore()
    }
}
