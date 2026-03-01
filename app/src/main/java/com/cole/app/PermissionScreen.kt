package com.cole.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 앱 접근 권한 안내 화면 (풀스크린).
 * Figma 861-4229 기반.
 * 앱 사용정보 접근, 다른 앱 위에 표시, 접근성 서비스(필수), 알림(선택) 안내.
 */
@Composable
fun PermissionScreen(
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "앱 접근 권한 안내",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "서비스 이용을 위해 다음의 허용이 필요합니다",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 필수 권한 카드
            PermissionCard(
                items = listOf(
                    PermissionItem(
                        iconResId = R.drawable.ic_perm_usage,
                        title = "앱 사용정보 접근 (필수)",
                        description = "앱별 사용 시간 측정과 사용 제한 기능 작동에 필요한 권한입니다",
                    ),
                    PermissionItem(
                        iconResId = R.drawable.ic_perm_overlay,
                        title = "다른 앱 위에 표시 (필수)",
                        description = "사용 제한 시 안내 화면을 화면 위에 표시하기 위해 필요합니다",
                    ),
                    PermissionItem(
                        iconResId = R.drawable.ic_perm_accessibility,
                        title = "접근성 서비스 (필수)",
                        description = "제한 중인 앱으로 이동할 때 사용 제한 화면을 표시하기 위해 필요합니다",
                    ),
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 선택 권한 카드
            PermissionCard(
                items = listOf(
                    PermissionItem(
                        iconResId = R.drawable.ic_perm_notification,
                        title = "알림 (선택)",
                        description = "사용 시간 초과 알림과 목표 달성 소식을 알림으로 알리기 위해 필요합니다",
                    ),
                ),
                footerContent = {
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_disclaimer_info),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppColors.TextCaption,
                        )
                        Text(
                            text = "① 선택 권한을 허용하지 않아도 서비스 이용이 가능합니다",
                            style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 하단 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            ColePrimaryButton(
                text = "나중에 하기",
                onClick = onNextClick,
            )
        }
    }
}

private data class PermissionItem(
    val iconResId: Int,
    val title: String,
    val description: String,
)

@Composable
private fun PermissionCard(
    items: List<PermissionItem>,
    modifier: Modifier = Modifier,
    footerContent: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundCard, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items.forEach { item ->
            PermissionItemRow(
                iconResId = item.iconResId,
                title = item.title,
                description = item.description,
            )
        }
        footerContent?.invoke()
    }
}

@Composable
private fun PermissionItemRow(
    iconResId: Int,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
        }
    }
}

