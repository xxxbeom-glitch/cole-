package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 제한 추가 바텀시트 (Figma 304-1760, 241-3409)
 * - 슬라이더: 사용 시간 선택 (30분~180분)
 * - 요일: 월~일 칩 선택
 */
@Composable
fun AppLimitSetupTimeBottomSheet(
    title: String = "사용 시간을 지정해주세요",
    subtitle: String = "본인의 사용패턴을 생각해 시간을 선택해주세요",
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
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, steps.lastIndex)) }
    val feedback = feedbackMessages.getOrElse(selectedIndex) { "" }

    BaseBottomSheet(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(selectedIndex, steps[selectedIndex]) },
        primaryButtonText = "계속 진행",
        secondaryButtonText = "돌아가기",
        onSecondaryClick = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = steps[selectedIndex],
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextHighlight),
                )
                Text(
                    text = feedback,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
            ColeStepBar(
                steps = steps,
                selectedIndex = selectedIndex,
                onStepSelected = { selectedIndex = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AppLimitSetupDayBottomSheet(
    title: String = "언제 반복할까요",
    subtitle: String = "제한할 요일을 선택해주세요",
    dayLabels: List<String> = listOf("월", "화", "수", "목", "금", "토", "일"),
    initialSelected: Set<Int> = emptySet(),
    onDismissRequest: () -> Unit,
    onPrimaryClick: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDays by remember { mutableStateOf(initialSelected) }

    BaseBottomSheet(
        title = title,
        subtitle = subtitle,
        onDismissRequest = onDismissRequest,
        onPrimaryClick = { onPrimaryClick(selectedDays) },
        primaryButtonText = "계속 진행",
        secondaryButtonText = "돌아가기",
        onSecondaryClick = onDismissRequest,
        modifier = modifier,
    ) {
        ColeChipRow(
            labels = dayLabels,
            selectedIndices = selectedDays,
            onChipClick = { i ->
                selectedDays = if (i in selectedDays) selectedDays - i else selectedDays + i
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
