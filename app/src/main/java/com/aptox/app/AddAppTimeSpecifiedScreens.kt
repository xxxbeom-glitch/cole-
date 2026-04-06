package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────
// 시간 지정 제한 플로우 (Figma 241-3287, 1407-5301, 1407-5364)
// 로직 미연결 — UI 전용
// ─────────────────────────────────────────────

private val TimeSpecifiedHeaderTopPadding = 10.dp

/**
 * "HH:MM 오전|오후" (또는 기존 "HH:MM AM|PM") 문자열을 분 단위 정수(0~1439)로 변환
 */
private fun timeStringToMinutes(time: String): Int? {
    return try {
        val parts = time.trim().split(":")
        if (parts.size < 2) return null
        val hour = parts[0].trim().toInt()
        val minuteTokens = parts[1].trim().split(Regex("\\s+"))
        if (minuteTokens.size < 2) return null
        val minute = minuteTokens[0].toInt()
        val period = minuteTokens[1]
        val isAm = period.equals("AM", ignoreCase = true) || period == "오전"
        val isPm = period.equals("PM", ignoreCase = true) || period == "오후"
        if (!isAm && !isPm) return null
        val hour24 = when {
            isAm && hour == 12 -> 0
            isPm && hour != 12 -> hour + 12
            else -> hour
        }
        hour24 * 60 + minute
    } catch (e: Exception) {
        null
    }
}

/**
 * "HH:MM 오전|오후" 형식 문자열을 오늘 날짜 기준 ms 타임스탬프로 변환.
 * 초·밀리초는 0으로 맞춰 분 단위 시각과 [adjustTimeSpecifiedWindow]의 분 단위 비교가 일치한다.
 */
private fun timeStringToTodayMs(time: String): Long? {
    val totalMin = timeStringToMinutes(time) ?: return null
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, totalMin / 60)
        set(java.util.Calendar.MINUTE, totalMin % 60)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

/**
 * 시작/종료 시각을 올바른 날짜로 조정:
 * - 시작: 분 단위로만 비교. 같은 분이면 오늘 구간으로 유지하고, 시작 분이 현재 분보다 과거일 때만 내일로 이동.
 * - 종료가 시작보다 앞서면(당일 자정 넘김 등) → endMs +1일
 */
private fun adjustTimeSpecifiedWindow(startMs: Long, endMs: Long, now: Long): Pair<Long, Long> {
    var s = startMs
    var e = endMs
    val sMin = s / 60_000L * 60_000L
    val nowMin = now / 60_000L * 60_000L
    if (sMin < nowMin) s += ONE_DAY_MS
    if (e <= s) e += ONE_DAY_MS
    return s to e
}

private enum class TimeSpecifiedStep {
    SETUP,    // Screen01: 앱·시작/종료 시간 선택 (Figma 241:3287)
    START_PICKER, // Screen02-A: 시작 시간 드럼롤 피커 바텀시트 (Figma 1407:5301)
    END_PICKER,   // Screen02-B: 종료 시간 드럼롤 피커 바텀시트
    COMPLETE, // Screen03: 설정 완료 요약 (Figma 1407:5364)
}

/**
 * 시간 지정 제한 플로우 호스트
 * - SETUP에서 「다음」 시 저장 후 COMPLETE(설정 완료)로 이동 — 완료 화면 이전 단계 뒤로가기는 미저장
 * - COMPLETE에서 뒤로가기/「홈으로 이동」은 이미 저장된 상태로 홈(onComplete)
 *
 * @param initialPrefilledApp 홈 빈 카드 등에서 「제한 앱 추가」→ 시간 지정으로 진입 시 미리 채울 앱 (AddAppFlowHost의 [initialPrefilledApp]과 동일 역할)
 */
@Composable
fun TimeSpecifiedFlowHost(
    onComplete: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
    initialPrefilledApp: com.aptox.app.model.SelectedAppInfo? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(TimeSpecifiedStep.SETUP) }

    // 선택값 상태 (홈에서 전달된 앱이 있으면 앱명·패키지 프리필)
    var selectedAppName by remember(initialPrefilledApp) {
        mutableStateOf(initialPrefilledApp?.appName.orEmpty())
    }
    var selectedPackageName by remember(initialPrefilledApp) {
        mutableStateOf(initialPrefilledApp?.packageName.orEmpty())
    }
    var selectedStartTime by remember { mutableStateOf<String?>(null) }
    var selectedEndTime by remember { mutableStateOf<String?>(null) }

    // 바텀시트 표시 여부
    var showAppSelectSheet by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    when (step) {
        TimeSpecifiedStep.SETUP,
        TimeSpecifiedStep.START_PICKER,
        TimeSpecifiedStep.END_PICKER -> {
            TimeSpecifiedScreen01(
                selectedAppName = selectedAppName,
                selectedStartTime = selectedStartTime,
                selectedEndTime = selectedEndTime,
                onAppRowClick = { showAppSelectSheet = true },
                onStartTimeRowClick = { showStartPicker = true },
                onEndTimeRowClick = { showEndPicker = true },
                onNextClick = next@{
                    val rawStartMs = selectedStartTime?.let { timeStringToTodayMs(it) }
                    val rawEndMs = selectedEndTime?.let { timeStringToTodayMs(it) }
                    if (rawStartMs == null || rawEndMs == null) return@next
                    val now = System.currentTimeMillis()
                    val (startMs, endMs) = adjustTimeSpecifiedWindow(rawStartMs, rawEndMs, now)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val repo = AppRestrictionRepository(context)
                            val restriction = com.aptox.app.model.AppRestriction(
                                packageName = selectedPackageName.ifBlank { "com.instagram.android" },
                                appName = selectedAppName.ifBlank { "인스타그램" },
                                limitMinutes = 0,
                                blockUntilMs = endMs,
                                baselineTimeMs = System.currentTimeMillis(),
                                startTimeMs = startMs,
                            )
                            repo.save(restriction)
                            val map = repo.toRestrictionMap()
                            AppMonitorService.start(context, map)
                            TimeSpecifiedRestrictionAlarmScheduler.scheduleAll(context)
                        }
                        step = TimeSpecifiedStep.COMPLETE
                    }
                },
                onBackClick = onBackFromFirst,
                modifier = modifier,
            )

            // 앱 선택 바텀시트
            if (showAppSelectSheet) {
                AddAppSelectBottomSheet(
                    initialSelected = if (selectedAppName.isNotEmpty()) setOf(selectedAppName) else emptySet(),
                    onDismissRequest = { showAppSelectSheet = false },
                    onSelectComplete = { names ->
                        selectedAppName = names.firstOrNull() ?: ""
                        showAppSelectSheet = false
                    },
                    onSelectCompleteWithPackages = { apps ->
                        apps.firstOrNull()?.let {
                            selectedAppName = it.appName
                            selectedPackageName = it.packageName
                        }
                    },
                )
            }

            // 시작 시간 드럼롤 피커 바텀시트
            if (showStartPicker) {
                DrumrollTimePickerBottomSheet(
                    title = "시작 시간을 설정해주세요",
                    onDismissRequest = { showStartPicker = false },
                    onConfirm = { hour, minute, amPm ->
                        selectedStartTime = "%02d:%02d %s".format(hour, minute, amPm)
                        showStartPicker = false
                    },
                )
            }

            // 종료 시간 드럼롤 피커 바텀시트
            if (showEndPicker) {
                DrumrollTimePickerBottomSheet(
                    title = "종료 시간을 설정해주세요",
                    onDismissRequest = { showEndPicker = false },
                    onConfirm = { hour, minute, amPm ->
                        selectedEndTime = "%02d:%02d %s".format(hour, minute, amPm)
                        showEndPicker = false
                    },
                )
            }
        }

        TimeSpecifiedStep.COMPLETE -> {
            // 이미 SETUP에서 저장됨 — 여기서는 홈으로만 복귀
            TimeSpecifiedScreen03(
                selectedAppName = selectedAppName.ifBlank { "인스타그램" },
                selectedStartTime = selectedStartTime ?: "",
                selectedEndTime = selectedEndTime ?: "",
                onHomeClick = onComplete,
                onBackClick = onComplete,
                modifier = modifier,
            )
        }
    }
}

// ─────────────────────────────────────────────
// Screen01: 앱·시작/종료 시간 선택 (Figma 241:3287)
// ─────────────────────────────────────────────

/**
 * 시간 지정 제한 설정 화면
 * - 앱 선택 / 시작 시간 / 종료 시간 SelectionRow 3개
 * - 하단 "다음" 버튼
 */
@Composable
fun TimeSpecifiedScreen01(
    selectedAppName: String,
    selectedStartTime: String?,
    selectedEndTime: String?,
    onAppRowClick: () -> Unit,
    onStartTimeRowClick: () -> Unit,
    onEndTimeRowClick: () -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canProceed = selectedAppName.isNotEmpty() &&
        selectedStartTime != null &&
        selectedEndTime != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = TimeSpecifiedHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AptoxHeaderSub(
            title = "시간 지정 제한",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = false,
            hasNotification = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "아래 내용을 선택해주세요",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // 앱 선택 행
                SelectionRow(
                    label = "앱을 선택해주세요",
                    variant = if (selectedAppName.isNotEmpty()) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedAppName,
                    onClick = onAppRowClick,
                )
                // 시작 시간 행
                SelectionRow(
                    label = "시작 시간을 설정해주세요",
                    variant = if (selectedStartTime != null) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedStartTime ?: "",
                    onClick = onStartTimeRowClick,
                )
                // 종료 시간 행
                SelectionRow(
                    label = "종료 시간을 설정해주세요",
                    variant = if (selectedEndTime != null) SelectionRowVariant.Selected else SelectionRowVariant.Default,
                    selectedValue = selectedEndTime ?: "",
                    onClick = onEndTimeRowClick,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AptoxPrimaryButton(
                text = "다음",
                onClick = onNextClick,
                enabled = canProceed,
            )
        }
    }
}

// ─────────────────────────────────────────────
// Screen03: 설정 완료 요약 (Figma 1407:5364)
// ─────────────────────────────────────────────

/**
 * 시간 지정 제한 설정 완료 화면
 * - 제목: "설정이 완료되었어요"
 * - 요약 InfoBox: 선택된 앱 / 제한 시작 시간 / 제한 종료 시간
 * - 하단: Primary 단일 "홈으로 이동" (앱 primary 색상)
 */
@Composable
fun TimeSpecifiedScreen03(
    selectedAppName: String,
    selectedStartTime: String,
    selectedEndTime: String,
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = TimeSpecifiedHeaderTopPadding)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AptoxHeaderSub(
            title = "시간 지정 제한",
            backIcon = painterResource(R.drawable.ic_back),
            onBackClick = onBackClick,
            showNotification = false,
            hasNotification = false,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "설정이 완료되었어요",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(36.dp))
            TimeSpecifiedSummaryBox(
                selectedAppName = selectedAppName,
                selectedStartTime = selectedStartTime,
                selectedEndTime = selectedEndTime,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            AptoxPrimaryButton(
                text = "홈으로 이동",
                onClick = onHomeClick,
            )
        }
    }
}

/**
 * 시간 지정 제한 설정 요약 InfoBox (Figma 1407:5369)
 * - 선택된 앱 / 제한 시작 시간 / 제한 종료 시간
 */
@Composable
private fun TimeSpecifiedSummaryBox(
    selectedAppName: String,
    selectedStartTime: String,
    selectedEndTime: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(12.dp))
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TimeSpecifiedSummaryRow(label = "선택된 앱", value = selectedAppName)
        TimeSpecifiedSummaryRow(label = "제한 시작 시간", value = selectedStartTime)
        TimeSpecifiedSummaryRow(label = "제한 종료 시간", value = selectedEndTime)
    }
}

// ─────────────────────────────────────────────
// 제한 방식 선택 바텀시트 (Figma 1408:5511)
// "일일 사용량 지정" / "지정 시간 제한" — 두 버튼 모두 Ghost 스타일 통일
// ─────────────────────────────────────────────

/**
 * 제한 방식 선택 바텀시트 (Figma 1408:5511)
 * - 두 버튼 모두 AptoxGhostButton 스타일로 통일
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionTypeSelectBottomSheet(
    onDailyLimitClick: () -> Unit,
    onTimeSpecifiedClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 56.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RestrictionTypeButton(
                title = "하루 사용량 지정",
                subtitle = "매일 하루 사용량을 조절하고 싶을 때",
                onClick = {
                    onDismissRequest()
                    onDailyLimitClick()
                },
            )
            RestrictionTypeButton(
                title = "지정 시간 제한",
                subtitle = "원하는 시간대의 사용을 제한하고 싶을 때",
                onClick = {
                    onDismissRequest()
                    onTimeSpecifiedClick()
                },
            )
        }
    }
}

/**
 * 제한 방식 선택 버튼 (Primary200 배경, Primary300 텍스트, 2줄 구성)
 */
@Composable
private fun RestrictionTypeButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgColor = if (isPressed) AppColors.Primary200.copy(alpha = 1f).let {
        androidx.compose.ui.graphics.lerp(it, Color.Black, 0.04f)
    } else AppColors.Primary200

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 10.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTypography.ButtonLarge.copy(color = AppColors.Primary300),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = subtitle,
            style = AppTypography.Caption2.copy(color = AppColors.Primary300),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TimeSpecifiedSummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        )
        Text(
            text = value,
            style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
        )
    }
}
