package com.aptox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Figma 1442-4904: 온보딩 진단 결과
 * - diagnosisScore: 0~100 환산 점수 (computeDiagnosisScore() 결과)
 * - 점수 카운트업 애니메이션 (0 → diagnosisScore, 800ms)
 * - 등급별 컬러·뱃지·멘트
 */
@Composable
fun DiagnosisResultScreen(
    userName: String,
    diagnosisScore: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resultType = diagnosisScoreToResultType(diagnosisScore)
    val style = diagnosisStyle(resultType)
    val message = diagnosisMessage(resultType)
    val barFraction = diagnosisScore / 100f

    // 점수 카운트업 애니메이션
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(diagnosisScore) {
        animatedScore.animateTo(
            targetValue = diagnosisScore.toFloat(),
            animationSpec = tween(durationMillis = 800),
        )
    }
    var displayedScore by remember { mutableIntStateOf(0) }
    LaunchedEffect(animatedScore.value) {
        displayedScore = animatedScore.value.roundToInt()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 328.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Text(
                    text = "${userName}님의 진단 결과",
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "${displayedScore}점",
                                style = AppTypography.Display1.copy(color = style.scoreColor),
                                textAlign = TextAlign.Center,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(style.badgeBg)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = style.badgeLabel,
                                    style = AppTypography.BodyBold.copy(color = style.badgeText),
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .width(280.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.Grey300),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barFraction.coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(style.barFill),
                            )
                        }
                    }
                    Text(
                        text = message,
                        style = AppTypography.BodyRegular.copy(color = AppColors.TextBody),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            AptoxPrimaryButton(
                text = "시작하기",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data class DiagnosisStyle(
    val scoreColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val badgeLabel: String,
    val barFill: Color,
)

private fun diagnosisStyle(type: SelfTestResultType): DiagnosisStyle = when (type) {
    SelfTestResultType.DANGER -> DiagnosisStyle(
        scoreColor = Color(0xFFDE4E4E),
        badgeBg = Color(0xFFFFC9C9),
        badgeText = Color(0xFFE42E2E),
        badgeLabel = "위험",
        barFill = Color(0xFFDE4E4E),
    )
    SelfTestResultType.CAUTION -> DiagnosisStyle(
        scoreColor = Color(0xFFE87E3C),
        badgeBg = Color(0xFFFFDEC9),
        badgeText = Color(0xFFED6830),
        badgeLabel = "주의",
        barFill = Color(0xFFE87E3C),
    )
    SelfTestResultType.GOOD, SelfTestResultType.HEALTH -> DiagnosisStyle(
        scoreColor = Color(0xFF3B5FB9),
        badgeBg = Color(0xFFD9E4FF),
        badgeText = Color(0xFF617AB7),
        badgeLabel = "양호",
        barFill = Color(0xFF3B5FB9),
    )
}

private fun diagnosisMessage(type: SelfTestResultType): String = when (type) {
    SelfTestResultType.DANGER ->
        "스마트폰 사용이 일상 전반에 영향을 주고 있어요.\n작은 변화부터 함께 시작해봐요."
    SelfTestResultType.CAUTION ->
        "스마트폰 사용이 일상에 조금씩 영향을 주고 있어요.\n지금이 습관을 바꾸기 딱 좋은 타이밍이에요."
    SelfTestResultType.GOOD, SelfTestResultType.HEALTH ->
        "스마트폰을 건강하게 사용하고 있어요.\n지금의 균형을 유지해보세요."
}
