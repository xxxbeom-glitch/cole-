package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Figma ST-10 (node-id=317-3002) 디자인 전용 테스트 화면
 * - 반원형 게이지 (ST10SemiCircleGauge)
 * - 헤더: 줄별 크기/색상 (스마트폰 중독 테스트 보라색 강조)
 * - 습관 리스트 좌우 분리 배치 (아이콘+상태, 상태 텍스트 검정)
 * - 버튼: "확인" (Primary) / "다시하기" (plain text)
 */
private object ST10Spacing {
    val ScreenPaddingH: Dp = 16.dp
    val TopPadding: Dp = 130.dp
    val HeaderLineGap: Dp = 8.dp
    val HeaderToGauge: Dp = 8.dp
    val GaugeToMessage: Dp = 24.dp
    val MessageToHabits: Dp = 52.dp
    /** Figma: 수면장애/집중력 저하/사용시간 증가 - width 240px, gap 10px */
    val HabitSectionWidth: Dp = 240.dp
    val HabitItemGap: Dp = 10.dp
    val HabitsToDisclaimer: Dp = 56.dp
    val DisclaimerToButton: Dp = 23.dp
    /** 프로젝트 하단 버튼 규칙: buttonGap 12dp, bottomButtonPadding 24dp */
    val ButtonToRetry: Dp = 12.dp
    val BottomPadding: Dp = 24.dp
}
data class HabitItemDataST10(
    val label: String,
    val statusText: String,
    val color: Color,
    val iconRes: Int
)

private val ColorGreen = Color(0xFF3C9F90)
private val ColorRed = Color(0xFFE35959)
private val ColorYellow = Color(0xFFFF976B)

private fun habitItemsST10(resultType: SelfTestResultType): List<HabitItemDataST10> = when (resultType) {
    SelfTestResultType.DANGER -> listOf(
        HabitItemDataST10("수면장애", "주의", ColorGreen, R.drawable.ic_result_normal),
        HabitItemDataST10("집중력 저하", "주의", ColorRed, R.drawable.ic_result_danger),
        HabitItemDataST10("사용시간 증가", "주의", ColorYellow, R.drawable.ic_result_caution),
    )
    SelfTestResultType.CAUTION -> listOf(
        HabitItemDataST10("수면장애", "정상", ColorGreen, R.drawable.ic_result_normal),
        HabitItemDataST10("집중력 저하", "주의", ColorRed, R.drawable.ic_result_danger),
        HabitItemDataST10("사용시간 증가", "주의", ColorYellow, R.drawable.ic_result_caution),
    )
    SelfTestResultType.GOOD -> listOf(
        HabitItemDataST10("수면장애", "정상", ColorGreen, R.drawable.ic_result_normal),
        HabitItemDataST10("집중력 저하", "정상", ColorRed, R.drawable.ic_result_danger),
        HabitItemDataST10("사용시간 증가", "주의", ColorYellow, R.drawable.ic_result_caution),
    )
    SelfTestResultType.HEALTH -> listOf(
        HabitItemDataST10("수면장애", "정상", ColorGreen, R.drawable.ic_result_normal),
        HabitItemDataST10("집중력 저하", "정상", ColorRed, R.drawable.ic_result_danger),
        HabitItemDataST10("사용시간 증가", "정상", ColorGreen, R.drawable.ic_result_normal),
    )
}

private fun mainMessageST10(resultType: SelfTestResultType): String = when (resultType) {
    SelfTestResultType.DANGER -> "스마트폰 사용 습관 점검이 필요해요!"
    SelfTestResultType.CAUTION -> "스마트폰 사용에 주의가 필요해요!"
    SelfTestResultType.GOOD -> "양호한 사용 습관을 갖고 계시네요!"
    SelfTestResultType.HEALTH -> "스마트폰 사용 습관이 건강해요!"
}

@Composable
fun SelfTestResultScreenST10(
    resultType: SelfTestResultType,
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    rawScore: Int,
    userName: String = "장원영",
    modifier: Modifier = Modifier,
) {
    val displayScore = rawScoreToDisplayScore(rawScore)
    val habits = habitItemsST10(resultType)
    val mainMsg = mainMessageST10(resultType)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 스크롤 영역
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ST10Spacing.ScreenPaddingH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(ST10Spacing.TopPadding))

            // 헤더 (Figma 443-2572: Display3, 스마트폰 중독 테스트만 Primary400)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ST10Spacing.HeaderLineGap),
            ) {
                Text(
                    text = "$userName 님의",
                    style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "스마트폰 중독 테스트",
                    style = AppTypography.Display3.copy(color = AppColors.Primary400),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "결과입니다",
                    style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(ST10Spacing.HeaderToGauge))

            ST10SemiCircleGauge(
                displayScore = displayScore,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(ST10Spacing.GaugeToMessage))

            Text(
                text = mainMsg,
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(ST10Spacing.MessageToHabits))

            // Figma: flex column, width 240px, justify-content center, align-items center, gap 10px
            Column(
                modifier = Modifier.width(ST10Spacing.HabitSectionWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ST10Spacing.HabitItemGap)
            ) {
                habits.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.label,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = item.color
                            )
                            Text(
                                text = item.statusText,
                                style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(ST10Spacing.HabitsToDisclaimer))

            Text(
                text = "본 자가진단 테스트는 스마트폰 사용 습관을 점검하기 위한 참고 자료이며, 의학적 진단 도구가 아님을 알려드립니다",
                style = AppTypography.Caption1.copy(color = AppColors.TextDisclaimer),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(ST10Spacing.DisclaimerToButton))
        }

        // 하단 고정 버튼 영역 (ColeTwoLineButton: Primary + Ghost)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = ST10Spacing.ScreenPaddingH)
                .padding(bottom = ST10Spacing.BottomPadding),
        ) {
            ColeTwoLineButton(
                primaryText = "확인",
                ghostText = "다시하기",
                onPrimaryClick = onStartClick,
                onGhostClick = onBackClick,
            )
        }
    }
}
