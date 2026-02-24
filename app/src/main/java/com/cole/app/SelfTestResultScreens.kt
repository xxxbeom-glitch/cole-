package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// ST-10 결과 화면: 게이지 + 점수 + 평가 텍스트 + 버튼만
// ─────────────────────────────────────────────

fun rawScoreToDisplayScore(rawScore: Int): Int =
    (250 + rawScore * 250f / 32f).roundToInt().coerceIn(250, 500)

fun displayScoreToInterpretation(displayScore: Int): String = when {
    displayScore < 334 -> "스마트폰 사용 습관 점검이 필요해요"
    displayScore < 417 -> "스마트폰 사용에 주의가 필요해요"
    else -> "스마트폰 사용 습관이 건강해요!"
}

@Composable
fun SelfTestResultScreen(
    resultType: SelfTestResultType,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    rawScore: Int,
    userName: String = "장원영",
    modifier: Modifier = Modifier,
) {
    val displayScore = rawScoreToDisplayScore(rawScore)
    val interpretation = displayScoreToInterpretation(displayScore)
    val fillProgress = ((displayScore - 250) / 250f).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = fillProgress,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_fill",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ResultGaugeGraph(
                fillProgress = animatedFill,
                displayScore = displayScore,
                interpretation = interpretation,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "확인",
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(
                text = "다시하기",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
