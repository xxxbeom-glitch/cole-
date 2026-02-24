package com.cole.app

import com.cole.app.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
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
    val atEnd = clampedIndex == steps.lastIndex
    // Figma: 각 segment 40px 균등분배. segment 중심 = (i + 0.5) / n
    val segmentCenterFraction = if (steps.size > 1) (clampedIndex + 0.5f) / steps.size else 0.5f
    val fillProgress = if (atEnd) 1f else segmentCenterFraction
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size -> trackWidthPx = size.width.toFloat() }
            .pointerInput(steps.size, trackWidthPx) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (trackWidthPx > 0f) {
                        val ratio = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                        val segment = (ratio * steps.size).toInt().coerceIn(0, steps.lastIndex)
                        onStepSelected(segment)
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // track + dot을 같은 영역에 겹쳐서 배치 (dot이 track 위에 올라감)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
        ) {
            // track과 fill을 하나의 클립된 Box 안에 겹쳐서
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(6.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.ChartTrackBackground),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillProgress)
                        .fillMaxHeight()
                        .clip(
                            RoundedCornerShape(
                                topStart = 6.dp,
                                bottomStart = 6.dp,
                                topEnd = if (atEnd) 6.dp else 2.dp,
                                bottomEnd = if (atEnd) 6.dp else 2.dp,
                            )
                        )
                        .background(AppColors.ChartTrackFill),
                )
            }
            // dot을 track과 동일한 세로 위치에 겹쳐서 배치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(steps.size) { index ->
                    val isActive = index <= clampedIndex
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isActive) AppColors.ChartTrackDotActive
                                    else AppColors.ChartTrackDotInactive
                                ),
                        )
                    }
                }
            }
            // 핸들: 40dp, dot·라벨과 동일한 segment 중심에 정렬 (각 40px 영역 균등분배)
            val handleWidthDp = 40.dp
            val handleOffsetDp = with(density) {
                val handleWidthPx = handleWidthDp.toPx()
                val centerX = trackWidthPx * segmentCenterFraction - handleWidthPx / 2f
                centerX.coerceIn(0f, (trackWidthPx - handleWidthPx).coerceAtLeast(0f)).toDp()
            }
            Box(
                modifier = Modifier
                    .width(handleWidthDp)
                    .height(24.dp)
                    .offset(x = handleOffsetDp)
                    .align(Alignment.CenterStart)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.06f),
                    ),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_stepbar_handle),
                    contentDescription = "슬라이더 핸들",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // 하단: 라벨을 dot과 동일한 grid로 중앙정렬
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { label ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextCaption, textAlign = TextAlign.Center),
                    )
                }
            }
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
