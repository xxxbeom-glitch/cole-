package com.cole.app

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 통계 화면 (Figma MA-01, node 394:3876)
 * - 탭: 오늘/주간/월간/연간
 * - 시간대별 사용량 카드 (3시간 단위 바 차트)
 * - 사용시간 최다 앱 카드 (실제 UsageStats 데이터, 시스템앱 제외)
 */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabLabels = listOf("오늘", "주간", "월간", "연간")
    val tabEnum = when (selectedTab) {
        0 -> StatisticsData.Tab.TODAY
        1 -> StatisticsData.Tab.WEEKLY
        2 -> StatisticsData.Tab.MONTHLY
        else -> StatisticsData.Tab.YEARLY
    }
    var timeSlotMinutes by remember { mutableStateOf<List<Long>>(List(8) { 0L }) }
    var appList by remember { mutableStateOf<List<StatisticsData.StatsAppItem>>(emptyList()) }
    LaunchedEffect(tabEnum) {
        withContext(Dispatchers.IO) {
            timeSlotMinutes = StatisticsData.loadTimeSlotMinutes(context, tabEnum)
            appList = StatisticsData.loadAppUsage(context, tabEnum)
        }
    }
    val maxSlot = timeSlotMinutes.maxOrNull()?.takeIf { it > 0 } ?: 1L
    val values = timeSlotMinutes.map { (it.toFloat() / maxSlot).coerceIn(0f, 1f) }
    val visibleSlotIndex = StatisticsData.getCurrentSlotIndex()

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

        TimeSlotUsageCard(values = values, visibleSlotIndex = visibleSlotIndex)

        AppUsageCard(apps = appList)
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
private fun TimeSlotUsageCard(
    values: List<Float>,
    visibleSlotIndex: Int,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values, visibleSlotIndex) {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 150),
        )
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600),
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
            visibleSlotIndex = visibleSlotIndex,
        )
    }
}

private val TimeSlotLabels = StatisticsData.SlotLabels

private val TopAppInfoTexts = listOf(
    "이 시간이면 서울 부산 KTX 왕복 8번이에요!",
    "해리포터 시리즈 전편을 두번 반복한 시간보다 많아요!",
    "손흥민이 토트넘에서 뛴 시간과 비슷해요",
    "10층짜리 빌딩을 짓는 시간보다 많아요",
    "이 정도면 아이유 콘서트 시간이랑 맞먹는 시간이에요",
)
private val BarChartHeight = 126.dp
private val BarWidth = 26.dp
private val BarCornerRadius = 2.dp
private val BarLabelGap = 14.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TimeSlotBarChart(
    values: List<Float>,
    progress: Float,
    visibleSlotIndex: Int,
    modifier: Modifier = Modifier,
) {
    val paddedValues = if (values.size >= 8) values.take(8) else values + List(8 - values.size) { 0f }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(BarChartHeight)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
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
                paddedValues.forEachIndexed { index, value ->
                    val isVisible = index <= visibleSlotIndex
                    val barProgress = if (isVisible) ((progress * (visibleSlotIndex + 1)) - index).coerceIn(0f, 1f) else 0f
                    val animatedHeight = if (isVisible) (value * barProgress).coerceIn(0f, 1f) else 0f
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (!isVisible) Modifier.pointerInteropFilter { true } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (isVisible) {
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
    apps: List<StatisticsData.StatsAppItem>,
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
                text = "사용시간 최다 앱",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            IcoDisclaimerInfo(modifier = Modifier.size(18.dp))
        }

        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
            apps.take(5).forEachIndexed { index, app ->
                StatsAppDataViewRow(
                    packageName = app.packageName,
                    name = app.name,
                    usageMinutes = app.usageMinutes,
                    isRestricted = app.isRestricted,
                    infoText = TopAppInfoTexts.getOrNull(index),
                )
            }
        }
    }
}

@Composable
private fun StatsAppDataViewRow(
    packageName: String,
    name: String,
    usageMinutes: String,
    isRestricted: Boolean,
    infoText: String?,
    modifier: Modifier = Modifier,
) {
    val appIcon = rememberAppIconPainter(packageName)
    AppStatusDataViewRow(
        appName = name,
        appIcon = appIcon,
        totalUsageMinutes = usageMinutes,
        modifier = modifier,
        showDangerLabel = isRestricted,
        showLock = isRestricted,
        infoText = infoText,
    )
}
