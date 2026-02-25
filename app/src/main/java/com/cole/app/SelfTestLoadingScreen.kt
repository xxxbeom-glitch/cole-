package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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

/**
 * ST-09: 자가테스트 결과 로딩 화면
 * - 잠시만 기다려주세요 / 작성하신 자가진단표를 분석중입니다
 * - 로딩 애니메이션 (3 dot 웨이브 → 합쳐짐 → 체크)
 * - 하단 인용문
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
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 상단: 잠시만 기다려주세요
                Text(
                    text = "잠시만 기다려주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    textAlign = TextAlign.Center,
                )

                // 메인: 작성하신 자가진단표를 분석중입니다 + 로딩→체크 애니메이션
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    Text(
                        text = "작성하신 자가진단표를\n분석중입니다",
                        style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                        textAlign = TextAlign.Center,
                    )
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingToCheckAnimation(onComplete = onFinish)
                    }
                }

                // 하단: 인용문 + 출처
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
}
