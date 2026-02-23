package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 온보딩/자가테스트 화면에서 사용하는 프로그레스 바.
 *
 * @param progress 0f ~ 1f 사이의 진행률
 * @param animated 진행률 변경 시 애니메이션 여부
 */
@Composable
fun ColeOnboardingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = if (animated) 300 else 0),
        label = "onboarding_progress",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.Grey550.copy(alpha = 0.1f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(AppColors.TextHighlight),
        )
    }
}

@Composable
fun ColeStepBar(
    steps: List<String>,
    selectedIndex: Int,
    onStepSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    val clampedIndex = selectedIndex.coerceIn(0, steps.lastIndex)
    val progress = if (steps.size > 1) clampedIndex.toFloat() / steps.lastIndex.toFloat() else 0f
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { size -> trackWidthPx = size.width.toFloat() }
                .pointerInput(steps.size, trackWidthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        if (trackWidthPx > 0f) {
                            val ratio = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            onStepSelected((ratio * steps.lastIndex).roundToInt())
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.ChartTrackBackground),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 2.dp, bottomEnd = 2.dp))
                    .background(AppColors.ChartTrackFill),
            )
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(steps.size) { index ->
                    val isActive = index <= clampedIndex
                    Box(modifier = Modifier.size(4.dp).clip(RoundedCornerShape(999.dp)).background(if (isActive) Color.Transparent else AppColors.ChartTrackDotInactive))
                }
            }
            val handleOffsetDp = with(density) {
                val handleWidthPx = 40.dp.toPx()
                (trackWidthPx * progress - handleWidthPx / 2f).coerceIn(0f, (trackWidthPx - handleWidthPx).coerceAtLeast(0f)).toDp()
            }
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(24.dp)
                    .offset(x = handleOffsetDp)
                    .shadow(3.dp, RoundedCornerShape(12.dp), ambientColor = Color(0x0F000000), spotColor = Color(0x0F000000))
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.ChartHandleBg)
                    .border(1.dp, AppColors.ChartHandleBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("‹", style = AppTypography.Caption1.copy(color = AppColors.ChartHandleArrow))
                    Text("›", style = AppTypography.Caption1.copy(color = AppColors.ChartHandleArrow))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            steps.forEach { Text(it, style = AppTypography.Caption1.copy(color = AppColors.TextCaption, textAlign = TextAlign.Center), modifier = Modifier.weight(1f)) }
        }
    }
}

@Composable
fun ColeLinearProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = AppColors.ChartTrackBackground,
    fillColor: Color = AppColors.ChartTrackFill,
    height: androidx.compose.ui.unit.Dp = 10.dp,
    animated: Boolean = true,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = if (animated) 300 else 0),
        label = "linear_progress",
    )
    Box(modifier = modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(6.dp)).background(trackColor)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 2.dp, bottomEnd = 2.dp))
                .background(fillColor),
        )
    }
}

@Composable
fun ColeLabeledProgressBar(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    fillColor: Color = AppColors.ChartTrackFill,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
            if (showPercent) Text("${(clampedProgress * 100).roundToInt()}%", style = AppTypography.Caption1.copy(color = AppColors.TextHighlight))
        }
        ColeLinearProgressBar(progress = clampedProgress, fillColor = fillColor)
    }
}
