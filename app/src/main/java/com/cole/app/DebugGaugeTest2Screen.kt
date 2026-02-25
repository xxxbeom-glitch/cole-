package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 게이지테스트 2: 5등분 세그먼트 게이지 (Figma 658:2777)
 * 회색 track + 컬러 fill
 */
@Composable
fun DebugGaugeTest2Screen(onBack: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0.5f) }
    var animationEnabled by remember { mutableStateOf(true) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (animationEnabled) tween(durationMillis = 400) else tween(0),
        label = "gauge2_fill",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            ColeGhostButton(text = "돌아가기", onClick = onBack)
        }

        Text(
            text = "게이지테스트 2",
            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        SegmentedGaugeBar(progress = animatedProgress)

        Text(
            text = "progress: ${"%.2f".format(animatedProgress)}",
            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            modifier = Modifier.padding(top = 16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "progress 조정 (0 ~ 1)",
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Slider(
                value = progress,
                onValueChange = { progress = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColeOutlinedTextButton(
                text = "0%",
                onClick = { progress = 0f },
                modifier = Modifier.weight(1f),
            )
            ColeOutlinedTextButton(
                text = "50%",
                onClick = { progress = 0.5f },
                modifier = Modifier.weight(1f),
            )
            ColeOutlinedTextButton(
                text = "100%",
                onClick = { progress = 1f },
                modifier = Modifier.weight(1f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "애니메이션: ${if (animationEnabled) "ON" else "OFF"}",
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ColeOutlinedTextButton(
                    text = "ON",
                    onClick = { animationEnabled = true },
                    modifier = Modifier.weight(1f),
                )
                ColeOutlinedTextButton(
                    text = "OFF",
                    onClick = { animationEnabled = false },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
