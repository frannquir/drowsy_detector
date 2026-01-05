package com.example.myapplication.domain.state

import android.os.SystemClock
import android.util.Log

/**
 * State machine for drowsiness detection with proper reset logic.
 *
 * Rules:
 * - Enters DROWSY_ACTIVE on nod event
 * - Exits to AWAKE only if face detected AND eyes open continuously for EYES_OPEN_CONFIRM_MS
 * - Remains DROWSY_ACTIVE if face lost or eyes closed (keeps alerting)
 * - Implements alert cooldown and optional escalation
 */
class DrowsinessStateMachine {

    // Configuration constants
    companion object {
        private const val TAG = "DrowsinessStateMachine"

        // Timing thresholds
        const val FACE_PRESENT_TIMEOUT_MS = 800L
        const val EYES_OPEN_CONFIRM_MS = 1200L
        const val ALERT_COOLDOWN_MS = 10_000L
        const val DROWSY_PERSIST_MS = 20_000L
        const val ESCALATION_STEP_MS = 5_000L

        // Alert levels
        const val MAX_ALERT_LEVEL = 5
    }

    enum class DrowsyState {
        AWAKE,
        DROWSY_PENDING,
        DROWSY_ACTIVE
    }

    data class Output(
        val currentState: DrowsyState,
        val shouldAlert: Boolean,
        val shouldStopAlert: Boolean,
        val alertLevel: Int
    )

    // State variables
    private var currentState = DrowsyState.AWAKE
    private var drowsyActivatedAtMs: Long? = null
    private var lastAlertTimeMs: Long? = null
    private var alertLevel = 0

    // Face tracking
    private var lastFaceSeenMs: Long? = null
    private var faceLostSinceMs: Long? = null

    // Awake confirmation tracking
    private var awakeConfirmStartMs: Long? = null

    /**
     * Update the state machine with current detections.
     *
     * @param faceDetected Whether a face is currently detected
     * @param eyesOpen Whether eyes are currently open (based on EAR threshold)
     * @param nodEvent Whether a nod was just detected in this frame
     * @param nowMs Current timestamp in milliseconds (use SystemClock.elapsedRealtime())
     * @return Output containing state and alert decisions
     */
    fun update(
        faceDetected: Boolean,
        eyesOpen: Boolean,
        nodEvent: Boolean,
        nowMs: Long
    ): Output {
        // Track face presence/loss
        if (faceDetected) {
            lastFaceSeenMs = nowMs
            faceLostSinceMs = null
        } else {
            if (faceLostSinceMs == null && lastFaceSeenMs != null) {
                faceLostSinceMs = nowMs
            }
        }

        val faceLost = faceLostSinceMs != null &&
                       (nowMs - faceLostSinceMs!!) > FACE_PRESENT_TIMEOUT_MS

        // State machine logic
        when (currentState) {
            DrowsyState.AWAKE -> {
                if (nodEvent) {
                    Log.d(TAG, "NOD detected → transitioning to DROWSY_ACTIVE")
                    currentState = DrowsyState.DROWSY_ACTIVE
                    drowsyActivatedAtMs = nowMs
                    lastAlertTimeMs = nowMs
                    alertLevel = 0
                    awakeConfirmStartMs = null
                    return Output(
                        currentState = currentState,
                        shouldAlert = true,
                        shouldStopAlert = false,
                        alertLevel = alertLevel
                    )
                }
            }

            DrowsyState.DROWSY_ACTIVE -> {
                // Check for awake confirmation (face + eyes open continuously)
                if (faceDetected && eyesOpen) {
                    if (awakeConfirmStartMs == null) {
                        awakeConfirmStartMs = nowMs
                        Log.d(TAG, "Started awake confirmation (face + eyes open)")
                    }

                    val awakeConfirmDurationMs = nowMs - awakeConfirmStartMs!!
                    if (awakeConfirmDurationMs >= EYES_OPEN_CONFIRM_MS) {
                        Log.d(TAG, "Awake confirmed for ${awakeConfirmDurationMs}ms → transitioning to AWAKE (stopping alerts)")
                        currentState = DrowsyState.AWAKE
                        drowsyActivatedAtMs = null
                        lastAlertTimeMs = null
                        alertLevel = 0
                        awakeConfirmStartMs = null
                        return Output(
                            currentState = currentState,
                            shouldAlert = false,
                            shouldStopAlert = true,
                            alertLevel = 0
                        )
                    }
                } else {
                    // Reset awake confirmation if eyes close or face lost
                    if (awakeConfirmStartMs != null) {
                        if (faceLost) {
                            Log.d(TAG, "Awake confirmation interrupted: face LOST (no face for ${nowMs - faceLostSinceMs!!}ms)")
                        } else if (!eyesOpen) {
                            Log.d(TAG, "Awake confirmation interrupted: eyes CLOSED")
                        }
                        awakeConfirmStartMs = null
                    }
                }

                // Handle alert scheduling with cooldown
                val timeSinceLastAlert = lastAlertTimeMs?.let { nowMs - it } ?: Long.MAX_VALUE
                val shouldAlert = timeSinceLastAlert >= getAlertInterval()

                if (shouldAlert) {
                    lastAlertTimeMs = nowMs

                    // Escalate alert level over time
                    val timeSinceActivation = drowsyActivatedAtMs?.let { nowMs - it } ?: 0L
                    alertLevel = minOf(
                        MAX_ALERT_LEVEL,
                        (timeSinceActivation / ESCALATION_STEP_MS).toInt()
                    )

                    val reason = when {
                        faceLost -> "face LOST"
                        !eyesOpen -> "eyes CLOSED"
                        else -> "eyes not fully open"
                    }
                    Log.d(TAG, "DROWSY_ACTIVE: alerting (reason: $reason, level: $alertLevel)")
                }

                return Output(
                    currentState = currentState,
                    shouldAlert = shouldAlert,
                    shouldStopAlert = false,
                    alertLevel = alertLevel
                )
            }

            DrowsyState.DROWSY_PENDING -> {
                // Reserved for future use
            }
        }

        return Output(
            currentState = currentState,
            shouldAlert = false,
            shouldStopAlert = false,
            alertLevel = 0
        )
    }

    /**
     * Get alert interval based on current level (escalates over time).
     */
    private fun getAlertInterval(): Long {
        return when (alertLevel) {
            0 -> ALERT_COOLDOWN_MS
            1 -> 8_000L
            2 -> 6_000L
            3 -> 4_000L
            4 -> 2_000L
            else -> 1_500L
        }
    }

    /**
     * Reset the state machine to AWAKE.
     */
    fun reset() {
        Log.d(TAG, "State machine reset")
        currentState = DrowsyState.AWAKE
        drowsyActivatedAtMs = null
        lastAlertTimeMs = null
        alertLevel = 0
        awakeConfirmStartMs = null
        lastFaceSeenMs = null
        faceLostSinceMs = null
    }

    /**
     * Get current state (for debugging/display).
     */
    fun getCurrentState(): DrowsyState = currentState
}
