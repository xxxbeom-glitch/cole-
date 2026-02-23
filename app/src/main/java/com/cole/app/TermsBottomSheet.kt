package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 약관 동의 바텀시트 (Figma 304-2919)
 * BaseBottomSheet 기반, 체크박스 리스트
 */
data class AgreementItem(
    val text: String,
    val isRequired: Boolean,
    var checked: Boolean,
    val onDetailClick: (() -> Unit)? = null,
)

@Composable
fun TermsBottomSheet(
    onDismissRequest: () -> Unit,
    onNextClick: () -> Unit,
    agreements: List<AgreementItem>,
    allAgreedState: Boolean,
    onAllAgreedChange: (Boolean) -> Unit,
    onItemCheckedChange: (index: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseBottomSheet(
        title = "이용약관",
        subtitle = "서비스 이용을 위해 이용약관 동의가 필요해요",
        onDismissRequest = onDismissRequest,
        onPrimaryClick = onNextClick,
        primaryButtonText = "계속 진행",
        primaryButtonEnabled = allAgreedState,
        secondaryButtonText = "돌아가기",
        onSecondaryClick = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 모두 동의 (Figma AgreementRow/Master)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Grey200, RoundedCornerShape(6.dp))
                    .clickable { onAllAgreedChange(!allAgreedState) }
                    .padding(horizontal = 12.dp, vertical = 16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColeCheckBox(checked = allAgreedState, onCheckedChange = onAllAgreedChange)
                    Text(
                        text = "모두 동의 합니다",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    )
                }
            }

            // 개별 항목 (Figma AgreementRow/Item)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                agreements.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemCheckedChange(index, !item.checked) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColeCheckBox(
                            checked = item.checked,
                            onCheckedChange = { onItemCheckedChange(index, it) },
                        )
                        Text(
                            text = item.text,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                        )
                    }
                }
            }
        }
    }
}
