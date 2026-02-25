package com.cole.app

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** 간격 테스트: 회원가입 첫페이지, 헤더~콘텐츠 38/36/32/28/26dp, 타이틀~서브 12/10/9/8/7dp */
@Composable
fun DebugSpacingTestScreen(onBack: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    val headerToContentOptions = listOf(38, 36, 32, 28, 26)
    val titleToSubtitleOptions = listOf(12, 10, 9, 8, 7)
    val headerToContentDp = headerToContentOptions[page].dp
    val titleToSubtitleDp = titleToSubtitleOptions[page].dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignUpHeader(title = "회원 가입", onBackClick = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(headerToContentDp))
            Column(verticalArrangement = Arrangement.spacedBy(titleToSubtitleDp)) {
                Text("회원 가입", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "원활한 이용을 위해 아래 내용들을 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "사용하실 이메일 주소를 적어주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                ColeTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "user@mail.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColePrimaryButton(
                text = "다음",
                onClick = { page = (page + 1) % headerToContentOptions.size },
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(text = "돌아가기", onClick = onBack, modifier = Modifier.fillMaxWidth())
        }
    }
}
