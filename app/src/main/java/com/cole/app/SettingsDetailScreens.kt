package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 설정 메뉴 세부 화면 (Figma MY-02~07)
 */

@Composable
fun AccountManageScreen(onBack: () -> Unit, onProfileClick: () -> Unit, onSocialClick: () -> Unit, onPasswordClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            SettingsRow(iconResId = R.drawable.ic_manageaccount, label = "프로필 정보", onClick = onProfileClick)
            SettingsDivider()
            SettingsRow(iconResId = R.drawable.ic_managesubs, label = "비밀번호 변경", onClick = onPasswordClick)
            SettingsDivider()
            SettingsRow(iconResId = R.drawable.ic_managesubs, label = "연결된 소셜 계정", onClick = onSocialClick)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SubscriptionManageScreen(onBack: () -> Unit, onPaymentClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        // 빈 플레이스홀더 카드 (185dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard),
        ) {}
        SettingsListCard {
            SettingsRow(iconResId = R.drawable.ic_manageaccount, label = "결제 수단 관리", onClick = onPaymentClick)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionSettingsScreen(
    onBack: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onUsageStatsClick: () -> Unit,
    onOverlayClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_manageaccount,
                label = "접근성 서비스",
                badgeText = "허용됨",
                badgeAllowed = true,
                subtitle = "앱 차단 기능에 필요해요",
                onClick = onAccessibilityClick,
            )
            SettingsDivider()
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_manageaccount,
                label = "사용 통계 접근",
                badgeText = "허용됨",
                badgeAllowed = true,
                subtitle = "사용 시간 측정에 필요해요",
                onClick = onUsageStatsClick,
            )
            SettingsDivider()
            SettingsRowWithBadge(
                iconResId = R.drawable.ic_manageaccount,
                label = "다른 앱 위에 표시",
                badgeText = "필요",
                badgeAllowed = false,
                subtitle = "차단 화면 표시에 필요해요",
                onClick = onOverlayClick,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AppInfoScreen(onBack: () -> Unit, onTermsClick: () -> Unit, onPrivacyClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            SettingsRowWithValue(
                iconResId = R.drawable.ic_manageaccount,
                label = "앱 버전",
                value = "1.0",
                showChevron = false,
                onClick = null,
            )
            SettingsDivider()
            SettingsRowWithValue(
                iconResId = R.drawable.ic_managesubs,
                label = "이용약관",
                showChevron = true,
                onClick = onTermsClick,
            )
            SettingsDivider()
            SettingsRowWithValue(
                iconResId = R.drawable.ic_managesubs,
                label = "개인정보처리방침",
                showChevron = true,
                onClick = onPrivacyClick,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun OpenSourceScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        SettingsListCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleTextRow(text = "data 1")
                SimpleTextRow(text = "data 2")
                SimpleTextRow(text = "data 3")
                SimpleTextRow(text = "data 4")
                SimpleTextRow(text = "data 5")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
