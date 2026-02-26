package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 앱 아이콘 테스트 화면 (디버그 전용)
 * Squircle + #000000 50% 오버레이 + 중앙 자물쇠 스타일 앱 아이콘을 테스트.
 * 넷플릭스 앱 아이콘을 사용.
 */
@Composable
fun DebugAppIconTestScreen(onBack: () -> Unit) {
    val netflixPainter = rememberDefaultAppIconPainter()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ColeGhostButton(text = "돌아가기", onClick = onBack)
        Text(
            text = "앱 아이콘 테스트",
            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "Squircle + #000000 50% 오버레이 + 중앙 자물쇠 (넷플릭스)",
            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
        )

        // 단일 아이콘 크기별
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "크기별 아이콘",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppIconSquircleLock(appIcon = netflixPainter, iconSize = 40.dp)
                    Text(text = "40dp", style = AppTypography.Caption2.copy(color = AppColors.TextCaption))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppIconSquircleLock(appIcon = netflixPainter, iconSize = 48.dp)
                    Text(text = "48dp", style = AppTypography.Caption2.copy(color = AppColors.TextCaption))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppIconSquircleLock(appIcon = netflixPainter, iconSize = 56.dp)
                    Text(text = "56dp", style = AppTypography.Caption2.copy(color = AppColors.TextCaption))
                }
            }
        }

        // 리스트 행 스타일 (AppStatusRow 비슷)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "진행 중인 앱 행 (목업)",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIconSquircleLock(appIcon = netflixPainter, iconSize = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("넷플릭스", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
                    Text("14분/30분 사용 중", style = AppTypography.Caption2.copy(color = AppColors.TextHighlight))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIconSquircleLock(appIcon = netflixPainter, iconSize = 56.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text("넷플릭스", style = AppTypography.BodyMedium.copy(color = AppColors.TextBody))
                    Text("09:50 일시 정지 중", style = AppTypography.Caption2.copy(color = AppColors.Red300))
                }
            }
        }

        // placeholder로 fallback 시
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.SurfaceBackgroundCard)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Placeholder (앱 미설치 시)",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextSecondary),
            )
            AppIconSquircleLock(
                appIcon = painterResource(R.drawable.ic_app_placeholder),
                iconSize = 56.dp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
