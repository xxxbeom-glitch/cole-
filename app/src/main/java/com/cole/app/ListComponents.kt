package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val CardShape = RoundedCornerShape(12.dp)
// Figma: X=0, Y=0, Blur=6, Spread=0, #000000 6%
private val CardShadowElevation = 6.dp
private val CardShadowColor = Color.Black.copy(alpha = 0.06f)

@Composable
fun ColeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = AppColors.White900,
            checkedTrackColor = AppColors.Primary300,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = AppColors.White900,
            uncheckedTrackColor = AppColors.Grey350,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = AppColors.Primary300.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = AppColors.Grey350.copy(alpha = 0.4f),
        ),
    )
}

enum class AppStatusVariant { Default, Button, DataView }

@Composable
fun AppStatusRow(
    appName: String,
    appIcon: Painter,
    modifier: Modifier = Modifier,
    variant: AppStatusVariant = AppStatusVariant.Default,
    usageText: String = "",
    usageLabel: String = "",
    onDetailClick: (() -> Unit)? = null,
    usageMinutes: String = "",
    sessionCount: String = "",
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = appIcon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp),
            )
        }
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
                when (variant) {
                    AppStatusVariant.Default -> Unit
                    AppStatusVariant.Button -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = usageText, style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
                            Text(text = usageLabel, style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                        }
                    }
                    AppStatusVariant.DataView -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "사용 시간", style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                            Text(text = usageMinutes, style = AppTypography.Caption2.copy(color = AppColors.FormTextError))
                            Box(modifier = Modifier.size(2.dp).clip(RoundedCornerShape(50)).background(AppColors.Grey400))
                            Text(text = "세션", style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                            Text(text = sessionCount, style = AppTypography.Caption2.copy(color = AppColors.FormTextError))
                        }
                    }
                }
            }
            if (variant == AppStatusVariant.Button && onDetailClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.ButtonSecondaryBgDefault)
                        .border(0.6.dp, AppColors.ButtonSecondaryBorderDefault, RoundedCornerShape(6.dp))
                        .clickable { onDetailClick() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(text = "자세히 보기", style = AppTypography.ButtonSmall.copy(color = AppColors.ButtonSecondaryTextDefault))
                }
            }
        }
    }
}

enum class SelectionRowVariant { Default, Selected, Switch }

@Composable
fun SelectionRow(
    label: String,
    modifier: Modifier = Modifier,
    variant: SelectionRowVariant = SelectionRowVariant.Default,
    selectedValue: String = "",
    switchChecked: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    chevronIcon: Painter? = null,
) {
    val defaultChevron = painterResource(R.drawable.ic_chevron_right)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(
                elevation = CardShadowElevation,
                shape = CardShape,
                clip = false,
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .clip(CardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (variant == SelectionRowVariant.Selected && selectedValue.isNotEmpty()) {
                Text(
                    text = selectedValue,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextHighlight, textAlign = TextAlign.End),
                    modifier = Modifier.width(100.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = chevronIcon ?: defaultChevron,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(24.dp),
            )
            if (variant == SelectionRowVariant.Switch) {
                ColeSwitch(checked = switchChecked, onCheckedChange = { onSwitchChange?.invoke(it) })
            }
        }
    }
}
