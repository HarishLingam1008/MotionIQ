package com.example.data.sensor

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Authoritative StepSensorManager for MotionIQ.
 *
 * Architecture & Responsibilities:
 * 1. Hardware Step Sensors (Authoritative Primary Source):
 *    - Priority 1: Sensor.TYPE_STEP_COUNTER (Monotonic cumulative hardware steps since boot)
 *    - Priority 2: Sensor.TYPE_STEP_DETECTOR (Single hardware step detector fallback)
 *    - No Fake / Accelerometer / Timer Step Generation: If neither hardware step sensor exists,
 *      reports "Step sensor unavailable on this device".
 *
 * 2. Persistent Daily Baseline:
 *    - todaySteps = (rawSensorSteps - sensorBaseline) + baseOffsetSteps
 *    - Preserves steps across device reboots and app restarts.
 *    - Handles midnight date rollovers and explicit user resets.
 *
 * 3. Secondary Multi-Sensor Fusion for Activity Classification:
 *    - Accelerometer, Gyroscope, Magnetometer, and Rotation Vector classify movement
 *      as "Walking", "Running", "Jogging", "Idle", or "Vehicle", and drive UI intensity meters.
 *    - Accelerometer NEVER increments step count.
 */
class StepSensorManager(private val context: Context) : SensorEventListener {

    companion object {
        private const val TAG = "MotionIQ_StepSensor"
        private const val PREFS_NAME = "motioniq_pedometer_prefs"
        private const val KEY_LAST_STEP_DATE = "last_step_date"
        private const val KEY_SENSOR_BASELINE = "sensor_baseline"
        private const val KEY_SAVED_TODAY_STEPS = "saved_today_steps"
        private const val KEY_OFFSET_STEPS = "offset_steps"
        private const val KEY_LAST_RAW_SENSOR = "last_raw_sensor_value"
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Hardware Sensors
    val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    val stepDetectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val gyroscopeSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    val rotationVectorSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    // Core Processing Engines
    val fusionEngine = SensorFusionEngine()
    val stepEngine = StepDetectionEngine()

    // Live UI State Flows
    private val _liveSteps = MutableStateFlow(0)
    val liveSteps: StateFlow<Int> = _liveSteps.asStateFlow()

    private val _activityState = MutableStateFlow("Idle")
    val activityState: StateFlow<String> = _activityState.asStateFlow()

    private val _sensorStatus = MutableStateFlow("Initializing Sensors")
    val sensorStatus: StateFlow<String> = _sensorStatus.asStateFlow()

    private val _isTracking = MutableStateFlow(true)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _liveIntensity = MutableStateFlow(0.0f)
    val liveIntensity: StateFlow<Float> = _liveIntensity.asStateFlow()

    private val _hasPermission = MutableStateFlow(true)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _movementState = MutableStateFlow(MovementState.STATIONARY)
    val movementState: StateFlow<MovementState> = _movementState.asStateFlow()

    private val _motionConfidence = MutableStateFlow(MotionConfidence())
    val motionConfidence: StateFlow<MotionConfidence> = _motionConfidence.asStateFlow()

    private val _fusionDiagnostics = MutableStateFlow(SensorFusionDiagnostics())
    val fusionDiagnostics: StateFlow<SensorFusionDiagnostics> = _fusionDiagnostics.asStateFlow()

    // Rotation Matrix state for orientation estimation
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val lastAccelForOrientation = FloatArray(3)
    private val lastMagForOrientation = FloatArray(3)
    private var hasAccelForOrientation = false
    private var hasMagForOrientation = false

    private var isListening = false
    private var manualOverrideState: String? = null

    init {
        logSensorAvailability()
        restorePersistedBaseline()
        checkPermission()
        updateSensorStatus()
    }

    private fun logSensorAvailability() {
        Log.i(TAG, "==================================================")
        Log.i(TAG, "MotionIQ Hardware Step Sensor Status:")
        Log.i(TAG, "TYPE_STEP_COUNTER: ${stepCounterSensor != null}")
        Log.i(TAG, "TYPE_STEP_DETECTOR: ${stepDetectorSensor != null}")
        Log.i(TAG, "TYPE_ACCELEROMETER: ${accelerometerSensor != null}")
        Log.i(TAG, "TYPE_GYROSCOPE: ${gyroscopeSensor != null}")
        Log.i(TAG, "==================================================")
    }

    fun checkPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        _hasPermission.value = granted
        if (!granted) {
            _sensorStatus.value = "MotionIQ needs Physical Activity permission to count your steps."
        } else {
            updateSensorStatus()
        }
        return granted
    }

    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun restorePersistedBaseline() {
        val today = getTodayDateString()
        val savedDate = prefs.getString(KEY_LAST_STEP_DATE, "") ?: ""
        val savedBaseline = prefs.getFloat(KEY_SENSOR_BASELINE, -1f)
        val savedOffset = prefs.getInt(KEY_OFFSET_STEPS, 0)
        val savedLastRaw = prefs.getFloat(KEY_LAST_RAW_SENSOR, -1f)
        val savedSteps = prefs.getInt(KEY_SAVED_TODAY_STEPS, 0)

        stepEngine.initializeBaseline(
            todayDate = today,
            savedDate = savedDate,
            savedBaseline = savedBaseline,
            savedBaseOffset = savedOffset,
            savedLastRaw = savedLastRaw,
            savedSteps = savedSteps
        )

        _liveSteps.value = stepEngine.todaySteps

        if (savedDate != today) {
            persistBaseline()
        }
    }

    fun persistBaseline() {
        prefs.edit()
            .putString(KEY_LAST_STEP_DATE, stepEngine.currentDateStr)
            .putFloat(KEY_SENSOR_BASELINE, stepEngine.sensorBaseline)
            .putInt(KEY_OFFSET_STEPS, stepEngine.baseOffsetSteps)
            .putFloat(KEY_LAST_RAW_SENSOR, stepEngine.lastRawSensorValue)
            .putInt(KEY_SAVED_TODAY_STEPS, stepEngine.todaySteps)
            .apply()
    }

    fun checkDateRollover() {
        val today = getTodayDateString()
        if (stepEngine.handleDateRollover(today)) {
            _liveSteps.value = stepEngine.todaySteps
            persistBaseline()
        }
    }

    private fun updateSensorStatus() {
        if (!_hasPermission.value) {
            _sensorStatus.value = "MotionIQ needs Physical Activity permission to count your steps."
            return
        }

        _sensorStatus.value = when {
            stepCounterSensor != null -> "Hardware Step Counter Active"
            stepDetectorSensor != null -> "Hardware Step Detector Active"
            else -> "Step sensor unavailable on this device"
        }
    }

    fun startListening() {
        if (isListening) return

        checkPermission()
        checkDateRollover()
        _isTracking.value = true

        // 1. Register Authoritative Hardware Step Sensors
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)
        } else if (stepDetectorSensor != null) {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // 2. Register Multi-Sensor telemetry for activity classification (Walking/Running/Idle)
        accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        gyroscopeSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        rotationVectorSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

        isListening = true
        updateSensorStatus()
        Log.i(TAG, "Step sensor listeners registered. Status: ${_sensorStatus.value}")
    }

    fun stopListening() {
        if (!isListening) return
        _isTracking.value = false
        sensorManager.unregisterListener(this)
        isListening = false
        manualOverrideState = null
        _activityState.value = "Idle"
        _liveIntensity.value = 0.0f
        _movementState.value = MovementState.STATIONARY
        Log.i(TAG, "Step sensor listeners unregistered.")
    }

    fun setInitialStepCount(steps: Int) {
        checkDateRollover()
        if (steps > 0 && steps > _liveSteps.value) {
            stepEngine.syncWithSavedSteps(steps)
            _liveSteps.value = stepEngine.todaySteps
            persistBaseline()
        }
    }

    fun resetSteps() {
        stepEngine.manualReset()
        _liveSteps.value = 0
        persistBaseline()
        Log.i(TAG, "Daily steps reset. New baseline: ${stepEngine.sensorBaseline}")
    }

    fun setManualActivityOverride(activity: String?) {
        manualOverrideState = activity
        _activityState.value = activity ?: "Idle"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isTracking.value || event == null) return

        checkDateRollover()
        val now = System.currentTimeMillis()
        val gpsSpeed = LocationTrackingManager.currentSpeedKmh.value

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val stepUpdated = stepEngine.processHardwareStepCounter(event.values[0], now)
                if (stepUpdated) {
                    _liveSteps.value = stepEngine.todaySteps
                    persistBaseline()
                    Log.i(TAG, "REAL STEP DETECTED (Step Counter): ${stepEngine.todaySteps}")
                }
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                // Secondary hardware fallback ONLY if TYPE_STEP_COUNTER is absent
                if (stepCounterSensor == null) {
                    val stepUpdated = stepEngine.processHardwareStepDetector(now)
                    if (stepUpdated) {
                        _liveSteps.value = stepEngine.todaySteps
                        persistBaseline()
                        Log.i(TAG, "REAL STEP DETECTED (Step Detector): ${stepEngine.todaySteps}")
                    }
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                // Used EXCLUSIVELY for activity classification & motion intensity.
                // NEVER increments steps.
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                lastAccelForOrientation[0] = ax
                lastAccelForOrientation[1] = ay
                lastAccelForOrientation[2] = az
                hasAccelForOrientation = true

                if (rotationVectorSensor == null && hasMagForOrientation) {
                    computeOrientationFromAccelAndMag()
                }

                val smoothedMag = fusionEngine.processAccelerometer(ax, ay, az, now)
                _liveIntensity.value = smoothedMag

                fusionEngine.updateMotionClassification(now, gpsSpeed, manualOverrideState)
                updateStateFlows(now)
            }

            Sensor.TYPE_GYROSCOPE -> {
                fusionEngine.processGyroscope(event.values[0], event.values[1], event.values[2])
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                val mx = event.values[0]
                val my = event.values[1]
                val mz = event.values[2]
                fusionEngine.processMagnetometer(mx, my, mz)

                lastMagForOrientation[0] = mx
                lastMagForOrientation[1] = my
                lastMagForOrientation[2] = mz
                hasMagForOrientation = true

                if (rotationVectorSensor == null && hasAccelForOrientation) {
                    computeOrientationFromAccelAndMag()
                }
            }

            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                fusionEngine.updateOrientation(azimuthDeg, pitchDeg, rollDeg)
            }
        }
    }

    private fun computeOrientationFromAccelAndMag() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelForOrientation, lastMagForOrientation)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthDeg = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
            val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
            fusionEngine.updateOrientation(azimuthDeg, pitchDeg, rollDeg)
        }
    }

    private fun updateStateFlows(now: Long) {
        val state = fusionEngine.movementState
        _movementState.value = state
        _motionConfidence.value = fusionEngine.motionConfidence

        if (manualOverrideState != null) {
            _activityState.value = manualOverrideState!!
        } else {
            _activityState.value = when (state) {
                MovementState.RUNNING -> "Running"
                MovementState.WALKING -> {
                    val speed = LocationTrackingManager.currentSpeedKmh.value
                    if (speed in 7.0f..10.0f) "Jogging" else "Walking"
                }
                MovementState.CYCLING -> "Cycling"
                MovementState.VEHICLE -> "Vehicle"
                MovementState.PHONE_MOVEMENT -> "Idle"
                MovementState.STATIONARY -> "Idle"
                MovementState.UNKNOWN -> "Idle"
            }
        }

        // Update Diagnostics Flow
        _fusionDiagnostics.value = SensorFusionDiagnostics(
            primarySensor = when {
                stepCounterSensor != null -> "Sensor.TYPE_STEP_COUNTER"
                stepDetectorSensor != null -> "Sensor.TYPE_STEP_DETECTOR"
                else -> "None"
            },
            fallbackSensor = when {
                stepCounterSensor != null && stepDetectorSensor != null -> "Sensor.TYPE_STEP_DETECTOR"
                else -> "None"
            },
            hasStepCounter = stepCounterSensor != null,
            hasStepDetector = stepDetectorSensor != null,
            hasAccelerometer = accelerometerSensor != null,
            hasGyroscope = gyroscopeSensor != null,
            hasMagnetometer = magnetometerSensor != null,
            hasRotationVector = rotationVectorSensor != null,
            currentHardwareStepValue = stepEngine.lastRawSensorValue,
            dailyBaseline = stepEngine.sensorBaseline,
            todaySteps = stepEngine.todaySteps,
            movementState = state,
            confidence = fusionEngine.motionConfidence,
            telemetry = fusionEngine.getTelemetrySnapshot(now),
            isPermissionGranted = _hasPermission.value,
            lastStepDetectedTimestamp = stepEngine.lastStepTimestamp,
            totalStepsValidated = stepEngine.totalStepsValidated,
            rejectedShakesCount = fusionEngine.rejectedShakesCount,
            isTracking = _isTracking.value
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getSafeDiagnostics(): StepSensorDiagnostics {
        return StepSensorDiagnostics(
            primarySensor = when {
                stepCounterSensor != null -> "Sensor.TYPE_STEP_COUNTER"
                stepDetectorSensor != null -> "Sensor.TYPE_STEP_DETECTOR"
                else -> "None"
            },
            fallbackSensor = when {
                stepCounterSensor != null && stepDetectorSensor != null -> "Sensor.TYPE_STEP_DETECTOR"
                else -> "None"
            },
            hasStepCounter = stepCounterSensor != null,
            hasStepDetector = stepDetectorSensor != null,
            hasAccelerometer = accelerometerSensor != null,
            currentSensorStepValue = stepEngine.lastRawSensorValue,
            baseline = stepEngine.sensorBaseline,
            todayDetectedSteps = _liveSteps.value,
            isPermissionGranted = _hasPermission.value
        )
    }
}

data class StepSensorDiagnostics(
    val primarySensor: String,
    val fallbackSensor: String,
    val hasStepCounter: Boolean,
    val hasStepDetector: Boolean,
    val hasAccelerometer: Boolean,
    val currentSensorStepValue: Float,
    val baseline: Float,
    val todayDetectedSteps: Int,
    val isPermissionGranted: Boolean
)
