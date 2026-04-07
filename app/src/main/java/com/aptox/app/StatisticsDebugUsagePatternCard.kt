package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 통계 화면 전용 DEBUG — "사용 패턴" 프로토타입.
 * [StatisticsScreen]에서 `SHOW_DEBUG_MENU && DEBUG`일 때만 호출할 것.
 */
@Composable
fun StatisticsDebugUsagePatternCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("일간", "주간", "월간")

    val pillLabels = remember(selectedTab) {
        when (selectedTab) {
            0 -> debugBuildDailyPillLabels()
            1 -> debugBuildWeeklyPillLabels()
            else -> debugBuildMonthlyPillLabels()
        }
    }
    var pillIndex by remember(selectedTab) {
        mutableIntStateOf(
            when (selectedTab) {
                0 -> (debugBuildDailyPillLabels().size - 1).coerceAtLeast(0)
                else -> 3
            },
        )
    }

    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var divideByDays by remember { mutableIntStateOf(0) }
    var rawTwelve by remember { mutableStateOf(List(12) { 0L }) }

    LaunchedEffect(selectedTab, pillIndex) {
        val pi = pillIndex.coerceIn(0, pillLabels.lastIndex.coerceAtLeast(0))
        val (s, e) = when (selectedTab) {
            0 -> debugDailyRangeForDayIndex(pi)
            1 -> debugWeeklyRangeForPillIndex(pi)
            else -> debugMonthlyRangeForPillIndex(pi)
        }
        val d = debugDivideByDaysForRange(s, e, selectedTab)
        startMs = s
        endMs = e
        divideByDays = d
        val twelve = withContext(Dispatchers.IO) {
            StatisticsData.loadTimeSlot12Minutes(
                context = context,
                startMs = s,
                endMs = e,
                divideByDays = d,
                allowedPackages = null,
            )
        }
        rawTwelve = if (twelve.size >= 12) twelve.take(12) else twelve + List(12 - twelve.size) { 0L }
    }

    val tabName = tabLabels.getOrElse(selectedTab) { "?" }
    val tsFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA) }
    val rawLine = rawTwelve.joinToString(",")

    val paddedTwelve = if (rawTwelve.size >= 12) rawTwelve.take(12) else rawTwelve + List(12 - rawTwelve.size) { 0L }
    val timeSlotMaxMinutes = 120L
    val normalized = paddedTwelve.map { (it.toFloat() / timeSlotMaxMinutes.toFloat()).coerceIn(0f, 1f) }
    val maxIdx = paddedTwelve.indices.maxByOrNull { paddedTwelve[it] }?.takeIf { paddedTwelve[it] > 0 } ?: -1

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 26.dp),
    ) {
        Text(
            text = "사용 패턴 (DEBUG)",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AptoxSegmentedTab(
            items = tabLabels,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (pillLabels.isNotEmpty()) {
            DebugStatsPeriodPillsRow(
                labels = pillLabels,
                selectedIndex = pillIndex.coerceIn(0, pillLabels.lastIndex),
                onSelectIndex = { pillIndex = it },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        TimeSlotBarChartComponent(
            values = normalized,
            maxValueIdx = maxIdx,
            showSpeechBubble = false,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "탭: $tabName",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "${tsFmt.format(startMs)} ~ ${tsFmt.format(endMs)}",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "divideByDays=$divideByDays",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "12슬롯(분): $rawLine",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
    }
}
