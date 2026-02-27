package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardShape = RoundedCornerShape(12.dp)
// Figma: X=0, Y=0, Blur=6, Spread=0, #000000 6%
private val CardShadowElevation = 6.dp
private val CardShadowColor = Color.Black.copy(alpha = 0.06f)

/** Figma 352-3425, 352-3527: List / Switch Button - ColeToggleSwitch 래퍼 (AndroidView 기반) */
@Composable
fun ColeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ColeToggleSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
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
        AppIconSquircleLock(appIcon = appIcon, iconSize = 56.dp)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            .then(
                when {
                    variant == SelectionRowVariant.Switch && onSwitchChange != null -> Modifier
                    onClick != null -> Modifier.clickable { onClick() }
                    else -> Modifier
                }
            )
            .padding(
                horizontal = 16.dp,
                vertical = if (variant == SelectionRowVariant.Switch) 19.dp else 22.dp, // Switch: 30dp 높이 확보 (68-38=30)
            ),
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
            if (variant != SelectionRowVariant.Switch) {
                Icon(
                    painter = chevronIcon ?: defaultChevron,
                    contentDescription = null,
                    tint = AppColors.TextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (variant == SelectionRowVariant.Switch) {
                Box(modifier = Modifier.wrapContentSize()) {
                    ColeToggleSwitch(checked = switchChecked, onCheckedChange = { onSwitchChange?.invoke(it) })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 설정 메뉴 리스트 컴포넌트 (Figma MY-02~07)
// ─────────────────────────────────────────────────────────────────────────────

private val SettingsDividerColor = Color(0xFFF3F3F3)
private val SettingsBadgeAllowed = Color(0xFF616bff)   // 허용됨
private val SettingsBadgeRequired = Color(0xFFe44242)  // 필요

@Composable
fun SettingsRow(
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Primary200),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = label,
                contentScale = ContentScale.None,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextPrimary,
        )
    }
}

@Composable
fun SettingsRowWithBadge(
    iconResId: Int,
    label: String,
    badgeText: String,
    badgeAllowed: Boolean,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Primary200),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = label,
                contentScale = ContentScale.None,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = label,
                    style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (badgeAllowed) SettingsBadgeAllowed else SettingsBadgeRequired)
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = AppTypography.Label.copy(color = AppColors.TextInvert, fontSize = 9.sp),
                    )
                }
            }
            Text(
                text = subtitle,
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextPrimary,
        )
    }
}

@Composable
fun SettingsRowWithValue(
    iconResId: Int,
    label: String,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Primary200),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = label,
                contentScale = ContentScale.None,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
        } else if (showChevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = AppColors.TextPrimary,
            )
        }
    }
}

@Composable
fun SimpleTextRow(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
        )
    }
}

@Composable
fun SettingsListCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

@Composable
fun SettingsDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SettingsDividerColor),
    )
}
