package com.example.myapplication.domain.analyzer

import android.os.SystemClock
import com.example.myapplication.data.model.AlertLevel
import com.example.myapplication.domain.state.DrowsinessStateMachine
import com.example.myapplication.data.model.DrowsinessMetrics
import com.example.myapplication.domain.detector.BlinkDetector
import com.example.myapplication.domain.detector.EyeAspectRatioDetector
import com.example.myapplication.domain.detector.MouthAspectRatioDetector
import com.example.myapplication.domain.detector.NodDetector
import com.example.myapplication.domain.detector.NodResult
import com.example.myapplication.domain.detector.YawnDetector
import com.example.myapplication.util.DrowsinessConstants.LEFT_EYE_INDICES
import com.example.myapplication.util.DrowsinessConstants.MOUTH_INDICES
import com.example.myapplication.util.DrowsinessConstants.RIGHT_EYE_INDICES
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class DrowsinessAnalyzer {
    private val earDetector = EyeAspectRatioDetector()
    private val marDetector = MouthAspectRatioDetector()
    private val blinkDetector = BlinkDetector()
    private val yawnDetector = YawnDetector()
    private val nodDetector = NodDetector()
    private val warningAnalyzer = WarningAnalyzer()
    private val drowsinessStateMachine = DrowsinessStateMachine()

    fun analyze(
        landmarks: List<NormalizedLandmark>?,
        currentTime: Long
    ): AnalysisResult {
        val faceDetected = landmarks != null

        if (landmarks == null) {
            // Handle face loss
            val nodResult = nodDetector.detectNod(null, currentTime)

            // Update state machine (face lost, so eyesOpen=false to prevent false awake confirm)
            val stateMachineOutput = drowsinessStateMachine.update(
                faceDetected = false,
                eyesOpen = false,
                nodEvent = nodResult.nodDetected,
                nowMs = SystemClock.elapsedRealtime()
            )

            return AnalysisResult(
                faceDetected = false,
                metrics = null,
                alertLevel = AlertLevel.NORMAL,
                blinkDetected = false,
                yawnDetected = false,
                nodResult = nodResult,
                warningResult = WarningResult(
                    warningActive = false,
                    newWarningEvent = false,
                    longClose = false,
                    warnings60 = 0,
                    warnings180 = 0,
                    eyesClosedDuration = 0f
                ),
                shouldActivateAlarm = stateMachineOutput.shouldAlert,
                shouldStopAlarm = stateMachineOutput.shouldStopAlert,
                awake = false,
                risky = true,  // Face loss is risky
                stateMachineState = stateMachineOutput.currentState,
                stateMachineAlertLevel = stateMachineOutput.alertLevel
            )
        }

        // Calculate EAR
        val leftEAR = earDetector.calculateEAR(landmarks, LEFT_EYE_INDICES)
        val rightEAR = earDetector.calculateEAR(landmarks, RIGHT_EYE_INDICES)
        val avgEAR = (leftEAR + rightEAR) / 2f

        // Calculate MAR
        val mar = marDetector.calculateMAR(landmarks, MOUTH_INDICES)

        // Detect blink
        val (blinkDetected, blinkCount) = blinkDetector.detectBlink(leftEAR, rightEAR)
        val bothEyesClosed = blinkDetector.getBothEyesClosed(leftEAR, rightEAR)
        val eyesOpen = !bothEyesClosed

        // Detect yawn
        val (yawnDetected, yawnCount) = yawnDetector.detectYawn(mar)

        // Detect nod
        val nodResult = nodDetector.detectNod(landmarks, currentTime)

        // Analyze warnings (keep for backward compatibility)
        val warningResult = warningAnalyzer.analyzeEyeClosure(bothEyesClosed, currentTime)

        // Update state machine with nod event
        val stateMachineOutput = drowsinessStateMachine.update(
            faceDetected = true,
            eyesOpen = eyesOpen,
            nodEvent = nodResult.nodDetected,
            nowMs = SystemClock.elapsedRealtime()
        )

        // Determine alert level
        val alertLevel = when {
            warningResult.warningActive -> AlertLevel.WARNING
            else -> AlertLevel.NORMAL
        }

        // Use state machine output for alarm activation
        val shouldActivateAlarm = stateMachineOutput.shouldAlert ||
            warningResult.longClose ||
            warningResult.warnings60 >= 2 ||
            warningResult.warnings180 >= 3

        val metrics = DrowsinessMetrics(
            leftEAR = leftEAR,
            rightEAR = rightEAR,
            averageEAR = avgEAR,
            mar = mar,
            blinkCount = blinkCount,
            yawnCount = yawnCount,
            nodCount = nodResult.nodCount,
            eyesClosedDuration = warningResult.eyesClosedDuration,
            headPitch = 0f // Not used in simplified version
        )

        return AnalysisResult(
            faceDetected = true,
            metrics = metrics,
            alertLevel = alertLevel,
            blinkDetected = blinkDetected,
            yawnDetected = yawnDetected,
            nodResult = nodResult,
            warningResult = warningResult,
            shouldActivateAlarm = shouldActivateAlarm,
            shouldStopAlarm = stateMachineOutput.shouldStopAlert,
            awake = eyesOpen,
            risky = bothEyesClosed,
            stateMachineState = stateMachineOutput.currentState,
            stateMachineAlertLevel = stateMachineOutput.alertLevel
        )
    }

    fun clearWarnings() {
        warningAnalyzer.clearWarnings()
        nodDetector.clearNodTimes()
        drowsinessStateMachine.reset()
    }
}

data class AnalysisResult(
    val faceDetected: Boolean,
    val metrics: DrowsinessMetrics?,
    val alertLevel: AlertLevel,
    val blinkDetected: Boolean,
    val yawnDetected: Boolean,
    val nodResult: NodResult,
    val warningResult: WarningResult,
    val shouldActivateAlarm: Boolean,
    val shouldStopAlarm: Boolean,
    val awake: Boolean,
    val risky: Boolean,
    val stateMachineState: DrowsinessStateMachine.DrowsyState,
    val stateMachineAlertLevel: Int
)
