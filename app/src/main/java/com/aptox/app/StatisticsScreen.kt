package com.aptox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.aptox.app.usage.UsageStatsLocalRepository

/** 통계 카드 도움말 바텀시트 타입 (Figma 932-8989, 932-8868, 932-8974) */
private enum class StatsHelpType(val title: String, val body: String) {
    DATE_CHART(
        "기간별 사용량",
        "주간은 월~일 단위로 요일별, 월간은 일별, 연간은 최대 6년 기준으로 앱 사용량을 보여줘요. 왼쪽 화살표로 더 과거, 오른쪽으로 최근 기간을 선택할 수 있어요.",
    ),
    CATEGORY(
        "카테고리 통계",
        "앱을 카테고리별로 묶어 사용 비율을 보여줘요. 상단 막대와 범례를 탭하면 해당 카테고리 앱만 필터할 수 있어요.",
    ),
    TIME_SLOT(
        "시간대별 사용량",
        "하루 중 어느 시간대에 스마트폰을 많이 사용하는지 보여줘요. 자신도 모르게 습관적으로 폰을 집어드는 시간대를 파악하고, 그 시간만 집중적으로 줄여보세요.",
    ),
}

/** 통계 카드 공통 패딩(모든 카드 동일) */
private val StatsCardPadding = 16.dp

/** 앱 사용 정보 접근 권한 미허용 시 안내 화면 */
@Composable
private fun StatsUsageAccessGuard(
    modifier: Modifier = Modifier,
    onGranted: () -> Unit = {},
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_disclaimer_info),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "앱 사용 정보 접근 권한이 필요해요",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "통계를 보려면 기기 설정에서\n'앱 사용 정보 접근 허용'을 켜주세요.",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AptoxPrimaryButton(
                text = "권한 설정하러 가기",
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

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
 * - 그룹 막대 차트 (전주 vs 이번주)
 * - 아래로 당기면 Daily Brief 캐시만 갱신 (탭 차트 데이터는 그대로)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasUsageAccess by remember { mutableStateOf(StatisticsData.hasUsageAccess(context)) }

    // 설정 화면에서 돌아올 때 권한 상태 재확인
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = StatisticsData.hasUsageAccess(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageAccess) {
        if (hasUsageAccess) {
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "usage_stats_initial_sync",
                androidx.work.ExistingWorkPolicy.KEEP,
                androidx.work.OneTimeWorkRequestBuilder<com.aptox.app.usage.UsageStatsSyncWorker>()
                    .setInputData(androidx.work.workDataOf(com.aptox.app.usage.UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
                    .build(),
            )
        }
    }

    if (!hasUsageAccess) {
        StatsUsageAccessGuard(
            modifier = modifier,
            onGranted = { hasUsageAccess = StatisticsData.hasUsageAccess(context) },
        )
        return
    }

    val statisticsViewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory(context))
    val selectedAppDetail by statisticsViewModel.selectedAppDetail.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 기본 주간
    val tabLabels = listOf("주간", "월간", "연간")
    val tabEnum = when (selectedTab) {
        0 -> StatisticsData.Tab.WEEKLY
        1 -> StatisticsData.Tab.MONTHLY
        else -> StatisticsData.Tab.YEARLY
    }

    // 주간 탭: 지난 완료 주 vs 그전 주 총 사용시간 감소율 → badge_016~018 (주 1회)
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            BadgeAutoGrant.checkWeeklyUsageReductionOnStatsOpen(context)
        }
    }

    // 카드별 독립적인 주/연도/월 네비게이션
    var weekOffsetDateChart by remember { mutableIntStateOf(0) }
    var monthOffsetDateChart by remember { mutableIntStateOf(0) } // 월간: 0=이번 달
    var yearOffsetDateChart by remember { mutableIntStateOf(0) }
    var weekOffsetCategory by remember { mutableIntStateOf(0) }
    var monthOffsetCategory by remember { mutableIntStateOf(0) } // 월간: 0=이번 달
    var yearOffsetCategory by remember { mutableIntStateOf(0) }
    var weekOffsetComparison by remember { mutableIntStateOf(0) } // 기본: 이번 주
    var monthOffsetComparison by remember { mutableIntStateOf(0) }  // 0=이번 달
    var yearOffsetComparison by remember { mutableIntStateOf(0) }

    var showHelpSheet by remember { mutableStateOf<StatsHelpType?>(null) }

    BackHandler(enabled = selectedAppDetail != null || showHelpSheet != null) {
        when {
            selectedAppDetail != null -> statisticsViewModel.onBottomSheetDismiss()
            showHelpSheet != null -> showHelpSheet = null
        }
    }

    var briefRefreshGeneration by remember { mutableIntStateOf(0) }
    var isBriefPullRefreshing by remember { mutableStateOf(false) }
    val pullRefreshScope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isBriefPullRefreshing,
        onRefresh = {
            pullRefreshScope.launch {
                isBriefPullRefreshing = true
                briefRefreshGeneration++
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 32.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DailyBriefCard(
                    refreshGeneration = briefRefreshGeneration,
                    onRegenerateComplete = { isBriefPullRefreshing = false },
                )

                AptoxSegmentedTab(
                    items = tabLabels,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                )

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
                    statisticsViewModel = statisticsViewModel,
                )
                StatsTimeSlotSection(
                    tabEnum = tabEnum,
                    weekOffset = weekOffsetComparison,
                    onWeekChange = { weekOffsetComparison = it },
                    monthOffset = monthOffsetComparison,
                    onMonthChange = { monthOffsetComparison = it },
                    yearOffset = yearOffsetComparison,
                    onYearChange = { yearOffsetComparison = it },
                    onInfoClick = { showHelpSheet = StatsHelpType.TIME_SLOT },
                )
                // externalTest 등 SHOW_DEBUG_MENU=false 빌드에서는 디버그 APK라도 통계 테스트 카드 숨김
                if (BuildConfig.SHOW_DEBUG_MENU && BuildConfig.DEBUG) {
                    StatisticsDebugUsagePatternCard()
                    StatisticsDebugTotalUsageTimeCard()
                    StatisticsDebugCategoryStatsCard(statisticsViewModel = statisticsViewModel)
                }
                // 맨 아래 스크롤 시 바텀바로부터 32dp 여백 (padding bottom으로 적용)
            }
    }

    showHelpSheet?.let { helpType ->
        StatsCardHelpBottomSheet(
            title = helpType.title,
            body = helpType.body,
            onDismiss = { showHelpSheet = null },
        )
    }

    selectedAppDetail?.let { detail ->
        AppDetailBottomSheet(
            state = detail,
            onDismiss = { statisticsViewModel.onBottomSheetDismiss() },
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
            // Figma 932-8989 등: BaseBottomSheet 헤더와 동일 — 제목~본문 8dp, 상단은 시트 상단 과대 여백 방지로 24dp
            Spacer(modifier = Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = body,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            AptoxPrimaryButton(
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

/**
 * Figma 926:8043 Daily Brief 카드 (탭 공통 영역)
 * - 탭 선택과 완전히 독립적으로 동작
 * - 데이터 기준: 어제 00:00 ~ 23:59:59, 캐시 키 DAILY_yyyyMMdd
 * - [refreshGeneration]이 증가하면 캐시를 지우고 템플릿 재생성 (pull-to-refresh)
 * - 카드: 흰 배경, primary-300 테두리
 * - 상단: Daily Brief + 제목 (gap 2dp), 본문 (gap 12dp)
 */
@Composable
private fun DailyBriefCard(
    refreshGeneration: Int = 0,
    onRegenerateComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var briefTitle by remember { mutableStateOf<String?>(null) }
    var briefBody by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshGeneration) {
        val (startMs, _, _) = StatisticsData.getYesterdayRange()
        val cacheKey = DailyBriefGenerator.cacheKey(startMs)
        try {
            if (refreshGeneration == 0) {
                BriefSummaryCache.get(context, cacheKey)?.let { entry ->
                    briefTitle = entry.title
                    briefBody = entry.body
                    return@LaunchedEffect
                }
                briefBody = "불러오는 중..."
            } else {
                BriefSummaryCache.remove(context, cacheKey)
                briefTitle = null
                briefBody = "불러오는 중..."
            }

            runCatching { DailyBriefGenerator.generate(context) }
                .onSuccess { (title, body) ->
                    BriefSummaryCache.put(context, cacheKey, BriefSummaryCache.Entry(title, body))
                    briefTitle = title
                    briefBody = body
                }
                .onFailure {
                    briefTitle = null
                    briefBody = null
                }
        } finally {
            if (refreshGeneration > 0) {
                onRegenerateComplete()
            }
        }
    }

    val displayTitle = briefTitle ?: "어제는 어떤 하루였나요?"
    val showBody = briefBody != null && briefBody != "불러오는 중..."

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
                    text = "Daily Brief",
                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                )
                Text(
                    text = displayTitle,
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                )
            }
            if (showBody) {
                AptoxInfoBoxCompactNewDesign(
                    text = briefBody!!,
                    maxLines = Int.MAX_VALUE,
                    contentPaddingHorizontal = 16.dp,
                    contentPaddingVertical = 18.dp,
                )
            } else if (briefBody == "불러오는 중...") {
                Text(
                    text = "불러오는 중...",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                )
            }
        }
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

/** 기간별 사용량 카드 상단 카테고리 탭 — API/수동 라벨 `주식·코인` 등과 매칭 */
private const val DateChartTabStock = "STOCK"

private fun matchesDateChartCategoryFilter(filterKey: String?, category: String): Boolean {
    if (filterKey == null) return true
    if (filterKey == DateChartTabStock) {
        return category == "주식,코인" || category == "주식·코인" || category == "주식/코인" || category == "주식&코인"
    }
    return category == filterKey
}

private data class DateChartCategoryTab(val label: String, val filterKey: String?)

private val DateChartCategoryTabs = listOf(
    DateChartCategoryTab("전체", null),
    DateChartCategoryTab("OTT", "OTT"),
    DateChartCategoryTab("SNS", "SNS"),
    DateChartCategoryTab("게임", "게임"),
    DateChartCategoryTab("웹툰", "웹툰"),
    DateChartCategoryTab("쇼핑", "쇼핑"),
    DateChartCategoryTab("주식 · 코인", DateChartTabStock),
)

/** 기간별 사용량 카테고리 탭 — 칩당 너비(디버그 제한 카드 120dp보다 좁게) */
private val StatsRestrictionStyleTabWidth = 70.dp
private val StatsRestrictionStyleTabHeight = 30.dp

@Composable
private fun StatsDateChartCategoryTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) AppColors.Primary50 else AppColors.SurfaceBackgroundCard
    val fg = if (selected) AppColors.Primary300 else AppColors.TextSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonSmall.copy(color = fg),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 기간별·시간대별 사용량 카드 공통 — 칩 70×30dp, 가로 스크롤·좌측 정렬 */
@Composable
private fun StatsDateChartCategoryTabRow(
    selectedFilterKey: String?,
    onSelectFilterKey: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(StatsRestrictionStyleTabHeight)
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateChartCategoryTabs.forEach { tab ->
            StatsDateChartCategoryTabChip(
                text = tab.label,
                selected = tab.filterKey == selectedFilterKey,
                onClick = { onSelectFilterKey(tab.filterKey) },
                modifier = Modifier
                    .width(StatsRestrictionStyleTabWidth)
                    .fillMaxHeight(),
            )
        }
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
    var dateChartCategoryFilterKey by remember { mutableStateOf<String?>(null) }

    var dayMinutes by remember { mutableStateOf<List<Long>>(List(7) { 0L }) }
    var dayOfMonthMinutes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var yearMinutes by remember { mutableStateOf<List<Long>>(emptyList()) }
    var yearLabels by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset, dateChartCategoryFilterKey) {
        withContext(Dispatchers.IO) {
            val categoryRepo = AppCategoryRepository(context)
            val allCategories = categoryRepo.getAllCategories()
            val allowedPackages: Set<String>? = if (dateChartCategoryFilterKey == null) {
                null
            } else {
                allCategories.filter { (_, cat) ->
                    matchesDateChartCategoryFilter(dateChartCategoryFilterKey, cat)
                }.keys.toSet()
            }
            when (tabEnum) {
                StatisticsData.Tab.WEEKLY -> {
                    val (s, e, _) = StatisticsData.getWeekRange(weekOffset)
                    dayMinutes = StatisticsData.loadDayOfWeekMinutes(context, s, e, allowedPackages)
                }
                StatisticsData.Tab.MONTHLY -> {
                    val (s, e, _) = StatisticsData.getSingleMonthRange(monthOffset)
                    dayOfMonthMinutes = StatisticsData.loadDayOfMonthMinutes(context, s, e, allowedPackages)
                }
                StatisticsData.Tab.YEARLY -> {
                    val (ranges, labels) = StatisticsData.getYearRanges(yearOffset)
                    val minutes = StatisticsData.loadYearsMinutes(context, ranges, allowedPackages)
                    val n = minOf(minutes.size, labels.size)
                    val nonZeroYears = minutes.take(n).zip(labels.take(n)).filter { it.first > 0 }
                    yearMinutes = nonZeroYears.map { it.first }
                    yearLabels = nonZeroYears.map { it.second }
                }
                else -> {}
            }
        }
    }

    val olderHasData = rememberOlderPeriodHasUsage(tabEnum, weekOffset, monthOffset, yearOffset)
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset, olderHasData) {
        val label = formatPeriodLabel(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, olderHasData, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, olderHasData, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, olderHasData, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }

    // 패딩·타이틀~탭·탭~차트 간격: 디버그 홈「제한 중인 앱」카드와 동일 (상 22, 좌우 16, 타이틀–탭 16, 탭–본문 24)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 26.dp),
    ) {
        StatsCardTitleRow(
            title = "기간별 사용량",
            titleIconSpacing = 4.dp,
            titleColor = AppColors.TextPrimary,
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            onInfoClick = onInfoClick,
            showPeriodSelector = tabEnum != StatisticsData.Tab.YEARLY,
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatsDateChartCategoryTabRow(
            selectedFilterKey = dateChartCategoryFilterKey,
            onSelectFilterKey = { dateChartCategoryFilterKey = it },
        )
        Spacer(modifier = Modifier.height(24.dp))
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> DayOfWeekBarChart(
                values = dayMinutes,
                isCurrentPeriod = weekOffset == 0,
            )
            StatisticsData.Tab.MONTHLY -> MonthDailyBarChart(
                values = dayOfMonthMinutes,
                isCurrentMonth = monthOffset == 0,
            )
            StatisticsData.Tab.YEARLY -> YearBarChart(
                values = yearMinutes,
                labels = yearLabels,
                isCurrentYear = yearOffset == 0,
            )
            else -> {}
        }
        // 보라색 텍스트 박스 숨김
        // AptoxInfoBoxCompactNewDesign(text = dateChartInsight, ...)
    }
}

private data class NavState(
    val text: String,
    val canPrev: Boolean,
    val canNext: Boolean,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
)

/**
 * 왼쪽(더 과거) 화살표: 바로 이전 기간(weekOffset-1 등)에 사용량이 있을 때만 활성화.
 */
@Composable
private fun rememberOlderPeriodHasUsage(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
): Boolean {
    val context = LocalContext.current
    var olderHasData by remember { mutableStateOf(true) }
    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset) {
        olderHasData = when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> withContext(Dispatchers.IO) {
                StatisticsData.hasAnyUsageInWeek(context, weekOffset - 1)
            }
            StatisticsData.Tab.MONTHLY -> withContext(Dispatchers.IO) {
                StatisticsData.hasAnyUsageInMonth(context, monthOffset - 1)
            }
            StatisticsData.Tab.YEARLY -> withContext(Dispatchers.IO) {
                StatisticsData.hasAnyUsageInYearChartWindow(context, yearOffset - 1)
            }
            else -> true
        }
    }
    return olderHasData
}

/** 시간대별 카드: 주간=달력 주, 월=달력 월, 연=단일 연도 — 데이터 로드와 동일한 기준으로 이전 구간 사용 여부 판별 */
@Composable
private fun rememberOlderPeriodHasUsageTimeSlot(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
): Boolean {
    val context = LocalContext.current
    var olderHasData by remember { mutableStateOf(true) }
    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset) {
        olderHasData = when (tabEnum) {
            StatisticsData.Tab.WEEKLY,
            StatisticsData.Tab.MONTHLY,
            StatisticsData.Tab.YEARLY -> withContext(Dispatchers.IO) {
                StatisticsData.hasAnyUsageInTimeSlotOlderRolling(
                    context, tabEnum, weekOffset, monthOffset, yearOffset,
                )
            }
            else -> true
        }
    }
    return olderHasData
}

/** 기간별 사용량 하단 인사이트 메시지 (Figma 919:3520 텍스트 카드) */
private fun formatDateChartInsight(
    tabEnum: StatisticsData.Tab,
    dayMinutes: List<Long>,
    dayOfMonthMinutes: List<Long>,
    yearMinutes: List<Long>,
    yearLabels: List<String>,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
): String {
    val periodPrefix = when {
        (tabEnum == StatisticsData.Tab.WEEKLY && weekOffset == 0) ||
        (tabEnum == StatisticsData.Tab.MONTHLY && monthOffset == 0) ||
        (tabEnum == StatisticsData.Tab.YEARLY && yearOffset == 0) -> "이번"
        else -> "지난"
    }
    return when (tabEnum) {
        StatisticsData.Tab.WEEKLY -> {
            val maxIdx = dayMinutes.withIndex().maxByOrNull { it.value }?.index ?: -1
            if (maxIdx < 0 || dayMinutes.all { it == 0L }) "사용량 데이터가 없어요"
            else "${periodPrefix}주엔 ${DayLabels[maxIdx]}요일의 스마트폰 사용시간이 가장 높았어요"
        }
        StatisticsData.Tab.MONTHLY -> {
            val maxIdx = dayOfMonthMinutes.withIndex().maxByOrNull { it.value }?.index ?: -1
            if (maxIdx < 0 || dayOfMonthMinutes.all { it == 0L }) "사용량 데이터가 없어요"
            else "${periodPrefix} 달 ${maxIdx + 1}일의 스마트폰 사용시간이 가장 높았어요"
        }
        StatisticsData.Tab.YEARLY -> {
            val minSize = minOf(yearMinutes.size, yearLabels.size)
            val maxIdx = yearMinutes.take(minSize).withIndex().maxByOrNull { it.value }?.index ?: -1
            if (maxIdx < 0 || yearMinutes.all { it == 0L }) "사용량 데이터가 없어요"
            else "${yearLabels.getOrNull(maxIdx) ?: ""}의 스마트폰 사용시간이 가장 높았어요"
        }
        else -> "사용량 데이터가 없어요"
    }
}

/** 우측 날짜/기간 표기: 주간=월~일 주 단위 "M.d ~ M.d", 월간·연간 동일 */
private fun formatPeriodLabel(tabEnum: StatisticsData.Tab, weekOffset: Int, monthOffset: Int, yearOffset: Int): String = when (tabEnum) {
    StatisticsData.Tab.WEEKLY -> StatisticsData.getWeekRange(weekOffset).third
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

/** 카테고리/제한앱 카드용: 주간은 getWeekRange 표기, 월간은 달 라벨 */
private fun formatPeriodLabelMonthAware(
    tabEnum: StatisticsData.Tab,
    weekOffset: Int,
    monthOffset: Int,
    yearOffset: Int,
): String = when (tabEnum) {
    StatisticsData.Tab.WEEKLY -> StatisticsData.getWeekRange(weekOffset).third
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
    titleColor: Color = AppColors.TextSecondary,
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
                style = AppTypography.HeadingH2.copy(color = titleColor),
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
private val ChartVerticalPadding = 10.dp
private val TotalChartHeight = BarChartHeight + ChartVerticalPadding * 2
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
/** Y축 틱 라벨을 각 그리드 라인(y = i/(n-1) × 차트 높이)과 수직 중앙 정렬 (주간/월간/연간 동일) */
@Composable
private fun ChartYAxisLabels(
    yTicks: List<Long>,
    modifier: Modifier = Modifier,
) {
    val ticks = yTicks.asReversed()
    if (ticks.size < 2) return
    val density = LocalDensity.current
    val halfLabelDp = with(density) {
        (ChartAxisTextStyle.lineHeight.toPx() / 2f).toDp()
    }
    val denom = ticks.lastIndex.coerceAtLeast(1)
    Box(
        modifier = modifier
            .width(ChartYAxisWidth)
            .height(TotalChartHeight)
            .padding(start = 2.dp)
            .alpha(0.8f)
            .padding(vertical = ChartVerticalPadding),
    ) {
        ticks.forEachIndexed { index, tick ->
            val fraction = index / denom.toFloat()
            Text(
                text = formatChartYLabel(tick),
                style = ChartAxisTextStyle,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(y = BarChartHeight * fraction - halfLabelDp),
            )
        }
    }
}

/** 분 단위 값을 차트 Y축 레이블로 포맷 (60분 이상이면 "XH", 6만분 이상이면 "X천H") */
private fun formatChartYLabel(minutes: Long): String = when {
    minutes <= 0 -> "0H"
    minutes >= 60_000 -> "${(minutes + 30_000) / 60_000}천H"
    minutes >= 60 -> "${minutes / 60}H"
    else -> "$minutes"
}

/** 기간별 사용량 Y축 고정: 균등 간격 라벨 0H~16H (주간/월간/연간 동일). 막대 높이는 16H 기준 정규화 */
private val PeriodChartFixedYTicks = listOf(0L, 240L, 480L, 720L, 960L) // 0, 4H, 8H, 12H, 16H
private val PeriodChartFixedMaxMinutes = 960L // 16H

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
    val normalized = padded.map { (it.toFloat() / PeriodChartFixedMaxMinutes).coerceIn(0f, 1f) }
    val highlightIdx = if (isCurrentPeriod) 6 else -1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartYAxisLabels(yTicks = PeriodChartFixedYTicks)
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(TotalChartHeight).padding(vertical = ChartVerticalPadding)) {
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
private val MonthDailySlotWidth = 36.dp

@Composable
private fun MonthDailyBarChart(
    values: List<Long>,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier,
) {
    val padded = values
    val daysInMonth = padded.size.coerceAtLeast(1)
    val normalized = padded.map { (it.toFloat() / PeriodChartFixedMaxMinutes).coerceIn(0f, 1f) }
    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
    val highlightIdx = if (isCurrentMonth && todayDay <= daysInMonth) todayDay - 1 else -1
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    LaunchedEffect(isCurrentMonth, todayDay) {
        if (!isCurrentMonth) return@LaunchedEffect
        delay(50)
        val slotWidthPx = with(density) { MonthDailySlotWidth.roundToPx() }
        // 오늘을 먼저 보이되, 직전(어제) 데이터가 왼쪽 경계에 살짝 걸쳐 보이게 스크롤
        val peekPx = (slotWidthPx * 0.35f).toInt()
        val targetScroll = maxOf(0, (todayDay - 1) * slotWidthPx - peekPx)
        scrollState.scrollTo(minOf(targetScroll, scrollState.maxValue.coerceAtLeast(0)))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Y축: 고정 (스크롤 안 됨)
            ChartYAxisLabels(yTicks = PeriodChartFixedYTicks)
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
                        .height(TotalChartHeight)
                        .padding(vertical = ChartVerticalPadding),
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
    val normalized = padded.map { (it.toFloat() / PeriodChartFixedMaxMinutes).coerceIn(0f, 1f) }
    val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
    val highlightIdx = if (isCurrentYear) currentMonth else -1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartYAxisLabels(yTicks = PeriodChartFixedYTicks)
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            Box(modifier = Modifier.weight(1f).height(TotalChartHeight).padding(vertical = ChartVerticalPadding)) {
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

/** 연간: 주간/월간과 동일 차트 스타일. 막대 26dp, X축 2024/2025/2026 */
@Composable
private fun YearBarChart(
    values: List<Long>,
    labels: List<String>,
    isCurrentYear: Boolean,
    modifier: Modifier = Modifier,
) {
    val count = maxOf(values.size, labels.size).coerceAtLeast(1)
    val padded = values.take(count) + List((count - values.size).coerceAtLeast(0)) { 0L }
    val displayLabels = labels.take(count) + List((count - labels.size).coerceAtLeast(0)) { "" }
    val normalized = padded.map { (it.toFloat() / PeriodChartFixedMaxMinutes).coerceIn(0f, 1f) }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val highlightIdx = displayLabels.indexOfFirst { it.toIntOrNull() == currentYear }.takeIf { it >= 0 } ?: -1
    val useWideYearBars = count < 3

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            ChartYAxisLabels(yTicks = PeriodChartFixedYTicks)
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            BoxWithConstraints(
                modifier = Modifier.weight(1f).height(TotalChartHeight).padding(vertical = ChartVerticalPadding),
            ) {
                val yearBarWidth = if (useWideYearBars) maxWidth * 0.45f else BarWidth
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
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    normalized.forEachIndexed { index, value ->
                        val isHighlight = isCurrentYear && (index == highlightIdx && value > 0f)
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
                                        .width(yearBarWidth)
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
            displayLabels.forEach { label ->
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

@Composable
private fun CategoryStatsLegendRow(
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
    val baseColor = CategoryColors[label] ?: Color.Gray
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
    statisticsViewModel: StatisticsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val olderHasData = rememberOlderPeriodHasUsage(tabEnum, weekOffset, monthOffset, yearOffset)
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset, olderHasData) {
        val label = formatPeriodLabelMonthAware(tabEnum, weekOffset, monthOffset, yearOffset)
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, olderHasData, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, olderHasData, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, olderHasData, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }
    var appList by remember { mutableStateOf<List<StatisticsData.StatsAppItem>>(emptyList()) }
    var categoryTotalsFromDb by remember { mutableStateOf<Map<String, Long>?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset) {
        val (startMs, endMs) = when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> StatisticsData.getWeekRange(weekOffset).let { it.first to it.second }
            StatisticsData.Tab.MONTHLY -> StatisticsData.getSingleMonthRange(monthOffset).let { it.first to it.second }
            StatisticsData.Tab.YEARLY -> StatisticsData.getSingleYearRange(yearOffset).let { it.first to it.second }
            else -> 0L to 0L
        }
        statisticsViewModel.syncCategoryStatsPeriod(tabEnum, weekOffset, monthOffset, yearOffset)
        val localRepo = UsageStatsLocalRepository(context)
        val startDate = UsageStatsLocalRepository.msToYyyyMmDd(startMs)
        val endDate = UsageStatsLocalRepository.msToYyyyMmDd(endMs)
        categoryTotalsFromDb = withContext(Dispatchers.IO) {
            if (localRepo.hasCategoryDataForDateRangeBlocking(startDate, endDate)) {
                localRepo.getCategoryTotalsForDateRangeBlocking(startDate, endDate)
            } else {
                null
            }
        }
        appList = withContext(Dispatchers.IO) {
            StatisticsData.loadAppUsageForAllowedCategories(context, startMs, endMs)
        }
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
                    val baseColor = CategoryColors[label] ?: Color.Gray
                    val displayColor = if (isPressed) lerp(baseColor, Color.Black, 0.08f) else baseColor
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight((pct / 100f).coerceAtLeast(0.02f))
                            .background(displayColor)
                            .clickable(interactionSource = interactionSource, indication = null) { selectedCategory = if (selectedCategory == label) null else label },
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
                    CategoryStatsLegendRow(
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
                    CategoryStatsLegendRow(
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

        Column(verticalArrangement = Arrangement.spacedBy(StatsCardListItemSpacing)) {
            filteredApps.take(6).forEach { app ->
                StatsAppRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    categoryTag = app.categoryTag,
                    onClick = { statisticsViewModel.onAppItemClick(app.packageName) },
                )
            }
        }
        // 보라색 텍스트 박스 숨김
        // AptoxInfoBoxCompactNewDesign(text = categoryInsight, ...)
    }
}

/** 카테고리 통계 하단 인사이트 메시지 (Figma 925:7299 텍스트 카드) */
private fun formatCategoryStatsInsight(segments: List<Pair<String, Float>>): String {
    if (segments.isEmpty()) return "카테고리 데이터가 없어요"
    val (topCat, topPct) = segments.first()
    val secondPct = segments.getOrNull(1)?.second ?: 0f
    val diff = (topPct - secondPct).toInt().coerceAtLeast(0)
    return if (diff > 0) "${topCat} 앱의 사용량이 다른 카테고리에 비해 ${diff}%나 더 많아요"
    else "${topCat} 앱을 가장 많이 사용했어요"
}

/** Figma 925:7299 — 앱 row: 아이콘(56dp) + 태그(위) + 앱 이름(아래) + 사용시간 */
@Composable
private fun StatsAppRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    categoryTag: String?,
    onClick: () -> Unit = {},
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

/** 시간대별 사용량 카드 (Figma 925-7593). 기간별 사용량 카드와 동일 패딩·카테고리 탭 */
@Composable
private fun StatsTimeSlotSection(
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
    var timeSlotCategoryFilterKey by remember { mutableStateOf<String?>(null) }
    val olderHasData = rememberOlderPeriodHasUsageTimeSlot(tabEnum, weekOffset, monthOffset, yearOffset)
    val nav = remember(tabEnum, weekOffset, monthOffset, yearOffset, olderHasData) {
        val label = when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> StatisticsData.getWeekRange(weekOffset).third
            StatisticsData.Tab.MONTHLY -> {
                val (_, _, monthLabel) = StatisticsData.getSingleMonthRange(monthOffset)
                monthLabel
            }
            StatisticsData.Tab.YEARLY -> {
                if (yearOffset == 0) "올해"
                else {
                    val y = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) + yearOffset
                    "${y}년"
                }
            }
            else -> StatisticsData.getWeekRange(0).third
        }
        when (tabEnum) {
            StatisticsData.Tab.WEEKLY -> NavState(label, olderHasData, weekOffset < 0, { onWeekChange(weekOffset - 1) }, { if (weekOffset < 0) onWeekChange(weekOffset + 1) })
            StatisticsData.Tab.MONTHLY -> NavState(label, olderHasData, monthOffset < 0, { onMonthChange(monthOffset - 1) }, { if (monthOffset < 0) onMonthChange(monthOffset + 1) })
            StatisticsData.Tab.YEARLY -> NavState(label, olderHasData, yearOffset < 0, { onYearChange(yearOffset - 1) }, { if (yearOffset < 0) onYearChange(yearOffset + 1) })
            else -> NavState("", false, false, {}, {})
        }
    }

    var timeSlotMinutes by remember { mutableStateOf<List<Long>>(List(12) { 0L }) }
    LaunchedEffect(tabEnum, weekOffset, monthOffset, yearOffset, timeSlotCategoryFilterKey) {
        withContext(Dispatchers.IO) {
            val categoryRepo = AppCategoryRepository(context)
            val allCategories = categoryRepo.getAllCategories()
            val allowedPackages: Set<String>? = if (timeSlotCategoryFilterKey == null) {
                null
            } else {
                allCategories.filter { (_, cat) ->
                    matchesDateChartCategoryFilter(timeSlotCategoryFilterKey, cat)
                }.keys.toSet()
            }
            val (startMs, endMs) = when (tabEnum) {
                StatisticsData.Tab.WEEKLY -> {
                    val (s, e, _) = StatisticsData.getWeekRange(weekOffset)
                    s to e
                }
                StatisticsData.Tab.MONTHLY -> {
                    val (s, e, _) = StatisticsData.getSingleMonthRange(monthOffset)
                    s to e
                }
                StatisticsData.Tab.YEARLY -> {
                    val (s, e, _) = StatisticsData.getSingleYearRange(yearOffset)
                    s to e
                }
                else -> {
                    val (s, e, _) = StatisticsData.getLastNDaysRange(7, 0)
                    s to e
                }
            }
            val divideByDays = when (tabEnum) {
                StatisticsData.Tab.MONTHLY,
                StatisticsData.Tab.YEARLY -> StatisticsData.daysInclusiveCappedAtNow(startMs, endMs)
                else -> 0
            }
            timeSlotMinutes = StatisticsData.loadTimeSlot12Minutes(
                context = context,
                startMs = startMs,
                endMs = endMs,
                divideByDays = divideByDays,
                allowedPackages = allowedPackages,
            )
        }
    }

    val padded = if (timeSlotMinutes.size >= 12) timeSlotMinutes.take(12) else timeSlotMinutes + List(12 - timeSlotMinutes.size) { 0L }
    val maxIdx = padded.indices.maxByOrNull { padded[it] }?.takeIf { padded[it] > 0 } ?: -1
    val timeSlotMaxMinutes = 120L // 막대당 최대 2H
    val yTicks = listOf(0L, 60L, 120L) // 0 / 1H / 2H 고정
    val normalized = padded.map { (it.toFloat() / timeSlotMaxMinutes).coerceIn(0f, 1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 26.dp),
    ) {
        StatsCardTitleRow(
            title = "시간대별 사용량",
            titleIconSpacing = 4.dp,
            titleColor = AppColors.TextPrimary,
            dateRangeText = nav.text,
            canGoPrev = nav.canPrev,
            canGoNext = nav.canNext,
            onPrevClick = nav.onPrev,
            onNextClick = nav.onNext,
            onInfoClick = onInfoClick,
            showPeriodSelector = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        StatsDateChartCategoryTabRow(
            selectedFilterKey = timeSlotCategoryFilterKey,
            onSelectFilterKey = { timeSlotCategoryFilterKey = it },
        )
        Spacer(modifier = Modifier.height(24.dp))
        TimeSlotBarChartComponent(
            values = normalized,
            maxValueIdx = maxIdx,
            showSpeechBubble = false,
            // speechBubbleText = "스마트폰 사용 최다 시간대", // 말풍선 제거
            modifier = Modifier.fillMaxWidth(),
        )

        // 보라색 텍스트 박스 숨김
        // AptoxInfoBoxCompactNewDesign(text = ..., ...)
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
        showLock = isRestricted,
        infoText = infoText,
    )
}
