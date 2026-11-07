package com.example.humanskeleton.art

import androidx.core.graphics.PathParser

/** 设计稿 ICONS：Material Symbols Rounded 风格，24dp 画板 / 1.8dp 描边，逐路径抽取 */
data class IconPathSpec(
    val d: String,
    val filled: Boolean,
    val width: Float,
    val dashOn: Float,
    val dashOff: Float,
    val alpha: Float
) {
    val path: android.graphics.Path by lazy(LazyThreadSafetyMode.NONE) { PathParser.createPathFromPathData(d) }
}

object SpecIcons {
    val all: Map<String, List<IconPathSpec>> = mapOf(
        "menu" to listOf(
            IconPathSpec("M4 7h16M4 12h16M4 17h16", false, 1.8f, 0f, 0f, 1f),
        ),
        "select" to listOf(
            IconPathSpec("M6 3.5 17.5 12l-5.4 1.2 2.7 5.8-2.6 1.2-2.7-5.8L6 17.4z", true, 1.8f, 0f, 0f, 1f),
        ),
        "bone" to listOf(
            IconPathSpec("M8.5 15.5 15.5 8.5", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M4.5 13.5 a2.4 2.4 0 1 0 4.8 0 a2.4 2.4 0 1 0 -4.8 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M8.1 17.1 a2.4 2.4 0 1 0 4.8 0 a2.4 2.4 0 1 0 -4.8 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M11.1 6.9 a2.4 2.4 0 1 0 4.8 0 a2.4 2.4 0 1 0 -4.8 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M14.7 10.5 a2.4 2.4 0 1 0 4.8 0 a2.4 2.4 0 1 0 -4.8 0", false, 1.8f, 0f, 0f, 1f),
        ),
        "link" to listOf(
            IconPathSpec("M10.5 13.5a3.6 3.6 0 0 1 0-5l2-2a3.6 3.6 0 0 1 5.1 5.1l-1.1 1.1", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M13.5 10.5a3.6 3.6 0 0 1 0 5l-2 2a3.6 3.6 0 0 1-5.1-5.1l1.1-1.1", false, 1.8f, 0f, 0f, 1f),
        ),
        "ik" to listOf(
            IconPathSpec("M8.8 12 a3.2 3.2 0 1 0 6.4 0 a3.2 3.2 0 1 0 -6.4 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M4.4 12 a7.6 7.6 0 1 0 15.2 0 a7.6 7.6 0 1 0 -15.2 0", false, 1.8f, 2f, 3f, 1f),
            IconPathSpec("M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2", false, 1.8f, 0f, 0f, 1f),
        ),
        "play" to listOf(
            IconPathSpec("M8 5.4 19 12 8 18.6z", true, 1.8f, 0f, 0f, 1f),
        ),
        "pause" to listOf(
            IconPathSpec("M9 5.5v13M15 5.5v13", false, 2.6f, 0f, 0f, 1f),
        ),
        "prev" to listOf(
            IconPathSpec("M16 5.5v13", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M17.5 12 8 6.2v11.6z", true, 1.8f, 0f, 0f, 1f),
        ),
        "next" to listOf(
            IconPathSpec("M8 5.5v13", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M6.5 12 16 6.2v11.6z", true, 1.8f, 0f, 0f, 1f),
        ),
        "undo" to listOf(
            IconPathSpec("M4.5 9.5h9a4.8 4.8 0 0 1 0 9.6H9", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M8 5.5 4 9.5l4 4", false, 1.8f, 0f, 0f, 1f),
        ),
        "redo" to listOf(
            IconPathSpec("M19.5 9.5h-9a4.8 4.8 0 0 0 0 9.6H15", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M16 5.5l4 4-4 4", false, 1.8f, 0f, 0f, 1f),
        ),
        "grid" to listOf(
            IconPathSpec("M4 9h16M4 15h16M9 4v16M15 4v16", false, 1.8f, 0f, 0f, 1f),
        ),
        "layers" to listOf(
            IconPathSpec("M12 3.6 3.6 8.2 12 12.8l8.4-4.6z", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M3.6 15.4 12 20l8.4-4.6", false, 1.8f, 0f, 0f, 1f),
        ),
        "image" to listOf(
            IconPathSpec("M6.1 4.5 H17.9 A2.6 2.6 0 0 1 20.5 7.1 V16.9 A2.6 2.6 0 0 1 17.9 19.5 H6.1 A2.6 2.6 0 0 1 3.5 16.9 V7.1 A2.6 2.6 0 0 1 6.1 4.5 Z", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M3.8 16.2 8.6 11.4l3.9 3.9 2.7-2.7 5 5", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M14 9.2 a1.4 1.4 0 1 0 2.8 0 a1.4 1.4 0 1 0 -2.8 0", true, 1.8f, 0f, 0f, 1f),
        ),
        "weight" to listOf(
            IconPathSpec("M4 12 a8 8 0 1 0 16 0 a8 8 0 1 0 -16 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M12 4a8 8 0 0 1 0 16z", true, 1.8f, 0f, 0f, 1f),
        ),
        "keyframe" to listOf(
            IconPathSpec("M12 3.6 20.4 12 12 20.4 3.6 12z", true, 1.8f, 0f, 0f, 1f),
        ),
        "onion" to listOf(
            IconPathSpec("M4 12 a5.4 5.4 0 1 0 10.8 0 a5.4 5.4 0 1 0 -10.8 0", false, 1.8f, 0f, 0f, .4f),
            IconPathSpec("M9.2 12 a5.4 5.4 0 1 0 10.8 0 a5.4 5.4 0 1 0 -10.8 0", false, 1.8f, 0f, 0f, 1f),
        ),
        "mirror" to listOf(
            IconPathSpec("M12 3v18", false, 1.8f, 2f, 2.6f, 1f),
            IconPathSpec("M9.6 6.4 4.2 12l5.4 5.6z", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M14.4 6.4 19.8 12l-5.4 5.6z", false, 1.8f, 0f, 0f, 1f),
        ),
        "zoom" to listOf(
            IconPathSpec("M4.6 11 a6.4 6.4 0 1 0 12.8 0 a6.4 6.4 0 1 0 -12.8 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M15.8 15.8 20 20", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M11 8.4v5.2M8.4 11h5.2", false, 1.8f, 0f, 0f, 1f),
        ),
        "more" to listOf(
            IconPathSpec("M10.1 5 a1.9 1.9 0 1 0 3.8 0 a1.9 1.9 0 1 0 -3.8 0", true, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M10.1 12 a1.9 1.9 0 1 0 3.8 0 a1.9 1.9 0 1 0 -3.8 0", true, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M10.1 19 a1.9 1.9 0 1 0 3.8 0 a1.9 1.9 0 1 0 -3.8 0", true, 1.8f, 0f, 0f, 1f),
        ),
        "export" to listOf(
            IconPathSpec("M12 15.5V3.8", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M8 7.8 12 3.8l4 4", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M4.5 15v3.4A1.6 1.6 0 0 0 6.1 20h11.8a1.6 1.6 0 0 0 1.6-1.6V15", false, 1.8f, 0f, 0f, 1f),
        ),
        "lock" to listOf(
            IconPathSpec("M7.2 10.4 H16.8 A2.6 2.6 0 0 1 19.4 13 V17.4 A2.6 2.6 0 0 1 16.8 20 H7.2 A2.6 2.6 0 0 1 4.6 17.4 V13 A2.6 2.6 0 0 1 7.2 10.4 Z", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M8 10.4V7.6a4 4 0 0 1 8 0v2.8", false, 1.8f, 0f, 0f, 1f),
        ),
        "eye" to listOf(
            IconPathSpec("M2.6 12S6.2 6.6 12 6.6 21.4 12 21.4 12 17.8 17.4 12 17.4 2.6 12 2.6 12z", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M9.2 12 a2.8 2.8 0 1 0 5.6 0 a2.8 2.8 0 1 0 -5.6 0", false, 1.8f, 0f, 0f, 1f),
        ),
        "add" to listOf(
            IconPathSpec("M12 5v14M5 12h14", false, 1.8f, 0f, 0f, 1f),
        ),
        "settings" to listOf(
            IconPathSpec("M9 12 a3 3 0 1 0 6 0 a3 3 0 1 0 -6 0", false, 1.8f, 0f, 0f, 1f),
            IconPathSpec("M12 2.8v2.4M12 18.8v2.4M4.5 7.5l2 1.2M17.5 15.3l2 1.2M4.5 16.5l2-1.2M17.5 8.7l2-1.2", false, 1.8f, 0f, 0f, 1f),
        ),
        "chevron" to listOf(
            IconPathSpec("M9.6 6 15.6 12l-6 6", false, 1.8f, 0f, 0f, 1f),
        ),
    )
}
