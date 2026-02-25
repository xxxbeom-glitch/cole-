package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val WAVE_DURATION_MS = 600
private const val CONVERGE_DURATION_MS = 300
private const val CHECK_DURATION_MS = 250

@Composable
fun DebugLoadingAnimationTestScreen(onBack: () -> Unit) {
    var phase by remember { mutableStateOf(LoadPhase.WAVE) }
    var replayKey by remember { mutableStateOf(0) }
    val convergeProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(replayKey) {
        phase = LoadPhase.WAVE
        convergeProgress.snapTo(0f)
        checkProgress.snapTo(0f)
        delay(WAVE_DURATION_MS.toLong())
        phase = LoadPhase.CONVERGE
        convergeProgress.animateTo(1f, animationSpec = tween(CONVERGE_DURATION_MS, easing = LinearEasing))
        phase = LoadPhase.CHECK
        checkProgress.animateTo(1f, animationSpec = tween(CHECK_DURATION_MS, easing = LinearEasing))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ColeGhostButton(text = "돌아가기", onClick = onBack)
        }

        Text(
            text = "로딩 애니메이션 테스트",
            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(24.dp)
                .clickable { replayKey++ },
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            LoadingToCheckAnimationContent(
                phase = phase,
                convergeProgress = convergeProgress.value,
                checkProgress = checkProgress.value,
            )
        }
    }
}
