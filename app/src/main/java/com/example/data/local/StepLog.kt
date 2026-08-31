package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_logs")
data class StepLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "guest",
    val date: String,
    val hour: Int,
    val stepCount: Int,
    val activityType: String = "Walking",
    val timestamp: Long = System.currentTimeMillis()
)
