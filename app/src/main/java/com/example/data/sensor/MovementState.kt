package com.example.data.sensor

/**
 * Valid movement classification states for MotionIQ Sensor Fusion.
 *
 * Only WALKING and RUNNING contribute to valid walking steps.
 * STATIONARY, PHONE_MOVEMENT, VEHICLE, CYCLING, and UNKNOWN are strictly rejected.
 */
enum class MovementState(val displayName: String) {
    STATIONARY("Stationary"),
    WALKING("Walking"),
    RUNNING("Running"),
    CYCLING("Cycling"),
    VEHICLE("Vehicle"),
    PHONE_MOVEMENT("Phone Movement"),
    UNKNOWN("Unknown")
}

/**
 * Continuous confidence scores for each activity modality [0.0 - 1.0].
 */
data class MotionConfidence(
    val movementConfidence: Float = 0.0f,
    val walkingConfidence: Float = 0.0f,
    val runningConfidence: Float = 0.0f,
    val vehicleConfidence: Float = 0.0f
)

/**
 * Real-time raw and preprocessed sensor telemetry for developer diagnostics.
 */
data class SensorRawTelemetry(
    val ax: Float = 0.0f,
    val ay: Float = 0.0f,
    val az: Float = 0.0f,
    val gx: Float = 0.0f,
    val gy: Float = 0.0f,
    val gz: Float = 0.0f,
    val mx: Float = 0.0f,
    val my: Float = 0.0f,
    val mz: Float = 0.0f,
    val rawMag: Float = 0.0f,
    val linearMag: Float = 0.0f,
    val smoothedMag: Float = 0.0f,
    val dynamicThreshold: Float = 1.4f,
    val gyroMag: Float = 0.0f,
    val magMag: Float = 0.0f,
    val azimuthDeg: Float = 0.0f,
    val pitchDeg: Float = 0.0f,
    val rollDeg: Float = 0.0f,
    val timestamp: Long = 0L
)

/**
 * Developer Diagnostics payload detailing sensor fusion, state classification,
 * step engine metrics, and sensor availability.
 */
data class SensorFusionDiagnostics(
    val primarySensor: String = "None",
    val fallbackSensor: String = "None",
    val hasStepCounter: Boolean = false,
    val hasStepDetector: Boolean = false,
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasRotationVector: Boolean = false,
    val currentHardwareStepValue: Float = -1f,
    val dailyBaseline: Float = -1f,
    val todaySteps: Int = 0,
    val movementState: MovementState = MovementState.STATIONARY,
    val confidence: MotionConfidence = MotionConfidence(),
    val telemetry: SensorRawTelemetry = SensorRawTelemetry(),
    val isPermissionGranted: Boolean = true,
    val lastStepDetectedTimestamp: Long = 0L,
    val totalStepsValidated: Int = 0,
    val rejectedShakesCount: Int = 0,
    val isTracking: Boolean = true
)
