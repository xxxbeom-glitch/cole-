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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
        DebugScreen.Login -> LoginScreen(
            logo = painterResource(R.drawable.ic_logo),
            onLoginClick = { _, _ -> onBack() },
            onSignUpClick = onBack,
            onNaverLoginClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onForgotPasswordClick = {},
        )
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
        DebugScreen.SelfTestLoading -> SelfTestLoadingScreen(onFinish = onBack)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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

@Composable
private fun DebugDesignSystemSection() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // 타이포그래피
        Text(
            text = "타이포그래피 (AppTypography)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
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

        Spacer(modifier = Modifier.height(16.dp))

        // 에셋
        Text(
            text = "에셋 (아이콘/이미지)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        val drawables = listOf(
            R.drawable.ic_logo to "ic_logo",
            R.drawable.ic_back to "ic_back",
            R.drawable.ic_add to "ic_add",
            R.drawable.ic_chevron_right to "ic_chevron_right",
            R.drawable.ic_notification_on to "ic_notification_on",
            R.drawable.ic_notification_off to "ic_notification_off",
            R.drawable.ic_error_info to "ic_error_info",
            R.drawable.ic_disclaimer_info to "ic_disclaimer_info",
            R.drawable.ic_lock_app to "ic_lock_app",
            R.drawable.ic_lock_label to "ic_lock_label",
            R.drawable.ic_app_placeholder to "ic_app_placeholder",
            R.drawable.ic_lock to "ic_lock",
            R.drawable.ic_app_lock to "ic_app_lock",
            R.drawable.ob_04_logo to "ob_04_logo",
        )
        drawables.chunked(4).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chunk.forEach { (resId, label) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Grey200),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(resId),
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                            )
                        }
                        Text(
                            text = label,
                            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // 소셜 로그인 아이콘 (png 등)
        val socialDrawables = listOf(
            R.drawable.ic_naver to "ic_naver",
            R.drawable.ic_kakao to "ic_kakao",
            R.drawable.ic_google to "ic_google",
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            socialDrawables.forEach { (resId, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(AppColors.Grey200),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(resId),
                            contentDescription = label,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 컴포넌트
        Text(
            text = "컴포넌트",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ColePrimaryButton(text = "Primary 버튼", onClick = {})
            ColeGhostButton(text = "Ghost 버튼", onClick = {})
            ColeSecondaryButton(text = "Secondary 버튼", onClick = {})
            ColeHeaderSub(
                title = "헤더 (Sub)",
                backIcon = painterResource(R.drawable.ic_back),
                onBackClick = {},
                showNotification = true,
                hasNotification = true,
            )
            ColeHeaderHome(
                logo = painterResource(R.drawable.ic_logo),
                hasNotification = true,
            )
            ColeInfoBox(text = "ColeInfoBox 예시 텍스트")
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
