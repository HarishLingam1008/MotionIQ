package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_achievements")
data class GoalAchievement(
    @PrimaryKey val id: String, // Format: "${userId}_${achievementId}"
    val userId: String = "guest",
    val achievementId: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedTimestamp: Long? = null
)
