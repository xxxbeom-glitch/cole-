package com.aptox.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.activity.ComponentActivity

/**
 * 앱 사용정보·접근성 필수 권한이 모두 허용되었는지 (알림 등 선택 권한 제외).
 * 오버레이 권한은 AlertDialog 방식으로 전환되어 더 이상 필요하지 않음.
 */
fun android.content.Context.areRequiredAppPermissionsGranted(): Boolean {
    val usageGranted = StatisticsData.hasUsageAccess(this)
    val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
    val accessibilityGranted = enabled.contains(packageName)
    return usageGranted && accessibilityGranted
}

/** API 23 미만이거나 이미 예외인 경우 true */
fun android.content.Context.isIgnoringBatteryOptimizations(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    val pm = getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(packageName)
}

private const val PREFS_PERM_FLOW = "aptox_permissions"
private const val KEY_POST_NOTIFICATIONS_PROMPTED = "post_notifications_prompted"

/** Android 8+ 앱 알림 설정 화면 (런타임 권한이 더 이상 뜨지 않을 때) */
private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
    }
    try {
        startActivity(intent)
    } catch (_: Exception) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )
    }
}

/**
 * 앱 접근 권한 안내 화면 (풀스크린).
 * Figma 1125-5261 기반. 아이콘 원본 사이즈 유지, 설정 바로가기 → 기기 설정 연동.
 */
@Composable
fun PermissionScreen(
    onPrimaryClick: () -> Unit,
    onGhostClick: () -> Unit, // DebugMenuScreen 호환성을 위해 시그니처 유지
    /** false면 필수 미충족 시에도 '다음에 하기'로 진행 가능 (디버그 미리보기용) */
    enforceRequiredPermissionsForNext: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permFlowPrefs = remember { context.getSharedPreferences(PREFS_PERM_FLOW, Context.MODE_PRIVATE) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        refresh++
    }

    val usageGranted = remember(refresh) { StatisticsData.hasUsageAccess(context) }
    val accessibilityGranted = remember(refresh) {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        enabled.contains(context.packageName)
    }
    val batteryOptimizationExempt = remember(refresh) { context.isIgnoringBatteryOptimizations() }
    val requiredOk = remember(refresh) { context.areRequiredAppPermissionsGranted() }
    val notificationGranted = remember(refresh) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }
    val allRequiredGranted = requiredOk

    LaunchedEffect(requiredOk) {
        if (requiredOk) {
            AptoxApplication.startAppMonitorIfNeeded(context.applicationContext)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // 가로 중앙 정렬
            verticalArrangement = Arrangement.Center, // 세로(컨텐츠 그룹) 중앙 정렬
        ) {
            Text(
                text = "앱 접근 권한 안내",
                style = AppTypography.Display2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "서비스 이용을 위해 다음의 허용이 필요해요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(36.dp))

            // 필수 권한 카드 (배터리 예외는 권장 — 재부팅 후 모니터링 안정화)
            PermissionCard(
                items = buildList {
                    add(
                        PermissionItem(
                            iconResId = R.drawable.ic_perm_usage,
                            title = "앱 사용정보 접근 (필수)",
                            description = "앱별 사용 시간 측정과 사용 제한 기능 작동에 필요한 권한입니다",
                            onSettingsClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                            isGranted = usageGranted,
                        ),
                    )
                    add(
                        PermissionItem(
                            iconResId = R.drawable.ic_perm_accessibility,
                            title = "접근성 서비스 (필수)",
                            description = "제한 중인 앱으로 이동할 때 사용 제한 화면을 표시하기 위해 필요합니다",
                            onSettingsClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                            isGranted = accessibilityGranted,
                        ),
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        add(
                            PermissionItem(
                                iconResId = R.drawable.ic_perm_battery,
                                title = "배터리 최적화 예외 (권장)",
                                description = "재부팅 후에도 앱 모니터링과 알림이 끊기지 않도록 배터리 절전에서 제외해 주세요",
                                onSettingsClick = {
                                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(i)
                                },
                                isGranted = batteryOptimizationExempt,
                            ),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Android 13+: POST_NOTIFICATIONS — 모니터링 포그라운드 알림·기타 알림 표시에 필요 (런타임 요청)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    items = listOf(
                        PermissionItem(
                            iconResId = R.drawable.ic_perm_notification,
                            title = "알림 (권장)",
                            description = "Android 13 이상에서는 모니터링 알림·사용 시간 안내를 표시하려면 알림 권한이 필요합니다. 허용하지 않으면 포그라운드 알림이 보이지 않을 수 있어요",
                            onSettingsClick = click@{
                                val act = context as? ComponentActivity ?: return@click
                                if (notificationGranted) return@click
                                when {
                                    ActivityCompat.shouldShowRequestPermissionRationale(
                                        act,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) -> {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    !permFlowPrefs.getBoolean(KEY_POST_NOTIFICATIONS_PROMPTED, false) -> {
                                        permFlowPrefs.edit()
                                            .putBoolean(KEY_POST_NOTIFICATIONS_PROMPTED, true)
                                            .apply()
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    else -> context.openAppNotificationSettings()
                                }
                            },
                            showSettingsButton = true,
                            actionLabelWhenNotGranted = "알림 허용하기",
                            isGranted = notificationGranted,
                        ),
                    ),
                )
            } else {
                PermissionCard(
                    items = listOf(
                        PermissionItem(
                            iconResId = R.drawable.ic_perm_notification,
                            title = "알림",
                            description = "사용 시간 초과 알림과 목표 달성 소식을 알림으로 알립니다",
                            onSettingsClick = { },
                            showSettingsButton = false,
                            isGranted = true,
                        ),
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 선택 권한 안내 (디스클라이머)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), // 디스클라이머 가로 중앙 정렬
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_disclaimer_info),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.TextDisclaimer,
                )
                Text(
                    text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        "알림을 끄면 모니터링 표시가 숨겨질 수 있어요. 앱 사용 제한(필수 권한)은 알림과 별개로 동작합니다"
                    } else {
                        "선택 권한을 허용하지 않아도 서비스 이용이 가능합니다"
                    },
                    style = AppTypography.Caption2.copy(color = AppColors.TextDisclaimer),
                )
            }
        }

        // 하단 버튼 (Figma 1125-5261): 필수 충족 시 Primary「다음」, 미충족 시 Text「다음에 하기」
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            if (allRequiredGranted) {
                AptoxPrimaryButton(
                    text = "다음",
                    onClick = onPrimaryClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                AptoxTextOnlyButton(
                    text = "다음에 하기",
                    onClick = {
                        if (enforceRequiredPermissionsForNext) {
                            onGhostClick()
                        } else {
                            onPrimaryClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private data class PermissionItem(
    val iconResId: Int,
    val title: String,
    val description: String,
    val onSettingsClick: () -> Unit,
    val showSettingsButton: Boolean = true,
    /** 미허용 시 표시할 액션 문구 (기본: 설정 바로가기) */
    val actionLabelWhenNotGranted: String? = null,
    val isGranted: Boolean = false,
)

private val PermissionCardShape = RoundedCornerShape(12.dp)
private val PermissionCardShadowColor = Color.Black.copy(alpha = 0.04f)

@Composable
private fun PermissionCard(
    items: List<PermissionItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp, 
                shape = PermissionCardShape, 
                spotColor = PermissionCardShadowColor, 
                ambientColor = PermissionCardShadowColor
            )
            .clip(PermissionCardShape)
            .background(AppColors.White900)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items.forEach { item ->
            PermissionItemRow(
                iconResId = item.iconResId,
                title = item.title,
                description = item.description,
                onSettingsClick = item.onSettingsClick,
                showSettingsButton = item.showSettingsButton,
                actionLabelWhenNotGranted = item.actionLabelWhenNotGranted,
                isGranted = item.isGranted,
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    iconResId: Int,
    title: String,
    description: String,
    onSettingsClick: () -> Unit,
    showSettingsButton: Boolean = true,
    actionLabelWhenNotGranted: String? = null,
    isGranted: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 아이콘: 첨부된 이미지 사이즈(96x96) 그대로 사용, ContentScale.Fit으로 비율 유지
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Text(
                text = description,
                style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
            )
            if (showSettingsButton) {
                if (isGranted) {
                    Text(
                        text = "허용됨",
                        style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = onSettingsClick)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = actionLabelWhenNotGranted ?: "설정 바로가기",
                            style = AppTypography.Caption2.copy(color = AppColors.Red300),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppColors.Red300,
                        )
                    }
                }
            }
        }
    }
}