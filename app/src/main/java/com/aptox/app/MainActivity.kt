package com.aptox.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import com.aptox.app.ui.components.AptoxToast
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowInsetsControllerCompat
import android.util.Log
import android.content.Context
import kotlinx.coroutines.launch

/**
 * 스플래시/권한 직후: 필수 권한 → (온보딩 1회) 앱 소개 → 이름+자가테스트(Ver2) → 분석 로딩(ST-09) → 사용패턴 결과 → 메인.
 * 권한 화면을 거친 경우에만 앱 소개가 끼고, 이미 권한이 있으면 스플래시 직후 Ver2로 진입.
 * [FirstRunFlowRepository] 완료 플래그는 사용패턴 분석 완료 시에만 true.
 */
private suspend fun resolveStepAfterAuth(context: Context, firstRunRepo: FirstRunFlowRepository): SignUpStep {
    if (!context.areRequiredAppPermissionsGranted()) return SignUpStep.PERMISSION
    if (!firstRunRepo.isOnboardingFlowCompleted()) {
        return SignUpStep.SELFTEST_VER2
    }
    return SignUpStep.MAIN
}

/** 권한 안내 화면 종료 후(허용 완료 또는 「다음에 하기」) 온보딩이면 앱 소개 화면으로 */
private suspend fun resolveStepAfterPermissionScreen(context: Context, firstRunRepo: FirstRunFlowRepository): SignUpStep {
    if (!firstRunRepo.isOnboardingFlowCompleted()) {
        return SignUpStep.APP_INTRO_ONBOARDING
    }
    return SignUpStep.MAIN
}

@Composable
fun AptoxRootContent(
    pendingPauseFlowFromOverlay: PendingPauseFlowFromOverlay? = null,
    pendingOpenBottomSheetPackage: String? = null,
    onOpenBottomSheetConsumed: () -> Unit = {},
    pendingNavIndex: Int? = null,
    onNavIndexConsumed: () -> Unit = {},
) {
    if (!BuildConfig.SHOW_DEBUG_MENU || !BuildConfig.DEBUG) {
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
    /** App intro after device permission screen; next → SelfTest Ver2 */
    APP_INTRO_ONBOARDING,
    LOGIN,
    EMAIL,
    PASSWORD,
    NAME_BIRTH_PHONE,
    VERIFICATION,
    COMPLETE,
    SELFTEST,
    SELFTEST_VER2,
    SELFTEST_LOADING,
    USAGE_PATTERN_ANALYSIS,
    ONBOARDING_START,
    SELFTEST_RESULT,
    ADD_APP,
    TIME_SPECIFIED,
    MAIN,
    // 비밀번호 재설정 RS-01 ~ RS-03
    PASSWORD_RESET_EMAIL,
    PASSWORD_RESET_CODE,
    PASSWORD_RESET_NEW,
}

class MainActivity : ComponentActivity() {

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
        AptoxApplication.startAppMonitorIfNeeded(applicationContext, clearForegroundPkg = pendingOpenBottomSheetPackage != null)
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    if (BuildConfig.SHOW_DEBUG_MENU && BuildConfig.DEBUG) {
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
        if (i?.action != BlockDialogActivity.ACTION_PAUSE_FLOW_FROM_OVERLAY) return null
        val pkg = i.getStringExtra(BlockDialogActivity.EXTRA_PACKAGE_NAME)
        val name = i.getStringExtra(BlockDialogActivity.EXTRA_APP_NAME)
        val blockUntilMs = i.getLongExtra(BlockDialogActivity.EXTRA_BLOCK_UNTIL_MS, 0L)
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
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginCancelToast by remember { mutableStateOf<String?>(null) }
    var loginCancelToastKey by remember { mutableStateOf(0) }
    var autoOpenPackage by remember { mutableStateOf<String?>(null) }
    var prefilledApp by remember { mutableStateOf<com.aptox.app.model.SelectedAppInfo?>(null) }
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val firstRunRepo = remember { FirstRunFlowRepository(context) }
    val firebaseAnalytics = remember {
        FirebaseAnalytics.getInstance(context.applicationContext)
    }

    BackHandler(
        enabled = step == SignUpStep.ADD_APP || step == SignUpStep.TIME_SPECIFIED,
    ) {
        prefilledApp = null
        step = SignUpStep.MAIN
    }

    // 현재 화면 렌더링
    when (step) {
        SignUpStep.SPLASH -> SplashScreen(
            onFinish = {
                scope.launch {
                    step = resolveStepAfterAuth(context, firstRunRepo)
                }
            },
        )
        SignUpStep.PERMISSION -> PermissionScreen(
            onPrimaryClick = {
                scope.launch {
                    if (!context.areRequiredAppPermissionsGranted()) return@launch
                    step = resolveStepAfterPermissionScreen(context, firstRunRepo)
                }
            },
            onGhostClick = {
                scope.launch {
                    step = resolveStepAfterPermissionScreen(context, firstRunRepo)
                }
            },
        )
        SignUpStep.APP_INTRO_ONBOARDING -> AppIntroOnboardingScreen(
            onNextClick = { step = SignUpStep.SELFTEST_VER2 },
            onBackClick = { step = SignUpStep.PERMISSION },
        )
        SignUpStep.LOGIN -> Box(modifier = Modifier.fillMaxSize()) {
            SplashLoginScreen(
                initialButtonsVisible = true,
                onGoogleLoginClick = {
                    loginError = null
                    loginLoading = true
                    scope.launch {
                        authRepository.signInWithGoogle(context)
                            .onSuccess {
                                loginLoading = false
                                scope.launch {
                                    step = resolveStepAfterAuth(context, firstRunRepo)
                                }
                            }
                            .onFailure { e ->
                                loginLoading = false
                                if (LoginAnalytics.isGoogleLoginCancelled(e)) {
                                    LoginAnalytics.logLoginCancelled(
                                        firebaseAnalytics,
                                        "google",
                                        "signup_login",
                                    )
                                    loginCancelToastKey++
                                    loginCancelToast = "로그인을 취소했습니다"
                                } else {
                                    LoginAnalytics.logLoginFailed(
                                        firebaseAnalytics,
                                        "google",
                                        e.message ?: "구글 로그인에 실패했어요",
                                    )
                                    loginError = e.message ?: "구글 로그인에 실패했어요"
                                }
                            }
                    }
                },
                errorMessage = loginError,
                onClearError = { loginError = null },
                isLoading = loginLoading,
            )
            AptoxToast(
                message = loginCancelToast ?: "",
                visible = loginCancelToast != null,
                onDismiss = { loginCancelToast = null },
                replayKey = loginCancelToastKey,
            )
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
        SignUpStep.SELFTEST -> SelfTestScreen(
            onBackClick = { step = SignUpStep.MAIN },
            onComplete = { answers ->
                selfTestAnswers = answers
                step = SignUpStep.SELFTEST_LOADING
            },
        )
        SignUpStep.SELFTEST_VER2 -> SelfTestScreenVer2(
            onBack = { step = SignUpStep.APP_INTRO_ONBOARDING },
            onComplete = { name, answers ->
                selfTestUserName = name
                UserPreferencesRepository(context).userName = name
                selfTestAnswers = answers
                step = SignUpStep.SELFTEST_LOADING
            },
        )
        SignUpStep.SELFTEST_LOADING -> SelfTestLoadingScreen(
            onFinish = { step = SignUpStep.USAGE_PATTERN_ANALYSIS },
            userName = selfTestUserName.ifBlank {
                UserPreferencesRepository(context).userName ?: "아영"
            },
        )
        SignUpStep.USAGE_PATTERN_ANALYSIS -> DiagnosisResultScreen(
            userName = selfTestUserName.ifBlank {
                UserPreferencesRepository(context).userName ?: "아영"
            },
            diagnosisScore = computeDiagnosisScore(selfTestAnswers),
            onFinish = { step = SignUpStep.ONBOARDING_START },
        )
        SignUpStep.ONBOARDING_START -> OnboardingStartScreen(
            onFinish = {
                scope.launch {
                    firstRunRepo.setOnboardingFlowCompleted(true)
                    step = SignUpStep.MAIN
                }
            },
        )
        SignUpStep.SELFTEST_RESULT -> {
            val answerCount = if (selfTestAnswers.size == 8) 8 else 7
            val sum = selfTestAnswers.values.sumOf { (4 - it).coerceIn(0, 4) }
            val raw = sum.coerceIn(answerCount, answerCount * 4)
            SelfTestResultScreenST10(
                resultType = computeSelfTestResultType(selfTestAnswers),
                onStartClick = {
                    scope.launch {
                        firstRunRepo.setOnboardingFlowCompleted(true)
                        step = SignUpStep.MAIN
                    }
                },
                onBackClick = { step = SignUpStep.SELFTEST },
                rawScore = raw,
                answerCount = answerCount,
            )
        }
        SignUpStep.ADD_APP -> AddAppFlowHost(
            onComplete = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            onBackFromFirst = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            initialPrefilledApp = prefilledApp,
        )
        SignUpStep.TIME_SPECIFIED -> TimeSpecifiedFlowHost(
            onComplete = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            onBackFromFirst = {
                prefilledApp = null
                step = SignUpStep.MAIN
            },
            initialPrefilledApp = prefilledApp,
        )
        SignUpStep.MAIN -> MainFlowHost(
            onAddAppClick = { app ->
                prefilledApp = app
                step = SignUpStep.ADD_APP
            },
            onTimeSpecifiedClick = { app ->
                prefilledApp = app
                step = SignUpStep.TIME_SPECIFIED
            },
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
