package com.aptox.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val AnimationToTextGap = 10.dp
private val TopTextsGap = 8.dp

/**
 * ST-09: 자가테스트 결과 로딩 화면
 * - 상단: 로딩 애니메이션 / 하단: 잠시만 기다려 주세요 · {이름}님의 ~ 확인 중이에요
 * - 앱 분류 캐싱은 스플래시에서 완료됨. 여기서는 UsageStats DB 동기화만 수행.
 * - UsageStats 권한은 온보딩 권한 화면 이후에 부여되므로 이 시점에 실행.
 */
@Composable
fun SelfTestLoadingScreen(
    onFinish: () -> Unit,
    userName: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var triggerCheck by remember { mutableStateOf(false) }
    val displayName = remember(userName) {
        userName.ifBlank { UserPreferencesRepository(context).userName ?: "아영" }
    }

    LaunchedEffect(Unit) {
        val preload = AppDataPreloadRepository(context)
        withContext(Dispatchers.IO) { preload.syncUsageStatsToDb() }
        triggerCheck = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 328.dp)
                .padding(horizontal = 24.dp)
                .offset(y = (-32).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                LoadingToCheckAnimation(
                    triggerCheck = triggerCheck,
                    onComplete = onFinish,
                )
            }
            Spacer(modifier = Modifier.height(AnimationToTextGap))
            Text(
                text = "잠시만 기다려 주세요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(TopTextsGap))
            Text(
                text = "${displayName}님의\n스마트폰 과의존 레벨을\n확인 중이에요",
                style = AppTypography.HeadingH2.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
        }
    }
}
