package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyActivity::class,
        StepLog::class,
        UserProfile::class,
        GoalAchievement::class,
        MealLog::class,
        CompletedActivity::class,
        SavedRoute::class
    ],
    version = 9,
    exportSchema = false
)
abstract class MotionIQDatabase : RoomDatabase() {
    abstract fun dailyActivityDao(): DailyActivityDao
    abstract fun stepLogDao(): StepLogDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun goalAchievementDao(): GoalAchievementDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun completedActivityDao(): CompletedActivityDao
    abstract fun savedRouteDao(): SavedRouteDao

    companion object {
        @Volatile
        private var INSTANCE: MotionIQDatabase? = null

        fun getInstance(context: Context): MotionIQDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MotionIQDatabase::class.java,
                    "motioniq_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
