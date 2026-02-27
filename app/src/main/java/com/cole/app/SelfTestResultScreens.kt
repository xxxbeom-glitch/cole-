package com.cole.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// ST-10 자가테스트 결과 화면 (Figma 576-2686)
// ─────────────────────────────────────────────

/** (레거시) rawScore → displayScore 250~500 (ResultGaugeGraph / DebugGaugeTestScreen용) */
fun rawScoreToDisplayScore(rawScore: Int): Int =
    (250 + rawScore.coerceIn(0, 32) * 250f / 32f).roundToInt().coerceIn(250, 500)

/** (레거시) displayScore → 해석 문자열 (ResultGaugeGraph / DebugGaugeTestScreen용) */
fun displayScoreToInterpretation(displayScore: Int): String = when {
    displayScore < 334 -> "스마트폰 사용 습관 점검이 필요해요"
    displayScore < 417 -> "스마트폰 사용에 주의가 필요해요"
    else -> "스마트폰 사용 습관이 건강해요!"
}

/** rawScore(8~32) → 게이지 progress 0~1 */
fun rawScoreToProgress(rawScore: Int): Float =
    ((rawScore - 8) / 24f).coerceIn(0f, 1f)

private val GaugeBarWidth = 260.dp
private val GaugeBarHeight = 12.dp
private val GaugeTrackColor = Color(0xFFE8E8E8)

private fun gaugeGradientColors(resultType: SelfTestResultType): Pair<Color, Color> = when (resultType) {
    SelfTestResultType.DANGER -> Color(0xFFFFC2C2) to Color(0xFFFF6B6B)  // bad
    SelfTestResultType.CAUTION -> Color(0xFFFFD5C3) to Color(0xFFFF976B) // warning
    SelfTestResultType.GOOD -> Color(0xFFD2F3FF) to Color(0xFF67D0F5)   // good
    SelfTestResultType.HEALTH -> Color(0xFFC8FFF2) to Color(0xFF51E2C0)  // great
}

private fun mainMessage(resultType: SelfTestResultType): String = when (resultType) {
    SelfTestResultType.DANGER -> "스마트폰 사용 습관 점검이 필요해요"
    SelfTestResultType.CAUTION -> "스마트폰 사용에 주의가 필요해요"
    SelfTestResultType.GOOD -> "양호한 사용 습관을 갖고 계시네요"
    SelfTestResultType.HEALTH -> "건강한 사용 습관을 갖고 계시네요"
}

private fun subMessage(resultType: SelfTestResultType): String = when (resultType) {
    SelfTestResultType.DANGER -> "지금부터 습관을 바꿔보세요"
    SelfTestResultType.CAUTION -> "습관 개선을 시작해보세요"
    SelfTestResultType.GOOD -> "지금의 좋은 습관을 계속 유지해보세요"
    SelfTestResultType.HEALTH -> "지금의 좋은 습관을 계속 유지해보세요"
}

private data class HabitItem(val label: String, val dotColor: Color, val statusText: String)

// 스크린샷: 수면=초록, 집중력=빨강, 사용시간=주황
private val HabitDotGreen = Color(0xFF3C9F90)
private val HabitDotRed = Color(0xFFE35959)
private val HabitDotOrange = Color(0xFFFF976B)

private fun habitItems(resultType: SelfTestResultType): List<HabitItem> = when (resultType) {
    SelfTestResultType.DANGER -> listOf(
        HabitItem("수면장애", HabitDotGreen, "주의"),
        HabitItem("집중력 저하", HabitDotRed, "주의"),
        HabitItem("사용시간 증가", HabitDotOrange, "주의"),
    )
    SelfTestResultType.CAUTION -> listOf(
        HabitItem("수면장애", HabitDotGreen, "정상"),
        HabitItem("집중력 저하", HabitDotRed, "주의"),
        HabitItem("사용시간 증가", HabitDotOrange, "주의"),
    )
    SelfTestResultType.GOOD -> listOf(
        HabitItem("수면장애", HabitDotGreen, "정상"),
        HabitItem("집중력 저하", HabitDotRed, "정상"),
        HabitItem("사용시간 증가", HabitDotOrange, "주의"),
    )
    SelfTestResultType.HEALTH -> listOf(
        HabitItem("수면장애", HabitDotGreen, "정상"),
        HabitItem("집중력 저하", HabitDotRed, "정상"),
        HabitItem("사용시간 증가", HabitDotGreen, "정상"),
    )
}

@Composable
fun SelfTestResultScreen(
    resultType: SelfTestResultType,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    rawScore: Int,
    userName: String = "장원영",
    modifier: Modifier = Modifier,
) {
    val progress = rawScoreToProgress(rawScore.coerceIn(8, 32))
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "gauge_progress",
    )
    val habits = habitItems(resultType)
    val mainMsg = mainMessage(resultType)
    val subMsg = subMessage(resultType)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // "장원영 님은" (이름 밑줄)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = userName,
                style = AppTypography.HeadingH2.copy(
                    color = AppColors.TextPrimary,
                    textDecoration = TextDecoration.Underline,
                ),
            )
            Text(
                text = " 님은",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 메인 문구
        Text(
            text = mainMsg,
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 부가 문구
        Text(
            text = subMsg,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 게이지 바 260dp + 화살표
        SelfTestResultGaugeBar(
            progress = animatedProgress,
            resultType = resultType,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 습관 항목 3개 (컬러 점 + 정상/주의) - 좌측 정렬
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            habits.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(item.dotColor),
                    )
                    Text(
                        text = item.statusText,
                        style = AppTypography.Caption1.copy(color = AppColors.TextBody),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 디스클레임
        Text(
            text = "본 자가진단 테스트는 스마트폰 사용 습관을 점검하기 위한 참고 자료이며, 의학적 진단 도구가 아님을 알려드립니다",
            style = AppTypography.Caption1.copy(color = AppColors.TextDisclaimer),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "확인",
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(
                text = "다시하기",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SelfTestResultGaugeBar(
    progress: Float,
    resultType: SelfTestResultType,
    modifier: Modifier = Modifier,
) {
    val clamped = progress.coerceIn(0f, 1f)
    val (gradientStart, gradientEnd) = gaugeGradientColors(resultType)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(GaugeBarWidth)
                .height(GaugeBarHeight + 24.dp)
                .clipToBounds(),
        ) {
            // 트랙
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(GaugeBarWidth)
                    .height(GaugeBarHeight)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GaugeTrackColor),
            )

            // 그라데이션 Fill (좌측부터 progress만큼)
            if (clamped > 0.001f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(GaugeBarWidth * clamped)
                        .height(GaugeBarHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(gradientStart, gradientEnd),
                            ),
                        ),
                )
            }

            // 화살표 (점수 위치)
            val arrowOffsetDp = (GaugeBarWidth * clamped - 10.dp).coerceIn(0.dp, GaugeBarWidth - 20.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(GaugeBarWidth)
                    .height(GaugeBarHeight + 24.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = arrowOffsetDp)
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
    }
}
