package com.aptox.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aptox.app.ui.components.AptoxToast

private val toastMessages = listOf(
    "로그인을 취소했습니다",
    "네트워크 연결을 확인해 주세요",
    "저장되었습니다",
    "최소 7일치의 데이터가 누적 된 후 보실 수 있어요",
)

@Composable
fun ToastAnimationTestScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf(toastMessages[0]) }
    var replayKey by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AptoxGhostButton(text = "← 돌아가기", onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "토스트 팝업 테스트",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "버튼을 눌러 토스트 팝업을 확인하세요.",
            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
        )
        Spacer(modifier = Modifier.height(8.dp))

        toastMessages.forEach { msg ->
            AptoxPrimaryButton(
                text = msg,
                onClick = {
                    toastMessage = msg
                    toastVisible = true
                    replayKey++
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    AptoxToast(
        message = toastMessage,
        visible = toastVisible,
        onDismiss = { toastVisible = false },
        replayKey = replayKey,
    )
}
