package com.cole.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import com.google.firebase.functions.FirebaseFunctionsException
import com.navercorp.nid.NidOAuth
import com.navercorp.nid.oauth.util.NidOAuthCallback
import kotlinx.coroutines.launch

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

    /** 네이버 로그인 결과를 외부(코루틴)로 전달하기 위한 콜백 */
    var naverLoginCallback: NidOAuthCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 네이버 로그인 SDK 초기화 (KeyStoreException 등 설치 직후 간헐적 크래시 방지)
        try {
            NidOAuth.initialize(this, "BQa59cheqz4qQQ2H9Xen", "Ujdrf2_Czv", "cole.")
        } catch (e: Throwable) {
            Log.e("Cole", "NidOAuth init 실패 (네이버 로그인 비활성화됨)", e)
        }

        // Android 13+ 알림 권한 런타임 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001,
                )
            }
        }
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
    val context = LocalContext.current
    var step by remember { mutableStateOf(SignUpStep.SPLASH) }
    var selfTestAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // 현재 화면 렌더링
    when (step) {
        // SPLASH + LOGIN이 통합된 SplashLoginScreen (애니메이션 포함)
        SignUpStep.SPLASH -> SplashLoginScreen(
            onNaverLoginClick = {
                val activity = context as? MainActivity ?: return@SplashLoginScreen
                loginError = null
                loginLoading = true
                scope.launch {
                    authRepository.signInWithNaver(activity)
                        .onSuccess {
                            loginLoading = false
                            step = SignUpStep.ONBOARDING
                        }
                        .onFailure { e ->
                            loginLoading = false
                            loginError = when (e) {
                                is FirebaseFunctionsException -> "네이버: ${e.code} - ${e.message}"
                                else -> e.message ?: "네이버 로그인에 실패했어요"
                            }
                        }
                }
            },
            onKakaoLoginClick = {
                loginError = null
                loginLoading = true
                scope.launch {
                    authRepository.signInWithKakao(context)
                        .onSuccess {
                            loginLoading = false
                            step = SignUpStep.ONBOARDING
                        }
                        .onFailure { e ->
                            loginLoading = false
                            loginError = when (e) {
                                is FirebaseFunctionsException -> "카카오: ${e.code} - ${e.message}"
                                else -> e.message ?: "카카오 로그인에 실패했어요"
                            }
                        }
                }
            },
            onGoogleLoginClick = {
                loginError = null
                loginLoading = true
                scope.launch {
                    authRepository.signInWithGoogle(context)
                        .onSuccess {
                            loginLoading = false
                            step = SignUpStep.ONBOARDING
                        }
                        .onFailure { e ->
                            loginLoading = false
                            loginError = e.message ?: "구글 로그인에 실패했어요"
                        }
                }
            },
            errorMessage = loginError,
            onClearError = { loginError = null },
            isLoading = loginLoading,
        )
        SignUpStep.LOGIN -> {
            // LOGIN 스텝은 SPLASH로 리다이렉트 (통합 화면 사용)
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { step = SignUpStep.SPLASH }
            }
        }
        // 비활성화: 회원가입 플로우 (EMAIL, PASSWORD, NAME_BIRTH_PHONE, VERIFICATION, COMPLETE)
        SignUpStep.EMAIL,
        SignUpStep.PASSWORD,
        SignUpStep.NAME_BIRTH_PHONE,
        SignUpStep.VERIFICATION,
        SignUpStep.COMPLETE -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { step = SignUpStep.LOGIN }
            }
        }
        SignUpStep.ONBOARDING -> OnboardingScreen(
            onSkipClick = { step = SignUpStep.SELFTEST },
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
        // 비활성화: 비밀번호 찾기 플로우
        SignUpStep.PASSWORD_RESET_EMAIL,
        SignUpStep.PASSWORD_RESET_CODE,
        SignUpStep.PASSWORD_RESET_NEW -> {
            Box(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { step = SignUpStep.LOGIN }
            }
        }
    }
}
