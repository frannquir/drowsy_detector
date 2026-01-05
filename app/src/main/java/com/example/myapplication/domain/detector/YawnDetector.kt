package com.example.myapplication.domain.detector

import com.example.myapplication.util.DrowsinessConstants.YAWN_END_THRESHOLD
import com.example.myapplication.util.DrowsinessConstants.YAWN_MIN_FRAMES
import com.example.myapplication.util.DrowsinessConstants.YAWN_START_THRESHOLD

class YawnDetector {
    private var yawnCounter = 0
    private var yawnFrames = 0
    private var isYawning = false

    fun detectYawn(mar: Float): Pair<Boolean, Int> {
        var yawnDetected = false

        // Hysteresis: different thresholds for start/end
        val yawnActive = if (isYawning) {
            mar > YAWN_END_THRESHOLD
        } else {
            mar > YAWN_START_THRESHOLD
        }

        if (yawnActive) {
            yawnFrames++
            if (yawnFrames >= YAWN_MIN_FRAMES && !isYawning) {
                yawnCounter++
                isYawning = true
                yawnDetected = true
            }
        } else {
            yawnFrames = 0
            isYawning = false
        }

        return Pair(yawnDetected, yawnCounter)
    }

    fun getYawnActive(): Boolean = isYawning
}
