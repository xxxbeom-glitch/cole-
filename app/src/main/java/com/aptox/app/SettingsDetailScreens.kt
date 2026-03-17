package com.aptox.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import android.provider.Settings
import com.aptox.app.StatisticsData

/**
 * 설정 메뉴 세부 화면 (Figma MY-02~07)
 */

private fun isAccessibilityEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.contains(context.packageName)
}

private fun isOverlayAllowed(context: Context): Boolean =
    Settings.canDrawOverlays(context)

@Composable
fun AccountManageScreen(
    onBack: () -> Unit,
    currentUserInfo: com.aptox.app.AuthRepository.CurrentUserInfo?,
    onLogout: () -> Unit,
    onGoogleClick: () -> Unit = {},
    onNaverClick: () -> Unit = {},
    onKakaoClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
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
                iconResId = R.drawable.ic_kakao,
                label = "카카오 계정 로그인",
                rightLabel = if (currentUserInfo?.providerLabel == "카카오") "로그아웃" else "로그인",
                onClick = if (currentUserInfo?.providerLabel == "카카오") onLogout else onKakaoClick,
            )
            SocialAccountCard(
                iconResId = R.drawable.ic_naver,
                label = "네이버 계정 로그인",
                rightLabel = if (currentUserInfo?.providerLabel == "네이버") "로그아웃" else "로그인",
                onClick = if (currentUserInfo?.providerLabel == "네이버") onLogout else onNaverClick,
            )
            SocialAccountCard(
                iconResId = R.drawable.ic_google,
                label = "구글 계정 로그인",
                rightLabel = if (currentUserInfo?.providerLabel == "구글") "로그아웃" else "로그인",
                onClick = if (currentUserInfo?.providerLabel == "구글") onLogout else onGoogleClick,
            )
        }

        // 하단 탈퇴하기 텍스트 버튼
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

@Composable
private fun SocialAccountCard(
    iconResId: Int,
    label: String,
    rightLabel: String = "로그인",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = label,
            contentScale = ContentScale.None,
            modifier = Modifier.size(30.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )
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
    onBack: () -> Unit,
    onCancelSubscription: () -> Unit = {},
) {
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
                .padding(bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 구독 플랜 카드
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "연간 구독",
                    style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₩46,800",
                        style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                    )
                    Text(
                        text = "월 환산 ₩3,900",
                        style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                    )
                }
            }

            // 갱신 안내 텍스트
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 2.dp),
            ) {
                IcoDisclaimerInfo(size = 14.dp, tint = AppColors.TextCaption)
                Text(
                    text = "2027년 2월 3일 오후 3시 21분에 갱신됩니다",
                    style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                )
            }
        }

        // 하단 버튼 영역 (AptoxTwoLineButton 가이드 적용)
        AptoxTwoLineButton(
            primaryText = "구독 해지",
            ghostText = "돌아가기",
            onPrimaryClick = onCancelSubscription,
            onGhostClick = onBack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        )
    }
}

/**
 * 알림 설정 화면 (Figma 1022-3823, 1068-4550)
 * - 기기 알림: 시스템 알림 허용 상태 + 설정 진입
 * - 주간 리포트 / 마감 임박 / 목표 달성 / 카운트 중지 알림: 토글
 * - 기기 알림 미허용 시 모든 토글 비활성화 + "기기 알림을 먼저 허용해주세요" 안내
 */
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { NotificationPreferences }
    var weeklyReport by remember { mutableStateOf(prefs.isWeeklyReportEnabled(context)) }
    var deadlineImminent by remember { mutableStateOf(prefs.isDeadlineImminentEnabled(context)) }
    var goalAchievedAlert by remember { mutableStateOf(prefs.isGoalAchievedEnabled(context)) }
    var countReminder by remember { mutableStateOf(prefs.isCountReminderEnabled(context)) }
    var deviceNotificationsEnabled by remember { mutableStateOf(true) }

    fun refreshDeviceNotifications() {
        deviceNotificationsEnabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    LaunchedEffect(Unit) { refreshDeviceNotifications() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshDeviceNotifications()
                weeklyReport = prefs.isWeeklyReportEnabled(context)
                deadlineImminent = prefs.isDeadlineImminentEnabled(context)
                goalAchievedAlert = prefs.isGoalAchievedEnabled(context)
                countReminder = prefs.isCountReminderEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }

    val toggleEnabled = deviceNotificationsEnabled
    LaunchedEffect(weeklyReport, deadlineImminent, goalAchievedAlert, countReminder, toggleEnabled) {
        prefs.setWeeklyReportEnabled(context, weeklyReport)
        prefs.setDeadlineImminentEnabled(context, deadlineImminent)
        prefs.setGoalAchievedEnabled(context, goalAchievedAlert)
        prefs.setCountReminderEnabled(context, countReminder)
        if (toggleEnabled) {
            WeeklyReportAlarmScheduler.applySchedule(context, weeklyReport)
        }
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
            // Figma 1022-3824: 기기 알림 단독 카드
            DeviceNotificationCard(
                badgeText = if (deviceNotificationsEnabled) "허용됨" else "허용되지 않음",
                badgeAllowed = deviceNotificationsEnabled,
                subtitle = if (deviceNotificationsEnabled) "알림을 받으려면 기기 알림 허용이 필요해요" else "기기 알림을 먼저 허용해주세요",
                onClick = {
                    if (!deviceNotificationsEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        })
                    }
                },
            )
            // Figma 1068-4550: 주간 리포트 / 마감 임박 / 목표 달성 / 카운트 중지 알림 카드
            SettingsListCard {
                SettingsRowWithToggle(
                    label = "주간 리포트",
                    subtitle = "월요일마다, 지난 한 주 사용 현황을 알려드려요",
                    checked = weeklyReport,
                    onCheckedChange = { weeklyReport = it },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "마감 임박 알림",
                    subtitle = "일일 한도 1분 전에 미리 알려드려요",
                    checked = deadlineImminent,
                    onCheckedChange = { deadlineImminent = it },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "목표 달성 알림",
                    subtitle = "챌린지 성공 시 바로 알려드려요",
                    checked = goalAchievedAlert,
                    onCheckedChange = { goalAchievedAlert = it },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "카운트 중지 알림",
                    subtitle = "카운트 중지를 잊으셨을 때 알려드려요",
                    checked = countReminder,
                    onCheckedChange = { countReminder = it },
                    enabled = toggleEnabled,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onUsageStatsClick: () -> Unit,
    onOverlayClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionState = remember { mutableStateOf(Triple(false, false, false)) }

    fun refreshPermissions() {
        permissionState.value = Triple(
            isAccessibilityEnabled(context),
            StatisticsData.hasUsageAccess(context),
            isOverlayAllowed(context),
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

    val (accessibilityEnabled, usageStatsEnabled, overlayAllowed) = permissionState.value

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
                iconResId = R.drawable.ic_manageaccount,
                label = "다른 앱 위에 표시",
                badgeText = if (overlayAllowed) "허용됨" else "허용되지 않음",
                badgeAllowed = overlayAllowed,
                subtitle = "차단 화면 표시에 필요해요",
                onClick = onOverlayClick,
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
