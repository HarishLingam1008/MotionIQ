package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.CompletedActivity
import com.example.data.local.DailyActivity
import com.example.data.local.GoalAchievement
import com.example.data.local.MealLog
import com.example.data.local.SavedRoute
import com.example.data.local.UserProfile
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.ui.components.BottomNavBar
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalorieCalculatorScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import android.content.Context

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    todayActivity: DailyActivity?,
    todayMeals: List<MealLog> = emptyList(),
    userProfile: UserProfile,
    allActivities: List<DailyActivity>,
    completedActivities: List<CompletedActivity> = emptyList(),
    savedRoutes: List<SavedRoute> = emptyList(),
    achievements: List<GoalAchievement>,
    liveSteps: Int,
    activityState: String,
    sensorStatus: String,
    isTracking: Boolean,
    isPaused: Boolean = false,
    sessionSeconds: Long = 0L,
    sessionSteps: Int = 0,
    sessionDistanceMeters: Double = 0.0,
    sessionCalories: Double = 0.0,
    liveIntensity: Float,
    isDarkMode: Boolean,
    formatDistance: (Double, Boolean) -> String,
    formatHeight: (Double, Boolean) -> String,
    formatWeight: (Double, Boolean) -> String,
    calculateBmi: (Double, Double) -> Double,
    getBmiCategory: (Double) -> Pair<String, String>,
    calculateBmr: (Double, Double, Int, String) -> Int = { _, _, _, _ -> 1600 },
    calculateDailyCaloriesNeeded: (Double, Double, Int, String, String) -> Int = { _, _, _, _, _ -> 2200 },
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onStartTrackingSession: (Context) -> Unit = {},
    onPauseTrackingSession: (Context) -> Unit = {},
    onResumeTrackingSession: (Context) -> Unit = {},
    onStopTrackingSession: (Context) -> Unit = {},
    onSetManualActivity: (String?) -> Unit = {},
    onSaveCompletedActivity: (CompletedActivity) -> Unit = {},
    onDeleteCompletedActivity: (CompletedActivity) -> Unit = {},
    onDeleteSavedRoute: (SavedRoute) -> Unit = {},
    onResetSteps: () -> Unit,
    onAddWater: (Int) -> Unit,
    onAddMealLog: (MealLog) -> Unit = {},
    onDeleteMealLog: (MealLog) -> Unit = {},
    onClearMeals: () -> Unit = {},
    onUpdateProfile: (UserProfile) -> Unit,
    onUpdateStepGoal: (Int) -> Unit,
    onUpdateWaterGoal: (Int) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleUnitSystem: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onResetData: () -> Unit,
    onSignOut: () -> Unit = {},
    fusionDiagnostics: SensorFusionDiagnostics = SensorFusionDiagnostics()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val hideBottomBarRoutes = listOf(Screen.Splash.route, Screen.Settings.route)
    val showBottomBar = currentRoute !in hideBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
                composable(Screen.Home.route) {
                    HomeDashboardScreen(
                        todayActivity = todayActivity,
                        userProfile = userProfile,
                        allActivities = allActivities,
                        liveSteps = liveSteps,
                        activityState = activityState,
                        sensorStatus = sensorStatus,
                        formatDistance = formatDistance,
                        onNavigateToActivity = { navController.navigate(Screen.Activity.route) },
                        onNavigateToGoals = { navController.navigate(Screen.Goals.route) },
                        onNavigateToMap = { navController.navigate(Screen.Map.route) },
                        onAddWater = onAddWater
                    )
                }

                composable(Screen.Activity.route) {
                    ActivityScreen(
                        liveSteps = liveSteps,
                        activityState = activityState,
                        sensorStatus = sensorStatus,
                        isTracking = isTracking,
                        isPaused = isPaused,
                        sessionSeconds = sessionSeconds,
                        sessionSteps = sessionSteps,
                        sessionDistanceMeters = sessionDistanceMeters,
                        sessionCalories = sessionCalories,
                        liveIntensity = liveIntensity,
                        userProfile = userProfile,
                        todayActivity = todayActivity,
                        completedActivities = completedActivities,
                        formatDistance = formatDistance,
                        onStartTracking = onStartTracking,
                        onStopTracking = onStopTracking,
                        onStartTrackingSession = onStartTrackingSession,
                        onPauseTrackingSession = onPauseTrackingSession,
                        onResumeTrackingSession = onResumeTrackingSession,
                        onStopTrackingSession = onStopTrackingSession,
                        onSetManualActivity = onSetManualActivity,
                        onSaveCompletedActivity = onSaveCompletedActivity,
                        onDeleteCompletedActivity = onDeleteCompletedActivity,
                        onResetSteps = onResetSteps
                    )
                }

                composable(Screen.Map.route) {
                    MapScreen(
                        userProfile = userProfile,
                        savedRoutes = savedRoutes,
                        isTracking = isTracking,
                        isPaused = isPaused,
                        sessionSeconds = sessionSeconds,
                        sessionSteps = sessionSteps,
                        formatDistance = formatDistance,
                        onStartTrackingSession = onStartTrackingSession,
                        onPauseTrackingSession = onPauseTrackingSession,
                        onResumeTrackingSession = onResumeTrackingSession,
                        onStopTrackingSession = onStopTrackingSession,
                        onDeleteSavedRoute = onDeleteSavedRoute
                    )
                }

                composable(Screen.AiCoach.route) {
                    AiCoachScreen(
                        todayActivity = todayActivity,
                        todayMeals = todayMeals,
                        userProfile = userProfile,
                        allActivities = allActivities,
                        liveSteps = liveSteps,
                        activityState = activityState,
                        formatDistance = formatDistance,
                        calculateBmi = calculateBmi,
                        getBmiCategory = getBmiCategory,
                        onAddWater = onAddWater,
                        onNavigateToActivity = { navController.navigate(Screen.Activity.route) }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        todayActivity = todayActivity,
                        allActivities = allActivities,
                        completedActivities = completedActivities,
                        savedRoutes = savedRoutes,
                        userProfile = userProfile,
                        formatDistance = formatDistance,
                        fusionDiagnostics = fusionDiagnostics,
                        liveSteps = liveSteps,
                        onDeleteCompletedActivity = onDeleteCompletedActivity
                    )
                }

                composable(Screen.Goals.route) {
                    GoalsScreen(
                        userProfile = userProfile,
                        achievements = achievements,
                        onUpdateStepGoal = onUpdateStepGoal,
                        onUpdateWaterGoal = onUpdateWaterGoal
                    )
                }

                composable(Screen.Health.route) {
                    HealthScreen(
                        userProfile = userProfile,
                        todayActivity = todayActivity,
                        todayMeals = todayMeals,
                        liveSteps = liveSteps,
                        formatDistance = formatDistance,
                        calculateBmi = calculateBmi,
                        getBmiCategory = getBmiCategory,
                        calculateBmr = calculateBmr,
                        calculateDailyCaloriesNeeded = calculateDailyCaloriesNeeded,
                        onAddWater = onAddWater,
                        onAddMealLog = onAddMealLog,
                        onDeleteMealLog = onDeleteMealLog,
                        onClearMeals = onClearMeals,
                        onUpdateProfile = onUpdateProfile
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        userProfile = userProfile,
                        allActivities = allActivities,
                        formatHeight = formatHeight,
                        formatWeight = formatWeight,
                        onUpdateProfile = onUpdateProfile,
                        onUpdateStepGoal = onUpdateStepGoal,
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onSignOut = onSignOut
                    )
                }

                composable(Screen.CalorieCalculator.route) {
                    CalorieCalculatorScreen(
                        userProfile = userProfile,
                        todayActivity = todayActivity,
                        onUpdateProfile = onUpdateProfile,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        userProfile = userProfile,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onToggleUnitSystem = onToggleUnitSystem,
                        onToggleNotifications = onToggleNotifications,
                        onUpdateStrideLength = { newStride -> onUpdateProfile(userProfile.copy(strideLengthMeters = newStride)) },
                        onUpdateProfile = onUpdateProfile,
                        onUpdateStepGoal = onUpdateStepGoal,
                        onResetData = onResetData,
                        onSignOut = onSignOut,
                        fusionDiagnostics = fusionDiagnostics,
                        onResetSteps = onResetSteps
                    )
                }
            }
        }
}
