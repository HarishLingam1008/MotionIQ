package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "completed_activities")
data class CompletedActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "guest",
    val activityType: String, // "Walking", "Running", "Jogging", "Cycling"
    val date: String, // e.g. "Jul 27, 2026, 3:35 PM"
    val durationSeconds: Long,
    val steps: Int? = null, // null for Cycling
    val distanceMeters: Double = 0.0,
    val calories: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
