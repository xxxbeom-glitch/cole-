package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource

/**
 * 통계 화면 (Figma MA-01, node 394:3876)
 * - 탭: 오늘/주간/연간/월간
 * - 시간대별 사용량 카드 (바 차트)
 * - 앱별 사용량 카드
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("오늘", "주간", "연간", "월간")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ColeSegmentedTab(
            items = tabLabels,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it },
        )

        // 시간대별 사용량 카드
        TimeSlotUsageCard(
            values = getMockTimeSlotValues(selectedTab),
        )

        // 앱별 사용량 카드
        AppUsageCard(
            apps = mockStatsApps,
        )

        Spacer(modifier = Modifier.height(18.dp))
    }
}

private fun getMockTimeSlotValues(tabIndex: Int): List<Float> {
    return when (tabIndex) {
        0 -> listOf(0f, 0f, 0.44f, 0.63f, 0.31f, 0.91f, 0.94f, 0.83f) // 오늘
        1 -> listOf(0.2f, 0.35f, 0.5f, 0.7f, 0.6f, 0.9f, 0.85f, 0.4f) // 주간
        2 -> listOf(0.15f, 0.4f, 0.55f, 0.75f, 0.65f, 0.95f, 0.8f, 0.45f) // 연간
        else -> listOf(0.1f, 0.3f, 0.45f, 0.6f, 0.7f, 0.88f, 0.72f, 0.5f) // 월간
    }
}

private val mockStatsApps = listOf(
    StatsAppItem("Instagram", "144분", "12회"),
    StatsAppItem("유튜브", "98분", "8회"),
    StatsAppItem("틱톡", "72분", "15회"),
    StatsAppItem("넷플릭스", "45분", "3회"),
)

private data class StatsAppItem(
    val name: String,
    val usageMinutes: String,
    val sessionCount: String,
)

@Composable
private fun TimeSlotUsageCard(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "시간대별 사용량",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(18.dp))
        }

        TimeSlotBarChart(
            values = values,
            progress = progress.value,
        )
    }
}

private val TimeSlotLabels = listOf(3, 6, 9, 12, 15, 18, 21, 24)
private val BarChartHeight = 126.dp
private val BarWidth = 26.dp
private val BarCornerRadius = 2.dp
private val BarLabelGap = 14.dp

@Composable
private fun TimeSlotBarChart(
    values: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(BarChartHeight)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                // 가이드라인 7개: 최상단(0) ~ 최하단(6) 포함, 차트가 그 안에 표시됨
                for (i in 0..6) {
                    val y = size.height * (i / 6f)
                    drawLine(
                        color = AppColors.ChartGuideline,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        pathEffect = pathEffect,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                values.forEachIndexed { index, value ->
                    val animatedHeight = (value * progress).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(BarChartHeight),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(BarWidth)
                                    .fillMaxHeight(animatedHeight)
                                    .align(Alignment.BottomCenter)
                                    .clip(RoundedCornerShape(BarCornerRadius))
                                    .background(AppColors.ChartTrackFill),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(BarLabelGap))
        Row(modifier = Modifier.fillMaxWidth()) {
            TimeSlotLabels.forEach { label ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$label",
                        style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    apps: List<StatsAppItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(12.dp), false, Color.Black.copy(alpha = 0.06f), Color.Black.copy(alpha = 0.06f))
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .padding(start = 16.dp, top = 28.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "앱별 사용량",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            apps.forEach { app ->
                StatsAppRow(
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    sessionCount = app.sessionCount,
                )
            }
        }
    }
}

@Composable
private fun StatsAppRow(
    name: String,
    usageMinutes: String,
    sessionCount: String,
    modifier: Modifier = Modifier,
    appIcon: Painter = painterResource(R.drawable.ic_app_placeholder),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppIconSquircleLock(appIcon = appIcon, iconSize = 56.dp)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = name,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "사용 시간", style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                Text(text = usageMinutes, style = AppTypography.Caption2.copy(color = AppColors.FormTextError))
                Box(modifier = Modifier.size(2.dp).clip(RoundedCornerShape(50)).background(AppColors.Grey400))
                Text(text = "세션", style = AppTypography.Caption2.copy(color = AppColors.TextSecondary))
                Text(text = sessionCount, style = AppTypography.Caption2.copy(color = AppColors.FormTextError))
            }
        }
    }
}
