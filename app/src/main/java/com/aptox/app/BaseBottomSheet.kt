package com.aptox.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
    subtitleContent: @Composable (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    primaryButtonEnabled: Boolean = true,
    dismissOnPrimaryClick: Boolean = true,
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
        scrimColor = Color.Black.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            // 상단 패딩 56dp (Figma pt-56)
            Spacer(modifier = Modifier.height(40.dp))

            // 헤더: title + subtitle
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                )
                if (subtitleContent != null) {
                    subtitleContent()
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    )
                }
            }

            // 컨텐츠와 버튼 사이 gap
            Spacer(modifier = Modifier.height(22.dp))
            content()
            Spacer(modifier = Modifier.height(36.dp))

            // 버튼 영역 (회원가입 플로우와 동일: bottom 24.dp + windowInsets)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AptoxPrimaryButton(
                    text = primaryButtonText,
                    enabled = primaryButtonEnabled,
                    onClick = {
                        if (dismissOnPrimaryClick) {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) onPrimaryClick()
                            }
                        } else {
                            onPrimaryClick()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (secondaryButtonText != null) {
                    AptoxGhostButton(
                        text = secondaryButtonText,
                        onClick = {
                            // 취소/뒤로가기: 즉시 콜백 호출 (상태 변경으로 시트 제거)
                            // invokeOnCompletion 사용 시 Primary 콜백과 충돌해 일시정지가 적용되는 버그 방지
                            (onSecondaryClick ?: onDismissRequest)()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
