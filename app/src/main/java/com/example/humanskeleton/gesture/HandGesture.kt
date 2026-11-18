package com.example.humanskeleton.gesture

/**
 * 体感手势词汇表。
 *
 * 输入是 MediaPipe Hand Landmarker 的 21 个归一化关键点；静态手势由手指伸展状态分类，
 * 动态手势由掌心轨迹识别。每个手势在 [action] 里声明它对应的骨骼动作，
 * UI 的手势说明表直接从这里生成，保证「定义」与「映射」不脱节。
 */
enum class HandGesture(
    val label: String,
    val action: String,
    val continuous: Boolean,
    val hint: String
) {
    NONE("未检测到手", "将手放入取景框", false, "手掌正对前置摄像头，距离 30–60 cm"),
    UNKNOWN("未识别手势", "保持一个标准手势", false, "参照下方手势表调整手指姿态"),
    OPEN_PALM("张开手掌", "掌心移动 → 重心平移", true, "五指张开，平移手掌带动全身重心"),
    FIST("握拳", "拳头移动 → 旋转选中骨骼", true, "握拳后左右移动，实时旋转当前骨骼"),
    PINCH("捏合", "捏合距离 → 骨骼长度", true, "拇指食指靠拢、其余手指保持伸展，改变距离缩放骨骼"),
    POINT("指向", "切换到下一根骨骼", false, "只伸食指，每次稳定保持 0.4 秒切换一次"),
    VICTORY("V 字", "播放 / 暂停", false, "食指 + 中指伸展，稳定保持后触发"),
    OK("OK", "复位姿态", false, "拇指食指成圈、其余三指伸展"),
    SWIPE_LEFT("向左挥", "后退一帧", false, "手掌快速向左挥动"),
    SWIPE_RIGHT("向右挥", "前进一帧", false, "手掌快速向右挥动"),
    SWIPE_UP("向上挥", "抬起手臂", false, "手掌快速向上挥动"),
    SWIPE_DOWN("向下挥", "放下手臂", false, "手掌快速向下挥动"),
    CIRCLE("画圈", "转动躯干", true, "掌心画圆，累计转角连续驱动骨盆与脊柱");

    val isSwipe: Boolean
        get() = this == SWIPE_LEFT || this == SWIPE_RIGHT ||
                this == SWIPE_UP || this == SWIPE_DOWN

    companion object {
        /** 手势说明表按「连续操控 / 离散动作 / 动态手势」分组展示 */
        val guide: List<HandGesture> = entries.filter { it != NONE && it != UNKNOWN }
    }
}

/** 归一化关键点：x / y ∈ [0,1]，与预览画面坐标系一致（已做前置摄像头镜像） */
data class HandPoint(val x: Float, val y: Float)

/**
 * 单帧识别结果。所有字段都是纯数据，渲染层与状态层只依赖它，
 * 不直接依赖 MediaPipe 的类，便于离线复现与测试。
 */
data class HandFrame(
    val hasHand: Boolean = false,
    val gesture: HandGesture = HandGesture.NONE,
    /** 分类置信度 0..1，用于 HUD 与低置信度抑制 */
    val confidence: Float = 0f,
    /** 21 个关键点，供预览小窗叠加骨架 */
    val points: List<HandPoint> = emptyList(),
    /** 掌心质心（归一化） */
    val centroidX: Float = 0.5f,
    val centroidY: Float = 0.5f,
    /** 拇指尖—食指尖距离 / 手掌尺度，越小表示捏得越紧 */
    val pinch: Float = 1f,
    /** 掌心倾角（弧度），由腕点指向中指掌指关节的向量计算 */
    val tilt: Float = 0f,
    /** 张开度 0（握拳）.. 1（五指全开） */
    val openness: Float = 0f,
    /** 动态手势的连续量：画圈累计转角（弧度）或挥动速度 */
    val motion: Float = 0f,
    val handedness: String = "",
    val timestampMs: Long = 0L
) {
    companion object {
        val EMPTY = HandFrame()
    }
}

/** MediaPipe 的 21 点连接关系，用于预览小窗绘制手部骨架 */
val HAND_CONNECTIONS: List<Pair<Int, Int>> = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,          // 拇指
    0 to 5, 5 to 6, 6 to 7, 7 to 8,          // 食指
    5 to 9, 9 to 10, 10 to 11, 11 to 12,     // 中指
    9 to 13, 13 to 14, 14 to 15, 15 to 16,   // 无名指
    13 to 17, 17 to 18, 18 to 19, 19 to 20,  // 小指
    0 to 17                                   // 掌缘
)
