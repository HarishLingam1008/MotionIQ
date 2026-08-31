package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyActivity
import com.example.ui.theme.CyberAccentMint
import com.example.ui.theme.CyberPrimaryCyan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeeklyStepHistoryCard(
    allActivities: List<DailyActivity>,
    modifier: Modifier = Modifier,
    dailyStepGoal: Int = 10000
) {
    val weeklyChartData = remember(allActivities) {
        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val activityMap = allActivities.associateBy { it.date }

        val calendar = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dateStr = sdfDate.format(cal.time)
            val dayLabel = sdfDay.format(cal.time)

            val activity = activityMap[dateStr]
            val steps = activity?.steps ?: 0

            ChartBarData(
                label = dayLabel,
                value = steps.toFloat(),
                formattedValue = "%,d steps".format(steps)
            )
        }
    }

    val totalWeeklySteps = weeklyChartData.sumOf { it.value.toInt() }
    val avgWeeklySteps = if (weeklyChartData.isNotEmpty()) totalWeeklySteps / weeklyChartData.size else 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_step_history_card")
    ) {
        CustomBarChart(
            title = "Weekly Step History 📊",
            data = weeklyChartData,
            barHeight = 190
        )
    }
}
