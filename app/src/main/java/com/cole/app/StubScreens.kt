package com.cole.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

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
private val MAIN_DAY_LABELS = listOf("수", "목", "금", "토", "일", "월", "화")
private val MainCardShape = RoundedCornerShape(12.dp)
private val MainCardShadowColor = Color.Black.copy(alpha = 0.06f)

// 목업: 일별 완료(이모지) / 미완료(날짜숫자)
private data class MainDayItem(val label: String, val isCompleted: Boolean, val emojiOrDay: String)

// 목업: 앱 제한 행 (usageTextColor/usageLabelColor: 일시정지중일 때 Red300)
private data class MainAppRestrictionItem(
    val appName: String,
    val usageText: String,
    val usageLabel: String,
    val showDetailButton: Boolean,
    val appIconResId: Int = R.drawable.ic_app_placeholder,
    val usageTextColor: Color? = null,
    val usageLabelColor: Color? = null,
)

private val MainDayAreaSize = 42.dp
private val MainDayCircleSize = 32.dp

@Composable
private fun MainDailyProgressSection(
    days: List<MainDayItem>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MAIN_DAY_LABELS.forEachIndexed { i, label ->
                val item = days.getOrNull(i) ?: MainDayItem(label, false, "")
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.size(MainDayAreaSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MainDayCircleSize)
                                .clip(CircleShape)
                                .background(if (item.isCompleted) AppColors.Primary300 else AppColors.Primary200),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = item.emojiOrDay,
                                style = AppTypography.Caption1.copy(
                                    color = if (item.isCompleted) AppColors.TextInvert else AppColors.TextBody,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
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
            appIcon = if (item.appIconResId == R.drawable.ic_app_placeholder) {
                rememberDefaultAppIconPainter()
            } else {
                painterResource(item.appIconResId)
            },
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
        if (item.showDetailButton) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.ButtonSecondaryBgDefault)
                    .border(0.6.dp, AppColors.ButtonSecondaryBorderDefault, RoundedCornerShape(6.dp))
                    .clickable { onDetailClick() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text(text = "자세히 보기", style = AppTypography.ButtonSmall.copy(color = AppColors.ButtonSecondaryTextDefault))
            }
        }
    }
}

@Composable
private fun MainAppRestrictionCard(
    apps: List<MainAppRestrictionItem>,
    onAddAppClick: () -> Unit,
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
                MainAppRestrictionRow(item = item)
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
            )
        }
    }
}

/** MA-01: 접근권한 허용 카드 (Figma 336:2910, 회색 배경) */
@Composable
private fun MainPermissionBanner(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MainCardShape)
            .background(AppColors.Grey200)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_permission_denied),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = "접근권한을 허용해주세요",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = AppColors.TextSecondary,
        )
    }
}

/** MA-02: 진행 중인 앱 빈 상태 (Figma 662:2907) */
@Composable
private fun MainAppRestrictionCardEmpty(
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
    addButtonText: String = "잠시만 멀어질 앱 추가하기",
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "아직 진행중인 앱이 없어요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "잠시 제한하고 싶은 앱을 추가해보세요",
                style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center,
            )
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
    data object Permission : SettingsDetail("권한설정")
    data object AppInfo : SettingsDetail("정보")
    data object OpenSource : SettingsDetail("오픈소스 라이센스")
}

/** MA-01 메인 화면 (Figma 336:2910): 기본 페이지, 데이터 있을 때 */
@Composable
fun MainScreenMA01(
    onAddAppClick: () -> Unit,
    onPermissionClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mockDaysMA01 = listOf(
        MainDayItem("수", true, "👍"),
        MainDayItem("목", true, "👍"),
        MainDayItem("금", true, "😥"),
        MainDayItem("토", true, "👍"),
        MainDayItem("일", false, "15"),
        MainDayItem("월", false, "16"),
        MainDayItem("화", false, "17"),
    )
    val mockAppsMA01 = listOf(
        MainAppRestrictionItem("인스타그램", "14분/30분", "사용 중", true),
        MainAppRestrictionItem("인스타그램", "15분/30분", "사용 중", true),
        MainAppRestrictionItem("인스타그램", "09:50", "일시 정지 중", false, usageTextColor = AppColors.Red300, usageLabelColor = AppColors.Red300),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        MainPermissionBanner(onClick = onPermissionClick)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
                    .clip(MainCardShape)
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(16.dp),
            ) {
                MainDailyProgressSection(days = mockDaysMA01)
            }
        }
        MainAppRestrictionCard(
            apps = mockAppsMA01,
            onAddAppClick = onAddAppClick,
            addButtonText = "잠시만 멀어질 앱 추가하기",
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/** MA-02 메인 화면 (Figma 662:2907): 데이터 없을 때 */
@Composable
fun MainScreenMA02(
    onAddAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mockDaysMA02 = listOf(
        MainDayItem("수", false, "11"),
        MainDayItem("목", false, "12"),
        MainDayItem("금", false, "13"),
        MainDayItem("토", false, "14"),
        MainDayItem("일", false, "15"),
        MainDayItem("월", false, "16"),
        MainDayItem("화", false, "17"),
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, MainCardShape, false, MainCardShadowColor, MainCardShadowColor)
                    .clip(MainCardShape)
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(16.dp),
            ) {
                MainDailyProgressSection(days = mockDaysMA02)
            }
        }
        MainAppRestrictionCardEmpty(onAddAppClick = onAddAppClick)
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MainFlowHost(onAddAppClick: () -> Unit, onLogout: () -> Unit) {
    var navIndex by remember { mutableIntStateOf(0) }
    var settingsDetail by remember { mutableStateOf<SettingsDetail?>(null) }
    val navDestinations = listOf(
        NavDestination("홈", R.drawable.ic_nav_home_inactive, R.drawable.ic_nav_home_active),
        NavDestination("챌린지", R.drawable.ic_nav_challenge_inactive, R.drawable.ic_nav_challenge_active),
        NavDestination("통계", R.drawable.ic_nav_stats_inactive, R.drawable.ic_nav_stats_active),
        NavDestination("설정", R.drawable.ic_nav_mypage_inactive, R.drawable.ic_nav_mypage_active),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 18.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        when {
            settingsDetail != null -> ColeHeaderSub(
                title = settingsDetail!!.title,
                backIcon = painterResource(R.drawable.ic_back),
                onBackClick = { settingsDetail = null },
                showNotification = true,
                modifier = Modifier.fillMaxWidth(),
            )
            navIndex == 0 -> ColeHeaderHome(logo = painterResource(R.drawable.ic_logo), hasNotification = true)
            navIndex == 1 -> ColeHeaderTitleWithNotification(
                title = "챌린지",
                hasNotification = true,
                modifier = Modifier.fillMaxWidth(),
            )
            navIndex == 2 -> ColeHeaderTitleWithNotification(
                title = "통계",
                hasNotification = true,
                modifier = Modifier.fillMaxWidth(),
            )
            navIndex == 3 -> ColeHeaderTitleWithNotification(title = "설정", hasNotification = true)
            else -> ColeHeaderHome(logo = painterResource(R.drawable.ic_logo), hasNotification = true)
        }
        when (navIndex) {
            0 -> {
                Box(modifier = Modifier.weight(1f)) {
                    MainScreenMA01(onAddAppClick = onAddAppClick)
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    ChallengeScreen()
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    StatisticsScreen()
                }
            }
            3 -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    when (settingsDetail) {
                        SettingsDetail.AccountManage -> AccountManageScreen(
                            onBack = { settingsDetail = null },
                            onProfileClick = { },
                            onSocialClick = { },
                            onPasswordClick = { },
                        )
                        SettingsDetail.Subscription -> SubscriptionManageScreen(
                            onBack = { settingsDetail = null },
                            onPaymentClick = { },
                        )
                        SettingsDetail.Permission -> PermissionSettingsScreen(
                            onBack = { settingsDetail = null },
                            onAccessibilityClick = { },
                            onUsageStatsClick = { },
                            onOverlayClick = { },
                        )
                        SettingsDetail.AppInfo -> AppInfoScreen(
                            onBack = { settingsDetail = null },
                            onTermsClick = { },
                            onPrivacyClick = { },
                        )
                        SettingsDetail.OpenSource -> OpenSourceScreen(onBack = { settingsDetail = null })
                        null -> MyPageScreen(
                            onAccountManageClick = { settingsDetail = SettingsDetail.AccountManage },
                            onSubscriptionManageClick = { settingsDetail = SettingsDetail.Subscription },
                            onNotificationClick = { },
                            onPermissionClick = { settingsDetail = SettingsDetail.Permission },
                            onAppInfoClick = { settingsDetail = SettingsDetail.AppInfo },
                            onOpenSourceClick = { settingsDetail = SettingsDetail.OpenSource },
                            onWithdrawClick = { },
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
                    Text(text = "준비중", style = AppTypography.HeadingH3.copy(color = AppColors.TextSecondary))
                }
            }
        }
        ColeBottomNavBar(
            destinations = navDestinations,
            selectedIndex = navIndex,
            onTabSelected = { navIndex = it },
            onPremiumClick = { },
        )
    }
}
