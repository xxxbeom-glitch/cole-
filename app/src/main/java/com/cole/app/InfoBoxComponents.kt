package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val CompactBoxPaddingHorizontal = 12.dp
private val CompactBoxPaddingVertical = 8.dp
private val CompactBoxCornerRadius = 6.dp
// 패딩(8*2) + Caption1 lineHeight(19sp) ≈ 35dp
private val CompactBoxMinHeight = 35.dp

/**
 * Figma 901-3501: InfoBox Compact
 * 패딩 horizontal=12dp(가변), vertical=8dp, minHeight=35dp, rounded 6dp
 */
@Composable
fun ColeInfoBoxCompact(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CompactBoxMinHeight)
            .background(AppColors.SurfaceBackgroundInfoBox, RoundedCornerShape(CompactBoxCornerRadius))
            .border(1.dp, AppColors.BorderInfoBox, RoundedCornerShape(CompactBoxCornerRadius))
            .padding(horizontal = CompactBoxPaddingHorizontal, vertical = CompactBoxPaddingVertical),
    ) {
        Text(
            text = text,
            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

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
