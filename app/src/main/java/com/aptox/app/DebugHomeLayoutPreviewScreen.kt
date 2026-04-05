package com.aptox.app

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val PreviewCardShape = RoundedCornerShape(12.dp)
private val PreviewCardShadow = Color.Black.copy(alpha = 0.06f)

private data class DebugHomePreviewLoad(
    val restrictions: List<MainAppRestrictionItem>,
    val top3: List<StatisticsData.StatsAppItem>,
)

/**
 * Figma 1520 등 2카드 홈 레이아웃 미리보기.
 * **디버그 메뉴에서만 진입** — `MainFlowHost` / 실제 홈 탭에는 연결하지 않음.
 */
@Composable
fun DebugHomeLayoutPreviewScreen(
    onBack: () -> Unit,
    onNavigateToAddAppFlow: () -> Unit,
    onNavigateToTimeSpecifiedFlow: () -> Unit,
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(context))
    val greeting by homeViewModel.greeting.collectAsState()

    var restrictionTab by remember { mutableIntStateOf(0) }
    var showRestrictionTypeSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = showRestrictionTypeSheet) {
        showRestrictionTypeSheet = false
    }

    val homeData by produceState<DebugHomePreviewLoad?>(initialValue = null, context) {
        while (true) {
            kotlin.runCatching {
                val items = withContext(Dispatchers.Default) { loadRestrictionItems(context) }
                val top3 = withContext(Dispatchers.IO) {
                    StatisticsData.loadTop3AppsFromAiCategories(context)
                }
                value = DebugHomePreviewLoad(items, top3)
            }
            delay(2000)
        }
    }

    val filteredRestrictions = remember(restrictionTab, homeData) {
        val items = homeData?.restrictions ?: emptyList()
        when (restrictionTab) {
            0 -> items.filter { it.restrictionType == RestrictionType.DAILY_USAGE }
            else -> items.filter { it.restrictionType == RestrictionType.TIME_SPECIFIED }
        }
    }

    val todayUsageMs by produceState(initialValue = 0L, context) {
        while (true) {
            kotlin.runCatching {
                value = withContext(Dispatchers.IO) {
                    StatisticsData.getTodayTotalUsageMs(context)
                }
            }.onFailure { e ->
                Log.w("DebugHomePreview", "getTodayTotalUsageMs 실패", e)
            }
            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        SignUpHeader(title = "홈 2카드 (디버그)", onBackClick = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "디버그 전용 화면입니다. 실제 메인 홈 탭과 별도입니다.",
                style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
                modifier = Modifier.fillMaxWidth(),
            )

            MainCommentSection(
                comment = greeting.title,
                subtext = greeting.subtext,
                onClick = {},
            )

            TodaySmartphoneUsageCard(usageMs = todayUsageMs)

            // 카드 1: 주간 최다 사용 앱 (큰 추가 버튼 없음)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PreviewCardShape, false, PreviewCardShadow, PreviewCardShadow)
                    .clip(PreviewCardShape)
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val weekData = homeData
                Text(
                    text = "일주일간 최대 사용시간 앱",
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                )
                when {
                    weekData == null -> {
                        Text(
                            text = "불러오는 중…",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        )
                    }
                    weekData.top3.isEmpty() -> {
                        Text(
                            text = "일주일간 사용 데이터를 확인해보세요",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            weekData.top3.forEach { app ->
                                MainHomeTopAppRow(
                                    app = app,
                                    onAddRestrictionClick = { showRestrictionTypeSheet = true },
                                )
                            }
                        }
                    }
                }
            }

            // 카드 2: 제한 중인 앱 + 탭 + 큰 버튼만 여기
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, PreviewCardShape, false, PreviewCardShadow, PreviewCardShadow)
                    .clip(PreviewCardShape)
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 18.dp),
            ) {
                Text(
                    text = "제한 중인 앱",
                    style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .width(244.dp) // 120 + 4 + 120
                            .height(30.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HomeRestrictionTabChip(
                            text = "하루 사용량 제한",
                            selected = restrictionTab == 0,
                            onClick = { restrictionTab = 0 },
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight(),
                        )
                        HomeRestrictionTabChip(
                            text = "지정 시간 제한",
                            selected = restrictionTab == 1,
                            onClick = { restrictionTab = 1 },
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight(),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                when {
                    homeData == null -> {
                        Text(
                            text = "불러오는 중…",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        )
                    }
                    filteredRestrictions.isEmpty() -> {
                        Text(
                            text = "이 카테고리에 표시할 앱이 없어요",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        )
                    }
                    else -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            filteredRestrictions.forEach { item ->
                                MainAppRestrictionRow(item = item, onDetailClick = {})
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                AptoxAddAppButton(
                    text = "사용량 제한 앱 추가",
                    icon = painterResource(R.drawable.ic_add_circle),
                    onClick = { showRestrictionTypeSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                )
            }
        }
    }

    if (showRestrictionTypeSheet) {
        RestrictionTypeSelectBottomSheet(
            onDailyLimitClick = {
                showRestrictionTypeSheet = false
                onNavigateToAddAppFlow()
            },
            onTimeSpecifiedClick = {
                showRestrictionTypeSheet = false
                onNavigateToTimeSpecifiedFlow()
            },
            onDismissRequest = { showRestrictionTypeSheet = false },
        )
    }
}
