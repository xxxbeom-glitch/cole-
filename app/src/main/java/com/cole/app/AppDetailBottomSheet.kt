package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// MA-01-1: 현재 진행중인 앱 상세 바텀시트 (Figma 350-1802)
// 메인에서 앱 클릭 시 표시
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheetMA011(
    appName: String,
    usageText: String,
    usageLabel: String,
    progress: Float,
    onDismissRequest: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // 앱 정보 행
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.TextDisabled),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_app_placeholder),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = appName,
                        style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = usageText,
                            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
                        )
                        Text(
                            text = usageLabel,
                            style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                        )
                    }
                }
            }

            // 사용량 프로그레스
            ColeLabeledProgressBar(
                label = "일일 사용량",
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
            )

            // 자세히 보기 버튼
            ColeSecondaryButton(
                text = "자세히 보기",
                onClick = onDetailClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
