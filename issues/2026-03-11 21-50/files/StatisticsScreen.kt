package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 통계 카드 도움말 바텀시트 타입 (Figma 932-8989, 932-8868, 932-8974) */
private enum class StatsHelpType(val title: String, val body: String) {
    DATE_CHART(
        "기간별 사용량",
        "주간은 요일별, 월간은 1~12월, 연간은 최대 6년 기준으로 앱 사용량을 보여줘요. 오른쪽 화살표로 과거 기간을 선택할 수 있어요.",
    ),
    CATEGORY(
        "카테고리 통계",
        "앱을 카테고리별로 묶어 사용 비율을 보여줘요. 상단 막대와 범례를 탭하면 해당 카테고리 앱만 필터할 수 있어요.",
    ),
    RESTRICTION(
        "제한 앱 분석",
        "시간 지정 제한과 일일 사용량 제한으로 설정한 앱의 사용 현황을 확인할 수 있어요. 제한을 잘 지킨 앱과 사용량을 한눈에 볼 수 있어요.",
    ),
}

/** 통계 카드 공통 패딩(모든 카드 동일) */
private val StatsCardPadding = 16.dp
/** 통계 카드 내부 섹션 간 간격 (제목-차트-리스트 등) */
private val StatsCardContentSpacing = 16.dp
/** 카드 내 리스트 항목 간 간격 */
private val StatsCardListItemSpacing = 12.dp

/**
 * 통계 화면 AN-01 (Figma 919-3517)
 * - 탭: 오늘/주간/월간/연간
 * - 인사이트 카드 (연속 달성일, 달성율, 유지율)
 * - 날짜 범위 선택 + 요일별 막대 차트
 * - 스택 바 (카테고리 비율) + 최다 앱 리스트
 * - 제한 방식 필터 (시간 지정/일일) + 제한 앱 리스트
 * - 그룹 막대 차트 (전주 vs 이번주)
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        if (StatisticsData.hasUsageAccess(context)) {
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "usage_stats_initial_sync",
                androidx.work.ExistingWorkPolicy.KEEP,
                androidx.work.OneTimeWorkRequestBuilder<com.cole.app.usage.UsageStatsSyncWorker>()
                    .setInputData(androidx.work.workDataOf(com.cole.app.usage.UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
                    .build(),
            )
        }
    }
    var selectedTab by remember { mutableIntStateOf(0) } // 기본 주간
    val tabLabels = listOf("주간", "월간", "연간")
    val tabEnum = when (selectedTab) {
        0 -> StatisticsData.Tab.WEEKLY
        1 -> StatisticsData.Tab.MONTHLY
        else -> StatisticsData.Tab.YEARLY
    }

    // 카드별 독립적인 주/연도/월 네비게이션
    var weekOffsetDateChart by remember { mutableIntStateOf(0) }
    var monthOffsetDateChart by remember { mutableIntStateOf(0) } // 월간: 0=이번 달
    var yearOffsetDateChart by remember { mutableIntStateOf(0) }
    var weekOffsetCategory by remember { mutableIntStateOf(0) }
    var monthOffsetCategory by remember { mutableIntStateOf(0) } // 월간: 0=이번 달
    var yearOffsetCategory by remember { mutableIntStateOf(0) }
    var weekOffsetRestriction by remember { mutableIntStateOf(0) }
    var monthOffsetRestriction by remember { mutableIntStateOf(0) } // 월간: 0=이번 달
    var yearOffsetRestriction by remember { mutableIntStateOf(0) }
    var weekOffsetComparison by remember { mutableIntStateOf(-1) } // 기본: 지난주
    var monthOffsetComparison by remember { mutableIntStateOf(0) }  // 0=이번 달
    var yearOffsetComparison by remember { mutableIntStateOf(0) }

    val restrictedApps = remember { AppRestrictionRepository(context).getAll() }
    var restrictionFilter by remember { mutableIntStateOf(0) } // 0=시간 지정, 1=일일
    val timeSpecifiedApps = restrictedApps.filter { it.blockUntilMs > 0 }
    val dailyLimitApps = restrictedApps.filter { it.blockUntilMs == 0L }
    var showHelpSheet by remember { mutableStateOf<StatsHelpType?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ColeSegmentedTab(
            items = tabLabels,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )

        StatsInsightCard(tabEnum = tabEnum)
        StatsDateChartSection(
            tabEnum = tabEnum,
            weekOffset = weekOffsetDateChart,
            onWeekChange = { weekOffsetDateChart = it },
            monthOffset = monthOffsetDateChart,
            onMonthChange = { monthOffsetDateChart = it },
            yearOffset = yearOffsetDateChart,
            onYearChange = { yearOffsetDateChart = it },
            onInfoClick = { showHelpSheet = StatsHelpType.DATE_CHART },
        )
        StatsStackedBarAndAppList(
            tabEnum = tabEnum,
            weekOffset = weekOffsetCategory,
            onWeekChange = { weekOffsetCategory = it },
            monthOffset = monthOffsetCategory,
            onMonthChange = { monthOffsetCategory = it },
            yearOffset = yearOffsetCategory,
            onYearChange = { yearOffsetCategory = it },
            onInfoClick = { showHelpSheet = StatsHelpType.CATEGORY },
        )
        StatsRestrictionSection(
            tabEnum = tabEnum,
            weekOffset = weekOffsetRestriction,
            onWeekChange = { weekOffsetRestriction = it },
            monthOffset = monthOffsetRestriction,
            onMonthChange = { monthOffsetRestriction = it },
            yearOffset = yearOffsetRestriction,
            onYearChange = { yearOffsetRestriction = it },
            filterIndex = restrictionFilter,
            onFilterChange = { restrictionFilter = it },
            timeSpecifiedApps = timeSpecifiedApps,
            dailyLimitApps = dailyLimitApps,
            onInfoClick = { showHelpSheet = StatsHelpType.RESTRICTION },
        )
        StatsTimeSlotSection(
            tabEnum = tabEnum,
            weekOffset = weekOffsetComparison,
            onWeekChange = { weekOffsetComparison = it },
            monthOffset = monthOffsetComparison,
            onMonthChange = { monthOffsetComparison = it },
            yearOffset = yearOffsetComparison,
            onYearChange = { yearOffsetComparison = it },
        )
        // 맨 아래 스크롤 시 바텀바에 가려지지 않도록 여백 추가
        Spacer(modifier = Modifier.height(100.dp))
    }

    showHelpSheet?.let { helpType ->
        StatsCardHelpBottomSheet(
            title = helpType.title,
            body = helpType.body,
            onDismiss = { showHelpSheet = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsCardHelpBottomSheet(
    title: String,
    body: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                text = title,
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = body,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
            Spacer(modifier = Modifier.height(36.dp))
            ColePrimaryButton(
                text = "확인",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Brief 카드 탭별 제목/본문/통계 라벨 */
private val BriefContentByTab = mapOf(
    StatisticsData.Tab.WEEKLY to Triple(
        "지난주와 비슷한 한 주예요",
        "지난주와 비슷한 패턴이에요. 꾸준히 유지하고 있다는 것 자체가 대단해요. 한 가지만 더 개선한다면 저녁 시간대 사용을 줄여보세요.",
        "연속 달성일" to "56일",
    ),
    StatisticsData.Tab.MONTHLY to Triple(
        "지난달과 비슷한 한 달이에요",
        "지난달과 비슷한 패턴이에요. 꾸준히 유지하고 있다는 것 자체가 대단해요. 한 가지만 더 개선한다면 월 중순 사용을 줄여보세요.",
        "연속 달성주" to "8주",
    ),
    StatisticsData.Tab.YEARLY to Triple(
        "지난해와 비슷한 한 해예요",
        "지난해와 비슷한 패턴이에요. 꾸준히 유지하고 있다는 것 자체가 대단해요. 한 가지만 더 개선한다면 분기별 사용을 줄여보세요.",
        "연속 달성달" to "6달",
    ),
)

/**
 * Figma 926:8043 Brief 카드
 * - 카드: 흰 배경, primary-300 테두리
 * - 상단: Brief + 제목 (gap 2dp), 본문 (gap 12dp)
 * - 하단 통계 박스: Grey100 배경, 연속 달성일/주/달·달성율·유지율
 */
@Composable
private fun StatsInsightCard(
    tabEnum: StatisticsData.Tab = StatisticsData.Tab.WEEKLY,
    modifier: Modifier = Modifier,
) {
    val content = BriefContentByTab[tabEnum] ?: BriefContentByTab[StatisticsData.Tab.WEEKLY]!!
    val (title, body, statPair) = content
    val (stat1Label, stat1Value) = statPair
    val effectiveStat1Value = when {
        tabEnum == StatisticsData.Tab.WEEKLY && DebugTestSettings.debugWeeklyChallengeDays != null ->
            "${DebugTestSettings.debugWeeklyChallengeDays}일"
        else -> stat1Value
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .border(1.dp, AppColors.Primary300, RoundedCornerShape(12.dp))
            .padding(top = 26.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(StatsCardContentSpacing),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(StatsCardListItemSpacing),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Brief",
                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                )
                Text(
                    text = title,
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                )
            }
            Text(
                text = body,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
        }
        StatsInsightStatBox(stat1Label = stat1Label, stat1Value = effectiveStat1Value)
    }
}

/** Figma 926:8043 Brief 카드 세로 구분선 에셋 */
@Composable
private fun IcoStatsBriefDivider(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_stats_brief_divider),
        contentDescription = null,
        modifier = modifier.size(1.dp, 16.dp),
        tint = Color.Unspecified,
    )
}

/** Figma 926:8095 — Grey100 배경, 83dp 높이, 구분선 */
@Composable
private fun StatsInsightStatBox(
    stat1Label: String = "연속 달성일",
    stat1Value: String = "56일",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(83.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.Grey100)
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(6.dp))
            .padding(horizontal = 0.dp, vertical = StatsCardContentSpacing),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatsInsightStatItem(label = stat1Label, value = stat1Value)
        IcoStatsBriefDivider()
        StatsInsightStatItem(label = "달성율", value = "65%")
        IcoStatsBriefDivider()
        StatsInsightStatItem(label = "유지율", value = "50%")
    }
}

@Composable
private fun RowScope.StatsInsightStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .weight(1f)
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Text(
            text = label,
            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = AppTypography.HeadingH3.copy(color = AppColors.Primary300),
        )
    }
}

/**
 * Figma 919:3520 / 919:3764 / 919:3830 — 기간별 사용량 카드
 * 주간: 요일별(월~일). 월간: 1일~31일(가로스크롤). 연간: 최대 6년(좌측정렬)
 */
@Composable
private fun StatsDateChartSection(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    yearOffset: Int,
    onYearChange: (Int) -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var dayMinutes by remember { mutableStateOf<List<Long>>(List(7) { 0L }) }
    var dayOfMonthMinutes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var yearMinutes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var yearLabels by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset) {
        withContext(Dispatchers.IO) {
            when (tabEnum) {
                StatisticsData.Tab.WEEKLY -> {
                    val (s, e, _) = StatisticsData.getWeekRange(weekOffset)
                    dayMinutes = StatisticsData.loadDayOfWeekMinutes(context, s, e)
                }
                StatisticsData.Tab.MONTHLY -> {
                    val (s, e, _) = StatisticsData.getSingleMonthRange(monthOffset)
                    dayOfMonthMinutes = StatisticsData.loadDayOfMonthMinutes(context, s, e)
                }
                StatisticsData.Tab.YEARLY -> {
                    val (ranges, labels) = StatisticsData.getYearRanges(yearOffset)
                    yearLabels = labels
                    yearMinutes = StatisticsData.loadYearsMinutes(context, ranges)
                }
                else -> {}
            }
        }
    }

    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset) {
        val label = formatPeriodLabel(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, true, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, true, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, true, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatsCardTitleRow(
            title = "기간별 사용량",
            titleIconSpacing = 4.dp,
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            onInfoClick = onInfoClick,
            showPeriodSelector = tabEnum != StatisticsData.Tab.YEARLY,
        )
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> DayOfWeekBarChart(
                values = dayMinutes,
                isCurrentPeriod = weekOffset == 0,
            )
            StatisticsData.Tab.MONTHLY -> MonthDailyBarChart(
                values = dayOfMonthMinutes,
                isCurrentMonth = monthOffset == 0,
            )
            StatisticsData.Tab.YEARLY -> {
                val minSize = minOf(yearMinutes.size, yearLabels.size)
                val safeMinutes = yearMinutes.take(minSize)
                val safeYearLabels = yearLabels.take(minSize)
                val indicesWithData = safeMinutes.indices.filter { safeMinutes.getOrElse(it) { 0L } > 0L }
                val displayValues = if (indicesWithData.isEmpty()) safeMinutes else indicesWithData.map { safeMinutes.getOrElse(it) { 0L } }
                val displayLabels = if (indicesWithData.isEmpty()) safeYearLabels else indicesWithData.map { safeYearLabels.getOrElse(it) { "" } }
                YearBarChart(
                    values = displayValues,
                    labels = displayLabels,
                    isCurrentYear = yearOffset == 0,
                )
            }
            else -> {}
        }
    }
}

private data class NavState(
    val text: String,
    val canPrev: Boolean,
    val canNext: Boolean,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
)

/** 우측 날짜/기간 표기: 주간="이번 주"/"N주 전", 월간="이번 달"/"N월", 연간="2025년" 등 */
private fun formatPeriodLabel(tabEnum: StatisticsData.Tab, weekOffset: Int, monthOffset: Int, yearOffset: Int): String = when (tabEnum) {
    StatisticsData.Tab.WEEKLY -> when (weekOffset) {
        0 -> "이번 주"
        else -> "${-weekOffset}주 전"
    }
    StatisticsData.Tab.MONTHLY -> {
        val (_, _, label) = StatisticsData.getSingleMonthRange(monthOffset)
        label
    }
    StatisticsData.Tab.YEARLY -> {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + yearOffset
        "${year}년"
    }
    else -> ""
}

/** 카테고리/제한앱 카드용: 월간 탭이면 "3월"/"이번 달" 등 */
private fun formatPeriodLabelMonthAware(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
): String = when (tabEnum) {
    StatisticsData.Tab.WEEKLY -> when (weekOffset) {
        0 -> "이번 주"
        else -> "${-weekOffset}주 전"
    }
    StatisticsData.Tab.MONTHLY -> {
        val (_, _, label) = StatisticsData.getSingleMonthRange(monthOffset)
        label
    }
    StatisticsData.Tab.YEARLY -> {
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + yearOffset
        "${year}년"
    }
    else -> ""
}

/** 카드 제목 + 날짜 범위 Row (스크린샷: 제목 좌측, 날짜+화살표 우측) */
@Composable
private fun StatsCardTitleRow(
    title: String,
    dateRangeText: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    titleIconSpacing: Dp = 2.dp,
    onInfoClick: (() -> Unit)? = null,
    showPeriodSelector: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(titleIconSpacing),
        ) {
            Text(
                text = title,
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(16.dp)
                    .then(if (onInfoClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onInfoClick() } else Modifier),
            ) {
                IcoDisclaimerInfo(modifier = Modifier.size(16.dp))
            }
        }
        if (showPeriodSelector) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoPrev) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPrevClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavLeft(enabled = canGoPrev, size = 22.dp)
            }
            Text(
                text = dateRangeText,
                style = AppTypography.Caption2.copy(color = AppColors.TextPrimary),
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoNext) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNextClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavRight(enabled = canGoNext, size = 22.dp)
            }
        }
        }
    }
}

/** 날짜 범위 Row만 (제목 없는 카드용) */
@Composable
private fun StatsDateRangeRow(
    dateRangeText: String,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoPrev) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onPrevClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavLeft(enabled = canGoPrev, size = 22.dp)
            }
            Text(
                text = dateRangeText,
                style = AppTypography.Caption2.copy(color = AppColors.TextPrimary),
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .then(if (canGoNext) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNextClick() } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                IcoNavRight(enabled = canGoNext, size = 22.dp)
            }
        }
    }
}

/** Figma 919:3520 / 1053:6211 — 기간별 사용량 차트 공통 */
private val DayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private val BarChartHeight = 126.dp
private val BarWidth = 26.dp
private val BarCornerRadius = 2.dp
/** Figma: Y축 레이블 너비 26px */
private val ChartYAxisWidth = 26.dp
/** Figma: Y축~차트 간격 12px (38 - 26) */
private val ChartYAxisToChartGap = 6.dp
/** Figma: X축 레이블 상단 간격 10px */
private val ChartToXLabelsGap = 10.dp
/** 시간대별 사용량: 차트~X축 라벨 간격 */
private val TimeSlotChartToLabelsGap = 12.dp
/** Figma Caption1: 12sp Medium, lineHeight 19sp, TextCaption (#4d4d4d) */
private val ChartAxisTextStyle = AppTypography.Caption1.copy(color = AppColors.TextCaption)

/** 분 단위 값을 차트 Y축 레이블로 포맷 (60분 이상이면 "XH") */
private fun formatChartYLabel(minutes: Long): String = when {
    minutes <= 0 -> "0"
    minutes >= 60 -> "${minutes / 60}H"
    else -> "$minutes"
}

/** 6개 Y축 틱 값 계산 (0 ~ niceMax, 균등 분할) */
private fun computeChartYTicks(maxMinutes: Long): List<Long> {
    val niceMax = (maxMinutes / 60f).let { h -> (kotlin.math.ceil(h / 2) * 2).toInt().coerceAtLeast(1) } * 60
    return (0..5).map { i -> (niceMax * i / 5).toLong() }
}

/** 시간대별 사용량 전용: Y축 0, 1H, 2H (3개 고정) */
private fun computeTimeSlotYTicks(): List<Long> = listOf(0L, 60L, 120L)

/** values: [월,화,수,목,금,토,일] 순. 이번주만 max bar Primary, 과거 주는 모두 Grey350 */
@Composable
private fun DayOfWeekBarChart(
    values: List<Long>,
    isCurrentPeriod: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val padded = if (values.size >= 7) values.take(7) else values + List(7 - values.size) { 0L }
    val maxVal = padded.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val yTicks = computeChartYTicks(maxVal)
    val niceMax = yTicks.last().coerceAtLeast(1L)
    val normalized = padded.map { (it.toFloat() / niceMax).coerceIn(0f, 1f) }
    val highlightIdx = if (isCurrentPeriod) 6 else -1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier
                    .width(ChartYAxisWidth)
                    .height(BarChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                yTicks.asReversed().forEach { tick ->
                    Text(
                        text = formatChartYLabel(tick),
                        style = ChartAxisTextStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(BarChartHeight)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..5) {
                        val y = size.height * (i / 5f)
                        drawLine(
                            color = AppColors.Grey450.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    normalized.forEachIndexed { index, value ->
                        val isHighlight = isCurrentPeriod && (index == highlightIdx && value > 0f)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(BarWidth)
                                        .fillMaxHeight(value)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(
                                            if (isHighlight) AppColors.ChartTrackFill
                                            else AppColors.Grey350
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(ChartToXLabelsGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ChartYAxisWidth + ChartYAxisToChartGap)
                .alpha(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DayLabels.forEach { label ->
                Text(
                    text = label,
                    style = ChartAxisTextStyle,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 월간: 1일~31일, 주간과 동일 Y축·막대 스타일. 가로 스크롤. 이번 달만 오늘 Primary */
private val MonthDailySlotWidth = 24.dp

@Composable
private fun MonthDailyBarChart(
    values: List<Long>,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier,
) {
    val padded = values
    val daysInMonth = padded.size.coerceAtLeast(1)
    val maxVal = padded.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val yTicks = computeChartYTicks(maxVal)
    val niceMax = yTicks.last().coerceAtLeast(1L)
    val normalized = padded.map { (it.toFloat() / niceMax).coerceIn(0f, 1f) }
    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    val highlightIdx = if (isCurrentMonth && todayDay <= daysInMonth) todayDay - 1 else -1
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            // Y축: 고정 (스크롤 안 됨)
            Column(
                modifier = Modifier
                    .width(ChartYAxisWidth)
                    .height(BarChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                yTicks.asReversed().forEach { tick ->
                    Text(
                        text = formatChartYLabel(tick),
                        style = ChartAxisTextStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            // 막대 + X축 범례만 가로 스크롤
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
            ) {
            Column(modifier = Modifier.width(MonthDailySlotWidth * daysInMonth)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BarChartHeight),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (i in 0..5) {
                            val y = size.height * (i / 5f)
                            drawLine(
                                color = AppColors.Grey450.copy(alpha = 0.6f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                pathEffect = pathEffect,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        normalized.forEachIndexed { index, value ->
                            val isHighlight = isCurrentMonth && (index == highlightIdx && value > 0f)
                            Column(
                                modifier = Modifier.width(MonthDailySlotWidth),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(BarWidth)
                                            .fillMaxHeight(value)
                                            .clip(RoundedCornerShape(BarCornerRadius))
                                            .background(
                                                if (isHighlight) AppColors.ChartTrackFill
                                                else AppColors.Grey350
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(ChartToXLabelsGap))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    (1..daysInMonth).forEach { day ->
                        Text(
                            text = "${day}일",
                            style = ChartAxisTextStyle,
                            modifier = Modifier.width(MonthDailySlotWidth),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            }
        }
    }
}

/** Figma 919:3764 — 연간 월별: 1~12월, 막대 16dp (연간 전용) */
private val MonthBarWidth = 16.dp
private val MonthLabels = (1..12).map { "${it}월" }

@Composable
private fun MonthBarChart(
    values: List<Long>,
    isCurrentYear: Boolean,
    modifier: Modifier = Modifier,
) {
    val padded = if (values.size >= 12) values.take(12) else values + List(12 - values.size) { 0L }
    val maxVal = padded.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val yTicks = computeChartYTicks(maxVal)
    val niceMax = yTicks.last().coerceAtLeast(1L)
    val normalized = padded.map { (it.toFloat() / niceMax).coerceIn(0f, 1f) }
    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    val highlightIdx = if (isCurrentYear) currentMonth else -1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier
                    .width(ChartYAxisWidth)
                    .height(BarChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                yTicks.asReversed().forEach { tick ->
                    Text(text = formatChartYLabel(tick), style = ChartAxisTextStyle)
                }
            }
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(BarChartHeight)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..5) {
                        val y = size.height * (i / 5f)
                        drawLine(
                            color = AppColors.Grey450.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    normalized.forEachIndexed { index, value ->
                        val isHighlight = isCurrentYear && index == highlightIdx && value > 0f
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(MonthBarWidth)
                                        .fillMaxHeight(value)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(
                                            if (isHighlight) AppColors.ChartTrackFill
                                            else AppColors.Grey350
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(ChartToXLabelsGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ChartYAxisWidth + ChartYAxisToChartGap)
                .alpha(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MonthLabels.forEach { label ->
                Text(
                    text = label,
                    style = ChartAxisTextStyle,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Figma 919:3830 — 연간: 최대 6년, 막대 42dp. 6년 미만이면 좌측정렬. 올해만 Primary */
private val YearBarWidth = 42.dp

@Composable
private fun YearBarChart(
    values: List<Long>,
    labels: List<String>,
    isCurrentYear: Boolean,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty() && labels.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(BarChartHeight))
        return
    }
    val count = values.size.coerceAtLeast(1)
    val maxVal = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val yTicks = computeChartYTicks(maxVal)
    val niceMax = yTicks.last().coerceAtLeast(1L)
    val normalized = values.map { (it.toFloat() / niceMax).coerceIn(0f, 1f) }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val displayLabels = labels.take(count).let { l ->
        if (l.size >= count) l else l + (1..(count - l.size)).map { "" }
    }
    val barArrangement = if (count < 6) Arrangement.spacedBy(4.dp, Alignment.Start) else Arrangement.SpaceEvenly

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier
                    .width(ChartYAxisWidth)
                    .height(BarChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                yTicks.asReversed().forEach { tick ->
                    Text(text = formatChartYLabel(tick), style = ChartAxisTextStyle)
                }
            }
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(BarChartHeight)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..5) {
                        val y = size.height * (i / 5f)
                        drawLine(
                            color = AppColors.Grey450.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = barArrangement,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    normalized.forEachIndexed { index, value ->
                        val yearStr = displayLabels.getOrElse(index) { "" }
                        val year = yearStr.toIntOrNull() ?: 0
                        val isHighlight = isCurrentYear && year == currentYear && value > 0f
                        Column(
                            modifier = Modifier.width(YearBarWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier.width(YearBarWidth).fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier.width(YearBarWidth)
                                        .fillMaxHeight(value)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(
                                            if (isHighlight) AppColors.ChartTrackFill
                                            else AppColors.Grey350
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(ChartToXLabelsGap))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ChartYAxisWidth + ChartYAxisToChartGap)
                .alpha(0.8f),
            horizontalArrangement = barArrangement,
        ) {
            displayLabels.take(count).forEach { label ->
                Box(
                    modifier = Modifier.width(YearBarWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = ChartAxisTextStyle,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Figma 925:7299 / 1044 — 카테고리 색상: OTT, SNS, 게임, 쇼핑, 웹툰, 주식,코인, 기타 */
private val CategoryColors = mapOf(
    "OTT" to Color(0xFFEBCFFF),
    "SNS" to Color(0xFFFFC34B),
    "게임" to Color(0xFF818CFF),
    "쇼핑" to Color(0xFFA2A2A2),
    "웹툰" to Color(0xFF88C9FF),
    "주식,코인" to Color(0xFF3D9E5D),
    "기타" to Color(0xFFBDBDBD),
)

/** Figma 925:7299 — 카테고리 통계 디자인용 더미 앱 목록 (usageMs로 OTT36/SNS20/게임19/쇼핑18/웹툰15/주식코인2% 비율) */
private val CategoryStatsDummyApps = listOf(
    StatisticsData.StatsAppItem(
        packageName = "com.instagram.android",
        name = "Instagram",
        usageMinutes = "1,688분",
        sessionCount = "0회",
        isRestricted = true,
        categoryTag = "SNS",
        usageMs = 20_000_000L,
        isWarning = false,
    ),
    StatisticsData.StatsAppItem(
        packageName = "com.netflix.mediaclient",
        name = "넷플릭스",
        usageMinutes = "1,200분",
        sessionCount = "0회",
        isRestricted = false,
        categoryTag = "OTT",
        usageMs = 36_000_000L,
        isWarning = true,
    ),
    StatisticsData.StatsAppItem(
        packageName = "com.ncsoft.lineagew",
        name = "리니지W",
        usageMinutes = "1,001분",
        sessionCount = "0회",
        isRestricted = false,
        categoryTag = "게임",
        usageMs = 19_000_000L,
        isWarning = false,
    ),
    StatisticsData.StatsAppItem(
        packageName = "com.banhala.android",
        name = "에이블리",
        usageMinutes = "880분",
        sessionCount = "0회",
        isRestricted = false,
        categoryTag = "쇼핑",
        usageMs = 18_000_000L,
        isWarning = false,
    ),
    StatisticsData.StatsAppItem(
        packageName = "com.nhn.android.webtoon",
        name = "네이버 웹툰",
        usageMinutes = "726분",
        sessionCount = "0회",
        isRestricted = false,
        categoryTag = "웹툰",
        usageMs = 15_000_000L,
        isWarning = false,
    ),
    StatisticsData.StatsAppItem(
        packageName = "com.shinhan.sbanking",
        name = "신한투자증권",
        usageMinutes = "380분",
        sessionCount = "0회",
        isRestricted = false,
        categoryTag = "주식,코인",
        usageMs = 2_000_000L,
        isWarning = false,
    ),
)

/** Figma 925:7299 — 카테고리 통계. 그래프/범례 클릭 시 해당 카테고리 앱 필터. 기본 전체 */
@Composable
private fun StatsStackedBarAndAppList(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    yearOffset: Int,
    onYearChange: (Int) -> Unit,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset) {
        val label = formatPeriodLabelMonthAware(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, true, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, true, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, true, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }
    // TODO: 실제 데이터 연동 시 LaunchedEffect로 loadAppUsage 사용
    val appList = remember { CategoryStatsDummyApps }
    var selectedCategory by remember { mutableStateOf<String?>(null) } // null = 전체

    // 앱 사용량 기반 카테고리 비율 (많이 쓴 순 지그재그)
    val segments = remember(appList) {
        val usageByCategory = appList
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        val total = usageByCategory.values.sum()
        if (total == 0L) {
            listOf("OTT" to 35f, "SNS" to 20f, "게임" to 19f, "쇼핑" to 18f, "웹툰" to 15f, "주식,코인" to 2f, "기타" to 1f)
        } else {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / total * 100) }
        }
    }

    val filteredApps = remember(appList, selectedCategory) {
        if (selectedCategory == null) appList
        else appList.filter { it.categoryTag == selectedCategory }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatsCardTitleRow(
            title = "카테고리 통계",
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            titleIconSpacing = 4.dp,
            onInfoClick = onInfoClick,
        )

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
                segments.forEach { (label, pct) ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((pct / 100f).coerceAtLeast(0.02f))
                            .background(CategoryColors[label] ?: Color.Gray)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedCategory = if (selectedCategory == label) null else label },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            segments.chunked(3).forEachIndexed { columnIndex, chunk ->
                val isLeft = columnIndex == 0
                Column(
                    modifier = Modifier.width(148.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    chunk.forEach { (label, pct) ->
                        Row(
                            modifier = Modifier
                                .width(148.dp)
                                .padding(start = if (isLeft) 8.dp else 22.dp, end = if (isLeft) 22.dp else 8.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedCategory = if (selectedCategory == label) null else label },
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
                                        .background(CategoryColors[label] ?: Color.Gray),
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

        if (selectedCategory != null) {
            Text(
                text = "$selectedCategory 카테고리 앱",
                style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(StatsCardListItemSpacing)) {
            filteredApps.take(6).forEach { app ->
                StatsAppRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    categoryTag = app.categoryTag,
                    showDangerLabel = app.isRestricted,
                    showWarningLabel = app.isWarning,
                )
            }
        }
    }
}

/** Figma 925:7299 — 앱 row: 아이콘(56dp) + 태그(위) + 앱 이름(아래) + 사용시간 */
@Composable
private fun StatsAppRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    categoryTag: String?,
    showDangerLabel: Boolean,
    showWarningLabel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp),
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
                        StatsCategoryTag(tag = tag)
                    }
                    if (showDangerLabel) {
                        LabelDanger()
                    }
                    if (showWarningLabel) {
                        LabelWarning()
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
            text = usageMinutes,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
        )
    }
}

@Composable
private fun StatsCategoryTag(tag: String, modifier: Modifier = Modifier) {
    val bgColor = CategoryColors[tag] ?: AppColors.Grey350
    val textColor = when (tag) {
        "SNS" -> Color(0xFF553C0A)
        "OTT" -> Color(0xFF55366B)
        "기타" -> Color(0xFF424242)
        else -> Color.White
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = tag,
            style = AppTypography.LabelDanger.copy(color = textColor),
        )
    }
}

/** Figma 925:7519/925:7436 — 제한 앱 분석 디자인용 더미 데이터 */
private val RestrictionDummyTimeSpecified = listOf(
    com.cole.app.model.AppRestriction("com.instagram.android", "Instagram", 60, System.currentTimeMillis()),
    com.cole.app.model.AppRestriction("com.netflix.mediaclient", "넷플릭스", 60, System.currentTimeMillis()),
)
private val RestrictionDummyDailyLimit = listOf(
    com.cole.app.model.AppRestriction("com.instagram.android", "Instagram", 120, 0L),
    com.cole.app.model.AppRestriction("com.netflix.mediaclient", "넷플릭스", 120, 0L),
    com.cole.app.model.AppRestriction("com.google.android.youtube", "유튜브", 120, 0L),
)
private val RestrictionDummyMinutesTimeSpecified = mapOf(
    "com.instagram.android" to 630L,
    "com.netflix.mediaclient" to 630L,
)
private val RestrictionDummyMinutesDailyLimit = mapOf(
    "com.instagram.android" to 150L,
    "com.netflix.mediaclient" to 150L,
    "com.google.android.youtube" to 150L,
)

/** 제한 앱 분석 카드 */
@Composable
private fun StatsRestrictionSection(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    yearOffset: Int,
    onYearChange: (Int) -> Unit,
    filterIndex: Int,
    onFilterChange: (Int) -> Unit,
    timeSpecifiedApps: List<com.cole.app.model.AppRestriction>,
    dailyLimitApps: List<com.cole.app.model.AppRestriction>,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset) {
        val label = formatPeriodLabelMonthAware(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, true, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, true, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, true, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }
    // TODO: 실제 데이터 연동 시 LaunchedEffect로 loadRestrictedMinutesPerApp 사용
    val displayApps = if (filterIndex == 0) RestrictionDummyTimeSpecified else RestrictionDummyDailyLimit
    val restrictedByPkg = if (filterIndex == 0) RestrictionDummyMinutesTimeSpecified else RestrictionDummyMinutesDailyLimit

    val sortedByRestricted = displayApps
        .map { it to (restrictedByPkg[it.packageName] ?: 0L) }
        .sortedByDescending { it.second }
        .map { it.first }

    val topApp = sortedByRestricted.firstOrNull()
    val topMinutes = topApp?.let { restrictedByPkg[it.packageName] ?: 0L } ?: 0L

    val infoMessage = when {
        filterIndex == 0 && topMinutes > 0 -> "지난주엔 ${topApp?.appName ?: "앱"} 을 무려 ${java.text.DecimalFormat("#,###").format(topMinutes)}분이나 사용하지 않으셨네요! 정말 대단하세요."
        filterIndex == 1 && topMinutes > 0 -> "지난주엔 유튜브를 150분만 사용하셨어요. 굉장한 절제력이에요!"
        else -> "제한 앱은 설정에서 수정할 수 있어요"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(top = 26.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatsCardTitleRow(
            title = "제한 앱 분석",
            titleIconSpacing = 4.dp,
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            onInfoClick = onInfoClick,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("시간 지정 제한", "일일 사용량 제한").forEachIndexed { index, label ->
                val selected = filterIndex == index
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) AppColors.Primary50 else Color.Transparent)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onFilterChange(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = if (selected) AppTypography.Caption2.copy(color = AppColors.TextHighlight)
                        else AppTypography.Caption1.copy(color = AppColors.Grey500),
                    )
                }
            }
        }

        if (displayApps.isEmpty()) {
            Text(
                text = "제한 중인 앱이 없어요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                modifier = Modifier.padding(vertical = StatsCardContentSpacing),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(StatsCardListItemSpacing)) {
                sortedByRestricted.forEach { app ->
                    val appIcon = rememberAppIconPainter(app.packageName)
                    val restrictedMinutes = restrictedByPkg[app.packageName] ?: 0L
                    val displayText = "${java.text.DecimalFormat("#,###").format(restrictedMinutes)}분"
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
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
                            Text(
                                text = app.appName,
                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                            )
                        }
                        Text(
                            text = displayText,
                            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                        )
                    }
                }
            }
        }

        ColeInfoBoxCompact(
            text = infoMessage,
            maxLines = 2,
            contentPaddingHorizontal = 16.dp,
            contentPaddingVertical = 18.dp,
        )

        // 비교용: 새 디자인 (border 삭제, Primary50 배경, TextBody 텍스트)
        Text(
            text = "새 디자인",
            style = AppTypography.Caption2.copy(color = AppColors.Grey500),
            modifier = Modifier.padding(top = 8.dp),
        )
        ColeInfoBoxCompactNewDesign(text = infoMessage, maxLines = 2)
    }
}

/** 시간대별 사용량 카드 (Figma 925-7593). 기간별 사용량과 동일 Y축·가로선 스타일 */
@Composable
private fun StatsTimeSlotSection(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    onWeekChange: (Int) -> Unit,
    monthOffset: Int,
    onMonthChange: (Int) -> Unit,
    yearOffset: Int,
    onYearChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset) {
        val label = formatPeriodLabelMonthAware(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, true, weekOffset < -1, { onWeekChange(weekOffset - 1) }, { if (weekOffset < -1) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, true, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, true, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }

    var timeSlotMinutes by remember { mutableStateOf<List<Long>>(List(12) { 0L }) }
    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset) {
        withContext(Dispatchers.IO) {
            val (startMs, endMs, _) = when (tabEnum) {
                StatisticsData.Tab.WEEKLY -> StatisticsData.getWeekRange(weekOffset)
                StatisticsData.Tab.MONTHLY -> StatisticsData.getSingleMonthRange(monthOffset)
                StatisticsData.Tab.YEARLY -> StatisticsData.getMonthRange(yearOffset)
                else -> StatisticsData.getWeekRange(0)
            }
            timeSlotMinutes = StatisticsData.loadTimeSlotMinutes12(context, startMs, endMs)
        }
    }

    val padded = if (timeSlotMinutes.size >= 12) timeSlotMinutes.take(12) else timeSlotMinutes + List(12 - timeSlotMinutes.size) { 0L }
    val maxIdx = padded.indices.maxByOrNull { padded[it] }?.takeIf { padded[it] > 0 } ?: -1
    val yTicks = computeTimeSlotYTicks()
    val niceMax = 120L // 2H 고정
    val normalized = padded.map { (it.toFloat() / niceMax).coerceIn(0f, 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        StatsCardTitleRow(
            title = "시간대별 사용량",
            titleIconSpacing = 4.dp,
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            showPeriodSelector = true,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(TimeSlotChartToLabelsGap),
        ) {
            TimeSlotBarChart(
                values = normalized,
                yTicks = yTicks,
                maxValueIdx = maxIdx,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ChartYAxisWidth + ChartYAxisToChartGap)
                    .alpha(0.8f),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatisticsData.TimeSlot4SectionLabels.forEach { label ->
                    Text(
                        text = label,
                        style = ChartAxisTextStyle,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        ColeInfoBoxCompact(
            text = when (tabEnum) {
                StatisticsData.Tab.WEEKLY -> "지난주와 비슷한 패턴이에요. 꾸준히 유지하고 있다는 것 자체가 대단해요. 한 가지만 더 개선한다면 저녁 시간대 사용을 줄여보세요."
                StatisticsData.Tab.MONTHLY -> "이번 달 시간대별 사용 패턴이에요. 어떤 시간에 가장 많이 쓰는지 확인해보세요."
                StatisticsData.Tab.YEARLY -> "올해 시간대별 사용 패턴이에요. 습관적인 사용 시간대를 확인해보세요."
                else -> "시간대별 사용 패턴을 확인해보세요."
            },
            contentPaddingHorizontal = 16.dp,
            contentPaddingVertical = 18.dp,
        )
    }
}

/** 시간대별 막대 차트. 4구간×3막대, Y축 0/1H/2H 3개, 최대값 막대 Red300 */
private val TimeSlotBarWidth = 16.dp
private val TimeSlotBarGap = 8.dp

@Composable
private fun TimeSlotBarChart(
    values: List<Float>,
    yTicks: List<Long>,
    maxValueIdx: Int,
    modifier: Modifier = Modifier,
) {
    val padded = if (values.size >= 12) values.take(12) else values + List(12 - values.size) { 0f }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier = Modifier
                    .width(ChartYAxisWidth)
                    .height(BarChartHeight)
                    .padding(start = 2.dp)
                    .alpha(0.8f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                yTicks.asReversed().forEach { tick ->
                    Text(
                        text = formatChartYLabel(tick),
                        style = ChartAxisTextStyle,
                    )
                }
            }
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(BarChartHeight)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    for (i in 0..2) {
                        val y = size.height * (i / 2f)
                        drawLine(
                            color = AppColors.Grey450.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = pathEffect,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    (0..3).forEach { section ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(TimeSlotBarGap, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            (0..2).forEach { barInSection ->
                                val idx = section * 3 + barInSection
                                val value = padded.getOrElse(idx) { 0f }
                                val isMax = idx == maxValueIdx && value > 0f
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(TimeSlotBarWidth)
                                            .fillMaxHeight(value)
                                            .clip(RoundedCornerShape(BarCornerRadius))
                                            .background(
                                                if (isMax) AppColors.Red300
                                                else AppColors.Grey350
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 월간/연간 탭용 기존 스타일 콘텐츠 */
@Composable
private fun StatsLegacyContent(
    tabEnum: StatisticsData.Tab,
) {
    var timeSlotMinutes by remember { mutableStateOf<List<Long>>(List(8) { 0L }) }
    var appList by remember { mutableStateOf<List<StatisticsData.StatsAppItem>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(tabEnum) {
        withContext(Dispatchers.IO) {
            val (startMs, endMs) = StatisticsData.getTimeRange(context, tabEnum)
            timeSlotMinutes = StatisticsData.loadTimeSlotMinutes(context, tabEnum)
            appList = StatisticsData.loadAppUsage(context, startMs, endMs)
        }
    }

    val maxSlot = timeSlotMinutes.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val values = timeSlotMinutes.map { (it.toFloat() / maxSlot).coerceIn(0f, 1f) }
    val visibleSlotIndex = StatisticsData.getCurrentSlotIndex()

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TimeSlotUsageCard(values = values, visibleSlotIndex = visibleSlotIndex)
        AppUsageCard(apps = appList)
    }
}

private val TopAppInfoTexts = listOf(
    "이 시간이면 서울 부산 KTX 왕복 8번이에요!",
    "해리포터 시리즈 전편을 두번 반복한 시간보다 많아요!",
    "손흥민이 토트넘에서 뛴 시간과 비슷해요",
    "10층짜리 빌딩을 짓는 시간보다 많아요",
    "이 정도면 아이유 콘서트 시간이랑 맞먹는 시간이에요",
)

@Composable
private fun TimeSlotUsageCard(
    values: List<Float>,
    visibleSlotIndex: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values, visibleSlotIndex) {
        progress.animateTo(0f, tween(150))
        progress.animateTo(1f, tween(600))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(StatsCardPadding),
        verticalArrangement = Arrangement.spacedBy(StatsCardContentSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "시간대별 사용량",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(16.dp))
        }

        val paddedValues = if (values.size >= 8) values.take(8) else values + List(8 - values.size) { 0f }
        Row(
            modifier = Modifier.fillMaxWidth().height(126.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            paddedValues.forEachIndexed { index, value ->
                val isVisible = index <= visibleSlotIndex
                val barProgress = if (isVisible) ((progress.value * (visibleSlotIndex + 1)) - index).coerceIn(0f, 1f) else 0f
                val animatedHeight = if (isVisible) (value * barProgress).coerceIn(0f, 1f) else 0f
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (isVisible) {
                            Box(
                                modifier = Modifier
                                    .width(BarWidth)
                                    .fillMaxHeight(animatedHeight)
                                    .clip(RoundedCornerShape(BarCornerRadius))
                                    .background(AppColors.ChartTrackFill),
                            )
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            StatisticsData.SlotLabels.forEach { label ->
                Text(
                    text = "$label",
                    style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    apps: List<StatisticsData.StatsAppItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(StatsCardPadding),
        verticalArrangement = Arrangement.spacedBy(StatsCardContentSpacing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "사용시간 최다 앱",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(16.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(StatsCardContentSpacing)) {
            apps.take(5).forEachIndexed { index, app ->
                StatsAppDataViewRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    isRestricted = app.isRestricted,
                    infoText = TopAppInfoTexts.getOrNull(index),
                )
            }
        }
    }
}

@Composable
private fun StatsAppDataViewRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    isRestricted: Boolean,
    infoText: String?,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    AppStatusDataViewRow(
        appName = name,
        appIcon = appIcon,
        totalUsageMinutes = usageMinutes,
        modifier = modifier,
        showDangerLabel = isRestricted,
        showLock = isRestricted,
        infoText = infoText,
    )
}
