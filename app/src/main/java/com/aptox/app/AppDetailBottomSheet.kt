package com.aptox.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import kotlinx.coroutines.launch

private val detailMinuteFormat = DecimalFormat("#,###")

/** Figma: 수치 영역 고정 폭 120dp */
private val AppDetailValueColumnWidth = 120.dp

/** 2시간 구간 12칸; 칸은 가로 너비와 동일한 세로(1:1). corner 2dp, 슬롯 간격 7dp */
private val AppDetailTimeSlotBarGap = 7.dp
private val AppDetailTimeSlotBarCorner = 2.dp
private const val AppDetailSegmentCount = 12

private fun AppDetailAverageTimeSlotBarTone.toBarColor(): Color {
    val p = AppColors.Primary300 // #6C54DD
    return when (this) {
        AppDetailAverageTimeSlotBarTone.MUTED -> AppColors.Grey350
        AppDetailAverageTimeSlotBarTone.PRIMARY_20 -> p.copy(alpha = 0.2f)
        AppDetailAverageTimeSlotBarTone.PRIMARY_40 -> p.copy(alpha = 0.4f)
        AppDetailAverageTimeSlotBarTone.PRIMARY_80 -> p.copy(alpha = 0.8f)
        AppDetailAverageTimeSlotBarTone.PRIMARY_FULL -> p.copy(alpha = 1f)
    }
}

/**
 * 통계 카테고리 앱 상세 바텀시트 (Figma 1465-4640)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    state: AppDetailUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val iconPainter = remember(state.packageName, state.appIcon) {
        state.appIcon?.let { BitmapPainter(it.asImageBitmap()) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "상세 내역",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(38.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (iconPainter != null) {
                        Image(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_app_placeholder),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Text(
                    text = state.appName,
                    style = AppTypography.BodyBold.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Figma 1588-7491: 요약 카드 한 장 — 3행 통계 + 평균 사용 시간대(1588-7728 gap 8)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundInfoBox)
                    .border(0.5.dp, AppColors.BorderInfoBox, RoundedCornerShape(12.dp))
                    .padding(vertical = 22.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppDetailStatRow(
                    label = "총 사용시간",
                    value = "${detailMinuteFormat.format(state.totalMinutes)}분",
                )
                AppDetailStatRow(
                    label = "총 실행횟수",
                    value = "${detailMinuteFormat.format(state.launchCount.toLong())}회",
                )
                AppDetailStatRow(
                    label = "평균 세션",
                    value = "${detailMinuteFormat.format(state.avgSessionMinutes)}분",
                )
                AppDetailAverageTimeSlotBlock(
                    barTones = state.averageTimeSlotBarTones,
                )
            }

            Spacer(modifier = Modifier.height(38.dp))

            AptoxPrimaryButton(
                text = "닫기",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp),
            )
        }
    }
}

@Composable
private fun AppDetailStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
            textAlign = TextAlign.End,
            modifier = Modifier.width(AppDetailValueColumnWidth),
        )
    }
}

/** Figma 1588-7728 — 라벨 행 + 8dp + 1:1 막대(12칸) + 10dp + X축 4구간 Caption( opacity 0.8) */
@Composable
private fun AppDetailAverageTimeSlotBlock(
    barTones: List<AppDetailAverageTimeSlotBarTone>?,
    modifier: Modifier = Modifier,
) {
    val tones12 = List(AppDetailSegmentCount) { index ->
        barTones?.getOrNull(index) ?: AppDetailAverageTimeSlotBarTone.MUTED
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "평균 사용 시간대",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
            Spacer(modifier = Modifier.width(AppDetailValueColumnWidth))
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gaps = AppDetailTimeSlotBarGap * (AppDetailSegmentCount - 1)
                val barWidth = (maxWidth - gaps) / AppDetailSegmentCount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDetailTimeSlotBarGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tones12.forEach { tone ->
                        Box(
                            modifier = Modifier
                                .size(barWidth)
                                .clip(RoundedCornerShape(AppDetailTimeSlotBarCorner))
                                .background(tone.toBarColor()),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.8f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatisticsData.TimeSlot4SectionLabels.forEach { label ->
                    Text(
                        text = label,
                        style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
