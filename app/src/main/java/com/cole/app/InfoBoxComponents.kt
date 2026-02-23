package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Figma: InfoBox / 안내 박스
 * SurfaceBackgroundInfoBox, BorderInfoBox 사용
 */
@Composable
fun ColeInfoBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(12.dp))
            .border(1.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.BodyRegular.copy(color = AppColors.TextBody),
        )
    }
}
