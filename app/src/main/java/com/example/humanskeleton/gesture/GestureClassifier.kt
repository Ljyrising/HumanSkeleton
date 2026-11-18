package com.example.humanskeleton.gesture

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

/**
 * 由 MediaPipe 的 21 个手部关键点分类静态手势。
 *
 * 判定全部基于「距离比值」而不是 y 坐标大小，因此手掌旋转、倾斜或左右手互换都不影响结果。
 * 手掌尺度 palm = |wrist(0) — middleMCP(9)|，用来把所有阈值归一化，与人物离摄像头的远近无关。
 *
 * 关键点索引：0 腕；1-4 拇指；5-8 食指；9-12 中指；13-16 无名指；17-20 小指。
 */
object GestureClassifier {

    private const val EXTEND_RATIO = 1.10f
    /** 拇指尖—食指尖 / 手掌尺度低于此值视为捏合；收紧一点可避免攥拳时误判 */
    private const val PINCH_THRESHOLD = 0.42f

    fun classify(
        points: List<HandPoint>,
        handedness: String = "",
        handScore: Float = 0.9f,
        timestampMs: Long = 0L
    ): HandFrame {
        if (points.size < 21) return HandFrame(timestampMs = timestampMs)

        fun d(a: Int, b: Int): Float {
            val p = points[a]; val q = points[b]
            return hypot(q.x - p.x, q.y - p.y)
        }

        val palm = d(0, 9).coerceAtLeast(1e-4f)

        // 四指：指尖到腕 / PIP 到腕，且指尖到 MCP / PIP 到 MCP，双条件避免「弯着但朝外」误判
        fun fourFinger(tip: Int, pip: Int, mcp: Int): Boolean =
            d(tip, 0) > d(pip, 0) * EXTEND_RATIO &&
                    d(tip, mcp) > d(pip, mcp) * 1.05f

        val index = fourFinger(8, 6, 5)
        val middle = fourFinger(12, 10, 9)
        val ring = fourFinger(16, 14, 13)
        val pinky = fourFinger(20, 18, 17)
        // 拇指：张开时指尖明显远离小指掌指关节
        val thumb = d(4, 17) > d(3, 17) + 0.16f * palm

        val extended = booleanArrayOf(thumb, index, middle, ring, pinky)
        val count = extended.count { it }
        val othersOpen = listOf(middle, ring, pinky).count { it }
        val pinch = (d(4, 8) / palm).coerceIn(0f, 3f)
        val pinching = pinch < PINCH_THRESHOLD

        val gesture = when {
            // ① 捏合类：握拳时拇指本来就会贴住食指，单看捏合距离无法区分两者，
            //    因此用「其余三指是否伸展」区分 OK / 捏合 / 攥拳。
            pinching && othersOpen == 3 -> HandGesture.OK
            pinching && othersOpen >= 1 -> HandGesture.PINCH
            pinching -> HandGesture.FIST
            // ② 指型类必须先于 count <= 1 判定：只伸食指时 count 恰好为 1，
            //    若先判 FIST，「指向」手势将永远不会触发。
            index && middle && !ring && !pinky -> HandGesture.VICTORY
            index && !middle && !ring && !pinky -> HandGesture.POINT
            count >= 4 -> HandGesture.OPEN_PALM
            count <= 1 -> HandGesture.FIST
            else -> HandGesture.UNKNOWN
        }

        // 掌心质心用五个掌骨点，比全 21 点平均更稳定（手指开合不会让质心乱跳）
        val palmIds = intArrayOf(0, 5, 9, 13, 17)
        var cx = 0f; var cy = 0f
        for (i in palmIds) { cx += points[i].x; cy += points[i].y }
        cx /= palmIds.size; cy /= palmIds.size

        val wrist = points[0]
        val middleMcp = points[9]
        val tilt = atan2(middleMcp.y - wrist.y, middleMcp.x - wrist.x)

        // 置信度：手部检测分数为主，分类裕度为辅；手掌过小（离得太远）时降权
        val sizeFactor = min(1f, palm / 0.12f)
        val confidence = (0.45f * handScore + 0.35f * sizeFactor + 0.20f).coerceIn(0f, 1f)

        return HandFrame(
            hasHand = true,
            gesture = gesture,
            confidence = confidence,
            points = points,
            centroidX = cx,
            centroidY = cy,
            pinch = pinch,
            tilt = tilt,
            openness = count / 5f,
            handedness = handedness,
            timestampMs = timestampMs
        )
    }
}
