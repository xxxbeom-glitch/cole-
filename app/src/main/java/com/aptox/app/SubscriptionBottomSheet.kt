package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 구독 결제 진입 바텀시트 (Figma 1566-4815 연간 선택 / 1574-4881 월간 선택).
 * UI 전용 — 결제·스토어 연동 없음.
 */
enum class SubscriptionPlanTier {
    Annual,
    Monthly,
}

/** 가격 옆 /연간·/월간 — 과한 lineHeight 제거 후 Bottom 정렬 + 살짝 위로 보정해 숫자 하단(0 끝선)에 맞춤 */
private val subscriptionPriceSuffixStyle =
    AppTypography.HeadingH3.copy(
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextBody,
    )

private val subscriptionPriceSuffixBaselineOffset = (-3).dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    initialPlan: SubscriptionPlanTier = SubscriptionPlanTier.Annual,
    onStartSubscriptionClick: (SubscriptionPlanTier) -> Unit = {},
) {
    var selected by remember(initialPlan) { mutableStateOf(initialPlan) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SubscriptionHeadlineTitle(
                    highlightPriceForMonthlyEquivalent = selected == SubscriptionPlanTier.Annual,
                )
                Text(
                    text = "더 강력한 디지털 디톡스 시작",
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SubscriptionAnnualCard(
                    selected = selected == SubscriptionPlanTier.Annual,
                    onClick = { selected = SubscriptionPlanTier.Annual },
                )
                SubscriptionMonthlyCard(
                    selected = selected == SubscriptionPlanTier.Monthly,
                    onClick = { selected = SubscriptionPlanTier.Monthly },
                )
            }

            Spacer(modifier = Modifier.height(46.dp))

            AptoxPrimaryButton(
                text = when (selected) {
                    SubscriptionPlanTier.Annual -> "프리미엄 연간 구독 시작하기"
                    SubscriptionPlanTier.Monthly -> "프리미엄 월간 구독 시작하기"
                },
                onClick = {
                    onStartSubscriptionClick(selected)
                    onDismissRequest()
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SubscriptionHeadlineTitle(highlightPriceForMonthlyEquivalent: Boolean) {
    val price = if (highlightPriceForMonthlyEquivalent) "₩3,083" else "₩3,900"
    Text(
        text = "월 $price 으로",
        style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SubscriptionAnnualCard(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) AppColors.Primary300 else AppColors.Grey350
    val badgeBg = if (selected) AppColors.Primary300 else AppColors.Grey350
    val badgeFg = if (selected) AppColors.White900 else AppColors.Grey600

    // 카드 상단 여백 15dp; 뱃지는 우측 정렬 + 카드 오른쪽 가장자리 기준 16dp 안쪽, 세로는 상단 테두리에 맞춤
    val cardTopInset = 15.dp
    val badgeHalfHeight = 14.dp
    val badgeHorizontalInset = 16.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = cardTopInset)
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "연간 구독",
                    style = AppTypography.HeadingH3.copy(color = AppColors.TextBody),
                    modifier = Modifier.widthIn(max = 120.dp),
                )
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "₩46,800",
                        style = AppTypography.BodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = AppColors.TextBody.copy(alpha = 0.6f),
                            textDecoration = TextDecoration.LineThrough,
                        ),
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            text = "₩37,000",
                            style = AppTypography.HeadingH1.copy(
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                color = AppColors.TextBody,
                            ),
                        )
                        Text(
                            text = "/연간",
                            modifier = Modifier.offset(y = subscriptionPriceSuffixBaselineOffset),
                            style = subscriptionPriceSuffixStyle,
                        )
                    }
                    Text(
                        text = "₩3,083/월의 특별한 혜택",
                        style = AppTypography.BodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            color = AppColors.TextBody.copy(alpha = 0.8f),
                        ),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .offset(y = cardTopInset - badgeHalfHeight)
                .padding(end = badgeHorizontalInset),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeBg)
                    .padding(horizontal = 22.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "7일 무료체험 + 20% 할인",
                    style = AppTypography.TabUnselected.copy(
                        fontSize = 14.sp,
                        lineHeight = 14.sp,
                        color = badgeFg,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionMonthlyCard(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) AppColors.Primary300 else AppColors.Grey350
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "월간 구독",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextBody),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "₩3,900",
                    style = AppTypography.HeadingH1.copy(
                        fontSize = 24.sp,
                        lineHeight = 32.sp,
                        color = AppColors.TextBody,
                    ),
                )
                Text(
                    text = "/월간",
                    modifier = Modifier.offset(y = subscriptionPriceSuffixBaselineOffset),
                    style = subscriptionPriceSuffixStyle,
                )
            }
        }
    }
}
