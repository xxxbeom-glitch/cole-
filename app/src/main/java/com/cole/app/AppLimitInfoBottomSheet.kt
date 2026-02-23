package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 제한 중인 앱 정보 바텀시트 (Figma 350-1802)
 * MA-01-1: 현재 진행중인 앱 상세
 */
data class AppLimitSummaryRow(
    val label: String,
    val value: String,
)

@Composable
fun AppLimitInfoBottomSheet(
    title: String,
    bodyText: String,
    appName: String,
    appIcon: Painter,
    appUsageText: String = "",
    appUsageLabel: String = "",
    summaryRows: List<AppLimitSummaryRow>,
    onDismissRequest: () -> Unit,
    onDetailClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    primaryButtonText: String = "계속 진행",
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onPrimaryClick,
        primaryButtonText = primaryButtonText,
        secondaryButtonText = "돌아가기",
        onSecondaryClick = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 본문
            Text(
                text = bodyText,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )

            // AppStatusRow (Figma AppStatusItem)
            AppStatusRow(
                appName = appName,
                appIcon = appIcon,
                variant = AppStatusVariant.Button,
                usageText = appUsageText,
                usageLabel = appUsageLabel,
                onDetailClick = onDetailClick,
            )

            // Summary (Figma InfoBox Summary)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                summaryRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = row.label,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        )
                        Text(
                            text = row.value,
                            style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}
