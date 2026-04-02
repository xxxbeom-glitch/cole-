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
import androidx.compose.material3.TextButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    tertiaryButtonText: String? = null,
    onTertiaryClick: (() -> Unit)? = null,
    /**
     * false이면 스와이프/드래그·스크림 탭·시스템 백으로는 닫히지 않으며,
     * [sheetState.hide]를 호출하는 본문 버튼(Primary/Tertiary)으로만 시트가 내려갈 수 있음.
     */
    allowGestureDismiss: Boolean = true,
    content: @Composable () -> Unit,
) {
    var allowHideTransition by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (!allowGestureDismiss && target == SheetValue.Hidden) allowHideTransition else true
        },
    )
    val scope = rememberCoroutineScope()
    val sheetProperties = if (allowGestureDismiss) {
        ModalBottomSheetDefaults.properties
    } else {
        ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.6f),
        properties = sheetProperties,
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
                            allowHideTransition = true
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                allowHideTransition = false
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
                            (onSecondaryClick ?: onDismissRequest)()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (tertiaryButtonText != null && onTertiaryClick != null) {
                    TextButton(
                        onClick = {
                            allowHideTransition = true
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                allowHideTransition = false
                                if (!sheetState.isVisible) onTertiaryClick()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = tertiaryButtonText,
                            style = AppTypography.ButtonLarge.copy(color = AppColors.Red300),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
