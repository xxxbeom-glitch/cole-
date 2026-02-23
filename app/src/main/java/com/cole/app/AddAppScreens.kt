package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// AA-01: 제한 앱 추가 플로우
// AA-02A-03, AA-03-01 공통 화면 재사용
// ─────────────────────────────────────────────

/** 제한 앱 추가 플로우 단계 */
enum class AddAppStep {
    AA_01,           // 앱 선택/추가
    AA_02A_01,       // 제한 방식 선택
    // 일일사용량 제한 플로우 (5화면)
    AA_DAILY_01,     // 일일 사용 한도 설정 (304-1507)
    AA_DAILY_02,     // 사용량 경고 설정 (258-2392)
    AA_DAILY_03,     // 적용 요일 설정 (304-1760)
    AA_DAILY_04,     // 설정 요약 확인 (241-3409)
    AA_DAILY_05,     // 완료 (243-6318)
    // 시간지정제한 플로우
    AA_02A_02, AA_02A_03,  // 사용 시간 + 요일 (StepBar)
    AA_02B_02,       // 앱 차단 — 설정
    AA_03_01,        // 공통: 완료 확인
}

// ─────────────────────────────────────────────
// AA-01: 앱 선택 화면 (Figma 229-1111)
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA01(
    onAddAppClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(
                title = "앱 제한",
                backIcon = painterResource(R.drawable.ic_back),
                onBackClick = onBackClick,
                showNotification = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "제한할 앱을 선택해주세요",
                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
            )
            ColeInfoBox(
                text = "선택한 앱에 사용 시간 제한이나 차단을 설정할 수 있습니다.",
            )
            Spacer(modifier = Modifier.height(24.dp))
            ColeAddAppButton(
                text = "앱 추가하기",
                icon = painterResource(R.drawable.ic_add),
                onClick = onAddAppClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-01: 제한 방식 선택 (Figma 241-3084)
// 일일사용량 제한 | 시간지정제한 | 앱 차단
// ─────────────────────────────────────────────

@Composable
fun AddAppScreenAA02A01(
    onDailyLimitClick: () -> Unit,   // 일일사용량 제한
    onTimeSpecifiedClick: () -> Unit, // 시간지정제한
    onBlockClick: () -> Unit,         // 앱 차단
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val limitOptions = listOf(
        SelectionCardItem("일일사용량 제한", "매일 사용할 수 있는 시간을 설정합니다", "30분"),
        SelectionCardItem("시간지정제한", "특정 시간대에만 사용 가능하도록 설정합니다", "설정"),
        SelectionCardItem("앱 차단", "특정 시간대에 앱 사용을 차단합니다", "설정"),
    )
    var selected by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("제한 방식을 선택해주세요", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            ColeSelectionCardGroup(
                items = limitOptions,
                selectedIndex = selected,
                onItemSelected = { selected = it },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(
                    text = "다음",
                    onClick = {
                        when (selected) {
                            0 -> onDailyLimitClick()
                            1 -> onTimeSpecifiedClick()
                            else -> onBlockClick()
                        }
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// 일일사용량 제한 플로우 (5화면)
// Figma 304-1507, 258-2392, 304-1760, 241-3409, 243-6318
// ─────────────────────────────────────────────

@Composable
fun AddAppDailyLimitScreen01(
    onNextClick: (minutes: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeSteps = listOf("30분", "1시간", "2시간", "3시간", "4시간")
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("일일 사용 한도를 설정해주세요", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            Text("하루 동안 사용할 수 있는 최대 시간을 선택하세요", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            Text("사용 시간", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeStepBar(steps = timeSteps, selectedIndex = selectedIndex, onStepSelected = { selectedIndex = it })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = { onNextClick(timeSteps[selectedIndex]) })
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen02(
    onNextClick: (warnEnabled: Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var warnAt80 by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("사용량 경고 알림", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            Text("설정한 한도의 80%에 도달하면 알림을 보냅니다.", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            SelectionRow(
                label = "80% 도달 시 알림",
                variant = SelectionRowVariant.Switch,
                switchChecked = warnAt80,
                onSwitchChange = { warnAt80 = it },
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = { onNextClick(warnAt80) })
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen03(
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }
    val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("적용 요일을 선택해주세요", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            Text("선택한 요일에만 일일 사용 한도가 적용됩니다.", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
            ColeChipRow(labels = dayLabels, selectedIndices = selectedDays, onChipClick = { i -> selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "다음", onClick = onNextClick)
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen04(
    limitMinutes: String,
    warnEnabled: Boolean,
    onConfirmClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정 내용을 확인해주세요", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            ColeInfoBox(
                text = "• 일일 사용 한도: $limitMinutes\n• 80% 도달 알림: ${if (warnEnabled) "ON" else "OFF"}",
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "확인", onClick = onConfirmClick)
            }
        }
    }
}

@Composable
fun AddAppDailyLimitScreen05(
    appName: String,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정이 완료되었습니다", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            ColeInfoBox(text = "$appName 앱에 일일 사용량 제한이 적용되었습니다.")
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "완료", onClick = onCompleteClick)
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-02A-03 / AA-03-01 공통: 사용 시간 + 요일 설정
// Figma 241-3341 — AA-02A-03, AA-03-01 재사용
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonTimeScheduleScreen(
    title: String,
    timeSteps: List<String> = listOf("30분", "1시간", "2시간", "3시간", "4시간"),
    initialTimeIndex: Int = 0,
    dayLabels: List<String> = listOf("월", "화", "수", "목", "금", "토", "일"),
    primaryButtonText: String = "다음",
    onPrimaryClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timeIndex by remember { mutableIntStateOf(initialTimeIndex) }
    var selectedDays by remember { mutableStateOf(emptySet<Int>()) }

    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(title, style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            Text("사용 시간", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeStepBar(steps = timeSteps, selectedIndex = timeIndex, onStepSelected = { timeIndex = it })
            Text("적용 요일", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
            ColeChipRow(labels = dayLabels, selectedIndices = selectedDays, onChipClick = { i -> selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i })
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = primaryButtonText, onClick = onPrimaryClick)
            }
        }
    }
}

// ─────────────────────────────────────────────
// AA-03-01 공통: 완료 확인 화면 (Figma 238-2311, 304-1974)
// ─────────────────────────────────────────────

@Composable
fun AddAppCommonConfirmScreen(
    appName: String,
    limitDescription: String,
    onCompleteClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(title = "앱 제한", backIcon = painterResource(R.drawable.ic_back), onBackClick = onBackClick, showNotification = true, modifier = Modifier.fillMaxWidth())
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("설정이 완료되었습니다", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
            ColeInfoBox(text = "$appName 앱에 $limitDescription 가 적용되었습니다.")
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColePrimaryButton(text = "완료", onClick = onCompleteClick)
            }
        }
    }
}

@Composable
fun AddAppFlowHost(
    onComplete: () -> Unit,
    onBackFromFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(AddAppStep.AA_01) }
    var selectedAppName by remember { mutableStateOf("인스타그램") }
    val timeSteps = listOf("30분", "1시간", "2시간", "3시간", "4시간")
    // 일일사용량 제한 플로우 상태
    var dailyLimitMinutes by remember { mutableStateOf("30분") }
    var dailyWarnEnabled by remember { mutableStateOf(true) }

    when (step) {
        AddAppStep.AA_01 -> AddAppScreenAA01(
            onAddAppClick = { step = AddAppStep.AA_02A_01 },
            onBackClick = onBackFromFirst,
        )
        AddAppStep.AA_02A_01 -> AddAppScreenAA02A01(
            onDailyLimitClick = { step = AddAppStep.AA_DAILY_01 },
            onTimeSpecifiedClick = { step = AddAppStep.AA_02A_03 },
            onBlockClick = { step = AddAppStep.AA_02B_02 },
            onBackClick = { step = AddAppStep.AA_01 },
        )
        // 일일사용량 제한 플로우 (5화면)
        AddAppStep.AA_DAILY_01 -> AddAppDailyLimitScreen01(
            onNextClick = { mins -> dailyLimitMinutes = mins; step = AddAppStep.AA_DAILY_02 },
            onBackClick = { step = AddAppStep.AA_02A_01 },
        )
        AddAppStep.AA_DAILY_02 -> AddAppDailyLimitScreen02(
            onNextClick = { enabled -> dailyWarnEnabled = enabled; step = AddAppStep.AA_DAILY_03 },
            onBackClick = { step = AddAppStep.AA_DAILY_01 },
        )
        AddAppStep.AA_DAILY_03 -> AddAppDailyLimitScreen03(
            onNextClick = { step = AddAppStep.AA_DAILY_04 },
            onBackClick = { step = AddAppStep.AA_DAILY_02 },
        )
        AddAppStep.AA_DAILY_04 -> AddAppDailyLimitScreen04(
            limitMinutes = dailyLimitMinutes,
            warnEnabled = dailyWarnEnabled,
            onConfirmClick = { step = AddAppStep.AA_DAILY_05 },
            onBackClick = { step = AddAppStep.AA_DAILY_03 },
        )
        AddAppStep.AA_DAILY_05 -> AddAppDailyLimitScreen05(
            appName = selectedAppName,
            onCompleteClick = onComplete,
            onBackClick = { step = AddAppStep.AA_DAILY_04 },
        )
        // 시간지정제한 플로우
        AddAppStep.AA_02A_02, AddAppStep.AA_02A_03 -> AddAppCommonTimeScheduleScreen(
            title = "사용 시간과 요일을 설정해주세요",
            timeSteps = timeSteps,
            primaryButtonText = "완료",
            onPrimaryClick = { step = AddAppStep.AA_03_01 },
            onBackClick = { step = AddAppStep.AA_02A_01 },
        )
        AddAppStep.AA_02B_02 -> AddAppCommonTimeScheduleScreen(
            title = "차단 시간을 설정해주세요",
            timeSteps = listOf("09:00~12:00", "12:00~18:00", "18:00~22:00", "전체"),
            primaryButtonText = "완료",
            onPrimaryClick = { step = AddAppStep.AA_03_01 },
            onBackClick = { step = AddAppStep.AA_02A_01 },
        )
        AddAppStep.AA_03_01 -> AddAppCommonConfirmScreen(
            appName = selectedAppName,
            limitDescription = "시간지정제한",
            onCompleteClick = onComplete,
            onBackClick = { step = AddAppStep.AA_02A_03 },
        )
    }
}
