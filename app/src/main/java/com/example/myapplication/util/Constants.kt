package com.example.myapplication.util

object DrowsinessConstants {
    // EAR Detection
    const val EAR_THRESHOLD = 0.21f
    const val BLINK_MIN_FRAMES = 2

    // MAR Detection (with hysteresis)
    const val YAWN_START_THRESHOLD = 0.35f
    const val YAWN_END_THRESHOLD = 0.25f
    const val YAWN_MIN_FRAMES = 5

    // Warning System
    const val WARNING_EYES_CLOSED_SEC = 2.0f
    const val LONG_EYES_CLOSED_SEC = 4.0f
    const val STABLE_AWAKE_SEC = 2.0f

    // Alarm Triggers
    const val WARN_WINDOW_SHORT = 60.0f  // 2 warnings in 60s
    const val WARN_WINDOW_LONG = 180.0f  // 3 warnings in 180s

    // Nod Detection
    const val NOSE_IDX = 1
    const val DIP_THRESHOLD = 0.035f
    const val VEL_THRESHOLD = 0.08f
    const val MIN_DIP_TIME = 0.10f
    const val NOD_COOLDOWN = 1.0f
    const val PEAK_CONFIRM = 0.045f
    const val NOD_WINDOW = 15.0f  // Double nod detection

    // Alarm Escalation
    const val ALARM_START_INTERVAL = 3.0f
    const val ALARM_MIN_INTERVAL = 0.5f
    const val ALARM_ACCEL_EVERY = 2.0f
    const val ALARM_LEVEL_MAX = 10
    const val ALARM_ACCEL_RATE = 0.4f  // Decrease per level

    // MediaPipe Landmarks
    val LEFT_EYE_INDICES = listOf(33, 160, 158, 133, 153, 144)
    val RIGHT_EYE_INDICES = listOf(362, 385, 387, 263, 373, 380)
    val MOUTH_INDICES = listOf(61, 13, 82, 312, 291, 317, 87, 14)
}
