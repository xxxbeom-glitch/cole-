package com.cole.app

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat

/**
 * 구독 안내 화면 (MA-01 구독 유도)
 *
 * 캐러셀 동작:
 * - 0페이지(연간): 카드 좌측 = 화면 좌측 16dp, 우측에 월간 카드 살짝 보임
 * - 1페이지(월간): 카드 우측 = 화면 우측 16dp에 딱 붙음
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubscriptionGuideScreen(
    onClose: () -> Unit,
    onSubscribeClick: (isAnnual: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 2 }, initialPage = 0)
    val selectedPlan = pagerState.currentPage
    val view = LocalView.current
    val density = LocalDensity.current
    val navBarBottom = WindowInsets.navigationBars.getBottom(density)
    val ctaHeight = if (navBarBottom > 0) 96.dp else 78.dp

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.isAppearanceLightStatusBars = false
        onDispose {
            insetsController?.isAppearanceLightStatusBars = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF111111))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_subscription_close),
                contentDescription = "닫기",
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onClose() },
                tint = Color.Unspecified,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 110.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            // 타이틀·기능목록 + 카드 (기능목록~카드 22dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
            // 타이틀 + 기능 목록 (타이틀~아래 26dp)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Text(
                    text = "더 강하게,\n더 오래 제한하세요",
                    style = AppTypography.Display3.copy(
                        color = AppColors.TextInvert,
                        fontSize = 30.sp,
                        lineHeight = 42.sp,
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                // 기능 목록
            val features = listOf(
                "시간지정차단 최대 360분",
                "일일사용량제한 최대 180분",
                "루틴 (매일 반복) 기능",
                "일시정지 시간 10분",
                "일시정지 횟수 월 3회",
                "상세 통계",
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                features.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_subscription_check_badge),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified,
                        )
                        Text(
                            text = text,
                            style = AppTypography.Display3.copy(
                                color = AppColors.TextInvert,
                                fontWeight = FontWeight(510),
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                            ),
                        )
                    }
                }
                }
            }

            // ── 구독 플랜 카드 캐러셀 + 디스클라이머 (간격 12dp) ──
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 캐러셀
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    // ★ pageSize를 고정 296dp로 지정
                    // contentPadding start/end=16: 좌측 카드 좌측 16dp, 우측 카드 우측 16dp 고정
                    pageSize = PageSize.Fixed(296.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                    pageSpacing = 12.dp,
                    beyondViewportPageCount = 1,
                ) { page ->
                    when (page) {
                        0 -> SubscriptionPlanCard(
                            title = "연간 구독",
                            originalPrice = "₩46,800",
                            price = "₩28,000",
                            priceUnit = "/연간",
                            badge = "7일 무료체험 + 약 40% 할인",
                            subtitle = "₩2,333/월의 특별한 혜택",
                            isSelected = selectedPlan == 0,
                            isHighlighted = true,
                            onClick = { },
                        )
                        else -> SubscriptionPlanCard(
                            title = "월간 구독",
                            price = "₩3,900",
                            priceUnit = "/월간",
                            isSelected = selectedPlan == 1,
                            isHighlighted = false,
                            onClick = { },
                        )
                    }
                }

                // 고지 문구
                Text(
                    text = "정기 결제는 언제든지 취소 가능하며, 구독 만료 24시간 전까지 해지하지 않으면 자동 갱신됩니다. 구독하지 않아도 Pro 기능을 제외한 모든 기능을 이용할 수 있습니다.",
                    style = AppTypography.Caption2.copy(color = AppColors.TextDisclaimer),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            }
        }

        // CTA 버튼 (네비바 있을 때만 높이 +18dp, windowInsets로 하단 여백)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ctaHeight)
                .background(AppColors.Primary300)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .clickable { onSubscribeClick(selectedPlan == 0) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "지금 시작하면 7일은 무료",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextInvert),
            )
        }
    }
}

/**
 * 구독 플랜 카드
 */
@Composable
private fun SubscriptionPlanCard(
    title: String,
    price: String,
    priceUnit: String,
    isSelected: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    originalPrice: String? = null,
    badge: String? = null,
    subtitle: String? = null,
) {
    val actualBg = if (isHighlighted) Color(0x146C54DD) else Color(0xFF1C1C1C)
    // Figma 766-2814(선택) / 770-2817(미선택): 카드=라디오, 보라색 테두리=선택
    // ColeSelectionCard와 동일: 선택 1.5dp/Primary300, 미선택 1dp
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val borderColorFinal =
        if (isSelected) AppColors.Primary300 else Color(0x33FFFFFF)
    val textAlpha = if (isSelected) 1f else 0.1f

    val badgeOverhang = 14.dp

    Box(
        modifier = modifier
            // ★ 카드 너비는 pager의 PageSize.Fixed(296.dp)가 결정
            // 여기서는 fillMaxWidth()로 pager가 준 너비를 그대로 사용
            .fillMaxWidth()
            .padding(top = badgeOverhang),
    ) {
        // ── 카드 본체 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(actualBg)
                .border(borderWidth, borderColorFinal, RoundedCornerShape(12.dp))
                .clickable { onClick() },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 좌측: 타이틀
                Text(
                    text = title,
                    style = AppTypography.HeadingH3.copy(
                        color = AppColors.TextInvert.copy(alpha = textAlpha),
                        fontWeight = FontWeight(650),
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                    ),
                )

                Spacer(modifier = Modifier.weight(1f))

                // 우측: 가격 정보
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (originalPrice != null) {
                        Text(
                            text = originalPrice,
                            style = AppTypography.Caption2.copy(
                                color = AppColors.TextInvert.copy(alpha = if (isSelected) 0.5f else textAlpha),
                                textDecoration = TextDecoration.LineThrough,
                                fontSize = 14.sp,
                            ),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = price,
                            style = AppTypography.HeadingH1.copy(
                                color = AppColors.TextInvert.copy(alpha = textAlpha),
                                fontWeight = FontWeight(850),
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                            ),
                        )
                        Text(
                            text = priceUnit,
                            style = AppTypography.Caption1.copy(
                                color = AppColors.TextInvert.copy(alpha = if (isSelected) 0.8f else textAlpha),
                                fontWeight = FontWeight(850),
                                fontSize = 12.sp,
                                lineHeight = 32.sp,
                            ),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = AppTypography.Caption2.copy(
                                color = AppColors.TextInvert.copy(alpha = if (isSelected) 0.7f else textAlpha),
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }
            }
        }

        // ── 뱃지: 우측 가격 블록 상단에 정렬 ──
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
                    .offset(y = (-badgeOverhang))
                    .zIndex(1f)
                    .clip(RoundedCornerShape(99.dp))
                    .background(AppColors.Primary300)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Text(
                    text = badge,
                    style = AppTypography.TabSelected.copy(
                        color = AppColors.TextInvert.copy(alpha = textAlpha),
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}
