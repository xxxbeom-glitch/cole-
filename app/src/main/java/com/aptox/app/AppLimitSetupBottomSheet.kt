package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 제한 추가 바텀시트 (Figma 304-1760, 241-3409)
 * - 시간 선택: 칩 Row (DESIGNSYSTEM.md 하루 사용량 지정)
 * - 요일: 월~일 칩 선택 (AppLimitSetupDayBottomSheet)
 */
@Composable
fun AppLimitSetupTimeBottomSheet(
    title: String = "사용 시간을 지정해주세요",
    subtitle: String = "사용 시간을 너무 짧게 시작하면 역효과가 생겨요",
    steps: List<String> = listOf("30분", "60분", "90분", "120분", "150분", "180분"),
    initialIndex: Int = 0,
    feedbackMessages: List<String> = listOf(
        "초보자가 시작하기에 딱 좋은 시간이에요!",
        "아주 적당한 선택이에요!",
        "충분한 여유가 있어요!",
        "적절한 시간이에요!",
        "넉넉한 시간이에요!",
        "충분한 시간이에요!",
    ),
    onDismissRequest: () -> Unit,
    onPrimaryClick: (Int, String) -> Unit,
    primaryButtonText: String = "다음",
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, steps.lastIndex)) }

    BaseBottomSheet(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(selectedIndex, steps[selectedIndex]) },
        primaryButtonText = primaryButtonText,
        modifier = modifier,
    ) {
        // Figma 304-1760, node 1164:4824 — flex, width 328px, align-items center, gap 12dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(80.dp, 60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (selected) Modifier.background(AppColors.Primary200)
                            else Modifier
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { selectedIndex = index },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = AppTypography.BodyMedium.copy(
                            color = if (selected) AppColors.TextHighlight else AppColors.TextTertiary,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun AppLimitSetupDayBottomSheet(
    title: String = "언제 반복할까요",
    subtitle: String = "반복 설정 안 하면 오늘 하루만 적용돼요",
    dayLabels: List<String> = listOf("월", "화", "수", "목", "금", "토", "일"),
    initialSelected: Set<Int> = emptySet(),
    initialRepeatEnabled: Boolean = initialSelected.isNotEmpty(),
    onDismissRequest: () -> Unit,
    onPrimaryClick: (Set<Int>) -> Unit,
    primaryButtonText: String = "다음",
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableStateOf(initialSelected) }
    var repeatEnabled by remember { mutableStateOf(initialRepeatEnabled) }

    BaseBottomSheet(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(if (repeatEnabled) selectedDays else emptySet()) },
        primaryButtonText = primaryButtonText,
        primaryButtonEnabled = !repeatEnabled || selectedDays.isNotEmpty(),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, AppColors.Grey250, RoundedCornerShape(12.dp))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "요일 반복하기",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                )
                AptoxSwitch(
                    checked = repeatEnabled,
                    onCheckedChange = { repeatEnabled = it },
                )
            }
            if (repeatEnabled) {
                // 요일 선택 칩: 선택 안 된 항목 배경 투명 + Border 제거, 텍스트 BodyMedium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayLabels.forEachIndexed { i, label ->
                        val selected = i in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .then(
                                    if (selected) Modifier.background(AppColors.Primary200)
                                    else Modifier
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.BodyMedium.copy(
                                    color = if (selected) AppColors.TextHighlight else AppColors.TextTertiary,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLimitSetupDurationBottomSheet(
    title: String = "언제까지 할까요",
    subtitle: String = "제한을 적용할 기간을 선택해주세요",
    options: List<String> = listOf("1주", "2주", "4주", "8주", "무제한"),
    initialIndex: Int = 0,
    onDismissRequest: () -> Unit,
    onPrimaryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, options.lastIndex)) }

    BaseBottomSheet(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(options[selectedIndex]) },
        primaryButtonText = "계속 진행",
        modifier = modifier,
    ) {
        AptoxStepBar(
            steps = options,
            selectedIndex = selectedIndex,
            onStepSelected = { selectedIndex = it },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
