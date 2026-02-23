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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.ButtonPrimaryBgDefault,
            contentColor = AppColors.ButtonPrimaryTextDefault,
            disabledContainerColor = AppColors.ButtonPrimaryBgDisabled,
            disabledContentColor = AppColors.ButtonPrimaryTextDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
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
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(35.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AppColors.ButtonSecondaryBgDefault,
            contentColor = AppColors.ButtonSecondaryTextDefault,
            disabledContainerColor = AppColors.ButtonSecondaryBgDisabled,
            disabledContentColor = AppColors.ButtonSecondaryTextDisabled,
        ),
        border = BorderStroke(
            width = 0.6.dp,
            color = if (enabled) AppColors.ButtonSecondaryBorderDefault
                    else AppColors.ButtonSecondaryBorderDisabled,
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonSmall,
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
    Button(
        onClick = onClick,
        enabled = enabled,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = AppTypography.ButtonLarge,
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(53.dp),
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = AppColors.TextInvert,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = AppColors.TextInvert.copy(alpha = 0.4f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) AppColors.TextInvert
                    else AppColors.TextInvert.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
    ) {
        Text(
            text = text,
            style = AppTypography.ButtonLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
