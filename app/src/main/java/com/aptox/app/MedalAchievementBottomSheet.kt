package com.aptox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aptox.app.model.BadgeDefinition
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 메달 획득 시 표시되는 풀페이지 오버레이 (Figma 1033-10786)
 * ModalBottomSheet → Dialog 풀페이지로 변경
 *
 * 디자인:
 * - 배경: 검정 (전체 화면)
 * - 중앙: 메달 아이콘 (184dp 마스크 그룹) + 애니메이션
 * - 텍스트: "챌린지 성공!" (Display3 26sp ExtraBold 흰색)
 * - 뱃지 이름 (HeadingH3 18sp 흰색 90% opacity)
 * - 조건 텍스트 (BodyMedium 15sp 흰색 90% opacity)
 * - 하단 버튼: "확인" (Primary300 배경, 흰색 Bold 16sp)
 */
@Composable
fun MedalAchievementBottomSheet(
    badge: BadgeDefinition,
    animationType: MedalAnimationType = MedalAnimationType.COMBINED,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .windowInsetsPadding(WindowInsets.navigationBars),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(80.dp))

                // 메달 아이콘 + 애니메이션
                MedalAnimatedIcon(
                    badge = badge,
                    animationType = animationType,
                    size = 166.dp,
                )

                // 텍스트 영역
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "챌린지 성공!",
                        style = AppTypography.Display3.copy(color = Color.White),
                        textAlign = TextAlign.Center,
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = badge.title,
                            style = AppTypography.HeadingH3.copy(
                                color = Color.White.copy(alpha = 0.9f),
                            ),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = badge.condition,
                            style = AppTypography.BodyMedium.copy(
                                color = Color.White.copy(alpha = 0.9f),
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))

                // 확인 버튼
                AptoxPrimaryButton(
                    text = "확인",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

/**
 * 메달 아이콘 + 선택된 애니메이션 효과
 */
@Composable
fun MedalAnimatedIcon(
    badge: BadgeDefinition,
    animationType: MedalAnimationType,
    size: androidx.compose.ui.unit.Dp = 166.dp,
    autoPlay: Boolean = true,
) {
    val scaleAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }
    val sparkleProgress = remember { Animatable(0f) }
    val particleProgress = remember { Animatable(0f) }
    val density = LocalDensity.current.density

    // SHIMMER: 무한 반복 sweep
    val shimmerTransition: InfiniteTransition? = if (
        animationType == MedalAnimationType.SHIMMER || animationType == MedalAnimationType.COMBINED
    ) rememberInfiniteTransition(label = "shimmer") else null
    val shimmerOffset by (shimmerTransition?.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "shimmerOffset",
    ) ?: androidx.compose.runtime.mutableFloatStateOf(0f))

    // CONFETTI: 무한 반복 낙하
    val confettiTransition: InfiniteTransition? = if (
        animationType == MedalAnimationType.CONFETTI || animationType == MedalAnimationType.COMBINED
    ) rememberInfiniteTransition(label = "confetti") else null
    val confettiProgress by (confettiTransition?.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "confettiProgress",
    ) ?: androidx.compose.runtime.mutableFloatStateOf(0f))

    LaunchedEffect(autoPlay) {
        if (!autoPlay) return@LaunchedEffect
        when (animationType) {
            MedalAnimationType.SCALE_BOUNCE -> {
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(stiffness = 300f, dampingRatio = 0.4f),
                )
            }
            MedalAnimationType.PARTICLES -> {
                scaleAnim.snapTo(1f)
                particleProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                )
            }
            MedalAnimationType.SHIMMER -> {
                scaleAnim.snapTo(1f)
            }
            MedalAnimationType.CONFETTI -> {
                scaleAnim.snapTo(1f)
            }
            MedalAnimationType.COMBINED -> {
                // 1. scale bounce 등장
                scaleAnim.animateTo(
                    targetValue = 1.15f,
                    animationSpec = spring(stiffness = 300f, dampingRatio = 0.4f),
                )
                // 2. 회전
                launch {
                    rotationAnim.animateTo(
                        targetValue = 720f,
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                    )
                }
                // 3. 파티클 방사
                launch {
                    particleProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing),
                    )
                }
                // 4. scale 복귀
                kotlinx.coroutines.delay(500)
                scaleAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(150, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // CONFETTI 레이어 (뒤)
        if (animationType == MedalAnimationType.CONFETTI || animationType == MedalAnimationType.COMBINED) {
            ConfettiCanvas(
                progress = confettiProgress,
                modifier = Modifier.size(size * 2f),
            )
        }

        // 메달 아이콘
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    val s = if (animationType == MedalAnimationType.SCALE_BOUNCE ||
                        animationType == MedalAnimationType.COMBINED) scaleAnim.value else 1f
                    scaleX = s
                    scaleY = s
                    rotationY = if (animationType == MedalAnimationType.COMBINED) rotationAnim.value else 0f
                    transformOrigin = TransformOrigin.Center
                    cameraDistance = (8f * density).coerceAtLeast(1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(badge.iconResId),
                contentDescription = badge.title,
                modifier = Modifier.size(size),
                contentScale = ContentScale.Fit,
            )

            // SHIMMER 레이어
            if (animationType == MedalAnimationType.SHIMMER || animationType == MedalAnimationType.COMBINED) {
                Canvas(modifier = Modifier.size(size)) {
                    val w = this.size.width
                    val stripW = w * 0.35f
                    val left = (shimmerOffset * w).coerceIn(-stripW, w)
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                            startX = left,
                            endX = left + stripW,
                        ),
                    )
                }
            }
        }

        // PARTICLES 레이어 (앞)
        if (animationType == MedalAnimationType.PARTICLES || animationType == MedalAnimationType.COMBINED) {
            ParticlesCanvas(
                progress = particleProgress.value,
                modifier = Modifier.size(size * 2f),
            )
        }
    }
}

/** 파티클 8개 방사형 확산 */
@Composable
private fun ParticlesCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val particleColors = listOf(
        AppColors.Primary300,
        Color(0xFFFFD700),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        AppColors.Primary300,
        Color(0xFFFFD700),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
    )
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = 80.dp.toPx()
        val alpha = (1f - progress).coerceIn(0f, 1f)
        val radius = progress * maxRadius

        for (i in 0..7) {
            val angle = (45.0 * i) * PI / 180.0
            val x = cx + radius * cos(angle).toFloat()
            val y = cy + radius * sin(angle).toFloat()
            drawCircle(
                color = particleColors[i].copy(alpha = alpha),
                radius = 6.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

/** 색종이 낙하 (50개) */
@Composable
private fun ConfettiCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val confettiColors = listOf(
        AppColors.Primary300,
        Color(0xFFFFD700),
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFF9F43),
        Color(0xFFA29BFE),
    )
    // 50개 색종이 고정 시드 (재구성 방지)
    val particles = remember {
        List(50) { i ->
            val seed = i * 7919L
            val rng = java.util.Random(seed)
            ConfettiParticle(
                xFraction = rng.nextFloat(),
                speedFraction = 0.5f + rng.nextFloat() * 0.5f,
                colorIndex = i % confettiColors.size,
                rotationSpeed = rng.nextFloat() * 360f,
                phase = rng.nextFloat(),
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val t = ((progress + p.phase) % 1f)
            val y = t * (h + 20.dp.toPx()) - 10.dp.toPx()
            val x = p.xFraction * w
            val rotation = t * p.rotationSpeed
            val color = confettiColors[p.colorIndex]
            rotate(rotation, pivot = Offset(x, y)) {
                drawRect(
                    color = color,
                    topLeft = Offset(x - 3.dp.toPx(), y - 6.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 12.dp.toPx()),
                )
            }
        }
    }
}

private data class ConfettiParticle(
    val xFraction: Float,
    val speedFraction: Float,
    val colorIndex: Int,
    val rotationSpeed: Float,
    val phase: Float,
)

/**
 * 메달 획득 애니메이션 종류
 */
enum class MedalAnimationType {
    /** 메달 주변 shimmer 광택 sweep (무한 반복) */
    SHIMMER,
    /** scale 0→1.2→1.0 bouncy 등장 */
    SCALE_BOUNCE,
    /** 주변 별/빛 파티클 8개 방사형 확산 */
    PARTICLES,
    /** 화면 상단에서 색종이 낙하 */
    CONFETTI,
    /** 위 전부 합친 풀버전 */
    COMBINED,
}
