package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 앱 제한 일시정지 바텀시트 (Figma UL-01, UL-02, UL-03 / 151-1047, 151-1242, 151-1298)
 * BaseBottomSheet 활용
 */
@Composable
fun AppLimitPauseProposalBottomSheet(
    onDismissRequest: () -> Unit,
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit,
    /** false이면 primary 버튼 비활성화 + 텍스트 변경, secondary는 "닫기" */
    canPause: Boolean = true,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = "5분간 일시정지 하시겠어요?",
        subtitle = "5분간 앱을 자유롭게 사용할 수 있습니다.\n하루 2회까지 사용 가능해요",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = if (canPause) onContinueClick else onDismissRequest,
        primaryButtonText = if (canPause) "계속 진행" else "일시정지를 하실 수 없습니다",
        primaryButtonEnabled = canPause,
        secondaryButtonText = if (canPause) "아니오" else "닫기",
        onSecondaryClick = onBackClick,
        modifier = modifier,
    ) { }
}

@Composable
fun AppLimitPauseConfirmBottomSheet(
    appName: String,
    appIcon: Painter,
    /** 시간지정제한 타입: "22분 후 제한 해제" */
    usageText: String,
    /** 시간지정제한 타입: 보통 "" */
    usageLabel: String,
    onDismissRequest: () -> Unit,
    onPauseClick: () -> Unit,
    onBackClick: () -> Unit,
    onDetailClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = "5분간 일시정지 하시겠어요?",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onPauseClick,
        primaryButtonText = "일시정지 1회 사용하기",
        secondaryButtonText = "취소",
        onSecondaryClick = onBackClick,
        modifier = modifier,
    ) {
        PauseAppRow(
            appName = appName,
            appIcon = appIcon,
            usageText = usageText,
            usageLabel = usageLabel,
            showDetailButton = onDetailClick != null,
            onDetailClick = onDetailClick ?: {},
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.material3.Text(
            text = "5분간 앱을 자유롭게 사용할 수 있습니다.\n하루 2회까지 사용 가능해요.\n단, 일시정지 시간은 사용 통계에 포함되지 않아요",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
        )
    }
}

@Composable
fun AppLimitPauseCompleteBottomSheet(
    appName: String,
    appIcon: Painter,
    remainingChances: Int,
    onDismissRequest: () -> Unit,
    onLaunchAppClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = "5분간 일시정지 완료",
        subtitle = "지금부터 5분간 앱을 사용하실 수 있어요\n이제 남은 기회는 ${remainingChances}번이에요",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onLaunchAppClick,
        primaryButtonText = "앱 실행하기",
        secondaryButtonText = null,
        modifier = modifier,
    ) {
        PauseAppRow(
            appName = appName,
            appIcon = appIcon,
            usageText = "",
            usageLabel = "",
            showDetailButton = false,
            onDetailClick = {},
        )
    }
}

@Composable
private fun PauseAppRow(
    appName: String,
    appIcon: Painter,
    usageText: String,
    usageLabel: String,
    showDetailButton: Boolean,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIconSquircleLock(appIcon = appIcon)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = appName,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(120.dp),
                )
                if (usageText.isNotEmpty() || usageLabel.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = usageText, style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
                        Text(text = usageLabel, style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                    }
                }
            }
            if (showDetailButton) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.ButtonSecondaryBgDefault)
                        .border(0.6.dp, AppColors.ButtonSecondaryBorderDefault, RoundedCornerShape(6.dp))
                        .clickable(onClick = onDetailClick)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "자세히 보기",
                        style = AppTypography.ButtonSmall.copy(color = AppColors.ButtonSecondaryTextDefault),
                    )
                }
            }
        }
    }
}
