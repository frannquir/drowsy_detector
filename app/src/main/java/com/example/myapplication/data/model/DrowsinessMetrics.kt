package com.example.myapplication.data.model

data class DrowsinessMetrics(
    val leftEAR: Float,
    val rightEAR: Float,
    val averageEAR: Float,
    val mar: Float,
    val blinkCount: Int,
    val yawnCount: Int,
    val nodCount: Int,
    val eyesClosedDuration: Float,
    val headPitch: Float
)
