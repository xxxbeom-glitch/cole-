package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 알림 내역 데이터 모델 (Figma 1068-3998)
 * - type: Firestore 저장 타입 ("badge" 등)
 * - typeLabel: 화면 표시용 ("챌린지 성공", WARNING 등)
 * - timeText: 방금, 10분 전 등
 * - title: 첫 줄 메시지
 * - body: 둘째 줄 메시지 (선택)
 * - badgeId: 뱃지 알림 시 badgeId (탭 시 뱃지 화면 이동용)
 */
data class NotificationHistoryItem(
    val type: String,
    val typeLabel: String,
    val timeText: String,
    val title: String,
    val body: String? = null,
    val badgeId: String? = null,
    /** 알림 탭 시 이동 대상 (예: statistics_weekly) */
    val navTarget: String? = null,
)

// [레거시] 샘플 데이터 — Firestore 실시간 연동으로 대체됨
// fun sampleNotificationHistoryItems(count: Int): List<NotificationHistoryItem> = ...

private val NotificationCardShape = RoundedCornerShape(12.dp)
private val NotificationCardShadowColor = Color.Black.copy(alpha = 0.06f)

/**
 * 알림 내역 화면 (Figma 1068-3998, 1068-4394)
 * - Firestore users/{userId}/notifications 실시간 구독
 * - 헤더: 뒤로가기 + "알림" + 알림 아이콘
 * - 리스트: 카드 형태, 18dp 간격, 탭 시 onItemClick 호출
 * - 빈 상태: "받은 알림 내역이 없어요" 중앙 표시
 * - 로딩: 중앙 CircularProgressIndicator
 */
@Composable
fun NotificationHistoryScreen(
    userId: String?,
    onBack: () -> Unit,
    onItemClick: (NotificationHistoryItem) -> Unit = {},
    onNotificationSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val repo = remember { NotificationRepository() }
    var items by remember { mutableStateOf<List<NotificationHistoryItem>?>(null) }

    LaunchedEffect(userId) {
        if (userId != null) {
            repo.getNotificationsFlow(userId).collect { items = it }
        } else {
            items = emptyList()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 10.dp),
    ) {
        // 헤더: 뒤로가기 + "알림" (다른 헤더와 동일: statusBars + 10.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "뒤로가기",
                modifier = Modifier
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onBack() },
                tint = AppColors.TextPrimary,
            )
            Text(
                text = "알림",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.size(36.dp))
        }

        when {
            items == null && userId != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = AppColors.Primary300,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            items.isNullOrEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "받은 알림 내역이 없어요",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 40.dp),
                    )
                }
            }
            else -> {
                val list = items!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 24.dp,
                        bottom = 80.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(list) { item ->
                        NotificationHistoryCard(
                            item = item,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationHistoryCard(
    item: NotificationHistoryItem,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, NotificationCardShape, false, NotificationCardShadowColor, NotificationCardShadowColor)
            .clip(NotificationCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.typeLabel,
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
            )
            Text(
                text = item.timeText,
                style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                textAlign = TextAlign.End,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                text = item.title,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
            )
            if (item.body != null) {
                Text(
                    text = item.body,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                )
            }
        }
    }
}
