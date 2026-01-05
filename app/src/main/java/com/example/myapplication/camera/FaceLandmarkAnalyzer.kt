package com.example.myapplication.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.io.ByteArrayOutputStream

class FaceLandmarkAnalyzer(
    private val context: Context
) : ImageAnalysis.Analyzer {

    private var faceLandmarker: FaceLandmarker? = null
    private var isInitialized = false
    private var initializationFailed = false

    var onLandmarksDetected: ((List<NormalizedLandmark>?) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    // Lazy initialization - moved from init block to avoid crash on construction
    private fun initializeLandmarker() {
        if (isInitialized || initializationFailed) return

        try {
            Log.d(TAG, "Initializing FaceLandmarker...")

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .setResultListener { result, inputImage ->
                    val numFaces = result.faceLandmarks().size
                    Log.d(TAG, "MediaPipe result: $numFaces face(s) detected")
                    val landmarks = result.faceLandmarks().firstOrNull()
                    if (landmarks != null) {
                        Log.d(TAG, "Face landmarks found: ${landmarks.size} points")
                    } else {
                        Log.d(TAG, "No face landmarks in this frame")
                    }
                    onLandmarksDetected?.invoke(landmarks)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "FaceLandmarker error: ${error.message}", error)
                    onError?.invoke(error.message ?: "Unknown MediaPipe error")
                }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            isInitialized = true
            Log.d(TAG, "FaceLandmarker initialized successfully")

        } catch (e: UnsatisfiedLinkError) {
            initializationFailed = true
            val msg = "Native library not found: ${e.message}\n" +
                      "Ensure you're running on ARM device (arm64-v8a or armeabi-v7a).\n" +
                      "MediaPipe doesn't support x86/x86_64 emulators."
            Log.e(TAG, msg, e)
            onError?.invoke(msg)

        } catch (e: Exception) {
            initializationFailed = true
            val msg = "Failed to initialize FaceLandmarker: ${e.message}"
            Log.e(TAG, msg, e)
            onError?.invoke(msg)
        }
    }

    companion object {
        private const val TAG = "FaceLandmarkAnalyzer"
    }

    override fun analyze(imageProxy: ImageProxy) {
        // Lazy initialization on first frame
        if (!isInitialized && !initializationFailed) {
            initializeLandmarker()
        }

        // Skip processing if initialization failed
        if (initializationFailed || faceLandmarker == null) {
            Log.w(TAG, "Skipping frame: initFailed=$initializationFailed, landmarker=${faceLandmarker != null}")
            imageProxy.close()
            return
        }

        try {
            Log.d(TAG, "Processing frame: ${imageProxy.width}x${imageProxy.height}, " +
                      "rotation=${imageProxy.imageInfo.rotationDegrees}, " +
                      "format=${imageProxy.format}")

            val bitmap = imageProxyToBitmap(imageProxy)
            if (bitmap != null) {
                Log.d(TAG, "Bitmap created: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
                val mpImage = BitmapImageBuilder(bitmap).build()
                val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000
                Log.d(TAG, "Calling detectAsync with timestamp=$timestampMs")
                faceLandmarker?.detectAsync(mpImage, timestampMs)
            } else {
                Log.e(TAG, "Failed to convert ImageProxy to Bitmap!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing frame: ${e.message}", e)
        } finally {
            // CRITICAL: Always close ImageProxy to prevent memory leak
            imageProxy.close()
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer // Y
            val uBuffer = imageProxy.planes[1].buffer // U
            val vBuffer = imageProxy.planes[2].buffer // V

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            Log.d(TAG, "Converting YUV: ySize=$ySize, uSize=$uSize, vSize=$vSize")

            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            val compressed = yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)

            if (!compressed) {
                Log.e(TAG, "Failed to compress YUV to JPEG")
                return null
            }

            val imageBytes = out.toByteArray()
            Log.d(TAG, "JPEG compressed to ${imageBytes.size} bytes")

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                Log.e(TAG, "BitmapFactory.decodeByteArray returned null")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Exception in imageProxyToBitmap: ${e.message}", e)
            null
        }
    }

    fun close() {
        try {
            faceLandmarker?.close()
            faceLandmarker = null
            isInitialized = false
            Log.d(TAG, "FaceLandmarker closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing FaceLandmarker: ${e.message}", e)
        }
    }
}
