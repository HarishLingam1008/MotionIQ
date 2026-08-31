package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.coach.CoachInputData
import com.example.data.coach.OfflineAiCoachEngine
import com.example.data.coach.PersonalizedWorkoutPlan
import com.example.data.local.CompletedActivity
import com.example.data.local.DailyActivity
import com.example.data.local.GoalAchievement
import com.example.data.local.MealLog
import com.example.data.local.MotionIQDatabase
import com.example.data.local.SavedRoute
import com.example.data.local.UserProfile
import com.example.data.repository.MotionRepository
import com.example.data.sensor.LocationTrackingManager
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.data.sensor.StepSensorManager
import com.example.data.sensor.StepTrackingManager
import com.example.util.BmiCalculator
import com.example.util.GoogleAuthManager
import com.example.util.GoogleAuthUser
import com.example.util.formatFriendlyAuthError
import com.example.util.hashPassword
import com.example.util.isApiKeyOrConfigError
import com.example.util.isValidEmailAddress
import com.example.util.isValidGmailAddress
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MotionIQDatabase.getInstance(application)
    private val repository = MotionRepository(
        dailyActivityDao = db.dailyActivityDao(),
        stepLogDao = db.stepLogDao(),
        userProfileDao = db.userProfileDao(),
        goalAchievementDao = db.goalAchievementDao(),
        mealLogDao = db.mealLogDao(),
        completedActivityDao = db.completedActivityDao(),
        savedRouteDao = db.savedRouteDao()
    )

    private val sharedPrefs = application.getSharedPreferences("motion_iq_session", Context.MODE_PRIVATE)

    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance().apply {
            try {
                firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            } catch (e: Exception) {
                Log.w("MotionIQ_Auth", "Could not set app verification setting: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.w("MotionIQ_Auth", "FirebaseAuth not initialized: ${e.message}")
        null
    }

    private val _authUser = MutableStateFlow<FirebaseUser?>(firebaseAuth?.currentUser)
    val authUser: StateFlow<FirebaseUser?> = _authUser.asStateFlow()

    private val _isSessionActive = MutableStateFlow(
        firebaseAuth?.currentUser != null || sharedPrefs.getBoolean("is_logged_in", false)
    )
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    val currentUserId: StateFlow<String> = _authUser
        .map { user ->
            user?.uid ?: sharedPrefs.getString("user_id", "local_user") ?: "local_user"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), firebaseAuth?.currentUser?.uid ?: sharedPrefs.getString("user_id", "local_user") ?: "local_user")

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    val stepSensorManager: StepSensorManager
        get() = StepTrackingManager.getSensorManager(getApplication())

    val userProfile: StateFlow<UserProfile> = currentUserId
        .flatMapLatest { uid -> repository.getUserProfile(uid) }
        .map { it ?: UserProfile(id = currentUserId.value, name = sharedPrefs.getString("user_name", "Athlete") ?: "Athlete") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile(id = currentUserId.value, name = sharedPrefs.getString("user_name", "Athlete") ?: "Athlete"))

    val todayActivity: StateFlow<DailyActivity?> = currentUserId
        .flatMapLatest { uid -> repository.getActivityForToday(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayMeals: StateFlow<List<MealLog>> = currentUserId
        .flatMapLatest { uid -> repository.getMealLogsForToday(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<DailyActivity>> = currentUserId
        .flatMapLatest { uid -> repository.getAllActivities(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedActivities: StateFlow<List<CompletedActivity>> = currentUserId
        .flatMapLatest { uid -> repository.getCompletedActivities(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRoutes: StateFlow<List<SavedRoute>> = currentUserId
        .flatMapLatest { uid -> repository.getSavedRoutes(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<GoalAchievement>> = currentUserId
        .flatMapLatest { uid -> repository.getAllAchievements(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveSteps: StateFlow<Int>
        get() = StepTrackingManager.getSensorManager(getApplication()).liveSteps
    val activityState: StateFlow<String>
        get() = StepTrackingManager.getSensorManager(getApplication()).activityState
    val sensorStatus: StateFlow<String>
        get() = StepTrackingManager.getSensorManager(getApplication()).sensorStatus
    val isTracking: StateFlow<Boolean> = StepTrackingManager.isTracking
    val isPaused: StateFlow<Boolean> = StepTrackingManager.isPaused
    val sessionSeconds: StateFlow<Long> = StepTrackingManager.sessionSeconds
    val sessionSteps: StateFlow<Int> = StepTrackingManager.sessionSteps
    val sessionDistanceMeters: StateFlow<Double> = StepTrackingManager.sessionDistanceMeters
    val sessionCalories: StateFlow<Double> = StepTrackingManager.sessionCalories
    val liveIntensity: StateFlow<Float>
        get() = StepTrackingManager.getSensorManager(getApplication()).liveIntensity
    val fusionDiagnostics: StateFlow<SensorFusionDiagnostics>
        get() = StepTrackingManager.getSensorManager(getApplication()).fusionDiagnostics

    private val _unlockedAchievementDialog = MutableStateFlow<GoalAchievement?>(null)
    val unlockedAchievementDialog: StateFlow<GoalAchievement?> = _unlockedAchievementDialog.asStateFlow()

    private val _workoutTipsPlan = MutableStateFlow<PersonalizedWorkoutPlan?>(null)
    val workoutTipsPlan: StateFlow<PersonalizedWorkoutPlan?> = _workoutTipsPlan.asStateFlow()

    private val _isGeneratingWorkoutTips = MutableStateFlow(false)
    val isGeneratingWorkoutTips: StateFlow<Boolean> = _isGeneratingWorkoutTips.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        _authUser.value = user
        if (user != null) {
            saveSession(user.uid, user.email ?: "", user.displayName ?: "Athlete")
        }
    }

    init {
        StepTrackingManager.init(getApplication())

        firebaseAuth?.addAuthStateListener(authStateListener)

        viewModelScope.launch {
            currentUserId.collectLatest { uid ->
                val name = sharedPrefs.getString("user_name", "MotionIQ Athlete") ?: "MotionIQ Athlete"
                repository.initializeUserIfNotExists(uid, name)
                val todayAct = repository.ensureTodayActivityExists(uid)
                stepSensorManager.setInitialStepCount(todayAct.steps)
            }
        }

        // Synchronize live sensor step changes to Room DB
        viewModelScope.launch {
            liveSteps.collectLatest { count ->
                if (count >= 0) {
                    repository.updateStepsForToday(currentUserId.value, count, activityState.value)
                }
            }
        }

        stepSensorManager.startListening()
    }

    fun isValidEmailFormat(email: String): Boolean {
        return isValidGmailAddress(email)
    }

    private fun saveSession(uid: String, email: String, name: String) {
        sharedPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_id", uid)
            .putString("user_email", email)
            .putString("user_name", name)
            .apply()
        _isSessionActive.value = true
    }

    private fun clearSession() {
        sharedPrefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_id")
            .remove("user_email")
            .remove("user_name")
            .apply()
        _isSessionActive.value = false
        _authUser.value = null
    }

    fun signInWithEmailAndPassword(email: String, pass: String) {
        clearAuthMessages()
        val trimmedEmail = email.trim().lowercase()
        val trimmedPass = pass.trim()

        if (!isValidGmailAddress(trimmedEmail)) {
            _authError.value = "Please enter a valid email address."
            return
        }
        if (trimmedPass.isEmpty()) {
            _authError.value = "Please enter your password."
            return
        }
        if (trimmedPass.length < 8) {
            _authError.value = "Password must contain at least 8 characters."
            return
        }

        _authLoading.value = true
        Log.d("MotionIQ_Auth", "EMAIL LOGIN DEBUG - email: [REDACTED], length: ${trimmedEmail.length}")

        if (firebaseAuth != null) {
            Log.d("MotionIQ_Auth", "EMAIL LOGIN DEBUG - Initiating signInWithEmailAndPassword on FirebaseAuth (App: ${firebaseAuth.app.name}, ProjectId: motioniq-2520f)")
            firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        _authUser.value = user
                        val uid = user?.uid ?: ""
                        val name = user?.displayName ?: trimmedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        saveSession(uid, trimmedEmail, name)
                        updateProfile(userProfile.value.copy(id = uid, name = name))
                        Log.d("MotionIQ_Auth", "EMAIL LOGIN DEBUG - Sign-in SUCCESS. UID: $uid, Providers: ${user?.providerData?.map { it.providerId }}")
                    } else {
                        val ex = task.exception
                        val errCode = (ex as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
                        val errMsg = ex?.message ?: "No error message"
                        Log.e("MotionIQ_Auth", "EMAIL LOGIN DEBUG - errorCode: $errCode, errorMessage: $errMsg, exceptionClass: ${ex?.javaClass?.name}")

                        // STEP 3: Check whether this account might be registered with Google provider instead
                        firebaseAuth.fetchSignInMethodsForEmail(trimmedEmail)
                            .addOnCompleteListener { fetchTask ->
                                if (fetchTask.isSuccessful) {
                                    val methods = fetchTask.result?.signInMethods ?: emptyList()
                                    Log.d("MotionIQ_Auth", "EMAIL LOGIN DEBUG - Registered sign-in methods for email: $methods")
                                    if (methods.contains(GoogleAuthProvider.PROVIDER_ID) && !methods.contains(EmailAuthProvider.PROVIDER_ID)) {
                                        _authError.value = "This account uses Google Sign-In. Please use Continue with Google."
                                        return@addOnCompleteListener
                                    }
                                }
                                _authError.value = formatFriendlyAuthError(ex)
                            }
                    }
                }
        } else {
            _authLoading.value = false
            Log.e("MotionIQ_Auth", "EMAIL LOGIN DEBUG - FirebaseAuth instance is null!")
            _authError.value = "Authentication service is unavailable. Please check your configuration."
        }
    }

    fun signUpWithEmailAndPassword(fullName: String, email: String, pass: String, confirmPass: String) {
        clearAuthMessages()
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim().lowercase()

        if (trimmedName.isEmpty()) {
            _authError.value = "Please enter your full name."
            return
        }
        if (!isValidGmailAddress(trimmedEmail)) {
            _authError.value = "Please enter a valid email address."
            return
        }
        if (pass.length < 8) {
            _authError.value = "Password must contain at least 8 characters."
            return
        }
        if (pass != confirmPass) {
            _authError.value = "Passwords do not match."
            return
        }

        _authLoading.value = true
        Log.d("MotionIQ_Auth", "EMAIL SIGNUP DEBUG - email: [REDACTED], name: $trimmedName")

        if (firebaseAuth != null) {
            Log.d("MotionIQ_Auth", "EMAIL SIGNUP DEBUG - Initiating createUserWithEmailAndPassword on FirebaseAuth")
            firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, pass)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        val uid = user?.uid ?: ""
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            _authUser.value = firebaseAuth.currentUser
                        }

                        saveSession(uid, trimmedEmail, trimmedName)
                        updateProfile(userProfile.value.copy(id = uid, name = trimmedName))
                        _authUser.value = user
                        Log.d("MotionIQ_Auth", "EMAIL SIGNUP DEBUG - Sign-up SUCCESS. UID: $uid")
                    } else {
                        val ex = task.exception
                        val errCode = (ex as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
                        val errMsg = ex?.message ?: "No error message"
                        Log.e("MotionIQ_Auth", "EMAIL SIGNUP DEBUG - errorCode: $errCode, errorMessage: $errMsg, exceptionClass: ${ex?.javaClass?.name}")
                        _authError.value = formatFriendlyAuthError(ex)
                    }
                }
        } else {
            _authLoading.value = false
            Log.e("MotionIQ_Auth", "EMAIL SIGNUP DEBUG - FirebaseAuth instance is null!")
            _authError.value = "Authentication service is unavailable. Please check your configuration."
        }
    }

    fun sendPasswordResetEmail(email: String) {
        clearAuthMessages()
        val trimmedEmail = email.trim().lowercase()
        if (!isValidEmailAddress(trimmedEmail)) {
            _authError.value = "Please enter a valid email address."
            return
        }

        if (firebaseAuth != null) {
            _authLoading.value = true
            Log.d("MotionIQ_Auth", "PASSWORD RESET DEBUG - email: [REDACTED]")
            firebaseAuth.sendPasswordResetEmail(trimmedEmail)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        _authSuccessMessage.value = "Password reset link sent to $trimmedEmail."
                    } else {
                        val ex = task.exception
                        val errCode = (ex as? FirebaseAuthException)?.errorCode ?: "UNKNOWN"
                        val errMsg = ex?.message ?: "No error message"
                        Log.e("MotionIQ_Auth", "PASSWORD RESET DEBUG - errorCode: $errCode, errorMessage: $errMsg, exceptionClass: ${ex?.javaClass?.name}")
                        _authError.value = formatFriendlyAuthError(ex)
                    }
                }
        } else {
            _authError.value = "Authentication service is unavailable. Please check your configuration."
        }
    }

    fun continueAsGuest() {
        clearAuthMessages()
        val uid = "guest"
        val name = "MotionIQ Athlete"
        val email = "guest@motioniq.local"
        saveSession(uid, email, name)
        updateProfile(userProfile.value.copy(id = uid, name = name))
    }

    fun signInWithGoogleUser(user: GoogleAuthUser) {
        clearAuthMessages()
        _authLoading.value = true
        Log.d("MotionIQ_Auth", "Processing Google Sign-In for email: ${user.email}, name: ${user.displayName}")

        if (firebaseAuth != null) {
            val credential = GoogleAuthProvider.getCredential(user.idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    _authLoading.value = false
                    if (task.isSuccessful) {
                        val fbUser = firebaseAuth.currentUser
                        _authUser.value = fbUser
                        val uid = fbUser?.uid ?: user.userId
                        val googleName = fbUser?.displayName ?: user.displayName
                        val email = fbUser?.email ?: user.email

                        Log.d("MotionIQ_Auth", "Firebase Auth successful with Google credential. UID: $uid")
                        saveSession(uid, email, googleName)
                        updateProfile(userProfile.value.copy(id = uid, name = googleName))
                        _authSuccessMessage.value = "Welcome back, $googleName!"
                    } else {
                        val ex = task.exception
                        Log.w("MotionIQ_Auth", "Firebase signInWithCredential returned error: ${ex?.message}")
                        if (isApiKeyOrConfigError(ex) || ex?.message?.contains("configuration", ignoreCase = true) == true) {
                            Log.i("MotionIQ_Auth", "Authenticating via Google Credential fallback mode.")
                            val uid = user.userId.ifEmpty { "google_" + kotlin.math.abs(user.email.hashCode()) }
                            saveSession(uid, user.email, user.displayName)
                            updateProfile(userProfile.value.copy(id = uid, name = user.displayName))
                            _authSuccessMessage.value = "Welcome, ${user.displayName}!"
                        } else {
                            _authError.value = formatFriendlyAuthError(ex)
                        }
                    }
                }
        } else {
            _authLoading.value = false
            Log.i("MotionIQ_Auth", "FirebaseAuth not configured. Authenticating via Google Credential directly.")
            val uid = user.userId.ifEmpty { "google_" + kotlin.math.abs(user.email.hashCode()) }
            saveSession(uid, user.email, user.displayName)
            updateProfile(userProfile.value.copy(id = uid, name = user.displayName))
            _authSuccessMessage.value = "Welcome, ${user.displayName}!"
        }
    }

    fun signInWithGoogleToken(idToken: String, email: String? = null, name: String? = null) {
        val resolvedEmail = email ?: ""
        val resolvedName = name ?: if (resolvedEmail.contains("@")) resolvedEmail.substringBefore("@").replaceFirstChar { it.uppercase() } else "Athlete"
        val stableUid = if (resolvedEmail.isNotEmpty()) "google_" + kotlin.math.abs(resolvedEmail.hashCode()) else "google_" + kotlin.math.abs(idToken.take(16).hashCode())

        signInWithGoogleUser(
            GoogleAuthUser(
                idToken = idToken,
                userId = stableUid,
                email = resolvedEmail,
                displayName = resolvedName
            )
        )
    }

    fun setAuthError(message: String) {
        _authLoading.value = false
        _authError.value = message
    }

    fun signOut(context: Context? = null) {
        Log.d("MotionIQ_Auth", "Signing out of MotionIQ session...")
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w("MotionIQ_Auth", "Firebase signOut error: ${e.message}")
        }
        if (context != null) {
            viewModelScope.launch {
                GoogleAuthManager.signOut(context)
            }
        }
        clearSession()
        clearAuthMessages()
        Log.d("MotionIQ_Auth", "User session successfully cleared.")
    }

    fun clearAuthMessages() {
        _authError.value = null
        _authSuccessMessage.value = null
    }

    fun startTrackingSession(context: Context) {
        val profile = userProfile.value
        StepTrackingManager.userWeightKg = if (profile.weightKg > 0) profile.weightKg else 70.0
        StepTrackingManager.userStrideMeters = if (profile.strideLengthMeters > 0) profile.strideLengthMeters else 0.75
        StepTrackingManager.startTrackingSession(context)
    }

    fun pauseTrackingSession(context: Context) {
        StepTrackingManager.pauseTrackingSession(context)
    }

    fun resumeTrackingSession(context: Context) {
        StepTrackingManager.resumeTrackingSession(context)
    }

    fun stopTrackingSession(context: Context) {
        val steps = StepTrackingManager.sessionSteps.value
        val distance = StepTrackingManager.sessionDistanceMeters.value
        val duration = StepTrackingManager.sessionSeconds.value
        val calories = StepTrackingManager.sessionCalories.value
        val activityType = StepTrackingManager.getSensorManager(context).activityState.value

        StepTrackingManager.stopTrackingSession(context)

        if (steps > 0 || duration > 5) {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy, h:mm a", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())
            saveCompletedActivity(
                CompletedActivity(
                    userId = currentUserId.value,
                    activityType = activityType,
                    date = dateStr,
                    durationSeconds = duration,
                    steps = steps,
                    distanceMeters = distance,
                    calories = calories,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun startTracking() {
        StepTrackingManager.getSensorManager(getApplication()).startListening()
    }

    fun stopTracking() {
        StepTrackingManager.getSensorManager(getApplication()).stopListening()
    }

    fun setManualActivityOverride(type: String?) {
        StepTrackingManager.getSensorManager(getApplication()).setManualActivityOverride(type)
    }

    fun saveCompletedActivity(activity: CompletedActivity) {
        viewModelScope.launch {
            repository.saveCompletedActivity(activity)
            checkGoalAchievements()
        }
    }

    fun deleteCompletedActivity(activity: CompletedActivity) {
        viewModelScope.launch {
            repository.deleteCompletedActivity(activity)
        }
    }

    fun deleteSavedRoute(route: SavedRoute) {
        viewModelScope.launch {
            repository.deleteRoute(route)
        }
    }

    fun resetTodaySteps() {
        viewModelScope.launch {
            stepSensorManager.resetSteps()
            repository.resetTodayData(currentUserId.value)
        }
    }

    fun addManualWater(amountMl: Int) {
        viewModelScope.launch {
            repository.updateWaterIntake(currentUserId.value, amountMl)
            checkGoalAchievements()
        }
    }

    fun addMealLog(meal: MealLog) {
        viewModelScope.launch {
            repository.addMealLog(currentUserId.value, meal)
        }
    }

    fun deleteMealLog(meal: MealLog) {
        viewModelScope.launch {
            repository.deleteMealLog(meal)
        }
    }

    fun clearTodayMeals() {
        viewModelScope.launch {
            repository.clearTodayMeals(currentUserId.value)
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveUserProfile(profile.copy(id = currentUserId.value))
        }
    }

    fun setStepGoal(goal: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(dailyStepGoal = goal))
        }
    }

    fun setWaterGoal(goal: Int) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(dailyWaterGoalMl = goal))
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            val newMode = if (enabled) "Dark" else "Light"
            repository.saveUserProfile(current.copy(themeMode = newMode))
        }
    }

    fun toggleUnitSystem(isImperial: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            val newUnit = if (isImperial) "Imperial" else "Metric"
            repository.saveUserProfile(current.copy(unitSystem = newUnit))
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value
            repository.saveUserProfile(current.copy(notificationsEnabled = enabled))
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllDataForUser(currentUserId.value)
            stepSensorManager.setInitialStepCount(0)
        }
    }

    private fun checkGoalAchievements() {
        viewModelScope.launch {
            val todayAct = todayActivity.value ?: return@launch
            val profile = userProfile.value

            if (todayAct.steps >= profile.dailyStepGoal) {
                repository.unlockAchievement(
                    currentUserId.value,
                    "STEP_GOAL_${todayAct.date}",
                    "Step Goal Crusher",
                    "Hit daily goal of ${profile.dailyStepGoal} steps!",
                    "🏆"
                )
            }

            if (todayAct.waterIntakeMl >= profile.dailyWaterGoalMl) {
                repository.unlockAchievement(
                    currentUserId.value,
                    "WATER_GOAL_${todayAct.date}",
                    "Hydration Master",
                    "Drank ${profile.dailyWaterGoalMl}ml of water!",
                    "💧"
                )
            }
        }
    }

    fun dismissAchievementDialog() {
        _unlockedAchievementDialog.value = null
    }

    fun formatDistance(meters: Double, isImperial: Boolean): String {
        return if (isImperial) {
            val miles = meters / 1609.34
            "%.2f mi".format(miles)
        } else {
            val km = meters / 1000.0
            "%.2f km".format(km)
        }
    }

    fun formatHeight(cm: Double, isImperial: Boolean): String {
        return if (isImperial) {
            val totalInches = (cm / 2.54).roundToInt()
            val feet = totalInches / 12
            val inches = totalInches % 12
            "$feet' $inches\""
        } else {
            "%.0f cm".format(cm)
        }
    }

    fun formatWeight(kg: Double, isImperial: Boolean): String {
        return if (isImperial) {
            val lbs = kg * 2.20462
            "%.1f lbs".format(lbs)
        } else {
            "%.1f kg".format(kg)
        }
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        return BmiCalculator.calculateBMI(weightKg = weightKg, heightCm = heightCm)
    }

    fun getBmiCategory(bmi: Double): Pair<String, String> {
        return BmiCalculator.getBmiCategory(bmi)
    }

    fun calculateBmr(weightKg: Double, heightCm: Double, age: Int, gender: String): Int {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 1600
        val isMale = gender.equals("male", ignoreCase = true)
        val bmr = if (isMale) {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        } else {
            (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
        }
        return bmr.roundToInt().coerceAtLeast(1000)
    }

    fun calculateDailyCaloriesNeeded(weightKg: Double, heightCm: Double, age: Int, gender: String, goal: String): Int {
        val bmr = calculateBmr(weightKg, heightCm, age, gender)
        val maintenance = (bmr * 1.375).roundToInt()
        return when {
            goal.contains("loss", ignoreCase = true) -> (maintenance - 400).coerceAtLeast(1200)
            goal.contains("gain", ignoreCase = true) -> maintenance + 400
            else -> maintenance
        }
    }

    fun refreshWorkoutTips(forceOnline: Boolean = true) {
        viewModelScope.launch {
            _isGeneratingWorkoutTips.value = true
            try {
                val profile = userProfile.value
                val activity = todayActivity.value
                val currentSteps = liveSteps.value.coerceAtLeast(activity?.steps ?: 0)
                val currentGoal = if (profile.dailyStepGoal > 0) profile.dailyStepGoal else 8000
                val distMeters = activity?.distanceMeters ?: (currentSteps * profile.strideLengthMeters)
                val cals = activity?.calories ?: (currentSteps * 0.04)
                val mins = activity?.activeMinutes ?: (currentSteps / 100)
                val currentAct = activityState.value.ifBlank { "Walking" }
                val bmi = calculateBmi(profile.weightKg, profile.heightCm)
                val (bmiCat, _) = getBmiCategory(bmi)

                val input = CoachInputData(
                    name = profile.name,
                    steps = currentSteps,
                    stepGoal = currentGoal,
                    distanceMeters = distMeters,
                    caloriesBurned = cals,
                    activeMinutes = mins,
                    waterIntakeMl = activity?.waterIntakeMl ?: 0,
                    waterGoalMl = profile.dailyWaterGoalMl,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    bmi = bmi,
                    bmiCategory = bmiCat,
                    currentActivity = currentAct,
                    activityLevel = profile.activityLevel,
                    age = profile.age,
                    gender = profile.gender
                )

                _workoutTipsPlan.value = OfflineAiCoachEngine.generateWorkoutTips(input, profile)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error generating workout tips: ${e.message}", e)
                val profile = userProfile.value
                val currentSteps = liveSteps.value
                val safeGoal = if (profile.dailyStepGoal > 0) profile.dailyStepGoal else 8000
                val input = CoachInputData(
                    name = profile.name,
                    steps = currentSteps,
                    stepGoal = safeGoal,
                    distanceMeters = currentSteps * 0.75,
                    caloriesBurned = currentSteps * 0.04,
                    activeMinutes = currentSteps / 100,
                    waterIntakeMl = 0,
                    waterGoalMl = profile.dailyWaterGoalMl,
                    heightCm = profile.heightCm,
                    weightKg = profile.weightKg,
                    bmi = 22.0,
                    bmiCategory = "Normal",
                    currentActivity = "Walking",
                    activityLevel = profile.activityLevel,
                    age = profile.age,
                    gender = profile.gender
                )
                _workoutTipsPlan.value = OfflineAiCoachEngine.generateWorkoutTips(input, profile)
            } finally {
                _isGeneratingWorkoutTips.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth?.removeAuthStateListener(authStateListener)
    }
}
