package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// NavDestination — 하단 내비게이션 탭 정의
// Figma: App Bar (홈, 챌린지, 통계, 마이)
// 사용자 제공 PNG (Group_88 시리즈) drawable 사용, 아이콘 24dp
// ─────────────────────────────────────────────────────────────────────────────

data class NavDestination(
    val label: String,
    val iconResId: Int,
    val activeIconResId: Int = iconResId,
)

// ─────────────────────────────────────────────────────────────────────────────
// ColeBottomNavBar
// Figma: App Bar - 1~4 On
// 80dp 영역, 아이콘 24dp / Caption1 12sp, 선택 시 Primary300 / 비선택 Grey400
// 상단 모서리 둥글게 (bottom nav이므로 화면 하단에서 위쪽 모서리가 둥글게)
// 하단 프리미엄 배너 42dp, #2b2b2b
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeBottomNavBar(
    destinations: List<NavDestination>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showPremiumBanner: Boolean = true,
    onPremiumClick: (() -> Unit)? = null,
    premiumBannerText: String = "더 강하게 제한하고 싶다면 프리미엄으로",
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AppColors.Grey250))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(79.dp)
                .background(AppColors.SurfaceBackgroundCard)
                .padding(top = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(42.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEachIndexed { index, destination ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) destination.activeIconResId else destination.iconResId
                        ),
                        contentDescription = destination.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = destination.label,
                        style = AppTypography.Caption1.copy(
                            color = if (isSelected) AppColors.Primary300 else AppColors.Grey400,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }

        if (showPremiumBanner) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(Color(0xFF2B2B2B))
                    .then(
                        if (onPremiumClick != null) Modifier.clickable { onPremiumClick() }
                        else Modifier
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = premiumBannerText,
                    style = AppTypography.ButtonSmall.copy(color = AppColors.TextInvert),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = AppColors.TextInvert,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeHeaderHome
// Figma: Header - Home (로고 + 알림)
// 56dp, 좌측 로고 83x32, 우측 알림 36x36(아이콘 24x24)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeHeaderHome(
    logo: Painter,
    modifier: Modifier = Modifier,
    hasNotification: Boolean = false,
    onNotificationClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = logo,
                contentDescription = "cole",
                tint = Color.Unspecified,
                modifier = Modifier.width(83.dp).height(32.dp),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (onNotificationClick != null) Modifier.clickable { onNotificationClick() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        id = if (hasNotification) R.drawable.ic_notification_on else R.drawable.ic_notification_off
                    ),
                    contentDescription = "알림",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeHeaderTitleWithNotification
// Figma: Header - Sub (Title with Notification)
// 뒤로가기 없음, 타이틀 좌측 정렬 + 알림 우측
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeHeaderTitleWithNotification(
    title: String,
    modifier: Modifier = Modifier,
    hasNotification: Boolean = false,
    onNotificationClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.size(36.dp))
            Text(
                text = title,
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (onNotificationClick != null) Modifier.clickable { onNotificationClick() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        id = if (hasNotification) R.drawable.ic_notification_on else R.drawable.ic_notification_off
                    ),
                    contentDescription = "알림",
                    modifier = Modifier.size(36.dp),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeHeaderSub
// Figma: Header - Sub with Back (뒤로가기 + 타이틀 + 알림)
// 56dp, 16dp 패딩, 뒤로가기 36x36(아이콘 24x24), 타이틀 HeadingH3 18sp, 알림 36x36
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeHeaderSub(
    title: String,
    modifier: Modifier = Modifier,
    backIcon: Painter? = null,
    onBackClick: (() -> Unit)? = null,
    actionIcon: Painter? = null,
    onActionClick: (() -> Unit)? = null,
    showNotification: Boolean = true,
    hasNotification: Boolean = true,
    onNotificationClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                if (backIcon != null) {
                    Icon(
                        painter = backIcon,
                        contentDescription = "뒤로가기",
                        modifier = Modifier
                            .size(36.dp)
                            .clickable(enabled = onBackClick != null) { onBackClick?.invoke() },
                        tint = AppColors.TextPrimary,
                    )
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            Text(
                text = title,
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterVertically),
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        when {
                            showNotification && onNotificationClick != null -> Modifier.clickable { onNotificationClick() }
                            actionIcon != null && onActionClick != null -> Modifier.clickable { onActionClick() }
                            else -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    showNotification -> Icon(
                        painter = painterResource(
                            id = if (hasNotification) R.drawable.ic_notification_on else R.drawable.ic_notification_off
                        ),
                        contentDescription = "알림",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Unspecified,
                    )
                    actionIcon != null -> Icon(
                        painter = actionIcon,
                        contentDescription = "액션",
                        modifier = Modifier.size(24.dp),
                        tint = AppColors.TextPrimary,
                    )
                    else -> Spacer(modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColePageIndicator
// Figma: Indicator — 활성 22x6 pill TextHighlight, 비활성 6x6 Grey350, gap 6dp
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .then(
                        if (isActive) Modifier.width(22.dp) else Modifier.size(6.dp)
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isActive) AppColors.TextHighlight else AppColors.Grey350
                    ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeSegmentedTab
// Figma: Tab - 2/3/4 Button
// tab-bg #eee, padding 6dp, gap 4dp, item h 40dp, 선택 White/1f1f1f Bold, 비선택 #eee/#858585 Medium
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeSegmentedTab(
    items: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.InteractiveTabTabBg)
            .padding(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) AppColors.InteractiveTabSelected
                            else AppColors.InteractiveTabUnselected
                        )
                        .clickable { onTabSelected(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = if (isSelected) AppTypography.TabSelected.copy(
                            color = AppColors.InteractiveTabTextSelected,
                            textAlign = TextAlign.Center,
                        ) else AppTypography.TabUnselected.copy(
                            color = AppColors.InteractiveTabTextUnselected,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}
