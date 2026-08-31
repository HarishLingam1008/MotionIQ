package com.example.data.sensor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.service.StepTrackingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object StepTrackingManager {

    private const val PREFS_NAME = "motioniq_tracking_prefs"
    private const val KEY_IS_TRACKING = "is_tracking"
    private const val KEY_IS_PAUSED = "is_paused"
    private const val KEY_SESSION_SECONDS = "session_seconds"
    private const val KEY_SESSION_START_STEPS = "session_start_steps"
    private const val KEY_SESSION_STEPS = "session_steps"
    private const val KEY_ACTIVITY_TYPE = "activity_type"

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences
    private var sensorManager: StepSensorManager? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _sessionSeconds = MutableStateFlow(0L)
    val sessionSeconds: StateFlow<Long> = _sessionSeconds.asStateFlow()

    private val _sessionStartSteps = MutableStateFlow(0)
    val sessionStartSteps: StateFlow<Int> = _sessionStartSteps.asStateFlow()

    private val _sessionSteps = MutableStateFlow(0)
    val sessionSteps: StateFlow<Int> = _sessionSteps.asStateFlow()

    private val _sessionDistanceMeters = MutableStateFlow(0.0)
    val sessionDistanceMeters: StateFlow<Double> = _sessionDistanceMeters.asStateFlow()

    private val _sessionCalories = MutableStateFlow(0.0)
    val sessionCalories: StateFlow<Double> = _sessionCalories.asStateFlow()

    // Weight and Stride length default values
    var userWeightKg: Double = 70.0
    var userStrideMeters: Double = 0.75

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        if (!::appContext.isInitialized) {
            appContext = applicationContext
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            restoreState()
        }
        if (sensorManager == null) {
            sensorManager = StepSensorManager(appContext).apply {
                startListening()
            }
            // Observe live steps from StepSensorManager
            scope.launch {
                sensorManager?.liveSteps?.collect { totalSteps ->
                    if (_isTracking.value && !_isPaused.value) {
                        val currentActivity = sensorManager?.activityState?.value ?: "Walking"
                        if (currentActivity != "Cycling" && currentActivity != "Vehicle") {
                            if (_sessionStartSteps.value <= 0) {
                                _sessionStartSteps.value = totalSteps
                            }
                            val stepsInSession = (totalSteps - _sessionStartSteps.value).coerceAtLeast(0)
                            _sessionSteps.value = stepsInSession
                            updateCalculatedMetrics()
                        }
                    }
                }
            }
        }
    }

    fun getSensorManager(context: Context? = null): StepSensorManager {
        if (context != null) {
            init(context)
        }
        if (sensorManager == null && ::appContext.isInitialized) {
            sensorManager = StepSensorManager(appContext).apply { startListening() }
        }
        return sensorManager ?: throw IllegalStateException("StepTrackingManager is not initialized with Context")
    }

    private fun restoreState() {
        val tracking = prefs.getBoolean(KEY_IS_TRACKING, false)
        val paused = prefs.getBoolean(KEY_IS_PAUSED, false)
        val seconds = prefs.getLong(KEY_SESSION_SECONDS, 0L)
        val startSteps = prefs.getInt(KEY_SESSION_START_STEPS, 0)
        val sessionSteps = prefs.getInt(KEY_SESSION_STEPS, 0)

        _isTracking.value = tracking
        _isPaused.value = paused
        _sessionSeconds.value = seconds
        _sessionStartSteps.value = startSteps
        _sessionSteps.value = sessionSteps
        updateCalculatedMetrics()

        if (tracking && !paused) {
            startTimer()
        }
    }

    fun updateCalculatedMetrics() {
        val selectedAct = LocationTrackingManager.selectedActivity.value
        val gpsDist = LocationTrackingManager.totalDistanceMeters.value

        val distM = if (gpsDist > 0.0 || selectedAct == "Cycling") {
            gpsDist
        } else {
            val effStride = when (selectedAct) {
                "Running" -> userStrideMeters * 1.3
                "Jogging" -> userStrideMeters * 1.15
                else -> userStrideMeters
            }
            (_sessionSteps.value * effStride).coerceAtLeast(0.0)
        }

        _sessionDistanceMeters.value = distM

        val met = when (selectedAct) {
            "Running" -> 9.8
            "Jogging" -> 7.0
            "Cycling" -> 7.5
            else -> 3.5
        }
        val hours = _sessionSeconds.value / 3600.0
        val cal = met * userWeightKg * hours
        _sessionCalories.value = if (cal > 0.0) cal else ((distM / 1000.0) * userWeightKg * 0.75)
    }

    fun startTrackingSession(context: Context) {
        init(context)

        if (_isTracking.value && !_isPaused.value) return // Already active session running

        if (!_isTracking.value) {
            _isTracking.value = true
            _isPaused.value = false
            _sessionSeconds.value = 0L
            _sessionStartSteps.value = sensorManager?.liveSteps?.value ?: 0
            _sessionSteps.value = 0
            updateCalculatedMetrics()
            LocationTrackingManager.startGpsTracking(appContext)
        } else if (_isPaused.value) {
            _isPaused.value = false
        }

        saveState()
        startTimer()

        val serviceIntent = Intent(appContext, StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(serviceIntent)
        } else {
            appContext.startService(serviceIntent)
        }
    }

    fun pauseTrackingSession(context: Context) {
        init(context)
        if (!_isTracking.value || _isPaused.value) return // Already paused or not tracking

        _isPaused.value = true
        stopTimer()
        saveState()

        val serviceIntent = Intent(appContext, StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_PAUSE
        }
        appContext.startService(serviceIntent)
    }

    fun resumeTrackingSession(context: Context) {
        init(context)
        if (!_isTracking.value || !_isPaused.value) return // Already active or not tracking

        _isPaused.value = false
        startTimer()
        saveState()

        val serviceIntent = Intent(appContext, StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_RESUME
        }
        appContext.startService(serviceIntent)
    }

    fun stopTrackingSession(context: Context) {
        init(context)
        if (!_isTracking.value) return

        _isTracking.value = false
        _isPaused.value = false
        stopTimer()
        LocationTrackingManager.stopGpsTracking()
        clearSavedState()

        val serviceIntent = Intent(appContext, StepTrackingService::class.java).apply {
            action = StepTrackingService.ACTION_STOP
        }
        appContext.startService(serviceIntent)
    }

    private fun startTimer() {
        stopTimer()
        timerJob = scope.launch {
            var loopCount = 0
            while (_isTracking.value && !_isPaused.value) {
                delay(1000L)
                _sessionSeconds.value += 1
                updateCalculatedMetrics()
                loopCount++
                if (loopCount % 5 == 0) {
                    saveState()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun saveState() {
        prefs.edit()
            .putBoolean(KEY_IS_TRACKING, _isTracking.value)
            .putBoolean(KEY_IS_PAUSED, _isPaused.value)
            .putLong(KEY_SESSION_SECONDS, _sessionSeconds.value)
            .putInt(KEY_SESSION_START_STEPS, _sessionStartSteps.value)
            .putInt(KEY_SESSION_STEPS, _sessionSteps.value)
            .apply()
    }

    private fun clearSavedState() {
        prefs.edit()
            .putBoolean(KEY_IS_TRACKING, false)
            .putBoolean(KEY_IS_PAUSED, false)
            .putLong(KEY_SESSION_SECONDS, 0L)
            .putInt(KEY_SESSION_START_STEPS, 0)
            .putInt(KEY_SESSION_STEPS, 0)
            .apply()
        _sessionSeconds.value = 0L
        _sessionSteps.value = 0
        _sessionDistanceMeters.value = 0.0
        _sessionCalories.value = 0.0
    }

    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d:%02d", 0, minutes, secs)
        }
    }
}
