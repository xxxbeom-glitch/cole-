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

/** 리커트 선택 인덱스(0=전혀 그렇지 않다 … 3=매우 그렇다) → 저장값 (기존 (4-it) 합산과 호환) */
private fun likertIndexToStoredValue(index: Int): Int = 3 - index.coerceIn(0, 3)

private fun buildUsagePatternAnswerMap(likertAnswers: List<Int?>): Map<Int, Int> =
    likertAnswers.mapIndexedNotNull { idx, sel ->
        if (sel == null) null else idx to likertIndexToStoredValue(sel)
    }.toMap()

private val UsagePatternLikertOptions = listOf(
    "전혀 그렇지 않다",
    "그렇지 않다",
    "그렇다",
    "매우 그렇다",
)

private val UsagePatternQuestions = listOf(
    "스마트폰 사용을 줄여야겠다고\n느끼지만 잘 안 된다",
    "스마트폰 없으면\n불안하거나 초조하다",
    "스마트폰 때문에\n수면·식사 등 일상이 방해받는다",
    "일/공부 중에도\n습관적으로 스마트폰을 확인한다",
    "스마트폰이 없으면\n뭘 해야 할지 모르겠다",
    "자기 전이나 일어나자마자\n스마트폰부터 확인한다",
    "스마트폰 사용 시간이\n생각보다 훨씬 길게 느껴진다",
)

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
    val likertAnswers = remember { mutableStateListOf<Int?>(null, null, null, null, null, null, null) }

    val totalSteps = 8 // 이름 + 리커트 7문항
    val progress = (currentStep + 1) / totalSteps.toFloat()

    BackHandler { onBack() }

    val primaryEnabled = when (currentStep) {
        0 -> userName.isNotBlank()
        in 1..7 -> likertAnswers[currentStep - 1] != null
        else -> false
    }
    val primaryText = if (currentStep == 7) "완료" else "다음"
    val doPrimaryClick: () -> Unit = {
        when (currentStep) {
            0 -> { if (userName.isNotBlank()) { currentStep += 1 } }
            in 1..6 -> currentStep++
            7 -> {
                val answers = buildUsagePatternAnswerMap(likertAnswers)
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
                in 1..7 -> {
                    val qIdx = currentStep - 1
                    SingleSelectStepContent(
                        question = UsagePatternQuestions[qIdx],
                        options = UsagePatternLikertOptions,
                        selectedIndex = likertAnswers[qIdx],
                        onOptionSelected = { likertAnswers[qIdx] = it },
                    )
                }
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
