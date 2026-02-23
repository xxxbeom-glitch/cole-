package com.cole.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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
) {
    var selectedScreen by remember { mutableStateOf<DebugScreen?>(null) }

    if (selectedScreen != null) {
        Box(modifier = modifier.fillMaxSize()) {
            DebugScreenPreview(
                screen = selectedScreen!!,
                onBack = { selectedScreen = null },
            )
        }
    } else {
        DebugMenuScreen(
            onScreenSelect = { selectedScreen = it },
            onStartNormalFlow = onStartNormalFlow,
            modifier = modifier.fillMaxSize(),
        )
    }
}

/** 디버그 메뉴에서 이동 가능한 화면 */
sealed class DebugScreen(val category: String, val label: String) {
    // 인증/온보딩
    data object Splash : DebugScreen("인증/온보딩", "Splash")
    data object Login : DebugScreen("인증/온보딩", "Login")
    data object SignUpEmail : DebugScreen("인증/온보딩", "회원가입 - 이메일")
    data object SignUpPassword : DebugScreen("인증/온보딩", "회원가입 - 비밀번호")
    data object SignUpNameBirthPhone : DebugScreen("인증/온보딩", "회원가입 - 이름/생년월일/휴대전화")
    data object SignUpVerification : DebugScreen("인증/온보딩", "회원가입 - 인증코드")
    data object SignUpComplete : DebugScreen("인증/온보딩", "회원가입 완료")
    data object Onboarding : DebugScreen("인증/온보딩", "온보딩")
    data object SelfTest : DebugScreen("인증/온보딩", "자가테스트")
    data object SelfTestLoading : DebugScreen("인증/온보딩", "자가테스트 로딩")
    data object SelfTestResult : DebugScreen("인증/온보딩", "자가테스트 결과")

    // 앱 제한
    data object AddAppAA01 : DebugScreen("앱 제한", "앱 선택 (AA-01)")
    data object AddAppAA02A01 : DebugScreen("앱 제한", "제한 방식 선택 (AA-02A-01)")
    data object AddAppDaily01 : DebugScreen("앱 제한", "일일 사용 한도 (Daily 01)")
    data object AddAppDaily02 : DebugScreen("앱 제한", "사용량 경고 (Daily 02)")
    data object AddAppDaily03 : DebugScreen("앱 제한", "적용 요일 (Daily 03)")
    data object AddAppDaily04 : DebugScreen("앱 제한", "설정 요약 (Daily 04)")
    data object AddAppDaily05 : DebugScreen("앱 제한", "완료 (Daily 05)")
    data object AddAppFlowHost : DebugScreen("앱 제한", "앱 제한 플로우 전체")

    // 메인
    data object MainFlow : DebugScreen("메인", "메인 (홈/챌린지/통계/마이)")

    // 바텀시트
    data object BaseBottomSheet : DebugScreen("바텀시트", "BaseBottomSheet (기본형)")
    data object TermsBottomSheet : DebugScreen("바텀시트", "TermsBottomSheet (약관 동의)")
    data object AppLimitSetupTime : DebugScreen("바텀시트", "AppLimitSetupTime (시간 슬라이더)")
    data object AppLimitSetupDay : DebugScreen("바텀시트", "AppLimitSetupDay (요일 선택)")
    data object AppLimitInfoBottomSheet : DebugScreen("바텀시트", "AppLimitInfoBottomSheet (제한 앱 정보)")
}

@Composable
private fun DebugScreenPreview(
    screen: DebugScreen,
    onBack: () -> Unit,
) {
    when (screen) {
        DebugScreen.Splash -> DebugSplashPreview(onBack = onBack)
        DebugScreen.Login -> DebugLoginPreview(onBack = onBack)
        DebugScreen.SignUpEmail -> SignUpEmailScreen(
            onNextClick = { onBack() },
            onBackClick = onBack,
        )
        DebugScreen.SignUpPassword -> SignUpPasswordScreen(
            onNextClick = { onBack() },
            onBackClick = onBack,
        )
        DebugScreen.SignUpNameBirthPhone -> SignUpNameBirthPhoneScreen(
            onNextClick = { _, _, _ -> onBack() },
            onBackClick = onBack,
        )
        DebugScreen.SignUpVerification -> SignUpVerificationCodeScreen(
            onNextClick = { onBack() },
            onBackClick = onBack,
            onResendClick = {},
        )
        DebugScreen.SignUpComplete -> SignUpCompleteScreen(
            onStartClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.Onboarding -> OnboardingScreen(
            onSkipClick = onBack,
            onStartClick = onBack,
        )
        DebugScreen.SelfTest -> SelfTestScreen(
            onBackClick = onBack,
            onComplete = { onBack() },
        )
        DebugScreen.SelfTestLoading -> SelfTestLoadingScreen(onFinish = onBack)
        DebugScreen.SelfTestResult -> SelfTestResultScreen(
            resultType = SelfTestResultType.MIDDLE,
            onStartClick = onBack,
            onBackClick = onBack,
            rawScore = 12,
            userName = "디버그",
        )
        DebugScreen.AddAppAA01 -> AddAppScreenAA01(
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
            limitMinutes = "30분",
            warnEnabled = true,
            onConfirmClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppDaily05 -> AddAppDailyLimitScreen05(
            appName = "인스타그램",
            onCompleteClick = onBack,
            onBackClick = onBack,
        )
        DebugScreen.AddAppFlowHost -> AddAppFlowHost(
            onComplete = onBack,
            onBackFromFirst = onBack,
        )
        DebugScreen.BaseBottomSheet -> DebugBottomSheetPreview(onBack = onBack) { onSheetDismiss ->
            BaseBottomSheet(
                title = "앱을 선택해주세요",
                subtitle = "앱은 최대 3개까지 선택 가능합니다",
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
        else -> DebugPlaceholderScreen(screen = screen, onBack = onBack)
    }
}

@Composable
private fun DebugSplashPreview(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        SplashScreen(onFinish = onBack)
        ColeGhostButton(
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
private fun DebugLoginPreview(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        LoginScreen(
            logo = painterResource(R.drawable.ic_login_logo),
            onLoginClick = { _, _ -> onBack() },
            onSignUpClick = onBack,
            onNaverLoginClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onForgotPasswordClick = {},
        )
        ColeGhostButton(
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

/** 바텀시트 미리보기: "바텀시트 열기" 버튼 탭 시 바텀시트 표시 */
@Composable
private fun DebugBottomSheetPreview(
    onBack: () -> Unit,
    content: @Composable (onSheetDismiss: () -> Unit) -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColeGhostButton(text = "돌아가기", onClick = onBack)
        ColePrimaryButton(
            text = "바텀시트 열기",
            onClick = { showSheet = true },
        )
    }

    if (showSheet) {
        content { showSheet = false }
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
        ColePrimaryButton(text = "돌아가기", onClick = onBack)
    }
}

/** Debug 메뉴 메인 화면 */
@Composable
private fun DebugMenuScreen(
    onScreenSelect: (DebugScreen) -> Unit,
    onStartNormalFlow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("화면 목차", "디자인 시스템", "테스트 설정")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
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
                        .clickable { selectedTab = index }
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

        // 컨텐츠 (스크롤)
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> DebugScreenListSection(onScreenSelect = onScreenSelect)
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
            ColePrimaryButton(
                text = "앱 시작하기",
                onClick = onStartNormalFlow,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(
                text = "디버그 메뉴 유지",
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DebugScreenListSection(onScreenSelect: (DebugScreen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 앱 스플래시·로그인 바로가기
        Text(
            text = "바로가기",
            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(DebugScreen.Splash, DebugScreen.Login).forEach { screen ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Primary300)
                        .clickable { onScreenSelect(screen) }
                        .padding(16.dp),
                ) {
                    Text(
                        text = screen.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextInvert),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 자가테스트 플로우 바로가기
        Text(
            text = "자가테스트 플로우",
            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(DebugScreen.SelfTest, DebugScreen.SelfTestLoading, DebugScreen.SelfTestResult).forEach { screen ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Primary300)
                        .clickable { onScreenSelect(screen) }
                        .padding(12.dp),
                ) {
                    Text(
                        text = screen.label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextInvert),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        val allScreens = listOf(
            DebugScreen.Splash,
            DebugScreen.Login,
            DebugScreen.SignUpEmail,
            DebugScreen.SignUpPassword,
            DebugScreen.SignUpNameBirthPhone,
            DebugScreen.SignUpVerification,
            DebugScreen.SignUpComplete,
            DebugScreen.Onboarding,
            DebugScreen.SelfTest,
            DebugScreen.SelfTestLoading,
            DebugScreen.SelfTestResult,
            DebugScreen.AddAppAA01,
            DebugScreen.AddAppAA02A01,
            DebugScreen.AddAppDaily01,
            DebugScreen.AddAppDaily02,
            DebugScreen.AddAppDaily03,
            DebugScreen.AddAppDaily04,
            DebugScreen.AddAppDaily05,
            DebugScreen.AddAppFlowHost,
            DebugScreen.MainFlow,
            DebugScreen.BaseBottomSheet,
            DebugScreen.TermsBottomSheet,
            DebugScreen.AppLimitSetupTime,
            DebugScreen.AppLimitSetupDay,
            DebugScreen.AppLimitInfoBottomSheet,
        )
        val grouped = allScreens.groupBy { it.category }

        grouped.forEach { (category, screens) ->
            Text(
                text = category,
                style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
            )
            screens.forEach { screen ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .clickable { onScreenSelect(screen) }
                        .padding(16.dp),
                ) {
                    Text(
                        text = screen.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
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
            ).forEach { section ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .clickable { selectedSection = section }
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
        ColeGhostButton(text = "← 목차로", onClick = onBack)
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
        }
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
            ColeTwoLineButton("계속 진행", "돌아가기", onPrimaryClick = {}, onGhostClick = {})
            ColeTwoLineButton("계속 진행", "돌아가기", onPrimaryClick = {}, onGhostClick = {}, enabled = false)
            ColeGhostButton(text = "자세히 보기", onClick = {})
            ColeGhostButton(text = "자세히 보기", onClick = {}, enabled = false)
            ColeAddAppButton(text = "잠시만 멀어질 앱 추가하기", icon = painterResource(R.drawable.ic_add), onClick = {})
        }
    }
}

@Composable
private fun DebugProgressContent() {
    var sliderIndex by remember { mutableIntStateOf(1) }
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Progress")
        val steps = listOf("30분", "60분", "120분", "180분", "240분", "360분")
        ColeStepBar(steps = steps, selectedIndex = sliderIndex, onStepSelected = { sliderIndex = it })
        ColeLinearProgressBar(progress = 0.15f)
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
            ColeInfoBox(
                text = "사용 중 10분간의 시간이 끝나면 자동으로 다시 차단되며, 현재까지 진행된 시간엔 반영이 되지 않아요\n※ 10분 사용은 하루 최대 2회까지 가능해요.",
            )
        }
    }
}

@Composable
private fun DebugListsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Lists")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ColeSwitch(checked = false, onCheckedChange = {})
                ColeSwitch(checked = true, onCheckedChange = {})
            }
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Selected, selectedValue = "인스타그램", onClick = {})
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Default, onClick = {})
            SelectionRow(label = "앱을 선택해주세요", variant = SelectionRowVariant.Switch, switchChecked = false, onSwitchChange = {}, onClick = {})
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
        ColeTextFieldDefault(value = inputValue, onValueChange = { inputValue = it }, placeholder = hint)
        ColeTextFieldDisabled(value = "", placeholder = hint)
        ColeTextFieldError(value = errorValue, onValueChange = { errorValue = it }, placeholder = hint)
        ColeTextField(value = "", onValueChange = {}, placeholder = hint)
    }
}

@Composable
private fun DebugNavigationContent() {
    var navIndex by remember { mutableIntStateOf(0) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var tabIndexLimit by remember { mutableIntStateOf(0) }
    val navDestinations = listOf(
        NavDestination("홈", R.drawable.ic_nav_home),
        NavDestination("챌린지", R.drawable.ic_nav_challenge),
        NavDestination("통계", R.drawable.ic_nav_stats),
        NavDestination("마이", R.drawable.ic_nav_mypage),
    )
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        DebugSectionTitle("Navigation")
        ColeBottomNavBar(
            destinations = navDestinations,
            selectedIndex = navIndex,
            onTabSelected = { navIndex = it },
        )
        ColeHeaderTitleWithNotification(title = "통계", hasNotification = true)
        ColeHeaderSub(title = "통계", backIcon = painterResource(R.drawable.ic_back), onBackClick = {}, showNotification = true, hasNotification = true)
        ColeHeaderHome(logo = painterResource(R.drawable.ic_logo), hasNotification = true)
        ColeSegmentedTab(items = listOf("오늘", "주간", "연간", "월간"), selectedIndex = tabIndex, onTabSelected = { tabIndex = it })
        ColeSegmentedTab(items = listOf("주간", "월간", "연간"), selectedIndex = tabIndex.coerceAtMost(2), onTabSelected = { tabIndex = it })
        ColeSegmentedTab(items = listOf("시간 지정 제한", "일일 제한 시간"), selectedIndex = tabIndexLimit, onTabSelected = { tabIndexLimit = it })
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
            ColePrimaryButton(text = "다음", onClick = {})
            ColeGhostButton(text = "취소", onClick = {})
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
            ColeCheckBox(checked = true, onCheckedChange = {})
            ColeCheckBox(checked = false, onCheckedChange = {})
            ColeRadioButton(selected = true, onClick = {})
            ColeRadioButton(selected = false, onClick = {})
        }
        ColeChipRow(labels = listOf("월", "화", "수", "목", "금", "토", "일"), selectedIndices = chipSelected, onChipClick = { i -> chipSelected = if (i in chipSelected) chipSelected - i else chipSelected + i })
        ColeSelectionCardGroup(
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
private fun DebugTestSettingsSection() {
    var subscriptionState by remember { mutableIntStateOf(0) }
    var blockedAppCount by remember { mutableIntStateOf(0) }
    var usageTime by remember { mutableIntStateOf(0) }
    var pause5min by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "테스트 설정",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )

        // 구독 상태
        SettingItem(
            title = "구독 상태",
            options = listOf("무료", "구독중", "만료"),
            selectedIndex = subscriptionState,
            onSelect = { subscriptionState = it },
        )

        // 차단 앱 개수
        SettingItem(
            title = "차단 앱 개수",
            options = listOf("없음", "1개", "5개"),
            selectedIndex = blockedAppCount,
            onSelect = { blockedAppCount = it },
        )

        // 사용 시간
        SettingItem(
            title = "사용 시간",
            options = listOf("0분", "30분", "초과"),
            selectedIndex = usageTime,
            onSelect = { usageTime = it },
        )

        // 5분 일시정지
        SettingItem(
            title = "5분 일시정지",
            options = listOf("미사용", "사용완료"),
            selectedIndex = pause5min,
            onSelect = { pause5min = it },
        )
    }
}

@Composable
private fun SettingItem(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) AppColors.Primary300
                            else AppColors.Grey200
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(
                            color = if (isSelected) AppColors.TextInvert else AppColors.TextBody,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
