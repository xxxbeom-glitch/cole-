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
import java.text.Collator
import java.util.Locale

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
    onLogout: () -> Unit,
    onGoogleClick: () -> Unit = {},
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
fun SubscriptionManageScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 56.dp),
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
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val toggleEnabled = deviceNotificationsEnabled
    LaunchedEffect(badgeAcquired, deadlineImminent, countReminder, timeSpecifiedStart, timeSpecifiedEnd) {
        prefs.setBadgeAcquiredEnabled(context, badgeAcquired)
        prefs.setDeadlineImminentEnabled(context, deadlineImminent)
        prefs.setCountReminderEnabled(context, countReminder)
        prefs.setTimeSpecifiedStartEnabled(context, timeSpecifiedStart)
        prefs.setTimeSpecifiedEndEnabled(context, timeSpecifiedEnd)
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
                    onCheckedChange = { badgeAcquired = it },
                    enabled = toggleEnabled,
                )
            }

            // 하루 사용량 지정 섹션
            NotificationSection(label = "하루 사용량 지정") {
                SettingsRowWithToggle(
                    label = "마감 임박 알림",
                    subtitle = "일일 한도 1분 전에 미리 알려드려요",
                    checked = deadlineImminent,
                    onCheckedChange = { deadlineImminent = it },
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

            // 지정 시간 제한 섹션
            NotificationSection(label = "지정 시간 제한") {
                SettingsRowWithToggle(
                    label = "시작 알림",
                    subtitle = "설정된 시간에 시작하면 알려드려요",
                    checked = timeSpecifiedStart,
                    onCheckedChange = { timeSpecifiedStart = it },
                    enabled = toggleEnabled,
                )
                SettingsDivider()
                SettingsRowWithToggle(
                    label = "종료 알림",
                    subtitle = "설정된 시간에 끝나면 알려드려요",
                    checked = timeSpecifiedEnd,
                    onCheckedChange = { timeSpecifiedEnd = it },
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
    val permissionState = remember { mutableStateOf(Pair(false, false)) }

    fun refreshPermissions() {
        permissionState.value = Pair(
            isAccessibilityEnabled(context),
            StatisticsData.hasUsageAccess(context),
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

    val (accessibilityEnabled, usageStatsEnabled) = permissionState.value

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
