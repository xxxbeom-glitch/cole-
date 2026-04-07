package com.aptox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aptox.app.usage.UsageStatsLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * 통계 화면 전용 DEBUG — "총 사용시간" 프로토타입.
 * [StatisticsScreen]에서 `SHOW_DEBUG_MENU && DEBUG`일 때만 호출할 것.
 */
@Composable
fun StatisticsDebugTotalUsageTimeCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var mainTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("일간", "주간", "월간")

    val pillLabels = remember(mainTab) {
        when (mainTab) {
            0 -> debugBuildDailyPillLabels()
            1 -> debugBuildWeeklyPillLabels()
            else -> debugBuildMonthlyPillLabels()
        }
    }
    var pillIndex by remember(mainTab) {
        mutableIntStateOf(
            when (mainTab) {
                0 -> (debugBuildDailyPillLabels().size - 1).coerceAtLeast(0)
                else -> 3
            },
        )
    }

    var barValues by remember { mutableStateOf<List<Long>>(emptyList()) }
    var barLabels by remember { mutableStateOf<List<String>>(emptyList()) }
    var barSelectIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(mainTab, pillIndex) {
        val pi = pillIndex.coerceIn(0, pillLabels.lastIndex.coerceAtLeast(0))
        val (loadedValues, loadedLabels) = withContext(Dispatchers.IO) {
            when (mainTab) {
                0 -> {
                    val (s, e, _) = StatisticsData.getWeekRange(0)
                    val full = StatisticsData.loadDayOfWeekMinutes(context, s, e, null)
                    val n = debugTodayIndexFromMonday() + 1
                    val vals = full.take(n)
                    val labels = (0 until n).map { dayIdx ->
                        val c = Calendar.getInstance()
                        c.timeInMillis = s
                        c.add(Calendar.DAY_OF_YEAR, dayIdx)
                        formatDebugDailyPillLabel(c)
                    }
                    vals to labels
                }
                1 -> {
                    val (ws, we, _) = StatisticsData.getWeekRange(pi - 3)
                    val vals = StatisticsData.loadDayOfWeekMinutes(context, ws, we, null)
                    vals to dayLabelsKorean
                }
                else -> {
                    val (ms, me) = debugMonthlyRangeForPillIndex(pi)
                    val full = StatisticsData.loadDayOfMonthMinutes(context, ms, me, null)
                    val mo = pi - 3
                    val dim = Calendar.getInstance().apply { timeInMillis = ms }.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val nShow = if (mo == 0) {
                        Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                    } else {
                        dim
                    }
                    val vals = full.take(nShow)
                    val labels = (1..nShow).map { "${it}" }
                    vals to labels
                }
            }
        }
        barValues = loadedValues
        barLabels = loadedLabels
        val last = (loadedValues.size - 1).coerceAtLeast(0)
        barSelectIndex = when (mainTab) {
            0 -> pi.coerceIn(0, last)
            1 -> if (pi == pillLabels.lastIndex) debugTodayIndexFromMonday().coerceIn(0, last) else 0
            else -> {
                val mo = pi - 3
                if (mo == 0) {
                    (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, last)
                } else {
                    0
                }
            }
        }
    }

    val tabName = tabLabels.getOrElse(mainTab) { "?" }
    val selIdx = barSelectIndex.coerceIn(0, (barValues.size - 1).coerceAtLeast(0))
    val piSafe = pillIndex.coerceIn(0, pillLabels.lastIndex.coerceAtLeast(0))
    val totalMinutes = barValues.getOrElse(selIdx) { 0L }
    val (rangeStart, rangeEnd) = debugTotalUsageSelectedDayRange(mainTab, piSafe, selIdx)
    val tsFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 26.dp),
    ) {
        Text(
            text = "총 사용시간 (DEBUG)",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AptoxSegmentedTab(
            items = tabLabels,
            selectedIndex = mainTab,
            onTabSelected = { mainTab = it },
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (pillLabels.isNotEmpty()) {
            DebugStatsPeriodPillsRow(
                labels = pillLabels,
                selectedIndex = piSafe,
                onSelectIndex = { pillIndex = it },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (barValues.isNotEmpty()) {
            DebugTotalUsageSelectableBarChart(
                valuesMinutes = barValues,
                xLabels = barLabels,
                selectedIndex = selIdx,
                onBarSelected = { idx ->
                    barSelectIndex = idx
                    if (mainTab == 0) pillIndex = idx
                },
                scrollable = mainTab == 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "탭: $tabName",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "${tsFmt.format(rangeStart)} ~ ${tsFmt.format(rangeEnd)}",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "총 사용시간: ${totalMinutes}분",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
    }
}

private val dayLabelsKorean = listOf("월", "화", "수", "목", "금", "토", "일")

/** 선택한 막대(일) 기준 [startMs, endMs] (표시용). */
private fun debugTotalUsageSelectedDayRange(mainTab: Int, pillIndex: Int, barIdx: Int): Pair<Long, Long> {
    return when (mainTab) {
        0 -> debugDailyRangeForDayIndex(pillIndex)
        1 -> {
            val (ws, _, _) = StatisticsData.getWeekRange(pillIndex - 3)
            val c = Calendar.getInstance()
            c.timeInMillis = ws
            c.add(Calendar.DAY_OF_YEAR, barIdx.coerceIn(0, 6))
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val start = c.timeInMillis
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            val endOfDay = c.timeInMillis
            val today = System.currentTimeMillis()
            val startKey = UsageStatsLocalRepository.msToYyyyMmDd(start)
            val todayKey = UsageStatsLocalRepository.msToYyyyMmDd(today)
            val end = if (startKey == todayKey) min(today, endOfDay) else endOfDay
            start to end
        }
        else -> {
            val (ms, me) = debugMonthlyRangeForPillIndex(pillIndex)
            val c = Calendar.getInstance()
            c.timeInMillis = ms
            val dim = c.getActualMaximum(Calendar.DAY_OF_MONTH)
            val day = (barIdx + 1).coerceIn(1, dim)
            c.set(Calendar.DAY_OF_MONTH, day)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            val start = c.timeInMillis
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            c.set(Calendar.MILLISECOND, 999)
            val endOfDay = c.timeInMillis
            val today = System.currentTimeMillis()
            val startKey = UsageStatsLocalRepository.msToYyyyMmDd(start)
            val todayKey = UsageStatsLocalRepository.msToYyyyMmDd(today)
            val end = if (startKey == todayKey) min(today, endOfDay) else endOfDay
            start to min(end, me)
        }
    }
}

/** 기간별 사용량 차트와 동일: Y 0~16H, 막대 보라=ChartTrackFill, 회색=Grey350 */
@Composable
private fun DebugTotalUsageSelectableBarChart(
    valuesMinutes: List<Long>,
    xLabels: List<String>,
    selectedIndex: Int,
    onBarSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    fixedBarWidth: Dp = 28.dp,
) {
    val periodChartFixedYTicks = listOf(0L, 240L, 480L, 720L, 960L)
    val periodChartFixedMaxMinutes = 960L
    val barChartHeight = 126.dp
    val chartVerticalPadding = 10.dp
    val totalChartHeight = barChartHeight + chartVerticalPadding * 2
    val barWidth = if (scrollable) fixedBarWidth else 26.dp
    val barCornerRadius = 2.dp
    val chartYAxisWidth = 26.dp
    val chartYAxisToChartGap = 6.dp
    val chartToXLabelsGap = 10.dp
    val chartAxisTextStyle = AppTypography.Caption1.copy(color = AppColors.TextCaption)
    val hScroll = rememberScrollState()

    fun formatY(minutes: Long): String = when {
        minutes <= 0 -> "0H"
        minutes >= 60_000 -> "${(minutes + 30_000) / 60_000}천H"
        minutes >= 60 -> "${minutes / 60}H"
        else -> "$minutes"
    }

    val n = valuesMinutes.size.coerceAtLeast(1)
    val normalized = valuesMinutes.map { (it.toFloat() / periodChartFixedMaxMinutes.toFloat()).coerceIn(0f, 1f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            val density = LocalDensity.current
            val ticks = periodChartFixedYTicks.asReversed()
            val halfLabelDp = with(density) {
                (chartAxisTextStyle.lineHeight.toPx() / 2f).toDp()
            }
            val denom = ticks.lastIndex.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .width(chartYAxisWidth)
                    .height(totalChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f)
                    .padding(vertical = chartVerticalPadding),
            ) {
                ticks.forEachIndexed { index, tick ->
                    val fraction = index / denom.toFloat()
                    Text(
                        text = formatY(tick),
                        style = chartAxisTextStyle,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = barChartHeight * fraction - halfLabelDp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(chartYAxisToChartGap))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(totalChartHeight)
                    .padding(vertical = chartVerticalPadding),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                    for (i in 0..4) {
                        val y = size.height * (i / 4f)
                        drawLine(
                            color = AppColors.Grey450.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                if (scrollable) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(hScroll),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        repeat(n) { idx ->
                            val value = normalized.getOrElse(idx) { 0f }
                            val isSelected = idx == selectedIndex
                            Column(
                                modifier = Modifier
                                    .width(barWidth)
                                    .fillMaxHeight()
                                    .clickable(onClick = { onBarSelected(idx) }),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(value)
                                            .clip(RoundedCornerShape(barCornerRadius))
                                            .background(
                                                if (isSelected) AppColors.ChartTrackFill
                                                else AppColors.Grey350,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        repeat(n) { idx ->
                            val value = normalized.getOrElse(idx) { 0f }
                            val isSelected = idx == selectedIndex
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(onClick = { onBarSelected(idx) }),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(barWidth)
                                            .fillMaxHeight(value)
                                            .clip(RoundedCornerShape(barCornerRadius))
                                            .background(
                                                if (isSelected) AppColors.ChartTrackFill
                                                else AppColors.Grey350,
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(chartToXLabelsGap))
        if (scrollable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = chartYAxisWidth + chartYAxisToChartGap)
                    .alpha(0.8f)
                    .horizontalScroll(hScroll),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                repeat(n) { idx ->
                    Text(
                        text = xLabels.getOrElse(idx) { "" },
                        style = chartAxisTextStyle,
                        modifier = Modifier
                            .width(fixedBarWidth)
                            .clickable(onClick = { onBarSelected(idx) }),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = chartYAxisWidth + chartYAxisToChartGap)
                    .alpha(0.8f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                repeat(n) { idx ->
                    Text(
                        text = xLabels.getOrElse(idx) { "" },
                        style = chartAxisTextStyle,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = { onBarSelected(idx) }),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
