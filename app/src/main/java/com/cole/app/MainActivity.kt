package com.cole.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.launch

private fun getPasswordResetErrorMessage(e: Throwable, fallback: String): String {
    return when (e) {
        is FirebaseFunctionsException -> when (e.code) {
            FirebaseFunctionsException.Code.NOT_FOUND ->
                "서버 연결에 실패했어요. Cloud Functions가 배포되었는지 확인해주세요."
            else -> e.message ?: fallback
        }
        else -> e.message ?: fallback
    }
}

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
    ONBOARDING,
    SELFTEST,
    SELFTEST_LOADING,
    SELFTEST_RESULT,
    ADD_APP,
    MAIN,
    // 비밀번호 재설정 RS-01 ~ RS-03
    PASSWORD_RESET_EMAIL,
    PASSWORD_RESET_CODE,
    PASSWORD_RESET_NEW,
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
    var signUpEmail by remember { mutableStateOf("") }
    var signUpPassword by remember { mutableStateOf("") }
    var signUpName by remember { mutableStateOf("") }
    var signUpBirth by remember { mutableStateOf("") }
    var signUpPhone by remember { mutableStateOf("") }
    var signUpLoading by remember { mutableStateOf(false) }
    var signUpResendLoading by remember { mutableStateOf(false) }
    var signUpError by remember { mutableStateOf<String?>(null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var passwordResetPhone by remember { mutableStateOf("") }
    var passwordResetCode by remember { mutableStateOf("") }
    var passwordResetLoading by remember { mutableStateOf(false) }
    var passwordResetResendLoading by remember { mutableStateOf(false) }
    var passwordResetError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

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
            logo = painterResource(R.drawable.ic_login_logo),
            onLoginClick = { email, password ->
                loginError = null
                loginLoading = true
                scope.launch {
                    authRepository.signInWithEmail(email, password)
                        .onSuccess {
                            loginLoading = false
                            step = SignUpStep.MAIN
                        }
                        .onFailure { e ->
                            loginLoading = false
                            loginError = when (e) {
                                is FirebaseAuthInvalidUserException -> "가입된 계정이 아닙니다"
                                is FirebaseAuthInvalidCredentialsException -> "가입된 계정이 아닙니다"
                                is FirebaseAuthException -> when (e.errorCode) {
                                    "ERROR_USER_NOT_FOUND", "USER_NOT_FOUND" -> "가입된 계정이 아닙니다"
                                    "ERROR_INVALID_CREDENTIAL", "INVALID_CREDENTIAL", "INVALID_CREDENTIALS" -> "가입된 계정이 아닙니다"
                                    else -> e.message ?: "로그인에 실패했어요"
                                }
                                else -> e.message ?: "로그인에 실패했어요"
                            }
                        }
                }
            },
            onSignUpClick = { showTerms = true },
            onNaverLoginClick = {},
            onKakaoLoginClick = {},
            onGoogleLoginClick = {},
            onForgotPasswordClick = { step = SignUpStep.PASSWORD_RESET_EMAIL },
            errorMessage = loginError,
            onClearError = { loginError = null },
            isLoading = loginLoading,
        )
        SignUpStep.EMAIL -> SignUpEmailScreen(
            onNextClick = { email ->
                signUpEmail = email
                signUpError = null
                step = SignUpStep.PASSWORD
            },
            onBackClick = { step = SignUpStep.LOGIN },
        )
        SignUpStep.PASSWORD -> SignUpPasswordScreen(
            onNextClick = { password ->
                signUpPassword = password
                signUpError = null
                step = SignUpStep.NAME_BIRTH_PHONE
            },
            onBackClick = { step = SignUpStep.EMAIL },
        )
        SignUpStep.NAME_BIRTH_PHONE -> SignUpNameBirthPhoneScreen(
            onNextClick = { name, birth, phone ->
                signUpName = name
                signUpBirth = birth
                signUpPhone = phone
                signUpError = null
                signUpLoading = true
                scope.launch {
                    authRepository.sendSignUpVerificationSms(phone)
                        .onSuccess {
                            signUpLoading = false
                            step = SignUpStep.VERIFICATION
                        }
                        .onFailure { e ->
                            signUpLoading = false
                            signUpError = getPasswordResetErrorMessage(e, "인증번호 발송에 실패했어요")
                        }
                }
            },
            onBackClick = { step = SignUpStep.PASSWORD },
        )
        SignUpStep.VERIFICATION -> SignUpVerificationCodeScreen(
            onNextClick = { code ->
                signUpError = null
                signUpLoading = true
                scope.launch {
                    authRepository.verifyAndCompleteSignUp(
                        signUpPhone,
                        code,
                        signUpEmail,
                        signUpPassword,
                        signUpName,
                        signUpBirth,
                    )
                        .onSuccess {
                            signUpLoading = false
                            step = SignUpStep.COMPLETE
                        }
                        .onFailure { e ->
                            signUpLoading = false
                            signUpError = when (e) {
                                is FirebaseAuthUserCollisionException -> "이미 사용 중인 이메일이에요"
                                is FirebaseAuthWeakPasswordException -> "비밀번호가 너무 약해요. 8자 이상 입력해주세요"
                                else -> getPasswordResetErrorMessage(e, "회원가입에 실패했어요")
                            }
                        }
                }
            },
            onBackClick = { step = SignUpStep.NAME_BIRTH_PHONE },
            onResendClick = {
                signUpError = null
                signUpResendLoading = true
                scope.launch {
                    authRepository.sendSignUpVerificationSms(signUpPhone)
                        .onSuccess { signUpResendLoading = false }
                        .onFailure { e ->
                            signUpResendLoading = false
                            signUpError = getPasswordResetErrorMessage(e, "재발송에 실패했어요")
                        }
                }
            },
            isLoading = signUpLoading,
            isResendLoading = signUpResendLoading,
            errorMessage = signUpError,
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
        SignUpStep.SELFTEST_RESULT -> SelfTestResultScreenST10(
            resultType = computeSelfTestResultType(selfTestAnswers),
            onStartClick = { step = SignUpStep.MAIN },
            onBackClick = { step = SignUpStep.SELFTEST },
            rawScore = selfTestAnswers.values.sumOf { (4 - it).coerceIn(0, 4) }.coerceIn(8, 32),
        )
        SignUpStep.ADD_APP -> AddAppFlowHost(
            onComplete = { step = SignUpStep.MAIN },
            onBackFromFirst = { step = SignUpStep.MAIN },
        )
        SignUpStep.MAIN -> MainFlowHost(
            onAddAppClick = { step = SignUpStep.ADD_APP },
            onLogout = { step = SignUpStep.LOGIN },
        )
        SignUpStep.PASSWORD_RESET_EMAIL -> PasswordResetPhoneScreen(
            onNextClick = { phone ->
                passwordResetPhone = phone
                passwordResetError = null
                passwordResetLoading = true
                scope.launch {
                    authRepository.sendPasswordResetSms(phone)
                        .onSuccess {
                            passwordResetLoading = false
                            step = SignUpStep.PASSWORD_RESET_CODE
                        }
                        .onFailure { e ->
                            passwordResetLoading = false
                            passwordResetError = getPasswordResetErrorMessage(e, "인증번호 발송에 실패했어요")
                        }
                }
            },
            onBackClick = { step = SignUpStep.LOGIN },
            isLoading = passwordResetLoading,
            errorMessage = passwordResetError,
            onClearError = { passwordResetError = null },
        )
        SignUpStep.PASSWORD_RESET_CODE -> PasswordResetCodeScreen(
            onNextClick = { code ->
                passwordResetCode = code
                step = SignUpStep.PASSWORD_RESET_NEW
            },
            onBackClick = {
                passwordResetError = null
                step = SignUpStep.PASSWORD_RESET_EMAIL
            },
            onResendClick = {
                passwordResetError = null
                passwordResetResendLoading = true
                scope.launch {
                    authRepository.sendPasswordResetSms(passwordResetPhone)
                        .onSuccess {
                            passwordResetResendLoading = false
                        }
                        .onFailure { e ->
                            passwordResetResendLoading = false
                            passwordResetError = getPasswordResetErrorMessage(e, "재발송에 실패했어요")
                        }
                }
            },
            isResendLoading = passwordResetResendLoading,
            errorMessage = passwordResetError,
        )
        SignUpStep.PASSWORD_RESET_NEW -> PasswordResetNewPasswordScreen(
            onNextClick = { newPassword ->
                passwordResetError = null
                passwordResetLoading = true
                scope.launch {
                    authRepository.verifyAndResetPassword(
                        passwordResetPhone,
                        passwordResetCode,
                        newPassword,
                    )
                        .onSuccess {
                            passwordResetLoading = false
                            step = SignUpStep.LOGIN
                        }
                        .onFailure { e ->
                            passwordResetLoading = false
                            passwordResetError = getPasswordResetErrorMessage(e, "비밀번호 변경에 실패했어요")
                        }
                }
            },
            onBackClick = { step = SignUpStep.PASSWORD_RESET_CODE },
            isLoading = passwordResetLoading,
            errorMessage = passwordResetError,
            onClearError = { passwordResetError = null },
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
