package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 자가테스트 ST-01 ~ ST-08 (Figma)
// 헤더: 스마트폰 중독 자가 테스트
// 8문항, progress bar, 선택지 4개(매우 그렇다~전혀 그렇지 않다)
// ─────────────────────────────────────────────

private val SelfTestQuestions = listOf(
    "스마트폰 사용시간이\n점점 늘어난다",
    "스마트폰이 없으면\n불안하거나 초조하다",
    "스마트폰 사용을\n줄이려 해도 잘 안 된다",
    "스마트폰을 하느라\n계획된 일을 못 한 적 있다",
    "스마트폰 없으면\n다른 일이 손에 안 잡힌다",
    "볼 게 없어도 습관적으로\n스마트폰을 열게 된다",
    "스마트폰 때문에\n수면에 방해를 받는다",
    "스마트폰 때문에 지인과\n갈등이 생긴 적 있다",
)

private val SelfTestOptions = listOf(
    "매우 그렇다",
    "그렇다",
    "그렇지 않다",
    "전혀 그렇지 않다",
)

// Figma: progress-question 30dp, question-options 40dp, options 12dp
private val ProgressQuestionGap = 30.dp
private val QuestionOptionsGap = 40.dp
private val OptionsGap = 12.dp

@Composable
fun SelfTestScreen(
    onBackClick: () -> Unit,
    onComplete: (Map<Int, Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    val answers = remember { mutableMapOf<Int, Int>() }

    val question = SelfTestQuestions[currentStep]
    val isLastStep = currentStep == SelfTestQuestions.lastIndex

    fun commitAndProceed() {
        selectedOption?.let { idx ->
            answers[currentStep] = idx
            selectedOption = null
            if (isLastStep) {
                onComplete(answers.toMap())
            } else {
                currentStep++
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(
                title = "스마트폰 중독 자가 테스트",
                backIcon = painterResource(id = R.drawable.ic_back),
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
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                ColeOnboardingProgressBar(
                    progress = (currentStep + 1) / SelfTestQuestions.size.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(ProgressQuestionGap + 10.dp))
                Text(
                    text = question,
                    style = AppTypography.Display2.copy(color = AppColors.TextPrimary),
                )
                Spacer(modifier = Modifier.height(QuestionOptionsGap))
                ColeSelfTestButtonGroup(
                    options = SelfTestOptions,
                    selectedIndex = selectedOption,
                    onOptionSelected = { selectedOption = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColePrimaryButton(
                    text = if (isLastStep) "테스트 결과 확인 하기" else "계속 진행",
                    onClick = { commitAndProceed() },
                    enabled = selectedOption != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                ColeGhostButton(
                    text = "돌아가기",
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
