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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// Chips (요일 선택 등 단일 문자 칩)
// ─────────────────────────────────────────────

@Composable
fun ColeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (selected) Modifier.background(AppColors.Primary300)
                else Modifier
                    .background(AppColors.White900)
                    .border(1.5.dp, AppColors.BorderDefault, RoundedCornerShape(6.dp))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppTypography.Caption1.copy(
                color = if (selected) AppColors.TextInvert else AppColors.TextBody,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
fun ColeChipRow(
    labels: List<String>,
    selectedIndices: Set<Int>,
    onChipClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            ColeChip(
                label = label,
                selected = index in selectedIndices,
                onClick = { onChipClick(index) },
            )
        }
    }
}

// ─────────────────────────────────────────────
// CheckBox
// ─────────────────────────────────────────────

@Composable
fun ColeCheckBox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (checked) Modifier.background(AppColors.InteractiveCheckBoxBgChecked)
                else Modifier
                    .background(AppColors.InteractiveCheckBoxBgUnchecked)
                    .border(1.5.dp, AppColors.InteractiveCheckBoxBorderUnchecked, CircleShape)
            )
            .then(
                if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            // 체크마크 (간단한 텍스트 대체 — 실제 프로젝트에서는 아이콘 리소스 사용 권장)
            Text(
                text = "✓",
                style = AppTypography.Caption1.copy(
                    color = AppColors.InteractiveCheckBoxBorderCheck,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────
// Radio Button
// ─────────────────────────────────────────────

@Composable
fun ColeRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(AppColors.InteractiveRadioBgSelected)
                else Modifier
                    .background(AppColors.White900)
                    .border(1.5.dp, AppColors.InteractiveRadioBorderUnselected, CircleShape)
            )
            .then(
                if (enabled) Modifier.clickable { onClick() } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(AppColors.InteractiveRadioBorderSelected),
            )
        }
    }
}

// ─────────────────────────────────────────────
// SelectionCard (라디오 버튼 포함 선택 카드)
// ─────────────────────────────────────────────

@Composable
fun ColeSelectionCard(
    title: String,
    description: String,
    trailingText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.border(1.5.dp, AppColors.Primary300, RoundedCornerShape(12.dp))
                else Modifier.border(1.dp, AppColors.BorderDefault, RoundedCornerShape(12.dp))
            )
            .background(AppColors.SurfaceBackgroundCard)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                ColeRadioButton(
                    selected = selected,
                    onClick = onClick,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = description,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                    )
                }
            }
            Text(
                text = trailingText,
                style = AppTypography.BodyBold.copy(
                    color = AppColors.TextHighlight,
                    textAlign = TextAlign.End,
                ),
                modifier = Modifier.width(64.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun ColeSelectionCardGroup(
    items: List<SelectionCardItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.forEachIndexed { index, item ->
            ColeSelectionCard(
                title = item.title,
                description = item.description,
                trailingText = item.trailingText,
                selected = index == selectedIndex,
                onClick = { onItemSelected(index) },
            )
        }
    }
}

data class SelectionCardItem(
    val title: String,
    val description: String,
    val trailingText: String,
)

// ─────────────────────────────────────────────
// SelfTestSelect (자가진단 / 온보딩 선택 버튼)
// ─────────────────────────────────────────────

@Composable
fun ColeSelfTestButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) AppColors.ButtonPrimaryBgDefault
                else AppColors.ButtonSecondaryBgDefault
            )
            .then(
                if (!selected) Modifier.border(
                    1.dp,
                    AppColors.ButtonSecondaryBorderDefault,
                    RoundedCornerShape(12.dp),
                )
                else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.BodyMedium.copy(
                color = if (selected) AppColors.TextInvert else AppColors.TextBody,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
fun ColeSelfTestButtonGroup(
    options: List<String>,
    selectedIndex: Int?,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { index, option ->
            ColeSelfTestButton(
                text = option,
                selected = index == selectedIndex,
                onClick = { onOptionSelected(index) },
            )
        }
    }
}
