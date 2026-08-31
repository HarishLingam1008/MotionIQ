package com.example.data.sensor

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * SensorFusionEngine combines Accelerometer, Gyroscope, Magnetometer, and Orientation
 * telemetry to provide noise-filtered linear dynamics, orientation tracking,
 * and robust motion classification (Stationary, Walking, Running, Phone Movement, Vehicle).
 */
class SensorFusionEngine {

    companion object {
        const val GRAVITY_ALPHA = 0.82f
        const val SMOOTH_ALPHA = 0.28f
        const val WINDOW_SIZE = 32

        // Thresholds
        const val STATIONARY_MAG_THRESHOLD = 0.38f
        const val STATIONARY_GYRO_THRESHOLD = 0.18f
        const val SHAKE_MAG_THRESHOLD = 14.0f
        const val SHAKE_GYRO_THRESHOLD = 4.5f
    }

    // Gravity estimation state
    private val gravity = FloatArray(3) { 0f }
    private var isGravityInitialized = false

    // Telemetry state
    var rawAx = 0f; private set
    var rawAy = 0f; private set
    var rawAz = 0f; private set
    var rawMag = 0f; private set

    var linearAx = 0f; private set
    var linearAy = 0f; private set
    var linearAz = 0f; private set
    var linearMag = 0f; private set

    var smoothedMag = 0f; private set
    var prevSmoothedMag = 0f; private set

    var gx = 0f; private set
    var gy = 0f; private set
    var gz = 0f; private set
    var gyroMag = 0f; private set

    var mx = 0f; private set
    var my = 0f; private set
    var mz = 0f; private set
    var magMag = 0f; private set

    var azimuthDeg = 0f; private set
    var pitchDeg = 0f; private set
    var rollDeg = 0f; private set

    // Statistics rolling buffer
    private val magWindow = FloatArray(WINDOW_SIZE)
    private var windowIdx = 0
    private var windowFilled = false

    // Jerk (derivative of acceleration) buffer
    private var prevLinearMag = 0f
    private var jerkVariance = 0f

    // Classification & Confidence
    var movementState: MovementState = MovementState.STATIONARY; private set
    var motionConfidence: MotionConfidence = MotionConfidence(); private set

    // Shake & Anti-False-Positive Tracking
    var lastShakeTimestamp = 0L; private set
    var rejectedShakesCount = 0; private set

    /**
     * Process 3-axis Accelerometer sensor event.
     */
    fun processAccelerometer(x: Float, y: Float, z: Float, now: Long): Float {
        rawAx = x
        rawAy = y
        rawAz = z
        rawMag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // 1. Gravity Estimation (Low-Pass Filter)
        if (!isGravityInitialized) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            isGravityInitialized = true
        } else {
            gravity[0] = GRAVITY_ALPHA * gravity[0] + (1f - GRAVITY_ALPHA) * x
            gravity[1] = GRAVITY_ALPHA * gravity[1] + (1f - GRAVITY_ALPHA) * y
            gravity[2] = GRAVITY_ALPHA * gravity[2] + (1f - GRAVITY_ALPHA) * z
        }

        // 2. Linear Acceleration Vector (Gravity Removed)
        linearAx = x - gravity[0]
        linearAy = y - gravity[1]
        linearAz = z - gravity[2]
        linearMag = sqrt((linearAx * linearAx + linearAy * linearAy + linearAz * linearAz).toDouble()).toFloat()

        // 3. Low-Pass Smoothing
        prevSmoothedMag = smoothedMag
        smoothedMag = SMOOTH_ALPHA * linearMag + (1f - SMOOTH_ALPHA) * smoothedMag

        // 4. Update Rolling Statistics Window
        magWindow[windowIdx % WINDOW_SIZE] = smoothedMag
        windowIdx++
        if (windowIdx >= WINDOW_SIZE) windowFilled = true

        // 5. Calculate Jerk
        val currentJerk = abs(linearMag - prevLinearMag)
        jerkVariance = 0.2f * currentJerk + 0.8f * jerkVariance
        prevLinearMag = linearMag

        // 6. Check for Violent Shakes / Drops / Twists
        if (linearMag > SHAKE_MAG_THRESHOLD || gyroMag > SHAKE_GYRO_THRESHOLD) {
            lastShakeTimestamp = now
            rejectedShakesCount++
        }

        // 7. Update Classification
        updateMotionClassification(now)

        return smoothedMag
    }

    /**
     * Process 3-axis Gyroscope sensor event (rad/s).
     */
    fun processGyroscope(x: Float, y: Float, z: Float) {
        gx = x
        gy = y
        gz = z
        gyroMag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    /**
     * Process 3-axis Magnetometer sensor event (uT).
     */
    fun processMagnetometer(x: Float, y: Float, z: Float) {
        mx = x
        my = y
        mz = z
        magMag = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }

    /**
     * Update Orientation angles (Azimuth, Pitch, Roll in degrees).
     */
    fun updateOrientation(azimuth: Float, pitch: Float, roll: Float) {
        azimuthDeg = azimuth
        pitchDeg = pitch
        rollDeg = roll
    }

    /**
     * Calculate dynamic adaptive threshold for step peak detection.
     * Uses rolling window mean + standard deviation multiplier.
     */
    fun calculateDynamicAdaptiveThreshold(): Float {
        val count = if (windowFilled) WINDOW_SIZE else max(1, windowIdx)
        var sum = 0f
        for (i in 0 until count) {
            sum += magWindow[i]
        }
        val mean = sum / count

        var varianceSum = 0f
        for (i in 0 until count) {
            val diff = magWindow[i] - mean
            varianceSum += diff * diff
        }
        val stdDev = sqrt((varianceSum / count).toDouble()).toFloat()

        // Adaptive peak threshold clamped to realistic walking dynamics [1.3 - 4.5] m/s^2
        return (mean + 0.55f * stdDev).coerceIn(1.35f, 4.5f)
    }

    /**
     * Computes motion classification and continuous confidence metrics.
     */
    fun updateMotionClassification(
        now: Long,
        gpsSpeedKmh: Float = 0f,
        manualActivityOverride: String? = null
    ) {
        if (manualActivityOverride != null) {
            movementState = when (manualActivityOverride.lowercase()) {
                "walking" -> MovementState.WALKING
                "running", "jogging" -> MovementState.RUNNING
                "cycling" -> MovementState.CYCLING
                "vehicle" -> MovementState.VEHICLE
                else -> MovementState.STATIONARY
            }
            motionConfidence = MotionConfidence(
                movementConfidence = 1.0f,
                walkingConfidence = if (movementState == MovementState.WALKING) 0.95f else 0.0f,
                runningConfidence = if (movementState == MovementState.RUNNING) 0.95f else 0.0f,
                vehicleConfidence = if (movementState == MovementState.VEHICLE) 0.95f else 0.0f
            )
            return
        }

        val isShakingRecently = (now - lastShakeTimestamp) < 1000L

        // Continuous Confidences calculation
        val moveConf = ((smoothedMag - 0.2f) / 1.5f).coerceIn(0.0f, 1.0f)
        val vehicleConf = when {
            gpsSpeedKmh > 22.0f -> 0.95f
            gpsSpeedKmh > 15.0f && smoothedMag < 1.0f -> 0.80f
            jerkVariance > 2.5f && smoothedMag in 0.4f..1.5f && gyroMag < 0.6f -> 0.65f // engine hum vibration
            else -> 0.0f
        }

        val walkConf = when {
            isShakingRecently || gyroMag > SHAKE_GYRO_THRESHOLD -> 0.0f
            vehicleConf > 0.6f -> 0.0f
            smoothedMag in 0.8f..3.8f && gyroMag in 0.1f..3.2f -> {
                ((smoothedMag - 0.6f) / 2.0f).coerceIn(0.4f, 0.95f)
            }
            smoothedMag in 0.4f..0.8f -> 0.35f // slow pace
            else -> 0.0f
        }

        val runConf = when {
            isShakingRecently -> 0.0f
            vehicleConf > 0.6f -> 0.0f
            smoothedMag > 2.2f || gpsSpeedKmh > 9.0f -> {
                ((smoothedMag - 1.8f) / 2.0f).coerceIn(0.5f, 0.98f)
            }
            else -> 0.0f
        }

        motionConfidence = MotionConfidence(
            movementConfidence = moveConf,
            walkingConfidence = walkConf,
            runningConfidence = runConf,
            vehicleConfidence = vehicleConf
        )

        // Classify discrete movement state
        movementState = when {
            vehicleConf >= 0.7f || gpsSpeedKmh > 22.0f -> MovementState.VEHICLE
            isShakingRecently || (gyroMag > SHAKE_GYRO_THRESHOLD && !isGravityInitialized) -> MovementState.PHONE_MOVEMENT
            gyroMag > 3.0f && smoothedMag < 1.2f -> MovementState.PHONE_MOVEMENT
            runConf > 0.6f -> MovementState.RUNNING
            walkConf >= 0.40f -> MovementState.WALKING
            smoothedMag < STATIONARY_MAG_THRESHOLD && gyroMag < STATIONARY_GYRO_THRESHOLD -> MovementState.STATIONARY
            smoothedMag >= STATIONARY_MAG_THRESHOLD && gyroMag > 0.8f -> MovementState.PHONE_MOVEMENT
            else -> MovementState.STATIONARY
        }
    }

    /**
     * Get current raw & preprocessed telemetry snapshot.
     */
    fun getTelemetrySnapshot(now: Long): SensorRawTelemetry {
        return SensorRawTelemetry(
            ax = rawAx,
            ay = rawAy,
            az = rawAz,
            gx = gx,
            gy = gy,
            gz = gz,
            mx = mx,
            my = my,
            mz = mz,
            rawMag = rawMag,
            linearMag = linearMag,
            smoothedMag = smoothedMag,
            dynamicThreshold = calculateDynamicAdaptiveThreshold(),
            gyroMag = gyroMag,
            magMag = magMag,
            azimuthDeg = azimuthDeg,
            pitchDeg = pitchDeg,
            rollDeg = rollDeg,
            timestamp = now
        )
    }

    fun reset() {
        isGravityInitialized = false
        windowIdx = 0
        windowFilled = false
        smoothedMag = 0f
        prevSmoothedMag = 0f
        lastShakeTimestamp = 0L
        rejectedShakesCount = 0
        movementState = MovementState.STATIONARY
        motionConfidence = MotionConfidence()
    }
}
