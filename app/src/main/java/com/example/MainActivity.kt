package com.example

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.NavGraph
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MotionIQTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.GoogleAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d("MotionIQ_Startup", "App started: MainActivity onCreate")

        try {
            val app = FirebaseApp.initializeApp(this)
            Log.d("MotionIQ_Startup", "FirebaseApp initialized: ${app?.name ?: "default"}")
        } catch (e: Exception) {
            Log.w("MotionIQ_Startup", "FirebaseApp initialization message: ${e.message}")
        }

        requestPedometerPermissions()

        setContent {
            val viewModel: MainViewModel = viewModel()
            var isSplashActive by remember { mutableStateOf(true) }

            val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
            val authLoading by viewModel.authLoading.collectAsStateWithLifecycle()
            val authError by viewModel.authError.collectAsStateWithLifecycle()
            val authSuccessMessage by viewModel.authSuccessMessage.collectAsStateWithLifecycle()

            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val todayActivity by viewModel.todayActivity.collectAsStateWithLifecycle()
            val todayMeals by viewModel.todayMeals.collectAsStateWithLifecycle()
            val allActivities by viewModel.allActivities.collectAsStateWithLifecycle()
            val completedActivities by viewModel.completedActivities.collectAsStateWithLifecycle()
            val savedRoutes by viewModel.savedRoutes.collectAsStateWithLifecycle()
            val achievements by viewModel.allAchievements.collectAsStateWithLifecycle()

            val liveSteps by viewModel.liveSteps.collectAsStateWithLifecycle()
            val activityState by viewModel.activityState.collectAsStateWithLifecycle()
            val sensorStatus by viewModel.sensorStatus.collectAsStateWithLifecycle()
            val isTracking by viewModel.isTracking.collectAsStateWithLifecycle()
            val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
            val sessionSeconds by viewModel.sessionSeconds.collectAsStateWithLifecycle()
            val sessionSteps by viewModel.sessionSteps.collectAsStateWithLifecycle()
            val sessionDistanceMeters by viewModel.sessionDistanceMeters.collectAsStateWithLifecycle()
            val sessionCalories by viewModel.sessionCalories.collectAsStateWithLifecycle()
            val liveIntensity by viewModel.liveIntensity.collectAsStateWithLifecycle()
            val fusionDiagnostics by viewModel.fusionDiagnostics.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDarkMode = when (userProfile.themeMode) {
                "Dark" -> true
                "Light" -> false
                else -> systemDark
            }

            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            val googleSignInLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                Log.d("MotionIQ_Auth", "GoogleSignIn ActivityResult received: resultCode=${result.resultCode}")
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                GoogleAuthManager.handleGoogleSignInIntentResult(
                    task = task,
                    onSuccess = { googleUser ->
                        Log.d("MotionIQ_Auth", "Google Sign-In returned user: ${googleUser.email} (${googleUser.displayName})")
                        viewModel.signInWithGoogleUser(googleUser)
                    },
                    onError = { errorMsg ->
                        Log.w("MotionIQ_Auth", "Google Sign-In error callback: $errorMsg")
                        viewModel.setAuthError(errorMsg)
                    },
                    onCancelled = {
                        Log.d("MotionIQ_Auth", "Google Sign-In dismissed without selection.")
                    }
                )
            }

            MotionIQTheme(darkTheme = isDarkMode) {
                if (isSplashActive) {
                    SplashScreen(
                        onNavigateToHome = {
                            Log.d("MotionIQ_Startup", "Splash Screen completed. Checking auth state: isSessionActive = $isSessionActive")
                            isSplashActive = false
                        }
                    )
                } else {
                    Crossfade(
                        targetState = isSessionActive,
                        label = "AuthSessionTransition"
                    ) { authenticated ->
                        if (!authenticated) {
                            Log.d("MotionIQ_Navigation", "Navigating to Login Screen (User not authenticated)")
                            LoginScreen(
                                isLoading = authLoading,
                                errorMessage = authError,
                                successMessage = authSuccessMessage,
                                onSignInClick = { email, pass ->
                                    Log.d("MotionIQ_Auth", "Sign In clicked for email: $email")
                                    viewModel.signInWithEmailAndPassword(email, pass)
                                },
                                onSignUpClick = { name, email, pass, confirmPass ->
                                    Log.d("MotionIQ_Auth", "Sign Up clicked for email: $email")
                                    viewModel.signUpWithEmailAndPassword(name, email, pass, confirmPass)
                                },
                                onForgotPasswordClick = { email ->
                                    Log.d("MotionIQ_Auth", "Forgot Password clicked for email: $email")
                                    viewModel.sendPasswordResetEmail(email)
                                },
                                onGoogleSignInClick = {
                                    coroutineScope.launch {
                                        viewModel.clearAuthMessages()
                                        GoogleAuthManager.signIn(
                                            context = context,
                                            onSuccess = { googleUser ->
                                                Log.d("MotionIQ_Auth", "Google Sign-In returned user: ${googleUser.email} (${googleUser.displayName})")
                                                viewModel.signInWithGoogleUser(googleUser)
                                            },
                                            onError = { errorMsg ->
                                                Log.w("MotionIQ_Auth", "Google Sign-In error callback: $errorMsg")
                                                viewModel.setAuthError(errorMsg)
                                            },
                                            onCancelled = {
                                                Log.d("MotionIQ_Auth", "Google Sign-In dismissed without selection.")
                                            },
                                            onLaunchIntentPicker = { intent ->
                                                Log.d("MotionIQ_Auth", "Launching native Google Sign-In intent chooser...")
                                                googleSignInLauncher.launch(intent)
                                            }
                                        )
                                    }
                                },
                                onGuestClick = {
                                    Log.d("MotionIQ_Auth", "Guest / Offline mode clicked")
                                    viewModel.continueAsGuest()
                                },
                                onAuthError = viewModel::setAuthError,
                                onClearMessages = viewModel::clearAuthMessages
                            )
                        } else {
                            Log.d("MotionIQ_Navigation", "Navigating to Home Dashboard (User authenticated)")
                            NavGraph(
                                todayActivity = todayActivity,
                                todayMeals = todayMeals,
                                userProfile = userProfile,
                                allActivities = allActivities,
                                completedActivities = completedActivities,
                                savedRoutes = savedRoutes,
                                achievements = achievements,
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
                                isDarkMode = isDarkMode,
                                formatDistance = viewModel::formatDistance,
                                formatHeight = viewModel::formatHeight,
                                formatWeight = viewModel::formatWeight,
                                calculateBmi = viewModel::calculateBmi,
                                getBmiCategory = viewModel::getBmiCategory,
                                calculateBmr = viewModel::calculateBmr,
                                calculateDailyCaloriesNeeded = viewModel::calculateDailyCaloriesNeeded,
                                onStartTracking = viewModel::startTracking,
                                onStopTracking = viewModel::stopTracking,
                                onStartTrackingSession = viewModel::startTrackingSession,
                                onPauseTrackingSession = viewModel::pauseTrackingSession,
                                onResumeTrackingSession = viewModel::resumeTrackingSession,
                                onStopTrackingSession = viewModel::stopTrackingSession,
                                onSetManualActivity = viewModel::setManualActivityOverride,
                                onSaveCompletedActivity = viewModel::saveCompletedActivity,
                                onDeleteCompletedActivity = viewModel::deleteCompletedActivity,
                                onDeleteSavedRoute = viewModel::deleteSavedRoute,
                                onResetSteps = viewModel::resetTodaySteps,
                                onAddWater = viewModel::addManualWater,
                                onAddMealLog = viewModel::addMealLog,
                                onDeleteMealLog = viewModel::deleteMealLog,
                                onClearMeals = viewModel::clearTodayMeals,
                                onUpdateProfile = viewModel::updateProfile,
                                onUpdateStepGoal = viewModel::setStepGoal,
                                onUpdateWaterGoal = viewModel::setWaterGoal,
                                onToggleDarkMode = viewModel::toggleDarkMode,
                                onToggleUnitSystem = viewModel::toggleUnitSystem,
                                onToggleNotifications = viewModel::toggleNotifications,
                                onResetData = viewModel::clearAllData,
                                onSignOut = { viewModel.signOut(context) },
                                fusionDiagnostics = fusionDiagnostics
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestPedometerPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.data.sensor.StepTrackingManager.getSensorManager(this).apply {
            checkPermission()
            checkDateRollover()
            startListening()
        }
    }
}
