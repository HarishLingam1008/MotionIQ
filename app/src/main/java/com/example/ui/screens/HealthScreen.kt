package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyActivity
import com.example.data.local.MealLog
import com.example.data.local.UserProfile
import com.example.data.model.DynamicFoodEngine
import com.example.data.model.FoodDataCatalog
import com.example.data.model.FoodRecommendation
import com.example.ui.theme.MotionOrange
import com.example.ui.theme.MotionPrimaryGreen
import com.example.ui.theme.MotionPurple
import com.example.ui.theme.MotionRed
import com.example.ui.theme.MotionSecondaryBlue
import com.example.util.BmiCalculator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HealthScreen(
    userProfile: UserProfile,
    todayActivity: DailyActivity?,
    todayMeals: List<MealLog>,
    liveSteps: Int = 0,
    formatDistance: (Double, Boolean) -> String,
    calculateBmi: (Double, Double) -> Double,
    getBmiCategory: (Double) -> Pair<String, String>,
    calculateBmr: (Double, Double, Int, String) -> Int,
    calculateDailyCaloriesNeeded: (Double, Double, Int, String, String) -> Int,
    onAddWater: (Int) -> Unit,
    onAddMealLog: (MealLog) -> Unit,
    onDeleteMealLog: (MealLog) -> Unit,
    onClearMeals: () -> Unit,
    onUpdateProfile: (UserProfile) -> Unit
) {
    var activeTab by remember { mutableStateOf("All") } // "All", "Daily Food Plan", "Food Recs", "Calculator", "Meals", "BMI", "Water"
    var showAddMealDialog by remember { mutableStateOf(false) }
    var selectedMealCategoryFilter by remember { mutableStateOf("All") }
    var selectedBudgetFilter by remember { mutableStateOf("All Budgets") } // "All Budgets", "Under ₹30", "Under ₹50", "Under ₹100"
    var selectedDietFilter by remember { mutableStateOf("All Diets") } // "All Diets", "Veg Only", "Non-Veg"
    var foodViewMode by remember { mutableStateOf("Indian Food Menu") } // "Indian Food Menu", "Macro-Targeted"
    var mealPlanSeed by remember { mutableStateOf(0) }

    // User Profile input states
    var heightInput by remember(userProfile.heightCm) { mutableStateOf(if (userProfile.heightCm > 0) userProfile.heightCm.toInt().toString() else "") }
    var weightInput by remember(userProfile.weightKg) { mutableStateOf(if (userProfile.weightKg > 0) userProfile.weightKg.toInt().toString() else "") }
    var ageInput by remember(userProfile.age) { mutableStateOf(if (userProfile.age > 0) userProfile.age.toString() else "") }
    var genderState by remember(userProfile.gender) { mutableStateOf(userProfile.gender) }
    var activityLevelState by remember(userProfile.activityLevel) { mutableStateOf(userProfile.activityLevel) }
    var fitnessGoalState by remember(userProfile.fitnessGoal) { mutableStateOf(userProfile.fitnessGoal) }
    var dietPrefState by remember(userProfile.dietPreference) { mutableStateOf(userProfile.dietPreference) }

    val isProfileComplete = userProfile.isProfileComplete()

    val currentHeightCm = heightInput.toDoubleOrNull() ?: userProfile.heightCm
    val currentWeightKg = weightInput.toDoubleOrNull() ?: userProfile.weightKg
    val currentAge = ageInput.toIntOrNull() ?: userProfile.age

    // Calculations using DynamicFoodEngine
    val targetMacros = remember(currentWeightKg, currentHeightCm, currentAge, genderState, activityLevelState, fitnessGoalState) {
        DynamicFoodEngine.calculateTargetMacros(
            weightKg = currentWeightKg,
            heightCm = currentHeightCm,
            age = currentAge,
            gender = genderState,
            activityLevel = activityLevelState,
            fitnessGoal = fitnessGoalState
        )
    }

    val dynamicMealRecs = remember(currentWeightKg, currentHeightCm, currentAge, genderState, activityLevelState, fitnessGoalState, dietPrefState) {
        DynamicFoodEngine.generateDynamicMeals(
            weightKg = currentWeightKg,
            heightCm = currentHeightCm,
            age = currentAge,
            gender = genderState,
            activityLevel = activityLevelState,
            fitnessGoal = fitnessGoalState,
            dietPreference = dietPrefState
        )
    }

    val isBmiValid = currentWeightKg > 0.0 && currentHeightCm > 0.0
    val bmiScore = if (isBmiValid) BmiCalculator.calculateBMI(weightKg = currentWeightKg, heightCm = currentHeightCm) else 0.0
    val (bmiCategory, bmiTip) = if (isBmiValid && bmiScore > 0.0) BmiCalculator.getBmiCategory(bmiScore) else Pair("Not Available", "Enter valid height and weight to calculate BMI.")

    val steps = if (liveSteps > 0 || (todayActivity?.steps ?: 0) == 0) liveSteps else (todayActivity?.steps ?: 0)
    val activeStepCalories = todayActivity?.calories ?: 0.0
    val distanceMeters = todayActivity?.distanceMeters ?: 0.0

    // BMR & Calorie calculations
    val bmr = if (isProfileComplete) targetMacros.bmr else 0
    val dailyCaloriesNeeded = if (isProfileComplete) targetMacros.dailyCalories else 0
    val estimatedTotalCaloriesBurned = if (isProfileComplete) (bmr + activeStepCalories).toInt() else 0

    // Meals calculation
    val caloriesConsumed = todayMeals.sumOf { it.calories }
    val remainingCalories = if (isProfileComplete) (dailyCaloriesNeeded - caloriesConsumed).coerceAtLeast(-9999) else 0

    // Water calculations
    val waterMl = todayActivity?.waterIntakeMl ?: 0
    val targetWaterMl = if (isProfileComplete) targetMacros.waterGoalMl else userProfile.dailyWaterGoalMl.coerceAtLeast(1000)
    val waterProgress = (waterMl.toFloat() / targetWaterMl.toFloat()).coerceIn(0f, 1f)

    // Daily 4-Meal Plan calculation (Carrot, Spinach, Dal emphasis + Goal Integration)
    val dailyFoodPlan = remember(mealPlanSeed, dietPrefState, dailyCaloriesNeeded, fitnessGoalState) {
        FoodDataCatalog.generateDailyMealPlan(
            seed = mealPlanSeed,
            dietPreference = dietPrefState,
            targetCalories = if (dailyCaloriesNeeded > 0) dailyCaloriesNeeded else 2000,
            fitnessGoal = fitnessGoalState
        )
    }

    val isImperial = userProfile.unitSystem.equals("Imperial", ignoreCase = true)

    val categories = listOf("All", "Daily Plan", "Food Recs", "Calculator", "Meals", "BMI", "Water")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("health_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Health & Nutrition",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Calorie BMR, Meal Logger, BMI & Hydration",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = activeTab == category,
                    onClick = { activeTab = category },
                    label = { Text(category, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PROMPT CARD FOR INCOMPLETE PROFILE
        if (!isProfileComplete) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Incomplete Profile",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Complete your profile to unlock BMI, BMR, calorie tracking and food recommendations.",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Profile Inputs directly inside the card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = { weightInput = it },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Male", "Female").forEach { g ->
                                FilterChip(
                                    selected = genderState.equals(g, ignoreCase = true),
                                    onClick = { genderState = g },
                                    label = { Text(g, fontSize = 12.sp) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val a = ageInput.toIntOrNull() ?: 0
                                val h = heightInput.toDoubleOrNull() ?: 0.0
                                val w = weightInput.toDoubleOrNull() ?: 0.0
                                val g = if (genderState.isNotBlank()) genderState else "Male"
                                if (a > 0 && h > 0.0 && w > 0.0) {
                                    onUpdateProfile(
                                        userProfile.copy(
                                            age = a,
                                            heightCm = h,
                                            weightKg = w,
                                            gender = g
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save & Unlock", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1. NUTRITION DASHBOARD (Overview Cards)
        if (activeTab == "All" || activeTab == "Dashboard") {
            Text(
                text = "Nutrition Dashboard",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Dashboard Grid (2x3 cards)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                // Card 1: Calories Burned
                DashboardMetricCard(
                    title = "CALORIES BURNED",
                    value = if (isProfileComplete) "%,d".format(estimatedTotalCaloriesBurned) else "0",
                    unit = "kcal",
                    subtitle = if (isProfileComplete) "BMR + Steps" else "Profile Required",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = MotionOrange,
                    modifier = Modifier.weight(1f)
                )

                // Card 2: Calories Consumed
                DashboardMetricCard(
                    title = "CONSUMED",
                    value = "%,d".format(caloriesConsumed),
                    unit = "kcal",
                    subtitle = "${todayMeals.size} Meals Logged",
                    icon = Icons.Default.Restaurant,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Card 3: Remaining Calories
                DashboardMetricCard(
                    title = "REMAINING",
                    value = if (isProfileComplete) "%,d".format(remainingCalories) else "Not Calculated",
                    unit = if (isProfileComplete) "kcal" else "",
                    subtitle = if (isProfileComplete) "Target: %,d".format(dailyCaloriesNeeded) else "Complete Profile",
                    icon = Icons.Default.Fastfood,
                    iconColor = MotionPurple,
                    modifier = Modifier.weight(1f)
                )

                // Card 4: BMI Score
                DashboardMetricCard(
                    title = "BMI SCORE",
                    value = if (isBmiValid && bmiScore > 0.0) "%.1f".format(bmiScore) else "Not Available",
                    unit = if (isBmiValid && bmiScore > 0.0) bmiCategory else "",
                    subtitle = "Body Mass Index",
                    icon = Icons.Default.MonitorWeight,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )

                // Card 5: Water Intake
                DashboardMetricCard(
                    title = "WATER INTAKE",
                    value = "%,d".format(waterMl),
                    unit = "ml",
                    subtitle = "Goal: %,d ml".format(targetWaterMl),
                    icon = Icons.Default.WaterDrop,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )

                // Card 6: Daily Steps
                val stepGoal = userProfile.dailyStepGoal.coerceAtLeast(1)
                val stepPercent = (steps * 100 / stepGoal).coerceIn(0, 100)
                DashboardMetricCard(
                    title = "DAILY STEPS",
                    value = "%,d".format(steps),
                    unit = "$stepPercent%",
                    subtitle = "Goal: %,d".format(stepGoal),
                    icon = Icons.Default.DirectionsWalk,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. CALORIE & BMR CALCULATOR (Mifflin-St Jeor)
        if (activeTab == "All" || activeTab == "Calculator") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MotionOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = "Calorie Calculator", tint = MotionOrange)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Calorie & BMR Calculator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Based on Mifflin-St Jeor Equation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageInput,
                            onValueChange = {
                                ageInput = it
                                it.toIntOrNull()?.let { newAge -> onUpdateProfile(userProfile.copy(age = newAge)) }
                            },
                            label = { Text("Age") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = heightInput,
                            onValueChange = {
                                heightInput = it
                                it.toDoubleOrNull()?.let { newH -> onUpdateProfile(userProfile.copy(heightCm = newH)) }
                            },
                            label = { Text("Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = weightInput,
                            onValueChange = {
                                weightInput = it
                                it.toDoubleOrNull()?.let { newW -> onUpdateProfile(userProfile.copy(weightKg = newW)) }
                            },
                            label = { Text("Weight (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gender Selector
                    Text(text = "GENDER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female").forEach { g ->
                            FilterChip(
                                selected = genderState.equals(g, ignoreCase = true),
                                onClick = {
                                    genderState = g
                                    onUpdateProfile(userProfile.copy(gender = g))
                                },
                                label = { Text(g) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Activity Level Chips
                    Text(text = "DAILY ACTIVITY LEVEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    val levels = listOf("Sedentary", "Lightly Active", "Moderately Active", "Very Active", "Extra Active")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(levels) { level ->
                            FilterChip(
                                selected = activityLevelState.equals(level, ignoreCase = true),
                                onClick = {
                                    activityLevelState = level
                                    onUpdateProfile(userProfile.copy(activityLevel = level))
                                },
                                label = { Text(level) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fitness Goal Selector
                    Text(text = "FITNESS GOAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    val goals = listOf("Weight Loss", "Maintain Weight", "Muscle Gain")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(goals) { goal ->
                            FilterChip(
                                selected = fitnessGoalState.equals(goal, ignoreCase = true),
                                onClick = {
                                    fitnessGoalState = goal
                                    onUpdateProfile(userProfile.copy(fitnessGoal = goal))
                                },
                                label = { Text(goal) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Diet Preference Selector
                    Text(text = "DIET PREFERENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Veg", "Non-Veg").forEach { diet ->
                            FilterChip(
                                selected = dietPrefState.equals(diet, ignoreCase = true),
                                onClick = {
                                    dietPrefState = diet
                                    onUpdateProfile(userProfile.copy(dietPreference = diet))
                                },
                                label = { Text(diet) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Results Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("BASAL METABOLIC RATE (BMR)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = if (isProfileComplete) "%,d kcal".format(bmr) else "Not Calculated",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TARGET CALORIES ($fitnessGoalState)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = if (isProfileComplete) "%,d kcal".format(dailyCaloriesNeeded) else "Not Calculated",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Daily Nutrition Targets Table
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("daily_nutrition_targets_table"),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "Daily Nutrition Targets",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    // Table Header Row: Nutrient | Target
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Nutrient",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Start
                                        )
                                        Text(
                                            text = "Target",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 1.dp
                                    )

                                    // 4-row Nutrition Targets Data
                                    val nutritionRows = listOf(
                                        "Protein" to if (isProfileComplete) "%.1f g".format(targetMacros.proteinG) else "0.0 g",
                                        "Carbohydrates" to if (isProfileComplete) "%.1f g".format(targetMacros.carbsG) else "0.0 g",
                                        "Fat" to if (isProfileComplete) "%.1f g".format(targetMacros.fatG) else "0.0 g",
                                        "Water" to if (isProfileComplete) "${targetMacros.waterGoalMl} ml" else "0 ml"
                                    )

                                    nutritionRows.forEachIndexed { index, (nutrient, targetValue) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 36.dp)
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = nutrient,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Start
                                            )
                                            Text(
                                                text = targetValue,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.End
                                            )
                                        }

                                        if (index < nutritionRows.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                                thickness = 0.8.dp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Estimated Step Burn Today:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (activeStepCalories > 0) "+%.1f kcal".format(activeStepCalories) else "0.0 kcal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MotionOrange)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Total Energy Expended Today:", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = if (isProfileComplete) "%,d kcal".format(estimatedTotalCaloriesBurned) else "0 kcal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ==================== 2.5 DAILY AFFORDABLE FOOD PLAN ====================
        if (activeTab == "All" || activeTab == "Daily Plan") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_food_plan_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with Title and Regenerate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MotionPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = MotionPurple
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Affordable Food Plan",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "4 Meals • Carrot, Spinach & Dal Priority",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { mealPlanSeed++ },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("regenerate_daily_food_plan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Regenerate Plan",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Regenerate",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Goal & Caloric Target Integration Banner
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "CALORIE TARGET ALIGNMENT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Target: ${if (dailyCaloriesNeeded > 0) dailyCaloriesNeeded else 2000} kcal • Plan: ${dailyFoodPlan.totalCalories} kcal",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MotionPrimaryGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = fitnessGoalState.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MotionPrimaryGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = dailyFoodPlan.goalFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Macros & Cost Pills Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MotionPrimaryGreen.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Protein", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${dailyFoodPlan.totalProteinG.toInt()}g", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MotionPrimaryGreen)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MotionSecondaryBlue.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Carbs", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${dailyFoodPlan.totalCarbsG.toInt()}g", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MotionSecondaryBlue)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MotionOrange.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Fat", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${dailyFoodPlan.totalFatG.toInt()}g", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MotionOrange)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MotionPurple.copy(alpha = 0.1f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Budget", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("~₹${dailyFoodPlan.estimatedCostInr}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MotionPurple)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Priority Vegetables & Ingredients Highlight Row
                    Text(
                        text = "PRIORITY STAPLE INGREDIENTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dailyFoodPlan.featuredVegetables) { veg ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = veg,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 MEALS LIST (Breakfast, Lunch, Evening Snack, Dinner)
                    Text(
                        text = "4-MEAL DAILY SCHEDULE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DailyMealScheduleCard(
                            slotLabel = "🌅 Breakfast",
                            food = dailyFoodPlan.breakfast,
                            accentColor = MotionOrange,
                            onLogMeal = {
                                onAddMealLog(
                                    MealLog(
                                        date = "",
                                        mealType = "Breakfast",
                                        foodName = dailyFoodPlan.breakfast.name,
                                        calories = dailyFoodPlan.breakfast.calories,
                                        proteinG = dailyFoodPlan.breakfast.proteinG,
                                        carbsG = dailyFoodPlan.breakfast.carbsG,
                                        fatG = dailyFoodPlan.breakfast.fatG
                                    )
                                )
                            }
                        )

                        DailyMealScheduleCard(
                            slotLabel = "☀️ Lunch",
                            food = dailyFoodPlan.lunch,
                            accentColor = MotionPrimaryGreen,
                            onLogMeal = {
                                onAddMealLog(
                                    MealLog(
                                        date = "",
                                        mealType = "Lunch",
                                        foodName = dailyFoodPlan.lunch.name,
                                        calories = dailyFoodPlan.lunch.calories,
                                        proteinG = dailyFoodPlan.lunch.proteinG,
                                        carbsG = dailyFoodPlan.lunch.carbsG,
                                        fatG = dailyFoodPlan.lunch.fatG
                                    )
                                )
                            }
                        )

                        DailyMealScheduleCard(
                            slotLabel = "☕ Evening Snack",
                            food = dailyFoodPlan.eveningSnack,
                            accentColor = MotionPurple,
                            onLogMeal = {
                                onAddMealLog(
                                    MealLog(
                                        date = "",
                                        mealType = "Snack",
                                        foodName = dailyFoodPlan.eveningSnack.name,
                                        calories = dailyFoodPlan.eveningSnack.calories,
                                        proteinG = dailyFoodPlan.eveningSnack.proteinG,
                                        carbsG = dailyFoodPlan.eveningSnack.carbsG,
                                        fatG = dailyFoodPlan.eveningSnack.fatG
                                    )
                                )
                            }
                        )

                        DailyMealScheduleCard(
                            slotLabel = "🌙 Dinner",
                            food = dailyFoodPlan.dinner,
                            accentColor = MotionSecondaryBlue,
                            onLogMeal = {
                                onAddMealLog(
                                    MealLog(
                                        date = "",
                                        mealType = "Dinner",
                                        foodName = dailyFoodPlan.dinner.name,
                                        calories = dailyFoodPlan.dinner.calories,
                                        proteinG = dailyFoodPlan.dinner.proteinG,
                                        carbsG = dailyFoodPlan.dinner.carbsG,
                                        fatG = dailyFoodPlan.dinner.fatG
                                    )
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Batch Action: Log all 4 meals at once
                    Button(
                        onClick = {
                            val meals = listOf(
                                "Breakfast" to dailyFoodPlan.breakfast,
                                "Lunch" to dailyFoodPlan.lunch,
                                "Snack" to dailyFoodPlan.eveningSnack,
                                "Dinner" to dailyFoodPlan.dinner
                            )
                            meals.forEach { (type, f) ->
                                onAddMealLog(
                                    MealLog(
                                        date = "",
                                        mealType = type,
                                        foodName = f.name,
                                        calories = f.calories,
                                        proteinG = f.proteinG,
                                        carbsG = f.carbsG,
                                        fatG = f.fatG
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_all_4_meals_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log All 4 Planned Meals Today", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. FOOD RECOMMENDATIONS
        if (activeTab == "All" || activeTab == "Food Recs") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Indian Food Recommendations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "50+ Affordable, Nutrient-Dense Culturally Familiar Choices",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Tap to Log",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector: Indian Food Catalog vs Personalized Targets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Indian Food Menu", "Macro-Targeted").forEach { mode ->
                        FilterChip(
                            selected = foodViewMode == mode,
                            onClick = { foodViewMode = mode },
                            label = { Text(if (mode == "Indian Food Menu") "🇮🇳 Indian Menu (56)" else "🎯 Personalized Targets", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                val foodCategories = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack", "Hydration")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(foodCategories) { cat ->
                        FilterChip(
                            selected = selectedMealCategoryFilter == cat,
                            onClick = { selectedMealCategoryFilter = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Budget & Diet Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val budgetOptions = listOf("All Budgets", "Under ₹30", "Under ₹50", "Under ₹100")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        items(budgetOptions) { budget ->
                            FilterChip(
                                selected = selectedBudgetFilter == budget,
                                onClick = { selectedBudgetFilter = budget },
                                label = { Text(budget, fontSize = 10.sp) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Diet Filter (Veg / Non-Veg)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All Diets", "Veg Only", "Non-Veg").forEach { diet ->
                        FilterChip(
                            selected = selectedDietFilter == diet,
                            onClick = { selectedDietFilter = diet },
                            label = { Text(diet, fontSize = 10.sp) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val sourceList = if (foodViewMode == "Macro-Targeted" && isProfileComplete) {
                    dynamicMealRecs
                } else {
                    FoodDataCatalog.recommendedFoods
                }

                val filteredFoods = sourceList.filter { food ->
                    val matchCategory = selectedMealCategoryFilter == "All" || food.category.equals(selectedMealCategoryFilter, ignoreCase = true)
                    val matchBudget = when (selectedBudgetFilter) {
                        "Under ₹30" -> food.priceInr <= 30
                        "Under ₹50" -> food.priceInr <= 50
                        "Under ₹100" -> food.priceInr <= 100
                        else -> true
                    }
                    val matchDiet = when (selectedDietFilter) {
                        "Veg Only" -> food.isVeg
                        "Non-Veg" -> !food.isVeg
                        else -> true
                    }
                    matchCategory && matchBudget && matchDiet
                }

                if (filteredFoods.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = "No Results", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "No food items match the current filters. Try changing budget or diet filters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredFoods) { food ->
                            FoodRecommendationCard(
                                food = food,
                                onLogMeal = {
                                    onAddMealLog(
                                        MealLog(
                                            date = "",
                                            mealType = food.category,
                                            foodName = food.name,
                                            calories = food.calories,
                                            proteinG = food.proteinG,
                                            carbsG = food.carbsG,
                                            fatG = food.fatG
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. MEAL LOGGER
        if (activeTab == "All" || activeTab == "Meals") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Restaurant, contentDescription = "Meal Logger", tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Daily Meal Logger", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = "Total Consumed: %,d kcal".format(caloriesConsumed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = { showAddMealDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Meal", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Meal", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (todayMeals.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Fastfood, contentDescription = "No Meals", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "No meals logged yet today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "Tap 'Log Meal' or pick from Recommendations above", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todayMeals.forEach { meal ->
                                MealItemRow(
                                    meal = meal,
                                    onDelete = { onDeleteMealLog(meal) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onClearMeals) {
                                Text("Clear Today's Meals", color = MotionRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. BMI CALCULATOR & GAUGE
        if (activeTab == "All" || activeTab == "BMI") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.MonitorWeight, contentDescription = "BMI Calculator", tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "BMI & Body Mass Index", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val scoreColor = when {
                        !isBmiValid || bmiScore <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                        bmiScore < 18.5 -> MaterialTheme.colorScheme.secondary
                        bmiScore in 18.5..24.9 -> MaterialTheme.colorScheme.primary
                        bmiScore in 25.0..29.9 -> MotionOrange
                        else -> MotionRed
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "BMI SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (isBmiValid && bmiScore > 0.0) "%.1f".format(bmiScore) else "Not Available",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = scoreColor)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = scoreColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = bmiCategory,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = scoreColor),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isBmiValid && bmiScore > 0.0) {
                        // BMI Gauge Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val secondaryC = MaterialTheme.colorScheme.secondary
                            val primaryC = MaterialTheme.colorScheme.primary
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                drawRoundRect(color = secondaryC, topLeft = Offset(0f, 0f), size = Size(w * 0.25f, h))
                                drawRoundRect(color = primaryC, topLeft = Offset(w * 0.25f, 0f), size = Size(w * 0.35f, h))
                                drawRoundRect(color = MotionOrange, topLeft = Offset(w * 0.60f, 0f), size = Size(w * 0.20f, h))
                                drawRoundRect(color = MotionRed, topLeft = Offset(w * 0.80f, 0f), size = Size(w * 0.20f, h))

                                val normalizedPos = ((bmiScore - 15.0) / 25.0).coerceIn(0.0, 1.0).toFloat()
                                val pointerX = w * normalizedPos
                                drawCircle(color = Color.White, radius = 9.dp.toPx(), center = Offset(pointerX, h / 2f))
                                drawCircle(color = scoreColor, radius = 6.dp.toPx(), center = Offset(pointerX, h / 2f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Lightbulb, contentDescription = "Health Suggestion", tint = scoreColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = bmiTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 6. WATER TRACKER
        if (activeTab == "All" || activeTab == "Water") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.WaterDrop, contentDescription = "Water Tracker", tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Water Tracker", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(text = "Daily Goal: %,d ml".format(targetWaterMl), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glass Visualizer
                        val animatedWaterFill by animateFloatAsState(
                            targetValue = waterProgress,
                            animationSpec = tween(durationMillis = 800),
                            label = "waterFill"
                        )

                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(96.dp)
                                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp, topStart = 6.dp, topEnd = 6.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(animatedWaterFill)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            val remainingWater = (targetWaterMl - waterMl).coerceAtLeast(0)
                            Text(
                                text = "%,d / %,d ml".format(waterMl, targetWaterMl),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                text = if (remainingWater > 0) "%,d ml remaining to goal".format(remainingWater) else "Goal Achieved! 🎉",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (remainingWater > 0) MaterialTheme.colorScheme.onSurfaceVariant else MotionPrimaryGreen
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { waterProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "QUICK ADD WATER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onAddWater(100) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+100 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { onAddWater(250) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+250 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { onAddWater(500) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+500 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { onAddWater(1000) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+1000 ml", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 7. DAILY HEALTH SUMMARY
        if (activeTab == "All" || activeTab == "Dashboard") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Daily Health Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SummaryRowItem(label = "Today's Steps", value = "%,d steps".format(steps), icon = Icons.Default.DirectionsWalk, color = MaterialTheme.colorScheme.primary)
                    SummaryRowItem(label = "Distance Covered", value = formatDistance(distanceMeters, isImperial), icon = Icons.Default.Timer, color = MaterialTheme.colorScheme.primary)
                    SummaryRowItem(label = "Total Calories Burned", value = if (isProfileComplete) "%,d kcal".format(estimatedTotalCaloriesBurned) else "0 kcal", icon = Icons.Default.LocalFireDepartment, color = MotionOrange)
                    SummaryRowItem(label = "Total Calories Consumed", value = "%,d kcal".format(caloriesConsumed), icon = Icons.Default.Restaurant, color = MotionPurple)
                    SummaryRowItem(label = "Water Intake", value = "%,d / %,d ml".format(waterMl, targetWaterMl), icon = Icons.Default.WaterDrop, color = MaterialTheme.colorScheme.secondary)
                    SummaryRowItem(label = "BMR", value = if (isProfileComplete) "%,d kcal".format(bmr) else "Not Calculated", icon = Icons.Default.LocalFireDepartment, color = MotionOrange)
                    SummaryRowItem(label = "BMI Score", value = if (isBmiValid && bmiScore > 0.0) "%.1f (%s)".format(bmiScore, bmiCategory) else "Not Available", icon = Icons.Default.MonitorWeight, color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Meal Dialog
    if (showAddMealDialog) {
        AddMealDialog(
            onDismiss = { showAddMealDialog = false },
            onConfirm = { meal ->
                onAddMealLog(meal)
                showAddMealDialog = false
            }
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun FoodRecommendationCard(
    food: FoodRecommendation,
    onLogMeal: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(230.dp)
            .height(230.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = food.category.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (food.isVeg) MotionPrimaryGreen.copy(alpha = 0.15f) else MotionRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (food.isVeg) "🌱 VEG" else "🍗 NON-VEG",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = if (food.isVeg) MotionPrimaryGreen else MotionRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MotionOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "₹${food.priceInr}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                            color = MotionOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Text(
                    text = "${food.calories} kcal • P:${food.proteinG.toInt()}g C:${food.carbsG.toInt()}g F:${food.fatG.toInt()}g",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = food.benefits,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }

            Button(
                onClick = onLogMeal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log Meal (₹${food.priceInr})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MealItemRow(
    meal: MealLog,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meal.mealType.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (meal.proteinG > 0 || meal.carbsG > 0 || meal.fatG > 0) {
                    Text(
                        text = "P: ${meal.proteinG.toInt()}g • C: ${meal.carbsG.toInt()}g • F: ${meal.fatG.toInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${meal.calories} kcal",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MotionOrange)
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete meal", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryRowItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }

        Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (MealLog) -> Unit
) {
    var selectedType by remember { mutableStateOf("Breakfast") }
    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }

    val mealTypes = listOf("Breakfast", "Lunch", "Dinner", "Snack")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Meal Entry", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("MEAL CATEGORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    mealTypes.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name (e.g. Grilled Salmon)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cals = caloriesText.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && cals > 0) {
                        onConfirm(
                            MealLog(
                                date = "",
                                mealType = selectedType,
                                foodName = foodName.trim(),
                                calories = cals,
                                proteinG = proteinText.toDoubleOrNull() ?: 0.0,
                                carbsG = carbsText.toDoubleOrNull() ?: 0.0,
                                fatG = fatText.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DailyMealScheduleCard(
    slotLabel: String,
    food: FoodRecommendation,
    accentColor: Color,
    onLogMeal: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = slotLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (food.isVeg) MotionPrimaryGreen.copy(alpha = 0.12f) else MotionOrange.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (food.isVeg) "🌱 VEG" else "🥚 NON-VEG",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = if (food.isVeg) MotionPrimaryGreen else MotionOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "₹${food.priceInr}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onLogMeal,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log ${food.name}",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Serving: ${food.servingSize}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Nutritional highlights
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔥 ${food.calories} kcal",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MotionOrange
                )
                Text(
                    text = "💪 ${food.proteinG}g protein",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MotionPrimaryGreen
                )
                Text(
                    text = "🌾 ${food.carbsG}g carbs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (food.benefits.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 ${food.benefits}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }
    }
}
