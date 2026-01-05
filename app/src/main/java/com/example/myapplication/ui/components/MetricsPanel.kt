package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.DrowsinessMetrics

@Composable
fun MetricsPanel(
    metrics: DrowsinessMetrics?,
    faceDetected: Boolean
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "METRICS",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00D4FF)
        )

        if (faceDetected && metrics != null) {
            Text(
                text = "👁️ EAR: ${String.format("%.3f", metrics.averageEAR)}",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "👄 MAR: ${String.format("%.3f", metrics.mar)}",
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Blinks: ${metrics.blinkCount}",
                fontSize = 14.sp,
                color = Color(0xFFFFB74D),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Yawns: ${metrics.yawnCount}",
                fontSize = 14.sp,
                color = Color(0xFFFFB74D),
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Nods: ${metrics.nodCount}",
                fontSize = 14.sp,
                color = Color(0xFFFFB74D),
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "No face detected",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
