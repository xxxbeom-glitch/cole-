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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * 5등분 세그먼트 게이지 (Figma 658:2777)
 * - Track: 회색 (#E8E8E8)
 * - Fill: 5색 (빨강→주황→노랑→파랑→민트)
 */
@Composable
fun SegmentedGaugeBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val segmentColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFFFF8D5D),
        Color(0xFFFFD43F),
        Color(0xFF67D0F5),
        Color(0xFF56E3D0),
    )
    val trackColor = Color(0xFFE8E8E8)

    Row(
        modifier = modifier
            .width(300.dp)
            .height(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(5) { i ->
            val segmentProgress = (clamped * 5) - i
            val fillFraction = segmentProgress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(trackColor),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (fillFraction > 0.001f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(segmentColors[i]),
                    )
                }
            }
        }
    }
}

/**
 * 전체 원형 게이지
 * - 트랙: 연한 회색 전체 원
 * - Fill: 상단(12시)에서 시계방향, 4색 균등 그라데이션 (FF6B6B → FFD93D → 6BCBFF → 4EEEBD)
 */
@Composable
fun SimpleFullCircleGauge(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)

    Box(modifier = modifier.size(180.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 12f
            val pad = strokePx + 4f
            val diameter = minOf(size.width, size.height) - pad * 2
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcCenter = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)

            // Track: 전체 원 (연한 회색)
            drawArc(
                color = Color(0xFFE8E8E8),
                startAngle = 270f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Fill: 4색 균등 그라데이션
            if (clamped > 0.001f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        center = arcCenter,
                        colors = listOf(
                            Color(0xFFFF6B6B),
                            Color(0xFFFFD93D),
                            Color(0xFF6BCBFF),
                            Color(0xFF4EEEBD),
                        ),
                    ),
                    startAngle = 270f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/**
 * 단순 반원 게이지 (디버그 테스트용)
 * track + fill만 그림. 점수/코멘트/라벨 없음.
 * 임의의 반원 track, 임의의 fill 컬러.
 */
@Composable
fun SimpleSemiCircleGauge(
    fillProgress: Float,
    modifier: Modifier = Modifier,
) {
    val clampedProgress = fillProgress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(240.dp)
            .aspectRatio(2f)
            .background(Color(0xFFF5F5F5))
            .padding(20.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14f
            val pad = strokePx + 4f
            // 반원: arcW=2*arcH, stroke 여유 확보
            val maxW = size.width - pad * 2
            val maxH = size.height - pad * 2
            val arcW = minOf(maxW, maxH * 2f).coerceAtLeast(1f)
            val arcH = (arcW / 2f).coerceAtLeast(1f)
            val topLeft = Offset(
                (size.width - arcW) / 2f,
                (size.height - arcH) / 2f
            )

            // Track: 임의 반원 (연한 회색)
            drawArc(
                color = Color(0xFFD0D0D0),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcW, arcH),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // Fill: progress만큼 (임의 컬러 - 블루 계열)
            if (clampedProgress > 0.001f) {
                val fillSweep = 180f * clampedProgress
                drawArc(
                    color = Color(0xFF4A90D9),
                    startAngle = 180f,
                    sweepAngle = fillSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcW, arcH),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
    }
}

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
                    val arcCenter = Offset(topLeft.x + arcW / 2, topLeft.y + arcH)

                    // 1) 트랙: 전체 반원 (밝은 회색)
                    drawArc(
                        color = AppColors.Grey300.copy(alpha = 0.6f),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(arcW, arcH),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )

                    // 2) Fill: progress만큼 채워지는 반원 (임의 그라데이션: mint→blue→orange→red)
                    if (clampedProgress > 0.001f) {
                        val fillSweep = 180f * clampedProgress
                        drawArc(
                            brush = Brush.sweepGradient(
                                center = arcCenter,
                                colors = listOf(
                                    Color(0xFF7DD3C0),  // mint/teal
                                    AppColors.Blue300,
                                    AppColors.Orange300,
                                    AppColors.Red300,
                                ),
                            ),
                            startAngle = 180f,
                            sweepAngle = fillSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(arcW, arcH),
                            style = Stroke(width = strokePx, cap = StrokeCap.Round),
                        )
                    }
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
