package com.aptox.app

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aptox.app.subscription.PremiumStatusRepository
import com.aptox.app.subscription.SubscriptionManager

/**
 * 탈퇴하기 확인 풀페이지 (Figma MY-02-01)
 * 탈퇴 사유 라디오 선택 → 탈퇴하기 버튼 활성화
 * 프리미엄 구독 중이면 탈퇴 버튼 위에 Play 구독 해지 안내를 표시한다.
 */
@Composable
fun WithdrawConfirmScreen(
    onConfirmWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val premiumFromStore by PremiumStatusRepository.subscribedFlow(context.applicationContext)
        .collectAsState(initial = false)
    val showPremiumSubscriptionNotice =
        SubscriptionManager.isSubscribedWithStore(premiumFromStore, context)

    val reasons = listOf(
        "앱이 필요 없어졌어요",
        "사용하기 불편해요",
        "원하는 기능이 없어요",
        "개인정보 보호 때문에",
    )
    var selectedIndex by remember { mutableStateOf(-1) }
    val isSelected = selectedIndex >= 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 라디오 선택지 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.SurfaceBackgroundCard)
                    .padding(vertical = 22.dp),
            ) {
                reasons.forEachIndexed { index, reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedIndex = index }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AptoxRadioButton(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                        )
                        Text(
                            text = reason,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showPremiumSubscriptionNotice) {
                Text(
                    text = "현재 프리미엄 구독 중입니다. 탈퇴 전 Google Play에서 구독을 직접 해지하지 않으면 결제가 계속될 수 있습니다.",
                    style = AppTypography.Disclaimer.copy(color = AppColors.TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            AptoxPrimaryButton(
                text = "탈퇴하기",
                onClick = onConfirmWithdraw,
                enabled = isSelected,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
