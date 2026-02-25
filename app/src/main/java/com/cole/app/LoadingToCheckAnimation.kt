package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/** 로딩 → 체크 애니메이션 컬러 */
object LoadingToCheckColors {
    val LoadingCircleBg = Color(0xFFE6F0FF)
    val LoadingDotColor = Color(0xFF5D9CFB)
    val CheckCircleBg = Color(0xFFDBFBD7)
    val CheckMarkColor = Color(0xFF8DEA7E)
}

enum class LoadPhase { WAVE, CONVERGE, CHECK }

private const val WAVE_DURATION_MS = 600
private const val CONVERGE_DURATION_MS = 300
private const val CHECK_DURATION_MS = 250

/**
 * 3 dot 웨이브 → 합쳐짐 → 체크 아이콘 애니메이션.
 * @param onComplete 체크 애니메이션 완료 시 호출 (null이면 호출 안 함)
 */
@Composable
fun LoadingToCheckAnimation(
    modifier: Modifier = Modifier,
    onComplete: (() -> Unit)? = null,
) {
    var phase by remember { mutableStateOf(LoadPhase.WAVE) }
    val convergeProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        phase = LoadPhase.WAVE
        convergeProgress.snapTo(0f)
        checkProgress.snapTo(0f)
        delay(WAVE_DURATION_MS.toLong())
        phase = LoadPhase.CONVERGE
        convergeProgress.animateTo(1f, animationSpec = tween(CONVERGE_DURATION_MS, easing = LinearEasing))
        phase = LoadPhase.CHECK
        checkProgress.animateTo(1f, animationSpec = tween(CHECK_DURATION_MS, easing = LinearEasing))
        delay(300)
        onComplete?.invoke()
    }

    LoadingToCheckAnimationContent(
        modifier = modifier,
        phase = phase,
        convergeProgress = convergeProgress.value,
        checkProgress = checkProgress.value,
    )
}

/**
 * 외부에서 phase/progress를 제어하는 버전 (디버그 테스트용).
 */
@Composable
fun LoadingToCheckAnimationContent(
    phase: LoadPhase,
    convergeProgress: Float,
    checkProgress: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val waveAmpPx = with(density) { 6.dp.toPx() }
    val dotSpacingPx = with(density) { 6.dp.toPx() - 1f }
    val strokeW = with(density) { 10.dp.toPx() - 1f }

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Box(
        modifier = modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(
                when (phase) {
                    LoadPhase.WAVE, LoadPhase.CONVERGE -> LoadingToCheckColors.LoadingCircleBg
                    LoadPhase.CHECK -> LoadingToCheckColors.CheckCircleBg
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (phase) {
            LoadPhase.WAVE -> {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy((dotSpacingPx / density.density).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(0, 1, 2).forEach { i ->
                        val offsetYPx = (sin(2 * PI * wavePhase + i * 2 * PI / 3).toFloat() * waveAmpPx).toInt()
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .offset { IntOffset(0, offsetYPx) }
                                .clip(CircleShape)
                                .background(LoadingToCheckColors.LoadingDotColor),
                        )
                    }
                }
            }
            LoadPhase.CONVERGE -> {
                val baseOffsets = listOf(-1f, 0f, 1f).map { it * dotSpacingPx }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    listOf(0, 1, 2).forEach { i ->
                        val xFrom = baseOffsets[i]
                        val xTo = 0f
                        val xPx = (xFrom + (xTo - xFrom) * convergeProgress).toInt()
                        val yPx = (sin(2 * PI * wavePhase + i * 2 * PI / 3).toFloat() * waveAmpPx * (1f - convergeProgress)).toInt()
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset { IntOffset(xPx, yPx) }
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(LoadingToCheckColors.LoadingDotColor),
                        )
                    }
                }
            }
            LoadPhase.CHECK -> {
                Canvas(modifier = Modifier.size(56.dp).scale(checkProgress)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.82f, h * 0.28f)
                    }
                    drawPath(
                        path = path,
                        color = LoadingToCheckColors.CheckMarkColor,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}
