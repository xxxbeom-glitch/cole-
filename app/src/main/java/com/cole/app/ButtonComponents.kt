package com.cole.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ColePrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonPrimaryBgDefault,
            contentColor = AppColors.ButtonPrimaryTextDefault,
            disabledContainerColor = AppColors.ButtonPrimaryBgDisabled,
            disabledContentColor = AppColors.ButtonPrimaryTextDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(text = text, style = AppTypography.ButtonLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ColeGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonGhostBgDefault,
            contentColor = AppColors.ButtonGhostTextDefault,
            disabledContainerColor = AppColors.ButtonGhostBgDisabled,
            disabledContentColor = AppColors.ButtonGhostTextDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
    ) {
        Text(text = text, style = AppTypography.ButtonLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ColeTwoLineButton(
    primaryText: String,
    ghostText: String,
    onPrimaryClick: () -> Unit,
    onGhostClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ghostEnabled: Boolean = enabled,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ColePrimaryButton(text = primaryText, onClick = onPrimaryClick, enabled = enabled)
        ColeGhostButton(text = ghostText, onClick = onGhostClick, enabled = ghostEnabled)
    }
}

@Composable
fun ColeSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(35.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.ButtonSecondaryBgDefault,
            contentColor = AppColors.ButtonSecondaryTextDefault,
            disabledContainerColor = AppColors.ButtonSecondaryBgDisabled,
            disabledContentColor = AppColors.ButtonSecondaryTextDisabled,
        ),
        border = BorderStroke(0.6.dp, if (enabled) AppColors.ButtonSecondaryBorderDefault else AppColors.ButtonSecondaryBorderDisabled),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(text = text, style = AppTypography.ButtonSmall)
    }
}

@Composable
fun ColeAddAppButton(text: String, icon: Painter, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonPrimaryBgDefault,
            contentColor = AppColors.ButtonPrimaryTextDefault,
            disabledContainerColor = AppColors.ButtonPrimaryBgDisabled,
            disabledContentColor = AppColors.ButtonPrimaryTextDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.Unspecified)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, style = AppTypography.ButtonLarge, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 나의 스마트폰 중독 지수 카드용 아웃라인 버튼 (Figma 619-2806)
 * border 1dp, 배경 흰색, 60dp 높이, 12dp 라운드
 */
@Composable
fun ColeOutlinedTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.White900,
            contentColor = AppColors.TextPrimary,
            disabledContainerColor = AppColors.SurfaceBackgroundBackground,
            disabledContentColor = AppColors.TextDisabled,
        ),
        border = BorderStroke(1.dp, if (enabled) AppColors.BorderDefault else AppColors.Grey400),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(text = text, style = AppTypography.ButtonLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ColeInvertButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(53.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = AppColors.TextInvert,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.TextInvert.copy(alpha = 0.4f),
        ),
        border = BorderStroke(1.dp, if (enabled) AppColors.TextInvert else AppColors.TextInvert.copy(alpha = 0.4f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
    ) {
        Text(text = text, style = AppTypography.ButtonLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
