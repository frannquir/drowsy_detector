package com.example.myapplication.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onAnalyzerReady: (FaceLandmarkAnalyzer) -> Unit,
    private val onError: ((String) -> Unit)? = null
) {
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var analyzer: FaceLandmarkAnalyzer? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Image analysis use case
            analyzer = FaceLandmarkAnalyzer(context).apply {
                // Handle initialization errors
                this.onError = { errorMsg ->
                    Log.e("CameraManager", "Analyzer error: $errorMsg")
                    onError?.invoke(errorMsg)
                }
            }

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor, analyzer!!)
                }

            onAnalyzerReady(analyzer!!)

            // Select front camera for drowsiness detection
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e("CameraManager", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        try {
            // Clean up analyzer first
            analyzer?.close()
            analyzer = null

            // Unbind camera
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            }, ContextCompat.getMainExecutor(context))

            // Shutdown executor
            analysisExecutor.shutdown()

            Log.d("CameraManager", "Camera stopped and resources cleaned up")
        } catch (e: Exception) {
            Log.e("CameraManager", "Error stopping camera", e)
        }
    }
}
