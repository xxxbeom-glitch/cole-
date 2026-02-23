package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 플로우 스텁 - 추후 구현 */
enum class SelfTestResultType { LOW, MIDDLE, HIGH }

fun computeSelfTestResultType(answers: Map<Int, Int>): SelfTestResultType {
    val score = answers.values.sumOf { (4 - it).coerceAtLeast(0) }
    return when {
        score < 5 -> SelfTestResultType.LOW
        score < 10 -> SelfTestResultType.MIDDLE
        else -> SelfTestResultType.HIGH
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Splash", style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        ColePrimaryButton(text = "시작", onClick = onFinish, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun OnboardingScreen(onSkipClick: () -> Unit, onStartClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("온보딩", style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        ColePrimaryButton(text = "시작하기", onClick = onStartClick, modifier = Modifier.fillMaxWidth())
        ColeGhostButton(text = "건너뛰기", onClick = onSkipClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SelfTestScreen(
    onBackClick: () -> Unit,
    onComplete: (Map<Int, Int>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("자가테스트", style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        ColePrimaryButton(
            text = "완료 (스텁)",
            onClick = { onComplete(mapOf(0 to 1, 1 to 2)) },
            modifier = Modifier.fillMaxWidth(),
        )
        ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun SelfTestResultScreen(
    resultType: SelfTestResultType,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    rawScore: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("결과: $resultType (점수:$rawScore)", style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        ColePrimaryButton(text = "시작하기", onClick = onStartClick, modifier = Modifier.fillMaxWidth())
        ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MainFlowHost(onAddAppClick: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("메인", style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary))
        ColePrimaryButton(text = "앱 추가", onClick = onAddAppClick, modifier = Modifier.fillMaxWidth())
        ColeGhostButton(text = "로그아웃", onClick = onLogout, modifier = Modifier.fillMaxWidth())
    }
}
