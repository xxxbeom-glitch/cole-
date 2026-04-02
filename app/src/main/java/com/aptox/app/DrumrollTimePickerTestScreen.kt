package com.aptox.app

import android.util.Log
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "DrumrollTimePicker"

// Figma: 각 셀 높이 48dp, 표시 셀 수 3개 (위 1 + 선택 1 + 아래 1)
private val CELL_HEIGHT: Dp = 48.dp
private const val VISIBLE_CELLS = 3

private fun paddedItemCount(itemsSize: Int): Int {
    val pad = VISIBLE_CELLS / 2
    return itemsSize + 2 * pad
}

@Composable
fun DrumrollTimePickerTestScreen(onBack: () -> Unit) {
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AptoxGhostButton(text = "돌아가기", onClick = onBack)
        AptoxPrimaryButton(
            text = "드럼롤 시간 선택 바텀시트 열기",
            onClick = { showSheet = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showSheet) {
        DrumrollTimePickerBottomSheet(
            onDismissRequest = { showSheet = false },
            onConfirm = { hour, minute, amPm ->
                Log.d(TAG, "선택된 시간: $amPm ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}")
                showSheet = false
            },
        )
    }
}

/**
 * 드럼롤 시간 선택 바텀시트 (Figma 997:3736)
 * - 시(1~12) / 분(00~59) / 오전·오후 세 컬럼 드럼롤 피커
 * - 선택 셀: Primary200 배경 + Primary300 텍스트 + rounded-12dp
 * - 비선택 셀: FormInputBgDefault 배경 + alpha 0.1f
 * - 스크롤이 멈춘 뒤(`isScrollInProgress == false`) 중앙 셀 인덱스로 스냅·확정
 * - 완료 버튼은 동일 로직으로 LazyListState에서 최종값 읽기
 * @param onConfirm 세 번째 인자: "오전" 또는 "오후"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumrollTimePickerBottomSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int, amPm: String) -> Unit,
    title: String = "제한 시간대를 선택해 주세요",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cellHeightPx = with(density) { CELL_HEIGHT.toPx() }

    val now = remember {
        val cal = java.util.Calendar.getInstance()
        Triple(
            cal.get(java.util.Calendar.HOUR).let { if (it == 0) 12 else it },
            cal.get(java.util.Calendar.MINUTE),
            if (cal.get(java.util.Calendar.AM_PM) == java.util.Calendar.AM) "오전" else "오후",
        )
    }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = now.first - 1)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = now.second)
    val amPmListState = rememberLazyListState(initialFirstVisibleItemIndex = if (now.third == "오전") 0 else 1)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
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
                text = title,
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DrumrollColumn(
                    listState = hourListState,
                    items = (1..12).map { it.toString().padStart(2, '0') },
                    cellHeightPx = cellHeightPx,
                    modifier = Modifier.weight(1f),
                )
                DrumrollColumn(
                    listState = minuteListState,
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    cellHeightPx = cellHeightPx,
                    modifier = Modifier.weight(1f),
                )
                DrumrollColumn(
                    listState = amPmListState,
                    items = listOf("오전", "오후"),
                    cellHeightPx = cellHeightPx,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(46.dp))

            AptoxPrimaryButton(
                text = "완료",
                onClick = {
                    scope.launch {
                        val hourIdx = snapColumnToNearestAndResolveIndex(
                            hourListState,
                            cellHeightPx,
                            paddedItemCount(12),
                            itemsSize = 12,
                        )
                        val minuteIdx = snapColumnToNearestAndResolveIndex(
                            minuteListState,
                            cellHeightPx,
                            paddedItemCount(60),
                            itemsSize = 60,
                        )
                        val amPmIdx = snapColumnToNearestAndResolveIndex(
                            amPmListState,
                            cellHeightPx,
                            paddedItemCount(2),
                            itemsSize = 2,
                        )
                        onConfirm(
                            hourIdx + 1,
                            minuteIdx,
                            if (amPmIdx == 0) "오전" else "오후",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

/**
 * 스크롤이 완전히 멈춘 뒤(`isScrollInProgress == false`) 가장 가까운 셀까지 스냅하고,
 * 뷰포트 중앙에 해당하는 실제 아이템 인덱스를 반환한다.
 * (완료 버튼·스크롤 종료 스냅 모두 동일 로직 사용)
 */
private suspend fun snapColumnToNearestAndResolveIndex(
    listState: LazyListState,
    cellHeightPx: Float,
    paddedItemCount: Int,
    itemsSize: Int,
): Int {
    val padding = VISIBLE_CELLS / 2
    val paddedLastIndex = paddedItemCount - 1
    val maxFirst = (paddedLastIndex - padding).coerceAtLeast(0)

    while (true) {
        while (listState.isScrollInProgress) {
            delay(16)
        }
        val firstIndex = listState.firstVisibleItemIndex
        val offset = listState.firstVisibleItemScrollOffset

        val snapToNext = offset > cellHeightPx / 2f
        val targetFirstIndex = (firstIndex + if (snapToNext) 1 else 0)
            .coerceIn(0, maxFirst)

        val remainingPx = if (snapToNext) {
            cellHeightPx - offset
        } else {
            -offset.toFloat()
        }

        if (abs(remainingPx) < 0.5f) {
            val centerPaddedIndex = (targetFirstIndex + padding).coerceIn(0, paddedLastIndex)
            return (centerPaddedIndex - padding).coerceIn(0, (itemsSize - 1).coerceAtLeast(0))
        }

        listState.animateScrollBy(
            value = remainingPx,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f,
            ),
        )
    }
}

/**
 * 단일 드럼롤 컬럼
 *
 * - `isScrollInProgress`가 false가 되는 시점에 [snapColumnToNearestAndResolveIndex]로 스냅
 * - 강조 인덱스는 LazyListState를 직접 읽어 중앙 셀과 동일한 식으로 계산 (스크롤 중에도 일치)
 */
@Composable
private fun DrumrollColumn(
    listState: LazyListState,
    items: List<String>,
    cellHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    val padding = VISIBLE_CELLS / 2  // 위아래 빈 셀 수 = 1
    val paddedItems = buildList {
        repeat(padding) { add("") }
        addAll(items)
        repeat(padding) { add("") }
    }

    LaunchedEffect(listState, cellHeightPx, items.size) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                snapColumnToNearestAndResolveIndex(
                    listState,
                    cellHeightPx,
                    paddedItemCount = paddedItems.size,
                    itemsSize = items.size,
                )
            }
    }

    val firstVisible = listState.firstVisibleItemIndex
    val scrollOffset = listState.firstVisibleItemScrollOffset
    val centerFirst = if (scrollOffset > cellHeightPx / 2f) firstVisible + 1 else firstVisible
    val highlightIndex = (centerFirst + padding).coerceIn(0, paddedItems.lastIndex)

    val consumeVerticalForSheet = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // LazyColumn 스크롤 후 남은 세로 델타를 소비해 ModalBottomSheet 드래그로 전파되지 않게 함
                return Offset(0f, available.y)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .height(CELL_HEIGHT * VISIBLE_CELLS)
            .nestedScroll(consumeVerticalForSheet),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = true,
    ) {
        items(paddedItems.size) { index ->
            val isSelected = index == highlightIndex
            val isEmpty = paddedItems[index].isEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CELL_HEIGHT)
                    .then(
                        when {
                            isEmpty -> Modifier
                            isSelected -> Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Primary200)
                            else -> Modifier
                                .alpha(0.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.FormInputBgDefault)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!isEmpty) {
                    Text(
                        text = paddedItems[index],
                        style = AppTypography.HeadingH3.copy(
                            color = if (isSelected) AppColors.Primary300 else AppColors.TextPrimary,
                            textAlign = TextAlign.Center,
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * 단일 목록(예: "30분", "60분")을 드럼롤로 선택하는 바텀시트.
 * [DrumrollTimePickerBottomSheet]와 동일한 셀 높이·스냅·중앙 하이라이트 스타일.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumrollDurationPickerBottomSheet(
    items: List<String>,
    initialIndex: Int = 0,
    title: String,
    subtitle: String? = null,
    confirmButtonText: String = "다음",
    onDismissRequest: () -> Unit,
    onConfirm: (index: Int, value: String) -> Unit,
) {
    if (items.isEmpty()) return

    val safeIndex = initialIndex.coerceIn(0, items.lastIndex)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val cellHeightPx = with(density) { CELL_HEIGHT.toPx() }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeIndex)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
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
                text = title,
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            DrumrollColumn(
                listState = listState,
                items = items,
                cellHeightPx = cellHeightPx,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(46.dp))

            AptoxPrimaryButton(
                text = confirmButtonText,
                onClick = {
                    scope.launch {
                        val idx = snapColumnToNearestAndResolveIndex(
                            listState,
                            cellHeightPx,
                            paddedItemCount(items.size),
                            itemsSize = items.size,
                        )
                        onConfirm(idx, items[idx])
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
