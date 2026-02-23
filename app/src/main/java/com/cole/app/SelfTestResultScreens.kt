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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ─────────────────────────────────────────────
// ST-10: 자가테스트 결과 화면 (Figma 317:3002)
// ─────────────────────────────────────────────

// rawScore 0~32 → 표시 점수 250~500 (임의 매핑, 추후 로직 반영)
fun rawScoreToDisplayScore(rawScore: Int): Int = (250 + rawScore * 250 / 32).coerceIn(250, 500)

fun displayScoreToInterpretation(displayScore: Int): String = when {
    displayScore < 334 -> "스마트폰 사용 습관이 건강해요!"
    displayScore < 417 -> "스마트폰 사용에 주의가 필요해요"
    else -> "스마트폰 사용 습관 점검이 필요해요"
}

// 습관 breakdown (임의 로직, 추후 반영)
private data class HabitItem(val label: String, val iconResId: Int, val status: String)
private fun habitItemsFromResult(resultType: SelfTestResultType): List<HabitItem> = when (resultType) {
    SelfTestResultType.LOW -> listOf(
        HabitItem("수면장애", R.drawable.ic_result_normal, "정상"),
        HabitItem("집중력 저하", R.drawable.ic_result_normal, "정상"),
        HabitItem("사용시간 증가", R.drawable.ic_result_normal, "정상"),
    )
    SelfTestResultType.MIDDLE -> listOf(
        HabitItem("수면장애", R.drawable.ic_result_normal, "정상"),
        HabitItem("집중력 저하", R.drawable.ic_result_caution, "주의"),
        HabitItem("사용시간 증가", R.drawable.ic_result_caution, "주의"),
    )
    SelfTestResultType.HIGH -> listOf(
        HabitItem("수면장애", R.drawable.ic_result_caution, "주의"),
        HabitItem("집중력 저하", R.drawable.ic_result_danger, "위험"),
        HabitItem("사용시간 증가", R.drawable.ic_result_danger, "위험"),
    )
}

@Composable
fun SelfTestResultScreen(
    resultType: SelfTestResultType,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    rawScore: Int,
    userName: String = "사용자",
    modifier: Modifier = Modifier,
) {
    val displayScore = rawScoreToDisplayScore(rawScore)
    val interpretation = displayScoreToInterpretation(displayScore)
    val habitItems = habitItemsFromResult(resultType)

    // 게이지 fill 애니메이션: 250~500 → 0~1
    val fillProgress = ((displayScore - 250) / 250f).coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = fillProgress,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_fill",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 타이틀: "○○○ 님의 스마트폰 중독 테스트 결과입니다"
        Text(
            text = buildAnnotatedString {
                append("${userName} 님의\n")
                withStyle(SpanStyle(color = AppColors.TextHighlight)) {
                    append("스마트폰 중독 테스트\n")
                }
                append("결과입니다")
            },
            style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 게이지 영역 (좌→우 fill 애니메이션)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val gaugeWidth = maxWidth
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clipToBounds(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedFill)
                            .height(148.dp)
                            .clipToBounds(),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.st_result_gauge),
                            contentDescription = null,
                            modifier = Modifier
                                .width(gaugeWidth)
                                .height(148.dp),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("250", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                    Text("500", style = AppTypography.Caption1.copy(color = AppColors.TextCaption))
                }
            }
        }

        // 점수 + 해석
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$displayScore",
                style = AppTypography.Display1.copy(color = AppColors.TextPrimary),
            )
            Text(
                text = interpretation,
                style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary),
            )
        }
        Spacer(modifier = Modifier.height(52.dp))

        // 습관 breakdown
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            habitItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.label,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Image(
                            painter = painterResource(id = item.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = item.status,
                            style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(77.dp))

        // 면책 문구
        Text(
            text = "본 자가진단 테스트는 스마트폰 사용 습관을 점검하기 위한\n참고 자료이며, 의학적 진단 도구가 아님을 알려드립니다",
            style = AppTypography.Disclaimer.copy(color = AppColors.TextCaption),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "계속 진행",
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(
                text = "돌아가기",
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
