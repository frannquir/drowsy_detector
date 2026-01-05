package com.example.myapplication.ui.screen

import android.Manifest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.camera.CameraManager
import com.example.myapplication.ui.components.AlertOverlay
import com.example.myapplication.ui.components.CameraPreview
import com.example.myapplication.ui.components.MetricsPanel
import com.example.myapplication.ui.viewmodel.DrowsinessViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DrowsinessDetectionScreen(
    viewModel: DrowsinessViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var cameraManager by remember { mutableStateOf<CameraManager?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Camera preview
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onPreviewViewCreated = { previewView ->
                    if (cameraManager == null) {
                        cameraManager = CameraManager(
                            context = context,
                            lifecycleOwner = lifecycleOwner,
                            onAnalyzerReady = { analyzer ->
                                analyzer.onLandmarksDetected = { landmarks ->
                                    viewModel.onLandmarksDetected(landmarks)
                                }
                            }
                        )
                        cameraManager?.startCamera(previewView)
                    }
                }
            )

            // Alert overlays
            AlertOverlay(
                alertLevel = uiState.alertLevel,
                blinkDetected = uiState.blinkDetected,
                yawnDetected = uiState.yawnDetected,
                nodFlash = uiState.nodFlash
            )

            // Metrics panel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                MetricsPanel(
                    metrics = uiState.metrics,
                    faceDetected = uiState.faceDetected
                )
            }

            // Status indicator (top right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (uiState.alarmActive) "🚨 ALERT" else "✅ ACTIVE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.alarmActive) Color.Red else Color.Green,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

        } else {
            // Permission request screen
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DROWSINESS DETECTOR",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00D4FF),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Camera permission required",
                        fontSize = 16.sp,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00D4FF)
                        )
                    ) {
                        Text(
                            text = "Grant Permission",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager?.stopCamera()
        }
    }
}
