package com.aptox.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** Figma 925-7593 — 시간대별 사용량 막대 차트 공통 상수 */
private val TimeSlotBarChartHeight = 126.dp
private val ChartVerticalPadding = 10.dp
private val TotalChartHeight = TimeSlotBarChartHeight + ChartVerticalPadding * 2
private val TimeSlotBarWidth = 16.dp
private val BarCornerRadius = 2.dp
private val ChartYAxisWidth = 26.dp
private val ChartYAxisToChartGap = 6.dp
private val TimeSlot12BarCount = 12
private val ChartAxisTextStyle = AppTypography.Caption1.copy(color = AppColors.TextCaption)
private val TimeSlotYTicks = listOf(0L, 60L, 120L) // 0, 1H, 2H

private fun formatTimeSlotYLabel(minutes: Long): String = when {
    minutes <= 0 -> "0"
    minutes >= 60 -> "${minutes / 60}H"
    else -> "$minutes"
}

/**
 * 시간대별 사용량 막대 차트 (통계 화면과 동일).
 * 12개 2시간 막대, X축 4개 6시간 라벨, Y축 0/1H/2H 고정.
 *
 * @param values 12개 슬롯별 정규화 값 (0~1). max 2H=120분 기준
 * @param maxValueIdx 최대값 인덱스 (빨간색 강조, -1이면 강조 없음)
 * @param showSpeechBubble true 시 말풍선 표시 (통계 화면용)
 * @param speechBubbleText 말풍선 텍스트 (showSpeechBubble=true일 때)
 */
@Composable
fun TimeSlotBarChartComponent(
    values: List<Float>,
    maxValueIdx: Int,
    showSpeechBubble: Boolean = false,
    speechBubbleText: String? = "스마트폰 사용 최다 시간대",
    modifier: Modifier = Modifier,
) {
    val barCount = TimeSlot12BarCount
    val yTicks = TimeSlotYTicks
    val padded = if (values.size >= barCount) values.take(barCount) else values + List(barCount - values.size) { 0f }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            @Composable
            fun YAxisLabels() {
                val ticks = yTicks.asReversed()
                val labelDensity = LocalDensity.current
                val halfLabelDp = with(labelDensity) {
                    (ChartAxisTextStyle.lineHeight.toPx() / 2f).toDp()
                }
                val denom = ticks.lastIndex.coerceAtLeast(1)
                Box(
                    modifier = Modifier
                        .width(ChartYAxisWidth)
                        .height(TotalChartHeight)
                        .padding(start = 2.dp)
                        .alpha(0.8f)
                        .padding(vertical = ChartVerticalPadding),
                ) {
                    ticks.forEachIndexed { index, tick ->
                        val fraction = index / denom.toFloat()
                        Text(
                            text = formatTimeSlotYLabel(tick),
                            style = ChartAxisTextStyle,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(y = TimeSlotBarChartHeight * fraction - halfLabelDp),
                        )
                    }
                }
            }
            YAxisLabels()
            Spacer(modifier = Modifier.width(ChartYAxisToChartGap))
            BoxWithConstraints(modifier = Modifier.weight(1f).height(TotalChartHeight).padding(vertical = ChartVerticalPadding)) {
                val chartWidthPx = with(density) { maxWidth.roundToPx() }
                val chartHeightPx = with(density) { maxHeight.roundToPx() }
                val barWidthPx = with(density) { TimeSlotBarWidth.roundToPx() }
                val gapPx = if (chartWidthPx > barWidthPx * barCount) (chartWidthPx - barWidthPx * barCount) / (barCount + 1) else 0

                val (barLeftPx, barRightPx) = if (showSpeechBubble && maxValueIdx in 0 until barCount && padded.getOrElse(maxValueIdx) { 0f } > 0f) {
                    val left = gapPx + maxValueIdx * (barWidthPx + gapPx)
                    val right = left + barWidthPx
                    left to right
                } else -1 to -1

                val bubbleWidthPx = with(density) { 180.dp.roundToPx() }
                val speechBubbleGapPx = with(density) { 2.dp.roundToPx() }
                val speechBubbleGapVPx = with(density) { 4.dp.roundToPx() }
                val halfChartPx = chartWidthPx / 2
                val barCenterPx = if (barLeftPx >= 0) (barLeftPx + barRightPx) / 2 else -1
                val showBubbleOnRight = barCenterPx in 0..chartWidthPx && barCenterPx < halfChartPx
                val maxBarValue = padded.getOrElse(maxValueIdx) { 0f }
                val barTopPx = (chartHeightPx * (1f - maxBarValue)).toInt()
                val offsetYPx = barTopPx + speechBubbleGapVPx
                val (tailDirection, align, offsetXPx) = when {
                    barLeftPx < 0 -> Triple(TailDirection.Start, Alignment.TopStart, -1)
                    showBubbleOnRight -> {
                        val leftEdge = barRightPx + speechBubbleGapPx
                        val clamped = leftEdge.coerceIn(0, (chartWidthPx - bubbleWidthPx).coerceAtLeast(0))
                        Triple(TailDirection.Start, Alignment.TopStart, clamped)
                    }
                    else -> {
                        val rightEdge = barLeftPx - speechBubbleGapPx
                        val clamped = rightEdge.coerceIn(bubbleWidthPx, chartWidthPx)
                        Triple(TailDirection.End, Alignment.TopEnd, chartWidthPx - clamped)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 기간별 사용량과 동일: Y틱 개수만큼 가로 점선(맨 아래 바닥선 포함: 0 / 1H / 2H)
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        val denom = (yTicks.size - 1).coerceAtLeast(1)
                        for (i in 0 until yTicks.size) {
                            val y = size.height * (i.toFloat() / denom)
                            drawLine(
                                color = AppColors.Grey450.copy(alpha = 0.6f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                pathEffect = pathEffect,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        (0 until barCount).forEach { idx ->
                            val value = padded.getOrElse(idx) { 0f }
                            val isMax = idx == maxValueIdx && value > 0f
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(TimeSlotBarWidth),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(TimeSlotBarWidth)
                                        .fillMaxHeight(value)
                                        .clip(RoundedCornerShape(BarCornerRadius))
                                        .background(
                                            if (isMax) AppColors.Red300
                                            else AppColors.Grey350
                                        ),
                                )
                            }
                        }
                    }
                    if (showSpeechBubble && offsetXPx >= 0 && speechBubbleText != null) {
                        SpeechBubble(
                            text = speechBubbleText,
                            tailDirection = tailDirection,
                            modifier = Modifier
                                .align(align)
                                .offset {
                                    val x = if (align == Alignment.TopStart) offsetXPx else -offsetXPx
                                    IntOffset(x, offsetYPx)
                                },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ChartYAxisWidth + ChartYAxisToChartGap)
                .alpha(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatisticsData.TimeSlot4SectionLabels.forEach { label ->
                Text(
                    text = label,
                    style = ChartAxisTextStyle,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
