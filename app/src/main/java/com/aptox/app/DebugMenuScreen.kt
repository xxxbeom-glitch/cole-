package com.aptox.app

import com.aptox.app.subscription.SubscriptionManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Debug 메뉴 호스트.
 * 배포 시 이 화면만 제거하고 SignUpFlowHost를 직접 진입점으로 교체하면 됨.
 */
@Composable
fun DebugFlowHost(
    onStartNormalFlow: () -> Unit,
    modifier: Modifier = Modifier,
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    var selectedScreen by remember { mutableStateOf<DebugScreen?>(null) }
    val menuScrollState = rememberScrollState()

    LaunchedEffect(pendingPauseFlowFromOverlay, pendingOpenBottomSheetPackage, pendingNavIndex) {
        if (pendingPauseFlowFromOverlay != null || pendingOpenBottomSheetPackage != null || pendingNavIndex != null) {
            selectedScreen = DebugScreen.MainFlow
        }
    }

    var addAppReturnTo by remember { mutableStateOf<DebugScreen?>(null) } // MainFlow에서 진입 시 완료 후 돌아갈 화면
    if (selectedScreen != null) {
        val activity = LocalContext.current as? MainActivity
        Box(modifier = modifier.fillMaxSize()) {
            DebugScreenPreview(
                screen = selectedScreen!!,
                onBack = { selectedScreen = null; addAppReturnTo = null },
                onNavigateToScreen = { target ->
                    if (target == DebugScreen.AddAppFlowHost) addAppReturnTo = DebugScreen.MainFlow
                    selectedScreen = target
                },
                onAddAppComplete = {
                    selectedScreen = addAppReturnTo ?: null
                    addAppReturnTo = null
                },
                pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
                onPauseFlowConsumed = { activity?.clearPendingPauseFlowFromOverlay() },
                pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
                onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
                pendingNavIndex = pendingNavIndex,
                onNavIndexConsumed = onNavIndexConsumed,
            )
        }
    } else {
        DebugMenuScreen(
            onScreenSelect = { selectedScreen = it },
            onStartNormalFlow = onStartNormalFlow,
            menuScrollState = menuScrollState,
            modifier = modifier.fillMaxSize(),
        )
    }
}

/** 디버그 메뉴에서 이동 가능한 화면 */
sealed class DebugScreen(val category: String, val label: String) {
    // 인증/온보딩
    data object Splash : DebugScreen("인증/온보딩", "스플래시")
    data object Permission : DebugScreen("인증/온보딩", "기기 권한 안내")
    data object SelfTestVer2 : DebugScreen("인증/온보딩", "스마트폰 사용패턴 테스트")
    data object SelfTestLoading : DebugScreen("인증/온보딩", "테스트 결과 로딩 애니메이션")
    data object UsagePatternAnalysis : DebugScreen("인증/온보딩", "진단 결과")
    /** 테스트 → 로딩 → 진단 결과까지 이어지는 전체 플로우 */
    data object SelfTestFullFlow : DebugScreen("인증/온보딩", "사용패턴 테스트 → 진단 결과 (전체 플로우)")
    data object AppIntroOnboarding : DebugScreen("인증/온보딩", "앱 소개 (App intro)")
    // 화면 목차에서 제거됨 (바로가기/자가테스트 플로우)
    data object Login : DebugScreen("인증/온보딩", "Login")
    data object SelfTest : DebugScreen("인증/온보딩", "자가테스트")
    data object SelfTestResult : DebugScreen("인증/온보딩", "자가테스트 결과")
    // 앱 제한
    data object AddAppAA01 : DebugScreen("앱 제한", "AA-01: 제한 방법 선택")
    data object AddAppAppSelect : DebugScreen("앱 제한", "앱 선택")
    data object AddAppAA02A01 : DebugScreen("앱 제한", "AA-02A-01: 제한 방식 (3가지)")
    data object AddAppDaily01 : DebugScreen("앱 제한", "AA-02B: 일일 사용 한도")
    data object AddAppDaily02 : DebugScreen("앱 제한", "AA-02B: 사용량 경고")
    data object AddAppDaily03 : DebugScreen("앱 제한", "AA-02B: 적용 요일")
    data object AddAppDaily04 : DebugScreen("앱 제한", "AA-02B: 설정 요약")
    data object AddAppDaily05 : DebugScreen("앱 제한", "AA-02B: 완료")
    data object AddAppFlowHost : DebugScreen("앱 제한", "앱 제한 플로우 전체")
    data object TimeSpecifiedFlow : DebugScreen("앱 제한", "시간 지정 제한 플로우 전체")

    // 메인
    data object MainFlow : DebugScreen("메인", "메인 (홈/챌린지/통계/설정)")

    // 테스트
    data object SpacingTest : DebugScreen("테스트", "간격테스트 (헤더~콘텐츠 38/36/32/28/26dp)")
    data object GaugeTest : DebugScreen("테스트", "게이지 테스트 (ResultGaugeGraph)")
    data object GaugeTest2 : DebugScreen("테스트", "게이지테스트 2 (5등분 세그먼트)")
    data object SelfTestResultST10 : DebugScreen("테스트", "자가테스트 결과 ST-10 (반원형 게이지)")
    data object LoadingAnimation : DebugScreen("테스트", "로딩 애니메이션 (3dot → 체크)")
    data object AppIconTest : DebugScreen("테스트", "앱 아이콘 테스트")
    data object ToastAnimationTest : DebugScreen("테스트", "토스트 애니메이션 테스트")
    data object DrumrollTimePicker : DebugScreen("테스트", "드럼롤 시간 선택 피커")

    // 바텀시트
    data object BaseBottomSheet : DebugScreen("바텀시트", "BaseBottomSheet (기본형)")
    data object TermsBottomSheet : DebugScreen("바텀시트", "TermsBottomSheet (약관 동의)")
    data object AppLimitSetupTime : DebugScreen("바텀시트", "AppLimitSetupTime (시간 슬라이더)")
    data object AppLimitSetupDay : DebugScreen("바텀시트", "AppLimitSetupDay (요일 선택)")
    data object AppLimitInfoBottomSheet : DebugScreen("바텀시트", "AppLimitInfoBottomSheet (시간 지정 제한)")
    data object AppLimitInfoBottomSheetDaily : DebugScreen("바텀시트", "AppLimitInfoBottomSheetDaily (일일 사용량 제한)")
    data object AppLimitInfoBottomSheetPaused : DebugScreen("바텀시트", "AppLimitInfoBottomSheetPaused (일시 정지 중)")
    data object AddAppAppCategoryBottomSheet : DebugScreen("바텀시트", "AddAppAppCategoryBottomSheet (앱의 종류 지정)")

    // 앱 제한 일시정지 (UL)
    data object AppLimitPauseProposal : DebugScreen("앱 제한 일시정지", "UL-01: 제안")
    data object AppLimitPauseConfirm : DebugScreen("앱 제한 일시정지", "UL-02: 확인")
    data object AppLimitPauseComplete : DebugScreen("앱 제한 일시정지", "UL-03: 완료")

    // 권한 — 화면 목차에서 제거 (인증/온보딩에 Permission 사용)

    // 사용시간/모니터링/오버레이
    data object UsageStatsTest : DebugScreen("테스트", "앱별 사용시간 (UsageStats)")
    data object AppMonitorTest : DebugScreen("테스트", "앱 모니터 서비스 (시작/중지)")
    /** BlockDialogActivity AlertDialog 3종 (시간지정·일일초과·카운트미시작) */
    data object BlockOverlayTest : DebugScreen("테스트", "차단 AlertDialog (3종)")

    /** 내부 저장소 크래시 로그 조회 (UncaughtExceptionHandler 자동 저장) */
    data object CrashLogs : DebugScreen("디버그", "크래시 로그")
}

@Composable
private fun DebugScreenPreview(
    screen: DebugScreen,
    onBack: () -> Unit,
    onNavigateToScreen: (DebugScreen) -> Unit = {},
    onAddAppComplete: () -> Unit = {},
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    onPauseFlowConsumed: () -> Unit = {},
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    when (screen) {
        DebugScreen.Splash -> DebugSplashPreview(onBack = onBack)
        DebugScreen.Login -> DebugLoginPreview(onBack = onBack)
        DebugScreen.SelfTest -> SelfTestScreen(
            onBackClick = onBack,
            onComplete = { onBack() },
        )
        DebugScreen.SelfTestVer2 -> SelfTestScreenVer2(onBack = onBack)
        DebugScreen.SelfTestLoading -> SelfTestLoadingScreen(onFinish = onBack)
        DebugScreen.UsagePatternAnalysis -> DebugUsagePatternAnalysisPreview(onBack = onBack)
        DebugScreen.SelfTestFullFlow -> DebugSelfTestFullFlowPreview(onBack = onBack)
        DebugScreen.SelfTestResult -> SelfTestResultScreen(
            resultType = SelfTestResultType.CAUTION,
            onStartClick = onBack,
            onBackClick = onBack,
            rawScore = 23,
            userName = "장원영",
        )
        DebugScreen.AddAppAA01 -> AddAppScreenAA01(
            onTimeSpecifiedClick = onBack,
            onDailyLimitClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppAppSelect -> AddAppScreenAppSelect(
            onAddAppClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppAA02A01 -> AddAppScreenAA02A01(
            onDailyLimitClick = onBack,
            onTimeSpecifiedClick = onBack,
            onBlockClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily01 -> AddAppDailyLimitScreen01(
            selectedAppNames = emptySet(),
            selectedDailyMinutes = null,
            onAppRowClick = { },
            onTimeRowClick = { },
            onNextClick = { onBack() },
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily02 -> AddAppDailyLimitScreen02(
            onNextClick = { onBack() },
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily03 -> AddAppDailyLimitScreen03(
            onNextClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily04 -> AddAppDailyLimitScreen04(
            limitMinutes = "1시간",
            onConfirmClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily05 -> AddAppDailyLimitScreen05(
            appName = "인스타그램",
            limitMinutes = "1시간 30분",
            onCompleteClick = onBack,
            onAddAnotherClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppFlowHost -> AddAppFlowHost(
            onComplete = onAddAppComplete,
            onBackFromFirst = onAddAppComplete,
        )
        DebugScreen.TimeSpecifiedFlow -> TimeSpecifiedFlowHost(
            onComplete = onBack,
            onBackFromFirst = onBack,
        )
        DebugScreen.MainFlow -> MainFlowHost(
            onAddAppClick = { onNavigateToScreen(DebugScreen.AddAppFlowHost) },
            onLogout = { onBack() },
            isFreeUser = !SubscriptionManager.isSubscribed(LocalContext.current),
            initialPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            onPauseFlowConsumed = onPauseFlowConsumed,
            initialAutoOpenPackage = pendingOpenBottomSheetPackage,
            onAutoOpenConsumed = onOpenBottomSheetConsumed,
            initialNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
        DebugScreen.SpacingTest -> DebugSpacingTestScreen(onBack = onBack)
        DebugScreen.GaugeTest -> DebugGaugeTestScreen(onBack = onBack)
        DebugScreen.GaugeTest2 -> DebugGaugeTest2Screen(onBack = onBack)
        DebugScreen.SelfTestResultST10 -> SelfTestResultScreenST10(
            resultType = SelfTestResultType.CAUTION,
            onStartClick = onBack,
            onBackClick = onBack,
            rawScore = 23,
            userName = "장원영",
        )
        DebugScreen.LoadingAnimation -> DebugLoadingAnimationTestScreen(onBack = onBack)
        DebugScreen.AppIconTest -> DebugAppIconTestScreen(onBack = onBack)
        DebugScreen.BaseBottomSheet -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            BaseBottomSheet(
                title = "앱을 선택해주세요",
                subtitle = "앱은 최대 1개까지 선택가능 해요\n이미 제한이 진행중인 앱은 선택하실 수 없어요",
                onDismissRequest = onSheetDismiss,
                onPrimaryClick = onSheetDismiss,
                primaryButtonText = "계속 진행",
                secondaryButtonText = "돌아가기",
                onSecondaryClick = onSheetDismiss,
            ) {
                Text(
                    text = "컨텐츠 영역",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
        }
        DebugScreen.TermsBottomSheet -> DebugTermsBottomSheetPreview(onBack = onBack)
        DebugScreen.AppLimitSetupTime -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitSetupTimeBottomSheet(
                onDismissRequest = onSheetDismiss,
                onPrimaryClick = { _, _ -> onSheetDismiss() },
            )
        }
        DebugScreen.AppLimitSetupDay -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitSetupDayBottomSheet(
                onDismissRequest = onSheetDismiss,
                onPrimaryClick = { _ -> onSheetDismiss() },
            )
        }
        DebugScreen.AppLimitInfoBottomSheet -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitInfoBottomSheet(
                title = "잠시의 쉼표입니까, 아니면 오늘의 마침표입니까?",
                bodyText = "현재 Threads 앱의 목표의 40%를 넘어섰어요\n짧은 숨 고르기는 '10분간 사용하기'로, 불가피한 종결은 '즉시 차단 해제'를 통해 본인의 속도에 맞춰 진행해주세요",
                appName = "넷플릭스",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                appUsageText = "7회",
                appUsageLabel = "최근 한 주간 유혹받은 횟수",
                summaryRows = listOf(
                    AppLimitSummaryRow("선택된 앱", "인스타그램"),
                    AppLimitSummaryRow("일일 사용시간", "1시간 30분"),
                    AppLimitSummaryRow("반복 요일", "월, 화, 수, 목"),
                    AppLimitSummaryRow("적용 기간", "4주"),
                    AppLimitSummaryRow("시작 시기", "지금 즉시"),
                ),
                onDismissRequest = onSheetDismiss,
                onDetailClick = onSheetDismiss,
                onPrimaryClick = onSheetDismiss,
            )
        }
        DebugScreen.AppLimitInfoBottomSheetDaily -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitInfoBottomSheetDaily(
                packageName = "com.netflix.mediaclient",
                appName = "넷플릭스",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                limitMinutes = 90,
                onDismissRequest = onSheetDismiss,
            )
        }
        DebugScreen.AppLimitInfoBottomSheetPaused -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitInfoBottomSheetPaused(
                title = "제한 중인 앱",
                appName = "인스타그램",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                pauseRemainingText = "09:50",
                summaryRows = listOf(
                    AppLimitSummaryRow("일시 정지 남은 시간", "09:50"),
                    AppLimitSummaryRow("오늘 사용 시간", "14분/30분"),
                ),
                onDismissRequest = onSheetDismiss,
                onPrimaryClick = onSheetDismiss,
                primaryButtonText = "제한 재개",
            )
        }
        DebugScreen.AppLimitPauseProposal -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitPauseProposalBottomSheet(
                onDismissRequest = onSheetDismiss,
                onContinueClick = onSheetDismiss,
                onBackClick = onSheetDismiss,
            )
        }
        DebugScreen.AppLimitPauseConfirm -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitPauseConfirmBottomSheet(
                appName = "넷플릭스",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                usageText = "14분/30분",
                usageLabel = "사용 중",
                onDismissRequest = onSheetDismiss,
                onPauseClick = onSheetDismiss,
                onBackClick = onSheetDismiss,
                onDetailClick = onSheetDismiss,
            )
        }
        DebugScreen.AppLimitPauseComplete -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AppLimitPauseCompleteBottomSheet(
                appName = "넷플릭스",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                remainingChances = 1,
                onDismissRequest = onSheetDismiss,
                onLaunchAppClick = onSheetDismiss,
            )
        }
        DebugScreen.AddAppAppCategoryBottomSheet -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            AddAppAppCategoryBottomSheet(
                initialCategory = "게임",
                onDismissRequest = onSheetDismiss,
                onPrimaryClick = { onSheetDismiss() },
            )
        }
        DebugScreen.Permission -> PermissionScreen(
            onPrimaryClick = onBack,
            onGhostClick = onBack,
            enforceRequiredPermissionsForNext = false,
        )
        DebugScreen.AppIntroOnboarding -> AppIntroOnboardingScreen(
            onNextClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.ToastAnimationTest -> ToastAnimationTestScreen(onBack = onBack)
        DebugScreen.DrumrollTimePicker -> DrumrollTimePickerTestScreen(onBack = onBack)
        DebugScreen.UsageStatsTest -> UsageStatsTestScreen(onBack = onBack)
        DebugScreen.AppMonitorTest -> AppMonitorTestScreen(onBack = onBack)
        DebugScreen.BlockOverlayTest -> BlockOverlayTestScreen(onBack = onBack)
        DebugScreen.CrashLogs -> CrashLogScreen(onBack = onBack)
        else -> DebugPlaceholderScreen(screen = screen, onBack = onBack)
    }
}

@Composable
private fun DebugSplashPreview(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        SplashScreen(onFinish = onBack)
        AptoxGhostButton(
            text = "돌아가기",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .widthIn(max = 120.dp),
        )
    }
}

@Composable
private fun DebugUsagePatternAnalysisPreview(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiagnosisResultScreen(
            userName = "아영",
            diagnosisScore = 85,
            onFinish = onBack,
        )
        AptoxGhostButton(
            text = "돌아가기",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .widthIn(max = 120.dp),
        )
    }
}

/**
 * 사용패턴 테스트 → 로딩 → 진단 결과까지 이어지는 전체 플로우 디버그 프리뷰.
 * 실제 온보딩과 동일하게 이름 입력 + 7문항 응답 → 로딩 → 결과 화면까지 직접 테스트 가능.
 */
@Composable
private fun DebugSelfTestFullFlowPreview(onBack: () -> Unit) {
    var phase by remember { mutableStateOf(0) } // 0=테스트, 1=로딩, 2=결과
    var userName by remember { mutableStateOf("") }
    var answers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    when (phase) {
        0 -> SelfTestScreenVer2(
            onBack = onBack,
            onComplete = { name, ans ->
                userName = name
                answers = ans
                phase = 1
            },
        )
        1 -> SelfTestLoadingScreen(
            onFinish = { phase = 2 },
            userName = userName,
        )
        2 -> Box(modifier = Modifier.fillMaxSize()) {
            DiagnosisResultScreen(
                userName = userName,
                diagnosisScore = computeDiagnosisScore(answers),
                onFinish = onBack,
            )
        }
    }
}

@Composable
private fun DebugLoginPreview(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreen(
            logo = painterResource(R.drawable.ic_login_logo),
            onLoginClick = { _, _ -> onBack() },
            onSignUpClick = onBack,
            onGoogleLoginClick = {},
            onForgotPasswordClick = {},
        )
        AptoxGhostButton(
            text = "돌아가기",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .widthIn(max = 120.dp),
        )
    }
}

@Composable
private fun DebugBottomSheetPreview(
    onBack: () -> Unit,
    content: @Composable (onSheetDismiss: () -> Unit) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AptoxGhostButton(text = "돌아가기", onClick = onBack)
        AptoxPrimaryButton(
            text = "바텀시트 열기",
            onClick = { showSheet = true },
        )
    }

    if (showSheet) {
        content {
            scope.launch {
                delay(350)
                showSheet = false
            }
        }
    }
}

@Composable
private fun DebugTermsBottomSheetPreview(onBack: () -> Unit) {
    val agreements = remember {
        mutableStateListOf(
            AgreementItem("(필수) 만 18세 이상입니다", true, false),
            AgreementItem("(필수) 이용약관에 동의합니다", true, false),
            AgreementItem("(필수) 개인정보 수집 및 이용 동의합니다", true, false),
        )
    }
    var allAgreed by remember { mutableStateOf(false) }

    DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
        TermsBottomSheet(
            onDismissRequest = onSheetDismiss,
            onNextClick = onSheetDismiss,
            agreements = agreements,
            allAgreedState = allAgreed,
            onAllAgreedChange = { checked ->
                allAgreed = checked
                agreements.forEachIndexed { i, _ ->
                    agreements[i] = agreements[i].copy(checked = checked)
                }
            },
            onItemCheckedChange = { index, checked ->
                agreements[index] = agreements[index].copy(checked = checked)
                allAgreed = agreements.all { it.checked }
            },
        )
    }
}

@Composable
private fun DebugPlaceholderScreen(screen: DebugScreen, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = screen.label,
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "미구현 또는 별도 플로우 내 화면",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(24.dp))
        AptoxPrimaryButton(text = "돌아가기", onClick = onBack)
    }
}

/** Debug 메뉴 메인 화면 */
@Composable
private fun DebugMenuScreen(
    onScreenSelect: (DebugScreen) -> Unit,
    onStartNormalFlow: () -> Unit,
    menuScrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("화면 목차", "디자인 시스템", "테스트 설정")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Debug 메뉴",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            )
        }

        // 탭
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) AppColors.Primary300
                            else AppColors.Grey200
                        )
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedTab = index }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = title,
                        style = AppTypography.Caption1.copy(
                            color = if (isSelected) AppColors.TextInvert else AppColors.TextBody,
                        ),
                    )
                }
            }
        }

        // 컨텐츠
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DebugScreenListSection(
                    onScreenSelect = onScreenSelect,
                    scrollState = menuScrollState,
                )
                1 -> DebugDesignSystemSection()
                2 -> DebugTestSettingsSection()
            }
        }

        // 하단 고정 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AptoxPrimaryButton(
                text = "앱 시작하기",
                onClick = onStartNormalFlow,
                modifier = Modifier.fillMaxWidth(),
            )
            AptoxGhostButton(
                text = "디버그 메뉴 유지",
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DebugScreenListSection(
    onScreenSelect: (DebugScreen) -> Unit,
    scrollState: ScrollState,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(top = 48.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 바로가기 영역 — 삭제됨 (주석 보존)
        // Text(text = "바로가기", ...)
        // Row { Splash, Login }

        // 자가테스트 플로우 영역 — 삭제됨 (주석 보존)
        // Text(text = "자가테스트 플로우", ...)
        // Row { SelfTest, SelfTestLoading, UsagePatternAnalysis, SelfTestResult }

        // 인증/온보딩: 재정렬된 순서
        Text(
            text = "인증/온보딩",
            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
        )
        listOf(
            DebugScreen.Splash,
            DebugScreen.Permission,
            DebugScreen.AppIntroOnboarding,
            DebugScreen.SelfTestFullFlow,
            DebugScreen.SelfTestVer2,
            DebugScreen.SelfTestLoading,
            DebugScreen.UsagePatternAnalysis,
        ).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(
                    text = screen.label,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 앱 제한 플로우
        Text(
            text = "앱 제한",
            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
        )
        listOf(DebugScreen.AddAppFlowHost, DebugScreen.TimeSpecifiedFlow).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(text = screen.label, style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 메인
        Text(text = "메인", style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
        listOf(DebugScreen.MainFlow).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(text = screen.label, style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 바텀시트 / 앱 제한 일시정지 — 앱 제한 플로우 영역 유지
        Text(text = "바텀시트", style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
        listOf(
            DebugScreen.BaseBottomSheet,
            DebugScreen.TermsBottomSheet,
            DebugScreen.AppLimitSetupTime,
            DebugScreen.AppLimitSetupDay,
            DebugScreen.AppLimitInfoBottomSheet,
            DebugScreen.AppLimitInfoBottomSheetDaily,
            DebugScreen.AppLimitInfoBottomSheetPaused,
            DebugScreen.AddAppAppCategoryBottomSheet,
        ).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(text = screen.label, style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "앱 제한 일시정지", style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
        listOf(
            DebugScreen.AppLimitPauseProposal,
            DebugScreen.AppLimitPauseConfirm,
            DebugScreen.AppLimitPauseComplete,
        ).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(text = screen.label, style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "디버그", style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
        listOf(DebugScreen.CrashLogs).forEach { screen ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onScreenSelect(screen) }
                    .padding(16.dp),
            ) {
                Text(text = screen.label, style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            }
        }
    }
}

/** 디자인 시스템 하위 섹션 (스크린샷 카테고리 기준) */
private sealed class DesignSystemSection(val label: String) {
    data object Typography : DesignSystemSection("타이포그래피")
    data object Buttons : DesignSystemSection("버튼")
    data object Progress : DesignSystemSection("프로그레스")
    data object Infobox : DesignSystemSection("인포박스")
    data object Lists : DesignSystemSection("리스트")
    data object Form : DesignSystemSection("폼")
    data object Navigation : DesignSystemSection("네비게이션")
    data object Select : DesignSystemSection("선택")
    data object IconsLabel : DesignSystemSection("아이콘/라벨")
    data object BottomSheets : DesignSystemSection("바텀시트")
    data object Dialogs : DesignSystemSection("다이얼로그")
    data object MedalAnimation : DesignSystemSection("메달 애니메이션 테스트")
    data object AiAppCategoryClassification : DesignSystemSection("AI 앱 카테고리 분류 테스트")
    /** 테스트 영역에서 이동 — 화면 목차 테스트 영역 삭제된 항목 */
    data object Tests : DesignSystemSection("테스트")
}

@Composable
private fun DebugDesignSystemSection() {
    var selectedSection by remember { mutableStateOf<DesignSystemSection?>(null) }

    if (selectedSection != null) {
        DebugDesignSystemDetailSection(
            section = selectedSection!!,
            onBack = { selectedSection = null },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "디자인 시스템",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            )
            listOf(
                DesignSystemSection.Typography,
                DesignSystemSection.Buttons,
                DesignSystemSection.Progress,
                DesignSystemSection.Infobox,
                DesignSystemSection.Lists,
                DesignSystemSection.Form,
                DesignSystemSection.Navigation,
                DesignSystemSection.Select,
                DesignSystemSection.IconsLabel,
                DesignSystemSection.BottomSheets,
                DesignSystemSection.Dialogs,
                DesignSystemSection.MedalAnimation,
                DesignSystemSection.AiAppCategoryClassification,
                DesignSystemSection.Tests,
            ).forEach { section ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selectedSection = section }
                        .padding(16.dp),
                ) {
                    Text(
                        text = section.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugDesignSystemDetailSection(
    section: DesignSystemSection,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AptoxGhostButton(text = "← 목차로", onClick = onBack)
        when (section) {
            DesignSystemSection.Typography -> DebugTypographyContent()
            DesignSystemSection.Buttons -> DebugButtonsContent()
            DesignSystemSection.Progress -> DebugProgressContent()
            DesignSystemSection.Infobox -> DebugInfoboxContent()
            DesignSystemSection.Lists -> DebugListsContent()
            DesignSystemSection.Form -> DebugFormContent()
            DesignSystemSection.Navigation -> DebugNavigationContent()
            DesignSystemSection.Select -> DebugSelectContent()
            DesignSystemSection.IconsLabel -> DebugIconsLabelContent()
            DesignSystemSection.BottomSheets -> DebugBottomSheetsContent(onBack = onBack)
            DesignSystemSection.Dialogs -> DebugDialogsContent()
            DesignSystemSection.MedalAnimation -> MedalAnimationTestScreen(onBack = onBack)
            DesignSystemSection.AiAppCategoryClassification -> AiAppCategoryClassificationScreen(onBack = onBack)
            DesignSystemSection.Tests -> DebugTestsContent(onBack = onBack)
        }
    }
}

@Composable
private fun DebugTestsContent(onBack: () -> Unit) {
    var selectedScreen by remember { mutableStateOf<DebugScreen?>(null) }
    val testScreens = listOf(
        DebugScreen.ToastAnimationTest,
        DebugScreen.DrumrollTimePicker,
        DebugScreen.AppIconTest,
        DebugScreen.UsageStatsTest,
        DebugScreen.AppMonitorTest,
        DebugScreen.BlockOverlayTest,
    )
    if (selectedScreen != null) {
        // 부모 Column이 verticalScroll — 자식에 fillMaxSize 금지(스크롤 뷰 무한 높이)
        Box(modifier = Modifier.fillMaxWidth()) {
            DebugScreenPreview(
                screen = selectedScreen!!,
                onBack = { selectedScreen = null },
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AptoxGhostButton(text = "← 목차로", onClick = onBack)
            DebugSectionTitle("테스트")
            testScreens.forEach { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .clickable { selectedScreen = screen }
                        .padding(16.dp),
                ) {
                    Text(
                        text = screen.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugBottomSheetsContent(onBack: () -> Unit) {
    var selectedSheet by remember { mutableStateOf<DebugScreen?>(null) }

    if (selectedSheet != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            DebugScreenPreview(
                screen = selectedSheet!!,
                onBack = { selectedSheet = null },
            )
        }
    } else {
        // 부모 DebugDesignSystemDetailSection에 이미 verticalScroll 있음 → 중첩 스크롤 금지
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AptoxGhostButton(text = "← 목차로", onClick = onBack)
            DebugSectionTitle("바텀시트")
            listOf(
                DebugScreen.BaseBottomSheet,
                DebugScreen.TermsBottomSheet,
                DebugScreen.AppLimitSetupTime,
                DebugScreen.AppLimitSetupDay,
                DebugScreen.AppLimitInfoBottomSheet,
                DebugScreen.AppLimitInfoBottomSheetDaily,
                DebugScreen.AppLimitInfoBottomSheetPaused,
                DebugScreen.AddAppAppCategoryBottomSheet,
                DebugScreen.AppLimitPauseProposal,
                DebugScreen.AppLimitPauseConfirm,
                DebugScreen.AppLimitPauseComplete,
            ).forEach { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .clickable { selectedSheet = screen }
                        .padding(16.dp),
                ) {
                    Text(
                        text = screen.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugDialogsContent() {
    var showGuideDialog by remember { mutableStateOf(false) }
    var showRequiredPermissionDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("다이얼로그")
        Text(
            text = "AptoxGuideDialog (Figma 310:2725) — 아이콘 + 제목 + 부제 + 날짜 + 2줄 버튼",
            style = AppTypography.Caption1.copy(color = AppColors.TextBody),
        )
        AptoxPrimaryButton(
            text = "가이드 다이얼로그 미리보기",
            onClick = { showGuideDialog = true },
        )
        Text(
            text = "AptoxRequiredPermissionDialog (Figma 1285-4277) — 필수 권한 안내 · 단일 닫기",
            style = AppTypography.Caption1.copy(color = AppColors.TextBody),
        )
        AptoxPrimaryButton(
            text = "필수 권한 다이얼로그 미리보기",
            onClick = { showRequiredPermissionDialog = true },
        )
    }
    if (showGuideDialog) {
        AptoxGuideDialog(
            onDismissRequest = { showGuideDialog = false },
            title = "꾸준한 실천",
            subtitle = "오늘도 목표를 향해 한 걸음 더 나아갔어요.",
            date = "2025.02.25",
            primaryButtonText = "계속 진행",
            secondaryButtonText = "돌아가기",
            onPrimaryClick = { showGuideDialog = false },
            onSecondaryClick = { showGuideDialog = false },
        )
    }
    if (showRequiredPermissionDialog) {
        AptoxRequiredPermissionDialog(
            onDismissRequest = { showRequiredPermissionDialog = false },
            onCloseClick = { showRequiredPermissionDialog = false },
        )
    }
}

@Composable
private fun DebugSectionTitle(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.TextPrimary),
        )
    }
}

@Composable
private fun DebugTypographyContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("타이포그래피")
        val typographyItems = listOf(
            "Display1" to AppTypography.Display1,
            "Display2" to AppTypography.Display2,
            "Display3" to AppTypography.Display3,
            "HeadingH1" to AppTypography.HeadingH1,
            "HeadingH2" to AppTypography.HeadingH2,
            "HeadingH3" to AppTypography.HeadingH3,
            "BodyMedium" to AppTypography.BodyMedium,
            "BodyRegular" to AppTypography.BodyRegular,
            "BodyBold" to AppTypography.BodyBold,
            "Caption1" to AppTypography.Caption1,
            "Caption2" to AppTypography.Caption2,
            "Label" to AppTypography.Label,
            "ButtonLarge" to AppTypography.ButtonLarge,
            "ButtonSmall" to AppTypography.ButtonSmall,
            "Disclaimer" to AppTypography.Disclaimer,
            "Input" to AppTypography.Input,
            "TabSelected" to AppTypography.TabSelected,
            "TabUnselected" to AppTypography.TabUnselected,
        )
        typographyItems.forEach { (name, style) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                )
                Text(
                    text = "스마트폰에 빼앗긴",
                    style = style.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = "당신의 소중한 일상",
                    style = style.copy(color = AppColors.TextPrimary),
                )
            }
        }
    }
}

@Composable
private fun DebugButtonsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Buttons")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AptoxTwoLineButton("계속 진행", "돌아가기", onPrimaryClick = {}, onGhostClick = {})
            AptoxTwoLineButton("계속 진행", "돌아가기", onPrimaryClick = {}, onGhostClick = {}, enabled = false)
            AptoxGhostButton(text = "자세히 보기", onClick = {})
            AptoxGhostButton(text = "자세히 보기", onClick = {}, enabled = false)
            AptoxAddAppButton(text = "사용제한 앱 추가", icon = painterResource(R.drawable.ic_add_circle), onClick = {})
        }
    }
}

@Composable
private fun DebugProgressContent() {
    var sliderIndex by remember { mutableIntStateOf(1) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Progress")
        val steps = listOf("30분", "60분", "120분", "180분", "240분", "360분")
        AptoxStepBar(steps = steps, selectedIndex = sliderIndex, onStepSelected = { sliderIndex = it })
        AptoxLinearProgressBar(progress = 0.15f)
    }
}

@Composable
private fun DebugInfoboxContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Infobox")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                listOf(
                    "선택된 앱" to "인스타그램",
                    "일일 사용시간" to "1시간 30분",
                    "반복 요일" to "월, 화, 수, 목",
                    "적용 기간" to "4주",
                    "시작 시기" to "지금 즉시",
                ).forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = label, style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
                        Text(text = value, style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight))
                    }
                }
            }
            AptoxInfoBox(
                text = "사용 중 10분간의 시간이 끝나면 자동으로 다시 차단되며, 현재까지 진행된 시간엔 반영이 되지 않아요\n※ 10분 사용은 하루 최대 2회까지 가능해요.",
            )
        }
    }
}

@Composable
private fun DebugListsContent() {
    var switchOff by remember { mutableStateOf(false) }
    var switchOn by remember { mutableStateOf(true) }
    var selectionRowSwitch by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Lists")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("스위치 (AptoxSwitch / AptoxToggleSwitch)", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            Row(
                modifier = Modifier.height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    AptoxSwitch(checked = switchOff, onCheckedChange = { switchOff = it })
                }
                Box(modifier = Modifier.wrapContentSize()) {
                    AptoxSwitch(checked = switchOn, onCheckedChange = { switchOn = it })
                }
            }
            Text("AppStatusRow", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            AppStatusRow(appName = "인스타그램", appIcon = painterResource(R.drawable.ic_app_placeholder))
            AppStatusRow(
                appName = "인스타그램",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                variant = AppStatusVariant.Button,
                usageText = "14분",
                usageLabel = "30분 사용 중",
                onDetailClick = {},
            )
            AppStatusRow(
                appName = "인스타그램",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                variant = AppStatusVariant.DataView,
                usageMinutes = "144분",
                sessionCount = "12회",
            )
            Text("LabelDanger / AppStatusDataViewRow (Figma 901-3018)", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            LabelDanger(text = "제한 중")
            AppStatusDataViewRow(
                appName = "인스타그램",
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                totalUsageMinutes = "9,999분",
                infoText = "이 시간이면 서울 부산 KTX 왕복 8번이에요!",
            )
            Text("AptoxInfoBoxCompact", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            AptoxInfoBoxCompact(text = "이 시간이면 서울 부산 KTX 왕복 8번이에요!")
            Text("SelectionRow", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Selected, selectedValue = "인스타그램", onClick = {})
            SelectionRow(label = "앱의 종류를 지정해주세요", variant = SelectionRowVariant.Selected, selectedValue = "게임", onClick = {})
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Default, onClick = {})
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Switch, switchChecked = selectionRowSwitch, onSwitchChange = { selectionRowSwitch = it }, onClick = {})
        }
    }
}

@Composable
private fun DebugFormContent() {
    var inputValue by remember { mutableStateOf("") }
    var errorValue by remember { mutableStateOf("user@mail.com") }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Form")
        val hint = "영어 대/소문자 및 숫자 포함 8자리 이상"
        AptoxTextFieldDefault(value = inputValue, onValueChange = { inputValue = it }, placeholder = hint)
        AptoxTextFieldDisabled(value = "", placeholder = hint)
        AptoxTextFieldError(value = errorValue, onValueChange = { errorValue = it }, placeholder = hint)
        AptoxTextField(value = "", onValueChange = {}, placeholder = hint)
    }
}

@Composable
private fun DebugNavigationContent() {
    var navIndex by remember { mutableIntStateOf(0) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var tabIndexLimit by remember { mutableIntStateOf(0) }
    val navDestinations = listOf(
        NavDestination("홈", R.drawable.ic_nav_home_inactive, R.drawable.ic_nav_home_active),
        NavDestination("챌린지", R.drawable.ic_nav_challenge_inactive, R.drawable.ic_nav_challenge_active),
        NavDestination("통계", R.drawable.ic_nav_stats_inactive, R.drawable.ic_nav_stats_active),
        NavDestination("설정", R.drawable.ic_nav_mypage_inactive, R.drawable.ic_nav_mypage_active),
    )
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Navigation")
        AptoxBottomNavBar(
            destinations = navDestinations,
            selectedIndex = navIndex,
            onTabSelected = { navIndex = it },
        )
        AptoxHeaderTitleWithNotification(title = "통계", hasNotification = true)
        AptoxHeaderSub(title = "통계", backIcon = painterResource(R.drawable.ic_back), onBackClick = {}, showNotification = true, hasNotification = true)
        AptoxHeaderHome(logo = painterResource(R.drawable.ic_logo), hasNotification = true)
        AptoxSegmentedTab(items = listOf("오늘", "주간", "연간", "월간"), selectedIndex = tabIndex, onTabSelected = { tabIndex = it })
        AptoxSegmentedTab(items = listOf("주간", "월간", "연간"), selectedIndex = tabIndex.coerceAtMost(2), onTabSelected = { tabIndex = it })
        AptoxSegmentedTab(items = listOf("시간 지정 제한", "일일 제한 시간"), selectedIndex = tabIndexLimit, onTabSelected = { tabIndexLimit = it })
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("5분간 일시정지 완료", style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary))
            Text("지금부터 5분간 앱을 사용하실 수 있어요\n이제 남은 기회는 1번이에요", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            AptoxPrimaryButton(text = "다음", onClick = {})
            AptoxGhostButton(text = "취소", onClick = {})
        }
    }
}

@Composable
private fun DebugSelectContent() {
    var chipSelected by remember { mutableStateOf(setOf(0, 1, 2, 3)) }
    var cardIndex by remember { mutableIntStateOf(0) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Select")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AptoxCheckBox(checked = true, onCheckedChange = {})
            AptoxCheckBox(checked = false, onCheckedChange = {})
            AptoxSwitch(checked = true, onCheckedChange = {})
            AptoxSwitch(checked = false, onCheckedChange = {})
            AptoxRadioButton(selected = true, onClick = {})
            AptoxRadioButton(selected = false, onClick = {})
        }
        AptoxChipRow(labels = listOf("월", "화", "수", "목", "금", "토", "일"), selectedIndices = chipSelected, onChipClick = { i -> chipSelected = if (i in chipSelected) chipSelected - i else chipSelected + i })
        AptoxSelectionCardGroup(
            items = listOf(
                SelectionCardItem("즉시 차단 해제", "앱 사용 차단을 종료하고\n다시 앱을 사용할 수 있어요", "1,900원"),
                SelectionCardItem("10분간 사용하기", "앱 사용 차단을 종료하고\n다시 앱을 사용할 수 있어요", "0/2회"),
            ),
            selectedIndex = cardIndex,
            onItemSelected = { cardIndex = it },
        )
    }
}

@Composable
private fun DebugIconsLabelContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Icons / Label")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IcoAppLockOn()
            IcoAppLabel()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painter = painterResource(R.drawable.ic_naver), contentDescription = null, modifier = Modifier.size(48.dp))
            Image(painter = painterResource(R.drawable.ic_kakao), contentDescription = null, modifier = Modifier.size(48.dp))
            Image(painter = painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(48.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(painter = painterResource(R.drawable.ic_notification_on), contentDescription = null, modifier = Modifier.size(36.dp))
            Image(painter = painterResource(R.drawable.ic_notification_off), contentDescription = null, modifier = Modifier.size(36.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painter = painterResource(R.drawable.ic_error_info), contentDescription = null, modifier = Modifier.size(24.dp))
            Image(painter = painterResource(R.drawable.ic_disclaimer_info), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            LabelPro()
            LabelWarning()
            LabelDanger()
        }
    }
}

@Composable
private fun DebugTestActionRow(
    rowKey: String,
    leftText: String,
    actions: List<Pair<String, () -> Unit>>,
    lastAction: Pair<String, String>?,
    onAction: (String, String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Grey100)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = leftText,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { (label, onClick) ->
                val isActive = lastAction?.first == rowKey && lastAction?.second == label
                Text(
                    text = label,
                    style = AppTypography.Caption1.copy(
                        color = if (isActive) AppColors.Primary400 else AppColors.TextBody,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            onClick = {
                                onClick()
                                onAction(rowKey, label)
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DebugTestSettingsSection() {
    if (!com.aptox.app.BuildConfig.DEBUG) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notificationCount by remember { mutableIntStateOf(DebugTestSettings.debugNotificationHistoryCount ?: 0) }
    var restrictions by remember { mutableStateOf(AppRestrictionRepository(context).getAll()) }
    val dailyApps = remember(restrictions) { restrictions.filter { it.blockUntilMs == 0L } }

    fun refresh() { restrictions = AppRestrictionRepository(context).getAll() }

    @Composable
    fun DebugDivider() {
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.BorderDivider),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "테스트 설정",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )

        // 1. 제한앱 모두 삭제
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "제한앱 모두 삭제", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "제한 앱 목록을 초기화하고 모니터 서비스를 중지합니다.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            AptoxPrimaryButton(
                text = "제한앱 모두 삭제",
                onClick = {
                    AppRestrictionRepository(context).clearAll()
                    AppMonitorService.stop(context)
                    refresh()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DebugDivider()

        // 2. 일일사용량 제한 테스트
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "일일사용량 제한 테스트", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "등록된 제한 앱의 오늘 누적 사용량을 ManualTimerRepository에 강제 저장합니다.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            if (dailyApps.isEmpty()) {
                Text(text = "등록된 일일사용량 제한 앱이 없습니다.", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
            } else {
                var selectedApp by remember { mutableStateOf(dailyApps.firstOrNull()) }
                var usageMinutes by remember { mutableStateOf("0") }
                DebugDropdownRow(
                    label = "제한 앱",
                    options = dailyApps.map { "${it.appName} (${it.limitMinutes}분)" },
                    selectedIndex = dailyApps.indexOf(selectedApp).coerceAtLeast(0),
                    onSelect = { selectedApp = dailyApps.getOrNull(it) },
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "오늘 사용량 (분)", style = AppTypography.Caption1.copy(color = AppColors.TextBody))
                    BasicTextField(
                        value = usageMinutes,
                        onValueChange = { if (it.all { c -> c.isDigit() }) usageMinutes = it },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppColors.Grey150)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                AptoxPrimaryButton(
                    text = "사용량 강제 설정",
                    onClick = {
                        selectedApp?.let { app ->
                            ManualTimerRepository(context).setTodayUsageMinutes(app.packageName, usageMinutes.toIntOrNull() ?: 0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        DebugDivider()

        // 2-1. 하루 사용량 3분 옵션
        var show3MinOption by remember { mutableStateOf(DebugTestSettings.debugShow3MinDailyOption) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "하루 사용량 3분 항목", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "일일 사용량 제한 화면에서 3분 선택지를 표시할지 여부. 테스트 시 ON, 출시용 체험 시 OFF.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = if (show3MinOption) "3분 표시 (테스트용)" else "3분 숨김", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
                AptoxSwitch(
                    checked = show3MinOption,
                    onCheckedChange = {
                        show3MinOption = it
                        DebugTestSettings.debugShow3MinDailyOption = it
                    },
                )
            }
        }
        DebugDivider()

        // 3. 알림내역 갯수
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "알림내역 갯수", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "알림내역 화면에 표시할 테스트 아이템 수를 선택합니다.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 3, 5, 10).forEach { count ->
                    val isSelected = notificationCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AppColors.Primary300 else AppColors.Grey200)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                notificationCount = count
                                DebugTestSettings.debugNotificationHistoryCount = count
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Text(text = "${count}개", style = AppTypography.Caption1.copy(color = if (isSelected) AppColors.TextInvert else AppColors.TextBody), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
        DebugDivider()

        // 4. 뱃지 강제 지급
        val badgeIds = (1..18).map { "badge_%03d".format(it) }
        var selectedBadgeId by remember { mutableStateOf(badgeIds.first()) }
        var showBadgeGranted by remember { mutableStateOf<String?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "뱃지 강제 지급", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "획득 팝업 정상 표시 여부 확인용. Firestore users/{userId}/badges에 저장됩니다.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            DebugDropdownRow(
                label = "뱃지",
                options = badgeIds,
                selectedIndex = badgeIds.indexOf(selectedBadgeId).coerceAtLeast(0),
                onSelect = { selectedBadgeId = badgeIds.getOrNull(it) ?: badgeIds.first() },
            )
            AptoxPrimaryButton(
                text = "강제 지급",
                onClick = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) return@AptoxPrimaryButton
                    scope.launch {
                        kotlin.runCatching { BadgeRepository(context = context).grantBadge(uid, selectedBadgeId) }
                        showBadgeGranted = selectedBadgeId
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showBadgeGranted != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showBadgeGranted = null },
                title = { Text("뱃지 지급됨") },
                text = { Text("${showBadgeGranted} 가 Firestore에 저장되었습니다.") },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { showBadgeGranted = null }) { Text("확인") } },
            )
        }
        DebugDivider()

        // 5. 달성 조건 수치 조작
        val progressRepo = remember { BadgeProgressRepository(context) }
        var accumInput by remember { mutableStateOf(progressRepo.accumulatedAchievementDays.toString()) }
        var consecInput by remember { mutableStateOf(progressRepo.consecutiveAchievementDays.toString()) }
        var showProgressResult by remember { mutableStateOf<String?>(null) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "달성 조건 수치 조작", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "누적 달성일을 반영 후 badge_004~009 지급 조건만 시도합니다. (실제 로직은 BadgeAutoGrant + 자정 리셋)", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "누적 달성일", style = AppTypography.Caption1.copy(color = AppColors.TextBody), modifier = Modifier.widthIn(min = 80.dp))
                BasicTextField(value = accumInput, onValueChange = { if (it.all { c -> c.isDigit() }) accumInput = it }, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(AppColors.Grey150).padding(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "연속 달성일", style = AppTypography.Caption1.copy(color = AppColors.TextBody), modifier = Modifier.widthIn(min = 80.dp))
                BasicTextField(value = consecInput, onValueChange = { if (it.all { c -> c.isDigit() }) consecInput = it }, modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(AppColors.Grey150).padding(12.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AptoxPrimaryButton(text = "저장", onClick = {
                    progressRepo.accumulatedAchievementDays = accumInput.toIntOrNull() ?: 0
                    progressRepo.consecutiveAchievementDays = consecInput.toIntOrNull() ?: 0
                }, modifier = Modifier.weight(1f))
                AptoxPrimaryButton(text = "오늘 달성 처리", onClick = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (uid == null) { showProgressResult = "로그인 필요"; return@AptoxPrimaryButton }
                    val accum = progressRepo.accumulatedAchievementDays
                    val consec = progressRepo.consecutiveAchievementDays
                    scope.launch {
                        val granted = BadgeAutoGrant.debugApplyProgressAndGrant(context, uid, accum, consec)
                        showProgressResult = if (granted.isEmpty()) "새로 지급된 뱃지 없음" else "지급됨: ${granted.joinToString()}"
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
        if (showProgressResult != null) {
            androidx.compose.material3.AlertDialog(onDismissRequest = { showProgressResult = null }, title = { Text("달성 처리") }, text = { Text(showProgressResult!!) }, confirmButton = { androidx.compose.material3.TextButton(onClick = { showProgressResult = null }) { Text("확인") } })
        }
        DebugDivider()

        // 6. 앱 사용제한 기록 초기화
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "앱 사용제한 기록 초기화", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "비로그인 로컬 기록 + timeout dedup prefs 삭제. 로그인 시 Firestore users/{userId}/appLimitLogs 전체 삭제.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            AptoxPrimaryButton(
                text = "앱 사용제한 기록 초기화",
                onClick = {
                    scope.launch {
                        AppLimitLogRepository.clearTimeoutPrefs(context)
                        AppLimitLogLocalPreferences.clear(context)
                        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) runCatching { AppLimitLogRepository().clearAll(uid) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DebugDivider()

        // 7. 뱃지 전체 초기화
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "뱃지 전체 초기화", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary))
            Text(text = "Firestore users/{userId}/badges 전체 삭제 + badgeProgress 리셋. 처음부터 다시 테스트할 때 사용.", style = AppTypography.Caption1.copy(color = AppColors.TextSecondary))
            AptoxPrimaryButton(
                text = "뱃지 초기화",
                onClick = {
                    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@AptoxPrimaryButton
                    scope.launch {
                        BadgeRepository(context = context).deleteAllUserBadges(uid)
                        BadgeProgressRepository(context).resetAll()
                        BadgeStatsPreferences.resetAll(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DebugDropdownRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedText = options.getOrNull(selectedIndex) ?: "선택"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Grey150)
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "$label: $selectedText", style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary), modifier = Modifier.weight(1f))
        Text(text = "▼", style = AppTypography.Caption1.copy(color = AppColors.TextBody))
    }
    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    options.forEachIndexed { index, opt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(index); showDialog = false }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(opt, style = AppTypography.BodyMedium.copy(color = if (index == selectedIndex) AppColors.Primary400 else AppColors.TextPrimary))
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showDialog = false }) { Text("닫기") } },
        )
    }
}
