package com.aptox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aptox.app.usage.UsageStatsLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private val DebugCategoryCardListSpacing = 12.dp

/** [StatsStackedBarAndAppList]와 동일 팔레트 (Figma 925:7299) */
private val DebugCategoryColors = mapOf(
    "OTT" to Color(0xFFEBCFFF),
    "SNS" to Color(0xFFFFC34B),
    "게임" to Color(0xFF818CFF),
    "쇼핑" to Color(0xFFA2A2A2),
    "웹툰" to Color(0xFF88C9FF),
    "주식,코인" to Color(0xFF3D9E5D),
    "기타" to Color(0xFFBDBDBD),
)

/**
 * 통계 화면 전용 DEBUG — 카테고리 통계 탭(일/주/월) 프로토타입.
 * [StatisticsScreen]에서 `SHOW_DEBUG_MENU && DEBUG`일 때만 호출할 것.
 */
@Composable
fun StatisticsDebugCategoryStatsCard(
    statisticsViewModel: StatisticsViewModel,
    modifier: Modifier = Modifier,
) {
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

    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(0L) }
    var appList by remember { mutableStateOf<List<StatisticsData.StatsAppItem>>(emptyList()) }
    var categoryTotalsFromDb by remember { mutableStateOf<Map<String, Long>?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var categoryDebugLine by remember { mutableStateOf("") }

    LaunchedEffect(mainTab, pillIndex) {
        val pi = pillIndex.coerceIn(0, pillLabels.lastIndex.coerceAtLeast(0))
        val (s, e) = when (mainTab) {
            0 -> debugDailyRangeForDayIndex(pi)
            1 -> debugWeeklyRangeForPillIndex(pi)
            else -> debugMonthlyRangeForPillIndex(pi)
        }
        startMs = s
        endMs = e
        statisticsViewModel.syncCategoryStatsPeriodForRange(s, e)
        val startDate = UsageStatsLocalRepository.msToYyyyMmDd(s)
        val endDate = UsageStatsLocalRepository.msToYyyyMmDd(e)
        val localRepo = UsageStatsLocalRepository(context)
        categoryTotalsFromDb = withContext(Dispatchers.IO) {
            if (localRepo.hasCategoryDataForDateRangeBlocking(startDate, endDate)) {
                localRepo.getCategoryTotalsForDateRangeBlocking(startDate, endDate)
            } else {
                null
            }
        }
        appList = withContext(Dispatchers.IO) {
            StatisticsData.loadAppUsageForAllowedCategories(context, s, e)
        }
        val dbTotals = categoryTotalsFromDb
        val usageByCategory = if (dbTotals != null && dbTotals.values.sum() > 0L) {
            dbTotals
        } else {
            appList
                .filter { it.categoryTag != null }
                .groupBy { it.categoryTag!! }
                .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        }
        categoryDebugLine = usageByCategory.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { (cat, ms) ->
                "${cat}=${ms / 60_000}분"
            }
        selectedCategory = null
    }

    val segments = remember(appList, categoryTotalsFromDb) {
        val dbTotals = categoryTotalsFromDb
        val usageByCategory = if (dbTotals != null && dbTotals.values.sum() > 0L) {
            dbTotals
        } else {
            appList
                .filter { it.categoryTag != null }
                .groupBy { it.categoryTag!! }
                .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        }
        val total = usageByCategory.values.sum()
        if (total == 0L) emptyList()
        else {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / total * 100) }
        }
    }

    val displaySegments = remember(segments) {
        segments.filter { it.second.toInt() > 0 }
    }

    val filteredApps = remember(appList, selectedCategory) {
        if (selectedCategory == null) appList
        else appList.filter { it.categoryTag == selectedCategory }
    }

    val tabName = tabLabels.getOrElse(mainTab) { "?" }
    val tsFmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "카테고리 통계 (DEBUG)",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
        )
        AptoxSegmentedTab(
            items = tabLabels,
            selectedIndex = mainTab,
            onTabSelected = { mainTab = it },
        )
        if (pillLabels.isNotEmpty()) {
            DebugStatsPeriodPillsRow(
                labels = pillLabels,
                selectedIndex = pillIndex.coerceIn(0, pillLabels.lastIndex),
                onSelectIndex = { pillIndex = it },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFD9D9D9)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                displaySegments.forEach { (label, pct) ->
                    val interactionSource = remember(label) { MutableInteractionSource() }
                    var isPressed by remember { mutableStateOf(false) }
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect { interaction ->
                            when (interaction) {
                                is PressInteraction.Press -> isPressed = true
                                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                                else -> {}
                            }
                        }
                    }
                    val baseColor = DebugCategoryColors[label] ?: Color.Gray
                    val displayColor = if (isPressed) lerp(baseColor, Color.Black, 0.08f) else baseColor
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((pct / 100f).coerceAtLeast(0.02f))
                            .background(displayColor)
                            .clickable(interactionSource = interactionSource, indication = null) {
                                selectedCategory = if (selectedCategory == label) null else label
                            },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            val leftLegend = displaySegments.filterIndexed { index, _ -> index % 2 == 0 }
            val rightLegend = displaySegments.filterIndexed { index, _ -> index % 2 == 1 }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                leftLegend.forEach { (label, pct) ->
                    DebugCategoryLegendRow(
                        label = label,
                        pct = pct,
                        isLeftColumn = true,
                        selectedCategory = selectedCategory,
                        onClick = { selectedCategory = if (selectedCategory == label) null else label },
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rightLegend.forEach { (label, pct) ->
                    DebugCategoryLegendRow(
                        label = label,
                        pct = pct,
                        isLeftColumn = false,
                        selectedCategory = selectedCategory,
                        onClick = { selectedCategory = if (selectedCategory == label) null else label },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(
                    color = AppColors.Grey400,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(DebugCategoryCardListSpacing)) {
            filteredApps.take(6).forEach { app ->
                DebugStatsAppRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    categoryTag = app.categoryTag,
                    onClick = { statisticsViewModel.onAppItemClick(app.packageName) },
                )
            }
        }

        Text(
            text = "탭: $tabName",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "${tsFmt.format(startMs)} ~ ${tsFmt.format(endMs)}",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
        Text(
            text = "카테고리(분): $categoryDebugLine",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
    }
}

@Composable
private fun DebugCategoryLegendRow(
    label: String,
    pct: Float,
    isLeftColumn: Boolean,
    selectedCategory: String?,
    onClick: () -> Unit,
) {
    val interactionSource = remember(label) { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                else -> {}
            }
        }
    }
    val baseColor = DebugCategoryColors[label] ?: Color.Gray
    val displayColor = if (isPressed) lerp(baseColor, Color.Black, 0.08f) else baseColor
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isLeftColumn) 8.dp else 22.dp, end = if (isLeftColumn) 22.dp else 8.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(displayColor),
            )
            Text(
                text = label,
                style = AppTypography.Caption2.copy(
                    color = if (selectedCategory == label) AppColors.TextHighlight else AppColors.TextCaption,
                ),
            )
        }
        Text(
            text = "${pct.toInt()}%",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
        )
    }
}

@Composable
private fun DebugStatsAppRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    categoryTag: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember(packageName) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBox(
                appIcon = appIcon,
                size = 56.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    categoryTag?.let { tag ->
                        CategoryTag(tag = tag)
                    }
                }
                Text(
                    text = name,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = "$usageMinutes 사용",
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
        )
    }
}

