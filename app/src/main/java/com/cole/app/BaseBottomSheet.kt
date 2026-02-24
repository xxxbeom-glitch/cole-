package com.cole.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 기본형 바텀시트 (Figma 511-2749)
 * - title: 필수
 * - subtitle: 옵션
 * - primaryButtonText: 필수
 * - secondaryButtonText: 옵션 (없으면 버튼 1개)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    onPrimaryClick: () -> Unit,
    primaryButtonText: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    primaryButtonEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .heightIn(min = 100.dp)
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            // 상단 패딩 56dp (Figma pt-56)
            Spacer(modifier = Modifier.height(40.dp))

            // 헤더: title + subtitle
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    )
                }
            }

            // 컨텐츠와 버튼 사이 gap 46dp (Figma)
            // weight(1f)로 유한 높이를 부여해 verticalScroll 가능
            Spacer(modifier = Modifier.height(22.dp))
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
            Spacer(modifier = Modifier.height(46.dp))

            // 버튼 영역 (회원가입 플로우와 동일: bottom 24.dp + windowInsets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColePrimaryButton(
                    text = primaryButtonText,
                    enabled = primaryButtonEnabled,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) onPrimaryClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (secondaryButtonText != null) {
                    ColeGhostButton(
                        text = secondaryButtonText,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) (onSecondaryClick ?: onDismissRequest)()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
