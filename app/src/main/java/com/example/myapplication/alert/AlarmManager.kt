package com.example.myapplication.alert

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import com.example.myapplication.util.DrowsinessConstants.ALARM_ACCEL_EVERY
import com.example.myapplication.util.DrowsinessConstants.ALARM_ACCEL_RATE
import com.example.myapplication.util.DrowsinessConstants.ALARM_LEVEL_MAX
import com.example.myapplication.util.DrowsinessConstants.ALARM_MIN_INTERVAL
import com.example.myapplication.util.DrowsinessConstants.ALARM_START_INTERVAL
import com.example.myapplication.util.DrowsinessConstants.STABLE_AWAKE_SEC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class AlarmManager(private val context: Context) {
    private var alarmActive = false
    private var alarmLevel = 0
    private var nextAlarmTime = 0L
    private var awakeSince: Long? = null
    private var lastRiskyTime = 0L

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun updateAlarm(
        awake: Boolean,
        risky: Boolean,
        currentTime: Long,
        shouldActivate: Boolean,
        shouldStop: Boolean = false
    ): AlarmState {
        // Force stop if state machine says so
        if (shouldStop && alarmActive) {
            android.util.Log.d("AlarmManager", "Stopping alarm (state machine confirmed awake)")
            alarmActive = false
            alarmLevel = 0
            nextAlarmTime = 0
            awakeSince = null
            lastRiskyTime = 0
            return AlarmState(false, 0)
        }

        // Activate alarm if conditions met
        if (!alarmActive && shouldActivate) {
            android.util.Log.d("AlarmManager", "Activating alarm")
            alarmActive = true
            alarmLevel = 0
            nextAlarmTime = currentTime
            awakeSince = null
            lastRiskyTime = currentTime
        }

        if (alarmActive) {
            if (awake) {
                // User awake - check for stable disarm
                if (awakeSince == null) {
                    awakeSince = currentTime
                }

                val awakeTime = (currentTime - awakeSince!!) / 1000.0f
                if (awakeTime >= STABLE_AWAKE_SEC) {
                    // Disarm completely
                    android.util.Log.d("AlarmManager", "Alarm disarmed (legacy stable awake)")
                    alarmActive = false
                    alarmLevel = 0
                    nextAlarmTime = 0
                    awakeSince = null
                    lastRiskyTime = 0
                } else {
                    // Eyes open but not stable - beep fast
                    if (currentTime >= nextAlarmTime) {
                        playTripleBeep()
                        nextAlarmTime = currentTime + 1000 // 1 second
                    }
                }
            } else {
                // Still risky - escalate
                awakeSince = null

                // Escalate level
                if (lastRiskyTime == 0L) {
                    lastRiskyTime = currentTime
                } else if ((currentTime - lastRiskyTime) >= ALARM_ACCEL_EVERY * 1000) {
                    alarmLevel = min(ALARM_LEVEL_MAX, alarmLevel + 1)
                    lastRiskyTime = currentTime
                }

                // Beep scheduler
                if (currentTime >= nextAlarmTime) {
                    when {
                        alarmLevel >= 5 -> playTripleBeep()
                        alarmLevel >= 2 -> playDoubleBeep()
                        else -> playSingleBeep()
                    }

                    val interval = max(
                        ALARM_MIN_INTERVAL,
                        ALARM_START_INTERVAL - alarmLevel * ALARM_ACCEL_RATE
                    )
                    nextAlarmTime = currentTime + (interval * 1000).toLong()
                }
            }
        }

        return AlarmState(alarmActive, alarmLevel)
    }

    private fun playSingleBeep() {
        coroutineScope.launch {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 300)
        }
    }

    private fun playDoubleBeep() {
        coroutineScope.launch {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            delay(250)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
        }
    }

    private fun playTripleBeep() {
        coroutineScope.launch {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
            delay(200)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
            delay(200)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
        }
    }

    fun reset() {
        alarmActive = false
        alarmLevel = 0
    }

    fun release() {
        toneGenerator.release()
    }
}

data class AlarmState(
    val active: Boolean,
    val level: Int
)
