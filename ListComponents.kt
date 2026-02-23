import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

// ─────────────────────────────────────────────────────────────────────────────
// 공통 Shadow 값 (Figma: Shadow/Card — 0px 0px 6px rgba(0,0,0,0.06))
// ─────────────────────────────────────────────────────────────────────────────

private val CardShape = RoundedCornerShape(12.dp)
private val CardElevation = 2.dp  // shadow 근사값

// ─────────────────────────────────────────────────────────────────────────────
// ColeSwitch
//
// Figma: List / Switch Button (On / Off)
// 표준 Material Switch를 cole 디자인 토큰으로 래핑
//
// 사용 예시:
//   ColeSwitch(checked = isOn, onCheckedChange = { isOn = it })
// ─────────────────────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────────────────────
// AppStatusRow — 앱 상태 행 (3가지 변형)
//
// Figma: List / App Status (Default / Button / DataView)
//
// variant 별 차이:
//   Default   — 앱 이름만 표시
//   Button    — 앱 이름 + 사용량(highlight) + "자세히 보기" 버튼
//   DataView  — 앱 이름 + 사용 시간·세션 통계 (error 색상)
//
// 사용 예시:
//   AppStatusRow(appName = "Instagram", appIcon = painterResource(...))
//   AppStatusRow(
//       appName = "Instagram", appIcon = ...,
//       variant = AppStatusVariant.Button,
//       usageText = "14분/30분", usageLabel = "사용 중",
//       onDetailClick = { ... }
//   )
//   AppStatusRow(
//       appName = "Instagram", appIcon = ...,
//       variant = AppStatusVariant.DataView,
//       usageMinutes = "144분", sessionCount = "12회"
//   )
// ─────────────────────────────────────────────────────────────────────────────

enum class AppStatusVariant { Default, Button, DataView }

@Composable
fun AppStatusRow(
    appName: String,
    appIcon: Painter,
    modifier: Modifier = Modifier,
    variant: AppStatusVariant = AppStatusVariant.Default,
    // Button variant
    usageText: String = "",
    usageLabel: String = "",
    onDetailClick: (() -> Unit)? = null,
    // DataView variant
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
        // 앱 아이콘
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.TextDisabled),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Icon(
                painter = appIcon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp).padding(bottom = 4.dp),
            )
        }

        // 텍스트 영역
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // 앱 이름
                Text(
                    text = appName,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(120.dp),
                )

                // 변형별 서브텍스트
                when (variant) {
                    AppStatusVariant.Default -> Unit

                    AppStatusVariant.Button -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = usageText,
                                style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
                            )
                            Text(
                                text = usageLabel,
                                style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                            )
                        }
                    }

                    AppStatusVariant.DataView -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "사용 시간",
                                style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                            )
                            Text(
                                text = usageMinutes,
                                style = AppTypography.Caption2.copy(color = AppColors.FormTextError),
                            )
                            // 구분 점
                            Box(
                                modifier = Modifier
                                    .size(2.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(AppColors.Grey400),
                            )
                            Text(
                                text = "세션",
                                style = AppTypography.Caption2.copy(color = AppColors.TextSecondary),
                            )
                            Text(
                                text = sessionCount,
                                style = AppTypography.Caption2.copy(color = AppColors.FormTextError),
                            )
                        }
                    }
                }
            }

            // Button variant: "자세히 보기" 버튼
            if (variant == AppStatusVariant.Button && onDetailClick != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.ButtonSecondaryBgDefault)
                        .border(
                            width = 0.6.dp,
                            color = AppColors.ButtonSecondaryBorderDefault,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .clickable { onDetailClick() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "자세히 보기",
                        style = AppTypography.ButtonSmall.copy(color = AppColors.ButtonSecondaryTextDefault),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SelectionRow — 앱 선택 행 (3가지 변형)
//
// Figma: List / SelectionRow (Default / Selected / Switch)
// 카드 형태(흰 배경 + 그림자)의 선택 행 컴포넌트
//
// variant 별 차이:
//   Default   — 레이블 + 화살표 아이콘
//   Selected  — 레이블 + 선택된 값(highlight) + 화살표 아이콘
//   Switch    — 레이블 + 화살표 아이콘 + 토글 스위치
//
// 사용 예시:
//   SelectionRow(label = "앱을 선택해주세요", onClick = { ... })
//   SelectionRow(label = "앱을 선택해주세요", selectedValue = "인스타그램", onClick = { ... })
//   SelectionRow(
//       label = "앱을 선택해주세요",
//       variant = SelectionRowVariant.Switch,
//       switchChecked = isOn,
//       onSwitchChange = { isOn = it },
//       onClick = { ... }
//   )
// ─────────────────────────────────────────────────────────────────────────────

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .shadow(elevation = CardElevation, shape = CardShape, clip = false)
            .clip(CardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 레이블
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )

        // 우측 영역
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Selected: 선택된 값 텍스트
            if (variant == SelectionRowVariant.Selected && selectedValue.isNotEmpty()) {
                Text(
                    text = selectedValue,
                    style = AppTypography.BodyMedium.copy(
                        color = AppColors.TextHighlight,
                        textAlign = TextAlign.End,
                    ),
                    modifier = Modifier.wrapContentWidth(),
                )
            }

            // Default / Selected: 화살표 아이콘
            if (variant != SelectionRowVariant.Switch) {
                if (chevronIcon != null) {
                    Icon(
                        painter = chevronIcon,
                        contentDescription = null,
                        tint = AppColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    // 화살표 아이콘 자리 (실제 프로젝트에서 R.drawable.ic_arrow_right 등으로 교체)
                    Box(modifier = Modifier.size(24.dp))
                }
            }

            // Switch: 토글
            if (variant == SelectionRowVariant.Switch) {
                ColeSwitch(
                    checked = switchChecked,
                    onCheckedChange = { onSwitchChange?.invoke(it) },
                )
            }
        }
    }
}
