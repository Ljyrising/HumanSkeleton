package com.example.humanskeleton.gesture

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * 掌心轨迹追踪：把连续帧组织成动态手势（四向挥动、画圈），并给离散手势加「稳定保持」判定。
 *
 * 三层防误触：
 *  1) 质心做 EMA 平滑，抑制关键点抖动；
 *  2) 挥动要求窗口内净位移、方向优势、时间窗三者同时成立，并有 700ms 冷却；
 *  3) 离散手势（指向 / V 字 / OK）必须稳定保持 380ms，且两次触发间隔 ≥ 900ms。
 */
class HandMotionTracker(private val onDiscrete: (HandGesture) -> Unit) {

    private data class Sample(val x: Float, val y: Float, val t: Long)

    private val window = ArrayDeque<Sample>()
    private var smoothX = 0.5f
    private var smoothY = 0.5f
    private var hasSmooth = false

    private var lastSwipeAt = 0L
    private var lastDiscreteAt = 0L
    private var dwellGesture = HandGesture.NONE
    private var dwellSince = 0L

    private var lastAngle = 0f
    private var hasAngle = false
    private var circleAccum = 0f
    private var circleUntil = 0L

    companion object {
        private const val WINDOW_MS = 620L
        private const val SWIPE_DISTANCE = 0.20f
        private const val SWIPE_COOLDOWN = 700L
        private const val DWELL_MS = 380L
        private const val DISCRETE_COOLDOWN = 900L
        private const val CIRCLE_TURN = 4.6f      // ≈ 0.73 圈，避免小幅抖动被当成画圈
        private const val CIRCLE_HOLD_MS = 1400L
    }

    fun reset() {
        window.clear()
        hasSmooth = false
        dwellGesture = HandGesture.NONE
        hasAngle = false
        circleAccum = 0f
        circleUntil = 0L
    }

    /**
     * 输入 [GestureClassifier] 的单帧静态结果，返回「附加了动态手势」的结果。
     * 连续操控字段（质心 / 捏合 / 倾角）原样保留，交由 EditorState 驱动骨骼。
     */
    fun update(frame: HandFrame): HandFrame {
        if (!frame.hasHand) {
            reset()
            return frame
        }
        val now = frame.timestampMs

        // ① EMA 平滑质心
        if (!hasSmooth) {
            smoothX = frame.centroidX; smoothY = frame.centroidY; hasSmooth = true
        } else {
            smoothX = smoothX * 0.68f + frame.centroidX * 0.32f
            smoothY = smoothY * 0.68f + frame.centroidY * 0.32f
        }
        window.addLast(Sample(smoothX, smoothY, now))
        while (window.size > 2 && now - window.first().t > WINDOW_MS) window.removeFirst()

        // ② 画圈：以窗口均值为圆心累计转角
        if (window.size >= 5) {
            var mx = 0f; var my = 0f
            for (s in window) { mx += s.x; my += s.y }
            mx /= window.size; my /= window.size
            val radius = hypot(smoothX - mx, smoothY - my)
            val angle = atan2(smoothY - my, smoothX - mx)
            if (hasAngle && radius > 0.025f) {
                var delta = angle - lastAngle
                if (delta > PI) delta -= (2f * PI.toFloat())
                if (delta < -PI) delta += (2f * PI.toFloat())
                if (abs(delta) < 1.2f) circleAccum += delta   // 丢弃瞬移造成的巨大跳变
            }
            lastAngle = angle; hasAngle = true
            if (abs(circleAccum) >= CIRCLE_TURN) {
                circleUntil = now + CIRCLE_HOLD_MS
                circleAccum *= 0.35f                          // 保留方向，避免数值无限增长
            }
        }

        // ③ 四向挥动：窗口首尾净位移 + 方向优势 + 冷却
        var dynamic: HandGesture? = null
        if (window.size >= 3 && now - lastSwipeAt > SWIPE_COOLDOWN) {
            val a = window.first(); val b = window.last()
            val dt = b.t - a.t
            val dx = b.x - a.x; val dy = b.y - a.y
            if (dt in 90..WINDOW_MS) {
                dynamic = when {
                    abs(dx) > SWIPE_DISTANCE && abs(dx) > abs(dy) * 1.5f ->
                        if (dx > 0f) HandGesture.SWIPE_RIGHT else HandGesture.SWIPE_LEFT
                    abs(dy) > SWIPE_DISTANCE && abs(dy) > abs(dx) * 1.5f ->
                        if (dy > 0f) HandGesture.SWIPE_DOWN else HandGesture.SWIPE_UP
                    else -> null
                }
                if (dynamic != null) {
                    lastSwipeAt = now
                    onDiscrete(dynamic)
                    window.clear()
                    dwellGesture = HandGesture.NONE
                }
            }
        }

        // ④ 离散静态手势：稳定保持后才触发一次，避免随手一晃反复切换
        val g = frame.gesture
        val eligible = g != HandGesture.NONE && g != HandGesture.UNKNOWN &&
                !g.continuous && !g.isSwipe && frame.confidence >= 0.55f
        if (eligible && dynamic == null) {
            if (g == dwellGesture) {
                if (now - dwellSince >= DWELL_MS && now - lastDiscreteAt >= DISCRETE_COOLDOWN) {
                    lastDiscreteAt = now
                    onDiscrete(g)
                    dwellSince = now
                }
            } else {
                dwellGesture = g
                dwellSince = now
            }
        } else {
            dwellGesture = HandGesture.NONE
        }

        // ⑤ 画圈期间覆盖为连续手势，累计转角作为旋转量交给状态层
        return when {
            dynamic != null -> frame.copy(gesture = dynamic, motion = 0f)
            now < circleUntil -> frame.copy(gesture = HandGesture.CIRCLE, motion = circleAccum)
            else -> frame
        }
    }
}
