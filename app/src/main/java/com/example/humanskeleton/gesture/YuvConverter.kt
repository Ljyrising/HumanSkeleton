package com.example.humanskeleton.gesture

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * CameraX ImageAnalysis 的 YUV_420_888 → ARGB_8888 Bitmap 转换。
 *
 * 不使用任何外部图像库：按 plane 的 rowStride / pixelStride 逐像素取样，
 * 因此兼容 U/V 平面交错（NV21 半平面）与独立平面两种设备实现。
 * MediaPipe 的 HandLandmarker 接收 BitmapImageBuilder 构造的 MPImage，故必须先转成 Bitmap。
 */
object YuvConverter {

    fun toBitmap(image: ImageProxy, mirror: Boolean): Bitmap {
        val w = image.width
        val h = image.height
        val planes = image.planes
        val yBuf = planes[0].buffer
        val uBuf = planes[1].buffer
        val vBuf = planes[2].buffer
        val yRow = planes[0].rowStride; val yPix = planes[0].pixelStride
        val uRow = planes[1].rowStride; val uPix = planes[1].pixelStride
        val vRow = planes[2].rowStride; val vPix = planes[2].pixelStride

        val argb = IntArray(w * h)
        var out = 0
        for (row in 0 until h) {
            val yOff = row * yRow
            val uvRow = row shr 1
            val uOff = uvRow * uRow
            val vOff = uvRow * vRow
            for (col in 0 until w) {
                val y = (yBuf.get(yOff + col * yPix).toInt() and 0xFF) - 16
                val uvCol = col shr 1
                val u = (uBuf.get(uOff + uvCol * uPix).toInt() and 0xFF) - 128
                val v = (vBuf.get(vOff + uvCol * vPix).toInt() and 0xFF) - 128

                // BT.601 有限范围 YUV → RGB，定点整数近似
                val yv = 1192 * y
                val r = ((yv + 1634 * v) shr 10).coerceIn(0, 255)
                val g = ((yv - 833 * v - 400 * u) shr 10).coerceIn(0, 255)
                val b = ((yv + 2066 * u) shr 10).coerceIn(0, 255)
                argb[out++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val src = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        src.setPixels(argb, 0, w, 0, 0, w, h)

        val rotation = image.imageInfo.rotationDegrees.toFloat()
        if (!mirror && rotation == 0f) return src

        val m = Matrix()
        // 前置摄像头做水平镜像，使「用户向右挥手」与画面、坐标一致（照镜子体验）
        if (mirror) m.postScale(-1f, 1f, w / 2f, h / 2f)
        if (rotation != 0f) m.postRotate(rotation)
        val dst = Bitmap.createBitmap(src, 0, 0, w, h, m, true)
        if (dst !== src) src.recycle()
        return dst
    }
}
