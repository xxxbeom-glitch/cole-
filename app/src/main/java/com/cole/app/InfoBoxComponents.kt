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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Figma: InfoBox / Notice 안내 박스
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
            .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 22.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.Disclaimer.copy(color = AppColors.TextTertiary),
            textAlign = TextAlign.Center,
        )
    }
}
