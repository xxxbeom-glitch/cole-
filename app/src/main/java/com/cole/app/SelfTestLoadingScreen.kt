package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * ST-09: 자가테스트 결과 로딩 화면
 * 원형 로딩 이미지가 채워지는 효과 (~1.8초) 후 자동으로 결과 화면으로 이동
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

    Scaffold(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp),
        containerColor = AppColors.SurfaceBackgroundBackground,
        topBar = {
            ColeHeaderSub(
                title = "스마트폰 중독 자가 테스트",
                backIcon = painterResource(id = R.drawable.ic_back),
                onBackClick = null,
                showNotification = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 원형 로딩 (채워지는 효과)
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 12.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // 배경 원
                        drawCircle(
                            color = AppColors.Grey550.copy(alpha = 0.15f),
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )

                        // 채워지는 원 (12시 방향부터 시계 방향)
                        drawArc(
                            color = AppColors.TextHighlight,
                            startAngle = 270f, // 12시부터 시작
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

                    // 중앙 퍼센트 텍스트
                    Text(
                        text = "${(animatedProgress * 100).toInt()}%",
                        style = AppTypography.HeadingH2.copy(color = AppColors.TextHighlight),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "결과를 불러오는 중...",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
