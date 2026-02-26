package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 다이얼로그 팝업 가이드 (Figma 310:2725)
 * - 아이콘/이미지 + 제목 + 부제 + 날짜 + 2줄 버튼
 * - 328dp 폭, 24dp 모서리, Shadow/Card
 */
@Composable
fun ColeGuideDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    date: String? = null,
    primaryButtonText: String = "계속 진행",
    secondaryButtonText: String = "돌아가기",
    onPrimaryClick: () -> Unit = { onDismissRequest() },
    onSecondaryClick: () -> Unit = { onDismissRequest() },
    icon: @Composable (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = modifier
                .widthIn(max = 328.dp)
                .shadow(6.dp, RoundedCornerShape(24.dp), false, Color.Black.copy(alpha = 0.06f))
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // 아이콘 + 텍스트 블록
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 아이콘 영역 (72dp) - 연보라 플레이스홀더 또는 커스텀
                    if (icon != null) {
                        icon()
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(AppColors.Primary100),
                        )
                    }
                    // 제목 + 부제
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                            textAlign = TextAlign.Center,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    if (date != null) {
                        Text(
                            text = date,
                            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                ColeTwoLineButton(
                    primaryText = primaryButtonText,
                    ghostText = secondaryButtonText,
                    onPrimaryClick = onPrimaryClick,
                    onGhostClick = onSecondaryClick,
                )
            }
        }
    }
}
