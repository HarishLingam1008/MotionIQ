package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.MotionIQDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MotionIQDatabase.getInstance(context)
                val userId = intent.getStringExtra("userId") ?: ""
                val profiles = if (userId.isNotEmpty()) {
                    db.userProfileDao().getUserProfileOnce(userId)
                } else null
                
                if (profiles != null && profiles.notificationsEnabled && profiles.waterReminderEnabled) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val nowStr = sdf.format(Calendar.getInstance().time)
                    
                    val start = profiles.waterReminderStartTime
                    val end = profiles.waterReminderEndTime
                    
                    if (isTimeBetween(nowStr, start, end)) {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
                        val todayActivity = db.dailyActivityDao().getActivityForDateOnce(profiles.id, todayStr)
                        val currentWater = todayActivity?.waterIntakeMl ?: 0
                        val goalWater = profiles.dailyWaterGoalMl
                        val remainingWater = goalWater - currentWater

                        if (remainingWater > 0) {
                            WaterReminderManager.showNotification(
                                context,
                                "Time to Drink Water! 💧",
                                "You've drunk $currentWater ml / $goalWater ml today ($remainingWater ml remaining). Stay hydrated!"
                            )
                        }
                    }
                }
                
                // Reschedule next check
                val interval = profiles?.waterReminderIntervalMins ?: 60
                WaterReminderManager.scheduleNextReminder(context, interval)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isTimeBetween(nowStr: String, startStr: String, endStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val now = sdf.parse(nowStr) ?: return true
            val start = sdf.parse(startStr) ?: return true
            val end = sdf.parse(endStr) ?: return true
            if (start.before(end)) {
                !now.before(start) && !now.after(end)
            } else {
                !now.before(start) || !now.after(end)
            }
        } catch (e: Exception) {
            true
        }
    }
}

object WaterReminderManager {
    const val CHANNEL_ID = "water_reminder_channel"
    const val NOTIFICATION_ID = 8801
    const val REQUEST_CODE = 9901

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Water Hydration Reminders"
            val descriptionText = "Periodic reminders to drink water and stay hydrated."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun scheduleNextReminder(context: Context, intervalMins: Int = 60) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeInterval = intervalMins.coerceAtLeast(15)
        val triggerAtMs = System.currentTimeMillis() + (safeInterval * 60 * 1000L)
        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
