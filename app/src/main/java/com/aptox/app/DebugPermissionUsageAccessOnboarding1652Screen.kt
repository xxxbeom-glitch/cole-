package com.aptox.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val PermissionOnboardingPageCount = 4

/** Pager 영역 높이(내부 스크롤) */
private val PermissionOnboardingPagerAreaHeight = 280.dp

/** [AptoxPrimaryButton] 높이와 동일 — 마지막 페이지가 아닐 때 하단 슬롯 점유용 */
private val PermissionOnboardingBottomCtaHeight = 60.dp

/** 상태줄 아래 ~ 닫기 버튼 상단까지 (한 곳에서만 조정) */
private val PermissionOnboardingCloseTopPadding = 40.dp

/** 닫기 버튼 행 높이 — 본문이 닫기와 겹치지 않도록 예약 */
private val PermissionOnboardingCloseBarHeight = 32.dp

/** 본문 컨텐츠 그룹만 추가로 아래로 내림 (닫기 X 위치는 유지) */
private val PermissionOnboardingBodyExtraTopInset = 40.dp

/** 하단 CTA 슬롯(패딩 포함) — 본문 weight 영역 계산과 항상 일치 */
private val PermissionOnboardingBottomSlotHeight =
    8.dp + PermissionOnboardingBottomCtaHeight + 16.dp

/**
 * Figma 앱 접근권한 온보딩. 실제 진입: [MainActivity] `SignUpStep.PERMISSION_ONBOARDING_1652` (스플래시 직후 1회).
 * 디버그 메뉴에서도 동일 컴포저블 미리보기 가능.
 *
 * - [1652-4712](https://www.figma.com/design/jTxTaPrc0c2cyGeSRknNEN/aptox?node-id=1652-4712): 1페이지(앱 사용정보 접근)
 * - [1656-5114](https://www.figma.com/design/jTxTaPrc0c2cyGeSRknNEN/aptox?node-id=1656-5114): 2페이지(사용 통계 접근)
 *
 * 페이지 순서: 앱 사용정보(필수) → 사용 통계(필수) → 배터리(필수) → 알림(선택).
 * 상단 제목·비주얼·진행 막대 고정, 막대 아래만 스와이프.
 *
 * @param onCloseSkipPermissions 우측 X — 권한을 나중에 설정하고 메인(홈)으로 보낼 때
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DebugPermissionUsageAccessOnboarding1652Screen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** 우측 상단 X — 권한은 나중에 하고 플로우 종료 후 메인(홈) 진입 */
    onCloseSkipPermissions: () -> Unit,
    /** 마지막 페이지 CTA. null이면 [onBack]과 동일하게 동작 */
    onStartAptox: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionRefresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val accessibilityGranted = remember(permissionRefresh) { context.isAptoxAccessibilityServiceEnabled() }
    val usageGranted = remember(permissionRefresh) { StatisticsData.hasUsageAccess(context) }
    val batteryGranted = remember(permissionRefresh) { context.isIgnoringBatteryOptimizations() }
    val notificationGranted = remember(permissionRefresh) { context.hasPostNotificationsGrantedForOnboarding() }

    LaunchedEffect(permissionRefresh) {
        // 권한 설정 화면에서 돌아올 때마다 현재 페이지의 권한이 허용되었는지 확인하여 자동 이동
        val currentPage = pagerState.currentPage
        val isCurrentPageGranted = when (currentPage) {
            0 -> accessibilityGranted
            1 -> usageGranted
            2 -> batteryGranted
            else -> notificationGranted
        }

        if (isCurrentPageGranted) {
            if (currentPage < PermissionOnboardingPageCount - 1) {
                // 마지막 페이지가 아니면 다음 페이지로 이동
                pagerState.animateScrollToPage(currentPage + 1)
            } else {
                // 마지막 페이지이고 모든 권한이 허용되었으면 온보딩 완료
                if (accessibilityGranted && usageGranted && batteryGranted && notificationGranted) {
                    (onStartAptox ?: onBack)()
                }
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { PermissionOnboardingPageCount })
    val isLastOnboardingPage =
        pagerState.currentPage == PermissionOnboardingPageCount - 1

    val stepFillFraction by remember {
        derivedStateOf {
            val p = pagerState.currentPage
            val o = pagerState.currentPageOffsetFraction
            ((p + o + 1f) / PermissionOnboardingPageCount.toFloat()).coerceIn(
                1f / PermissionOnboardingPageCount,
                1f,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Spacer(modifier = Modifier.height(PermissionOnboardingCloseTopPadding))
            Spacer(modifier = Modifier.height(PermissionOnboardingCloseBarHeight))
            Spacer(modifier = Modifier.height(PermissionOnboardingBodyExtraTopInset))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .fillMaxWidth(),
            ) {
                val bodyScroll = rememberScrollState()
                val bodyMaxHeight = maxHeight
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .heightIn(max = bodyMaxHeight)
                            .verticalScroll(bodyScroll)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "시작하기 전에",
                                style = AppTypography.Display3.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AppColors.TextPrimary,
                                ),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "더 정확한 서비스를 제공하기 위해\n다음의 기기 권한 허용이 필요해요",
                                style = AppTypography.BodyRegular.copy(color = AppColors.TextSecondary),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(46.dp))

                        PermissionOnboardingVisualCard(
                            modifier = Modifier.width(260.dp),
                        )

                        Spacer(modifier = Modifier.height(46.dp))

                        OnboardingPermissionStepProgressBar(fillFraction = stepFillFraction)

                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(PermissionOnboardingPagerAreaHeight),
                            verticalAlignment = Alignment.Top,
                        ) { page ->
                            val pageScroll = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(pageScroll),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                PermissionOnboardingPagerPage(
                                    pageIndex = page,
                                    isStepGranted = when (page) {
                                        0 -> accessibilityGranted
                                        1 -> usageGranted
                                        2 -> batteryGranted
                                        else -> notificationGranted
                                    },
                                    onSettingsClick = {
                                        when (page) {
                                            0 -> context.openAccessibilitySettingsOrFallback()
                                            1 -> context.openUsageAccessSettingsOrFallback()
                                            2 -> context.openBatteryOptimizationSettingsOrFallback()
                                            else -> context.openAppNotificationSettingsOrFallback()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // 고정 높이 슬롯: 레이아웃 계산이 페이지와 무관하게 동일
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PermissionOnboardingBottomSlotHeight)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (isLastOnboardingPage) {
                    AptoxPrimaryButton(
                        text = "aptox 시작하기",
                        onClick = { (onStartAptox ?: onBack)() },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(modifier = Modifier.height(PermissionOnboardingBottomCtaHeight))
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // 닫기: Column 흐름과 분리해 상단 위치가 본문/하단 수정에 흔들리지 않게 함
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = PermissionOnboardingCloseTopPadding,
                    end = 16.dp,
                )
                .size(PermissionOnboardingCloseBarHeight)
                .zIndex(2f)
                .clip(CircleShape)
                .background(AppColors.Grey200)
                .clickable(onClick = onCloseSkipPermissions),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_close_remove),
                contentDescription = "권한 설정 나중에 하기, 홈으로",
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PermissionOnboardingPagerPage(
    pageIndex: Int,
    isStepGranted: Boolean,
    onSettingsClick: () -> Unit,
) {
    when (pageIndex) {
        0 -> AppUsageInfoOnboardingPageContent(
            isGranted = isStepGranted,
            onSettingsClick = onSettingsClick,
        )
        1 -> UsageStatsOnboardingPageContent(
            isGranted = isStepGranted,
            onSettingsClick = onSettingsClick,
        )
        2 -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "배터리 사용량 제한 해제  (필수)",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "백그라운드에서 앱 사용을 꾸준히 추적하는 데\n필요한 권한이에요",
                    style = AppTypography.BodyRegular.copy(color = AppColors.TextCaption),
                    textAlign = TextAlign.Center,
                )
            }
            PermissionSettingsLinkRow(
                isGranted = isStepGranted,
                onClick = onSettingsClick,
            )
        }
        else -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "알림 (선택)",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "사용 시간 초과 및 목표 달성 알림을 받는 데\n필요한 권한이에요",
                    style = AppTypography.BodyRegular.copy(color = AppColors.TextCaption),
                    textAlign = TextAlign.Center,
                )
            }
            PermissionSettingsLinkRow(
                isGranted = isStepGranted,
                onClick = onSettingsClick,
            )
        }
    }
}

/** [1652-4712] 앱 사용정보 접근 — 접근성·설치된 앱 안내 */
@Composable
private fun AppUsageInfoOnboardingPageContent(
    isGranted: Boolean,
    onSettingsClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "앱 사용정보 접근 (필수)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "실행 중인 앱을 감지하고 차단하는 데\n필요한 권한이에요",
            style = AppTypography.BodyRegular.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
        )
    }
    PermissionSettingsLinkRow(
        isGranted = isGranted,
        onClick = onSettingsClick,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Primary50)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "접근성 → 설치된 앱 → aptox → 스위치 ON → 허용",
            style = AppTypography.Disclaimer.copy(color = AppColors.Primary400),
            textAlign = TextAlign.Center,
        )
    }
}

/** [1656-5114] 사용 통계 접근 — 사용정보 접근 설정 안내 */
@Composable
private fun UsageStatsOnboardingPageContent(
    isGranted: Boolean,
    onSettingsClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "사용 통계 접근  (필수)",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "앱별 사용 시간을 정확하게 측정하는 데\n필요한 권한이에요",
            style = AppTypography.BodyRegular.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
        )
    }
    PermissionSettingsLinkRow(
        isGranted = isGranted,
        onClick = onSettingsClick,
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Primary50)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "사용정보 접근 허용 → aptox → 스위치 ON",
            style = AppTypography.Disclaimer.copy(color = AppColors.Primary400),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionSettingsLinkRow(
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    val labelColor = if (isGranted) AppColors.TextBody else AppColors.Red300
    // 허용됨 / 설정 바로가기 동일 타이포(Caption2 Bold), 색만 구분
    val labelStyle = AppTypography.Caption2.copy(
        fontWeight = FontWeight.Bold,
        color = labelColor,
    )
    Row(
        modifier = Modifier
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = if (isGranted) "허용됨" else "설정 바로가기",
            style = labelStyle,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = labelColor,
        )
    }
}

private fun Context.isAptoxAccessibilityServiceEnabled(): Boolean {
    val enabled =
        Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    return enabled.contains(packageName)
}

private fun Context.hasPostNotificationsGrantedForOnboarding(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}

private fun Context.openAccessibilitySettingsOrFallback() {
    try {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    } catch (_: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) { }
    }
}

private fun Context.openUsageAccessSettingsOrFallback() {
    try {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    } catch (_: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) { }
    }
}

private fun Context.openBatteryOptimizationSettingsOrFallback() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        } else {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    } catch (_: Exception) {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) { }
        }
    }
}

private fun Context.openAppNotificationSettingsOrFallback() {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                },
            )
        } else {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                },
            )
        }
    } catch (_: Exception) {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                },
            )
        } catch (_: Exception) { }
    }
}

private const val PermissionVisualSwitchDelayMs = 700L
private const val PermissionToggleAnimDurationMs = 850
private const val PermissionRippleDurationMs = 3_200

private val PermissionOnboardingCardHeight = 72.dp
private val PermissionRippleBackdropPadV = 100.dp

@Composable
private fun PermissionOnboardingVisualCard(modifier: Modifier = Modifier) {
    var switchOn by remember { mutableStateOf(false) }
    val waveAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(PermissionVisualSwitchDelayMs)
        switchOn = true
    }

    LaunchedEffect(switchOn) {
        if (!switchOn) return@LaunchedEffect
        waveAnim.snapTo(0f)
        waveAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = PermissionRippleDurationMs,
                easing = LinearEasing,
            ),
        )
        waveAnim.snapTo(0f)
    }

    val rippleProgress = waveAnim.value

    val cardShape = RoundedCornerShape(16.dp)
    val rippleCanvasHeight = PermissionOnboardingCardHeight + PermissionRippleBackdropPadV * 2

    Box(
        modifier = modifier
            .height(PermissionOnboardingCardHeight),
    ) {
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(rippleCanvasHeight),
        ) {
            val cx = size.width - 36.dp.toPx()
            val cy = size.height * 0.5f
            val corners = listOf(
                Offset(0f, 0f),
                Offset(size.width, 0f),
                Offset(0f, size.height),
                Offset(size.width, size.height),
            )
            val reach = corners.maxOf { hypot(it.x - cx, it.y - cy) }
            val maxR = reach * 1.35f
            val p = rippleProgress
            if (p > 0.001f) {
                val radius = p * maxR
                val edgeFade = (1f - p).coerceIn(0f, 1f)
                if (radius > 4f && edgeFade > 0.02f) {
                    val aCenter = edgeFade * 0.48f
                    val aMid = edgeFade * 0.28f
                    val aEdge = edgeFade * 0.14f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to AppColors.Primary100.copy(alpha = aCenter),
                                0.38f to AppColors.Primary200.copy(alpha = aMid),
                                0.72f to AppColors.Primary300.copy(alpha = aEdge),
                                1f to Color.Transparent,
                            ),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(PermissionOnboardingCardHeight)
                .shadow(6.dp, cardShape, false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
                .clip(cardShape)
                .background(AppColors.SurfaceBackgroundCard)
                .zIndex(1f),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PermissionAptoxPurpleMark()
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "aptox",
                    style = AppTypography.ButtonLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = AppColors.TextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                AnimatedMiniToggle(checked = switchOn)
            }
        }
    }
}

@Composable
private fun PermissionAptoxPurpleMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.Primary300),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "aptox",
            style = AppTypography.Caption1.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                color = AppColors.White900,
            ),
        )
    }
}

@Composable
private fun AnimatedMiniToggle(checked: Boolean) {
    val density = LocalDensity.current
    val trackW = 46.dp
    val trackH = 28.dp
    val thumbSize = 22.dp
    val inset = 3.dp
    val slidePx = with(density) { (trackW - thumbSize - inset * 2).toPx() }

    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(
            durationMillis = PermissionToggleAnimDurationMs,
            easing = FastOutSlowInEasing,
        ),
        label = "toggle_thumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) AppColors.Primary300 else AppColors.Grey300,
        animationSpec = tween(
            durationMillis = PermissionToggleAnimDurationMs,
            easing = FastOutSlowInEasing,
        ),
        label = "toggle_track",
    )

    Box(
        modifier = Modifier
            .width(trackW)
            .height(trackH)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(inset)
                .offset { IntOffset((slidePx * progress).roundToInt(), 0) }
                .size(thumbSize)
                .shadow(2.dp, CircleShape, false, Color.Black.copy(alpha = 0.12f))
                .clip(CircleShape)
                .background(AppColors.White900),
        )
    }
}

/** Figma Progress / Progress Bar (Onbording): 60×4 트랙, 진행률은 현재 페이지에 연동 (고정 위치) */
@Composable
private fun OnboardingPermissionStepProgressBar(fillFraction: Float) {
    val trackW = 60.dp
    val barH = 4.dp
    val targetFillW = trackW * fillFraction
    val fillW: Dp by animateDpAsState(
        targetValue = targetFillW,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "onboarding_progress_fill",
    )
    Column(
        modifier = Modifier
            .width(trackW)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(trackW)
                .height(barH),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barH)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AppColors.Grey550.copy(alpha = 0.1f)),
            )
            Box(
                modifier = Modifier
                    .width(fillW.coerceAtLeast(0.dp))
                    .height(barH)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AppColors.Primary300),
            )
        }
    }
}
