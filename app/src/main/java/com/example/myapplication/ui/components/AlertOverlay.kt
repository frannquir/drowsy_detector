package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.AlertLevel

@Composable
fun AlertOverlay(
    alertLevel: AlertLevel,
    blinkDetected: Boolean,
    yawnDetected: Boolean,
    nodFlash: Boolean
) {
    // Warning overlay (2s eye closure)
    AnimatedVisibility(
        visible = alertLevel == AlertLevel.WARNING,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x4DFF9800)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️ WARNING!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    // Drowsy overlay (alarm activated)
    AnimatedVisibility(
        visible = alertLevel == AlertLevel.DROWSY,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCCFF0000)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🚨 DROWSINESS DETECTED!",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Please take a break",
                    fontSize = 24.sp,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }

    // Event flashes (top center)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 80.dp)
        ) {
            AnimatedVisibility(visible = blinkDetected) {
                Text(
                    text = "👁️ BLINK",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Cyan,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            AnimatedVisibility(visible = yawnDetected) {
                Text(
                    text = "🥱 YAWN",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Yellow,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = if (blinkDetected) 8.dp else 0.dp)
                )
            }

            AnimatedVisibility(visible = nodFlash) {
                Text(
                    text = "📉 NOD",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Magenta,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = if (blinkDetected || yawnDetected) 8.dp else 0.dp)
                )
            }
        }
    }
}
