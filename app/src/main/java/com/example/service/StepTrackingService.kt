package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.CompletedActivity
import com.example.data.local.MotionIQDatabase
import com.example.data.repository.MotionRepository
import com.example.data.sensor.StepTrackingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepTrackingService : Service() {

    companion object {
        const val CHANNEL_ID = "motioniq_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.action.START_TRACKING"
        const val ACTION_PAUSE = "com.example.action.PAUSE_TRACKING"
        const val ACTION_RESUME = "com.example.action.RESUME_TRACKING"
        const val ACTION_STOP = "com.example.action.STOP_TRACKING"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        StepTrackingManager.init(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForegroundWithNotification()
                startUpdateLoop()
            }
            ACTION_PAUSE -> {
                updateJob?.cancel()
                updateNotification(force = true)
            }
            ACTION_RESUME -> {
                startUpdateLoop()
                updateNotification(force = true)
            }
            ACTION_STOP -> {
                saveSessionRecordAndStop()
            }
        }

        return START_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MotionIQ:StepTrackingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // 10 minutes timeout refresh
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MotionIQ Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live foreground tracking notification for steps, distance, calories and duration"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private var lastNotificationTime = 0L

    private fun startForegroundWithNotification() {
        lastNotificationTime = System.currentTimeMillis()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            startForeground(NOTIFICATION_ID, notification, serviceTypes)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (StepTrackingManager.isTracking.value) {
                delay(2000L)
                updateNotification()
            }
        }
    }

    private fun updateNotification(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastNotificationTime < 1800L) {
            return
        }
        lastNotificationTime = now
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            // Ignore transient notification errors
        }
    }

    private fun buildNotification(): Notification {
        val isPaused = StepTrackingManager.isPaused.value
        val sensorManager = StepTrackingManager.getSensorManager()

        val steps = sensorManager.liveSteps.value
        val distKm = StepTrackingManager.sessionDistanceMeters.value / 1000.0
        val calories = StepTrackingManager.sessionCalories.value
        val seconds = StepTrackingManager.sessionSeconds.value
        val rawActivity = sensorManager.activityState.value
        val currentActivity = if (isPaused) "$rawActivity (Paused)" else if (rawActivity.isBlank() || rawActivity == "Idle") "Walking" else rawActivity

        val durationStr = StepTrackingManager.formatDuration(seconds)

        val contentText = String.format(
            Locale.getDefault(),
            "Steps: %d • %.2f km • %.0f kcal • %s",
            steps, distKm, calories, durationStr
        )

        val bigText = StringBuilder()
            .append("Today's Steps:\n").append(steps).append("\n\n")
            .append("Distance:\n").append(String.format(Locale.getDefault(), "%.2f km", distKm)).append("\n\n")
            .append("Calories:\n").append(String.format(Locale.getDefault(), "%.0f kcal", calories)).append("\n\n")
            .append("Duration:\n").append(durationStr).append("\n\n")
            .append("Current Activity:\n").append(currentActivity)
            .toString()

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚶 MotionIQ Tracking")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Actions: Pause / Resume / Stop
        if (isPaused) {
            val resumeIntent = PendingIntent.getService(
                this,
                101,
                Intent(this, StepTrackingService::class.java).apply { action = ACTION_RESUME },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_play,
                "▶ Resume",
                resumeIntent
            )
        } else {
            val pauseIntent = PendingIntent.getService(
                this,
                102,
                Intent(this, StepTrackingService::class.java).apply { action = ACTION_PAUSE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "⏸ Pause",
                pauseIntent
            )
        }

        val stopIntent = PendingIntent.getService(
            this,
            103,
            Intent(this, StepTrackingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_delete,
            "■ Stop",
            stopIntent
        )

        return builder.build()
    }

    private fun saveSessionRecordAndStop() {
        val duration = StepTrackingManager.sessionSeconds.value
        val steps = StepTrackingManager.sessionSteps.value
        val distM = StepTrackingManager.sessionDistanceMeters.value
        val cal = StepTrackingManager.sessionCalories.value
        val activity = StepTrackingManager.getSensorManager().activityState.value.let {
            if (it.isBlank() || it == "Idle") "Walking" else it
        }

        StepTrackingManager.stopTrackingSession(this)
        updateJob?.cancel()

        serviceScope.launch(Dispatchers.IO) {
            try {
                if (duration > 2) {
                    val db = MotionIQDatabase.getInstance(applicationContext)
                    val repository = MotionRepository(
                        db.dailyActivityDao(),
                        db.stepLogDao(),
                        db.userProfileDao(),
                        db.goalAchievementDao(),
                        db.mealLogDao(),
                        db.completedActivityDao(),
                        db.savedRouteDao()
                    )

                    val dateStr = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault()).format(Date())
                    val record = CompletedActivity(
                        activityType = activity,
                        date = dateStr,
                        durationSeconds = duration,
                        steps = if (activity == "Cycling") null else steps,
                        distanceMeters = if (com.example.data.sensor.LocationTrackingManager.totalDistanceMeters.value > 0) com.example.data.sensor.LocationTrackingManager.totalDistanceMeters.value else distM,
                        calories = cal,
                        timestamp = System.currentTimeMillis()
                    )
                    repository.saveCompletedActivity(record)

                    val routePoints = com.example.data.sensor.LocationTrackingManager.routePoints.value
                    if (routePoints.isNotEmpty()) {
                        val routePointsJson = com.example.data.local.SavedRoute.encodePoints(routePoints)
                        val savedRoute = com.example.data.local.SavedRoute(
                            activityType = activity,
                            dateString = dateStr,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = duration,
                            distanceMeters = if (com.example.data.sensor.LocationTrackingManager.totalDistanceMeters.value > 0) com.example.data.sensor.LocationTrackingManager.totalDistanceMeters.value else distM,
                            caloriesBurned = cal,
                            stepCount = steps,
                            avgSpeedKmh = com.example.data.sensor.LocationTrackingManager.avgSpeedKmh.value,
                            maxSpeedKmh = com.example.data.sensor.LocationTrackingManager.maxSpeedKmh.value,
                            startLatitude = routePoints.first().latitude,
                            startLongitude = routePoints.first().longitude,
                            endLatitude = routePoints.last().latitude,
                            endLongitude = routePoints.last().longitude,
                            routePointsJson = routePointsJson
                        )
                        repository.saveRoute(savedRoute)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        releaseWakeLock()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
