package com.example.humanskeleton.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 2D 仿射矩阵（齐次坐标，作用于列向量）：
 *   | a c e |
 *   | b d f |
 *   | 0 0 1 |
 * 表示：x' = a*x + c*y + e ;  y' = b*x + d*y + f
 *
 * 与骨骼层级前向动力学（FK）所需的 translate/rotate/scale/concat/invert/map 一一对应。
 * 该实现与浏览器原型 / Node 单测中验证过的矩阵逻辑完全一致。
 */
data class Mx(
    val a: Float, val b: Float, val c: Float, val d: Float,
    val e: Float, val f: Float
) {
    fun translate(tx: Float, ty: Float): Mx =
        Mx(a, b, c, d, a * tx + c * ty + e, b * tx + d * ty + f)

    fun rotate(rad: Float): Mx {
        val s = sin(rad); val co = cos(rad)
        return Mx(
            a * co + c * s,
            b * co + d * s,
            -a * s + c * co,
            -b * s + d * co,
            e, f
        )
    }

    fun scale(sx: Float, sy: Float): Mx =
        Mx(a * sx, b * sx, c * sy, d * sy, e, f)

    /** this · o （先应用 o，再应用 this 的语义下用于父链累积） */
    fun concat(o: Mx): Mx = Mx(
        a * o.a + c * o.b,
        b * o.a + d * o.b,
        a * o.c + c * o.d,
        b * o.c + d * o.d,
        a * o.e + c * o.f + e,
        b * o.e + d * o.f + f
    )

    fun invert(): Mx? {
        val det = a * d - b * c
        if (abs(det) < 1e-9f) return null // 退化（如缩放为 0）保护
        val ia = d / det
        val ib = -b / det
        val ic = -c / det
        val id = a / det
        return Mx(ia, ib, ic, id, -(ia * e + ic * f), -(ib * e + id * f))
    }

    fun map(x: Float, y: Float): Offset =
        Offset(a * x + c * y + e, b * x + d * y + f)

    /** 该矩阵的旋转角（弧度），用于父子旋转解算 */
    fun angle(): Float = atan2(b, a)

    /** 屏幕像素平移（直接加到位移分量，不随缩放放大） */
    fun panBy(dx: Float, dy: Float): Mx = Mx(a, b, c, d, e + dx, f + dy)

    /** 以屏幕坐标 centroid 为中心缩放 factor 倍（保持该点下世界坐标不动） */
    fun zoomAbout(factor: Float, centroid: Offset): Mx {
        val nz = (a * factor).coerceIn(0.3f, 5f)
        val inv = this.invert() ?: return this
        val w = inv.map(centroid.x, centroid.y)
        return Mx(nz, 0f, 0f, nz, centroid.x - nz * w.x, centroid.y - nz * w.y)
    }

    companion object {
        val Identity = Mx(1f, 0f, 0f, 1f, 0f, 0f)
    }
}

/** 骨骼局部 -y 方向（向上）映射到世界角度的便捷函数 */
fun atan2(x: Float, y: Float): Float = kotlin.math.atan2(x, y)
