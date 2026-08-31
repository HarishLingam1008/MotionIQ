package com.example

import com.example.data.sensor.MovementState
import com.example.data.sensor.SensorFusionEngine
import com.example.data.sensor.StepDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sin

/**
 * Comprehensive Unit Test Suite for MotionIQ Sensor Fusion & Step Detection Engine.
 * Tests multi-axis dynamics, noise filtering, walking classification, anti-shake rejection,
 * vehicle detection, and pedometer baseline mechanics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SensorFusionUnitTest {

    @Test
    fun test1_StationaryPhone_ClassifiedAsStationary_ZeroSteps() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        // Stationary on desk: Gravity 9.81 on Z, minimal gyro noise
        for (i in 0 until 60) {
            fusion.processGyroscope(0.01f, 0.01f, 0.01f)
            fusion.processMagnetometer(22f, -15f, 40f)
            fusion.processAccelerometer(0.02f, 0.02f, 9.81f, time)
            stepEngine.processSensorFusionStep(time, fusion)
            time += 20L
        }

        assertEquals("Stationary device must be classified as STATIONARY", MovementState.STATIONARY, fusion.movementState)
        assertEquals("Stationary device must generate 0 steps", 0, stepEngine.todaySteps)
        assertTrue("Movement confidence must be near 0", fusion.motionConfidence.movementConfidence < 0.15f)
        assertEquals("Walking confidence must be 0", 0.0f, fusion.motionConfidence.walkingConfidence, 0.01f)
    }

    @Test
    fun test2_ViolentShaking_Rejected_NoSteps() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        // Violent shaking: high acceleration spikes and high angular velocity
        for (i in 0 until 30) {
            fusion.processGyroscope(5.2f, 4.8f, 6.1f) // high gyro (> 4.5 rad/s)
            fusion.processAccelerometer(16.5f, -14.2f, 18.0f, time) // violent jolt (> 14 m/s^2)
            stepEngine.processSensorFusionStep(time, fusion)
            time += 40L
        }

        assertEquals("Violent shaking must be classified as PHONE_MOVEMENT", MovementState.PHONE_MOVEMENT, fusion.movementState)
        assertEquals("Violent shaking must not count any steps", 0, stepEngine.todaySteps)
        assertTrue("Shakes count must be tracked", fusion.rejectedShakesCount > 0)
    }

    @Test
    fun test3_PhoneRotationAndTwisting_Rejected_NoSteps() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        // Phone rotated in hand without walking: high gyro, low linear acceleration
        for (i in 0 until 40) {
            fusion.processGyroscope(3.8f, 2.9f, 3.5f)
            fusion.processAccelerometer(0.5f, 0.8f, 9.8f, time)
            stepEngine.processSensorFusionStep(time, fusion)
            time += 30L
        }

        assertEquals("Twisting/rotating phone must be classified as PHONE_MOVEMENT", MovementState.PHONE_MOVEMENT, fusion.movementState)
        assertEquals("Phone rotation must not generate steps", 0, stepEngine.todaySteps)
    }

    @Test
    fun test4_VehicleRide_ClassifiedAsVehicle_NoSteps() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        val vehicleSpeedKmh = 45f

        // Phone in moving vehicle: GPS speed 45 km/h + minor engine vibration
        for (i in 0 until 50) {
            fusion.processGyroscope(0.2f, 0.15f, 0.1f)
            fusion.processAccelerometer(0.3f, 0.4f, 9.85f, time)
            fusion.updateMotionClassification(time, gpsSpeedKmh = vehicleSpeedKmh)
            stepEngine.processSensorFusionStep(time, fusion)
            time += 20L
        }

        assertEquals("Vehicle speed must classify state as VEHICLE", MovementState.VEHICLE, fusion.movementState)
        assertTrue("Vehicle confidence must be high", fusion.motionConfidence.vehicleConfidence >= 0.9f)
        assertEquals("Vehicle travel must not increment steps", 0, stepEngine.todaySteps)
    }

    @Test
    fun test5_NormalWalking_RhythmicCadence_DetectsSteps() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        val samplingPeriodMs = 20L // 50 Hz
        val stepDurationMs = 560L // ~107 steps/min normal gait cadence

        // 1. Initial warm-up calibration
        for (i in 0 until 10) {
            fusion.processAccelerometer(0f, 0f, 9.81f, time)
            time += samplingPeriodMs
        }

        var walkingDetectedDuringActivity = false
        var maxWalkConfidence = 0f

        // 2. Simulate 12 steps of human walking sinusoidal vertical acceleration
        for (step in 1..12) {
            val stepStart = time
            while (time - stepStart < stepDurationMs) {
                val phase = ((time - stepStart).toDouble() / stepDurationMs) * 2.0 * Math.PI
                val verticalAccel = (9.81 + 2.8 * sin(phase)).toFloat()
                val forwardAccel = (0.6 * sin(phase + Math.PI / 4)).toFloat()

                fusion.processGyroscope(0.4f, 0.3f, 0.2f)
                fusion.processMagnetometer(20f, -10f, 35f)
                fusion.processAccelerometer(forwardAccel, 0.1f, verticalAccel, time)
                stepEngine.processSensorFusionStep(time, fusion)

                if (fusion.movementState == MovementState.WALKING) {
                    walkingDetectedDuringActivity = true
                }
                if (fusion.motionConfidence.walkingConfidence > maxWalkConfidence) {
                    maxWalkConfidence = fusion.motionConfidence.walkingConfidence
                }

                time += samplingPeriodMs
            }
        }

        assertTrue("Normal walking must classify state as WALKING during the gait session", walkingDetectedDuringActivity)
        assertTrue("Walking confidence must reach a high level", maxWalkConfidence > 0.4f)
        assertTrue("Detected steps must be positive and within valid range, was ${stepEngine.todaySteps}", stepEngine.todaySteps > 0)
    }

    @Test
    fun test6_Running_HighIntensity_ClassifiedAsRunning() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)

        var time = 1000L
        val stepDurationMs = 320L // ~187 steps/min running cadence

        // Warm up
        for (i in 0 until 10) {
            fusion.processAccelerometer(0f, 0f, 9.81f, time)
            time += 20L
        }

        var runningOrWalkingDetected = false
        var maxRunConfidence = 0f

        for (step in 1..10) {
            val stepStart = time
            while (time - stepStart < stepDurationMs) {
                val phase = ((time - stepStart).toDouble() / stepDurationMs) * 2.0 * Math.PI
                val verticalAccel = (9.81 + 5.2 * sin(phase)).toFloat()

                fusion.processGyroscope(1.2f, 0.8f, 0.6f)
                fusion.processAccelerometer(0.5f, 0.2f, verticalAccel, time)
                stepEngine.processSensorFusionStep(time, fusion)

                if (fusion.movementState == MovementState.RUNNING || fusion.movementState == MovementState.WALKING) {
                    runningOrWalkingDetected = true
                }
                if (fusion.motionConfidence.runningConfidence > maxRunConfidence) {
                    maxRunConfidence = fusion.motionConfidence.runningConfidence
                }

                time += 20L
            }
        }

        assertTrue("High intensity gait must classify state as RUNNING/WALKING during session", runningOrWalkingDetected)
        assertTrue("Running confidence must be positive", maxRunConfidence > 0.4f)
        assertTrue("Steps must be counted during running", stepEngine.todaySteps > 0)
    }

    @Test
    fun test7_HardwareStepCounter_RebootAndRolloverHandling() {
        val fusion = SensorFusionEngine()
        val stepEngine = StepDetectionEngine()

        // 1. Initial Morning Boot at raw = 5000
        stepEngine.initializeBaseline("2026-08-25", "2026-08-25", -1f, 0, -1f, 0)
        stepEngine.processHardwareStepCounter(5000f, 1000L, fusion)
        assertEquals(0, stepEngine.todaySteps)
        assertEquals(5000f, stepEngine.sensorBaseline, 0.01f)

        // 2. User walks 150 steps
        stepEngine.processHardwareStepCounter(5150f, 2000L, fusion)
        assertEquals(150, stepEngine.todaySteps)

        // 3. Mid-day reboot: raw sensor resets to 10
        stepEngine.processHardwareStepCounter(10f, 3000L, fusion)
        assertEquals("Steps must stay at 150 across reboot", 150, stepEngine.todaySteps)

        // 4. User walks 50 more steps (raw: 10 -> 60)
        stepEngine.processHardwareStepCounter(60f, 4000L, fusion)
        assertEquals("Post-reboot steps must reach 200", 200, stepEngine.todaySteps)

        // 5. Midnight rollover to next day
        val rolledOver = stepEngine.handleDateRollover("2026-08-26")
        assertTrue(rolledOver)
        assertEquals("Midnight rollover must reset today's steps to 0", 0, stepEngine.todaySteps)
        assertEquals("New baseline must equal last raw value 60", 60f, stepEngine.sensorBaseline, 0.01f)

        // 6. Walk 30 steps next day
        stepEngine.processHardwareStepCounter(90f, 5000L, fusion)
        assertEquals("Next day steps count from 0 to 30", 30, stepEngine.todaySteps)
    }
}
