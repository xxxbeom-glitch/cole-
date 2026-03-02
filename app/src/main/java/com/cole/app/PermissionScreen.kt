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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 앱 접근 권한 안내 화면 (풀스크린).
 * Figma 861-4229 원본 디자인 기반.
 */
@Composable
fun PermissionScreen(
    onPrimaryClick: () -> Unit,
    onGhostClick: () -> Unit, // DebugMenuScreen 호환성을 위해 시그니처 유지
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
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, // 가로 중앙 정렬
            verticalArrangement = Arrangement.Center, // 세로(컨텐츠 그룹) 중앙 정렬
        ) {
            Text(
                text = "앱 접근 권한 안내",
                style = AppTypography.Display2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "서비스 이용을 위해 다음의 허용이 필요합니다",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(36.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // 선택 권한 카드
            PermissionCard(
                items = listOf(
                    PermissionItem(
                        iconResId = R.drawable.ic_perm_notification,
                        title = "알림 (선택)",
                        description = "사용 시간 초과 알림과 목표 달성 소식을 알림으로 알리기 위해 필요합니다",
                    ),
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 선택 권한 안내 (디스클라이머)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), // 디스클라이머 가로 중앙 정렬
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_disclaimer_info),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.TextDisclaimer,
                )
                Text(
                    text = "선택 권한을 허용하지 않아도 서비스 이용이 가능합니다",
                    style = AppTypography.Caption2.copy(color = AppColors.TextDisclaimer),
                )
            }
        }

        // 하단 버튼 ("나중에 하기" 단일 구성)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            ColePrimaryButton(
                text = "나중에 하기",
                onClick = onPrimaryClick, // 메인 액션 바인딩
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private data class PermissionItem(
    val iconResId: Int,
    val title: String,
    val description: String,
)

private val PermissionCardShape = RoundedCornerShape(12.dp)
private val PermissionCardShadowColor = Color.Black.copy(alpha = 0.04f)

@Composable
private fun PermissionCard(
    items: List<PermissionItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp, 
                shape = PermissionCardShape, 
                spotColor = PermissionCardShadowColor, 
                ambientColor = PermissionCardShadowColor
            )
            .clip(PermissionCardShape)
            .background(AppColors.White900)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items.forEach { item ->
            PermissionItemRow(
                iconResId = item.iconResId,
                title = item.title,
                description = item.description,
            )
        }
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically, // 아이콘과 텍스트 그룹 간 수직 중앙 정렬로 수정
    ) {
        // 아이콘 리사이징 및 박스 배경 제거. 원본 리소스 직접 사용.
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit,
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
            )
            Text(
                text = description,
                style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
            )
        }
    }
}