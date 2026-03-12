package com.cole.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.Calendar

/** 자가테스트 결과 레벨 (8~32점 구간) */
enum class SelfTestResultType {
    /** 8~13점 */
    HEALTH,
    /** 14~19점 */
    GOOD,
    /** 20~25점 */
    CAUTION,
    /** 26~32점 */
    DANGER,
}

fun computeSelfTestResultType(answers: Map<Int, Int>): SelfTestResultType {
    val rawScore = answers.values.sumOf { (4 - it).coerceIn(0, 4) }.coerceIn(8, 32)
    return when {
        rawScore <= 13 -> SelfTestResultType.HEALTH
        rawScore <= 19 -> SelfTestResultType.GOOD
        rawScore <= 25 -> SelfTestResultType.CAUTION
        else -> SelfTestResultType.DANGER
    }
}

/** 홈 화면 제한 앱 영역 상태 (items + recommendedApp 동시 로드로 플래시 방지) */
private data class HomeRestrictionState(
    val items: List<MainAppRestrictionItem>,
    val recommendedApp: StatisticsData.StatsAppItem?,
)

/** rawScore(8~32)를 SelfTestResultType으로 변환 */
fun rawScoreToResultType(rawScore: Int): SelfTestResultType = when {
    rawScore <= 13 -> SelfTestResultType.HEALTH
    rawScore <= 19 -> SelfTestResultType.GOOD
    rawScore <= 25 -> SelfTestResultType.CAUTION
    else -> SelfTestResultType.DANGER
}

/**
 * 스플래시 화면 (Figma SP-01, node 409:6664)
 * - 배경: Primary300 #6C54DD
 * - 로고: 280x150, 화면 중앙, 좌우 40dp
 * - WindowInsets 적용
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000)
        onFinish()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary300)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_splash_logo),
            contentDescription = "cole.",
            modifier = Modifier
                .width(280.dp)
                .height(150.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun OnboardingScreen(onSkipClick: () -> Unit, onStartClick: () -> Unit) {
    OnboardingHost(onSkipClick = onSkipClick, onStartClick = onStartClick)
}

// MA-01 메인 화면 (Figma 336-2910)
private val MainCardShape = RoundedCornerShape(12.dp)
private val MainCardShadowColor = Color.Black.copy(alpha = 0.06f)

/** 제한 방식: 시간 지정 vs 일일 사용량 */
internal enum class RestrictionType {
    /** 시간 지정 제한 (예: 14분/30분 사용 중) */
    TIME_SPECIFIED,
    /** 일일 사용량 제한 (예: 45분/90분 7회) */
    DAILY_USAGE,
}

// 목업: 앱 제한 행 (usageTextColor/usageLabelColor: 일시정지중일 때 Red300)
internal data class MainAppRestrictionItem(
    val appName: String,
    val packageName: String = "",
    val usageText: String,
    val usageLabel: String,
    val showDetailButton: Boolean,
    val limitMinutes: Int = 0,
    val todayUsageMinutes: Int = 0,
    val appIconResId: Int = R.drawable.ic_app_placeholder,
    val usageTextColor: Color? = null,
    val usageLabelColor: Color? = null,
    val restrictionType: RestrictionType = RestrictionType.TIME_SPECIFIED,
    /** 시간 지정 제한 앱이 일시 정지 중일 때 true (Figma 782-2858 바텀시트) */
    val isPaused: Boolean = false,
    /** 일시 정지 시점의 오늘 사용 시간 (예: "14분/30분") */
    val usageBeforePause: String = "",
    // 일일 사용량 제한용 (restrictionType == DAILY_USAGE)
    val usageMinutes: String = "",
    val sessionCount: String = "",
    val dailyLimitMinutes: String = "",
    val repeatDays: String = "",
    val duration: String = "",
    /** 시간 지정 제한: 제한 해제 시각 (ms) */
    val blockUntilMs: Long = 0L,
    /** 오늘 일시정지 사용 횟수 */
    val pauseUsedCount: Int = 0,
    /** 오늘 일시정지 남은 횟수 */
    val pauseRemainingCount: Int = 2,
    /** 일시정지 중일 때 남은 분 */
    val pauseLeftMin: Int = 0,
)

/** MA-01: 코멘트 + 서브텍스트 + > 통계 이동 (Figma 932:9099) */
@Composable
private fun MainCommentSection(
    comment: String,
    subtext: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = comment,
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            )
            Text(
                text = subtext,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = "통계 보기",
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun MainAppRestrictionRow(
    item: MainAppRestrictionItem,
    modifier: Modifier = Modifier,
    onDetailClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        AppIconSquircleLock(
            appIcon = rememberAppIconPainter(item.packageName),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.appName,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.usageText,
                    style = AppTypography.Caption2.copy(color = item.usageTextColor ?: AppColors.TextHighlight),
                )
                if (item.usageLabel.isNotEmpty()) {
                    Text(
                        text = item.usageLabel,
                        style = AppTypography.Caption2.copy(color = item.usageLabelColor ?: AppColors.TextSecondary),
                    )
                }
            }
        }
        // 사용 중 / 일시 정지 중 / 일일 사용량 — 모두 바텀시트 있으므로 자세히 보기 항상 표시
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.ButtonSecondaryBgDefault)
                .border(0.6.dp, AppColors.ButtonSecondaryBorderDefault, RoundedCornerShape(6.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDetailClick() }
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(text = "자세히 보기", style = AppTypography.ButtonSmall.copy(color = AppColors.ButtonSecondaryTextDefault))
        }
    }
}

@Composable
private fun MainAppRestrictionCard(
    apps: List<MainAppRestrictionItem>,
    onAddAppClick: () -> Unit,
    onDetailClick: (MainAppRestrictionItem) -> Unit = {},
    modifier: Modifier = Modifier,
    addButtonText: String = "사용제한 앱 추가",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
            .clip(MainCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "진행 중인 앱",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            apps.forEach { item ->
                MainAppRestrictionRow(item = item, onDetailClick = { onDetailClick(item) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            ColeAddAppButton(
                text = addButtonText,
                icon = painterResource(R.drawable.ic_add_circle),
                onClick = onAddAppClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
            )
        }
    }
}

/** MA-02: 진행 중인 앱 빈 상태 (Figma 662:2910) — 추천 앱 있을 때 기기 데이터 기반 추천 UI */
@Composable
private fun MainAppRestrictionCardEmpty(
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    addButtonText: String = "사용제한 앱 추가",
    recommendedApp: StatisticsData.StatsAppItem? = null,
    onRecommendedAppClick: ((String) -> Unit)? = null,
    onInfoClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
            .clip(MainCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 26.dp, end = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (recommendedApp != null) "사용 제한 중인 앱" else "진행 중인 앱",
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
                )
                if (onInfoClick != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onInfoClick),
                    ) {
                        IcoDisclaimerInfo(modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (recommendedApp != null) {
                Text(
                    text = "기기 데이터 기반으로 많이 쓰는 앱을 불러왔어요. 제한하시겠어요?",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "아직 진행중인 앱이 없어요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "잠시 제한하고 싶은 앱을",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "추가해보세요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (recommendedApp != null) {
            Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .then(if (onRecommendedAppClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onRecommendedAppClick(recommendedApp.packageName) } else Modifier),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconBox(
                            appIcon = rememberAppIconPainter(recommendedApp.packageName),
                            size = 56.dp,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LabelDanger(text = "위험")
                            Text(
                                text = recommendedApp.name,
                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "하루 평균",
                            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                        )
                        val dailyAvgMin = (recommendedApp.usageMs / 7 / 60_000).toInt().coerceAtLeast(0)
                        Text(
                            text = "${java.text.DecimalFormat("#,###").format(dailyAvgMin)}분 사용",
                            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            ColeAddAppButton(
                text = addButtonText,
                icon = painterResource(R.drawable.ic_add_circle),
                onClick = onAddAppClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
            )
        }
    }
}

@Composable
private fun MainAddictionCard(
    score: Int,
    message: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
            .clip(MainCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "나의 스마트폰 중독 지수",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
        )
        ResultGaugeGraph(
            fillProgress = ((score - 250) / 250f).coerceIn(0f, 1f),
            displayScore = score,
            interpretation = message,
        )
        ColeOutlinedTextButton(text = "내 스마트폰 의존도는 몇점일까", onClick = onPrimaryClick)
    }
}

private sealed class SettingsDetail(val title: String) {
    data object AccountManage : SettingsDetail("계정관리")
    data object Subscription : SettingsDetail("구독관리")
    data object Notification : SettingsDetail("알림")
    data object Permission : SettingsDetail("권한설정")
    data object Withdraw : SettingsDetail("탈퇴하기")
}

/** MA-01 메인 화면: 실제 AppRestrictionRepository 데이터 사용 (Figma 336-2910) */
@Composable
internal fun MainScreenMA01(
    onAddAppClick: () -> Unit,
    onPermissionClick: () -> Unit = {},
    onDetailClick: (MainAppRestrictionItem) -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    // 제한 앱 목록 + 빈 상태일 때 추천앱을 함께 로드 (플래시 방지: 첫 로드 완료 후에만 UI 표시)
    val state by produceState<HomeRestrictionState?>(
        initialValue = null,
        context,
    ) {
        while (true) {
            val items = withContext(Dispatchers.Default) {
                loadRestrictionItems(context)
            }
            val recommended = if (items.isEmpty()) {
                withContext(Dispatchers.IO) {
                    StatisticsData.loadRecommendedAppForRestriction(context)
                }
            } else null
            value = HomeRestrictionState(items, recommended)
            delay(1000)
        }
    }
    val restrictionItems = state?.items ?: emptyList()
    val recommendedApp = state?.recommendedApp
    val isRestrictionStateReady = state != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MainCommentSection(
            comment = "최근 인스타그램의 사용시간이\n늘어나고 있어요!",
            subtext = "약간의 제한이 필요해요",
            onClick = onStatisticsClick,
        )

        if (isRestrictionStateReady && restrictionItems.isEmpty()) {
            MainAppRestrictionCardEmpty(
                onAddAppClick = onAddAppClick,
                addButtonText = if (recommendedApp != null) "사용제한 앱 추가" else "잠시만 멀어질 앱 추가하기",
                recommendedApp = recommendedApp,
                onRecommendedAppClick = null,
            )
        } else if (restrictionItems.isNotEmpty()) {
            MainAppRestrictionCard(
                apps = restrictionItems,
                onAddAppClick = onAddAppClick,
                onDetailClick = onDetailClick,
                addButtonText = "잠시만 멀어질 앱 추가하기",
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** ms를 HH:MM:SS 형식으로 포맷 */
private fun formatDurationHhMmSs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

/** Calendar.DAY_OF_WEEK(일=1..토=7) → 요일 인덱스(월=0..일=6) */
private fun todayDayIndex(): Int {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 6 else dow - 2
}

private fun parseRepeatDays(repeatDays: String): Set<Int> =
    repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.toSet()

/** 제한 요일 중 다음 제한일까지 남은 일수. 오늘이 제한일이면 0. */
private fun daysUntilNextRestriction(todayIdx: Int, restrictionDayIndices: Set<Int>): Int {
    if (todayIdx in restrictionDayIndices) return 0
    for (d in 1..7) {
        val idx = (todayIdx + d) % 7
        if (idx in restrictionDayIndices) return d
    }
    return 7
}

/** durationWeeks > 0일 때 기준 시각부터 N주가 지났는지 */
private fun isDurationExpired(baselineTimeMs: Long, durationWeeks: Int): Boolean {
    if (durationWeeks <= 0) return false
    val endMs = baselineTimeMs + durationWeeks * 7L * 24 * 60 * 60 * 1000
    return System.currentTimeMillis() >= endMs
}

/** 오늘 하루만: baseline 날짜가 오늘 이전이면 만료 */
private fun isTodayOnlyExpired(baselineTimeMs: Long): Boolean {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return baselineTimeMs < cal.timeInMillis
}

private fun loadRestrictionItems(context: Context): List<MainAppRestrictionItem> {
    val repo = AppRestrictionRepository(context)
    val restrictions = repo.getAll()
    if (restrictions.isEmpty()) return emptyList()

    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    val pauseRepo = PauseRepository(context)
    val visiblePkgs = AppVisibilityRepository(context).getPackagesWithVisibleWindows()
    val todayIdx = todayDayIndex()
    var needsServiceRefresh = false

    val result = restrictions.mapNotNull { restriction ->
        val isTimeSpecified = restriction.blockUntilMs > 0
        if (isTimeSpecified) {
            // 1. 시간지정: 제한 종료 시 목록에서 즉시 제거
            val now = System.currentTimeMillis()
            val remainingMs = restriction.blockUntilMs - now
            if (remainingMs <= 0) {
                repo.delete(restriction.packageName)
                needsServiceRefresh = true
                return@mapNotNull null
            }
            val todayMinutes = (usm?.let {
                UsageStatsUtils.getUsageSinceBaselineMinutes(it, restriction.packageName, restriction.baselineTimeMs, visiblePkgs)
            } ?: 0).toInt().coerceAtLeast(0)
            val isPaused = pauseRepo.isPaused(restriction.packageName)
            val pauseUsedCount = pauseRepo.getTodayCount(restriction.packageName)
            val pauseRemainingCount = pauseRepo.getRemainingCount(restriction.packageName)
            val pauseUntilMs = pauseRepo.getPauseUntilMs(restriction.packageName)
            val pauseLeftMs = (pauseUntilMs - now).coerceAtLeast(0)
            val pauseStartMs = pauseUntilMs - 5 * 60 * 1000
            val pauseElapsedMs = (now - pauseStartMs).coerceAtLeast(0)
            val (usageText, usageLabel) = when {
                isPaused -> formatDurationHhMmSs(pauseElapsedMs) to "일시정지 중"
                else -> "${formatDurationHhMmSs(remainingMs.coerceAtLeast(0))} 후" to "해제"
            }
            MainAppRestrictionItem(
                appName = restriction.appName,
                packageName = restriction.packageName,
                usageText = usageText,
                usageLabel = usageLabel,
                showDetailButton = true,
                limitMinutes = restriction.limitMinutes,
                todayUsageMinutes = todayMinutes,
                restrictionType = RestrictionType.TIME_SPECIFIED,
                blockUntilMs = restriction.blockUntilMs,
                isPaused = isPaused,
                pauseUsedCount = pauseUsedCount,
                pauseRemainingCount = pauseRemainingCount,
                pauseLeftMin = (pauseLeftMs / 60000).toInt(),
                usageTextColor = when {
                    isPaused -> AppColors.Red300
                    else -> AppColors.TextHighlight
                },
                usageLabelColor = AppColors.TextSecondary,
            )
        } else {
            // 일일 사용량: repeatDays 분기
            val repeatDaySet = parseRepeatDays(restriction.repeatDays)

            // 2. 오늘 하루만: 00시 기준 목록에서 제거
            if (repeatDaySet.isEmpty()) {
                if (isTodayOnlyExpired(restriction.baselineTimeMs)) {
                    repo.delete(restriction.packageName)
                    needsServiceRefresh = true
                    return@mapNotNull null
                }
            } else {
                // 3. 매일 반복 / 4. 격일·특정요일: 기간 만료 시 제거
                if (isDurationExpired(restriction.baselineTimeMs, restriction.durationWeeks)) {
                    repo.delete(restriction.packageName)
                    needsServiceRefresh = true
                    return@mapNotNull null
                }
            }

            val usageMs = usm?.let {
                UsageStatsUtils.getUsageSinceBaselineMs(it, restriction.packageName, restriction.baselineTimeMs, visiblePkgs)
            } ?: (DebugTestSettings.debugTodayUsageMinutes?.let { it * 60 * 1000 } ?: 0L)
            val limitMs = restriction.limitMinutes * 60L * 1000L
            val remainingMs = (limitMs - usageMs).coerceAtLeast(0)
            val todayMinutes = (usageMs / 60_000).toInt().coerceAtLeast(0)
            val todaySessionCount = (usm?.let { getTodaySessionCount(it, restriction.packageName) } ?: 0).toInt().coerceAtLeast(0)
            val limitMinutes = restriction.limitMinutes

            // 4. 격일·특정요일: 제한 없는 날 "N일 후 제한 예정" 표시
            val isEveryDay = repeatDaySet.size == 7
            val daysUntil = if (!isEveryDay && repeatDaySet.isNotEmpty()) daysUntilNextRestriction(todayIdx, repeatDaySet) else 0
            val (usageText, usageLabel) = when {
                daysUntil > 0 -> "${daysUntil}일 후 제한 예정" to "예정"
                remainingMs <= 0 -> "사용 가능한 시간 없음" to "남음"
                isEveryDay -> formatDurationHhMmSs(remainingMs) to "남음"
                else -> formatDurationHhMmSs(remainingMs) to "남음"
            }

            MainAppRestrictionItem(
                appName = restriction.appName,
                packageName = restriction.packageName,
                usageText = usageText,
                usageLabel = usageLabel,
                showDetailButton = true,
                limitMinutes = limitMinutes,
                todayUsageMinutes = todayMinutes,
                restrictionType = RestrictionType.DAILY_USAGE,
                usageMinutes = "${todayMinutes}분",
                sessionCount = "${todaySessionCount}회",
                dailyLimitMinutes = "${limitMinutes}분",
                usageTextColor = if (remainingMs <= 0) AppColors.Red300 else AppColors.TextHighlight,
                usageLabelColor = AppColors.TextSecondary,
            )
        }
    }

    if (needsServiceRefresh) {
        val map = repo.toRestrictionMap()
        AppMonitorService.stop(context)
        if (map.isNotEmpty()) AppMonitorService.start(context, map)
    }
    return result
}

private fun getTodaySessionCount(usm: UsageStatsManager, packageName: String): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val events = usm.queryEvents(cal.timeInMillis, System.currentTimeMillis()) ?: return 0
    var count = 0L
    val event = android.app.usage.UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        @Suppress("DEPRECATION")
        if (event.packageName == packageName && event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
            count++
        }
    }
    return count
}

/** MA-02 메인 화면 (Figma 662:2907): 데이터 없을 때 */
@Composable
fun MainScreenMA02(
    onAddAppClick: () -> Unit,
    onStatisticsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MainCommentSection(
            comment = "최근 인스타그램의 사용시간이\n늘어나고 있어요!",
            subtext = "약간의 제한이 필요해요",
            onClick = onStatisticsClick,
        )
        MainAppRestrictionCardEmpty(onAddAppClick = onAddAppClick)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MainFlowHost(
    onAddAppClick: () -> Unit,
    onLogout: () -> Unit,
    isFreeUser: Boolean = true,
    initialPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    onPauseFlowConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    var navIndex by remember { mutableIntStateOf(0) }
    var settingsDetail by remember { mutableStateOf<SettingsDetail?>(null) }
    var showBugReportSheet by remember { mutableStateOf(false) }
    var showTermsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showNotificationOverlay by remember { mutableStateOf(false) }
    var showSubscriptionGuide by remember { mutableStateOf(false) }
    var showAppLimitInfoSheet by remember { mutableStateOf(false) }
    var selectedAppForDetail by remember { mutableStateOf<MainAppRestrictionItem?>(null) }

    // 일시정지 플로우 state
    var showPauseProposalSheet by remember { mutableStateOf(false) }
    var showPauseConfirmSheet by remember { mutableStateOf(false) }
    var showPauseCompleteSheet by remember { mutableStateOf(false) }
    var selectedAppForPause by remember { mutableStateOf<MainAppRestrictionItem?>(null) }

    // 앱 제한 오버레이에서 일시정지 클릭 후 Cole 앱으로 진입 시 1단계(제안)부터 표시
    LaunchedEffect(initialPauseFlowFromOverlay) {
        val pending = initialPauseFlowFromOverlay ?: return@LaunchedEffect
        selectedAppForPause = MainAppRestrictionItem(
            appName = pending.appName,
            packageName = pending.packageName,
            usageText = "",
            usageLabel = "",
            showDetailButton = false,
            limitMinutes = 90, // pauseAvailable (60분 초과) 조건 충족
            blockUntilMs = pending.blockUntilMs,
        )
        showPauseProposalSheet = true
    }

    val navDestinations = listOf(
        NavDestination("홈", R.drawable.ic_nav_home_inactive, R.drawable.ic_nav_home_active),
        NavDestination("챌린지", R.drawable.ic_nav_challenge_inactive, R.drawable.ic_nav_challenge_active),
        NavDestination("통계", R.drawable.ic_nav_stats_inactive, R.drawable.ic_nav_stats_active),
        NavDestination("설정", R.drawable.ic_nav_mypage_inactive, R.drawable.ic_nav_mypage_active),
    )

    // 바텀바 항상 표시 (스크롤 시 숨김 비활성화)
    val scrollToHideConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return Offset.Zero
            }
        }
    }
    // 바텀바 실제 높이 측정 (수동 계산 대신 onGloballyPositioned로 정확히)
    val density = LocalDensity.current
    val navBarInsetBottom = WindowInsets.navigationBars.getBottom(density)
    val navBarPaddingDp = with(density) { navBarInsetBottom.toDp() }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarHeightDp = with(density) { bottomBarHeightPx.toDp() }
    // 시스템 네비바 영역 채움: 프리미엄 배너(#2B2B2B) / 유료(카드 배경)
    val bottomFillColor = if (isFreeUser) Color(0xFF2B2B2B) else AppColors.SurfaceBackgroundCard

    // 바텀바 하단 패딩 (앱바 높이 또는 기본값)
    val contentBottomPadding = when {
        bottomBarHeightDp > 0.dp -> bottomBarHeightDp
        else -> 122.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppColors.SurfaceBackgroundBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.SurfaceBackgroundBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 10.dp)
            ) {
                val onNotificationClick: () -> Unit = {
                    showNotificationOverlay = true
                }
                when {
                    navIndex == 0 -> ColeHeaderHome(
                        logo = painterResource(R.drawable.ic_logo),
                        hasNotification = true,
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 1 -> ColeHeaderTitleWithNotification(
                        title = "챌린지",
                        hasNotification = true,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 2 -> ColeHeaderTitleWithNotification(
                        title = "통계",
                        hasNotification = true,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 3 && settingsDetail != null -> ColeHeaderSub(
                        title = settingsDetail!!.title,
                        backIcon = painterResource(R.drawable.ic_back),
                        onBackClick = {
                            settingsDetail = when (settingsDetail) {
                                SettingsDetail.Withdraw -> SettingsDetail.AccountManage
                                else -> null
                            }
                        },
                        showNotification = true,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 3 -> ColeHeaderTitleWithNotification(
                        title = "설정",
                        hasNotification = true,
                        onNotificationClick = onNotificationClick,
                    )
                    else -> ColeHeaderHome(
                        logo = painterResource(R.drawable.ic_logo),
                        hasNotification = true,
                        onNotificationClick = onNotificationClick,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = contentBottomPadding),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                when (navIndex) {
                    0 -> {
                        Box(modifier = Modifier.weight(1f)) {
                            MainScreenMA01(
                                onAddAppClick = onAddAppClick,
                                onDetailClick = { item ->
                                    selectedAppForDetail = item
                                    showAppLimitInfoSheet = true
                                },
                                onStatisticsClick = { navIndex = 2 },
                            )
                        }
                    }
                    1 -> ChallengeScreen(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollToHideConnection),
                    )
                    2 -> StatisticsScreen(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollToHideConnection),
                    )
                    3 -> {
                        Column(modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(scrollToHideConnection)) {
                            when (settingsDetail) {
                                SettingsDetail.AccountManage -> AccountManageScreen(
                                    onBack = { settingsDetail = null },
                                    onProfileClick = { },
                                    onWithdrawClick = { settingsDetail = SettingsDetail.Withdraw },
                                )
                                SettingsDetail.Subscription -> SubscriptionManageScreen(
                                    onBack = { settingsDetail = null },
                                    onCancelSubscription = {
                                        val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                            setPackage("com.android.vending")
                                        })
                                    },
                                )
                                SettingsDetail.Notification -> NotificationSettingsScreen(
                                    onBack = { settingsDetail = null },
                                )
                                SettingsDetail.Permission -> PermissionSettingsScreen(
                                    onBack = { settingsDetail = null },
                                    onAccessibilityClick = {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    },
                                    onUsageStatsClick = {
                                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    },
                                    onOverlayClick = {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                        context.startActivity(intent)
                                    },
                                )
                                SettingsDetail.Withdraw -> WithdrawConfirmScreen(
                                    onBack = { settingsDetail = SettingsDetail.AccountManage },
                                    onConfirmWithdraw = {
                                        settingsDetail = null
                                        // TODO: 실제 탈퇴 API 호출 후 로그아웃 등 처리
                                    },
                                )
                                null -> MyPageScreen(
                                    onAccountManageClick = { settingsDetail = SettingsDetail.AccountManage },
                                    onSubscriptionManageClick = { settingsDetail = SettingsDetail.Subscription },
                                    onNotificationClick = { settingsDetail = SettingsDetail.Notification },
                                    onPermissionClick = { settingsDetail = SettingsDetail.Permission },
                                    onBugReportClick = { showBugReportSheet = true },
                                    onTermsClick = { showTermsSheet = true },
                                    onPrivacyClick = { showPrivacySheet = true },
                                )
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "준비중",
                                style = AppTypography.HeadingH3.copy(color = AppColors.TextSecondary),
                            )
                        }
                    }
                }
            }
            }

            // 시스템 네비바 영역 채움 (바텀바 숨김 시에도 항상 표시)
            if (navBarInsetBottom > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(navBarPaddingDp)
                        .background(bottomFillColor),
                )
            }
            // ── 바텀바 (항상 표시) ──
            Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .onGloballyPositioned { coordinates ->
                            bottomBarHeightPx = coordinates.size.height
                        },
                ) {
                    ColeBottomNavBar(
                        destinations = navDestinations,
                        selectedIndex = navIndex,
                        onTabSelected = {
                            if (it != navIndex) {
                                navIndex = it
                                if (it != 3) settingsDetail = null
                            }
                        },
                        showPremiumBanner = isFreeUser,
                        onPremiumClick = { if (isFreeUser) showSubscriptionGuide = true },
                    )
                }
        }

        // 진행중인 앱 상세 바텀시트
        if (showAppLimitInfoSheet && selectedAppForDetail != null) {
            val item = selectedAppForDetail!!
            val appIcon = rememberAppIconPainter(item.packageName)
            // 60분 초과일 때만 일시정지 가능
            val canPause = item.limitMinutes > 60

            val dismiss: () -> Unit = {
                showAppLimitInfoSheet = false
                selectedAppForDetail = null
            }

            when {
                // ── 스크린샷3: 시간지정 + 일시정지 진행 중 ──
                item.restrictionType == RestrictionType.TIME_SPECIFIED && item.isPaused -> {
                    val pauseStatusText = if (item.pauseLeftMin > 0) "${item.pauseLeftMin}분 남음 (${item.pauseUsedCount}회차)" else "5분 남음 (${item.pauseUsedCount}회차)"
                    AppLimitInfoBottomSheet(
                        title = "제한 중인 앱",
                        appName = item.appName,
                        appIcon = appIcon,
                        appUsageText = "일시정지 종료 후 이어서 진행됩니다",
                        appUsageLabel = "",
                        appUsageTextColor = AppColors.Red300,
                        summaryRows = listOf(
                            AppLimitSummaryRow("제한 시간", "${item.limitMinutes}분"),
                            AppLimitSummaryRow("남은 시간", "${(item.blockUntilMs - System.currentTimeMillis()).coerceAtLeast(0) / 60000}분"),
                            AppLimitSummaryRow("제한 방식", "시간 지정 제한"),
                            AppLimitSummaryRow("일시정지 사용 여부", pauseStatusText, valueColor = AppColors.Red300),
                        ),
                        primaryButtonText = "닫기",
                        secondaryButtonText = null,
                        onPrimaryClick = dismiss,
                        onDismissRequest = dismiss,
                    )
                }
                // ── 스크린샷1: 시간지정 + 일시정지 미사용 ──
                item.restrictionType == RestrictionType.TIME_SPECIFIED -> {
                    val remainingMin = (item.blockUntilMs - System.currentTimeMillis()).coerceAtLeast(0) / 60000
                    val pauseStatusText = if (item.pauseUsedCount == 0) "미사용" else "${item.pauseRemainingCount}회 남음"
                    AppLimitInfoBottomSheet(
                        title = "제한 중인 앱",
                        appName = item.appName,
                        appIcon = appIcon,
                        appUsageText = "${remainingMin}분 후 제한 해제",
                        appUsageLabel = "",
                        summaryRows = listOf(
                            AppLimitSummaryRow("제한 시간", "${item.limitMinutes}분"),
                            AppLimitSummaryRow("남은 시간", "${remainingMin}분"),
                            AppLimitSummaryRow("제한 방식", "시간 지정 제한"),
                            AppLimitSummaryRow("일시정지 사용 여부", pauseStatusText),
                        ),
                        primaryButtonText = "일시 정지하기",
                        isPrimaryEnabled = canPause,
                        onPrimaryClick = {
                            showAppLimitInfoSheet = false
                            selectedAppForPause = item
                            showPauseProposalSheet = true
                        },
                        secondaryButtonText = "닫기",
                        onSecondaryClick = dismiss,
                        onDismissRequest = dismiss,
                    )
                }
                // ── 스크린샷2: 일일사용량 제한 ──
                item.restrictionType == RestrictionType.DAILY_USAGE -> {
                    AppLimitInfoBottomSheetDaily(
                        title = "제한 중인 앱",
                        appName = item.appName,
                        appIcon = appIcon,
                        usageMinutes = item.usageMinutes,
                        sessionCount = item.sessionCount,
                        summaryRows = listOf(
                            AppLimitSummaryRow("일일 사용량", item.dailyLimitMinutes),
                            AppLimitSummaryRow("현재 사용량", item.usageMinutes),
                            AppLimitSummaryRow("제한 방식", "일일 사용량 제한"),
                        ),
                        onDismissRequest = dismiss,
                        onPrimaryClick = dismiss,
                        primaryButtonText = "닫기",
                    )
                }
            }
        }

        // 일시정지 플로우 바텀시트
        if (showPauseProposalSheet) {
            val pauseItem = selectedAppForPause
            // 일시정지 가능 조건: 제한 시간 60분 초과 & 오늘 남은 횟수 > 0
            val pauseAvailable = pauseItem != null &&
                pauseItem.limitMinutes > 60 &&
                PauseRepository(context).getRemainingCount(pauseItem.packageName) > 0
            AppLimitPauseProposalBottomSheet(
                onDismissRequest = {
                    showPauseProposalSheet = false
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
                onContinueClick = {
                    showPauseProposalSheet = false
                    showPauseConfirmSheet = true
                },
                onBackClick = {
                    showPauseProposalSheet = false
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
                canPause = pauseAvailable,
            )
        }

        if (showPauseConfirmSheet && selectedAppForPause != null) {
            val item = selectedAppForPause!!
            val appIcon = rememberAppIconPainter(item.packageName)
            val remainingMin = (item.blockUntilMs - System.currentTimeMillis()).coerceAtLeast(0) / 60000
            AppLimitPauseConfirmBottomSheet(
                appName = item.appName,
                appIcon = appIcon,
                usageText = "${remainingMin}분 후 제한 해제",
                usageLabel = "",
                onDismissRequest = {
                    showPauseConfirmSheet = false
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
                onBackClick = {
                    showPauseConfirmSheet = false
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
                onPauseClick = {
                    val pauseRepo = PauseRepository(context)
                    pauseRepo.startPause(item.packageName, 5)
                    val pauseUntilMs = pauseRepo.getPauseUntilMs(item.packageName)
                    PauseTimerNotificationService.start(
                        context,
                        item.packageName,
                        item.appName,
                        pauseUntilMs,
                    )
                    showPauseConfirmSheet = false
                    showPauseCompleteSheet = true
                },
            )
        }

        if (showPauseCompleteSheet && selectedAppForPause != null) {
            val item = selectedAppForPause!!
            val appIcon = rememberAppIconPainter(item.packageName)
            val remaining = PauseRepository(context).getRemainingCount(item.packageName)
            AppLimitPauseCompleteBottomSheet(
                appName = item.appName,
                appIcon = appIcon,
                remainingChances = remaining,
                onDismissRequest = {
                    showPauseCompleteSheet = false
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
                onLaunchAppClick = {
                    showPauseCompleteSheet = false
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(item.packageName)
                    launchIntent?.let { context.startActivity(it) }
                    selectedAppForPause = null
                    onPauseFlowConsumed()
                },
            )
        }
    }

    // 알림내역 페이지 오버레이 (헤더 알림 아이콘 탭 시)
    if (showNotificationOverlay) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.SurfaceBackgroundBackground),
        ) {
            NotificationHistoryScreen(
                items = sampleNotificationHistoryItems(
                    DebugTestSettings.debugNotificationHistoryCount ?: 0
                ),
                onBack = { showNotificationOverlay = false },
                onNotificationSettingsClick = null,
            )
        }
    }
    // 버그 신고 바텀시트
    if (showBugReportSheet) {
        val scope = rememberCoroutineScope()
        BugReportBottomSheet(
            onDismiss = { showBugReportSheet = false },
            onSubmit = { content ->
                scope.launch {
                    try {
                        FirebaseFunctions.getInstance()
                            .getHttpsCallable("submitBugReport")
                            .call(hashMapOf("content" to content))
                            .await()
                        showBugReportSheet = false
                        android.widget.Toast.makeText(
                            context,
                            "버그 신고가 등록되었어요. 감사해요!",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                    } catch (e: Exception) {
                        val msg = when (e) {
                            is FirebaseFunctionsException -> when (e.code) {
                                FirebaseFunctionsException.Code.NOT_FOUND -> "함수가 없어요. Firebase Functions를 배포해주세요. (firebase deploy --only functions)"
                                FirebaseFunctionsException.Code.UNAVAILABLE -> "서버를 사용할 수 없어요. 인터넷 연결을 확인해주세요."
                                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> "Firebase Blaze 플랜이 필요해요."
                                else -> "전송 실패: ${e.message}"
                            }
                            else -> "전송에 실패했어요. 잠시 후 다시 시도해주세요."
                        }
                        Log.e("Cole", "버그신고 전송 실패", e)
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                        showBugReportSheet = false
                    }
                }
            },
        )
    }
    // 이용약관 바텀시트
    if (showTermsSheet) {
        TermsPolicyBottomSheet(onDismiss = { showTermsSheet = false })
    }
    // 개인정보처리방침 바텀시트
    if (showPrivacySheet) {
        PrivacyPolicyBottomSheet(onDismiss = { showPrivacySheet = false })
    }

    // 구독 가이드 오버레이
    if (showSubscriptionGuide) {
        SubscriptionGuideScreen(
            onClose = { showSubscriptionGuide = false },
            onSubscribeClick = { isAnnual ->
                showSubscriptionGuide = false
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    }
}