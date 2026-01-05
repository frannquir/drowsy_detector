package com.example.myapplication.domain.detector

import com.example.myapplication.util.DrowsinessConstants.BLINK_MIN_FRAMES
import com.example.myapplication.util.DrowsinessConstants.EAR_THRESHOLD

class BlinkDetector {
    private var blinkCounter = 0
    private var blinkFrames = 0
    private var isBlinking = false

    fun detectBlink(leftEAR: Float, rightEAR: Float): Pair<Boolean, Int> {
        val bothEyesClosed = leftEAR < EAR_THRESHOLD && rightEAR < EAR_THRESHOLD
        var blinkDetected = false

        if (bothEyesClosed) {
            blinkFrames++
            if (blinkFrames >= BLINK_MIN_FRAMES && !isBlinking) {
                blinkCounter++
                isBlinking = true
                blinkDetected = true
            }
        } else {
            blinkFrames = 0
            isBlinking = false
        }

        return Pair(blinkDetected, blinkCounter)
    }

    fun getBothEyesClosed(leftEAR: Float, rightEAR: Float): Boolean {
        return leftEAR < EAR_THRESHOLD && rightEAR < EAR_THRESHOLD
    }
}
