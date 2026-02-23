import androidx.compose.animation.core.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// ColePrimaryButton
//
// Figma: Button / Two line Button (상단 Primary 버튼)
// 상태: Default / Pressed / Disabled
//
// 사용 예시:
//   ColePrimaryButton(text = "계속 진행", onClick = { ... })
//   ColePrimaryButton(text = "계속 진행", onClick = { ... }, enabled = false)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.ButtonPrimaryBgDisabled
            isPressed -> AppColors.ButtonPrimaryBgPressed
            else -> AppColors.ButtonPrimaryBgDefault
        },
        label = "primaryContainerColor",
    )
    val contentColor = when {
        !enabled -> AppColors.ButtonPrimaryTextDisabled
        else -> AppColors.ButtonPrimaryTextDefault
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .then(Modifier.background(containerColor)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeGhostButton
//
// Figma: Button / Two line Button (하단 Ghost 버튼)
// 상태: Default / Pressed / Disabled
//
// 사용 예시:
//   ColeGhostButton(text = "돌아가기", onClick = { ... })
//   ColeGhostButton(text = "돌아가기", onClick = { ... }, enabled = false)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = when {
        !enabled -> AppColors.ButtonGhostBgDisabled
        isPressed -> AppColors.ButtonGhostBgHover
        else -> AppColors.ButtonGhostBgDefault
    }
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.ButtonGhostTextDisabled
            isPressed -> AppColors.ButtonGhostTextHover
            else -> AppColors.ButtonGhostTextDefault
        },
        label = "ghostContentColor",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeTwoLineButton
//
// Figma: Button / Two line Button (Primary + Ghost 묶음)
// Primary(상단) + Ghost(하단) 버튼을 수직으로 묶은 복합 컴포넌트
//
// 사용 예시:
//   ColeTwoLineButton(
//       primaryText = "계속 진행",
//       ghostText = "돌아가기",
//       onPrimaryClick = { ... },
//       onGhostClick = { ... },
//   )
//   ColeTwoLineButton(..., enabled = false)  // 전체 비활성화
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeTwoLineButton(
    primaryText: String,
    ghostText: String,
    onPrimaryClick: () -> Unit,
    onGhostClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ColePrimaryButton(
            text = primaryText,
            onClick = onPrimaryClick,
            enabled = enabled,
        )
        ColeGhostButton(
            text = ghostText,
            onClick = onGhostClick,
            enabled = enabled,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeSecondaryButton
//
// Figma: Button / List Button (소형 보조 버튼 — "자세히 보기")
// 상태: Default / Pressed / Disabled
//
// 사용 예시:
//   ColeSecondaryButton(text = "자세히 보기", onClick = { ... })
//   ColeSecondaryButton(text = "자세히 보기", onClick = { ... }, enabled = false)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.ButtonSecondaryBgDisabled
            isPressed -> AppColors.ButtonSecondaryBgHover
            else -> AppColors.ButtonSecondaryBgDefault
        },
        label = "secondaryContainerColor",
    )
    val showBorder = enabled && !isPressed
    val borderColor = when {
        !enabled -> AppColors.ButtonSecondaryBorderDisabled
        else -> AppColors.ButtonSecondaryBorderDefault
    }
    val contentColor = when {
        !enabled -> AppColors.ButtonSecondaryTextDisabled
        else -> AppColors.ButtonSecondaryTextDefault
    }
    val textAlpha = when {
        !enabled || isPressed -> 0.9f
        else -> 1f
    }
    Box(
        modifier = modifier
            .height(35.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (showBorder) Modifier.border(0.6.dp, borderColor, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonSmall,
            color = contentColor.copy(alpha = textAlpha),
            modifier = Modifier.padding(
                top = 10.dp,
                bottom = 9.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeAddAppButton
//
// Figma: Button / Add App (아이콘 + 텍스트 Primary 버튼)
// 상태: Default / Pressed / Disabled
//
// 사용 예시:
//   ColeAddAppButton(text = "잠시만 멀어질 앱 추가하기", icon = painterResource(R.drawable.ic_plus), onClick = { ... })
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeAddAppButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> AppColors.ButtonPrimaryBgDisabled
            isPressed -> AppColors.ButtonPrimaryBgPressed
            else -> AppColors.ButtonPrimaryBgDefault
        },
        label = "addAppContainerColor",
    )
    val contentColor = when {
        !enabled -> AppColors.ButtonPrimaryTextDisabled
        else -> AppColors.ButtonPrimaryTextDefault
    }
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = AppTypography.ButtonLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeInvertButton
//
// Figma: Invert Button / Default (보라 배경 위 흰 테두리 + 흰 텍스트 버튼)
// 보라색 배경 섹션 위에 올라가는 반전 스타일 버튼
//
// 사용 예시:
//   ColeInvertButton(text = "설정하러 가기", onClick = { ... })
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeInvertButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) AppColors.TextInvert else AppColors.TextInvert.copy(alpha = 0.4f)
    val borderColor = if (enabled) AppColors.TextInvert else AppColors.TextInvert.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(53.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(
                enabled = enabled,
                indication = null,
                onClick = onClick,
            )
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            color = contentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, bottom = 16.dp, start = 12.dp, end = 12.dp),
        )
    }
}
