package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// ST-09 자가테스트 로딩 화면 여백
// ─────────────────────────────────────────────
private val TopTextsGap = 8.dp        // 잠시만 기다려주세요 ~ 작성하신~입니다
private val TextToAnimationGap = 10.dp // 작성하신~ ~ 애니메이션 (그대로)
private val AnimationToQuoteGap = 40.dp // 애니메이션 ~ 인용문
private val QuoteToSourceGap = 10.dp   // 인용문 ~ 출처

/**
 * ST-09: 자가테스트 결과 로딩 화면
 * - 잠시만 기다려주세요 / 작성하신~입니다 / 애니메이션 / 인용문 — 중앙정렬
 */
@Composable
fun SelfTestLoadingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "잠시만 기다려주세요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(TopTextsGap))
            Text(
                text = "작성하신 자가진단표를\n분석중입니다",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(TextToAnimationGap))
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingToCheckAnimation(onComplete = onFinish)
            }
            Spacer(modifier = Modifier.height(AnimationToQuoteGap))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(QuoteToSourceGap),
            ) {
                Text(
                    text = "SNS는 인생의 낭비다. 그 시간에 책을 읽어라.",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "알렉스 퍼거슨 (Sir Alex Ferguson)",
                    style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
