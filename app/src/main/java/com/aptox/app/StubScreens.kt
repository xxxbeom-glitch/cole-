package com.aptox.app

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import com.aptox.app.ui.components.AptoxToast
import com.aptox.app.ui.components.LocalBottomBarHeight
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.concurrent.TimeUnit
import java.util.Calendar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aptox.app.usage.UsageStatsLocalRepository

/** 주간 통계 탭 진입 시 7일치 미만이면 표시 */
private const val WEEKLY_STATS_NEED_DATA_TOAST =
    "최소 7일치의 데이터가 누적 된 후 보실 수 있어요"

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

/** 홈 화면 제한 앱 영역 상태 (items + top3Apps 동시 로드로 플래시 방지) */
private data class HomeRestrictionState(
    val items: List<MainAppRestrictionItem>,
    /** AI 분류 카테고리 중 가장 많이 쓴 앱 상위 3개 (제한 진행 중인 앱 없을 때 표시) */
    val top3Apps: List<StatisticsData.StatsAppItem>,
)

/** rawScore(8~32)를 SelfTestResultType으로 변환 */
fun rawScoreToResultType(rawScore: Int): SelfTestResultType = when {
    rawScore <= 13 -> SelfTestResultType.HEALTH
    rawScore <= 19 -> SelfTestResultType.GOOD
    rawScore <= 25 -> SelfTestResultType.CAUTION
    else -> SelfTestResultType.DANGER
}

/**
 * 스플래시 화면 (Figma 409-6664)
 * - 로고 + 로딩 바 (가짜 진행, AI 분류는 백그라운드에서 100% 수행)
 * - 0 → 30% 천천히 → 잠시 멈춤 → 30 → 85% → 잠시 멈춤 → 100% → onFinish
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val app = context.applicationContext as? AptoxApplication

        // 1. 0 → 30% 천천히 (가짜 진행)
        for (i in 1..30) {
            progress = i / 100f
            delay(50)
        }
        delay(400) // 멈춤

        // 2. 30 → 85%
        for (i in 31..85) {
            progress = i / 100f
            delay(30)
        }
        delay(400) // 멈춤

        // 3. 100% → 화면 전환
        progress = 1f
        delay(150)

        // 4. 백그라운드에서 AI 카테고리 분류 100% 수행 (스플래시 이후 화면 진행과 병렬)
        app?.applicationScope?.launch {
            runCatching {
                val preload = AppDataPreloadRepository(context)
                val installedApps = withContext(Dispatchers.IO) { preload.loadInstalledApps() }
                withContext(Dispatchers.IO) { preload.classifyAndCacheApps(installedApps) }
            }
        }

        onFinish()
    }

    SplashWithLoadingBar(
        progress = progress,
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary300)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    )
}

@Composable
private fun SplashWithLoadingBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "splash_progress",
    )
    val progressPercent = (animatedProgress * 100).toInt().coerceIn(0, 100)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        // Figma 409-6664: 로고 + 로딩 블록 (gap 26dp), 로딩 블록 내 퍼센티지 위·바 아래
        Column(
            modifier = Modifier.widthIn(max = 206.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = "aptox.",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 73.dp),
                contentScale = ContentScale.Fit,
            )
            Column(
                modifier = Modifier.width(121.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${progressPercent}%",
                    style = AppTypography.Caption1.copy(color = Color.White),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AppColors.Primary600),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(AppColors.Primary200),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        // 하단: 앱 버전 + 필명 (Figma 1127-5375)
        Column(
            modifier = Modifier.padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "V ${BuildConfig.VERSION_NAME}",
                style = AppTypography.Caption2.copy(color = Color.White.copy(alpha = 0.9f)),
            )
            Text(
                text = "Break the break",
                style = AppTypography.Caption2.copy(color = Color.White.copy(alpha = 0.9f)),
            )
        }
    }
}

/**
 * 앱 소개 온보딩 (권한 안내 직후 1회).
 * Placed before 이름·자가테스트 Ver2; copy/레이아웃은 추후 기획 반영.
 */
@Composable
fun AppIntroOnboardingScreen(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackClick)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        AptoxPrimaryButton(
            text = "다음",
            onClick = onNextClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
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
    /** 시간 지정 제한: 제한 시작 시각 (ms). 0이면 즉시 시작 */
    val startTimeMs: Long = 0L,
    /** 오늘 일시정지 사용 횟수 */
    val pauseUsedCount: Int = 0,
    /** 오늘 일시정지 남은 횟수 */
    val pauseRemainingCount: Int = 2,
    /** 일시정지 중일 때 남은 분 */
    val pauseLeftMin: Int = 0,
)

/** ms를 "N시간 N분" 형식으로 포맷 */
private fun formatUsageHoursMinutes(ms: Long): String {
    val totalMin = (ms / 60_000).toInt().coerceAtLeast(0)
    val hours = totalMin / 60
    val minutes = totalMin % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        else -> "${minutes}분"
    }
}

/**
 * Figma 1104-5640: 오늘 스마트폰 사용시간 카드
 * - 24시간 기준 게이지 (usageMs/86400000)
 * - 오늘 00:00~현재 실시간 기기 사용량 반영, 매일 자정 리셋
 */
@Composable
private fun TodaySmartphoneUsageCard(
    usageMs: Long,
    modifier: Modifier = Modifier,
) {
    val progress = (usageMs / (24.0 * 60 * 60 * 1000)).toFloat().coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
            .clip(MainCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "오늘 스마트폰 사용시간",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
            )
            Text(
                text = formatUsageHoursMinutes(usageMs),
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.Grey150),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AppColors.Primary300),
                contentAlignment = Alignment.Center,
            ) {}
        }
    }
}

/** MA-01: 코멘트 + 서브텍스트 + > 통계 이동 (Figma 932:9099, 662:2907) */
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

/** 홈 데이터 없을 때: 지난 일주일 top3 앱 카드 (Figma 662:2907) */
@Composable
private fun MainHomeDataEmptyCard(
    greetingTitle: String,
    greetingSubtext: String,
    top3Apps: List<StatisticsData.StatsAppItem>,
    onAddAppClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit,
    onTimeSpecifiedClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit = {},
    onStatisticsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentBetweenGreetingAndTop3: @Composable () -> Unit = {},
) {
    // 제한 방식 선택 바텀시트 상태
    var showRestrictionTypeSheet by remember { mutableStateOf(false) }
    // 바텀시트에서 선택 후 전달할 앱 정보 (null = 하단 "사용제한 앱 추가" 버튼 경로)
    var pendingAppInfo by remember { mutableStateOf<com.aptox.app.model.SelectedAppInfo?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MainCommentSection(
            comment = greetingTitle,
            subtext = greetingSubtext,
            onClick = onStatisticsClick,
        )
        contentBetweenGreetingAndTop3()
        // 카드 내부: 타이틀↔서브 12dp, 서브↔앱리스트 18dp, 앱행 간 12dp, 앱리스트↔버튼 18dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
                .clip(MainCardShape)
                .background(AppColors.SurfaceBackgroundCard)
                .padding(start = 16.dp, top = 26.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp), // 앱리스트 ↔ 버튼
        ) {
            if (top3Apps.isNotEmpty()) {
                Column {
                    Text(
                        text = "최근 7일 동안 가장 많은 시간을 사용한 앱 순으로 정렬했어요",
                        style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "이 앱들이 하루 중 꽤 많은 시간을 차지하고 있어요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        top3Apps.forEach { app ->
                            MainHomeTopAppRow(
                                app = app,
                                onAddRestrictionClick = {
                                    pendingAppInfo = com.aptox.app.model.SelectedAppInfo(
                                        appName = app.name,
                                        packageName = app.packageName,
                                    )
                                    showRestrictionTypeSheet = true
                                },
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "일주일간 사용 데이터를 확인해보세요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            AptoxAddAppButton(
                text = "사용제한 앱 추가",
                icon = painterResource(R.drawable.ic_add_circle),
                onClick = {
                    pendingAppInfo = null
                    showRestrictionTypeSheet = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
            )
        }
    }

    // 제한 방식 선택 바텀시트
    if (showRestrictionTypeSheet) {
        RestrictionTypeSelectBottomSheet(
            onDailyLimitClick = {
                showRestrictionTypeSheet = false
                onAddAppClick(pendingAppInfo)
            },
            onTimeSpecifiedClick = {
                showRestrictionTypeSheet = false
                onTimeSpecifiedClick(pendingAppInfo)
            },
            onDismissRequest = { showRestrictionTypeSheet = false },
        )
    }
}

/** 지난 일주일 top3 앱 행 (Figma 1397:4494 — 앱명 + 하루 평균 사용, 우측 제한 앱 추가) */
@Composable
private fun MainHomeTopAppRow(
    app: StatisticsData.StatsAppItem,
    onAddRestrictionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dailyAvgMin = (app.usageMs / 7 / 60_000).toInt().coerceAtLeast(0)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBox(
                appIcon = rememberAppIconPainter(app.packageName),
                size = 56.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = app.name,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "하루 평균 ",
                        style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                    )
                    Text(
                        text = "${java.text.DecimalFormat("#,###").format(dailyAvgMin)}분 사용",
                        style = AppTypography.Caption2.copy(color = AppColors.Red300),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        AptoxSecondaryButton(
            text = "제한 앱 추가",
            onClick = onAddRestrictionClick,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.appName,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
            AptoxAddAppButton(
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
            val dailyAvgMin = (recommendedApp.usageMs / 7 / 60_000).toInt().coerceAtLeast(0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .then(
                            if (onRecommendedAppClick != null) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { onRecommendedAppClick(recommendedApp.packageName) }
                            } else {
                                Modifier
                            },
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconBox(
                        appIcon = rememberAppIconPainter(recommendedApp.packageName),
                        size = 56.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = recommendedApp.name,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "하루 평균 ${java.text.DecimalFormat("#,###").format(dailyAvgMin)}분 사용",
                            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                AptoxSecondaryButton(
                    text = "제한 앱 추가",
                    onClick = onAddAppClick,
                )
            }
        }
        if (recommendedApp == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                AptoxAddAppButton(
                    text = addButtonText,
                    icon = painterResource(R.drawable.ic_add_circle),
                    onClick = onAddAppClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                )
            }
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
        AptoxOutlinedTextButton(text = "내 스마트폰 의존도는 몇점일까", onClick = onPrimaryClick)
    }
}

private sealed class SettingsDetail(val title: String) {
    data object AccountManage : SettingsDetail("계정관리")
    data object AppRestrictionHistory : SettingsDetail("앱 사용제한 기록")
    data class AppRestrictionHistoryDetail(val packageName: String, val appName: String) : SettingsDetail(appName)
    data object Subscription : SettingsDetail("구독관리")
    data object Notification : SettingsDetail("알림")
    data object Permission : SettingsDetail("권한설정")
    data object Withdraw : SettingsDetail("탈퇴하기")
    data object BugReport : SettingsDetail("버그 신고")
}

/** MA-01 메인 화면: 실제 AppRestrictionRepository 데이터 사용 (Figma 336-2910, 1104-5627) */
@Composable
internal fun MainScreenMA01(
    onAddAppClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit,
    onTimeSpecifiedClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onDetailClick: (MainAppRestrictionItem) -> Unit = {},
    onStatisticsClick: () -> Unit = {},
    restrictionRefreshKey: Int = 0,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(context))
    val greeting by homeViewModel.greeting.collectAsState()

    // 제한 앱 목록 + 빈 상태일 때 top3Apps 함께 로드 (플래시 방지)
    val state by produceState<HomeRestrictionState?>(
        initialValue = null,
        context,
        restrictionRefreshKey,
    ) {
        while (true) {
            kotlin.runCatching {
                val items = withContext(Dispatchers.Default) {
                    loadRestrictionItems(context)
                }
                val top3 = if (items.isEmpty()) {
                    withContext(Dispatchers.IO) {
                        StatisticsData.loadTop3AppsFromAiCategories(context)
                    }
                } else emptyList()
                value = HomeRestrictionState(items, top3)
            }.onFailure { e ->
                Log.w("MainScreenMA01", "loadRestrictionItems 실패", e)
            }
            delay(1000)
        }
    }
    val restrictionItems = state?.items ?: emptyList()
    val top3Apps = state?.top3Apps ?: emptyList()
    val isRestrictionStateReady = state != null

    // 오늘 스마트폰 사용시간 (실시간, 매일 자정 리셋)
    val todayUsageMs by produceState(initialValue = 0L, context) {
        while (true) {
            kotlin.runCatching {
                value = withContext(Dispatchers.IO) {
                    StatisticsData.getTodayTotalUsageMs(context)
                }
            }.onFailure { e -> Log.w("MainScreenMA01", "getTodayTotalUsageMs 실패", e) }
            delay(5000) // 5초마다 갱신
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (isRestrictionStateReady && restrictionItems.isEmpty()) {
            MainHomeDataEmptyCard(
                greetingTitle = greeting.title,
                greetingSubtext = greeting.subtext,
                top3Apps = top3Apps,
                onAddAppClick = { app -> onAddAppClick(app) },
                onTimeSpecifiedClick = { app -> onTimeSpecifiedClick(app) },
                onStatisticsClick = onStatisticsClick,
                contentBetweenGreetingAndTop3 = { TodaySmartphoneUsageCard(usageMs = todayUsageMs) },
            )
        } else if (restrictionItems.isNotEmpty()) {
            var showRestrictionTypeSheet by remember { mutableStateOf(false) }

            MainCommentSection(
                comment = "최근 인스타그램의 사용시간이\n늘어나고 있어요!",
                subtext = "약간의 제한이 필요해요",
                onClick = onStatisticsClick,
            )
            TodaySmartphoneUsageCard(usageMs = todayUsageMs)
            MainAppRestrictionCard(
                apps = restrictionItems,
                onAddAppClick = { showRestrictionTypeSheet = true },
                onDetailClick = onDetailClick,
                addButtonText = "사용제한 앱 추가",
            )

            if (showRestrictionTypeSheet) {
                RestrictionTypeSelectBottomSheet(
                    onDailyLimitClick = {
                        showRestrictionTypeSheet = false
                        onAddAppClick(null)
                    },
                    onTimeSpecifiedClick = {
                        showRestrictionTypeSheet = false
                        onTimeSpecifiedClick(null)
                    },
                    onDismissRequest = { showRestrictionTypeSheet = false },
                )
            }
        } else {
            MainCommentSection(
                comment = greeting.title,
                subtext = "데이터를 불러오는 중이에요",
                onClick = onStatisticsClick,
            )
            TodaySmartphoneUsageCard(usageMs = todayUsageMs)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private const val TIME_SPEC_ONE_DAY_MS = 24L * 60 * 60 * 1000

private fun isSameCalendarDay(aMs: Long, bMs: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = aMs }
    val cb = Calendar.getInstance().apply { timeInMillis = bMs }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
        ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/** timeMs가 now 기준 '내일' 달력 날짜인지 */
private fun isCalendarTomorrow(timeMs: Long, nowMs: Long): Boolean {
    val calTarget = Calendar.getInstance().apply { timeInMillis = timeMs }
    val calTomorrow = Calendar.getInstance().apply {
        timeInMillis = nowMs
        add(Calendar.DAY_OF_YEAR, 1)
    }
    return calTarget.get(Calendar.YEAR) == calTomorrow.get(Calendar.YEAR) &&
        calTarget.get(Calendar.DAY_OF_YEAR) == calTomorrow.get(Calendar.DAY_OF_YEAR)
}

/**
 * 시간 지정 제한의 (시작, 종료)를 now 이후 유효한 구간으로 맞춤 (일 단위 반복).
 */
private fun rollToNextTimeSpecifiedWindow(startTimeMs: Long, blockUntilMs: Long, now: Long): Pair<Long, Long> {
    var s = startTimeMs
    var e = blockUntilMs
    while (e <= now) {
        s += TIME_SPEC_ONE_DAY_MS
        e += TIME_SPEC_ONE_DAY_MS
    }
    return s to e
}

/**
 * 시간 지정 제한 상태 한 줄 문구 (카드·자세히 보기 공통).
 * - 제한 중: "오전/오후 H시( …분)까지 제한 중" (종료 시각만)
 * - 시작 전(당일): "오전/오후 H시부터 제한"
 * - 시작 전(다음날 등): "내일 오전/오후 H시부터 제한" (내일이면), 그 외는 "오전/오후 H시부터 제한"
 */
private fun timeSpecifiedStatusLine(startTimeMs: Long, blockUntilMs: Long, now: Long): String {
    val (s, e) = rollToNextTimeSpecifiedWindow(startTimeMs, blockUntilMs, now)
    val startStr = KoreanTimeFormat.formatHourClock(s)
    val endStr = KoreanTimeFormat.formatHourClock(e)
    return when {
        now >= s && now < e -> "${endStr}까지 제한 중"
        now < s -> {
            when {
                isSameCalendarDay(now, s) -> "${startStr}부터 제한"
                isCalendarTomorrow(s, now) -> "내일 ${startStr}부터 제한"
                else -> "${startStr}부터 제한"
            }
        }
        else -> "내일 ${startStr}부터 제한"
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

    val result = restrictions.mapNotNull { restriction ->
        val isTimeSpecified = restriction.startTimeMs > 0
        if (isTimeSpecified) {
            val now = System.currentTimeMillis()
            // 1. 시간지정: blockUntilMs가 지났으면 다음날 같은 시각으로 갱신 후 재조회
            //    (매일 반복 — 삭제하지 않음)
            val restriction = if (restriction.blockUntilMs <= now) {
                repo.renewExpiredTimeSpecifiedRestrictions()
                repo.getAll().find { it.packageName == restriction.packageName } ?: return@mapNotNull null
            } else restriction
            val remainingMs = restriction.blockUntilMs - now
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

            // 시간 지정 상태 문구: roll 후 구간 기준 (카드·자세히 보기와 동일)
            val (winStart, winEnd) = rollToNextTimeSpecifiedWindow(
                restriction.startTimeMs,
                restriction.blockUntilMs,
                now,
            )
            val isBeforeStart = !isPaused && now < winStart
            val isInRestriction = !isPaused && now >= winStart && now < winEnd

            val (usageText, usageLabel, textColor) = when {
                isPaused -> Triple(
                    formatDurationHhMmSs(pauseElapsedMs),
                    "일시정지 중",
                    AppColors.Red300,
                )
                else -> Triple(
                    timeSpecifiedStatusLine(restriction.startTimeMs, restriction.blockUntilMs, now),
                    "",
                    AppColors.TextHighlight,
                )
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
                startTimeMs = restriction.startTimeMs,
                isPaused = isPaused,
                pauseUsedCount = pauseUsedCount,
                pauseRemainingCount = pauseRemainingCount,
                pauseLeftMin = (pauseLeftMs / 60000).toInt(),
                usageTextColor = textColor,
                usageLabelColor = if (isPaused || isBeforeStart || isInRestriction) AppColors.TextSecondary else null,
            )
        } else {
            // 일일 사용량: repeatDays 분기
            val repeatDaySet = parseRepeatDays(restriction.repeatDays)

            // 2. 오늘 하루만: 00시 기준 목록에서 제거
            if (repeatDaySet.isEmpty()) {
                if (isTodayOnlyExpired(restriction.baselineTimeMs)) {
                    RestrictionDeleteHelper.deleteRestrictedApp(context, restriction.packageName, logRelease = false)
                    return@mapNotNull null
                }
            } else {
                // 3. 매일 반복 / 4. 격일·특정요일: 기간 만료 시 제거
                if (isDurationExpired(restriction.baselineTimeMs, restriction.durationWeeks)) {
                    RestrictionDeleteHelper.deleteRestrictedApp(context, restriction.packageName, logRelease = false)
                    return@mapNotNull null
                }
            }

            // 일일 사용량은 수동 타이머(카운트 시작/정지) 기반. ManualTimerRepository 사용.
            // UsageStatsUtils 대신 ManualTimerRepository 사용 시 카운트 중지 후 카드 시간이 정지함.
            val timerRepo = ManualTimerRepository(context)
            val usageMs = timerRepo.getTodayUsageMs(restriction.packageName)
            val limitMs = restriction.limitMinutes * 60L * 1000L
            val remainingMs = (limitMs - usageMs).coerceAtLeast(0)
            val todayMinutes = (usageMs / 60_000).toInt().coerceAtLeast(0)
            val todaySessionCount = (usm?.let { getTodaySessionCount(it, restriction.packageName) } ?: 0).toInt().coerceAtLeast(0)
            val limitMinutes = restriction.limitMinutes
            val isCountActive = timerRepo.isSessionActive(restriction.packageName)

            // 4. 격일·특정요일: 제한 없는 날 "N일 후 제한 예정" 표시
            val isEveryDay = repeatDaySet.size == 7
            val daysUntil = if (!isEveryDay && repeatDaySet.isNotEmpty()) daysUntilNextRestriction(todayIdx, repeatDaySet) else 0
            val (usageText, usageLabel) = when {
                daysUntil > 0 -> "${daysUntil}일 후 제한 예정" to "예정"
                remainingMs <= 0 -> "사용 가능한 시간 없음" to ""
                isCountActive -> formatDurationHhMmSs(remainingMs) to "남음"  // remainingMs = 한도에서 0으로 (카운트다운)
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
    onAddAppClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit,
    onTimeSpecifiedClick: (com.aptox.app.model.SelectedAppInfo?) -> Unit = {},
    onLogout: () -> Unit,
    isFreeUser: Boolean = true,
    initialPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    onPauseFlowConsumed: () -> Unit = {},
    /** 일일 사용량 제한 완료 후 "카운트 시작하기" 탭 시 자동으로 바텀시트를 열 packageName */
    initialAutoOpenPackage: String? = null,
    onAutoOpenConsumed: () -> Unit = {},
    /** 주간 리포트/목표 달성 알림 탭 시 열 탭 인덱스 (1=챌린지, 2=통계) */
    initialNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository() }
    val firebaseAnalytics = remember {
        FirebaseAnalytics.getInstance(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var accountRefreshKey by remember { mutableIntStateOf(0) }
    val currentUserInfo by produceState<AuthRepository.CurrentUserInfo?>(initialValue = null, accountRefreshKey) {
        value = authRepository.getCurrentUserInfo()
    }
    var navIndex by remember { mutableIntStateOf(0) }
    /** 챌린지(Firestore 뱃지)는 로그인 사용자만 — 비로그인 시 탭·딥링크 진입 차단 */
    var firebaseAuthUid by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { a ->
            firebaseAuthUid = a.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    val isLoggedIn = firebaseAuthUid != null
    var settingsDetail by remember { mutableStateOf<SettingsDetail?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    /** AptoxToast 동일 문구 연속 표시 시 애니·자동닫힘 재실행용 */
    var toastReplayKey by remember { mutableIntStateOf(0) }
    /** 7일치 이상 사용 기록이 있을 때만 통계 탭 진입 허용 (null=로딩 중) */
    var weeklyStatsEligible by remember { mutableStateOf<Boolean?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        weeklyStatsEligible = withContext(Dispatchers.IO) {
            UsageStatsLocalRepository(context).getDaysWithDataCountBlocking() >= StatisticsData.MIN_DAYS_FOR_WEEKLY
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    weeklyStatsEligible = withContext(Dispatchers.IO) {
                        UsageStatsLocalRepository(context).getDaysWithDataCountBlocking() >= StatisticsData.MIN_DAYS_FOR_WEEKLY
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tryNavigateToStatisticsTab: () -> Unit = {
        when (weeklyStatsEligible) {
            false -> {
                toastReplayKey += 1
                toastMessage = WEEKLY_STATS_NEED_DATA_TOAST
            }
            true -> {
                navIndex = 2
                settingsDetail = null
            }
            null -> {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        UsageStatsLocalRepository(context).getDaysWithDataCountBlocking() >= StatisticsData.MIN_DAYS_FOR_WEEKLY
                    }
                    weeklyStatsEligible = ok
                    if (ok) {
                        navIndex = 2
                        settingsDetail = null
                    } else {
                        toastReplayKey += 1
                        toastMessage = WEEKLY_STATS_NEED_DATA_TOAST
                    }
                }
            }
        }
    }
    var showTermsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showNotificationOverlay by remember { mutableStateOf(false) }
    var hasUnreadNotifications by remember { mutableStateOf(false) }

    LaunchedEffect(firebaseAuthUid) {
        if (firebaseAuthUid != null) {
            NotificationRepository(context).hasUnreadNotificationsFlow(firebaseAuthUid).collect {
                hasUnreadNotifications = it
            }
        } else {
            hasUnreadNotifications = false
        }
    }
    var showSubscriptionGuide by remember { mutableStateOf(false) }
    var showAppLimitInfoSheet by remember { mutableStateOf(false) }
    var selectedAppForDetail by remember { mutableStateOf<MainAppRestrictionItem?>(null) }
    var restrictionRefreshKey by remember { mutableIntStateOf(0) }
    /** 필수 권한(사용정보·오버레이·접근성) 미충족 시 앱 제한 추가 대신 표시 */
    var showRequiredPermissionDialog by remember { mutableStateOf(false) }

    // 일시정지 플로우 state
    var showPauseProposalSheet by remember { mutableStateOf(false) }
    var showPauseConfirmSheet by remember { mutableStateOf(false) }
    var showPauseCompleteSheet by remember { mutableStateOf(false) }
    var selectedAppForPause by remember { mutableStateOf<MainAppRestrictionItem?>(null) }

    // 일일 사용량 제한 완료 후 "카운트 시작하기" 탭 시 해당 앱 바텀시트 자동 오픈
    LaunchedEffect(initialAutoOpenPackage) {
        val pkg = initialAutoOpenPackage ?: return@LaunchedEffect
        onAutoOpenConsumed()
        val restrictions = AppRestrictionRepository(context).getAll()
        val restriction = restrictions.firstOrNull { it.packageName == pkg } ?: return@LaunchedEffect
        selectedAppForDetail = MainAppRestrictionItem(
            appName = restriction.appName,
            packageName = restriction.packageName,
            usageText = "",
            usageLabel = "",
            showDetailButton = true,
            limitMinutes = restriction.limitMinutes,
            restrictionType = RestrictionType.DAILY_USAGE,
        )
        showAppLimitInfoSheet = true
    }

    // 주간 리포트/목표 달성 알림 탭 시 해당 탭으로 이동
    LaunchedEffect(initialNavIndex, isLoggedIn) {
        val idx = initialNavIndex ?: return@LaunchedEffect
        if (idx in 1..3) {
            if (idx == 1 && !isLoggedIn) {
                toastReplayKey += 1
                toastMessage = "로그인 후 진행 가능합니다"
                onNavIndexConsumed()
                return@LaunchedEffect
            }
            if (idx == 2) {
                val ok = withContext(Dispatchers.IO) {
                    UsageStatsLocalRepository(context).getDaysWithDataCountBlocking() >= StatisticsData.MIN_DAYS_FOR_WEEKLY
                }
                weeklyStatsEligible = ok
                if (!ok) {
                    toastReplayKey += 1
                    toastMessage = WEEKLY_STATS_NEED_DATA_TOAST
                    onNavIndexConsumed()
                    return@LaunchedEffect
                }
            }
            navIndex = idx
            onNavIndexConsumed()
        }
    }

    // 챌린지 탭에 있는데 로그아웃한 경우 홈으로
    LaunchedEffect(firebaseAuthUid, navIndex) {
        if (firebaseAuthUid == null && navIndex == 1) {
            navIndex = 0
        }
    }

    // 앱 제한 오버레이에서 일시정지 클릭 후 aptox 앱으로 진입 시 1단계(제안)부터 표시
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

    val onAddAppClickGuarded: (com.aptox.app.model.SelectedAppInfo?) -> Unit = { app ->
        if (context.areRequiredAppPermissionsGranted()) {
            onAddAppClick(app)
        } else {
            showRequiredPermissionDialog = true
        }
    }

    val onTimeSpecifiedClickGuarded: (com.aptox.app.model.SelectedAppInfo?) -> Unit = { app ->
        if (context.areRequiredAppPermissionsGranted()) {
            onTimeSpecifiedClick(app)
        } else {
            showRequiredPermissionDialog = true
        }
    }

    // 바텀바 하단 패딩 (앱바 높이 또는 기본값)
    val contentBottomPadding = when {
        bottomBarHeightDp > 0.dp -> bottomBarHeightDp
        else -> 122.dp
    }

    CompositionLocalProvider(LocalBottomBarHeight provides bottomBarHeightDp) {
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
                    navIndex == 0 -> AptoxHeaderHome(
                        logo = painterResource(R.drawable.ic_logo),
                        hasNotification = hasUnreadNotifications,
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 1 -> AptoxHeaderTitleWithNotification(
                        title = "챌린지",
                        hasNotification = hasUnreadNotifications,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 2 -> AptoxHeaderTitleWithNotification(
                        title = "통계",
                        hasNotification = hasUnreadNotifications,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 3 && settingsDetail != null -> AptoxHeaderSub(
                        title = settingsDetail!!.title,
                        backIcon = painterResource(R.drawable.ic_back),
                        onBackClick = {
                            settingsDetail = when (val d = settingsDetail) {
                                SettingsDetail.Withdraw -> SettingsDetail.AccountManage
                                is SettingsDetail.AppRestrictionHistoryDetail -> SettingsDetail.AppRestrictionHistory
                                else -> null
                            }
                        },
                        showNotification = true,
                        hasNotification = hasUnreadNotifications,
                        modifier = Modifier.fillMaxWidth(),
                        onNotificationClick = onNotificationClick,
                    )
                    navIndex == 3 -> AptoxHeaderTitleWithNotification(
                        title = "설정",
                        hasNotification = hasUnreadNotifications,
                        onNotificationClick = onNotificationClick,
                    )
                    else -> AptoxHeaderHome(
                        logo = painterResource(R.drawable.ic_logo),
                        hasNotification = hasUnreadNotifications,
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
                                onAddAppClick = onAddAppClickGuarded,
                                onTimeSpecifiedClick = onTimeSpecifiedClickGuarded,
                                onDetailClick = { item ->
                                    selectedAppForDetail = item
                                    showAppLimitInfoSheet = true
                                },
                                onStatisticsClick = tryNavigateToStatisticsTab,
                                restrictionRefreshKey = restrictionRefreshKey,
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
                                    currentUserInfo = currentUserInfo,
                                    onLogout = {
                                        scope.launch {
                                            authRepository.signOut()
                                                .onSuccess { withContext(Dispatchers.Main.immediate) { accountRefreshKey++ } }
                                                .onFailure { e ->
                                                    toastMessage = "로그아웃 실패: ${e.message}"
                                                }
                                        }
                                    },
                                    onGoogleClick = {
                                        scope.launch {
                                            authRepository.signInWithGoogle(context)
                                                .onSuccess { withContext(Dispatchers.Main.immediate) { accountRefreshKey++ } }
                                                .onFailure { e ->
                                                    if (LoginAnalytics.isGoogleLoginCancelled(e)) {
                                                        LoginAnalytics.logLoginCancelled(
                                                            firebaseAnalytics,
                                                            "google",
                                                            "settings_account",
                                                        )
                                                        toastReplayKey += 1
                                                        toastMessage = "로그인을 취소했습니다"
                                                    } else {
                                                        LoginAnalytics.logLoginFailed(
                                                            firebaseAnalytics,
                                                            "google",
                                                            e.message ?: "구글 로그인 실패",
                                                        )
                                                        toastMessage = "구글 로그인 실패: ${e.message}"
                                                    }
                                                }
                                        }
                                    },
                                    onWithdrawClick = { settingsDetail = SettingsDetail.Withdraw },
                                )
                                SettingsDetail.AppRestrictionHistory -> AppRestrictionHistoryScreen(
                                    userId = firebaseAuthUid,
                                    onBack = { settingsDetail = null },
                                    onItemClick = { pkg, name ->
                                        settingsDetail = SettingsDetail.AppRestrictionHistoryDetail(pkg, name)
                                    },
                                )
                                is SettingsDetail.AppRestrictionHistoryDetail -> AppRestrictionHistoryDetailScreen(
                                    packageName = (settingsDetail as SettingsDetail.AppRestrictionHistoryDetail).packageName,
                                    appName = (settingsDetail as SettingsDetail.AppRestrictionHistoryDetail).appName,
                                    userId = firebaseAuthUid,
                                    onBack = { settingsDetail = SettingsDetail.AppRestrictionHistory },
                                )
                                // 구독 관리 — 유료 구독 플랜 없으므로 비활성화
                                // SettingsDetail.Subscription -> SubscriptionManageScreen(
                                //     onBack = { settingsDetail = null },
                                //     onCancelSubscription = {
                                //         val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
                                //         context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                //             setPackage("com.android.vending")
                                //         })
                                //     },
                                // )
                                SettingsDetail.Subscription -> { settingsDetail = null }
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
                                )
                                SettingsDetail.BugReport -> BugReportScreen(
                                    onBack = { settingsDetail = null },
                                    onSubmit = { title, content, imageUris ->
                                        Log.d("Aptox", "버그신고 onSubmit: imageUris.size=${imageUris.size}")
                                        try {
                                            val urls = if (imageUris.isEmpty()) {
                                                emptyList()
                                            } else {
                                                try {
                                                    BugReportRepository.uploadImages(context, imageUris)
                                                } catch (up: Exception) {
                                                    val msg = up.message?.takeIf { it.isNotBlank() } ?: "알 수 없는 오류"
                                                    val userMsg = when {
                                                        msg.contains("Object does not exist", ignoreCase = true) ->
                                                            "이미지 업로드에 잠시 문제가 있었어요. 잠시 후 다시 시도해주세요."
                                                        msg.contains("이미지를 읽을 수 없", ignoreCase = true) -> msg
                                                        else -> "이미지 업로드 실패: $msg"
                                                    }
                                                    throw RuntimeException(userMsg, up)
                                                }
                                            }
                                            Log.d("Aptox", "버그신고: uploadImages 완료 urls.size=${urls.size}")
                                            FirebaseFunctions.getInstance()
                                                .getHttpsCallable("submitBugReport")
                                                .withTimeout(60, TimeUnit.SECONDS)
                                                .call(hashMapOf(
                                                    "title" to title,
                                                    "content" to content,
                                                    "imageUrls" to urls,
                                                ))
                                                .await()
                                            toastMessage = "버그 신고가 등록되었어요. 감사해요!"
                                        } catch (e: Exception) {
                                            val fe = (e as? FirebaseFunctionsException) ?: (e.cause as? FirebaseFunctionsException)
                                            val msg = when {
                                                fe != null -> when (fe.code) {
                                                        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                                                            "등록이 완료됐을 수 있어요. Firebase 콘솔에서 확인해주세요."
                                                        FirebaseFunctionsException.Code.NOT_FOUND -> "함수를 찾을 수 없어요. Functions 배포를 확인해주세요."
                                                        FirebaseFunctionsException.Code.UNAVAILABLE -> "인터넷 연결을 확인해주세요."
                                                        FirebaseFunctionsException.Code.FAILED_PRECONDITION -> "Firebase 설정을 확인해주세요."
                                                        FirebaseFunctionsException.Code.INTERNAL -> "서버 오류가 발생했어요. Firebase 콘솔에서 등록 여부를 확인해주세요."
                                                        FirebaseFunctionsException.Code.INVALID_ARGUMENT -> fe.message ?: "입력값을 확인해주세요."
                                                        FirebaseFunctionsException.Code.PERMISSION_DENIED -> "권한이 없어요."
                                                        FirebaseFunctionsException.Code.UNIMPLEMENTED -> "함수가 아직 배포되지 않았을 수 있어요. Firebase에서 Functions 배포를 확인해주세요."
                                                        FirebaseFunctionsException.Code.UNAUTHENTICATED -> "로그인이 필요해요."
                                                        else -> fe.message?.takeIf { it.isNotBlank() } ?: "등록에 실패했어요. Firebase 콘솔에서 확인해주세요."
                                                    }
                                                    else -> e.message?.takeIf { it.isNotBlank() } ?: "등록에 실패했어요. 네트워크를 확인해주세요."
                                                }
                                            Log.e("Aptox", "버그신고: ${e.message}", e)
                                            throw RuntimeException(msg)
                                        }
                                    },
                                )
                                SettingsDetail.Withdraw -> WithdrawConfirmScreen(
                                    onConfirmWithdraw = {
                                        settingsDetail = null
                                        // TODO: 실제 탈퇴 API 호출 후 로그아웃 등 처리
                                    },
                                )
                                null -> MyPageScreen(
                                    onAccountManageClick = { settingsDetail = SettingsDetail.AccountManage },
                                    onAppRestrictionHistoryClick = { settingsDetail = SettingsDetail.AppRestrictionHistory },
                                    onSubscriptionManageClick = { /* 구독 관리 비활성화 */ },
                                    onNotificationClick = { settingsDetail = SettingsDetail.Notification },
                                    onPermissionClick = { settingsDetail = SettingsDetail.Permission },
                                    onBugReportClick = { settingsDetail = SettingsDetail.BugReport },
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
                    AptoxBottomNavBar(
                        destinations = navDestinations,
                        selectedIndex = navIndex,
                        onTabSelected = {
                            if (it != navIndex) {
                                if (it == 1 && !isLoggedIn) {
                                    toastReplayKey += 1
                                    toastMessage = "로그인 후 진행 가능합니다"
                                    return@AptoxBottomNavBar
                                }
                                if (it == 2) {
                                    tryNavigateToStatisticsTab()
                                    return@AptoxBottomNavBar
                                }
                                navIndex = it
                                if (it != 3) settingsDetail = null
                            }
                        },
                        showPremiumBanner = isFreeUser,
                        onPremiumClick = { if (isFreeUser) showSubscriptionGuide = true },
                    )
                }

            AptoxToast(
                message = toastMessage ?: "",
                visible = toastMessage != null,
                onDismiss = { toastMessage = null },
                replayKey = toastReplayKey,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (showRequiredPermissionDialog) {
            AptoxRequiredPermissionDialog(
                onDismissRequest = { showRequiredPermissionDialog = false },
                confirmButtonText = "확인",
                onCloseClick = {
                    showRequiredPermissionDialog = false
                    navIndex = 3
                    settingsDetail = SettingsDetail.Permission
                },
            )
        }

        // 진행중인 앱 상세 바텀시트
        if (showAppLimitInfoSheet && selectedAppForDetail != null) {
            val item = selectedAppForDetail!!
            val appIcon = rememberAppIconPainter(item.packageName)

            val dismiss: () -> Unit = {
                showAppLimitInfoSheet = false
                selectedAppForDetail = null
            }

            when {
                // ── 시간 지정 제한 (isPaused 여부 무관, 3가지 상태 표시) ──
                item.restrictionType == RestrictionType.TIME_SPECIFIED -> {
                    val now = System.currentTimeMillis()
                    val (winStart, winEnd) = rollToNextTimeSpecifiedWindow(
                        item.startTimeMs,
                        item.blockUntilMs,
                        now,
                    )
                    val pauseRepo = PauseRepository(context)
                    val pauseUntilMs = pauseRepo.getPauseUntilMs(item.packageName)
                    val pauseStartMs = pauseUntilMs - 5 * 60 * 1000
                    val pauseElapsedMs = (now - pauseStartMs).coerceAtLeast(0)
                    AppLimitInfoBottomSheet(
                        title = "제한 중인 앱",
                        appName = item.appName,
                        appIcon = appIcon,
                        appUsageText = if (item.isPaused) {
                            formatDurationHhMmSs(pauseElapsedMs)
                        } else {
                            timeSpecifiedStatusLine(item.startTimeMs, item.blockUntilMs, now)
                        },
                        appUsageLabel = if (item.isPaused) "일시정지 중" else "",
                        summaryRows = listOf(
                            AppLimitSummaryRow("제한 시작 시간", KoreanTimeFormat.formatHourClock(winStart)),
                            AppLimitSummaryRow("제한 종료 시간", KoreanTimeFormat.formatHourClock(winEnd)),
                            AppLimitSummaryRow("제한 방식", "시간 지정 제한"),
                        ),
                        primaryButtonText = "닫기",
                        onPrimaryClick = dismiss,
                        inlineReleaseButtonText = "제한 해제",
                        onInlineReleaseClick = {
                            RestrictionDeleteHelper.deleteRestrictedApp(context, item.packageName)
                            dismiss()
                        },
                        onDismissRequest = dismiss,
                    )
                }
                // ── 스크린샷2: 일일사용량 제한 ──
                item.restrictionType == RestrictionType.DAILY_USAGE -> {
                    AppLimitInfoBottomSheetDaily(
                        packageName = item.packageName,
                        appName = item.appName,
                        appIcon = appIcon,
                        limitMinutes = item.limitMinutes,
                        onDismissRequest = dismiss,
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
            val markNotificationsRead: () -> Unit = {
                if (firebaseAuthUid != null) {
                    NotificationRepository(context).markAsChecked(firebaseAuthUid, System.currentTimeMillis())
                }
            }
            NotificationHistoryScreen(
                userId = firebaseAuthUid,
                onBack = {
                    markNotificationsRead()
                    showNotificationOverlay = false
                },
                onItemClick = { item ->
                    markNotificationsRead()
                    showNotificationOverlay = false
                    when {
                        item.navTarget == "statistics_weekly" || item.type == "weekly_report" ->
                            tryNavigateToStatisticsTab()
                        item.badgeId != null -> {
                            navIndex = 1
                        }
                        else -> {
                            navIndex = 1
                        }
                    }
                },
                onNotificationSettingsClick = null,
            )
        }
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

    } // Box
    } // CompositionLocalProvider
}