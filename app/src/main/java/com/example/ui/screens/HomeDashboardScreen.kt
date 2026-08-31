package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import com.example.ui.components.AnimatedCountUpText
import com.example.ui.components.AnimatedHealthScoreRing
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.FuturisticButton
import com.example.ui.components.GlassCard
import com.example.ui.components.MetricCard
import com.example.ui.components.ProgressRing
import com.example.ui.components.WeeklyStepHistoryCard
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun HomeDashboardScreen(
    todayActivity: DailyActivity?,
    userProfile: UserProfile,
    allActivities: List<DailyActivity> = emptyList(),
    liveSteps: Int = 0,
    activityState: String,
    sensorStatus: String,
    formatDistance: (Double, Boolean) -> String,
    onNavigateToActivity: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onAddWater: (Int) -> Unit
) {
    val currentSteps = if (liveSteps > 0 || (todayActivity?.steps ?: 0) == 0) liveSteps else (todayActivity?.steps ?: 0)
    val targetSteps = userProfile.dailyStepGoal.coerceAtLeast(1)
    val progress = (currentSteps.toFloat() / targetSteps.toFloat()).coerceIn(0f, 1f)
    val isImperial = userProfile.unitSystem == "Imperial"

    val strideMeters = if (userProfile.strideLengthMeters > 0) userProfile.strideLengthMeters else 0.75
    val distanceMeters = if (todayActivity?.distanceMeters != null && todayActivity.distanceMeters > 0.0) {
        todayActivity.distanceMeters
    } else {
        currentSteps * strideMeters
    }
    val weightKg = if (userProfile.weightKg > 0) userProfile.weightKg else 70.0
    val calories = if (todayActivity?.calories != null && todayActivity.calories > 0.0) {
        todayActivity.calories
    } else {
        (distanceMeters / 1000.0) * weightKg * 0.75
    }
    val activeMinutes = if (todayActivity?.activeMinutes != null && todayActivity.activeMinutes > 0) {
        todayActivity.activeMinutes
    } else {
        currentSteps / 100
    }
    val waterMl = todayActivity?.waterIntakeMl ?: 0

    // Calculated Health Score
    val healthScore = (progress * 50 + (activeMinutes.coerceAtMost(60) / 60f) * 30 + (waterMl.coerceAtMost(2000) / 2000f) * 20).toInt().coerceIn(15, 100)

    // Time of day greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    var showConfetti by remember(currentSteps >= targetSteps) { mutableStateOf(currentSteps >= targetSteps) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SoftBackground,
                        Color(0xFFEFF4FA),
                        SoftBackground
                    )
                )
            )
            .testTag("home_dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header User Greeting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "$greetingText, ${userProfile.name} ✨",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "MotionIQ Spatial AI Dashboard",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        ),
                        color = CyberPrimaryCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Live Activity Status Pill
                val (statusIcon, statusColor) = when (activityState) {
                    "Running" -> Pair(Icons.AutoMirrored.Filled.DirectionsRun, CyberPinkGlow)
                    "Walking" -> Pair(Icons.AutoMirrored.Filled.DirectionsWalk, CyberPrimaryCyan)
                    else -> Pair(Icons.Default.Accessibility, CyberAccentMint)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = activityState,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activityState.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Physical Activity Permission / Sensor Alert Notice
            if (sensorStatus.contains("permission", ignoreCase = true) || sensorStatus.contains("not available", ignoreCase = true)) {
                val isPermission = sensorStatus.contains("permission", ignoreCase = true)
                GlassCard(
                    glowColor = if (isPermission) CyberPinkGlow else CyberSecondaryViolet,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPermission) Icons.AutoMirrored.Filled.DirectionsWalk else Icons.Default.Info,
                            contentDescription = "Sensor Alert",
                            tint = if (isPermission) CyberPinkGlow else CyberSecondaryViolet,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPermission) "Physical Activity Permission Required" else "Step Sensor Notice",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPermission) CyberPinkGlow else CyberSecondaryViolet
                            )
                            Text(
                                text = sensorStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ==================== BENTO GRID ====================

            // 1. Hero Steps Progress Bento Card
            GlassCard(
                glowColor = CyberPrimaryCyan,
                shape = RoundedCornerShape(28.dp),
                testTag = "hero_step_card"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(
                        progress = progress,
                        currentSteps = currentSteps,
                        targetSteps = targetSteps,
                        size = 220.dp,
                        strokeWidth = 18.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Goal Achieved Celebration Banner if completed
            AnimatedVisibility(
                visible = currentSteps >= targetSteps,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassCard(
                    glowColor = CyberAccentMint,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Goal Smashed",
                            tint = CyberAccentMint,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Target Smashed! 🎉",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberAccentMint
                            )
                            Text(
                                text = "Peak athletic performance achieved today. Outstanding!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Bento Row: Health Index & Activity Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Health Score Card
                GlassCard(
                    glowColor = CyberAccentMint,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedHealthScoreRing(
                            score = healthScore,
                            size = 110.dp,
                            strokeWidth = 10.dp
                        )
                    }
                }

                // Active Mode & Sensor Tile
                GlassCard(
                    glowColor = CyberSecondaryViolet,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .clickable { onNavigateToActivity() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberSecondaryViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = "Active State",
                                tint = CyberSecondaryViolet,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sensorStatus,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CyberSecondaryViolet
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TAP TO TRACK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = CyberPrimaryCyan
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Bento Full Tile: Live GPS Map Launcher
            GlassCard(
                glowColor = CyberPrimaryCyan,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMap() },
                testTag = "bento_gps_map_tile"
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CyberPrimaryCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "GPS Map",
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "🗺️ Live GPS Route Tracker",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "High-precision navigation & live speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Open Map",
                        tint = CyberPrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Bento Grid 2x2 Metrics: Calories, Distance, Active Time, Water
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Calories",
                    value = "%.0f".format(calories),
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = CyberAccentOrange,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Distance",
                    value = formatDistance(distanceMeters, isImperial),
                    unit = "",
                    icon = Icons.Default.Navigation,
                    accentColor = CyberPrimaryCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Active Mins",
                    value = "$activeMinutes",
                    unit = "mins",
                    icon = Icons.Default.Timer,
                    accentColor = CyberSecondaryViolet,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Hydration",
                    value = "$waterMl",
                    unit = "ml",
                    icon = Icons.Default.WaterDrop,
                    accentColor = CyberAccentMint,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Bento Row: Sleep Recovery & Heart Rate Pulse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sleep / Recovery Card
                GlassCard(
                    glowColor = CyberSecondaryViolet,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberSecondaryViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = "Sleep Recovery",
                                tint = CyberSecondaryViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "RECOVERY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "88% Prime",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Heart Rate / Intensity Card
                GlassCard(
                    glowColor = CyberPinkGlow,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyberPinkGlow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart Pulse",
                                tint = CyberPinkGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "HEART PULSE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "72 bpm",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Weekly Step History Visualization Bento Card
            WeeklyStepHistoryCard(
                allActivities = allActivities,
                dailyStepGoal = userProfile.dailyStepGoal
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 7. MotionIQ AI Coach Bento Highlight Card
            GlassCard(
                glowColor = CyberSecondaryViolet,
                shape = RoundedCornerShape(28.dp),
                testTag = "ai_coach_card"
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach",
                            tint = CyberSecondaryViolet,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MOTIONIQ AI COACH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CyberSecondaryViolet
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val aiInsight = if (progress >= 1.0f) {
                        "Outstanding endurance! You've reached your daily step target. Time for light recovery hydration."
                    } else if (progress >= 0.5f) {
                        "Over halfway to your target! An afternoon 15-minute brisk walk will easily complete your daily goal."
                    } else {
                        "Let's boost your activity! 2,000 extra steps today will burn ~90 extra calories and elevate your wellness score."
                    }

                    Text(
                        text = aiInsight,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FuturisticButton(
                    text = "Live Track",
                    onClick = onNavigateToActivity,
                    icon = Icons.Default.PlayArrow,
                    primaryColor = CyberPrimaryCyan,
                    secondaryColor = CyberSecondaryViolet,
                    modifier = Modifier.weight(1f)
                )

                FuturisticButton(
                    text = "+250ml Water",
                    onClick = { onAddWater(250) },
                    icon = Icons.Default.WaterDrop,
                    primaryColor = CyberAccentMint,
                    secondaryColor = CyberPrimaryCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Celebration Confetti Canvas
        ConfettiOverlay(
            trigger = showConfetti,
            onFinished = { showConfetti = false }
        )
    }
}



