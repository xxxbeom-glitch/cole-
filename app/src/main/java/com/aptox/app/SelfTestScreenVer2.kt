package com.aptox.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Ver2 4단계 답변 → 기존 8문항 형식 Map (rawScore/resultType 계산용) */
private fun buildVer2Answers(
    step1: Int?,
    step2: MutableList<Int>,
    step3: Int?,
    step4: Int?,
): Map<Int, Int> {
    val s4 = (step4 ?: 1).coerceIn(0, 3)
    val value = 3 - s4
    return (0..7).associateWith { value }
}

/**
 * 자가테스트 진단 ver2 (Figma 1127-*)
 * - 헤더 없음, 하단 2Line 버튼 (다음/이전, 마지막 페이지 완료/이전)
 * - 상단 100dp, Progress 60dp, Progress↔컨텐츠 26dp, 텍스트↔항목 36dp, 질문↔서브타이틀 8dp, 중앙정렬
 * - Figma 414-7253/7255: Default Primary200/Primary300, Selected Primary400/Primary100
 */
@Composable
fun SelfTestScreenVer2(
    onBack: () -> Unit,
    onComplete: (userName: String, answers: Map<Int, Int>) -> Unit = { _, _ -> onBack() },
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf("") }
    var step1Selected by remember { mutableStateOf<Int?>(null) }
    val step2Selected = remember { mutableStateListOf<Int>() }
    var step3Selected by remember { mutableStateOf<Int?>(null) }
    var step4Selected by remember { mutableStateOf<Int?>(null) }

    val totalSteps = 5
    val progress = (currentStep + 1) / totalSteps.toFloat()

    BackHandler { onBack() }

    val primaryEnabled = when (currentStep) {
        0 -> userName.isNotBlank()
        1 -> step1Selected != null
        2 -> step2Selected.isNotEmpty()
        3 -> step3Selected != null
        4 -> step4Selected != null
        else -> false
    }
    val primaryText = if (currentStep == 4) "완료" else "다음"
    val doPrimaryClick: () -> Unit = {
        when (currentStep) {
            0 -> { if (userName.isNotBlank()) { currentStep += 1 } }
            1, 2, 3 -> currentStep++
            4 -> {
                val answers = buildVer2Answers(step1Selected, step2Selected, step3Selected, step4Selected)
                onComplete(userName.trim(), answers)
            }
            else -> { }
        }
    }
    val doGhostClick: () -> Unit = {
        if (currentStep == 0) onBack() else currentStep--
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AptoxOnboardingProgressBar(progress = progress, modifier = Modifier.width(60.dp))
            }
            Spacer(modifier = Modifier.height(26.dp))

            when (currentStep) {
                0 -> NameStepContent(userName = userName, onNameChange = { userName = it.take(5) })
                1 -> SingleSelectStepContent(
                    question = "평소 스마트폰을 많이\n사용한다고 생각하세요?",
                    options = listOf(
                        "네, 많이 사용하는 것 같아요",
                        "아니요, 적당히 사용하는 것 같아요",
                        "잘 모르겠어요",
                    ),
                    selectedIndex = step1Selected,
                    onOptionSelected = { step1Selected = it },
                )
                2 -> MultiSelectStepContent(
                    question = "가장 많이 사용하는 앱은\n어떤 종류에요?",
                    subtitle = "중복 선택이 가능해요",
                    options = listOf(
                        "SNS (인스타그램, X, 틱톡 등)",
                        "OTT (유튜브, 넷플릭스, 디즈니+ 등)",
                        "게임 (라스트워, 리니지M 등)",
                        "웹툰 (네이버 웹툰, 카카오웹툰, 투믹스 등)",
                        "쇼핑 (쿠팡, 무신사, 에이블리, 29cm 등)",
                    ),
                    selectedIndices = step2Selected,
                    onOptionToggled = { idx ->
                        if (idx in step2Selected) step2Selected.remove(idx)
                        else step2Selected.add(idx)
                    },
                )
                3 -> SingleSelectStepContent(
                    question = "선택하신 앱을 하루동안\n대략 얼마나 사용하시나요?",
                    options = listOf(
                        "1시간 이하",
                        "1시간 이상",
                        "2시간 이상",
                        "3시간 이상",
                        "잘 모르겠어요",
                    ),
                    selectedIndex = step3Selected,
                    onOptionSelected = { step3Selected = it },
                )
                4 -> SingleSelectStepContent(
                    question = "지금 당장 스마트폰을 잃어버린다면\n기분이 어떠실 것 같으세요",
                    options = listOf(
                        "조금 심심할 것 같아요",
                        "조금 짜증나고 불안할 것 같아요",
                        "일이 손에 안잡히고 잠도 안올 것 같아요",
                        "아주 많이 화가 날 것 같아요",
                    ),
                    selectedIndex = step4Selected,
                    onOptionSelected = { step4Selected = it },
                )
            }
        }

        // 하단 고정 버튼 영역 (프로젝트 규칙: bottomPadding 24dp, horizontal 16dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            if (currentStep == 0) {
                AptoxPrimaryButton(
                    text = primaryText,
                    onClick = doPrimaryClick,
                    enabled = primaryEnabled,
                )
            } else {
                AptoxTwoLineButton(
                    primaryText = primaryText,
                    ghostText = "이전",
                    onPrimaryClick = doPrimaryClick,
                    onGhostClick = doGhostClick,
                    enabled = primaryEnabled,
                )
            }
        }
    }
}

@Composable
private fun NameStepContent(
    userName: String,
    onNameChange: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Text(
            text = "만나서 반가워요!\n제가 뭐라고 불러드리면 될까요?",
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
        )
        AptoxTextField(
            value = userName,
            onValueChange = onNameChange,
            placeholder = "이름 또는 닉네임, 최대 5자",
            modifier = Modifier.widthIn(max = 296.dp).fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@Composable
private fun SingleSelectStepContent(
    question: String,
    options: List<String>,
    selectedIndex: Int?,
    onOptionSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Text(
            text = question,
            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 296.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEachIndexed { index, option ->
                SelfTestVer2OptionButton(
                    text = option,
                    selected = index == selectedIndex,
                    onClick = { onOptionSelected(index) },
                )
            }
        }
    }
}

@Composable
private fun SelfTestVer2OptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(shape)
            .background(
                if (selected) AppColors.Primary400
                else AppColors.Primary200
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.BodyRegular.copy(
                color = if (selected) AppColors.Primary100 else AppColors.Primary300,
                textAlign = TextAlign.Center,
            )
        )
    }
}

@Composable
private fun MultiSelectStepContent(
    question: String,
    subtitle: String,
    options: List<String>,
    selectedIndices: List<Int>,
    onOptionToggled: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = question,
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = AppTypography.Caption2.copy(color = AppColors.TextDisclaimer),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().widthIn(max = 296.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEachIndexed { index, option ->
                SelfTestVer2OptionButton(
                    text = option,
                    selected = index in selectedIndices,
                    onClick = { onOptionToggled(index) },
                )
            }
        }
    }
}
