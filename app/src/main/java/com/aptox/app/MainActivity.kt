package com.aptox.app

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctionsException
import com.navercorp.nid.oauth.util.NidOAuthCallback
import kotlinx.coroutines.launch

@Composable
fun AptoxRootContent(
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    if (!BuildConfig.DEBUG) {
        SignUpFlowHost(
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
        return
    }
    var showDebugMenu by remember { mutableStateOf(true) }
    if (showDebugMenu) {
        DebugFlowHost(
            onStartNormalFlow = { showDebugMenu = false },
            modifier = Modifier.fillMaxSize(),
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
    } else {
        SignUpFlowHost(
            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
            onOpenBottomSheetConsumed = onOpenBottomSheetConsumed,
            pendingNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
        )
    }
}

/** 차단 오버레이에서 일시정지 클릭 시 1단계(제안)부터 시작하는 플로우용 데이터 */
data class PendingPauseFlowFromOverlay(
    val packageName: String,
    val appName: String,
    val blockUntilMs: Long,
)

enum class SignUpStep {
    SPLASH,
    PERMISSION,
    LOGIN,
    EMAIL,
    PASSWORD,
    NAME_BIRTH_PHONE,
    VERIFICATION,
    COMPLETE,
    ONBOARDING,
    SELFTEST,
    SELFTEST_VER2,
    SELFTEST_LOADING,
    USAGE_PATTERN_ANALYSIS,
    SELFTEST_RESULT,
    APP_EXPLANATION_ONBOARDING,
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

    /** 앱 제한 오버레이에서 일시정지 클릭 후 1단계(제안)부터 시작할 플로우 데이터 (packageName, appName, blockUntilMs) */
    private val pendingPauseFlowState = mutableStateOf<PendingPauseFlowFromOverlay?>(null)
    var pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay?
        get() = pendingPauseFlowState.value
        set(value) { pendingPauseFlowState.value = value }

    /** 시스템 알림/오버레이에서 카운트 중지·시작 버튼 탭 시 열 바텀시트의 packageName */
    private val pendingOpenBottomSheetState = mutableStateOf<String?>(null)
    var pendingOpenBottomSheetPackage: String?
        get() = pendingOpenBottomSheetState.value
        set(value) { pendingOpenBottomSheetState.value = value }

    /** 주간 리포트/목표 달성 알림 탭 시 열 탭 인덱스 (1=챌린지, 2=통계) */
    private val pendingNavIndexState = mutableStateOf<Int?>(null)
    var pendingNavIndex: Int?
        get() = pendingNavIndexState.value
        set(value) { pendingNavIndexState.value = value }

    fun clearPendingPauseFlowFromOverlay() {
        pendingPauseFlowState.value = null
    }

    fun clearPendingOpenBottomSheet() {
        pendingOpenBottomSheetState.value = null
    }

    fun clearPendingNavIndex() {
        pendingNavIndexState.value = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPauseFlowState.value = extractPauseFlowFromIntent(intent)
        pendingOpenBottomSheetState.value = intent.getStringExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET)
        if (intent.hasExtra(EXTRA_NAV_INDEX)) {
            pendingNavIndexState.value = intent.getIntExtra(EXTRA_NAV_INDEX, 0).takeIf { it in 1..3 }
        }
    }

    override fun onResume() {
        super.onResume()
        AptoxApplication.startAppMonitorIfNeeded(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        Log.d("AptoxMain", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("AptoxMain", "onStop")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NidOAuth 초기화는 네이버 로그인 시도 시점에만 수행 (KeyStoreException 크래시 방지)

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
        pendingPauseFlowState.value = extractPauseFlowFromIntent(intent)
        pendingOpenBottomSheetState.value = intent.getStringExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET)
        if (intent.hasExtra(EXTRA_NAV_INDEX)) {
            pendingNavIndexState.value = intent.getIntExtra(EXTRA_NAV_INDEX, 0).takeIf { it in 1..3 }
        }
        setContent {
            AptoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.SurfaceBackgroundBackground,
                ) {
                    if (BuildConfig.DEBUG) {
                        AptoxRootContent(
                            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
                            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
                            onOpenBottomSheetConsumed = ::clearPendingOpenBottomSheet,
                            pendingNavIndex = pendingNavIndex,
                            onNavIndexConsumed = ::clearPendingNavIndex,
                        )
                    } else {
                        SignUpFlowHost(
                            pendingPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
                            pendingOpenBottomSheetPackage = pendingOpenBottomSheetPackage,
                            onOpenBottomSheetConsumed = ::clearPendingOpenBottomSheet,
                            pendingNavIndex = pendingNavIndex,
                            onNavIndexConsumed = ::clearPendingNavIndex,
                        )
                    }
                }
            }
        }
    }

    companion object {
        /** 주간 리포트/목표 달성 알림 탭 시 열 탭 (1=챌린지, 2=통계) */
        const val EXTRA_NAV_INDEX = "com.aptox.app.EXTRA_NAV_INDEX"
    }

    private fun extractPauseFlowFromIntent(i: Intent?): PendingPauseFlowFromOverlay? {
        if (i?.action != BlockOverlayService.ACTION_PAUSE_FLOW_FROM_OVERLAY) return null
        val pkg = i.getStringExtra(BlockOverlayService.EXTRA_PACKAGE_NAME)
        val name = i.getStringExtra(BlockOverlayService.EXTRA_APP_NAME)
        val blockUntilMs = i.getLongExtra(BlockOverlayService.EXTRA_BLOCK_UNTIL_MS, 0L)
        return if (pkg != null && name != null) PendingPauseFlowFromOverlay(pkg, name, blockUntilMs) else null
    }
}

@Composable
fun SignUpFlowHost(
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val goToMain = pendingPauseFlowFromOverlay != null || pendingOpenBottomSheetPackage != null || pendingNavIndex != null
    var step by remember {
        mutableStateOf(if (goToMain) SignUpStep.MAIN else SignUpStep.SPLASH)
    }
    LaunchedEffect(pendingPauseFlowFromOverlay, pendingOpenBottomSheetPackage, pendingNavIndex) {
        if (goToMain) step = SignUpStep.MAIN
    }
    var selfTestAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var selfTestUserName by remember { mutableStateOf("") }
    var selfTestCameFromPermission by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var autoOpenPackage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    // 현재 화면 렌더링
    when (step) {
        SignUpStep.SPLASH -> SplashScreen(
            onFinish = {
                step = if (FirebaseAuth.getInstance().currentUser != null) SignUpStep.MAIN else SignUpStep.PERMISSION
            },
        )
        SignUpStep.PERMISSION -> PermissionScreen(
            onPrimaryClick = {
                selfTestCameFromPermission = true
                step = SignUpStep.SELFTEST_VER2
            },
            onGhostClick = {
                selfTestCameFromPermission = true
                step = SignUpStep.SELFTEST_VER2
            },
        )
        SignUpStep.LOGIN -> SplashLoginScreen(
            initialButtonsVisible = true,
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
            onSkipClick = {
                selfTestCameFromPermission = false
                step = SignUpStep.SELFTEST_VER2
            },
            onStartClick = {
                selfTestCameFromPermission = false
                step = SignUpStep.SELFTEST_VER2
            },
        )
        SignUpStep.SELFTEST -> SelfTestScreen(
            onBackClick = { step = SignUpStep.ONBOARDING },
            onComplete = { answers ->
                selfTestAnswers = answers
                step = SignUpStep.SELFTEST_LOADING
            },
        )
        SignUpStep.SELFTEST_VER2 -> SelfTestScreenVer2(
            onBack = { step = if (selfTestCameFromPermission) SignUpStep.PERMISSION else SignUpStep.ONBOARDING },
            onComplete = { name, answers ->
                selfTestUserName = name
                UserPreferencesRepository(context).userName = name
                selfTestAnswers = answers
                step = SignUpStep.SELFTEST_LOADING
            },
        )
        SignUpStep.SELFTEST_LOADING -> SelfTestLoadingScreen(
            onFinish = { step = SignUpStep.USAGE_PATTERN_ANALYSIS },
        )
        SignUpStep.USAGE_PATTERN_ANALYSIS -> UsagePatternAnalysisScreen(
            userName = selfTestUserName.ifBlank { "아영" },
            onFinish = { step = SignUpStep.MAIN },
        )
        SignUpStep.APP_EXPLANATION_ONBOARDING -> AppExplanationOnboardingScreen(
            onFinish = { step = SignUpStep.MAIN },
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
            onCompleteWithFirstPackage = { pkg ->
                autoOpenPackage = pkg
                step = SignUpStep.MAIN
            },
        )
        SignUpStep.MAIN -> MainFlowHost(
            onAddAppClick = { step = SignUpStep.ADD_APP },
            onLogout = { step = SignUpStep.LOGIN },
            initialPauseFlowFromOverlay = pendingPauseFlowFromOverlay,
            onPauseFlowConsumed = { activity?.clearPendingPauseFlowFromOverlay() },
            initialAutoOpenPackage = autoOpenPackage ?: pendingOpenBottomSheetPackage,
            onAutoOpenConsumed = {
                autoOpenPackage = null
                onOpenBottomSheetConsumed()
            },
            initialNavIndex = pendingNavIndex,
            onNavIndexConsumed = onNavIndexConsumed,
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
