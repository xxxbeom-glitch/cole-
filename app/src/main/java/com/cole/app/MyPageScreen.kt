package com.cole.app

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 마이 페이지 화면 (Figma MY-01, node 628-3863)
 * - 내 계정: 계정 관리, 구독 관리
 * - 시스템: 알림, 권한 설정
 * - 정보: 정보, 오픈소스
 * - 계정 탈퇴
 */
@Composable
fun MyPageScreen(
    onAccountManageClick: () -> Unit = {},
    onSubscriptionManageClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onAppInfoClick: () -> Unit = {},
    onOpenSourceClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 내 계정
        MyPageSection(title = "내 계정") {
            MyPageRowItem(
                iconResId = R.drawable.ic_manageaccount,
                label = "계정 관리",
                onClick = onAccountManageClick,
            )
            MyPageDivider()
            MyPageRowItem(
                iconResId = R.drawable.ic_managesubs,
                label = "구독 관리",
                onClick = onSubscriptionManageClick,
            )
        }

        // 시스템
        MyPageSection(title = "시스템") {
            MyPageRowItem(
                iconResId = R.drawable.ic_notisetting,
                label = "알림",
                onClick = onNotificationClick,
            )
            MyPageDivider()
            MyPageRowItem(
                iconResId = R.drawable.ic_premission,
                label = "권한 설정",
                onClick = onPermissionClick,
            )
        }

        // 정보
        MyPageSection(title = "정보") {
            MyPageRowItem(
                iconResId = R.drawable.ic_appinfo,
                label = "정보",
                onClick = onAppInfoClick,
            )
            MyPageDivider()
            MyPageRowItem(
                iconResId = R.drawable.ic_opensource,
                label = "오픈소스",
                onClick = onOpenSourceClick,
            )
        }

        // 계정 탈퇴 (단독 카드, 16dp radius per Figma)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(horizontal = 18.dp, vertical = 22.dp),
        ) {
            MyPageRowItem(
                iconResId = R.drawable.ic_delete,
                label = "계정 탈퇴",
                onClick = onWithdrawClick,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MyPageSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = title,
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
            modifier = Modifier.padding(start = 18.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(horizontal = 18.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun MyPageRowItem(
    iconResId: Int,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Primary200),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(iconResId),
                contentDescription = label,
                contentScale = ContentScale.None,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextPrimary,
        )
    }
}

@Composable
private fun MyPageDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFF3F3F3)),
    )
}
