package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "guest",
    val name: String = "User",
    val email: String = "",
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val age: Int = 0,
    val gender: String = "",
    val dailyStepGoal: Int = 8000,
    val dailyWaterGoalMl: Int = 2500,
    val activityLevel: String = "Moderately Active", // "Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active"
    val dailyCalorieGoal: Int = 2200,
    val unitSystem: String = "Metric", // "Metric" or "Imperial"
    val themeMode: String = "System", // "System", "Dark", "Light"
    val strideLengthMeters: Double = 0.75,
    val fitnessGoal: String = "Weight Loss", // "Weight Loss", "Maintain Weight", "Muscle Gain"
    val dietPreference: String = "Non-Veg", // "Veg", "Non-Veg"
    val notificationsEnabled: Boolean = true,
    val waterReminderEnabled: Boolean = true,
    val waterReminderIntervalMins: Int = 60,
    val waterReminderStartTime: String = "08:00",
    val waterReminderEndTime: String = "22:00",
    val language: String = "English"
) {
    fun isProfileComplete(): Boolean {
        return age > 0 && gender.isNotBlank() && heightCm > 0.0 && weightKg > 0.0
    }
}
