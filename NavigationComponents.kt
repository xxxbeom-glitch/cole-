import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────────────────────────────────────
// NavDestination — 하단 내비게이션 탭 정의 데이터 클래스
//
// 사용 예시:
//   val items = listOf(
//       NavDestination("홈",      painterResource(R.drawable.ic_home),      painterResource(R.drawable.ic_home_active)),
//       NavDestination("챌린지",  painterResource(R.drawable.ic_challenge), painterResource(R.drawable.ic_challenge_active)),
//       NavDestination("통계",    painterResource(R.drawable.ic_stats),     painterResource(R.drawable.ic_stats_active)),
//       NavDestination("마이",    painterResource(R.drawable.ic_my),        painterResource(R.drawable.ic_my_active)),
//   )
// ─────────────────────────────────────────────────────────────────────────────

data class NavDestination(
    val label: String,
    val icon: Painter,
    val activeIcon: Painter,
)

// ─────────────────────────────────────────────────────────────────────────────
// ColeBottomNavBar
//
// Figma: App Bar (하단 내비게이션 바)
// 아이콘 + 레이블 탭 4개 + 하단 프리미엄 배너로 구성
//
// 사용 예시:
//   ColeBottomNavBar(
//       destinations = items,
//       selectedIndex = currentTab,
//       onTabSelected = { currentTab = it },
//       onPremiumClick = { /* 프리미엄 화면 이동 */ },
//   )
//   // 프리미엄 배너 숨기기
//   ColeBottomNavBar(..., showPremiumBanner = false)
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
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // 탭 영역
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(AppColors.SurfaceBackgroundCard)
                .border(
                    width = 1.dp,
                    color = AppColors.Grey250,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
                .padding(top = 13.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
        ) {
            destinations.forEachIndexed { index, destination ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .width(42.dp)
                        .clickable { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    Icon(
                        painter = if (isSelected) destination.activeIcon else destination.icon,
                        contentDescription = destination.label,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp),
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

        // 프리미엄 배너
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
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = premiumBannerText,
                    style = AppTypography.ButtonSmall.copy(color = AppColors.TextInvert),
                )
                Icon(
                    // 실제 프로젝트에서 R.drawable.ic_arrow_right 등으로 교체
                    painter = androidx.compose.material3.icons.Icons.Default.ChevronRight
                        .let { return@let androidx.compose.ui.res.painterResource(android.R.drawable.arrow_up_float) },
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
//
// Figma: Header - Home (로고 + 알림 아이콘)
// 홈 화면 전용 헤더. 좌측 cole 로고, 우측 알림 아이콘
//
// 사용 예시:
//   ColeHeaderHome(
//       logo = painterResource(R.drawable.ic_cole_logo),
//       notificationIcon = painterResource(R.drawable.ic_notification),
//       hasNotification = true,
//       onNotificationClick = { ... },
//   )
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeHeaderHome(
    logo: Painter,
    notificationIcon: Painter,
    modifier: Modifier = Modifier,
    hasNotification: Boolean = false,
    onNotificationClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 로고
            Icon(
                painter = logo,
                contentDescription = "cole",
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(83.dp)
                    .height(32.dp),
            )

            // 알림 아이콘
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
                    painter = notificationIcon,
                    contentDescription = "알림",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
                // 알림 뱃지
                if (hasNotification) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AppColors.Red300)
                            .align(Alignment.TopEnd),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColeHeaderSub
//
// Figma: Header - Sub (뒤로가기 + 타이틀 + 알림)
// 서브 화면 헤더. 좌측 뒤로가기, 가운데 타이틀, 우측 알림 아이콘
//
// 사용 예시:
//   ColeHeaderSub(
//       title = "통계",
//       backIcon = painterResource(R.drawable.ic_back),
//       notificationIcon = painterResource(R.drawable.ic_notification),
//       onBackClick = { navController.popBackStack() },
//       onNotificationClick = { ... },
//   )
//   // 알림 아이콘 숨기기
//   ColeHeaderSub(title = "설정", backIcon = ..., showNotification = false, onBackClick = { ... })
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ColeHeaderSub(
    title: String,
    backIcon: Painter,
    modifier: Modifier = Modifier,
    notificationIcon: Painter? = null,
    hasNotification: Boolean = false,
    showNotification: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    onNotificationClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 뒤로가기 버튼
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .then(
                        if (onBackClick != null) Modifier.clickable { onBackClick() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = backIcon,
                    contentDescription = "뒤로가기",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp),
                )
            }

            // 타이틀
            Text(
                text = title,
                style = AppTypography.HeadingH3.copy(
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center,
                ),
            )

            // 알림 아이콘 (또는 빈 공간으로 타이틀 중앙 정렬 유지)
            if (showNotification && notificationIcon != null) {
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
                        painter = notificationIcon,
                        contentDescription = "알림",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp),
                    )
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(AppColors.Red300)
                                .align(Alignment.TopEnd),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ColePageIndicator
//
// Figma: Indicator (온보딩 페이지 인디케이터)
// 활성 점은 보라색 pill 형태, 비활성 점은 회색 원형
//
// 사용 예시:
//   ColePageIndicator(pageCount = 4, currentPage = 0)
//   ColePageIndicator(pageCount = 3, currentPage = 1)
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
//
// Figma: Tab - 2 Button / Tab - 3 Button / Tab - 4 Button
// 회색 배경 컨테이너 안에 탭 버튼들이 나열되는 세그먼트 컨트롤
// 탭 개수에 무관하게 items 리스트로 동적 구성
//
// 사용 예시:
//   // 2탭
//   ColeSegmentedTab(
//       items = listOf("시간 지정 제한", "일일 제한 시간"),
//       selectedIndex = tabIndex,
//       onTabSelected = { tabIndex = it },
//   )
//   // 3탭
//   ColeSegmentedTab(
//       items = listOf("주간", "월간", "연간"),
//       selectedIndex = tabIndex,
//       onTabSelected = { tabIndex = it },
//   )
//   // 4탭
//   ColeSegmentedTab(
//       items = listOf("오늘", "주간", "연간", "월간"),
//       selectedIndex = tabIndex,
//       onTabSelected = { tabIndex = it },
//   )
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
