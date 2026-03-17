package com.aptox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.aptox.app.R
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import com.aptox.app.model.BadgeMasterData

/**
 * 메달 애니메이션 테스트 화면
 * - 버전1: 360도 Y축 회전
 * - 버전2: 180도 앞뒤 뒤집기
 * - 획득 바텀시트 애니메이션 프리뷰 (MedalAnimationType 5가지)
 */
@Composable
fun MedalAnimationTestScreen(onBack: () -> Unit) {
    val dummyBadge = BadgeMasterData.badges.getOrNull(3)
        ?: BadgeMasterData.badges.first()

    // 바텀시트 프리뷰 상태
    var previewAnimationType by remember { mutableStateOf<MedalAnimationType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        AptoxGhostButton(text = "← 돌아가기", onClick = onBack)
        Text(
            text = "메달 애니메이션 테스트",
            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
        )

        // ── 메달 획득 바텀시트 애니메이션 프리뷰 ──
        MedalAnimationSectionTitle("메달 획득 바텀시트 애니메이션")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MedalAnimationType.entries.forEach { type ->
                AptoxSecondaryButton(
                    text = when (type) {
                        MedalAnimationType.SHIMMER -> "SHIMMER 미리보기"
                        MedalAnimationType.SCALE_BOUNCE -> "SCALE_BOUNCE 미리보기"
                        MedalAnimationType.PARTICLES -> "PARTICLES 미리보기"
                        MedalAnimationType.CONFETTI -> "CONFETTI 미리보기"
                        MedalAnimationType.COMBINED -> "COMBINED (풀버전) 미리보기"
                    },
                    onClick = { previewAnimationType = type },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // 버전1: 360도 회전 (2바퀴, 속도별 5케이스)
        MedalAnimationSectionTitle("버전 1 - 2바퀴 회전 (속도별)")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(150, 200, 250, 300, 350).forEach { durationMs ->
                MedalSpin360Demo(
                    durationMs = durationMs,
                    rotationPerTap = 2,
                )
            }
            MedalSpin360Demo(
                durationMs = 550,
                rotationPerTap = 3,
            )
        }

        // 버전1 확장: 탭 → 튀어나옴 → 그때 회전
        MedalAnimationSectionTitle("버전 1 확장 - 튀어나온 뒤 회전")
        MedalPopThenSpinDemo()

        // 버전1 확장 + 반짝임 5가지
        MedalAnimationSectionTitle("버전 1 확장 + 착지 반짝임 (5가지)")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            MedalPopThenSpinWithSparkle(SparkleType.GRADIENT_SWEEP)
            MedalPopThenSpinWithSparkle(SparkleType.ALPHA_FLASH)
            MedalPopThenSpinWithSparkle(SparkleType.SCALE_PULSE)
            MedalPopThenSpinWithSparkle(SparkleType.COLOR_FILTER)
            MedalPopThenSpinWithSparkle(SparkleType.SPARKLE_DOTS)
        }

        // 버전1 확장 + 그라데이션 스윕 (속도별 7가지)
        MedalAnimationSectionTitle("버전 1 확장 + 그라데이션 스윕 (속도별)")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(200, 250, 300, 350, 400, 450, 500).forEach { durationMs ->
                MedalPopThenSpinWithSparkle(
                    sparkleType = SparkleType.GRADIENT_SWEEP,
                    sparkleDurationMs = durationMs,
                )
            }
        }

        // 버전2: 앞뒤 뒤집기
        MedalAnimationSectionTitle("버전 2 - 앞뒤 뒤집기")
        MedalFlip180Demo()
    }

    // 바텀시트 프리뷰 오버레이
    previewAnimationType?.let { type ->
        MedalAchievementBottomSheet(
            badge = dummyBadge,
            animationType = type,
            onDismiss = { previewAnimationType = null },
        )
    }
}

@Composable
private fun MedalAnimationSectionTitle(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.TextPrimary),
        )
    }
}

/**
 * 버전 1: 탭 시 Y축 기준 회전 (2바퀴)
 * animateFloatAsState + graphicsLayer 사용, FastOutSlowInEasing
 */
@Composable
private fun MedalSpin360Demo(
    durationMs: Int,
    rotationPerTap: Int = 2,
) {
    val medalResId = BadgeMasterData.badges.getOrNull(3)?.iconResId ?: R.drawable.ico_level1
    var trigger by remember { mutableStateOf(0) }
    val rotationDegrees by animateFloatAsState(
        targetValue = trigger * (360f * rotationPerTap),
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "spin360",
    )
    val density = LocalDensity.current.density

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    rotationY = rotationDegrees
                    transformOrigin = TransformOrigin.Center
                    cameraDistance = (8f * density).coerceAtLeast(1f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { trigger += 1 },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(medalResId),
                contentDescription = "메달 (탭하여 회전)",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = "${durationMs}ms — 탭하면 ${rotationPerTap}바퀴 회전",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
        )
    }
}

/**
 * 버전 1 확장: 탭 → 먼저 튀어나옴(scale up) → 그때 회전(2바퀴)
 */
@Composable
private fun MedalPopThenSpinDemo() {
    val medalResId = BadgeMasterData.badges.getOrNull(3)?.iconResId ?: R.drawable.ico_level1
    var trigger by remember { mutableStateOf(0) }
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        // 1단계: 튀어나옴 (250ms)
        scaleAnim.animateTo(
            targetValue = 1.3f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
        // 2단계: 회전 (2바퀴, 500ms)
        rotationAnim.animateTo(
            targetValue = rotationAnim.value + 720f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        )
        // 3단계: 원래 크기로 (150ms)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        )
    }

    val density = LocalDensity.current.density

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    rotationY = rotationAnim.value
                    transformOrigin = TransformOrigin.Center
                    cameraDistance = (8f * density).coerceAtLeast(1f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { trigger += 1 },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(medalResId),
                contentDescription = "메달 (탭하여 튀어나온 뒤 회전)",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            text = "탭 → 튀어나옴 → 2바퀴 회전",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
        )
    }
}

/** 착지 시 반짝임 효과 5가지 */
private enum class SparkleType(val label: String) {
    GRADIENT_SWEEP("1. 그라데이션 스윕"),
    ALPHA_FLASH("2. 알파 플래시"),
    SCALE_PULSE("3. 스케일 펄스"),
    COLOR_FILTER("4. ColorFilter 밝기"),
    SPARKLE_DOTS("5. 반짝 포인트"),
}

/**
 * 버전 1 확장 + 착지 반짝임: 탭 → 튀어나옴 → 회전 → 제자리 왔을 때 반짝임
 */
@Composable
private fun MedalPopThenSpinWithSparkle(
    sparkleType: SparkleType,
    sparkleDurationMs: Int = 200,
) {
    val medalResId = BadgeMasterData.badges.getOrNull(3)?.iconResId ?: R.drawable.ico_level1
    var trigger by remember { mutableStateOf(0) }
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val sparkleProgress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        // 1단계: 튀어나옴 (250ms)
        scaleAnim.animateTo(
            targetValue = 1.3f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
        // 2단계: 회전 (2바퀴, 500ms)
        rotationAnim.animateTo(
            targetValue = rotationAnim.value + 720f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        )
        // 3단계: 원래 크기로 (150ms)
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        )
        // 4단계: 반짝임 (타입별)
        sparkleProgress.snapTo(0f)
        sparkleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = sparkleDurationMs, easing = FastOutSlowInEasing),
        )
        sparkleProgress.snapTo(0f)
    }

    val density = LocalDensity.current.density

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    val scale = when (sparkleType) {
                        SparkleType.SCALE_PULSE -> {
                            if (sparkleProgress.value > 0f && sparkleProgress.value < 1f) {
                                val t = sparkleProgress.value
                                1f + 0.05f * sin(t * PI.toFloat())
                            } else scaleAnim.value
                        }
                        else -> scaleAnim.value
                    }
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotationAnim.value
                    transformOrigin = TransformOrigin.Center
                    cameraDistance = (8f * density).coerceAtLeast(1f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { trigger += 1 },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(medalResId),
                contentDescription = "메달",
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                colorFilter = when (sparkleType) {
                    SparkleType.COLOR_FILTER -> {
                        if (sparkleProgress.value > 0f && sparkleProgress.value < 1f) {
                            val alpha = sin(sparkleProgress.value * PI.toFloat()) * 0.5f
                            ColorFilter.tint(Color.White.copy(alpha = alpha))
                        } else null
                    }
                    else -> null
                },
            )
            when (sparkleType) {
                SparkleType.GRADIENT_SWEEP -> {
                    val sweepX = sparkleProgress.value * 1.4f - 0.2f
                    if (sparkleProgress.value in 0.01f..0.99f) {
                        Canvas(modifier = Modifier.size(48.dp)) {
                            val w = size.width
                            val stripW = w * 0.35f
                            val left = (sweepX * w).coerceIn(-stripW, w)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.8f),
                                        Color.Transparent,
                                    ),
                                    startX = left,
                                    endX = left + stripW,
                                ),
                            )
                        }
                    }
                }
                SparkleType.ALPHA_FLASH -> {
                    val flashAlpha = when {
                        sparkleProgress.value <= 0f || sparkleProgress.value >= 1f -> 0f
                        sparkleProgress.value < 0.25f -> sparkleProgress.value / 0.25f * 0.6f
                        else -> (1f - sparkleProgress.value) / 0.75f * 0.6f
                    }
                    if (flashAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White.copy(alpha = flashAlpha)),
                        )
                    }
                }
                else -> {}
            }
            if (sparkleType == SparkleType.SPARKLE_DOTS) {
                Canvas(modifier = Modifier.size(48.dp)) {
                    if (sparkleProgress.value in 0.1f..0.9f) {
                        val alpha = sin((sparkleProgress.value - 0.1f) / 0.8f * PI.toFloat())
                        val r = 3.dp.toPx()
                        listOf(
                            Offset(size.width * 0.3f, size.height * 0.25f),
                            Offset(size.width * 0.7f, size.height * 0.4f),
                            Offset(size.width * 0.5f, size.height * 0.7f),
                        ).forEach { center ->
                            drawCircle(
                                color = Color.White,
                                radius = r,
                                center = center,
                                alpha = alpha,
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = when {
                sparkleDurationMs != 200 -> "${sparkleType.label} ${sparkleDurationMs}ms"
                else -> sparkleType.label
            },
            style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
        )
    }
}

/**
 * 버전 2: 탭 시 Y축 180도 앞뒤 뒤집기
 * 0~90도: 원본, 90~180도: 어두운 뒷면
 * 다시 탭 시 원래대로
 */
@Composable
private fun MedalFlip180Demo() {
    val medalResId = BadgeMasterData.badges.getOrNull(3)?.iconResId ?: R.drawable.ico_level1
    var isFlipped by remember { mutableStateOf(false) }
    val rotationDegrees by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "flip180",
    )

    val density = LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { isFlipped = !isFlipped },
            contentAlignment = Alignment.Center,
        ) {
            // 뒷면 (어두운 버전) - rotationY = 180일 때 정면
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        rotationY = rotationDegrees + 180f
                        transformOrigin = TransformOrigin.Center
                        cameraDistance = (8f * density).coerceAtLeast(1f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(medalResId),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color.DarkGray),
                )
            }
            // 앞면 (원본)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        rotationY = rotationDegrees
                        transformOrigin = TransformOrigin.Center
                        cameraDistance = (8f * density).coerceAtLeast(1f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(medalResId),
                    contentDescription = "메달 (탭하여 뒤집기)",
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
    Text(
        text = "메달을 탭하면 앞뒤로 뒤집힙니다",
        style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
        modifier = Modifier.fillMaxWidth(),
    )
}
