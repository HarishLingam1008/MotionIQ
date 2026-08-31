package com.example.data.repository

import com.example.data.local.CompletedActivity
import com.example.data.local.CompletedActivityDao
import com.example.data.local.DailyActivity
import com.example.data.local.DailyActivityDao
import com.example.data.local.GoalAchievement
import com.example.data.local.GoalAchievementDao
import com.example.data.local.MealLog
import com.example.data.local.MealLogDao
import com.example.data.local.StepLog
import com.example.data.local.StepLogDao
import com.example.data.local.UserProfile
import com.example.data.local.UserProfileDao
import com.example.data.local.SavedRoute
import com.example.data.local.SavedRouteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MotionRepository(
    private val dailyActivityDao: DailyActivityDao,
    private val stepLogDao: StepLogDao,
    private val userProfileDao: UserProfileDao,
    private val goalAchievementDao: GoalAchievementDao,
    private val mealLogDao: MealLogDao,
    private val completedActivityDao: CompletedActivityDao,
    private val savedRouteDao: SavedRouteDao
) {

    fun getUserProfile(userId: String): Flow<UserProfile?> = userProfileDao.getUserProfile(userId)
    fun getAllAchievements(userId: String): Flow<List<GoalAchievement>> = goalAchievementDao.getAllAchievements(userId)
    fun getAllActivities(userId: String): Flow<List<DailyActivity>> = dailyActivityDao.getAllActivities(userId)
    fun getCompletedActivities(userId: String): Flow<List<CompletedActivity>> = completedActivityDao.getAllCompletedActivities(userId)
    fun getSavedRoutes(userId: String): Flow<List<SavedRoute>> = savedRouteDao.getSavedRoutesForUser(userId)

    suspend fun saveRoute(route: SavedRoute): Long = withContext(Dispatchers.IO) {
        savedRouteDao.insertRoute(route)
    }

    suspend fun deleteRoute(route: SavedRoute) = withContext(Dispatchers.IO) {
        savedRouteDao.deleteRoute(route)
    }

    suspend fun saveCompletedActivity(activity: CompletedActivity) = withContext(Dispatchers.IO) {
        completedActivityDao.insertCompletedActivity(activity)
    }

    suspend fun deleteCompletedActivity(activity: CompletedActivity) = withContext(Dispatchers.IO) {
        completedActivityDao.deleteCompletedActivity(activity)
    }

    fun getActivityForToday(userId: String): Flow<DailyActivity?> {
        val todayStr = getTodayDateString()
        return dailyActivityDao.getActivityForDate(userId, todayStr)
    }

    fun getStepLogsForToday(userId: String): Flow<List<StepLog>> {
        val todayStr = getTodayDateString()
        return stepLogDao.getLogsForDate(userId, todayStr)
    }

    fun getMealLogsForToday(userId: String): Flow<List<MealLog>> {
        val todayStr = getTodayDateString()
        return mealLogDao.getMealLogsForDate(userId, todayStr)
    }

    suspend fun addMealLog(userId: String, mealLog: MealLog) = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val entry = mealLog.copy(userId = userId, date = todayStr)
        mealLogDao.insertMealLog(entry)
    }

    suspend fun deleteMealLog(mealLog: MealLog) = withContext(Dispatchers.IO) {
        mealLogDao.deleteMealLog(mealLog)
    }

    suspend fun clearTodayMeals(userId: String) = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        mealLogDao.clearMealsForDate(userId, todayStr)
    }

    suspend fun initializeUserIfNotExists(userId: String, displayName: String?) = withContext(Dispatchers.IO) {
        // Ensure UserProfile exists for this Firebase UID
        val existingProfile = userProfileDao.getUserProfileOnce(userId)
        if (existingProfile == null) {
            val name = if (!displayName.isNullOrBlank()) displayName else "User"
            userProfileDao.saveUserProfile(
                UserProfile(
                    id = userId,
                    name = name,
                    heightCm = 0.0,
                    weightKg = 0.0,
                    age = 0,
                    gender = ""
                )
            )
        }

        // Initialize Achievements for this Firebase UID
        val achievements = goalAchievementDao.getAllAchievements(userId).firstOrNull()
        if (achievements.isNullOrEmpty()) {
            goalAchievementDao.insertAchievements(getInitialAchievements(userId))
        }

        // Ensure today's activity entry exists with 0 values
        ensureTodayActivityExists(userId)
    }

    suspend fun updateStepsForToday(userId: String, newSteps: Int, activityType: String = "Walking") = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val profile = userProfileDao.getUserProfileOnce(userId) ?: UserProfile(id = userId)
        val compositeId = "${userId}_${todayStr}"

        // Calculate distance using user stride length (default 0.75m)
        val strideMeters = if (profile.strideLengthMeters > 0) profile.strideLengthMeters else 0.75
        val distanceMeters = newSteps * strideMeters
        val distanceKm = distanceMeters / 1000.0

        // Estimated Activity Calories burned based on steps/distance (0 if newSteps is 0 or weight is 0)
        val weight = if (profile.weightKg > 0) profile.weightKg else 70.0
        val activityCalories = if (newSteps > 0) distanceKm * weight * 0.75 else 0.0
        val activeMins = (newSteps / 100).coerceAtLeast(0)

        val existing = dailyActivityDao.getActivityForDateOnce(userId, todayStr)
        val updatedActivity = DailyActivity(
            id = compositeId,
            userId = userId,
            date = todayStr,
            steps = newSteps,
            calories = activityCalories,
            distanceMeters = distanceMeters,
            activeMinutes = activeMins,
            waterIntakeMl = existing?.waterIntakeMl ?: 0
        )

        dailyActivityDao.insertOrUpdateActivity(updatedActivity)

        // Log hourly step chunk
        if (newSteps > 0) {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            stepLogDao.insertStepLog(
                StepLog(
                    userId = userId,
                    date = todayStr,
                    hour = hour,
                    stepCount = newSteps,
                    activityType = activityType
                )
            )
        }

        checkAchievements(userId, updatedActivity, profile)
    }

    suspend fun updateWaterIntake(userId: String, amountMl: Int) = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val compositeId = "${userId}_${todayStr}"
        val existing = dailyActivityDao.getActivityForDateOnce(userId, todayStr)
            ?: DailyActivity(id = compositeId, userId = userId, date = todayStr)
        
        val newWater = (existing.waterIntakeMl + amountMl).coerceAtLeast(0)
        val updated = existing.copy(waterIntakeMl = newWater)
        dailyActivityDao.insertOrUpdateActivity(updated)

        val profile = userProfileDao.getUserProfileOnce(userId) ?: UserProfile(id = userId)
        checkAchievements(userId, updated, profile)
    }

    suspend fun unlockAchievement(userId: String, achievementId: String, title: String, description: String, iconName: String) = withContext(Dispatchers.IO) {
        val achievement = GoalAchievement(
            id = "${userId}_${achievementId}",
            userId = userId,
            achievementId = achievementId,
            title = title,
            description = description,
            iconName = iconName,
            isUnlocked = true,
            unlockedTimestamp = System.currentTimeMillis()
        )
        goalAchievementDao.insertAchievements(listOf(achievement))
    }

    suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        userProfileDao.saveUserProfile(profile)
    }

    suspend fun resetTodayData(userId: String) = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val compositeId = "${userId}_${todayStr}"
        val existing = dailyActivityDao.getActivityForDateOnce(userId, todayStr)
        dailyActivityDao.insertOrUpdateActivity(
            DailyActivity(
                id = compositeId,
                userId = userId,
                date = todayStr,
                steps = 0,
                calories = 0.0,
                distanceMeters = 0.0,
                activeMinutes = 0,
                waterIntakeMl = existing?.waterIntakeMl ?: 0
            )
        )
    }

    suspend fun clearAllDataForUser(userId: String) = withContext(Dispatchers.IO) {
        dailyActivityDao.clearAllForUser(userId)
        stepLogDao.clearAllForUser(userId)
        userProfileDao.clearAllForUser(userId)
        goalAchievementDao.clearAllForUser(userId)
        mealLogDao.clearAllForUser(userId)
        completedActivityDao.clearAllForUser(userId)
        savedRouteDao.clearAllForUser(userId)
        initializeUserIfNotExists(userId, null)
    }

    private suspend fun checkAchievements(userId: String, activity: DailyActivity, profile: UserProfile) {
        val currentAchievements = goalAchievementDao.getAllAchievements(userId).firstOrNull() ?: return

        currentAchievements.forEach { achievement ->
            if (!achievement.isUnlocked) {
                val shouldUnlock = when (achievement.achievementId) {
                    "first_step" -> activity.steps >= 1
                    "step_5k" -> activity.steps >= 5000
                    "step_10k" -> activity.steps >= 10000
                    "step_goal" -> activity.steps >= profile.dailyStepGoal
                    "water_2k" -> activity.waterIntakeMl >= profile.dailyWaterGoalMl
                    "active_30" -> activity.activeMinutes >= 30
                    else -> false
                }

                if (shouldUnlock) {
                    goalAchievementDao.updateAchievement(
                        achievement.copy(
                            isUnlocked = true,
                            unlockedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    suspend fun ensureTodayActivityExists(userId: String): DailyActivity = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val compositeId = "${userId}_${todayStr}"
        val existing = dailyActivityDao.getActivityForDateOnce(userId, todayStr)
        if (existing == null) {
            val newToday = DailyActivity(
                id = compositeId,
                userId = userId,
                date = todayStr,
                steps = 0,
                calories = 0.0,
                distanceMeters = 0.0,
                activeMinutes = 0,
                waterIntakeMl = 0
            )
            dailyActivityDao.insertOrUpdateActivity(newToday)
            newToday
        } else {
            existing
        }
    }

    private fun getInitialAchievements(userId: String): List<GoalAchievement> {
        val list = listOf(
            Triple("first_step", "First Step", "Logged your very first activity in MotionIQ"),
            Triple("step_5k", "5,000 Milestone", "Completed 5,000 steps in a single day"),
            Triple("step_10k", "10,000 Champion", "Hit the legendary 10k daily step mark"),
            Triple("step_goal", "Goal Smasher", "Reached 100% of your daily step goal"),
            Triple("water_2k", "Hydration Hero", "Reached your daily water intake target"),
            Triple("active_30", "Active 30", "Maintained 30+ minutes of active motion")
        )
        return list.map { (achId, title, desc) ->
            GoalAchievement(
                id = "${userId}_${achId}",
                userId = userId,
                achievementId = achId,
                title = title,
                description = desc,
                iconName = when (achId) {
                    "first_step" -> "directions_walk"
                    "step_5k" -> "military_tech"
                    "step_10k" -> "emoji_events"
                    "step_goal" -> "workspace_premium"
                    "water_2k" -> "water_drop"
                    else -> "timer"
                }
            )
        }
    }
}
