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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyActivity
import com.example.data.local.UserProfile
import com.example.ui.components.AnimatedCountUpText
import com.example.ui.components.FuturisticButton
import com.example.ui.components.GlassCard
import com.example.ui.components.StepGoalDialog
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberDanger
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.example.ui.theme.CyberSecondaryViolet
import com.example.ui.theme.CyberWarning
import com.example.util.BmiCalculator

@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    allActivities: List<DailyActivity>,
    formatHeight: (Double, Boolean) -> String,
    formatWeight: (Double, Boolean) -> String,
    onUpdateProfile: (UserProfile) -> Unit,
    onUpdateStepGoal: (Int) -> Unit = {},
    onNavigateToSettings: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var showStepGoalDialog by remember { mutableStateOf(false) }
    var stepGoalFeedbackMessage by remember { mutableStateOf<String?>(null) }

    var nameInput by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var heightInput by remember(userProfile.heightCm) { mutableStateOf(userProfile.heightCm.toInt().toString()) }
    var weightInput by remember(userProfile.weightKg) { mutableStateOf(userProfile.weightKg.toInt().toString()) }
    var ageInput by remember(userProfile.age) { mutableStateOf(userProfile.age.toString()) }
    var genderInput by remember(userProfile.gender) { mutableStateOf(userProfile.gender.ifBlank { "Male" }) }
    var goalInput by remember(userProfile.fitnessGoal) { mutableStateOf(userProfile.fitnessGoal.ifBlank { "Weight Loss" }) }

    val isImperial = userProfile.unitSystem == "Imperial"
    val lifetimeSteps = allActivities.sumOf { it.steps }

    if (showStepGoalDialog) {
        StepGoalDialog(
            currentGoal = userProfile.dailyStepGoal,
            onDismiss = { showStepGoalDialog = false },
            onConfirm = { newGoal ->
                onUpdateStepGoal(newGoal)
                onUpdateProfile(userProfile.copy(dailyStepGoal = newGoal))
                stepGoalFeedbackMessage = "Daily step goal updated to %,d steps.".format(newGoal)
                showStepGoalDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("profile_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Athlete Profile 👤",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = CyberPrimaryCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Feedback confirmation banner when step goal updated
        if (stepGoalFeedbackMessage != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CyberAccentMint.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .testTag("step_goal_success_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = CyberAccentMint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stepGoalFeedbackMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Avatar Header Glass Card
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
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(CyberPrimaryCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Avatar",
                        tint = CyberPrimaryCyan,
                        modifier = Modifier.size(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = userProfile.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${userProfile.gender.ifBlank { "Male" }} • Goal: ${userProfile.fitnessGoal.ifBlank { "Weight Loss" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberAccentMint
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Height", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatHeight(userProfile.heightCm, isImperial), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberPrimaryCyan))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = formatWeight(userProfile.weightKg, isImperial), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberSecondaryViolet))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Age", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${userProfile.age} yrs", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberWarning))
                    }

                    val profileBmi = if (userProfile.weightKg > 0 && userProfile.heightCm > 0) {
                        BmiCalculator.calculateBMI(weightKg = userProfile.weightKg, heightCm = userProfile.heightCm)
                    } else 0.0
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (profileBmi > 0.0) "%.1f".format(profileBmi) else "N/A",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = CyberAccentMint)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                FuturisticButton(
                    text = if (isEditing) "Close Editor" else "Edit Profile Fields",
                    onClick = { isEditing = !isEditing },
                    icon = Icons.Default.Edit,
                    primaryColor = if (isEditing) CyberSecondaryViolet else CyberPrimaryCyan,
                    secondaryColor = if (isEditing) CyberPinkGlow else CyberAccentMint,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Step Goal Configuration Card
        GlassCard(
            glowColor = CyberPrimaryCyan,
            shape = RoundedCornerShape(24.dp),
            testTag = "daily_step_goal_card"
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberPrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Step Goal",
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DAILY STEP GOAL",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personal daily movement milestone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = "%,d".format(userProfile.dailyStepGoal),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyberPrimaryCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                FuturisticButton(
                    text = "Change Daily Step Goal 🎯",
                    onClick = { showStepGoalDialog = true },
                    primaryColor = CyberPrimaryCyan,
                    secondaryColor = CyberAccentMint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("change_step_goal_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Profile Editor Card if toggled
        if (isEditing) {
            GlassCard(
                glowColor = CyberSecondaryViolet,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "EDIT LOCAL PROFILE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = genderInput,
                            onValueChange = { genderInput = it },
                            label = { Text("Gender") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        label = { Text("Goal (e.g. Weight Loss, Muscle Gain)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FuturisticButton(
                        text = "Save Profile Changes",
                        onClick = {
                            val newH = heightInput.toDoubleOrNull() ?: userProfile.heightCm
                            val newW = weightInput.toDoubleOrNull() ?: userProfile.weightKg
                            val newAge = ageInput.toIntOrNull() ?: userProfile.age

                            onUpdateProfile(
                                userProfile.copy(
                                    name = nameInput.ifBlank { "User" },
                                    age = newAge,
                                    gender = genderInput.ifBlank { "Male" },
                                    heightCm = newH,
                                    weightKg = newW,
                                    fitnessGoal = goalInput.ifBlank { "Weight Loss" }
                                )
                            )
                            isEditing = false
                        },
                        primaryColor = CyberAccentMint,
                        secondaryColor = CyberPrimaryCyan,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Lifetime Stats Card
        GlassCard(
            glowColor = CyberAccentMint,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = "Lifetime Stats", tint = CyberAccentMint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LIFETIME TOTALS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Step Count", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AnimatedCountUpText(
                            targetValue = lifetimeSteps,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberAccentMint,
                            formatPattern = "%,d"
                        )
                    }

                    Column {
                        Text(text = "Active Days Logged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${allActivities.size} Days", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = CyberPrimaryCyan))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FuturisticButton(
            text = "Log Out of MotionIQ",
            onClick = onSignOut,
            icon = Icons.AutoMirrored.Filled.Logout,
            primaryColor = CyberDanger,
            secondaryColor = CyberPinkGlow,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("logout_button")
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

