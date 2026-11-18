package com.example.humanskeleton.gesture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.ErrorListener
import com.google.mediapipe.tasks.core.OutputHandler
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX + MediaPipe Hand Landmarker 的采集管线。
 *
 * 流程：前置摄像头 → ImageAnalysis(YUV_420_888) → YuvConverter → MPImage
 *      → HandLandmarker.detectAsync(LIVE_STREAM) → 21 关键点
 *      → GestureClassifier（静态手势） → HandMotionTracker（动态手势 / 去抖）
 *      → 主线程回调 [onFrame] / [onDiscrete]。
 *
 * 模型 hand_landmarker.task 打包在 assets，完全离线推理，不依赖 Google Play 服务或网络。
 */
class HandTrackingController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val onFrame: (HandFrame) -> Unit,
    private val onDiscrete: (HandGesture) -> Unit,
    private val onStatus: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val tracker = HandMotionTracker { g -> main.post { onDiscrete(g) } }

    @Volatile
    private var landmarker: HandLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var started = false

    @Volatile
    private var closed = false

    @Volatile
    private var lastSubmitAt = 0L

    @Volatile
    private var lastTimestamp = 0L

    companion object {
        private const val MODEL_ASSET = "hand_landmarker.task"
        private const val MIN_FRAME_INTERVAL_MS = 55L   // 推理上限约 18fps，给 UI 留出余量
    }

    fun start() {
        if (started || closed) return
        started = true
        main.post { onStatus("正在加载手部模型…") }
        // hand_landmarker.task 约 7.8MB，同步 createFromOptions 会卡主线程；
        // 放在分析线程加载，完成后再回主线程绑定相机（bindToLifecycle 必须主线程）。
        executor.execute {
            try {
                val base = BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .setDelegate(Delegate.CPU)
                    .build()
                val options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(base)
                    .setRunningMode(RunningMode.LIVE_STREAM)
                    .setNumHands(1)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setResultListener(
                        OutputHandler.ResultListener<HandLandmarkerResult, MPImage> { result, _ ->
                            handleResult(result)
                        }
                    )
                    .setErrorListener(ErrorListener { e ->
                        main.post { onStatus("识别异常：${e.message ?: "未知错误"}") }
                    })
                    .build()
                landmarker = HandLandmarker.createFromOptions(context, options)
                main.post { openCamera() }
            } catch (e: Exception) {
                main.post { onStatus("模型加载失败：${e.message ?: "未知错误"}") }
            }
        }
    }

    private fun openCamera() {
        if (closed) return
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (closed) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider
                bind(provider)
                main.post { onStatus("手势识别已开启") }
            } catch (e: Exception) {
                main.post { onStatus("相机启动失败：${e.message ?: "未知错误"}") }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @Suppress("DEPRECATION")
    private fun bind(provider: ProcessCameraProvider) {
        val front = CameraSelector.DEFAULT_FRONT_CAMERA
        val selector = if (provider.hasCamera(front)) front else CameraSelector.DEFAULT_BACK_CAMERA
        val mirrored = selector == front

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(480, 360))
            .build()

        analysis.setAnalyzer(executor) { proxy ->
            val lm = landmarker
            val now = SystemClock.uptimeMillis()
            if (lm == null || closed || now - lastSubmitAt < MIN_FRAME_INTERVAL_MS) {
                proxy.close()
                return@setAnalyzer
            }
            lastSubmitAt = now
            try {
                val bitmap = YuvConverter.toBitmap(proxy, mirror = mirrored)
                val image = BitmapImageBuilder(bitmap).build()
                // LIVE_STREAM 要求时间戳严格单调递增
                val ts = maxOf(lastTimestamp + 1L, SystemClock.uptimeMillis())
                lastTimestamp = ts
                lm.detectAsync(image, ts)
            } catch (e: Exception) {
                main.post { onStatus("帧处理异常：${e.message ?: "未知错误"}") }
            } finally {
                proxy.close()
            }
        }

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
    }

    private fun handleResult(result: HandLandmarkerResult) {
        val now = SystemClock.uptimeMillis()
        val landmarks = result.landmarks().firstOrNull()
        val frame = if (landmarks == null || landmarks.size < 21) {
            HandFrame(timestampMs = now)
        } else {
            val points = landmarks.map { HandPoint(it.x(), it.y()) }
            val handed: List<Category> = result.handedness().firstOrNull() ?: emptyList()
            val category = handed.firstOrNull()
            GestureClassifier.classify(
                points = points,
                handedness = category?.categoryName() ?: "",
                handScore = category?.score() ?: 0.9f,
                timestampMs = now
            )
        }
        val tracked = tracker.update(frame)
        main.post { onFrame(tracked) }
    }

    fun stop() {
        if (closed) return
        closed = true
        try {
            cameraProvider?.unbindAll()
            landmarker?.close()
        } catch (_: Exception) {
            // 关闭阶段的异常不影响界面退出
        }
        landmarker = null
        executor.shutdown()
        tracker.reset()
        main.removeCallbacksAndMessages(null)
    }
}
