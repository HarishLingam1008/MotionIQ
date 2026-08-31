package com.example.data.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.data.local.RoutePoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class GpsDiagnostics(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val speedKmh: Float = 0f,
    val speedMps: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val provider: String = "None",
    val timestamp: Long = 0L,
    val trackingState: String = "STANDBY", // "STANDBY", "ACTIVE", "PAUSED"
    val totalPoints: Int = 0,
    val distanceMeters: Double = 0.0,
    val rejectedJumpsCount: Int = 0,
    val hasGpsFix: Boolean = false
)

object LocationTrackingManager {

    private const val TAG = "MotionIQ_Location"

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var fallbackLocationListener: LocationListener? = null

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _routePoints = MutableStateFlow<List<RoutePoint>>(emptyList())
    val routePoints: StateFlow<List<RoutePoint>> = _routePoints.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh: StateFlow<Float> = _currentSpeedKmh.asStateFlow()

    private val _avgSpeedKmh = MutableStateFlow(0.0)
    val avgSpeedKmh: StateFlow<Double> = _avgSpeedKmh.asStateFlow()

    private val _maxSpeedKmh = MutableStateFlow(0.0)
    val maxSpeedKmh: StateFlow<Double> = _maxSpeedKmh.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0.0)
    val totalDistanceMeters: StateFlow<Double> = _totalDistanceMeters.asStateFlow()

    private val _gpsAccuracyMeters = MutableStateFlow(0f)
    val gpsAccuracyMeters: StateFlow<Float> = _gpsAccuracyMeters.asStateFlow()

    private val _altitudeMeters = MutableStateFlow(0.0)
    val altitudeMeters: StateFlow<Double> = _altitudeMeters.asStateFlow()

    private val _isGpsEnabled = MutableStateFlow(true)
    val isGpsEnabled: StateFlow<Boolean> = _isGpsEnabled.asStateFlow()

    private val _selectedActivity = MutableStateFlow("Walking")
    val selectedActivity: StateFlow<String> = _selectedActivity.asStateFlow()

    private val _isTrackingActive = MutableStateFlow(false)
    val isTrackingActive: StateFlow<Boolean> = _isTrackingActive.asStateFlow()

    private val _gpsDiagnostics = MutableStateFlow(GpsDiagnostics())
    val gpsDiagnostics: StateFlow<GpsDiagnostics> = _gpsDiagnostics.asStateFlow()

    private var lastRecordedLocation: Location? = null
    private var lastProcessedLocation: Location? = null
    private var trackingStartTime = 0L
    private var rejectedJumpsCount = 0

    // Kalman Filter location smoother variables
    private var kalmanLat: Double = 0.0
    private var kalmanLng: Double = 0.0
    private var kalmanVariance: Double = -1.0

    fun init(context: Context) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        }
        checkLocationServicesEnabled(context)
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = hasFineLocationPermission(context)
        val coarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Checks if Android Location Services are enabled on the device.
     */
    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val isEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        _isGpsEnabled.value = isEnabled
        return isEnabled
    }

    fun checkGpsProviderStatus(context: Context): Boolean {
        return isLocationServicesEnabled(context)
    }

    fun checkLocationServicesEnabled(context: Context): Boolean {
        return isLocationServicesEnabled(context)
    }

    fun checkPlayServicesAvailable(context: Context): Boolean {
        val availability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        return resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
    }

    /**
     * Validates that coordinates are legitimate real-world GPS values.
     * Rejects (0,0) Null Island, NaN/Infinite values, out of range coordinates.
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude.isNaN() || latitude.isInfinite() || longitude.isNaN() || longitude.isInfinite()) return false
        if (latitude < -90.0 || latitude > 90.0) return false
        if (longitude < -180.0 || longitude > 180.0) return false
        // Filter out Null Island (0.0, 0.0)
        if (abs(latitude) < 0.0001 && abs(longitude) < 0.0001) return false
        return true
    }

    fun isValidLocation(location: Location?): Boolean {
        if (location == null) return false
        if (!isValidCoordinate(location.latitude, location.longitude)) return false
        if (location.time <= 0L) return false
        return true
    }

    /**
     * Fetches current location immediately using FusedLocationProviderClient and fallbacks.
     */
    @SuppressLint("MissingPermission")
    fun fetchLastLocation(context: Context, onLocationReady: ((Location) -> Unit)? = null) {
        init(context)
        if (!isLocationServicesEnabled(context)) {
            Log.w(TAG, "Location Services are disabled on device.")
            return
        }
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted.")
            return
        }

        try {
            // 1. Request fresh high-accuracy location fix immediately
            fusedLocationClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                ?.addOnSuccessListener { location ->
                    if (isValidLocation(location)) {
                        Log.d(TAG, "Fresh GPS fix acquired: lat=${location.latitude}, lng=${location.longitude}, acc=±${location.accuracy}m")
                        processNewLocation(location, isHighPriority = true)
                        onLocationReady?.invoke(location)
                    } else {
                        // 2. Fallback to cached last location
                        fusedLocationClient?.lastLocation?.addOnSuccessListener { cachedLoc ->
                            if (isValidLocation(cachedLoc)) {
                                Log.d(TAG, "Cached location retrieved: lat=${cachedLoc.latitude}, lng=${cachedLoc.longitude}")
                                processNewLocation(cachedLoc, isHighPriority = false)
                                onLocationReady?.invoke(cachedLoc)
                            }
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "getCurrentLocation failed (${e.message}), trying cached location.")
                    fusedLocationClient?.lastLocation?.addOnSuccessListener { cachedLoc ->
                        if (isValidLocation(cachedLoc)) {
                            processNewLocation(cachedLoc, isHighPriority = false)
                            onLocationReady?.invoke(cachedLoc)
                        }
                    }
                }

            // 3. Fallback to native LocationManager providers for immediate cached coordinates
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (lm != null) {
                val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (e: Exception) { null }
                val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { null }
                val bestNative = when {
                    isValidLocation(gpsLoc) && isValidLocation(netLoc) -> if (gpsLoc!!.time >= netLoc!!.time) gpsLoc else netLoc
                    isValidLocation(gpsLoc) -> gpsLoc
                    isValidLocation(netLoc) -> netLoc
                    else -> null
                }
                if (bestNative != null && (_currentLocation.value == null || bestNative.time > (_currentLocation.value?.time ?: 0L))) {
                    processNewLocation(bestNative, isHighPriority = false)
                    onLocationReady?.invoke(bestNative)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in fetchLastLocation: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchLastLocation: ${e.message}", e)
        }
    }

    /**
     * Starts continuous real-time GPS location updates.
     * Uses Priority.PRIORITY_HIGH_ACCURACY during active tracking sessions (2000-3000ms).
     * Uses balanced/high-accuracy standby mode when viewing the map.
     */
    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates(context: Context, isHighFrequencyTracking: Boolean = false) {
        init(context)
        if (!isLocationServicesEnabled(context)) {
            Log.w(TAG, "Location Services are disabled. Cannot start continuous updates.")
            return
        }
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "Location permissions not granted.")
            return
        }

        // Stop existing updates if configuration changed
        if (locationCallback != null) {
            stopContinuousLocationUpdates()
        }

        val isFine = hasFineLocationPermission(context)
        val priority = if (isFine || isHighFrequencyTracking) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val intervalMs = if (isHighFrequencyTracking) 2000L else 3000L
        val minIntervalMs = if (isHighFrequencyTracking) 1000L else 1500L
        val minDistanceM = if (isHighFrequencyTracking) 1.0f else 0.0f

        val locationRequest = LocationRequest.Builder(priority, intervalMs).apply {
            setMinUpdateIntervalMillis(minIntervalMs)
            setMinUpdateDistanceMeters(minDistanceM)
            if (isFine) {
                setWaitForAccurateLocation(true)
            }
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    processNewLocation(location, isHighPriority = isHighFrequencyTracking)
                }
            }
        }

        try {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            Log.d(TAG, "Continuous GPS updates started (highAccuracy=$isFine, tracking=$isHighFrequencyTracking, interval=${intervalMs}ms).")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException in requestLocationUpdates: ${e.message}")
            locationCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates from FusedLocationClient: ${e.message}", e)
            locationCallback = null
            startFallbackLocationManager(context)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startFallbackLocationManager(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        if (!hasLocationPermission(context)) return

        if (fallbackLocationListener == null) {
            fallbackLocationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    processNewLocation(location, isHighPriority = _isTrackingActive.value)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) { _isGpsEnabled.value = true }
                override fun onProviderDisabled(provider: String) { checkLocationServicesEnabled(context) }
            }
            try {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 1.0f, fallbackLocationListener!!, Looper.getMainLooper())
                }
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2.0f, fallbackLocationListener!!, Looper.getMainLooper())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting fallback LocationManager: ${e.message}")
            }
        }
    }

    fun stopContinuousLocationUpdates() {
        locationCallback?.let {
            try {
                fusedLocationClient?.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing FusedLocationClient updates: ${e.message}")
            }
        }
        locationCallback = null

        fallbackLocationListener?.let {
            // Unregister if needed
        }
    }

    fun setSelectedActivity(activity: String) {
        _selectedActivity.value = activity
    }

    @SuppressLint("MissingPermission")
    fun startGpsTracking(context: Context) {
        init(context)
        if (!isLocationServicesEnabled(context)) {
            Log.w(TAG, "Location Services are disabled. Cannot start GPS tracking session.")
            return
        }
        _isTrackingActive.value = true
        resetSession()
        trackingStartTime = System.currentTimeMillis()
        fetchLastLocation(context)
        startContinuousLocationUpdates(context, isHighFrequencyTracking = true)
        updateDiagnostics()
    }

    fun stopGpsTracking() {
        _isTrackingActive.value = false
        updateDiagnostics()
        // Keep location updates active for map display but at standard power interval
    }

    fun resetSession() {
        _routePoints.value = emptyList()
        _currentSpeedKmh.value = 0f
        _avgSpeedKmh.value = 0.0
        _maxSpeedKmh.value = 0.0
        _totalDistanceMeters.value = 0.0
        _gpsAccuracyMeters.value = 0f
        _altitudeMeters.value = 0.0
        lastRecordedLocation = null
        lastProcessedLocation = null
        trackingStartTime = 0L
        rejectedJumpsCount = 0
        kalmanVariance = -1.0
        updateDiagnostics()
    }

    /**
     * 1D Kalman Filter on latitude and longitude coordinates.
     * Prevents noisy jitter while preserving smooth real-world motion trajectory.
     */
    private fun filterAndSmoothLocation(rawLocation: Location): Location {
        val accuracy = if (rawLocation.accuracy > 0f) rawLocation.accuracy else 8f
        if (kalmanVariance < 0) {
            kalmanLat = rawLocation.latitude
            kalmanLng = rawLocation.longitude
            kalmanVariance = (accuracy * accuracy).toDouble()
            return rawLocation
        }

        val timeDeltaMs = if (lastProcessedLocation != null) {
            (rawLocation.time - lastProcessedLocation!!.time).coerceAtLeast(300L)
        } else 1000L

        kalmanVariance += (timeDeltaMs / 1000.0) * 1.5

        val kGain = kalmanVariance / (kalmanVariance + (accuracy * accuracy))
        kalmanLat += kGain * (rawLocation.latitude - kalmanLat)
        kalmanLng += kGain * (rawLocation.longitude - kalmanLng)
        kalmanVariance *= (1.0 - kGain)

        val smoothed = Location(rawLocation).apply {
            latitude = kalmanLat
            longitude = kalmanLng
        }
        return smoothed
    }

    /**
     * Core GPS Processing Engine:
     * 1. Validates coordinates and rejects invalid/impossible values.
     * 2. Filters out low-accuracy fixes (> 60m for route recording).
     * 3. Rejects stationary jitter to prevent fake distance accumulation.
     * 4. Implements Jump Protection to reject impossible speed spikes.
     * 5. Computes geographical distance, real-time speed, and records route points.
     */
    private fun processNewLocation(rawLocation: Location, isHighPriority: Boolean = false) {
        if (!isValidLocation(rawLocation)) {
            Log.w(TAG, "Rejected invalid GPS location (lat=${rawLocation.latitude}, lng=${rawLocation.longitude})")
            return
        }

        val accuracy = rawLocation.accuracy
        _gpsAccuracyMeters.value = accuracy

        // For visual dot display on map: allow fixes with accuracy <= 100m
        if (accuracy > 100f && _currentLocation.value != null) {
            Log.d(TAG, "Skipping map update: low accuracy (±${accuracy}m)")
            return
        }

        val smoothedLocation = filterAndSmoothLocation(rawLocation)
        _currentLocation.value = smoothedLocation
        _altitudeMeters.value = smoothedLocation.altitude
        lastProcessedLocation = smoothedLocation

        // Calculate speed (m/s -> km/h)
        val rawSpeedKmh = if (smoothedLocation.hasSpeed() && smoothedLocation.speed >= 0f) {
            smoothedLocation.speed * 3.6f
        } else if (lastRecordedLocation != null) {
            val dist = lastRecordedLocation!!.distanceTo(smoothedLocation)
            val timeSec = abs(smoothedLocation.time - lastRecordedLocation!!.time) / 1000.0
            if (timeSec > 0.3) (dist / timeSec).toFloat() * 3.6f else 0f
        } else 0f

        // Smooth speed indicator
        val speedKmh = if (_currentSpeedKmh.value > 0f) {
            (_currentSpeedKmh.value * 0.3f) + (rawSpeedKmh * 0.7f)
        } else rawSpeedKmh

        _currentSpeedKmh.value = speedKmh
        if (speedKmh > _maxSpeedKmh.value) {
            _maxSpeedKmh.value = speedKmh.toDouble()
        }

        // =========================================================================
        // ROUTE RECORDING & DISTANCE ACCUMULATION (When active tracking is ON)
        // =========================================================================
        if (_isTrackingActive.value) {
            // Accuracy threshold for route recording (reject fixes > 45m unless initial)
            val maxAllowedAccuracy = if (_routePoints.value.isEmpty()) 65f else 45f
            if (accuracy > maxAllowedAccuracy) {
                Log.d(TAG, "GPS accuracy (±${accuracy}m) too low for route recording. Threshold: ${maxAllowedAccuracy}m.")
                updateDiagnostics()
                return
            }

            val prev = lastRecordedLocation
            if (prev != null) {
                val distanceBetween = prev.distanceTo(smoothedLocation)
                val timeDeltaSec = (smoothedLocation.time - prev.time) / 1000.0

                // 1. Stationary Anti-Jitter Protection:
                // Prevents GPS noise from creating fake distances while user is standing still.
                val isStationaryNoise = distanceBetween < 1.8f ||
                        (speedKmh < 1.0f && distanceBetween < (accuracy * 0.35f).coerceAtLeast(1.8f))

                if (isStationaryNoise) {
                    Log.d(TAG, "Ignored stationary GPS jitter (${distanceBetween}m, speed=${speedKmh} km/h).")
                    updateDiagnostics()
                    return
                }

                // 2. Impossible Jump Protection:
                // If calculated movement implies unrealistic speed or teleports (> 45 m/s = 162 km/h),
                // or jump > 180m in under 3.5s, ignore the noisy spike.
                if (timeDeltaSec > 0) {
                    val impliedSpeedMps = distanceBetween / timeDeltaSec
                    val isImpossibleSpeed = impliedSpeedMps > 45.0 && accuracy > 3.0f
                    val isImpossibleTeleport = distanceBetween > 180.0 && timeDeltaSec < 3.5

                    if (isImpossibleSpeed || isImpossibleTeleport) {
                        Log.w(TAG, "Rejected impossible GPS jump: ${distanceBetween}m in ${timeDeltaSec}s (implied speed ${impliedSpeedMps * 3.6} km/h)")
                        rejectedJumpsCount++
                        updateDiagnostics()
                        return
                    }
                }

                // Legitimate movement detected: Accumulate exact GPS distance
                val newTotalDist = _totalDistanceMeters.value + distanceBetween
                _totalDistanceMeters.value = newTotalDist

                val newPoint = RoutePoint(
                    latitude = smoothedLocation.latitude,
                    longitude = smoothedLocation.longitude,
                    altitude = smoothedLocation.altitude,
                    speedKmh = speedKmh,
                    accuracyMeters = accuracy,
                    timestamp = smoothedLocation.time
                )
                _routePoints.value = _routePoints.value + newPoint
                lastRecordedLocation = smoothedLocation
            } else {
                // First valid recorded point of session
                val firstPoint = RoutePoint(
                    latitude = smoothedLocation.latitude,
                    longitude = smoothedLocation.longitude,
                    altitude = smoothedLocation.altitude,
                    speedKmh = speedKmh,
                    accuracyMeters = accuracy,
                    timestamp = smoothedLocation.time
                )
                _routePoints.value = listOf(firstPoint)
                lastRecordedLocation = smoothedLocation
            }

            // Update Average Speed over total session duration
            val totalSeconds = if (trackingStartTime > 0L) {
                (System.currentTimeMillis() - trackingStartTime) / 1000.0
            } else {
                val points = _routePoints.value
                if (points.size >= 2) (points.last().timestamp - points.first().timestamp) / 1000.0 else 0.0
            }

            if (totalSeconds > 2.0 && _totalDistanceMeters.value > 0.0) {
                val hours = totalSeconds / 3600.0
                val calculatedAvgKmh = (_totalDistanceMeters.value / 1000.0) / hours
                _avgSpeedKmh.value = if (calculatedAvgKmh.isNaN() || calculatedAvgKmh.isInfinite()) 0.0 else calculatedAvgKmh
            }
        }

        updateDiagnostics()
    }

    private fun updateDiagnostics() {
        val loc = _currentLocation.value
        _gpsDiagnostics.value = GpsDiagnostics(
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            accuracyMeters = _gpsAccuracyMeters.value,
            speedKmh = _currentSpeedKmh.value,
            speedMps = if (_currentSpeedKmh.value > 0f) _currentSpeedKmh.value / 3.6f else 0f,
            altitudeMeters = _altitudeMeters.value,
            provider = loc?.provider ?: if (fusedLocationClient != null) "fused" else "none",
            timestamp = loc?.time ?: System.currentTimeMillis(),
            trackingState = if (_isTrackingActive.value) "ACTIVE" else "STANDBY",
            totalPoints = _routePoints.value.size,
            distanceMeters = _totalDistanceMeters.value,
            rejectedJumpsCount = rejectedJumpsCount,
            hasGpsFix = loc != null && isValidCoordinate(loc.latitude, loc.longitude)
        )
    }
}
