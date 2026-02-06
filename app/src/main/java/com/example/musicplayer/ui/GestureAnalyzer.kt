package com.example.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GestureAnalyzer(
    context: Context,
    private val onGesture: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val mainExecutor = androidx.core.content.ContextCompat.getMainExecutor(context)
    private var gestureRecognizer: GestureRecognizer? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    // Gesture State
    private var lastGestureTime = 0L
    private val debounceTime = 1200L // Slightly longer debounce for static gestures
    private var lastPinchDistance = 0f // State for tracking zoom delta

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("gesture_recognizer.task")
            .build()

        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, inputImage ->
                processResult(result)
            }
            .build()

        try {
            gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (gestureRecognizer != null) {
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            }
            
            // Optimization: Skip bitmap creation if we can, but keeping current working flow
            imageProxy.toBitmap()?.let { bm ->
                val rotatedBitmap = Bitmap.createBitmap(
                    bm, 0, 0, bm.width, bm.height, matrix, true
                )
                
                val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                gestureRecognizer?.recognizeAsync(mpImage, SystemClock.uptimeMillis())
            }
        }
        imageProxy.close()
    }

    private fun processResult(result: GestureRecognizerResult) {
        val gestures = result.gestures()
        val landmarks = result.landmarks()
        val now = SystemClock.uptimeMillis()

        // Debounce handling
        if (now - lastGestureTime < debounceTime) return

        var gestureFound = false

        if (gestures.isNotEmpty()) {
            val topGesture = gestures.first().firstOrNull()
            val category = topGesture?.categoryName() ?: "None"
            val score = topGesture?.score() ?: 0f

            if (score > 0.5f) {
                when (category) {
                    "Open_Palm" -> {
                        mainExecutor.execute { onGesture("PAUSE_PLAY") }
                        lastGestureTime = now
                        gestureFound = true
                    }
                    "Thumb_Up" -> {
                        mainExecutor.execute { onGesture("NEXT") }
                        lastGestureTime = now
                        gestureFound = true
                    }
                    "Thumb_Down" -> {
                        mainExecutor.execute { onGesture("PREV") }
                        lastGestureTime = now
                        gestureFound = true
                    }
                }
            }
        }

        if (!gestureFound && landmarks.isNotEmpty() && landmarks[0].isNotEmpty()) {
            val hand = landmarks[0]
            val thumbTip = hand[4]
            val indexTip = hand[8]
            
            // Calculate distance (Euclidean distance on normalized coordinates 0.0-1.0)
            val dx = thumbTip.x() - indexTip.x()
            val dy = thumbTip.y() - indexTip.y()
            val currentDistance = kotlin.math.sqrt(dx*dx + dy*dy)

            // Dynamic Delta Logic
            // We compare current distance with the last framed distance
            if (lastPinchDistance > 0) {
                val delta = currentDistance - lastPinchDistance
                val sensitivity = 0.02f // Must move fingers by 2% of screen to trigger
                
                // Separate throttle for volume (faster than gestures)
                if (now - lastGestureTime > 300) { // 300ms delay between volume steps
                    if (delta > sensitivity) {
                        // Spreading fingers -> Zoom In -> Volume Up
                         mainExecutor.execute { onGesture("VOL_UP") }
                         lastGestureTime = now
                    } else if (delta < -sensitivity) {
                        // Closing fingers -> Zoom Out -> Volume Down
                         mainExecutor.execute { onGesture("VOL_DOWN") }
                         lastGestureTime = now
                    }
                }
            }
            
            // Update state for next frame
            lastPinchDistance = currentDistance
        } else {
            // Reset if hand lost or other gesture found
            lastPinchDistance = 0f
        }
    }

    fun close() {
        gestureRecognizer?.close()
        executor.shutdown()
    }
}
