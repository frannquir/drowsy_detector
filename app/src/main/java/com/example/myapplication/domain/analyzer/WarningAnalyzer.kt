package com.example.myapplication.domain.analyzer

import com.example.myapplication.util.DrowsinessConstants.LONG_EYES_CLOSED_SEC
import com.example.myapplication.util.DrowsinessConstants.WARNING_EYES_CLOSED_SEC
import com.example.myapplication.util.DrowsinessConstants.WARN_WINDOW_LONG
import com.example.myapplication.util.DrowsinessConstants.WARN_WINDOW_SHORT

class WarningAnalyzer {
    private var eyesClosedStart: Long? = null
    private var warningActive = false
    private var warningEventFired = false
    private var longCloseTriggered = false

    private val warnTimes = ArrayDeque<Long>()

    fun analyzeEyeClosure(
        bothEyesClosed: Boolean,
        currentTime: Long
    ): WarningResult {
        var newWarningEvent = false
        var longClose = false

        if (bothEyesClosed) {
            if (eyesClosedStart == null) {
                eyesClosedStart = currentTime
            }

            val duration = (currentTime - eyesClosedStart!!) / 1000.0f

            // 2-second warning
            if (duration >= WARNING_EYES_CLOSED_SEC) {
                warningActive = true
                if (!warningEventFired) {
                    warnTimes.add(currentTime)
                    warningEventFired = true
                    newWarningEvent = true
                }
            }

            // 4-second long close
            if (duration >= LONG_EYES_CLOSED_SEC && !longCloseTriggered) {
                longCloseTriggered = true
                longClose = true
            }
        } else {
            eyesClosedStart = null
            warningActive = false
            warningEventFired = false
            longCloseTriggered = false
        }

        // Prune old warnings
        pruneWarnings(currentTime)

        val warnings60 = warnTimes.count { (currentTime - it) <= WARN_WINDOW_SHORT * 1000 }
        val warnings180 = warnTimes.size

        return WarningResult(
            warningActive = warningActive,
            newWarningEvent = newWarningEvent,
            longClose = longClose,
            warnings60 = warnings60,
            warnings180 = warnings180,
            eyesClosedDuration = if (eyesClosedStart != null) {
                (currentTime - eyesClosedStart!!) / 1000.0f
            } else 0f
        )
    }

    private fun pruneWarnings(now: Long) {
        while (warnTimes.isNotEmpty() && (now - warnTimes.first()) > WARN_WINDOW_LONG * 1000) {
            warnTimes.removeFirst()
        }
    }

    fun clearWarnings() {
        warnTimes.clear()
    }
}

data class WarningResult(
    val warningActive: Boolean,
    val newWarningEvent: Boolean,
    val longClose: Boolean,
    val warnings60: Int,
    val warnings180: Int,
    val eyesClosedDuration: Float
)
