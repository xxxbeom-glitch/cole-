package com.aptox.app

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
import kotlin.math.roundToInt

/**
 * 게이지 테스트 화면 (디버그 전용)
 * ResultGaugeGraph를 독립적으로 테스트하기 위한 화면.
 * 슬라이더로 displayScore(250~500) 조정 → fillProgress/interpretation 자동 계산.
 * 애니메이션 on/off 전환 가능.
 */
@Composable
fun DebugGaugeTestScreen(onBack: () -> Unit) {
    var rawScore by remember { mutableFloatStateOf(16f) } // 7..28 (7문항 합계)
    var animationEnabled by remember { mutableStateOf(true) }

    val displayScore = rawScoreToDisplayScore(rawScore.roundToInt(), answerCount = 7)
    val interpretation = displayScoreToInterpretation(displayScore)
    val targetProgress = ((displayScore - 250) / 250f).coerceIn(0f, 1f)

    val fillProgress by animateFloatAsState(
        targetValue = if (animationEnabled) targetProgress else targetProgress,
        animationSpec = if (animationEnabled) tween(durationMillis = 600) else tween(0),
        label = "debug_gauge_fill",
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
        // 상단: 뒤로가기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            AptoxGhostButton(text = "돌아가기", onClick = onBack)
        }

        // 제목
        Text(
            text = "게이지 테스트",
            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        // 게이지 본체 (전체 원형, 4색 그라데이션)
        SimpleFullCircleGauge(progress = fillProgress)

        // 수치 정보 표시
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "rawScore: ${rawScore.roundToInt()} / 28",
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            Text(
                text = "displayScore: $displayScore  (range 250~500)",
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            Text(
                text = "fillProgress: ${"%.3f".format(targetProgress)}",
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            Text(
                text = "interpretation: $interpretation",
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
        }

        // 슬라이더 (rawScore 7..28, 7문항 합계)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "rawScore 조정 (7 ~ 28)",
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Slider(
                value = rawScore,
                onValueChange = { rawScore = it },
                valueRange = 7f..28f,
                steps = 20,
                modifier = Modifier.fillMaxWidth(),
            )
            // 구간 라벨
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("LOW (7)", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                Text("MID (17)", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                Text("HIGH (28)", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
            }
        }

        // 프리셋 버튼 (LOW / MID / HIGH 대표값)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "프리셋",
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AptoxOutlinedTextButton(
                    text = "LOW (7)",
                    onClick = { rawScore = 7f },
                    modifier = Modifier.weight(1f),
                )
                AptoxOutlinedTextButton(
                    text = "MID (17)",
                    onClick = { rawScore = 17f },
                    modifier = Modifier.weight(1f),
                )
                AptoxOutlinedTextButton(
                    text = "HIGH (28)",
                    onClick = { rawScore = 28f },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 애니메이션 토글
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "애니메이션: ${if (animationEnabled) "ON (600ms)" else "OFF (즉시)"}",
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AptoxOutlinedTextButton(
                    text = "ON",
                    onClick = { animationEnabled = true },
                    modifier = Modifier.weight(1f),
                )
                AptoxOutlinedTextButton(
                    text = "OFF",
                    onClick = { animationEnabled = false },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
