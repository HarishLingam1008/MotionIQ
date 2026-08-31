package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CompletedActivity
import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import com.example.ui.components.AiWorkoutTipsCard
import com.example.ui.components.AnimatedCountUpText
import com.example.ui.components.FuturisticButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import com.example.ui.theme.CyberWarning
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Context
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.platform.LocalContext
import com.example.data.sensor.StepTrackingManager
import com.example.util.BatteryOptimizationUtils

@Composable
fun ActivityScreen(
    liveSteps: Int,
    activityState: String, // "Walking", "Running", "Idle", "Cycling", "Jogging"
    sensorStatus: String,
    isTracking: Boolean,
    isPaused: Boolean = false,
    sessionSeconds: Long = 0L,
    sessionSteps: Int = 0,
    sessionDistanceMeters: Double = 0.0,
    sessionCalories: Double = 0.0,
    liveIntensity: Float,
    userProfile: UserProfile = UserProfile(),
    todayActivity: DailyActivity? = null,
    completedActivities: List<CompletedActivity> = emptyList(),
    formatDistance: (Double, Boolean) -> String = { m, imp -> if (imp) "%.2f mi".format(m / 1609.34) else "%.2f km".format(m / 1000.0) },
    onStartTracking: () -> Unit = {},
    onStopTracking: () -> Unit = {},
    onStartTrackingSession: (Context) -> Unit = {},
    onPauseTrackingSession: (Context) -> Unit = {},
    onResumeTrackingSession: (Context) -> Unit = {},
    onStopTrackingSession: (Context) -> Unit = {},
    onSetManualActivity: (String?) -> Unit = {},
    onSaveCompletedActivity: (CompletedActivity) -> Unit = {},
    onDeleteCompletedActivity: (CompletedActivity) -> Unit = {},
    onResetSteps: () -> Unit
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    val isImperial = userProfile.unitSystem == "Imperial"

    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context))
    }

    val stride = if (userProfile.strideLengthMeters > 0) userProfile.strideLengthMeters else 0.75
    val liveDistanceMeters = if (isTracking) sessionDistanceMeters else (liveSteps * stride)
    val liveCalories = if (isTracking) sessionCalories else ((liveDistanceMeters / 1000.0) * userProfile.weightKg * 0.75)

    val currentType = if (activityState.isBlank() || activityState == "Idle") "Walking" else activityState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("activity_screen")
    ) {
        Text(
            text = "Live Motion Studio ⚡",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Real-time pedometer & background foreground service tracking",
            style = MaterialTheme.typography.bodySmall,
            color = CyberPrimaryCyan
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Optimization Warning Banner
        if (!isBatteryOptimizationIgnored) {
            GlassCard(
                glowColor = CyberWarning,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = "Battery Optimization",
                        tint = CyberWarning,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery Optimization Active",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "To improve tracking accuracy, please allow MotionIQ to ignore battery optimization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FuturisticButton(
                        text = "Allow",
                        onClick = {
                            BatteryOptimizationUtils.requestIgnoreBatteryOptimization(context)
                            isBatteryOptimizationIgnored = BatteryOptimizationUtils.isIgnoringBatteryOptimizations(context)
                        },
                        primaryColor = CyberWarning,
                        secondaryColor = CyberAccentMint,
                        modifier = Modifier.heightIn(min = 36.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sensor Status Badge
        GlassCard(
            glowColor = CyberAccentMint,
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CyberAccentMint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Sensors",
                        tint = CyberAccentMint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SENSOR HARDWARE STATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isTracking) {
                            if (isPaused) "$sensorStatus (Service Paused)" else "$sensorStatus (Foreground Service Active)"
                        } else sensorStatus,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isTracking) (if (isPaused) CyberWarning else CyberAccentMint) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Large Live Step Counter Card
        GlassCard(
            glowColor = CyberPrimaryCyan,
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (stateIcon, stateColor) = when (activityState) {
                    "Cycling" -> Pair(Icons.Default.DirectionsBike, CyberSecondaryViolet)
                    "Running" -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, CyberPinkGlow)
                    "Walking" -> Pair(Icons.AutoMirrored.Filled.DirectionsWalk, CyberPrimaryCyan)
                    "Jogging" -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, CyberWarning)
                    else -> Pair(Icons.Default.Accessibility, MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = stateIcon,
                        contentDescription = activityState,
                        tint = stateColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isTracking) (if (isPaused) "SESSION PAUSED" else "ACTIVE SESSION TRACKING") else "CURRENT ACTIVITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = if (isPaused) CyberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = activityState.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = stateColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isTracking) {
                    Text(
                        text = StepTrackingManager.formatDuration(sessionSeconds),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberAccentMint,
                            fontSize = 32.sp
                        )
                    )
                    Text(
                        text = "Duration (HH:MM:SS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (activityState == "Cycling") {
                    Text(
                        text = formatDistance(liveDistanceMeters, isImperial),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp
                        ),
                        color = CyberSecondaryViolet
                    )

                    Text(
                        text = if (isTracking) "Session Distance Tracked" else "Distance Tracked Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AnimatedCountUpText(
                        targetValue = if (isTracking) sessionSteps else liveSteps,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 50.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isTracking) "Session Steps Tracked" else "Steps Tracked Today (Total)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val activeMins = if (isTracking) (sessionSeconds / 60).toInt() else (todayActivity?.activeMinutes ?: (liveSteps / 100))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDistance(liveDistanceMeters, isImperial),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberPrimaryCyan
                        )
                        Text(
                            text = "Distance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "%.0f kcal".format(liveCalories),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberWarning
                        )
                        Text(
                            text = "Est. Calories",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$activeMins min",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberSecondaryViolet
                        )
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Real-time Waveform Canvas
                val waveColor = stateColor
                val surfaceVar = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(surfaceVar)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        val points = 30
                        val stepX = w / (points - 1)

                        for (i in 0 until points - 1) {
                            val sinVal1 = kotlin.math.sin(i * 0.8).toFloat()
                            val sinVal2 = kotlin.math.sin((i + 1) * 0.8).toFloat()
                            val scaleFactor = liveIntensity * 3.5f + 4f

                            val amp1 = if (isTracking && !isPaused && activityState != "Idle") {
                                (sinVal1 * scaleFactor).coerceIn(-h / 2f + 5f, h / 2f - 5f)
                            } else 0f

                            val amp2 = if (isTracking && !isPaused && activityState != "Idle") {
                                (sinVal2 * scaleFactor).coerceIn(-h / 2f + 5f, h / 2f - 5f)
                            } else 0f

                            drawLine(
                                color = waveColor,
                                start = Offset(i * stepX, midY + amp1),
                                end = Offset((i + 1) * stepX, midY + amp2),
                                strokeWidth = 2.5.dp.toPx()
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isTracking) {
                FuturisticButton(
                    text = "Start Live",
                    onClick = {
                        onStartTrackingSession(context)
                    },
                    icon = Icons.Default.PlayArrow,
                    primaryColor = CyberAccentMint,
                    secondaryColor = CyberPrimaryCyan,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                )
            } else {
                if (isPaused) {
                    FuturisticButton(
                        text = "Resume",
                        onClick = {
                            onResumeTrackingSession(context)
                        },
                        icon = Icons.Default.PlayArrow,
                        primaryColor = CyberAccentMint,
                        secondaryColor = CyberPrimaryCyan,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp)
                    )
                } else {
                    FuturisticButton(
                        text = "Pause",
                        onClick = {
                            onPauseTrackingSession(context)
                        },
                        icon = Icons.Default.Pause,
                        primaryColor = CyberWarning,
                        secondaryColor = CyberDanger,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 54.dp)
                    )
                }

                FuturisticButton(
                    text = "Stop",
                    onClick = {
                        onStopTrackingSession(context)
                        onSetManualActivity(null)
                    },
                    icon = Icons.Default.Stop,
                    primaryColor = CyberDanger,
                    secondaryColor = CyberPinkGlow,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp)
                )
            }

            FuturisticButton(
                text = "Reset",
                onClick = { showResetDialog = true },
                icon = Icons.Default.Refresh,
                primaryColor = CyberDanger,
                secondaryColor = CyberPinkGlow,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI Personalized Daily Workout Tips
        AiWorkoutTipsCard(
            liveSteps = liveSteps,
            activityState = activityState,
            userProfile = userProfile,
            todayActivity = todayActivity,
            onStartWorkoutMode = { mode ->
                onSetManualActivity(mode)
                if (!isTracking) onStartTrackingSession(context)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Workout Selection
        Text(
            text = "SELECT WORKOUT MODE",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        val currentSelected = if (activityState.isBlank() || activityState == "Idle") "Walking" else activityState
        val isWalkingActive = currentSelected == "Walking"
        val isRunningActive = currentSelected == "Running"
        val isJoggingActive = currentSelected == "Jogging"
        val isCyclingActive = currentSelected == "Cycling"

        val activeMins = todayActivity?.activeMinutes ?: (liveSteps / 100)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityCard(
                    name = "Walking",
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    description = "Auto Pedometer",
                    accentColor = CyberPrimaryCyan,
                    isActive = isWalkingActive,
                    durationText = "$activeMins min",
                    distanceText = formatDistance(liveDistanceMeters, isImperial),
                    caloriesText = "%.0f kcal".format(liveCalories),
                    stepsText = "%,d".format(liveSteps),
                    speedText = null,
                    onStartClick = {
                        onSetManualActivity("Walking")
                        if (!isTracking) onStartTrackingSession(context)
                    },
                    modifier = Modifier.weight(1f)
                )

                ActivityCard(
                    name = "Running",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    description = "Pace Sensor",
                    accentColor = CyberPinkGlow,
                    isActive = isRunningActive,
                    durationText = "$activeMins min",
                    distanceText = formatDistance(liveDistanceMeters, isImperial),
                    caloriesText = "%.0f kcal".format(liveCalories),
                    stepsText = "%,d".format(liveSteps),
                    speedText = null,
                    onStartClick = {
                        onSetManualActivity("Running")
                        if (!isTracking) onStartTrackingSession(context)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActivityCard(
                    name = "Jogging",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    description = "Cardio Rhythm",
                    accentColor = CyberWarning,
                    isActive = isJoggingActive,
                    durationText = "$activeMins min",
                    distanceText = formatDistance(liveDistanceMeters, isImperial),
                    caloriesText = "%.0f kcal".format(liveCalories),
                    stepsText = "%,d".format(liveSteps),
                    speedText = null,
                    onStartClick = {
                        onSetManualActivity("Jogging")
                        if (!isTracking) onStartTrackingSession(context)
                    },
                    modifier = Modifier.weight(1f)
                )

                ActivityCard(
                    name = "Cycling",
                    icon = Icons.Default.DirectionsBike,
                    description = "Cadence Mode",
                    accentColor = CyberSecondaryViolet,
                    isActive = isCyclingActive,
                    durationText = "$activeMins min",
                    distanceText = formatDistance(liveDistanceMeters, isImperial),
                    caloriesText = "%.0f kcal".format(liveCalories),
                    stepsText = null,
                    speedText = null,
                    onStartClick = {
                        onSetManualActivity("Cycling")
                        if (!isTracking) onStartTrackingSession(context)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activity History Section
        ActivityHistoryList(
            completedActivities = completedActivities,
            isImperial = isImperial,
            formatDistance = formatDistance,
            onDeleteClick = onDeleteCompletedActivity
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Today's Steps?") },
            text = { Text("This will reset your tracked step count for today back to 0. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetSteps()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = CyberDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ActivityCard(
    name: String,
    icon: ImageVector,
    description: String,
    accentColor: Color,
    isActive: Boolean,
    durationText: String,
    distanceText: String,
    caloriesText: String,
    stepsText: String? = null,
    speedText: String? = null,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        glowColor = accentColor,
        modifier = modifier
            .testTag("activity_card_${name.lowercase()}")
            .clickable { onStartClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = name,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Activity Stats List
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = durationText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                }
                if (speedText != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = speedText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyberSecondaryViolet)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Distance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = distanceText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyberPrimaryCyan)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = caloriesText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyberWarning)
                }
                if (stepsText != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = stepsText, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = CyberAccentMint)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FuturisticButton(
                text = if (isActive) "Pause" else "Start",
                onClick = onStartClick,
                icon = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                primaryColor = if (isActive) accentColor else CyberPrimaryCyan,
                secondaryColor = if (isActive) CyberDanger else CyberSecondaryViolet,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

