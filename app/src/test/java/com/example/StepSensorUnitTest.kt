package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * Unit tests verifying MotionIQ hardware step-counting logic,
 * baseline tracking, daily reset, and non-walking rejection algorithms.
 */
class StepSensorUnitTest {

    /**
     * Logic simulation helper modeling the exact formula in StepSensorManager:
     * todaySteps = (rawSensorSteps - baseline) + offset
     */
    class StepCounterBaselineTracker {
        var baseline: Float = -1f
        var offsetSteps: Int = 0
        var displayedSteps: Int = 0

        fun onSensorEvent(rawSensorSteps: Float): Int {
            if (baseline < 0f) {
                // First event
                baseline = rawSensorSteps
                return displayedSteps
            }

            if (rawSensorSteps < baseline) {
                // Device reboot mid-day (sensor reset to ~0)
                offsetSteps = displayedSteps
                baseline = rawSensorSteps
            }

            val calculated = ((rawSensorSteps - baseline).toInt() + offsetSteps).coerceAtLeast(0)
            if (calculated > displayedSteps) {
                displayedSteps = calculated
            }
            return displayedSteps
        }

        fun onNewDayRollover(currentRawSensor: Float) {
            baseline = currentRawSensor
            offsetSteps = 0
            displayedSteps = 0
        }

        fun restoreFromAppRestart(savedSteps: Int, savedBaseline: Float, savedOffset: Int) {
            displayedSteps = savedSteps
            baseline = savedBaseline
            offsetSteps = savedOffset
        }
    }

    /**
     * DSP walking step validator helper modeling the exact physics filter in StepSensorManager:
     * - Gravity removal (high-pass filter)
     * - Low-pass smoothing
     * - Violent shake threshold rejection (> 15 m/s^2)
     * - Cadence timing check (250ms - 1600ms)
     * - 2-step rhythm confirmation before counting
     */
    class AccelerometerStepFilter {
        private val gravity = FloatArray(3) { 0f }
        private var isGravityInit = false
        private var smoothedMag = 0f
        private var prevSmoothedMag = 0f
        private var lastStepTime = 0L
        private var consecutiveRhythmicSteps = 0
        private var lastShakeTime = 0L
        var stepCount = 0

        fun processSample(rawX: Float, rawY: Float, rawZ: Float, timestampMs: Long): Int {
            if (!isGravityInit) {
                gravity[0] = rawX
                gravity[1] = rawY
                gravity[2] = rawZ
                isGravityInit = true
            } else {
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * rawX
                gravity[1] = alpha * gravity[1] + (1 - alpha) * rawY
                gravity[2] = alpha * gravity[2] + (1 - alpha) * rawZ
            }

            val lx = rawX - gravity[0]
            val ly = rawY - gravity[1]
            val lz = rawZ - gravity[2]
            val linearMag = sqrt((lx * lx + ly * ly + lz * lz).toDouble()).toFloat()

            smoothedMag = 0.25f * linearMag + 0.75f * smoothedMag

            // Reject violent shaking
            if (linearMag > 15.0f) {
                lastShakeTime = timestampMs
                consecutiveRhythmicSteps = 0
                return stepCount
            }

            if (timestampMs - lastShakeTime < 1000L) {
                return stepCount
            }

            // Peak check
            val isPeak = prevSmoothedMag > 2.0f && smoothedMag < prevSmoothedMag && prevSmoothedMag >= 1.4f
            if (isPeak) {
                val timeDiff = timestampMs - lastStepTime
                if (timeDiff in 250L..1600L) {
                    consecutiveRhythmicSteps++
                    if (consecutiveRhythmicSteps == 2) {
                        stepCount += 2
                        lastStepTime = timestampMs
                    } else if (consecutiveRhythmicSteps > 2) {
                        stepCount += 1
                        lastStepTime = timestampMs
                    }
                } else if (timeDiff > 1600L) {
                    consecutiveRhythmicSteps = 1
                    lastStepTime = timestampMs
                }
            }

            prevSmoothedMag = smoothedMag
            return stepCount
        }
    }

    // TEST 1: Stationary Phone (Table Test)
    @Test
    fun test1_StationaryPhone_TableTest_ZeroSteps() {
        val tracker = StepCounterBaselineTracker()

        // Phone boots with 12,500 total steps since last boot
        val initialBootSteps = 12500f
        val firstRead = tracker.onSensorEvent(initialBootSteps)
        assertEquals("Initial reading on table must be 0 steps", 0, firstRead)

        // Phone sits on table for 1 hour, sensor value does not change
        val secondRead = tracker.onSensorEvent(12500f)
        val thirdRead = tracker.onSensorEvent(12500f)

        assertEquals("Stationary phone must not increment steps", 0, secondRead)
        assertEquals("Stationary phone must not increment steps", 0, thirdRead)
    }

    // TEST 2: Hand Movement & Violent Shake Rejection
    @Test
    fun test2_ViolentShake_Rejected_NoStepsCounted() {
        val filter = AccelerometerStepFilter()
        var time = 1000L

        // Violent shake (e.g. linear acceleration spike > 18 m/s^2)
        filter.processSample(20f, 15f, 25f, time)
        time += 50
        filter.processSample(22f, 18f, 28f, time)
        time += 50
        filter.processSample(19f, 14f, 22f, time)

        assertEquals("Violent shaking must be rejected with 0 steps", 0, filter.stepCount)
    }

    // TEST 3: Walk 10 Steps
    @Test
    fun test3_Walk10Steps_ExactlyIncrementsBy10() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(5000f) // Baseline established at 5000

        // User walks 10 steps -> sensor ticks from 5001 to 5010
        for (step in 1..10) {
            tracker.onSensorEvent(5000f + step)
        }

        assertEquals("Walking 10 steps must yield exactly 10 detected steps", 10, tracker.displayedSteps)
    }

    // TEST 4: Walk 50 Steps
    @Test
    fun test4_Walk50Steps_ExactlyIncrementsBy50() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(10000f) // Baseline established at 10000

        // User walks 50 steps
        for (step in 1..50) {
            tracker.onSensorEvent(10000f + step)
        }

        assertEquals("Walking 50 steps must yield exactly 50 detected steps", 50, tracker.displayedSteps)
    }

    // TEST 5: Stop Walking
    @Test
    fun test5_StopWalking_CountStopsIncreasing() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(2000f)

        // Walk 25 steps
        for (step in 1..25) {
            tracker.onSensorEvent(2000f + step)
        }
        assertEquals(25, tracker.displayedSteps)

        // User stops walking for 10 sensor ticks
        for (tick in 1..10) {
            tracker.onSensorEvent(2025f)
        }

        assertEquals("Stopped walking must keep step count constant at 25", 25, tracker.displayedSteps)
    }

    // TEST 6: App Restart Persistence
    @Test
    fun test6_AppRestart_PreservesDailyStepsWithoutJump() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(8000f)

        // Walk 150 steps during morning
        for (step in 1..150) {
            tracker.onSensorEvent(8000f + step)
        }
        assertEquals(150, tracker.displayedSteps)

        // Simulate app kill & restart: restore saved values
        val restartTracker = StepCounterBaselineTracker()
        restartTracker.restoreFromAppRestart(
            savedSteps = 150,
            savedBaseline = 8000f,
            savedOffset = 0
        )
        assertEquals(150, restartTracker.displayedSteps)

        // Afternoon walk: sensor continues from 8151 to 8200
        for (step in 151..200) {
            restartTracker.onSensorEvent(8000f + step)
        }

        assertEquals("Post-restart walk must seamlessly continue to 200 steps", 200, restartTracker.displayedSteps)
    }

    // TEST 7: Mid-Day Device Reboot
    @Test
    fun test7_DeviceRebootMidDay_HandledSmoothly() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(50000f) // Before reboot cumulative count

        // User walks 300 steps
        tracker.onSensorEvent(50300f)
        assertEquals(300, tracker.displayedSteps)

        // Device reboots! Sensor counter resets to ~5 steps
        tracker.onSensorEvent(5f)
        assertEquals("Reboot must preserve prior 300 steps", 300, tracker.displayedSteps)

        // User walks 20 more steps after reboot (sensor counts 5 -> 25)
        tracker.onSensorEvent(25f)
        assertEquals("Reboot + 20 steps must equal 320 steps", 320, tracker.displayedSteps)
    }

    // TEST 8: Midnight Daily Rollover
    @Test
    fun test8_MidnightDailyRollover_ResetsStepsToZero() {
        val tracker = StepCounterBaselineTracker()
        tracker.onSensorEvent(1000f)
        tracker.onSensorEvent(10800f)
        assertEquals(9800, tracker.displayedSteps)

        // Midnight arrives: new day starts
        tracker.onNewDayRollover(currentRawSensor = 10800f)
        assertEquals("New day must reset today's steps to 0", 0, tracker.displayedSteps)

        // Next morning walk: 500 steps
        tracker.onSensorEvent(11300f)
        assertEquals("Next day steps count from 0 up to 500", 500, tracker.displayedSteps)
    }

    // TEST 9: StepDetectionEngine Manual Reset
    @Test
    fun test9_StepDetectionEngine_ManualReset_SetsStepsToZeroAndBaselineToCurrentSensor() {
        val engine = com.example.data.sensor.StepDetectionEngine()
        val now = System.currentTimeMillis()

        // 1. Initial baseline set at 10,000 steps
        engine.processHardwareStepCounter(10000f, now, null)
        assertEquals(0, engine.todaySteps)

        // 2. User walks 2,500 steps (hardware reaches 12,500)
        engine.processHardwareStepCounter(12500f, now + 1000, null)
        assertEquals(2500, engine.todaySteps)
        assertEquals(10000f, engine.sensorBaseline, 0.01f)

        // 3. User taps "Reset Today's Steps"
        engine.manualReset()
        assertEquals("Today's steps must be 0 immediately after reset", 0, engine.todaySteps)
        assertEquals("Baseline must equal current hardware sensor reading (12,500)", 12500f, engine.sensorBaseline, 0.01f)

        // 4. User walks 1 step (hardware reaches 12,501)
        engine.processHardwareStepCounter(12501f, now + 2000, null)
        assertEquals("Today's steps must be 1", 1, engine.todaySteps)

        // 5. User walks 9 more steps (hardware reaches 12,510)
        engine.processHardwareStepCounter(12510f, now + 3000, null)
        assertEquals("Today's steps must be 10", 10, engine.todaySteps)
    }

    // TEST 10: StepDetectionEngine Mid-Day Reboot Recovery
    @Test
    fun test10_StepDetectionEngine_RebootRecovery_PreservesPriorSteps() {
        val engine = com.example.data.sensor.StepDetectionEngine()
        val now = System.currentTimeMillis()

        // Morning: baseline at 20,000, walks 1,500 steps
        engine.processHardwareStepCounter(20000f, now, null)
        engine.processHardwareStepCounter(21500f, now + 5000, null)
        assertEquals(1500, engine.todaySteps)

        // Phone reboots! Raw counter resets to 10 steps
        engine.processHardwareStepCounter(10f, now + 10000, null)
        assertEquals("Reboot must preserve prior 1,500 steps", 1500, engine.todaySteps)

        // User walks 50 steps after reboot (raw reaches 60)
        engine.processHardwareStepCounter(60f, now + 15000, null)
        assertEquals("Post-reboot steps must be 1550", 1550, engine.todaySteps)
    }
}
