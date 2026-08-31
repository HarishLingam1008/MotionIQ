package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyActivityDao {
    @Query("SELECT * FROM daily_activity WHERE userId = :userId AND date = :date")
    fun getActivityForDate(userId: String, date: String): Flow<DailyActivity?>

    @Query("SELECT * FROM daily_activity WHERE userId = :userId AND date = :date")
    suspend fun getActivityForDateOnce(userId: String, date: String): DailyActivity?

    @Query("SELECT * FROM daily_activity WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(userId: String, limit: Int = 30): Flow<List<DailyActivity>>

    @Query("SELECT * FROM daily_activity WHERE userId = :userId ORDER BY date DESC")
    fun getAllActivities(userId: String): Flow<List<DailyActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivity(activity: DailyActivity)

    @Query("UPDATE daily_activity SET waterIntakeMl = :waterMl WHERE userId = :userId AND date = :date")
    suspend fun updateWaterIntake(userId: String, date: String, waterMl: Int)

    @Query("DELETE FROM daily_activity WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM daily_activity")
    suspend fun clearAll()
}

@Dao
interface StepLogDao {
    @Query("SELECT * FROM step_logs WHERE userId = :userId AND date = :date ORDER BY hour ASC")
    fun getLogsForDate(userId: String, date: String): Flow<List<StepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLog(stepLog: StepLog)

    @Query("DELETE FROM step_logs WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM step_logs")
    suspend fun clearAll()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun getUserProfile(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    suspend fun getUserProfileOnce(userId: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile WHERE id = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}

@Dao
interface GoalAchievementDao {
    @Query("SELECT * FROM goal_achievements WHERE userId = :userId")
    fun getAllAchievements(userId: String): Flow<List<GoalAchievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<GoalAchievement>)

    @Update
    suspend fun updateAchievement(achievement: GoalAchievement)

    @Query("DELETE FROM goal_achievements WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM goal_achievements")
    suspend fun clearAll()
}

@Dao
interface MealLogDao {
    @Query("SELECT * FROM meal_logs WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    fun getMealLogsForDate(userId: String, date: String): Flow<List<MealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealLog(mealLog: MealLog)

    @Delete
    suspend fun deleteMealLog(mealLog: MealLog)

    @Query("DELETE FROM meal_logs WHERE userId = :userId AND date = :date")
    suspend fun clearMealsForDate(userId: String, date: String)

    @Query("DELETE FROM meal_logs WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM meal_logs")
    suspend fun clearAll()
}

@Dao
interface CompletedActivityDao {
    @Query("SELECT * FROM completed_activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllCompletedActivities(userId: String): Flow<List<CompletedActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedActivity(activity: CompletedActivity)

    @Delete
    suspend fun deleteCompletedActivity(activity: CompletedActivity)

    @Query("DELETE FROM completed_activities WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM completed_activities")
    suspend fun clearAll()
}

@Dao
interface SavedRouteDao {
    @Query("SELECT * FROM saved_routes WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSavedRoutesForUser(userId: String): Flow<List<SavedRoute>>

    @Query("SELECT * FROM saved_routes WHERE id = :id")
    suspend fun getRouteById(id: Long): SavedRoute?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRoute): Long

    @Delete
    suspend fun deleteRoute(route: SavedRoute)

    @Query("DELETE FROM saved_routes WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("DELETE FROM saved_routes")
    suspend fun clearAll()
}
