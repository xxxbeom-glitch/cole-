package com.aptox.app

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 마이 페이지 화면 (Figma MY-01, node 628-3863)
 * - 내 계정: 계정 관리
 * - 제한 앱 관리: 앱 카테고리 수정, 앱 사용제한 기록
 * - 시스템: 알림, 권한 설정
 */
@Composable
fun MyPageScreen(
    onAccountManageClick: () -> Unit = {},
    onAppCategoryEditClick: () -> Unit = {},
    onAppRestrictionHistoryClick: () -> Unit = {},
    onSubscriptionManageClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onPermissionClick: () -> Unit = {},
    onBugReportClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp)
            .padding(bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
            // 내 계정
            MyPageSection(title = "내 계정") {
                MyPageRowItem(
                    iconResId = R.drawable.ic_manageaccount,
                    label = "계정 관리",
                    onClick = onAccountManageClick,
                )
                MyPageDivider()
                MyPageRowItem(
                    iconResId = R.drawable.ic_settings_subscription_manage,
                    label = "구독 관리",
                    onClick = onSubscriptionManageClick,
                )
            }

            // 제한 앱 관리
            MyPageSection(title = "제한 앱 관리") {
                MyPageRowItem(
                    iconResId = R.drawable.ic_settings_app_category,
                    label = "앱 카테고리 수정",
                    onClick = onAppCategoryEditClick,
                )
                MyPageDivider()
                MyPageRowItem(
                    iconResId = R.drawable.ic_app_restriction_history,
                    label = "앱 사용제한 기록",
                    onClick = onAppRestrictionHistoryClick,
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
                    iconResId = R.drawable.ic_terms,
                    label = "이용약관",
                    onClick = onTermsClick,
                )
                MyPageDivider()
                MyPageRowItem(
                    iconResId = R.drawable.ic_privacy,
                    label = "개인정보처리방침",
                    onClick = onPrivacyClick,
                )
                MyPageDivider()
                MyPageRowItem(
                    iconResId = R.drawable.ic_bug_report,
                    label = "버그신고",
                    onClick = onBugReportClick,
                )
            }
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
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
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
