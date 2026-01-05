package com.example.myapplication

import com.example.myapplication.domain.state.DrowsinessStateMachine
import com.example.myapplication.domain.state.DrowsinessStateMachine.DrowsyState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DrowsinessStateMachineTest {

    private lateinit var stateMachine: DrowsinessStateMachine
    private var currentTimeMs = 0L

    @Before
    fun setup() {
        stateMachine = DrowsinessStateMachine()
        currentTimeMs = 0L
    }

    @Test
    fun `nod triggers drowsy active state`() {
        val output = stateMachine.update(
            faceDetected = true,
            eyesOpen = true,
            nodEvent = true,
            nowMs = currentTimeMs
        )

        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)
        assertTrue(output.shouldAlert)
        assertFalse(output.shouldStopAlert)
    }

    @Test
    fun `drowsy stops after eyes open confirmation period`() {
        // Trigger nod
        stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        currentTimeMs += 500

        // Eyes open continuously
        var output = stateMachine.update(true, eyesOpen = true, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)

        // Advance past confirmation period
        currentTimeMs += DrowsinessStateMachine.EYES_OPEN_CONFIRM_MS
        output = stateMachine.update(true, eyesOpen = true, false, currentTimeMs)

        assertEquals(DrowsyState.AWAKE, output.currentState)
        assertTrue(output.shouldStopAlert)
    }

    @Test
    fun `drowsy persists if face lost after nod`() {
        // Trigger nod
        stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        currentTimeMs += 500

        // Face lost
        var output = stateMachine.update(
            faceDetected = false,
            eyesOpen = false,
            nodEvent = false,
            nowMs = currentTimeMs
        )

        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)
        assertFalse(output.shouldStopAlert)

        // Advance time - should remain drowsy
        currentTimeMs += 5000
        output = stateMachine.update(false, false, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)
    }

    @Test
    fun `drowsy persists if eyes remain closed after nod`() {
        // Trigger nod
        stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        currentTimeMs += 500

        // Eyes closed (face present)
        var output = stateMachine.update(
            faceDetected = true,
            eyesOpen = false,
            nodEvent = false,
            nowMs = currentTimeMs
        )

        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)
        assertFalse(output.shouldStopAlert)

        // Advance time - should remain drowsy
        currentTimeMs += 5000
        output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)
    }

    @Test
    fun `awake confirmation resets if eyes close`() {
        // Trigger nod
        stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        currentTimeMs += 500

        // Eyes open for partial confirmation period
        stateMachine.update(true, eyesOpen = true, false, currentTimeMs)
        currentTimeMs += 800  // Less than EYES_OPEN_CONFIRM_MS (1200ms)

        // Eyes close - should reset confirmation
        var output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)

        // Eyes open again
        currentTimeMs += 500
        output = stateMachine.update(true, eyesOpen = true, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)

        // Need to wait full confirmation period again
        currentTimeMs += 1000  // Not enough yet
        output = stateMachine.update(true, eyesOpen = true, false, currentTimeMs)
        assertEquals(DrowsyState.DROWSY_ACTIVE, output.currentState)

        currentTimeMs += 300  // Now past confirmation period
        output = stateMachine.update(true, eyesOpen = true, false, currentTimeMs)
        assertEquals(DrowsyState.AWAKE, output.currentState)
    }

    @Test
    fun `alert cooldown prevents rapid retriggering`() {
        // Trigger nod
        var output = stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        assertTrue(output.shouldAlert)

        currentTimeMs += 1000  // 1 second later

        // Should not alert again immediately (cooldown is 10 seconds)
        output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertFalse(output.shouldAlert)

        currentTimeMs += DrowsinessStateMachine.ALERT_COOLDOWN_MS

        // Now should alert again
        output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertTrue(output.shouldAlert)
    }

    @Test
    fun `alert escalation increases over time`() {
        // Trigger nod
        var output = stateMachine.update(true, true, nodEvent = true, currentTimeMs)
        assertEquals(0, output.alertLevel)

        // Advance time to trigger escalation
        currentTimeMs += DrowsinessStateMachine.ESCALATION_STEP_MS
        output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertTrue(output.alertLevel > 0)

        val previousLevel = output.alertLevel

        currentTimeMs += DrowsinessStateMachine.ESCALATION_STEP_MS
        output = stateMachine.update(true, eyesOpen = false, false, currentTimeMs)
        assertTrue(output.alertLevel > previousLevel)
    }
}
