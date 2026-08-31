package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfile
import com.example.data.sensor.SensorFusionDiagnostics
import com.example.ui.components.GlassCard
import com.example.ui.components.SensorDiagnosticsDialog
import com.example.ui.components.StepGoalDialog
import com.example.ui.theme.*

import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilterChip
import com.example.ui.components.FuturisticButton
import com.example.util.LanguageUtils

@Composable
fun SettingsScreen(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleUnitSystem: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onUpdateStrideLength: (Double) -> Unit = {},
    onUpdateProfile: (UserProfile) -> Unit = {},
    onUpdateStepGoal: (Int) -> Unit = {},
    onResetData: () -> Unit,
    onSignOut: () -> Unit = {},
    fusionDiagnostics: SensorFusionDiagnostics = SensorFusionDiagnostics(),
    onResetSteps: () -> Unit = {}
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showStrideDialog by remember { mutableStateOf(false) }
    var showStepGoalDialog by remember { mutableStateOf(false) }
    var showSensorDiagnosticsDialog by remember { mutableStateOf(false) }
    var strideInputText by remember(userProfile.strideLengthMeters) {
        mutableStateOf("%.2f".format(if (userProfile.strideLengthMeters > 0) userProfile.strideLengthMeters else 0.75))
    }

    if (showStepGoalDialog) {
        StepGoalDialog(
            currentGoal = userProfile.dailyStepGoal,
            onDismiss = { showStepGoalDialog = false },
            onConfirm = { newGoal ->
                onUpdateStepGoal(newGoal)
                onUpdateProfile(userProfile.copy(dailyStepGoal = newGoal))
                showStepGoalDialog = false
            }
        )
    }

    val isImperial = userProfile.unitSystem == "Imperial"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "System Settings ⚙️",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "App preferences, units, and offline telemetry controls",
            style = MaterialTheme.typography.bodySmall,
            color = CyberPrimaryCyan
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Appearance & Preferences
        GlassCard(
            glowColor = CyberPrimaryCyan,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "APP PREFERENCES",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Dark Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DarkMode, contentDescription = "Dark Mode", tint = CyberPrimaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Soft White Theme", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Apple & Samsung Health inspired clean mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = !isDarkMode,
                        onCheckedChange = { onToggleDarkMode(!it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SoftPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Units Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Straighten, contentDescription = "Units", tint = CyberSecondaryViolet)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Imperial Units", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = if (isImperial) "Miles, Lbs, Inches" else "Km, Kg, Cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = isImperial,
                        onCheckedChange = onToggleUnitSystem,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyberSecondaryViolet,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Daily Step Goal Config Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStepGoalDialog = true }
                        .padding(vertical = 4.dp)
                        .testTag("settings_daily_step_goal_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Daily Step Goal", tint = CyberPrimaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Daily Step Goal", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "User-configurable milestone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = "%,d steps".format(userProfile.dailyStepGoal),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyberPrimaryCyan)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stride Length Config Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStrideDialog = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Stride Length", tint = CyberAccentMint)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Stride Length", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Distance = Steps × Stride", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Text(
                        text = "%.2f m".format(if (userProfile.strideLengthMeters > 0) userProfile.strideLengthMeters else 0.75),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = CyberAccentMint)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notifications Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications", tint = CyberPrimaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Activity Reminders", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Daily step & hydration nudges", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = userProfile.notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberPrimaryCyan,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // WATER REMINDER SETTINGS CARD
        GlassCard(
            glowColor = CyberSecondaryViolet,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "WATER REMINDER SETTINGS 💧",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Water Reminder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WaterDrop, contentDescription = "Water Reminder", tint = CyberPrimaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Water Reminders", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Local background notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = userProfile.waterReminderEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateProfile(userProfile.copy(waterReminderEnabled = enabled))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberPrimaryCyan,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                }

                if (userProfile.waterReminderEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Goal Selector
                    Text(text = "Daily Water Target", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2000, 2500, 3000, 4000).forEach { goal ->
                            val isSelected = userProfile.dailyWaterGoalMl == goal
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateProfile(userProfile.copy(dailyWaterGoalMl = goal)) },
                                label = { Text("${goal / 1000}.${if (goal % 1000 != 0) (goal % 1000)/100 else 0}L") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reminder Interval Selector
                    Text(text = "Reminder Interval", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        mapOf(30 to "30 Min", 60 to "1 Hour", 120 to "2 Hours").forEach { (mins, label) ->
                            val isSelected = userProfile.waterReminderIntervalMins == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateProfile(userProfile.copy(waterReminderIntervalMins = mins)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Start Time / End Time Row
                    Text(text = "Schedule Window", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userProfile.waterReminderStartTime,
                            onValueChange = { newTime -> onUpdateProfile(userProfile.copy(waterReminderStartTime = newTime)) },
                            label = { Text("Start (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = userProfile.waterReminderEndTime,
                            onValueChange = { newTime -> onUpdateProfile(userProfile.copy(waterReminderEndTime = newTime)) },
                            label = { Text("End (HH:mm)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // LANGUAGE PREFERENCE CARD
        GlassCard(
            glowColor = CyberAccentMint,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = CyberAccentMint)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "LANGUAGE PREFERENCE 🌐",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select your preferred app language. Updates instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langRows = LanguageUtils.SUPPORTED_LANGUAGES.chunked(2)
                    langRows.forEach { rowLangs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowLangs.forEach { lang ->
                                val isSelected = userProfile.language.equals(lang, ignoreCase = true) ||
                                        (userProfile.language.isBlank() && lang == "English")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onUpdateProfile(userProfile.copy(language = lang))
                                    },
                                    label = {
                                        Text(
                                            text = lang,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowLangs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ACCOUNT & SESSION CARD
        GlassCard(
            glowColor = CyberDanger,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "ACCOUNT & SESSION 🔒",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Signed in as ${userProfile.name} (${userProfile.email.ifBlank { "Google Account" }})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                FuturisticButton(
                    text = "Sign Out of MotionIQ",
                    onClick = onSignOut,
                    icon = Icons.AutoMirrored.Filled.Logout,
                    primaryColor = CyberDanger,
                    secondaryColor = CyberPinkGlow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_signout_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Privacy & Maintenance
        GlassCard(
            glowColor = CyberDanger,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "PRIVACY & DATABASE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPrivacyDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.PrivacyTip, contentDescription = "Privacy Policy", tint = CyberAccentMint)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Privacy Statement", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "100% offline-first. Data remains on local device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSignOut() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Log Out", tint = CyberDanger)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Log Out of Account", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CyberDanger))
                        Text(text = "Sign out of your session and return to Login", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetDialog = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Reset Local Data", tint = CyberDanger)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Reset All Local Data", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = CyberDanger))
                        Text(text = "Clear Room database and restore defaults", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DEVELOPER & HARDWARE SENSORS
        GlassCard(
            glowColor = CyberPrimaryCyan,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "HARDWARE SENSORS & DIAGNOSTICS 🔬",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Developer tool to view live 3-axis accelerometer, gyroscope, magnetometer fusion telemetry, walking confidence scores, and pedometer baseline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                FuturisticButton(
                    text = "Open Sensor Diagnostics",
                    onClick = { showSensorDiagnosticsDialog = true },
                    icon = Icons.Default.BugReport,
                    primaryColor = CyberPrimaryCyan,
                    secondaryColor = CyberAccentMint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_diagnostics_button")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // App Version Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Version", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "MotionIQ Cyber Studio v2.0.0 (Production)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Application Data?") },
            text = { Text("This will permanently clear all stored step history, water logs, meal logs, and goal achievements. All activity statistics will be reset to zero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onResetData()
                        showResetDialog = false
                    }
                ) {
                    Text("Clear All Data", color = CyberDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy & Data Security") },
            text = {
                Text(
                    "MotionIQ is strictly offline-first. All your pedometer steps, health attributes, and activity logs are stored locally using SQLite / Room Database on your device.\n\nNo personal fitness data is uploaded to cloud servers or sold to third parties."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showStrideDialog) {
        AlertDialog(
            onDismissRequest = { showStrideDialog = false },
            title = { Text("Set Stride Length") },
            text = {
                Column {
                    Text(
                        text = "Your stride length is used to convert step count directly into walking distance (Distance = Steps × Stride). Default is 0.75 meters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = strideInputText,
                        onValueChange = { strideInputText = it },
                        label = { Text("Stride Length (meters)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newStride = strideInputText.toDoubleOrNull() ?: 0.75
                        if (newStride in 0.3..2.5) {
                            onUpdateStrideLength(newStride)
                        }
                        showStrideDialog = false
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStrideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSensorDiagnosticsDialog) {
        SensorDiagnosticsDialog(
            diagnostics = fusionDiagnostics,
            onDismiss = { showSensorDiagnosticsDialog = false },
            onResetSteps = {
                onResetSteps()
                showSensorDiagnosticsDialog = false
            }
        )
    }
}

