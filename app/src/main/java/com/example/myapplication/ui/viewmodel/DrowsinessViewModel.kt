package com.example.myapplication.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.alert.AlarmManager
import com.example.myapplication.data.model.AlertLevel
import com.example.myapplication.data.model.DrowsinessMetrics
import com.example.myapplication.domain.analyzer.DrowsinessAnalyzer
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DrowsinessViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DrowsinessUiState())
    val uiState: StateFlow<DrowsinessUiState> = _uiState.asStateFlow()

    private val analyzer = DrowsinessAnalyzer()
    private val alarmManager = AlarmManager(application)
    private var drowsyUntil = 0L

    fun onLandmarksDetected(landmarks: List<NormalizedLandmark>?) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val result = analyzer.analyze(landmarks, currentTime)

            // Update alarm state with stop signal from state machine
            val alarmState = alarmManager.updateAlarm(
                result.awake,
                result.risky,
                currentTime,
                result.shouldActivateAlarm,
                result.shouldStopAlarm
            )

            // Update drowsy display duration
            if (alarmState.active) {
                drowsyUntil = currentTime + 4000 // 4 seconds
            }

            // Clear nod times if alarm activates from double nod
            if (result.shouldActivateAlarm && result.nodResult.doubleNod) {
                analyzer.clearWarnings()
            }

            _uiState.update {
                it.copy(
                    faceDetected = result.faceDetected,
                    metrics = result.metrics,
                    alertLevel = if (currentTime < drowsyUntil) AlertLevel.DROWSY else result.alertLevel,
                    blinkDetected = result.blinkDetected,
                    yawnDetected = result.yawnDetected,
                    nodFlash = result.nodResult.showFlash,
                    alarmActive = alarmState.active,
                    alarmLevel = alarmState.level
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        alarmManager.release()
    }
}

data class DrowsinessUiState(
    val faceDetected: Boolean = false,
    val metrics: DrowsinessMetrics? = null,
    val alertLevel: AlertLevel = AlertLevel.NORMAL,
    val blinkDetected: Boolean = false,
    val yawnDetected: Boolean = false,
    val nodFlash: Boolean = false,
    val alarmActive: Boolean = false,
    val alarmLevel: Int = 0
)
