package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey val id: String, // Format: "${userId}_${date}"
    val userId: String = "guest",
    val date: String, // Format: YYYY-MM-DD
    val steps: Int = 0,
    val calories: Double = 0.0,
    val distanceMeters: Double = 0.0,
    val activeMinutes: Int = 0,
    val waterIntakeMl: Int = 0
)
