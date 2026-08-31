package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.coach.AiCoachAnalysis
import com.example.data.coach.AiCoachEngine
import com.example.data.coach.CoachInputData
import com.example.data.coach.OfflineAiCoachEngine
import com.example.data.coach.gemini.GeminiMultimodalClient
import com.example.data.local.DailyActivity
import com.example.data.local.MealLog
import com.example.data.local.UserProfile
import com.example.data.coach.CoachFoodDatabase
import com.example.ui.components.AiWorkoutTipsCard
import com.example.ui.theme.MotionPrimaryGreen
import com.example.ui.theme.MotionSecondaryBlue
import com.example.util.BmiCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCoachScreen(
    todayActivity: DailyActivity?,
    todayMeals: List<MealLog>,
    userProfile: UserProfile,
    allActivities: List<DailyActivity> = emptyList(),
    liveSteps: Int,
    activityState: String,
    formatDistance: (Double, Boolean) -> String,
    calculateBmi: (Double, Double) -> Double,
    getBmiCategory: (Double) -> Pair<String, String>,
    onAddWater: (Int) -> Unit,
    onNavigateToActivity: () -> Unit = {}
) {
    val steps = liveSteps.coerceAtLeast(todayActivity?.steps ?: 0)
    val distance = todayActivity?.distanceMeters ?: (steps * userProfile.strideLengthMeters)
    val calories = todayActivity?.calories ?: (steps * 0.04)
    val activeMinutes = todayActivity?.activeMinutes ?: (steps / 100)
    val waterIntake = todayActivity?.waterIntakeMl ?: 0
    val caloriesConsumed = todayMeals.sumOf { it.calories }

    val bmi = if (userProfile.weightKg > 0.0 && userProfile.heightCm > 0.0) {
        BmiCalculator.calculateBMI(weightKg = userProfile.weightKg, heightCm = userProfile.heightCm)
    } else 0.0
    val (bmiCategory, _) = BmiCalculator.getBmiCategory(bmi)

    val isImperial = userProfile.unitSystem.equals("Imperial", ignoreCase = true)

    val inputData = CoachInputData(
        name = userProfile.name,
        steps = steps,
        stepGoal = userProfile.dailyStepGoal,
        distanceMeters = distance,
        caloriesBurned = calories,
        activeMinutes = activeMinutes,
        waterIntakeMl = waterIntake,
        waterGoalMl = userProfile.dailyWaterGoalMl,
        heightCm = userProfile.heightCm,
        weightKg = userProfile.weightKg,
        bmi = bmi,
        bmiCategory = bmiCategory,
        currentActivity = activityState,
        activityLevel = userProfile.activityLevel,
        age = userProfile.age,
        gender = userProfile.gender,
        caloriesConsumed = caloriesConsumed,
        weeklyActivities = allActivities
    )

    val displayAnalysis = remember(inputData) { OfflineAiCoachEngine.analyze(inputData) }

    var selectedQuestion by remember { mutableStateOf<String?>(null) }
    var customQuestionText by remember { mutableStateOf("") }
    var lastAskedCustomQuestion by remember { mutableStateOf<String?>(null) }
    var customAnswerText by remember { mutableStateOf<String?>(null) }
    var isAskingCustomQuestion by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    val chatHistory = remember { mutableStateListOf<Pair<String, String>>() }
    val chatMessages = remember { mutableStateListOf<com.example.data.coach.CoachChatMessage>() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val sendTextQuery: (String?) -> Unit = { queryText ->
        val prompt = queryText?.trim() ?: ""
        if (prompt.isBlank()) {
            customAnswerText = "Please enter a question to ask your AI Coach."
        } else if (!isAskingCustomQuestion) {
            isAskingCustomQuestion = true
            customAnswerText = ""
            selectedQuestion = null
            lastAskedCustomQuestion = prompt
            customQuestionText = ""

            // Add user message to conversational chat stream
            val userMsg = com.example.data.coach.CoachChatMessage(
                isUser = true,
                messageText = prompt
            )
            chatMessages.add(userMsg)

            scope.launch {
                val response = AiCoachEngine.askCoach(
                    context = context,
                    question = prompt,
                    inputData = inputData,
                    userProfile = userProfile,
                    conversationHistory = chatHistory.toList()
                )

                customAnswerText = response.answerText

                val isErr = response.answerText.contains("rejected", ignoreCase = true) ||
                        response.answerText.contains("quota is unavailable", ignoreCase = true) ||
                        response.answerText.contains("rate limited", ignoreCase = true) ||
                        response.answerText.contains("authentication failed", ignoreCase = true) ||
                        response.answerText.contains("temporarily unavailable", ignoreCase = true) ||
                        response.answerText.contains("Unable to connect", ignoreCase = true)

                val defaultFollowUps = when {
                    isErr -> emptyList()
                    prompt.contains("step", ignoreCase = true) -> listOf("How can I increase my steps?", "Am I on track for my step goal?", "Workout tips for my steps")
                    prompt.contains("protein", ignoreCase = true) -> listOf("Cheap vegetarian protein sources", "How much protein do I need?", "Post-workout protein ideas")
                    prompt.contains("calorie", ignoreCase = true) || prompt.contains("weight", ignoreCase = true) -> listOf("How to lose fat safely?", "Daily calorie deficit target", "High volume low calorie foods")
                    prompt.contains("water", ignoreCase = true) || prompt.contains("hydration", ignoreCase = true) -> listOf("How much water should I drink?", "Hydration tips during workouts", "Benefits of hydration")
                    prompt.contains("workout", ignoreCase = true) || prompt.contains("exercise", ignoreCase = true) -> listOf("20-minute home workout routine", "Exercise for fat burn", "Recovery and stretching tips")
                    else -> listOf("Today's health summary", "Workout tips for my steps", "Diet suggestions")
                }

                val aiMsg = com.example.data.coach.CoachChatMessage(
                    isUser = false,
                    messageText = response.answerText,
                    detectedCategory = response.detectedCategory,
                    isError = isErr,
                    followUpInquiries = defaultFollowUps
                )
                chatMessages.add(aiMsg)

                if (prompt.isNotBlank()) {
                    chatHistory.add(prompt to response.answerText)
                }
                isAskingCustomQuestion = false
            }
        }
    }

    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Coach Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticItem(label = "AI Engine", status = "Local Offline Engine + AI", isOk = true)
                    DiagnosticItem(label = "Mode", status = "100% Text Assistant", isOk = true)
                    DiagnosticItem(label = "Metric Analytics", status = "Active", isOk = true)
                    DiagnosticItem(label = "Personalized Coaching", status = "Ready", isOk = true)
                    DiagnosticItem(label = "Privacy & Local Persistence", status = "Protected", isOk = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("ai_coach_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Banner
        item {
            HeaderCard(
                isLoading = isAskingCustomQuestion,
                isOffline = false,
                offlineMsg = null,
                onRetry = {},
                onOpenDiagnostics = { showDiagnosticsDialog = true }
            )
        }

        // 2. Health Score Card
        item {
            HealthScoreCard(
                score = displayAnalysis.healthScore,
                breakdown = displayAnalysis.scoreBreakdown
            )
        }

        // 3. Daily Motivation & State
        item {
            MotivationCard(
                motivation = displayAnalysis.dailyMotivation,
                currentActivity = activityState
            )
        }

        // 4. Primary Insights List
        item {
            InsightsSection(insights = displayAnalysis.primaryInsights)
        }

        // 5. Interactive "Ask Coach" Queries
        item {
            AskCoachSection(
                selectedQuestion = selectedQuestion,
                onSelectQuestion = { q ->
                    sendTextQuery(q)
                },
                customQuestionText = customQuestionText,
                onCustomQuestionChange = { customQuestionText = it },
                onSendCustomQuestion = {
                    sendTextQuery(customQuestionText)
                },
                lastAskedCustomQuestion = lastAskedCustomQuestion,
                onRetryCustomQuestion = {
                    lastAskedCustomQuestion?.let { sendTextQuery(it) }
                },
                isAskingCustomQuestion = isAskingCustomQuestion,
                customAnswerText = customAnswerText,
                chatHistory = chatHistory.toList(),
                chatMessages = chatMessages.toList(),
                onClearChat = {
                    chatMessages.clear()
                    chatHistory.clear()
                    customAnswerText = null
                    lastAskedCustomQuestion = null
                },
                inputData = inputData,
                analysis = displayAnalysis,
                onAddWater = onAddWater,
                onAskFollowUp = { question ->
                    sendTextQuery(question)
                }
            )
        }

        // 6. Workout Suggestion & AI Personalized Tips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                WorkoutSuggestionCard(
                    suggestion = displayAnalysis.workoutSuggestion,
                    onStartWorkout = onNavigateToActivity
                )

                AiWorkoutTipsCard(
                    liveSteps = liveSteps,
                    activityState = activityState,
                    userProfile = userProfile,
                    todayActivity = todayActivity,
                    onStartWorkoutMode = { onNavigateToActivity() }
                )
            }
        }

        // 7. Food Suggestion
        item {
            FoodSuggestionCard(suggestion = displayAnalysis.foodSuggestion)
        }

        // 8. Hydration Advice
        item {
            HydrationAdviceCard(
                hydration = displayAnalysis.hydrationAdvice,
                waterIntake = waterIntake,
                waterGoal = userProfile.dailyWaterGoalMl,
                onAddWater = onAddWater
            )
        }

        // 9. Recovery Advice
        item {
            RecoveryAdviceCard(recovery = displayAnalysis.recoveryAdvice)
        }

        // 10. Goal Progress Analysis
        item {
            GoalProgressCard(
                progress = displayAnalysis.goalProgress,
                formatDistance = formatDistance,
                distanceMeters = distance,
                isImperial = isImperial
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeaderCard(
    isLoading: Boolean = false,
    isOffline: Boolean = false,
    offlineMsg: String? = null,
    onRetry: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MotionPrimaryGreen,
                                    MotionSecondaryBlue
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Coach Icon",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MotionIQ AI Coach",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MotionPrimaryGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MotionPrimaryGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OFFLINE AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MotionPrimaryGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "100% On-Device Intelligence • Privacy-First Realtime Coaching",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onOpenDiagnostics) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI Coach Diagnostics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isOffline) {
                    IconButton(onClick = onRetry) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry Connection")
                    }
                }
            }
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MotionPrimaryGreen
                )
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Surface(
            color = if (isOk) MotionPrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOk) Color(0xFF007A33) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthScoreCard(
    score: Int,
    breakdown: com.example.data.coach.HealthScoreBreakdown
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Health Score",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time composite health index",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                score >= 80 -> MotionPrimaryGreen.copy(alpha = 0.25f)
                                score >= 50 -> MotionSecondaryBlue.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                        .border(
                            width = 2.dp,
                            color = when {
                                score >= 80 -> MotionPrimaryGreen
                                score >= 50 -> MotionSecondaryBlue
                                else -> MaterialTheme.colorScheme.error
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { (score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    score >= 80 -> MotionPrimaryGreen
                    score >= 50 -> MotionSecondaryBlue
                    else -> MaterialTheme.colorScheme.error
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreChip(label = "Steps", score = "${breakdown.stepScore}/35", icon = Icons.AutoMirrored.Filled.DirectionsWalk)
                ScoreChip(label = "Water", score = "${breakdown.waterScore}/25", icon = Icons.Default.WaterDrop)
                ScoreChip(label = "Active", score = "${breakdown.activeScore}/25", icon = Icons.Default.Timer)
                ScoreChip(label = "BMI", score = "${breakdown.bmiScore}/15", icon = Icons.Default.MonitorWeight)
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, score: String, icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = score,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MotivationCard(
    motivation: String,
    currentActivity: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Daily Motivation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Motivation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (currentActivity.lowercase()) {
                            "running", "jogging" -> Icons.AutoMirrored.Filled.DirectionsRun
                            "walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                            "cycling" -> Icons.Default.DirectionsBike
                            else -> Icons.Default.Speed
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentActivity,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "\"$motivation\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InsightsSection(insights: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Personalized AI Advice",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        insights.forEach { insight ->
            InsightCard(insightText = insight)
        }
    }
}

@Composable
private fun InsightCard(insightText: String) {
    val icon = when {
        insightText.contains("water", ignoreCase = true) -> Icons.Default.LocalDrink
        insightText.contains("step", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsWalk
        insightText.contains("activity", ignoreCase = true) || insightText.contains("walk", ignoreCase = true) -> Icons.Default.FitnessCenter
        insightText.contains("vegetables", ignoreCase = true) || insightText.contains("BMI", ignoreCase = true) -> Icons.Default.Restaurant
        insightText.contains("stretch", ignoreCase = true) -> Icons.Default.SelfImprovement
        else -> Icons.Default.Info
    }

    val iconColor = when {
        insightText.contains("low", ignoreCase = true) || insightText.contains("need", ignoreCase = true) || insightText.contains("inactive", ignoreCase = true) ->
            MaterialTheme.colorScheme.tertiary
        insightText.contains("excellent", ignoreCase = true) || insightText.contains("reached", ignoreCase = true) || insightText.contains("great", ignoreCase = true) ->
            MotionPrimaryGreen
        else -> MotionSecondaryBlue
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = insightText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AskCoachSection(
    selectedQuestion: String?,
    onSelectQuestion: (String) -> Unit,
    customQuestionText: String,
    onCustomQuestionChange: (String) -> Unit,
    onSendCustomQuestion: () -> Unit,
    lastAskedCustomQuestion: String?,
    onRetryCustomQuestion: () -> Unit,
    isAskingCustomQuestion: Boolean,
    customAnswerText: String?,
    chatHistory: List<Pair<String, String>> = emptyList(),
    chatMessages: List<com.example.data.coach.CoachChatMessage> = emptyList(),
    onClearChat: () -> Unit = {},
    inputData: CoachInputData,
    analysis: com.example.data.coach.AiCoachAnalysis,
    onAddWater: (Int) -> Unit,
    onAskFollowUp: (String) -> Unit = {}
) {
    val questions = listOf(
        "Today's health summary",
        "Workout tips for my steps",
        "Exercise suggestions",
        "Diet suggestions",
        "Hydration advice",
        "Recovery advice",
        "Am I on track for my step goal?",
        "Motivation",
        "Daily improvement tips"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MotionIQ AI Coach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (chatMessages.isNotEmpty()) {
                TextButton(
                    onClick = onClearChat,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Clear Chat",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear Chat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Conversational Chat Messages Feed
        if (chatMessages.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                chatMessages.forEach { msg ->
                    if (msg.isUser) {
                        // User message bubble (right-aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Card(
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MotionPrimaryGreen.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier
                                    .padding(start = 32.dp)
                                    .testTag("chat_message_user")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.messageText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        // AI Coach message bubble (left-aligned)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (msg.isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .padding(end = 24.dp)
                                    .testTag("chat_message_ai")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (msg.isError) MaterialTheme.colorScheme.error else MotionPrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (msg.isError) "AI Coach Notice" else "MotionIQ AI Coach",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (msg.isError) MaterialTheme.colorScheme.error else MotionPrimaryGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = msg.messageText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 20.sp,
                                        color = if (msg.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (msg.followUpInquiries.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Suggested follow-ups:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(msg.followUpInquiries) { inquiry ->
                                                SuggestionChip(
                                                    onClick = { onAskFollowUp(inquiry) },
                                                    label = { Text(inquiry, fontSize = 11.sp) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    if (msg.isError && !lastAskedCustomQuestion.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        FilledTonalButton(
                                            onClick = onRetryCustomQuestion,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Retry", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Question Input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customQuestionText,
                onValueChange = onCustomQuestionChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_ai_question_input"),
                placeholder = {
                    Text(
                        "Ask coach about workouts, diet, hydration...",
                        fontSize = 13.sp
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                enabled = !isAskingCustomQuestion
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Send Question Button
            val canSend = customQuestionText.isNotBlank() && !isAskingCustomQuestion
            IconButton(
                onClick = onSendCustomQuestion,
                enabled = canSend,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (canSend) MotionPrimaryGreen else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("send_coach_message_button")
            ) {
                if (isAskingCustomQuestion) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Question",
                        tint = if (canSend) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Analyzing Loading Banner
        if (isAskingCustomQuestion) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MotionPrimaryGreen.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = MotionPrimaryGreen,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "MotionIQ AI Coach is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MotionPrimaryGreen
                        )
                        Text(
                            text = "Generating personalized recommendations for you",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Preset Quick-Tap Questions
        Text(
            text = "Quick coach topics:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(questions) { question ->
                SuggestionChip(
                    onClick = {
                        if (!isAskingCustomQuestion) {
                            onSelectQuestion(question)
                        }
                    },
                    enabled = !isAskingCustomQuestion,
                    label = { Text(text = question, style = MaterialTheme.typography.labelMedium) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }
    }
}

private fun getAnswerForQuestion(
    question: String,
    input: CoachInputData,
    analysis: com.example.data.coach.AiCoachAnalysis
): String {
    return OfflineAiCoachEngine.generateOfflineAnswer(
        question = question,
        input = input,
        userProfile = null,
        conversationHistory = emptyList()
    )
}

@Composable
private fun WorkoutSuggestionCard(
    suggestion: com.example.data.coach.WorkoutSuggestion,
    onStartWorkout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MotionPrimaryGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MotionPrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Workout Suggestion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${suggestion.durationMinutes} min • ${suggestion.intensity}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Open Activity Tracker", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FoodSuggestionCard(suggestion: com.example.data.coach.FoodSuggestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Food & Nutrition Suggestion",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )

            Text(
                text = suggestion.macroFocus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = suggestion.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Recommended Meal Ideas:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            suggestion.mealIdeas.forEach { idea ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MotionPrimaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = idea,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun HydrationAdviceCard(
    hydration: com.example.data.coach.HydrationAdvice,
    waterIntake: Int,
    waterGoal: Int,
    onAddWater: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MotionSecondaryBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MotionSecondaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hydration Advice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MotionSecondaryBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "$waterIntake / $waterGoal ml",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = MotionSecondaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = hydration.status,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MotionSecondaryBlue
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = hydration.actionTip,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAddWater(250) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MotionSecondaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ 250 ml", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { onAddWater(500) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+ 500 ml")
                }
            }
        }
    }
}

@Composable
private fun RecoveryAdviceCard(recovery: com.example.data.coach.RecoveryAdvice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Recovery & Stretch Advice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = recovery.status,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = recovery.primaryTip,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Recommended Mobility Routine:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            recovery.stretches.forEach { stretch ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stretch,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    progress: com.example.data.coach.GoalProgressAnalysis,
    formatDistance: (Double, Boolean) -> String,
    distanceMeters: Double,
    isImperial: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MotionPrimaryGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MotionPrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Goal Progress Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MotionPrimaryGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = progress.overallStatus,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        color = MotionPrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProgressItem(label = "Step Target Progress", percent = progress.stepPercent, color = MotionPrimaryGreen)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressItem(label = "Hydration Target Progress", percent = progress.waterPercent, color = MotionSecondaryBlue)
            Spacer(modifier = Modifier.height(8.dp))
            ProgressItem(label = "Estimated Calorie Target", percent = progress.calorieBurnPercent, color = MaterialTheme.colorScheme.tertiary)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Coach Key Observations:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            progress.actionableInsights.forEach { insight ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = insight,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressItem(label: String, percent: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
