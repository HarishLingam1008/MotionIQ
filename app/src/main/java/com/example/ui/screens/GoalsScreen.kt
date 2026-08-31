package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.GoalAchievement
import com.example.data.local.UserProfile
import com.example.ui.components.FuturisticButton
import com.example.ui.components.StepGoalDialog
import com.example.ui.theme.MotionOrange
import com.example.ui.theme.MotionPrimaryBlue
import com.example.ui.theme.MotionPrimaryGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoalsScreen(
    userProfile: UserProfile,
    achievements: List<GoalAchievement>,
    onUpdateStepGoal: (Int) -> Unit,
    onUpdateWaterGoal: (Int) -> Unit
) {
    val stepPresets = listOf(3000, 5000, 7500, 8000, 10000, 15000)
    val waterPresets = listOf(1500, 2000, 2500, 3000, 3500)
    var showStepGoalDialog by remember { mutableStateOf(false) }

    if (showStepGoalDialog) {
        StepGoalDialog(
            currentGoal = userProfile.dailyStepGoal,
            onDismiss = { showStepGoalDialog = false },
            onConfirm = { newGoal ->
                onUpdateStepGoal(newGoal)
                showStepGoalDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("goals_screen")
    ) {
        Text(
            text = "Fitness Goals & Badges",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Customize targets and collect achievements",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Step Goal Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("goals_step_target_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DirectionsWalk, contentDescription = "Step Goal", tint = MotionPrimaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Daily Step Target", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = { showStepGoalDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("goals_custom_goal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custom")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "%,d steps".format(userProfile.dailyStepGoal),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MotionPrimaryGreen)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stepPresets.forEach { preset ->
                        val selected = userProfile.dailyStepGoal == preset
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateStepGoal(preset) },
                            label = { Text("${preset / 1000}k") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MotionPrimaryGreen,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = userProfile.dailyStepGoal.toFloat().coerceIn(1000f, 30000f),
                    onValueChange = { onUpdateStepGoal(it.toInt()) },
                    valueRange = 1000f..30000f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = MotionPrimaryGreen,
                        activeTrackColor = MotionPrimaryGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Water Goal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.WaterDrop, contentDescription = "Water Goal", tint = MotionPrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Daily Hydration Target", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "%,d ml".format(userProfile.dailyWaterGoalMl),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = MotionPrimaryBlue)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    waterPresets.forEach { preset ->
                        val selected = userProfile.dailyWaterGoalMl == preset
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateWaterGoal(preset) },
                            label = { Text("${preset / 1000}.${(preset % 1000) / 100}L") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MotionPrimaryBlue,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Achievement Badges Section
        Text(
            text = "Achievement Badges",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            achievements.forEach { achievement ->
                val icon = when (achievement.iconName) {
                    "directions_walk" -> Icons.Default.DirectionsWalk
                    "military_tech" -> Icons.Default.MilitaryTech
                    "emoji_events" -> Icons.Default.EmojiEvents
                    "workspace_premium" -> Icons.Default.WorkspacePremium
                    "water_drop" -> Icons.Default.WaterDrop
                    else -> Icons.Default.Timer
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (achievement.isUnlocked) 2.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (achievement.isUnlocked) MotionOrange.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (achievement.isUnlocked) icon else Icons.Default.Lock,
                                contentDescription = achievement.title,
                                tint = if (achievement.isUnlocked) MotionOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = achievement.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (achievement.isUnlocked && achievement.unlockedTimestamp != null) {
                            val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
                            val dateStr = sdf.format(Date(achievement.unlockedTimestamp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MotionPrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
