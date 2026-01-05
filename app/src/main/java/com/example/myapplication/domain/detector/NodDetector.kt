package com.example.myapplication.domain.detector

import com.example.myapplication.util.DrowsinessConstants.DIP_THRESHOLD
import com.example.myapplication.util.DrowsinessConstants.MIN_DIP_TIME
import com.example.myapplication.util.DrowsinessConstants.NOD_COOLDOWN
import com.example.myapplication.util.DrowsinessConstants.NOD_WINDOW
import com.example.myapplication.util.DrowsinessConstants.NOSE_IDX
import com.example.myapplication.util.DrowsinessConstants.PEAK_CONFIRM
import com.example.myapplication.util.DrowsinessConstants.VEL_THRESHOLD
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max

class NodDetector {
    private var nodCounter = 0
    private var noseYSmoothed: Float? = null
    private var noseBaseline: Float? = null
    private var prevNoseY: Float? = null
    private var prevNoseTime: Long? = null
    private var lastFaceTime: Long? = null

    private var nodCooldownUntil = 0L
    private var pendingDip = false
    private var dipTime: Long? = null
    private var dipPeakDelta = 0.0f
    private var nodFlashUntil = 0L

    private val nodTimes = ArrayDeque<Long>()

    fun detectNod(
        landmarks: List<NormalizedLandmark>?,
        currentTime: Long
    ): NodResult {
        if (landmarks == null) {
            return handleFaceLoss(currentTime)
        }

        lastFaceTime = currentTime
        val noseY = landmarks[NOSE_IDX].y()

        // Smooth nose position with EMA
        val alpha = 0.25f
        noseYSmoothed = if (noseYSmoothed == null) {
            noseY
        } else {
            alpha * noseY + (1 - alpha) * noseYSmoothed!!
        }

        // Update baseline when not in pending dip
        if (!pendingDip) {
            val beta = 0.02f
            noseBaseline = if (noseBaseline == null) {
                noseYSmoothed
            } else {
                beta * noseYSmoothed!! + (1 - beta) * noseBaseline!!
            }
        }

        // Calculate downward velocity
        var velocity = 0.0f
        if (prevNoseY != null && prevNoseTime != null) {
            val dt = max((currentTime - prevNoseTime!!) / 1000.0f, 0.001f)
            velocity = (noseYSmoothed!! - prevNoseY!!) / dt
        }

        prevNoseY = noseYSmoothed
        prevNoseTime = currentTime

        var nodDetected = false

        if (noseBaseline != null && currentTime >= nodCooldownUntil) {
            val delta = noseYSmoothed!! - noseBaseline!!

            // Detect downward dip
            if (delta > DIP_THRESHOLD && velocity > VEL_THRESHOLD) {
                if (!pendingDip) {
                    pendingDip = true
                    dipTime = currentTime
                    dipPeakDelta = delta
                } else {
                    dipPeakDelta = max(dipPeakDelta, delta)
                }
            }

            // Confirm nod
            if (pendingDip) {
                val dipDuration = (currentTime - dipTime!!) / 1000.0f
                if (dipDuration >= MIN_DIP_TIME && dipPeakDelta >= PEAK_CONFIRM) {
                    nodCounter++
                    nodCooldownUntil = currentTime + (NOD_COOLDOWN * 1000).toLong()
                    nodFlashUntil = currentTime + 400
                    pendingDip = false
                    nodDetected = true

                    // Track nod time for double-nod detection
                    nodTimes.add(currentTime)
                    pruneNodTimes(currentTime)
                } else if (dipDuration > 1.5f) {
                    pendingDip = false
                }
            }
        }

        val doubleNod = nodTimes.size >= 2

        return NodResult(
            nodDetected = nodDetected,
            nodCount = nodCounter,
            showFlash = currentTime < nodFlashUntil,
            doubleNod = doubleNod
        )
    }

    private fun handleFaceLoss(currentTime: Long): NodResult {
        if (lastFaceTime == null) {
            return NodResult(false, nodCounter, false, false)
        }

        val timeSinceFace = (currentTime - lastFaceTime!!) / 1000.0f

        if (timeSinceFace <= 0.6f && pendingDip && dipTime != null) {
            val timeSinceDip = (currentTime - dipTime!!) / 1000.0f
            if (timeSinceDip <= 0.7f && currentTime >= nodCooldownUntil) {
                // Count nod on face loss after dip
                nodCounter++
                nodCooldownUntil = currentTime + (NOD_COOLDOWN * 1000).toLong()
                nodFlashUntil = currentTime + 400
                pendingDip = false

                nodTimes.add(currentTime)
                pruneNodTimes(currentTime)

                val doubleNod = nodTimes.size >= 2
                return NodResult(true, nodCounter, true, doubleNod)
            }
        } else if (timeSinceFace > 0.6f) {
            pendingDip = false
        }

        return NodResult(false, nodCounter, currentTime < nodFlashUntil, false)
    }

    private fun pruneNodTimes(now: Long) {
        while (nodTimes.isNotEmpty() && (now - nodTimes.first()) > NOD_WINDOW * 1000) {
            nodTimes.removeFirst()
        }
    }

    fun clearNodTimes() {
        nodTimes.clear()
    }
}

data class NodResult(
    val nodDetected: Boolean,
    val nodCount: Int,
    val showFlash: Boolean,
    val doubleNod: Boolean
)
