package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Activity : Screen("activity", "Activity", Icons.AutoMirrored.Filled.DirectionsWalk)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object AiCoach : Screen("ai_coach", "AI Coach", Icons.Default.AutoAwesome)
    object Analytics : Screen("analytics", "Analytics", Icons.Default.BarChart)
    object Goals : Screen("goals", "Goals", Icons.Default.EmojiEvents)
    object Health : Screen("health", "Health", Icons.Default.Favorite)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object CalorieCalculator : Screen("calorie_calculator", "Calorie Calculator", Icons.Default.Favorite)

    companion object {
        val bottomNavItems = listOf(
            Home,
            Activity,
            Map,
            AiCoach,
            Health,
            Analytics,
            Profile
        )
    }
}
