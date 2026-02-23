package com.cole.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

@Composable
fun ColeRootContent() {
    var showDebugMenu by remember { mutableStateOf(true) }
    if (showDebugMenu) {
        DebugFlowHost(onStartNormalFlow = { showDebugMenu = false }, modifier = Modifier.fillMaxSize())
    } else {
        SignUpFlowHost()
    }
}

enum class SignUpStep {
    SPLASH,
    LOGIN,
    EMAIL,
    PASSWORD,
    NAME_BIRTH_PHONE,
    VERIFICATION,
    COMPLETE,
    SELFTEST,
    SELFTEST_LOADING,
    SELFTEST_RESULT,
    ADD_APP,
    MAIN,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }
        setContent {
            ColeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.SurfaceBackgroundBackground,
                ) {
                    if (BuildConfig.DEBUG) {
                        ColeRootContent()
                    } else {
                        SignUpFlowHost()
                    }
                }
            }
        }
    }
}

@Composable
fun SignUpFlowHost() {
    var step by remember { mutableStateOf(SignUpStep.SPLASH) }
    var showTerms by remember { mutableStateOf(false) }
    var selfTestAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    // 약관 상태 (필수만, 디자인 기준)
    val agreements = remember {
        mutableStateListOf(
            AgreementItem("(필수) 만 18세 이상입니다", true, false),
            AgreementItem("(필수) 이용약관에 동의합니다", true, false),
            AgreementItem("(필수) 개인정보 수집 및 이용 동의합니다", true, false),
        )
    }
    var allAgreed by remember { mutableStateOf(false) }

    // 현재 화면 렌더링
    when (step) {
        SignUpStep.SPLASH -> SplashScreen(
            onFinish = { step = SignUpStep.LOGIN },
        )
        SignUpStep.LOGIN -> LoginScreen(
            logo = painterResource(R.drawable.ic_logo),
            onLoginClick = { _, _ -> step = SignUpStep.MAIN },
            onSignUpClick = { showTerms = true },
            onNaverLoginClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onForgotPasswordClick = {},
        )
        SignUpStep.EMAIL -> SignUpEmailScreen(
            onNextClick = { step = SignUpStep.PASSWORD },
            onBackClick = { step = SignUpStep.LOGIN },
        )
        SignUpStep.PASSWORD -> SignUpPasswordScreen(
            onNextClick = { step = SignUpStep.NAME_BIRTH_PHONE },
            onBackClick = { step = SignUpStep.EMAIL },
        )
        SignUpStep.NAME_BIRTH_PHONE -> SignUpNameBirthPhoneScreen(
            onNextClick = { _, _, _ -> step = SignUpStep.VERIFICATION },
            onBackClick = { step = SignUpStep.PASSWORD },
        )
        SignUpStep.VERIFICATION -> SignUpVerificationCodeScreen(
            onNextClick = { step = SignUpStep.COMPLETE },
            onBackClick = { step = SignUpStep.NAME_BIRTH_PHONE },
            onResendClick = {},
        )
        SignUpStep.COMPLETE -> SignUpCompleteScreen(
            onStartClick = { step = SignUpStep.ONBOARDING },
            onBackClick = { step = SignUpStep.LOGIN },
        )
        SignUpStep.ONBOARDING -> OnboardingScreen(
            onSkipClick = { step = SignUpStep.LOGIN },
            onStartClick = { step = SignUpStep.SELFTEST },
        )
        SignUpStep.SELFTEST -> SelfTestScreen(
            onBackClick = { step = SignUpStep.ONBOARDING },
            onComplete = { answers ->
                selfTestAnswers = answers
                step = SignUpStep.SELFTEST_LOADING
            },
        )
        SignUpStep.SELFTEST_LOADING -> SelfTestLoadingScreen(
            onFinish = { step = SignUpStep.SELFTEST_RESULT },
        )
        SignUpStep.SELFTEST_RESULT -> SelfTestResultScreen(
            resultType = computeSelfTestResultType(selfTestAnswers),
            onStartClick = { step = SignUpStep.MAIN },
            onBackClick = { step = SignUpStep.SELFTEST },
            rawScore = selfTestAnswers.values.sumOf { 4 - it },
        )
        SignUpStep.ADD_APP -> AddAppFlowHost(
            onComplete = { step = SignUpStep.MAIN },
            onBackFromFirst = { step = SignUpStep.MAIN },
        )
        SignUpStep.MAIN -> MainFlowHost(
            onAddAppClick = { step = SignUpStep.ADD_APP },
            onLogout = { step = SignUpStep.LOGIN },
        )
    }

    // 이용약관 바텀시트
    if (showTerms) {
        TermsBottomSheet(
            onDismissRequest = { showTerms = false },
            onNextClick = {
                showTerms = false
                step = SignUpStep.EMAIL
            },
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
