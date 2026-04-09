package com.aptox.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.aptox.app.StatisticsData
import com.aptox.app.subscription.PremiumSnapshot
import com.aptox.app.subscription.PremiumStatusRepository
import com.aptox.app.subscription.SubscriptionBillingController
import com.aptox.app.subscription.SubscriptionManager
import com.aptox.app.subscription.SubscriptionRenewalMessage
import java.text.Collator
import java.util.Locale

/**
 * 계정관리 구글 행 부제: Gmail/Googlemail 은 `@` 뒤 생략(로컬파트만), 그 외 도메인은 전체 이메일.
 */
/** [642-4685] 권한 설정 — 배터리 최적화 예외 화면 열기 (온보딩·PermissionScreen과 동일 계열) */
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

internal fun formatGoogleAccountRowEmail(email: String?): String {
    if (email.isNullOrBlank()) return ""
    val trimmed = email.trim()
    val at = trimmed.lastIndexOf('@')
    if (at <= 0 || at >= trimmed.length - 1) return trimmed
    val local = trimmed.substring(0, at)
    val domain = trimmed.substring(at + 1).lowercase(Locale.getDefault())
    return when (domain) {
        "gmail.com", "googlemail.com" -> local
        else -> trimmed
    }
}

/** 구글 로그인 시 카드 메인 줄: 이메일(@gmail 등은 [formatGoogleAccountRowEmail] 규칙) 우선, 없으면 표시용 닉네임 등 */
private fun googleAccountRowPrimaryText(info: AuthRepository.CurrentUserInfo?): String {
    if (info == null) return ""
    val fromEmail = formatGoogleAccountRowEmail(info.email)
    if (fromEmail.isNotBlank()) return fromEmail
    val fromDisplay = formatGoogleAccountRowEmail(info.displayText)
    if (fromDisplay.isNotBlank()) return fromDisplay
    return info.displayText.trim().ifBlank { "구글 계정" }
}

/**
 * 설정 메뉴 세부 화면 (Figma MY-02~07)
 */

/**
 * 앱 사용제한 기록 리스트 화면 (Figma 1319-4533)
 * - Firestore appLimitLogs에 기록이 있는 모든 앱 (제한 해제된 앱 포함)
 * - 앱 아이콘 + 이름 + > 버튼
 */
@Composable
fun AppRestrictionHistoryScreen(
    userId: String?,
    onBack: () -> Unit,
    onItemClick: (packageName: String, appName: String) -> Unit,
) {
    val context = LocalContext.current
    val logRepo = remember { AppLimitLogRepository() }
    val packages by logRepo.getPackagesWithLogsFlow(context, userId).collectAsState(initial = emptyList())
    val sortedPackages = remember(packages) {
        val collator = Collator.getInstance(Locale.KOREAN)
        packages.sortedWith(compareBy(collator) { it.appName })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        when {
            sortedPackages.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "기록이 없어요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppColors.SurfaceBackgroundCard)
                        .padding(top = 26.dp, bottom = 32.dp, start = 18.dp, end = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    sortedPackages.forEach { pkg ->
                        AppRestrictionHistoryRow(
                            appName = pkg.appName,
                            packageName = pkg.packageName,
                            onClick = { onItemClick(pkg.packageName, pkg.appName) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRestrictionHistoryRow(
    appName: String,
    packageName: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIconBoxOrGreyIfUninstalled(
            packageName = packageName,
            size = 56.dp,
            force6dpClip = false,
        )
        Text(
            text = appName,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextSecondary,
        )
    }
}

/**
 * 앱 사용제한 기록 상세 화면 (Figma 1319-4815)
 * - 헤더 = 앱 이름
 * - 날짜별 타임라인, 시간 + 이벤트명
 * - start/stop: 기본 텍스트 색, release: 빨강
 */
@Composable
fun AppRestrictionHistoryDetailScreen(
    packageName: String,
    appName: String,
    userId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val logRepo = remember { AppLimitLogRepository() }
    val events by logRepo.getEventsFlow(context, userId, packageName).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 56.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        when {
            events.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "기록이 없어요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> {
                    val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.KOREAN)
                    val dateEntries = remember(events) {
                        val dateFormat = java.text.SimpleDateFormat("yyyy. MM. dd", java.util.Locale.KOREAN)
                        val grouped = events.groupBy { e ->
                            dateFormat.format(java.util.Date(e.timestamp))
                        }
                        grouped.entries
                            .sortedByDescending { (_, dayEvents) ->
                                dayEvents.maxOfOrNull { it.timestamp } ?: 0L
                            }
                            .map { (dateStr, dayEvents) ->
                                dateStr to dayEvents.sortedByDescending { it.timestamp }
                            }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
                            .clip(RoundedCornerShape(12.dp))
                            .background(AppColors.SurfaceBackgroundCard)
                            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 32.dp),
                    ) {
                        dateEntries.forEachIndexed { index, entry ->
                            val (dateStr, dayEvents) = entry
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFF3F3F3)),
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text(
                                    text = dateStr,
                                    style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    dayEvents.forEach { event ->
                                        val eventLabel = when (event.eventType) {
                                            "start" -> "카운트 시작"
                                            "stop" -> "카운트 중지"
                                            "release" -> "사용자 제한 해제"
                                            "timeout" -> "시간 소진"
                                            else -> event.eventType
                                        }
                                        val color = if (event.eventType == "release") AppColors.Red300 else AppColors.TextBody
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = timeFormat.format(java.util.Date(event.timestamp)),
                                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                                modifier = Modifier.widthIn(min = 60.dp),
                                            )
                                            Text(
                                                text = eventLabel,
                                                style = AppTypography.BodyMedium.copy(color = color),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.contains(context.packageName)
}

@Composable
fun AccountManageScreen(
    onBack: () -> Unit,
    currentUserInfo: com.aptox.app.AuthRepository.CurrentUserInfo?,
    /** Firebase 로그인 여부 — false면 회원 탈퇴 진입점 미노출 */
    isLoggedIn: Boolean,
    onLogout: () -> Unit,
    onGoogleClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val isGoogleLoggedIn = currentUserInfo?.providerLabel == "구글"
    val cardLabel = if (isGoogleLoggedIn) {
        googleAccountRowPrimaryText(currentUserInfo)
    } else {
        "구글 계정 로그인"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            SocialAccountCard(
                iconResId = R.drawable.ic_google,
                label = cardLabel,
                rightLabel = if (isGoogleLoggedIn) "로그아웃" else "로그인",
                onClick = {
                    if (isGoogleLoggedIn) {
                        showLogoutConfirm = true
                    } else {
                        onGoogleClick()
                    }
                },
            )
        }

        if (showLogoutConfirm) {
            AptoxConfirmDialog(
                onDismissRequest = { showLogoutConfirm = false },
                title = stringResource(R.string.account_logout_confirm_message),
                confirmButtonText = stringResource(R.string.dialog_yes),
                onConfirmClick = {
                    showLogoutConfirm = false
                    onLogout()
                },
                dismissButtonText = stringResource(R.string.dialog_no),
                onDismissButtonClick = { showLogoutConfirm = false },
            )
        }

        if (isLoggedIn) {
            // 하단 탈퇴하기 텍스트 버튼 (로그인 시에만)
            Text(
                text = "탈퇴하기",
                style = AppTypography.BodyRegular.copy(color = AppColors.TextCaption),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onWithdrawClick,
                    )
                    .padding(bottom = 28.dp, top = 16.dp),
            )
        }
    }
}

@Composable
private fun SocialAccountCard(
    iconResId: Int,
    label: String,
    subtitle: String? = null,
    rightLabel: String = "로그인",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = label,
            contentScale = ContentScale.None,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = rightLabel,
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AppColors.TextPrimary,
            )
        }
    }
}

@Composable
fun SubscriptionManageScreen(
    onOpenPremiumOffer: () -> Unit = {},
) {
    val context = LocalContext.current
    val premiumFromStore by PremiumStatusRepository.subscribedFlow(context.applicationContext)
        .collectAsState(initial = false)
    val premiumSnapshot by PremiumStatusRepository.premiumSnapshotFlow(context.applicationContext)
        .collectAsState(
            initial = PremiumSnapshot(
                subscribed = false,
                expiryEpochMillis = 0L,
                autoRenewing = true,
                basePlanId = null,
            ),
        )
    val showSubscribedPlan = SubscriptionManager.hasActiveSubscriptionForManagement(
        premiumFromStore,
        context.applicationContext,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        when {
            showSubscribedPlan -> {
                SubscriptionManageSubscribedPlanSection(
                    renewalOrExpiryLine = SubscriptionRenewalMessage.renewalOrExpiryLine(
                        premiumSnapshot.expiryEpochMillis,
                        premiumSnapshot.autoRenewing,
                    ),
                    basePlanId = premiumSnapshot.basePlanId,
                )
            }
            !SubscriptionManager.PREMIUM_OFFERING_LIVE -> {
                SubscriptionManageFreePlanSection(
                    onOpenPremiumOffer = onOpenPremiumOffer,
                    preLaunchCopy = true,
                )
            }
            else -> {
                SubscriptionManageFreePlanSection(
                    onOpenPremiumOffer = onOpenPremiumOffer,
                    preLaunchCopy = false,
                )
            }
        }
    }
}

/** 무료 플랜 — 유료 미오픈 시·미구독 시 */
@Composable
private fun SubscriptionManageFreePlanSection(
    onOpenPremiumOffer: () -> Unit,
    preLaunchCopy: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "무료 플랜",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = "기본 앱 제한·통계 기능을 무료로 이용 중이에요.",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
        }
        Text(
            text = if (preLaunchCopy) {
                "지금은 유료 구독 오픈 전이에요. 아래에서 프리미엄 요금·혜택만 미리 확인할 수 있어요."
            } else {
                "프리미엄 구독 시 제한 앱 수 무제한, 고급 통계 등 추가 기능을 이용할 수 있어요."
            },
            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        AptoxPrimaryButton(
            text = if (preLaunchCopy) "프리미엄 안내 보기" else "프리미엄 구독하기",
            onClick = onOpenPremiumOffer,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 유료 구독 중 — Figma 1629-5489(월간), 642-4630(연간) `04_3_1_설정_구독관리`
 * 카드 px16·py26·12dp 라운드, 안내는 Disclaimer 13/20 + ic_disclaimer_info 18
 */
@Composable
private fun SubscriptionManageSubscribedPlanSection(
    renewalOrExpiryLine: String,
    basePlanId: String?,
) {
    val isMonthly = basePlanId == SubscriptionBillingController.BASE_PLAN_MONTHLY
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(horizontal = 16.dp, vertical = 26.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isMonthly) {
                Text(
                    text = "월간 구독",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                )
                Text(
                    text = "₩3,900",
                    style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                )
            } else {
                Text(
                    text = "연간 구독",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₩46,800",
                        style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                    )
                    Text(
                        text = "월 환산 ₩3,900",
                        style = AppTypography.Caption2.copy(color = AppColors.TextTertiary),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IcoDisclaimerInfo(size = 18.dp, tint = AppColors.TextDisclaimer)
            Text(
                text = renewalOrExpiryLine,
                style = AppTypography.Disclaimer.copy(color = AppColors.TextDisclaimer),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 알림 설정 화면 (Figma 1022-3823)
 * - 기기 알림: 시스템 알림 허용 상태 + 설정 진입
 * - 챌린지: 뱃지 획득 알림
 * - 하루 사용량 지정: 마감 임박 / 카운트 중지 알림
 * - 지정 시간 제한: 시작 / 종료 알림
 * - 기기 알림 미허용 시 모든 토글 비활성화
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { NotificationPreferences }
    var badgeAcquired by remember { mutableStateOf(prefs.isBadgeAcquiredEnabled(context)) }
    var deadlineImminent by remember { mutableStateOf(prefs.isDeadlineImminentEnabled(context)) }
    var countReminder by remember { mutableStateOf(prefs.isCountReminderEnabled(context)) }
    var timeSpecifiedStart by remember { mutableStateOf(prefs.isTimeSpecifiedStartEnabled(context)) }
    var timeSpecifiedEnd by remember { mutableStateOf(prefs.isTimeSpecifiedEndEnabled(context)) }
    var deviceNotificationsEnabled by remember {
        mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // 이 화면 진입 시점에 이미 RESUMED 이면 Observer 가 ON_RESUME 을 다시 보내지 않음 → 채널·prefs 불일치 방지
    LaunchedEffect(Unit) {
        TimeSpecifiedRestrictionNotificationHelper.syncChannelsWithPreferences(context)
    }

    fun refreshDeviceNotifications() {
        deviceNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDeviceNotifications()
                badgeAcquired = prefs.isBadgeAcquiredEnabled(context)
                deadlineImminent = prefs.isDeadlineImminentEnabled(context)
                countReminder = prefs.isCountReminderEnabled(context)
                timeSpecifiedStart = prefs.isTimeSpecifiedStartEnabled(context)
                timeSpecifiedEnd = prefs.isTimeSpecifiedEndEnabled(context)
                TimeSpecifiedRestrictionNotificationHelper.syncChannelsWithPreferences(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val toggleEnabled = deviceNotificationsEnabled

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 기기 알림 단독 카드
            DeviceNotificationCard(
                badgeText = if (deviceNotificationsEnabled) "허용됨" else "허용되지 않음",
                badgeAllowed = deviceNotificationsEnabled,
                subtitle = if (deviceNotificationsEnabled) "알림을 받으려면 기기 알림 허용이 필요해요" else "기기 알림을 먼저 허용해주세요",
                onClick = {
                    context.startActivity(
                        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                },
            )

            // 챌린지 섹션
            NotificationSection(label = "챌린지") {
                SettingsRowWithToggle(
                    label = "뱃지 획득 알림",
                    subtitle = "뱃지를 획득할 때마다 알려드려요",
                    checked = badgeAcquired,
                    onCheckedChange = { v ->
                        prefs.setBadgeAcquiredEnabled(context, v)
                        badgeAcquired = v
                    },
                    enabled = toggleEnabled,
                )
            }

            // 하루 사용량 지정 섹션
            NotificationSection(label = "하루 사용량 지정") {
                SettingsRowWithToggle(
                    label = "마감 임박 알림",
                    subtitle = "일일 한도 1분 전에 미리 알려드려요",
                    checked = deadlineImminent,
                    onCheckedChange = { v ->
                        prefs.setDeadlineImminentEnabled(context, v)
                        deadlineImminent = v
                    },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "카운트 중지 알림",
                    subtitle = "카운트 중지를 잊으셨을 때 알려드려요",
                    checked = countReminder,
                    onCheckedChange = { v ->
                        prefs.setCountReminderEnabled(context, v)
                        countReminder = v
                    },
                    enabled = toggleEnabled,
                )
            }

            // 지정 시간 제한 섹션
            NotificationSection(label = "지정 시간 제한") {
                SettingsRowWithToggle(
                    label = "시작 알림",
                    subtitle = "설정된 시간에 시작하면 알려드려요",
                    checked = timeSpecifiedStart,
                    onCheckedChange = { v ->
                        prefs.setTimeSpecifiedStartEnabled(context, v)
                        timeSpecifiedStart = v
                        TimeSpecifiedRestrictionNotificationHelper.syncChannelsWithPreferences(context)
                    },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "종료 알림",
                    subtitle = "설정된 시간에 끝나면 알려드려요",
                    checked = timeSpecifiedEnd,
                    onCheckedChange = { v ->
                        prefs.setTimeSpecifiedEndEnabled(context, v)
                        timeSpecifiedEnd = v
                        TimeSpecifiedRestrictionNotificationHelper.syncChannelsWithPreferences(context)
                    },
                    enabled = toggleEnabled,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * 알림 설정 섹션: 레이블 텍스트 + 카드 컨테이너
 */
@Composable
private fun NotificationSection(
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
            modifier = Modifier.padding(start = 18.dp),
        )
        SettingsListCard(content = content)
    }
}

@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onUsageStatsClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = remember { mutableStateOf(Triple(false, false, true)) }

    fun refreshPermissions() {
        permissionState.value = Triple(
            isAccessibilityEnabled(context),
            StatisticsData.hasUsageAccess(context),
            context.isIgnoringBatteryOptimizations(),
        )
    }

    LaunchedEffect(Unit) { refreshPermissions() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val (accessibilityEnabled, usageStatsEnabled, batteryOptimizationExempt) = permissionState.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_manageaccount,
                label = "접근성 서비스",
                badgeText = if (accessibilityEnabled) "허용됨" else "허용되지 않음",
                badgeAllowed = accessibilityEnabled,
                subtitle = "앱 차단 기능에 필요해요",
                onClick = onAccessibilityClick,
            )
            SettingsDivider()
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_manageaccount,
                label = "사용 통계 접근",
                badgeText = if (usageStatsEnabled) "허용됨" else "허용되지 않음",
                badgeAllowed = usageStatsEnabled,
                subtitle = "사용 시간 측정에 필요해요",
                onClick = onUsageStatsClick,
            )
            SettingsDivider()
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_perm_battery,
                label = "백그라운드 배터리 사용 허용",
                badgeText = if (batteryOptimizationExempt) "허용됨" else "허용되지 않음",
                badgeAllowed = batteryOptimizationExempt,
                subtitle = "백그라운드에서 앱 사용을 꾸준히 추적해요",
                onClick = { context.openBatteryOptimizationSettingsOrFallback() },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppInfoScreen(onBack: () -> Unit, onTermsClick: () -> Unit, onPrivacyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            SettingsRowWithValue(
                iconResId = R.drawable.ic_manageaccount,
                label = "앱 버전",
                value = "1.0",
                showChevron = false,
                onClick = null,
            )
            SettingsDivider()
            SettingsRowWithValue(
                iconResId = R.drawable.ic_managesubs,
                label = "이용약관",
                showChevron = true,
                onClick = onTermsClick,
            )
            SettingsDivider()
            SettingsRowWithValue(
                iconResId = R.drawable.ic_managesubs,
                label = "개인정보처리방침",
                showChevron = true,
                onClick = onPrivacyClick,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OpenSourceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleTextRow(text = "data 1")
                SimpleTextRow(text = "data 2")
                SimpleTextRow(text = "data 3")
                SimpleTextRow(text = "data 4")
                SimpleTextRow(text = "data 5")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
