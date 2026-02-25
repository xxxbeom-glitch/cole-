package com.cole.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 자가테스트 결과 게이지
 * Figma: 아크 208×104, 라인 16px (반원 비율 2:1 고정으로 찌그러짐 방지)
 * 1층: 배경 2층: 아크 3층: 점수+평가 4층: 화살표 5층: 라벨
 */
object ResultGaugeGraph {
    val ContainerWidth: Dp = 328.dp
    val ContainerHeight: Dp = 179.dp
    /** Figma 아크 원본 치수 (반원: width=2*height) */
    val ArcWidth: Dp = 208.dp
    val ArcHeight: Dp = 104.dp
    /** Figma 라인 두께 */
    val ArcStrokeWidth: Dp = 16.dp
}

@Composable
fun ResultGaugeGraph(
    fillProgress: Float,
    displayScore: Int,
    interpretation: String,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = fillProgress.coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .width(ResultGaugeGraph.ContainerWidth)
            .clipToBounds(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 328×179 컨테이너 (Figma 576-2695): 배경 + 게이지 + 점수 + 텍스트 + 화살표 + 라벨
        Box(
            modifier = Modifier
                .width(ResultGaugeGraph.ContainerWidth)
                .height(ResultGaugeGraph.ContainerHeight)
                .background(AppColors.SurfaceBackgroundBackground)
                .clipToBounds(),
        ) {
            // 1층: 배경 (Box 위에서 적용됨)

            // 2층: 게이지 아크 208×104 (Figma, 라인 16px) - aspectRatio(2f)로 반원 비율 강제
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(ResultGaugeGraph.ArcWidth)
                    .aspectRatio(2f),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val actualW = size.width
                    val actualH = size.height
                    val targetRatio = 2f
                    val (arcW, arcH) = if (actualH * targetRatio > actualW) {
                        Pair(actualW, actualW / targetRatio)
                    } else {
                        Pair(actualH * targetRatio, actualH)
                    }
                    val topLeft = Offset((actualW - arcW) / 2, (actualH - arcH) / 2)
                    val strokePx = ResultGaugeGraph.ArcStrokeWidth.toPx()

                    drawArc(
                        color = AppColors.Grey300.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcW, arcH),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }

            // 3층: 점수 + 평가 텍스트 (아크 위 중앙)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$displayScore",
                    style = AppTypography.Display1.copy(color = AppColors.TextPrimary),
                )
                Box(modifier = Modifier.height(4.dp))
                Text(
                    text = interpretation,
                    style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary),
                    textAlign = TextAlign.Center,
                )
            }

            // 4층: 화살표 (208 아크 기준으로 위치)
            if (clampedProgress > 0.02f) {
                val arcLeftOffset = (ResultGaugeGraph.ContainerWidth - ResultGaugeGraph.ArcWidth) / 2
                val arrowCenterDp = ResultGaugeGraph.ArcWidth * clampedProgress
                val arrowStartDp = (arcLeftOffset + (arrowCenterDp - 10.dp)).coerceIn(
                    arcLeftOffset,
                    ResultGaugeGraph.ContainerWidth - 20.dp,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = arrowStartDp)
                            .size(20.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_result_gauge_arrow),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }

            // 5층: 라벨 250, 350, 500 (아크 하단)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(ResultGaugeGraph.ArcWidth)
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("250", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                Text("350", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                Text("500", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
            }
        }
    }
}
