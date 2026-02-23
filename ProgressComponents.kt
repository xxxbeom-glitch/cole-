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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// OnboardingProgressBar (온보딩 진행 바)
// ─────────────────────────────────────────────

/**
 * 온보딩 화면에서 사용하는 심플 프로그레스 바.
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
        targetValue = if (animated) clampedProgress else clampedProgress,
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

// ─────────────────────────────────────────────
// StepBar (스텝 슬라이더)
// ─────────────────────────────────────────────

/**
 * 스텝 눈금이 있는 슬라이더 형태의 프로그레스 바.
 * 드래그 가능한 핸들을 통해 값을 조절할 수 있습니다.
 *
 * @param steps 표시할 스텝 라벨 목록 (예: listOf("30분", "60분", "120분", ...))
 * @param selectedIndex 현재 선택된 스텝 인덱스
 * @param onStepSelected 스텝 선택 시 콜백
 */
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 트랙 + 핸들 레이어
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(steps.size, trackWidthPx) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        if (trackWidthPx > 0f) {
                            val ratio = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            val newIndex = (ratio * steps.lastIndex).roundToInt()
                            onStepSelected(newIndex)
                        }
                    }
                },
        ) {
            // 배경 트랙
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.ChartTrackBackground),
            )

            // 채워진 트랙
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(10.dp)
                    .align(Alignment.CenterStart)
                    .clip(
                        RoundedCornerShape(
                            topStart = 6.dp, bottomStart = 6.dp,
                            topEnd = 2.dp, bottomEnd = 2.dp,
                        )
                    )
                    .background(AppColors.ChartTrackFill),
            )

            // 스텝 점 (dot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(steps.size) { index ->
                    val isActive = index <= clampedIndex
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isActive) Color.Transparent
                                else AppColors.ChartTrackDotInactive
                            ),
                    )
                }
            }

            // 드래그 핸들
            val handleOffsetDp = with(density) {
                val handleWidthPx = 40.dp.toPx()
                val offsetPx = (trackWidthPx * progress - handleWidthPx / 2f)
                    .coerceIn(0f, (trackWidthPx - handleWidthPx).coerceAtLeast(0f))
                offsetPx.toDp()
            }
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(24.dp)
                    .offset(x = handleOffsetDp)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color(0x0F000000),
                        spotColor = Color(0x0F000000),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.ChartHandleBg)
                    .border(1.dp, AppColors.ChartHandleBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 핸들 화살표 아이콘 (텍스트 대체 — 실제 프로젝트에서는 아이콘 리소스 사용)
                    Text(
                        text = "‹",
                        style = AppTypography.Caption1.copy(color = AppColors.ChartHandleArrow),
                    )
                    Text(
                        text = "›",
                        style = AppTypography.Caption1.copy(color = AppColors.ChartHandleArrow),
                    )
                }
            }
        }

        // 스텝 라벨 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            steps.forEach { label ->
                Text(
                    text = label,
                    style = AppTypography.Caption1.copy(
                        color = AppColors.TextCaption,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.width(40.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// LinearProgressBar (일반 선형 프로그레스 바)
// ─────────────────────────────────────────────

/**
 * 일반적인 선형 프로그레스 바.
 *
 * @param progress 0f ~ 1f 사이의 진행률
 * @param trackColor 배경 트랙 색상
 * @param fillColor 채워진 부분 색상
 * @param height 바의 높이
 * @param animated 진행률 변경 시 애니메이션 여부
 */
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(6.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .clip(
                    RoundedCornerShape(
                        topStart = 6.dp, bottomStart = 6.dp,
                        topEnd = 2.dp, bottomEnd = 2.dp,
                    )
                )
                .background(fillColor),
        )
    }
}

// ─────────────────────────────────────────────
// LabeledProgressBar (라벨 포함 프로그레스 바)
// ─────────────────────────────────────────────

/**
 * 제목과 퍼센트 텍스트가 함께 표시되는 프로그레스 바.
 *
 * @param label 좌측 라벨 텍스트
 * @param progress 0f ~ 1f 사이의 진행률
 * @param showPercent 우측에 퍼센트 텍스트 표시 여부
 */
@Composable
fun ColeLabeledProgressBar(
    label: String,
    progress: Float,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    fillColor: Color = AppColors.ChartTrackFill,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            if (showPercent) {
                Text(
                    text = "${(clampedProgress * 100).roundToInt()}%",
                    style = AppTypography.Caption1.copy(color = AppColors.TextHighlight),
                )
            }
        }
        ColeLinearProgressBar(
            progress = clampedProgress,
            fillColor = fillColor,
        )
    }
}
