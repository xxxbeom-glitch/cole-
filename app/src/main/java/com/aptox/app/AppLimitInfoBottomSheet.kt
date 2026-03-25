package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

/**
 * 제한 중인 앱 정보 바텀시트 (Figma 350-1802)
 * MA-01-1: 현재 진행중인 앱 상세
 */
data class AppLimitSummaryRow(
    val label: String,
    val value: String,
    /** null이면 기본 TextHighlight(보라색) 사용 */
    val valueColor: Color? = null,
)

@Composable
fun AppLimitInfoBottomSheet(
    title: String,
    appName: String,
    appIcon: Painter,
    appUsageText: String = "",
    appUsageLabel: String = "",
    appUsageTextColor: androidx.compose.ui.graphics.Color? = null,
    summaryRows: List<AppLimitSummaryRow>,
    onDismissRequest: () -> Unit,
    onPrimaryClick: () -> Unit,
    primaryButtonText: String = "계속 진행",
    secondaryButtonText: String? = "돌아가기",
    onSecondaryClick: (() -> Unit)? = null,
    isPrimaryEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    bodyText: String? = null,
    onDetailClick: (() -> Unit)? = null,
    /**
     * [AppLimitInfoBottomSheetDaily]와 동일: 앱 상태 행 오른쪽에 `AptoxSecondaryButton`으로 표시.
     * 지정 시 푸터의 secondary(ghost)는 숨김 — 제한 해제는 인라인만 사용.
     */
    inlineReleaseButtonText: String? = null,
    onInlineReleaseClick: (() -> Unit)? = null,
) {
    val footerSecondaryText =
        if (inlineReleaseButtonText != null) null else secondaryButtonText
    BaseBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onPrimaryClick,
        primaryButtonText = primaryButtonText,
        primaryButtonEnabled = isPrimaryEnabled,
        secondaryButtonText = footerSecondaryText,
        onSecondaryClick = onSecondaryClick ?: onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (bodyText != null) {
                Text(
                    text = bodyText,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }

            if (inlineReleaseButtonText != null && onInlineReleaseClick != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppStatusRow(
                        appName = appName,
                        appIcon = appIcon,
                        variant = AppStatusVariant.Button,
                        usageText = appUsageText,
                        usageLabel = appUsageLabel,
                        usageTextColor = appUsageTextColor,
                        onDetailClick = onDetailClick,
                        modifier = Modifier.weight(1f),
                    )
                    AptoxSecondaryButton(
                        text = inlineReleaseButtonText,
                        onClick = onInlineReleaseClick,
                    )
                }
            } else {
                AppStatusRow(
                    appName = appName,
                    appIcon = appIcon,
                    variant = AppStatusVariant.Button,
                    usageText = appUsageText,
                    usageLabel = appUsageLabel,
                    usageTextColor = appUsageTextColor,
                    onDetailClick = onDetailClick,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                summaryRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.label,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        )
                        Text(
                            text = row.value,
                            style = AppTypography.BodyBold.copy(color = row.valueColor ?: AppColors.TextHighlight),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 제한 중인 앱 정보 바텀시트 - 시간 지정 제한 앱 일시 정지 중 (Figma 782-2858)
 */
@Composable
fun AppLimitInfoBottomSheetPaused(
    title: String,
    appName: String,
    appIcon: Painter,
    pauseRemainingText: String,
    summaryRows: List<AppLimitSummaryRow>,
    onDismissRequest: () -> Unit,
    onPrimaryClick: () -> Unit,
    primaryButtonText: String = "제한 재개",
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onPrimaryClick,
        primaryButtonText = primaryButtonText,
        secondaryButtonText = "돌아가기",
        onSecondaryClick = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppStatusRow(
                appName = appName,
                appIcon = appIcon,
                variant = AppStatusVariant.Button,
                usageText = pauseRemainingText,
                usageLabel = "일시 정지 중",
                usageTextColor = AppColors.Red300,
                usageLabelColor = AppColors.Red300,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                summaryRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.label,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        )
                        Text(
                            text = row.value,
                            style = AppTypography.BodyBold.copy(color = row.valueColor ?: AppColors.TextHighlight),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 제한 중인 앱 정보 바텀시트 - 일일 사용량 제한 (Figma 1159-4418, 1159-4492)
 * 수동 타이머(카운트 시작/정지) 연동.
 */
@Composable
fun AppLimitInfoBottomSheetDaily(
    packageName: String,
    appName: String,
    appIcon: Painter,
    limitMinutes: Int,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { ManualTimerRepository(context) }
    val logRepo = remember { AppLimitLogRepository() }

    fun launchApp() {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) context.startActivity(launchIntent)
    }
    var todayUsageMinutes by remember { mutableStateOf((repo.getTodayUsageMs(packageName) / 60_000).toInt()) }
    var isCountActive by remember { mutableStateOf(repo.isSessionActive(packageName)) }
    var showCountStartDialog by remember { mutableStateOf(false) }

    fun refreshFromRepo() {
        repo.ensureMidnightResetIfNeeded()
        val usage = (repo.getTodayUsageMs(packageName) / 60_000).toInt()
        todayUsageMinutes = usage
        isCountActive = repo.isSessionActive(packageName)
        if (isCountActive && usage >= limitMinutes) {
            repo.endSession(packageName)
            AppLimitLogRepository.saveTimeoutEventIfNeeded(context, packageName, appName)
            isCountActive = false
        }
    }

    LaunchedEffect(packageName) {
        refreshFromRepo()
    }
    // 앱 포그라운드 복귀 시 저장된 startTimeMs 기준으로 경과 시간 재계산 (백그라운드에서 멈춘 것처럼 보이는 버그 방지)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshFromRepo()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // 카운트 진행 중일 때 1초마다 사용량 갱신 (startTimeMs 기준 now-startMs 계산으로 포그라운드에서 실시간 표시)
    LaunchedEffect(packageName, isCountActive) {
        if (!isCountActive) return@LaunchedEffect
        while (true) {
            delay(1000L)
            todayUsageMinutes = (repo.getTodayUsageMs(packageName) / 60_000).toInt()
            if (todayUsageMinutes >= limitMinutes) {
                repo.endSession(packageName)
                AppLimitLogRepository.saveTimeoutEventIfNeeded(context, packageName, appName)
                isCountActive = false
                return@LaunchedEffect
            }
            isCountActive = repo.isSessionActive(packageName)
        }
    }
    // 카운트 중지 상태에서도 자정 경과 시 사용량 초기화 반영 (30초마다 날짜 체크)
    LaunchedEffect(packageName) {
        while (true) {
            delay(30_000L)
            repo.ensureMidnightResetIfNeeded()
            refreshFromRepo()
        }
    }

    val usageExhausted = todayUsageMinutes >= limitMinutes

    BaseBottomSheet(
        title = "사용량 확인",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = {
            if (usageExhausted) return@BaseBottomSheet
            if (isCountActive) {
                // 카운트 정지: 세션 종료 후 바텀시트 자동 닫기
                val totalUsageMs = repo.getTodayUsageMs(packageName)
                repo.endSession(packageName)
                (context.applicationContext as? AptoxApplication)?.applicationScope?.launch {
                    logRepo.saveEvent(
                        context.applicationContext,
                        FirebaseAuth.getInstance().currentUser?.uid,
                        packageName,
                        "stop",
                        appName,
                    )
                }
                BadgeAutoGrant.onCountEnded(context, packageName, limitMinutes, totalUsageMs)
                isCountActive = false
                todayUsageMinutes = (repo.getTodayUsageMs(packageName) / 60_000).toInt()
                onDismissRequest()
            } else {
                // 카운트 시작: 세션 시작 → 안내 팝업 → 확인 시 앱 실행
                repo.startSession(packageName)
                (context.applicationContext as? AptoxApplication)?.applicationScope?.launch {
                    logRepo.saveEvent(
                        context.applicationContext,
                        FirebaseAuth.getInstance().currentUser?.uid,
                        packageName,
                        "start",
                        appName,
                    )
                }
                BadgeAutoGrant.onCountStartButtonPressed(context)
                isCountActive = true
                todayUsageMinutes = (repo.getTodayUsageMs(packageName) / 60_000).toInt()
                showCountStartDialog = true
            }
        },
        primaryButtonText = when {
            usageExhausted -> "오늘 사용량을 전부 사용하셨어요"
            isCountActive -> "카운트 중지"
            else -> "카운트 시작"
        },
        primaryButtonEnabled = !usageExhausted,
        secondaryButtonText = "닫기",
        onSecondaryClick = onDismissRequest,
        dismissOnPrimaryClick = usageExhausted || isCountActive,
        modifier = modifier,
    ) {
        if (showCountStartDialog) {
            AptoxConfirmDialog(
                onDismissRequest = { showCountStartDialog = false },
                title = "카운트가 시작됐어요",
                subtitle = "앱 사용이 끝나면 반드시\n카운트 중지를 눌러주세요\n중지하지 않으면 사용시간이\n계속 누적돼요",
                confirmButtonText = "확인",
                onConfirmClick = {
                    showCountStartDialog = false
                    launchApp()
                },
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppStatusRow(
                    appName = appName,
                    appIcon = appIcon,
                    variant = AppStatusVariant.Button,
                    usageText = if (usageExhausted) "사용 완료" else "$todayUsageMinutes/${limitMinutes}분",
                    usageLabel = if (usageExhausted) "" else "사용 중",
                    usageTextColor = AppColors.TextHighlight,
                    modifier = Modifier.weight(1f),
                )
                AptoxSecondaryButton(
                    text = "제한 해제",
                    onClick = {
                        RestrictionDeleteHelper.deleteRestrictedApp(context, packageName)
                        onDismissRequest()
                    },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                listOf(
                    AppLimitSummaryRow("하루 사용량", "${limitMinutes}분"),
                    AppLimitSummaryRow("현재 사용량", "${todayUsageMinutes}분"),
                    AppLimitSummaryRow("제한 방식", "하루 사용량 제한", valueColor = AppColors.TextHighlight),
                ).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.label,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        )
                        Text(
                            text = row.value,
                            style = AppTypography.BodyBold.copy(color = row.valueColor ?: AppColors.TextHighlight),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

/*
 * [기존 UsageStats 기반 - 주석 처리]
 * fun AppLimitInfoBottomSheetDailyLegacy(
 *     title: String, appName: String, appIcon: Painter,
 *     usageMinutes: String, sessionCount: String, summaryRows: List<AppLimitSummaryRow>,
 *     onDismissRequest: () -> Unit, onPrimaryClick: () -> Unit,
 *     primaryButtonText: String = "계속 진행", modifier: Modifier = Modifier,
 * ) { ... }
 */