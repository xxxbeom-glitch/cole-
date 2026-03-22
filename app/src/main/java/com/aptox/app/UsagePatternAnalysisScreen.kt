package com.aptox.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Figma Shadow/Card: X=0, Y=0, Blur=6, Spread=0, #000000 6% */
private val CardShadowColor = Color.Black.copy(alpha = 0.06f)

private enum class CenterPhase { TITLE, CARDS, START_BUTTON }

/** 카드 1용: 최다 사용 앱 이름, 7일 총 시간(h), 일평균(h) */
private data class Card1Data(val appName: String, val totalHours: Int, val averageHours: Double)

/** 카드 2용: 메시지, 카테고리별 막대 비율(0~1) */
private data class Card2Data(val message: String, val categoryBars: List<Pair<String, Float>>)

/** 애니메이션 설정: duration(ms), stagger(ms) */
private data class SmoothnessConfig(
    val durationMs: Int,
    val staggerMs: Int,
    val label: String = "",
)

/** Figma 1127-5788, 1127-5822: 스마트폰 사용 패턴 분석 화면 */
@Composable
fun UsagePatternAnalysisScreen(
    userName: String,
    onFinish: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var titleVisible by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf(CenterPhase.TITLE) }
    var card1Show by remember { mutableStateOf(false) }
    var card2Show by remember { mutableStateOf(false) }
    var card3Show by remember { mutableStateOf(false) }
    var card1Data by remember { mutableStateOf<Card1Data?>(null) }
    var card2Data by remember { mutableStateOf<Card2Data?>(null) }

    LaunchedEffect(Unit) {
        val (startMs, endMs) = StatisticsData.getLastNDaysRange(7, 0).let { it.first to it.second }
        val apps = withContext(Dispatchers.IO) {
            StatisticsData.loadAppUsageForAllowedCategories(context, startMs, endMs)
        }
        val topApp = apps.maxByOrNull { it.usageMs }
        card1Data = topApp?.let { app ->
            val totalHours = (app.usageMs / (1000 * 60 * 60)).toInt()
            val averageHours = totalHours / 7.0
            Card1Data(appName = app.name, totalHours = totalHours, averageHours = averageHours)
        }
        val usageByCategory = apps
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, list) -> list.sumOf { it.usageMs } }
        val totalMs = usageByCategory.values.sum()
        val topCategory = usageByCategory.maxByOrNull { it.value }?.key
        card2Data = if (usageByCategory.isNotEmpty() && totalMs > 0) {
            val maxMs = usageByCategory.values.maxOrNull() ?: 1L
            val bars = usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / maxMs).coerceIn(0f, 1f) }
            val message = topCategory?.let {
                "${userName}님은 $it 앱을 상당히 많이 사용하시고 계세요"
            } ?: "사용 패턴을 확인해보세요"
            Card2Data(message = message, categoryBars = bars)
        } else null
    }

    val config = SmoothnessConfig(800, 550, "보통")
    val ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val cardSpringSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    LaunchedEffect(Unit) {
        titleVisible = true
        delay(600 + 1600)
        phase = CenterPhase.CARDS
        // 모든 카드 한꺼번에 표시
        card1Show = true
        card2Show = true
        card3Show = true
        // 4초 대기 후 카드 하나씩 사라지며 다음 페이지로
        delay(4000)
        card1Show = false
        delay(350)
        card2Show = false
        delay(350)
        card3Show = false
        delay(400)
        phase = CenterPhase.START_BUTTON
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 타이틀 ↔ 카드 그룹 전환 (화면 중앙에 딱 위치)
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    fadeIn(animationSpec = tween(config.durationMs, easing = ease)) togetherWith
                        fadeOut(animationSpec = tween(config.durationMs, easing = ease))
                },
                label = "centerPhase",
            ) { state ->
                when (state) {
                    CenterPhase.TITLE -> AnimatedVisibility(
                        visible = titleVisible,
                        enter = fadeIn(animationSpec = tween(config.durationMs, easing = ease)),
                        exit = fadeOut(animationSpec = tween(config.durationMs, easing = ease)),
                    ) {
                        Column(
                            modifier = Modifier.width(328.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${userName}의 일주일간",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "스마트폰 사용패턴을",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "분석해봤어요",
                                style = AppTypography.Display3.copy(color = AppColors.TextPrimary),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    CenterPhase.CARDS -> {
                        val offset1 by animateDpAsState(
                            targetValue = if (card1Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card1Offset",
                        )
                        val alpha1 by animateFloatAsState(
                            targetValue = if (card1Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card1Alpha",
                        )
                        val offset2 by animateDpAsState(
                            targetValue = if (card2Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card2Offset",
                        )
                        val alpha2 by animateFloatAsState(
                            targetValue = if (card2Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card2Alpha",
                        )
                        val offset3 by animateDpAsState(
                            targetValue = if (card3Show) 0.dp else 60.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                            label = "card3Offset",
                        )
                        val alpha3 by animateFloatAsState(
                            targetValue = if (card3Show) 1f else 0f,
                            animationSpec = cardSpringSpec,
                            label = "card3Alpha",
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset1)
                                    .alpha(alpha1),
                            ) {
                                val data = card1Data
                                if (data != null) {
                                    UsagePatternCard1Animated(
                                        userName = userName,
                                        appName = data.appName,
                                        totalHours = data.totalHours,
                                        recommendedHours = 1.0,
                                        averageHours = data.averageHours,
                                        config = config,
                                        ease = ease,
                                    )
                                } else {
                                    UsagePatternCard1Empty()
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset2)
                                    .alpha(alpha2),
                            ) {
                                val data = card2Data
                                if (data != null) {
                                    UsagePatternCard2Animated(
                                        userName = userName,
                                        message = data.message,
                                        categoryBars = data.categoryBars,
                                        config = config,
                                        ease = ease,
                                    )
                                } else {
                                    UsagePatternCard2Empty()
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = offset3)
                                    .alpha(alpha3),
                            ) {
                                UsagePatternCard3Animated(userName = userName, config = config, ease = ease)
                            }
                        }
                    }
                    CenterPhase.START_BUTTON -> Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Spacer(modifier = Modifier.height(0.dp))
                        AptoxPrimaryButton(
                            text = "시작하기",
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

private val stepSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow,
)

/** 카드 1: 앱 사용량 요약 (유튜브 304시간) - 카드 → 타이틀 → 본문 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard1Animated(
    userName: String,
    appName: String,
    totalHours: Int,
    recommendedHours: Double,
    averageHours: Double,
    config: SmoothnessConfig,
    ease: Easing,
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        step = 1
        delay(config.staggerMs.toLong())
        step = 2
        delay(config.staggerMs.toLong())
        step = 3
    }

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append(appName) }
                        append("를 ")
                        withStyle(SpanStyle(color = AppColors.Primary300)) { append("${totalHours}시간") }
                        append("이나\n사용하셨어요")
                    },
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            AnimatedVisibility(
                visible = step >= 2,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = "${appName} 하루 권장 시청 사용 시간은 ${recommendedHours.toInt()}시간이에요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
            AnimatedVisibility(
                visible = step >= 3,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = "${userName}님은 하루 평균 ${"%.1f".format(averageHours)}시간 사용하셨어요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
        }
    }
}

@Composable
private fun UsagePatternCard1Empty() {
    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "사용량 데이터가 없어요",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
        )
    }
}

@Composable
private fun UsagePatternCard2Empty() {
    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "카테고리 데이터가 없어요",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
        )
    }
}

/** 카드 1: 앱 사용량 요약 (유튜브 304시간) - 기본 버전 */
@Composable
private fun UsagePatternCard1(
    userName: String,
    appName: String,
    totalHours: Int,
    recommendedHours: Double,
    averageHours: Double,
) {
    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = AppColors.Primary300)) { append(appName) }
                    append("를 ")
                    withStyle(SpanStyle(color = AppColors.Primary300)) { append("${totalHours}시간") }
                    append("이나\n사용하셨어요")
                },
                style = AppTypography.HeadingH3.copy(
                    color = AppColors.TextPrimary,
                    lineHeight = AppTypography.HeadingH3.lineHeight,
                ),
            )
            Text(
                text = "${appName} 하루 권장 시청 사용 시간은 ${recommendedHours.toInt()}시간이에요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
            Text(
                text = "${userName}님은 하루 평균 ${"%.1f".format(averageHours)}시간 사용하셨어요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
        }
    }
}

/** 카드 2: 카테고리별 사용량 막대 - 타이틀 → 막대 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard2Animated(
    userName: String,
    message: String,
    categoryBars: List<Pair<String, Float>>,
    config: SmoothnessConfig,
    ease: Easing,
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        step = 1
        categoryBars.forEachIndexed { i, _ ->
            delay(config.staggerMs.toLong())
            step = 2 + i
        }
    }

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = message,
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                categoryBars.forEachIndexed { i, (label, ratio) ->
                    AnimatedVisibility(
                        visible = step >= 2 + i,
                        enter = fadeIn(animationSpec = stepSpringSpec),
                        exit = fadeOut(animationSpec = stepSpringSpec),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = label,
                                style = AppTypography.Caption1.copy(color = AppColors.TextTertiary),
                                modifier = Modifier.width(36.dp),
                            )
                            CategoryBar(ratio = ratio, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(
    ratio: Float,
    modifier: Modifier = Modifier,
) {
    val clampedRatio = ratio.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(AppColors.Grey100),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedRatio)
                .fillMaxHeight()
                .clip(RoundedCornerShape(7.dp))
                .background(AppColors.Primary300),
        )
    }
}

/** 12개 슬롯(0~2,2~4,...,22~24시) 인덱스 → 시간대 문구 */
private fun formatTimeSlotInsight(maxIdx: Int): String = when (maxIdx) {
    0 -> "새벽 0시부터 2시까지"
    1 -> "새벽 2시부터 4시까지"
    2 -> "새벽 4시부터 6시까지"
    3 -> "아침 6시부터 8시까지"
    4 -> "오전 8시부터 10시까지"
    5 -> "오전 10시부터 12시까지"
    6 -> "낮 12시부터 2시까지"
    7 -> "오후 2시부터 4시까지"
    8 -> "오후 4시부터 6시까지"
    9 -> "저녁 6시부터 8시까지"
    10 -> "저녁 8시부터 10시까지"
    11 -> "밤 10시부터 12시까지"
    else -> "사용량이 가장 많은 시간대"
}

/** 카드 3: 시간대별 그래프 - 타이틀 → 차트 순 단계별 애니메이션 */
@Composable
private fun UsagePatternCard3Animated(
    userName: String,
    config: SmoothnessConfig,
    ease: Easing,
) {
    val context = LocalContext.current
    var timeSlotMinutes by remember { mutableStateOf<List<Long>?>(null) }
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            val (startMs, endMs) = StatisticsData.getLastNDaysRange(7, 0).let { (s, e, _) -> s to e }
            StatisticsData.loadTimeSlot12Minutes(context, startMs, endMs, 0)
        }
        timeSlotMinutes = loaded
    }

    LaunchedEffect(Unit) {
        step = 1
        delay(config.staggerMs.toLong())
        step = 2
    }

    val padded = timeSlotMinutes?.let { if (it.size >= 12) it.take(12) else it + List(12 - it.size) { 0L } }
        ?: List(12) { 0L }
    val maxIdx = padded.indices.maxByOrNull { padded[it] }?.takeIf { padded[it] > 0 } ?: -1
    val timeSlotMaxMinutes = 120L
    val normalized = padded.map { (it.toFloat() / timeSlotMaxMinutes).coerceIn(0f, 1f) }
    val insightText = if (maxIdx >= 0) "${formatTimeSlotInsight(maxIdx)}\n사용량이 가장 많았어요" else "사용량 데이터가 없어요"

    UsagePatternCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
            AnimatedVisibility(
                visible = step >= 1,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                Text(
                    text = insightText,
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextPrimary,
                        lineHeight = AppTypography.HeadingH3.lineHeight,
                    ),
                )
            }
            AnimatedVisibility(
                visible = step >= 2,
                enter = fadeIn(animationSpec = stepSpringSpec),
                exit = fadeOut(animationSpec = stepSpringSpec),
            ) {
                TimeSlotBarChartComponent(
                    values = normalized,
                    maxValueIdx = maxIdx,
                    showSpeechBubble = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun UsagePatternCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val cardShape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                clip = false,
                ambientColor = CardShadowColor,
                spotColor = CardShadowColor,
            )
            .background(AppColors.SurfaceBackgroundCard, cardShape)
            .padding(horizontal = 16.dp, vertical = 26.dp),
    ) {
        content()
    }
}
