package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * ST-09: 자가테스트 결과 로딩 화면 (Figma 스크린샷 기준)
 * - 잠시만 기다려주세요 / 작성하신 자가진단표를 분석중입니다
 * - 원형 진행률 (화면 45%, 파란색)
 * - 하단 인용문
 */
@Composable
fun SelfTestLoadingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        progress = 1f
        delay(1800)
        onFinish()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1800),
        label = "circular_progress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            val screenWidth = maxWidth
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 상단: 잠시만 기다려주세요
                Text(
                    text = "잠시만 기다려주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    textAlign = TextAlign.Center,
                )

                // 메인: 작성하신 자가진단표를 분석중입니다 + 원형 진행률
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Text(
                        text = "작성하신 자가진단표를\n분석중입니다",
                        style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                        textAlign = TextAlign.Center,
                    )
                    // 원형 진행률: 화면 너비의 45% (Figma)
                    val circleSize = (screenWidth * 0.45f).coerceAtLeast(120.dp).coerceAtMost(200.dp)
                    Box(
                        modifier = Modifier.size(circleSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 12.dp.toPx()
                            drawCircle(
                                color = AppColors.Grey300.copy(alpha = 0.5f),
                                radius = (size.minDimension - strokeWidth) / 2,
                                center = Offset(size.width / 2, size.height / 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = AppColors.Blue300,
                                startAngle = 270f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                size = androidx.compose.ui.geometry.Size(
                                    size.width - strokeWidth,
                                    size.height - strokeWidth,
                                ),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            )
                        }
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = AppTypography.HeadingH2.copy(color = AppColors.Blue300),
                        )
                    }
                }

                // 하단: 인용문 + 출처
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "SNS는 인생의 낭비다. 그 시간에 책을 읽어라.",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "알렉스 퍼거슨 (Sir Alex Ferguson)",
                        style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
