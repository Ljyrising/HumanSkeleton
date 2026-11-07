package com.example.humanskeleton.art

import androidx.core.graphics.PathParser

/**
 * 设计稿 2d-skeleton-ui-spec.html 中的骨架插画，逐路径原样抽取，未做任何重绘。
 * viewBox = 300 x 560；分组沿用设计稿的 b-head / b-spine / ... 部件划分。
 */
data class ArtStyle(
    val role: String,        // 语义类别，用于深色 / 部件归属着色变体
    val strokeColor: Long,  // ARGB
    val fillColor: Long,    // ARGB
    val width: Float,
    val alpha: Float,
    val filled: Boolean,
    val stroked: Boolean
)

sealed class ArtCmd {
    abstract val style: ArtStyle
    data class PathData(val d: String, override val style: ArtStyle) : ArtCmd() {
        /** android.graphics.Path 只解析一次并缓存 */
        val path: android.graphics.Path by lazy(LazyThreadSafetyMode.NONE) { PathParser.createPathFromPathData(d) }
    }
    data class Ellipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, override val style: ArtStyle) : ArtCmd()
    data class Circle(val cx: Float, val cy: Float, val r: Float, override val style: ArtStyle) : ArtCmd()
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float, override val style: ArtStyle) : ArtCmd()
}

data class ArtGroup(val id: String, val cmds: List<ArtCmd>)
data class RigSegment(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val highlight: Boolean)
data class RigNode(val x: Float, val y: Float, val r: Float, val anchor: Boolean)

object SkeletonArt {
    const val VB_W = 300f
    const val VB_H = 560f

    val groups: List<ArtGroup> = listOf(
        ArtGroup("ground-g", listOf(
            ArtCmd.Ellipse(150f, 522f, 78f, 10f, ArtStyle("ground", 0xFF1D1B20L, 0xFF1D1B20L, 0f, 0.07f, true, false)),
        )),
        ArtGroup("b-head", listOf(
            ArtCmd.PathData("M150 26c21 0 35 15 35 37 0 13-4 23-10 29v13c0 6-5 11-11 11h-28c-6 0-11-5-11-11V92c-6-6-10-16-10-29 0-22 14-37 35-37z", ArtStyle("skull", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.PathData("M133 104h34M140 104v7M147 104v7M154 104v7M161 104v7", ArtStyle("skull-d", 0xFF8A8778L, 0xFF8A8778L, 2.4f, 0.85f, false, true)),
            ArtCmd.Ellipse(136.5f, 66f, 8.6f, 9.8f, ArtStyle("socket", 0xFFB4B2A9L, 0xFFB4B2A9L, 0f, 0.55f, true, false)),
            ArtCmd.Ellipse(163.5f, 66f, 8.6f, 9.8f, ArtStyle("socket", 0xFFB4B2A9L, 0xFFB4B2A9L, 0f, 0.55f, true, false)),
            ArtCmd.PathData("M150 79l-5.4 12h10.8z", ArtStyle("skull-d", 0xFF8A8778L, 0xFF8A8778L, 2.4f, 0.85f, false, true)),
        )),
        ArtGroup("b-spine", listOf(
            ArtCmd.PathData("M150 118v182", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M150 118v182", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M150 150v90", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 150v90", ArtStyle("thin-in", 0xFFEDE8DFL, 0xFFF4F1EAL, 3.4f, 1f, false, true)),
        )),
        ArtGroup("b-ribs", listOf(
            ArtCmd.PathData("M150 152C124 150 106 164 106 184", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 152C176 150 194 164 194 184", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 169C122 167 102 183 102 204", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 169C178 167 198 183 198 204", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 186C121 184 100 202 100 224", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 186C179 184 200 202 200 224", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 203C123 201 104 217 104 237", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 203C177 201 196 217 196 237", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 220C127 218 111 231 111 247", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M150 220C173 218 189 231 189 247", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
        )),
        ArtGroup("b-clav", listOf(
            ArtCmd.PathData("M150 142C133 134 119 133 108 138", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M150 142C133 134 119 133 108 138", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M150 142C167 134 181 133 192 138", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M150 142C167 134 181 133 192 138", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
        )),
        ArtGroup("b-armL", listOf(
            ArtCmd.PathData("M107 145c-5 27-10 48-13 71", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M107 145c-5 27-10 48-13 71", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M94 216c-3 27-6 49-8 70", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M94 216c-3 27-6 49-8 70", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M86 286c-3 12-5 22-5 32", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M86 286c-3 12-5 22-5 32", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 3.4f, 1f, false, true)),
            ArtCmd.PathData("M81 318l-4 8M81 318l1 9M81 318l7 7", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
        )),
        ArtGroup("b-armR", listOf(
            ArtCmd.PathData("M193 145c5 27 10 48 13 71", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M193 145c5 27 10 48 13 71", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M206 216c3 27 6 49 8 70", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M206 216c3 27 6 49 8 70", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M214 286c3 12 5 22 5 32", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M214 286c3 12 5 22 5 32", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 3.4f, 1f, false, true)),
            ArtCmd.PathData("M219 318l4 8M219 318l-1 9M219 318l-7 7", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
        )),
        ArtGroup("b-pelvis", listOf(
            ArtCmd.PathData("M150 300c-16 0-32-8-38-24", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M150 300c-16 0-32-8-38-24", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M150 300c16 0 32-8 38-24", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M150 300c16 0 32-8 38-24", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M112 276c6 12 20 20 38 20 18 0 32-8 38-20", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
        )),
        ArtGroup("b-legL", listOf(
            ArtCmd.PathData("M129 306c-4 33-6 58-7 86", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M129 306c-4 33-6 58-7 86", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M122 392c-2 32-3 56-4 82", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M122 392c-2 32-3 56-4 82", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M130 398c-1 30-2 54-3 76", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
            ArtCmd.PathData("M130 398c-1 30-2 54-3 76", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 2.4f, 1f, false, true)),
            ArtCmd.PathData("M118 474c-8 8-16 16-20 24", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M118 474c-8 8-16 16-20 24", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 3.4f, 1f, false, true)),
            ArtCmd.PathData("M98 498h12", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
        )),
        ArtGroup("b-legR", listOf(
            ArtCmd.PathData("M171 306c4 33 6 58 7 86", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M171 306c4 33 6 58 7 86", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M178 392c2 32 3 56 4 82", ArtStyle("tube", 0xFFB4B2A9L, 0xFFB4B2A9L, 10f, 1f, false, true)),
            ArtCmd.PathData("M178 392c2 32 3 56 4 82", ArtStyle("tube-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 6f, 1f, false, true)),
            ArtCmd.PathData("M170 398c1 30 2 54 3 76", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
            ArtCmd.PathData("M170 398c1 30 2 54 3 76", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 2.4f, 1f, false, true)),
            ArtCmd.PathData("M182 474c8 8 16 16 20 24", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 7f, 1f, false, true)),
            ArtCmd.PathData("M182 474c8 8 16 16 20 24", ArtStyle("thin-in", 0xFFF4F1EAL, 0xFFF4F1EAL, 3.4f, 1f, false, true)),
            ArtCmd.PathData("M202 498h-12", ArtStyle("thin", 0xFFB4B2A9L, 0xFFB4B2A9L, 5f, 1f, false, true)),
        )),
        ArtGroup("b-joints", listOf(
            ArtCmd.Circle(107f, 145f, 4.4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(193f, 145f, 4.4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(94f, 216f, 4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(206f, 216f, 4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(86f, 286f, 3.6f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(214f, 286f, 3.6f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(129f, 306f, 4.2f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(171f, 306f, 4.2f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(122f, 392f, 4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(178f, 392f, 4f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(118f, 474f, 3.6f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
            ArtCmd.Circle(182f, 474f, 3.6f, ArtStyle("joint", 0xFFB4B2A9L, 0xFFF4F1EAL, 3f, 1f, true, true)),
        )),
    )

    /** 设计稿 RIG 骨骼链（primary 3.2dp，圆头） */
    val rig: List<RigSegment> = listOf(
        RigSegment(150f, 300f, 150f, 142f, false),
        RigSegment(150f, 142f, 150f, 100f, false),
        RigSegment(150f, 142f, 107f, 145f, false),
        RigSegment(150f, 142f, 193f, 145f, false),
        RigSegment(107f, 145f, 94f, 216f, false),
        RigSegment(193f, 145f, 206f, 216f, false),
        RigSegment(94f, 216f, 86f, 286f, false),
        RigSegment(206f, 216f, 214f, 286f, false),
        RigSegment(86f, 286f, 81f, 316f, false),
        RigSegment(214f, 286f, 219f, 316f, false),
        RigSegment(129f, 306f, 122f, 392f, false),
        RigSegment(171f, 306f, 178f, 392f, false),
        RigSegment(122f, 392f, 118f, 474f, false),
        RigSegment(178f, 392f, 182f, 474f, false),
        RigSegment(118f, 474f, 100f, 498f, false),
        RigSegment(182f, 474f, 200f, 498f, false),
    )

    /** 高亮线（设计稿 hl，用于骨盆两侧） */
    val rigHighlights: List<RigSegment> = listOf(
        RigSegment(129f, 306f, 150f, 300f, true),
        RigSegment(171f, 306f, 150f, 300f, true),
    )

    val rigNodes: List<RigNode> = listOf(
        RigNode(150f, 300f, 5.6f, true),
        RigNode(150f, 142f, 4.3f, false),
        RigNode(150f, 100f, 4.3f, false),
        RigNode(107f, 145f, 4.3f, false),
        RigNode(193f, 145f, 4.3f, false),
        RigNode(94f, 216f, 4f, false),
        RigNode(206f, 216f, 4f, false),
        RigNode(86f, 286f, 3.8f, false),
        RigNode(214f, 286f, 3.8f, false),
        RigNode(129f, 306f, 4f, false),
        RigNode(171f, 306f, 4f, false),
        RigNode(122f, 392f, 3.8f, false),
        RigNode(178f, 392f, 3.8f, false),
        RigNode(118f, 474f, 3.6f, false),
        RigNode(182f, 474f, 3.6f, false),
    )

    /** 部件归属着色视图（设计稿 .v-parts） */
    val partTint = mapOf(
        "b-armL" to 0xFF1D9E75L,
        "b-armR" to 0xFF378ADDL,
        "b-legL" to 0xFFBA7517L,
        "b-legR" to 0xFFD4537EL,
        "b-head" to 0xFF7D5260L,
        "b-spine" to 0xFF6750A4L,
        "b-ribs" to 0xFF6750A4L,
        "b-pelvis" to 0xFF6750A4L,
        "b-clav" to 0xFF6750A4L,
    )

    /** 深色画布（设计稿 .sk-dark） */
    val darkTint = mapOf(
        "tube" to 0xFF57525CL,
        "thin" to 0xFF57525CL,
        "skull-d" to 0xFF8A8590L,
        "socket" to 0xFF57525CL,
        "joint" to 0xFF57525CL,
    )
    val darkInner = 0xFFE6E0E9L
    val darkRig = 0xFFD0BCFFL
}
