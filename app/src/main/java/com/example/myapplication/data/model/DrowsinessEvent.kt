package com.example.myapplication.data.model

import java.util.UUID

data class DrowsinessEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val alertLevel: AlertLevel,
    val metrics: DrowsinessMetrics
)
