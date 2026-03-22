package com.aptox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.aptox.app.AppTypography

private val ToastBackgroundColor = Color(0x3C000000)
private val ToastMinWidth = 100.dp
private val ToastHorizontalPadding = 20.dp
private val ToastVerticalPadding = 8.dp
private val ToastShape = RoundedCornerShape(999.dp)

/** 바텀바 위 여백 (바텀바 높이 위에 추가로 띄울 간격) */
private val ToastAboveBottomBarSpacing = 40.dp

private val AutoDismissDelayMs = 2000L
private val AnimationDurationMs = 300

/**
 * 바텀 네비게이션 바의 실측 높이를 AptoxToast에 전달하는 CompositionLocal.
 * MainFlowHost에서 onGloballyPositioned로 측정한 값을 제공하고,
 * 바텀바가 없는 화면에서는 기본값 0.dp가 사용됨.
 */
val LocalBottomBarHeight = compositionLocalOf { 0.dp }

/**
 * Figma 기준 커스텀 오버레이 Toast.
 * - Popup으로 구현하여 어느 화면에서 호출해도 항상 전체 화면 기준으로 위치를 잡음
 * - 위치: 시스템 네비게이션 바 + 앱 바텀바 높이 + 40dp 위
 * - 등장: fade + slideInVertically (300ms)
 * - 사라짐: fadeOut (300ms), 2초 후 onDismiss
 * @param replayKey 같은 문구로 연속 표시할 때마다 증가시키면 애니메이션·자동 닫힘이 다시 동작
 */
@Composable
fun AptoxToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    replayKey: Int = 0,
) {
    val bottomBarHeight: Dp = LocalBottomBarHeight.current

    LaunchedEffect(visible, message, replayKey) {
        if (visible && message.isNotEmpty()) {
            kotlinx.coroutines.delay(AutoDismissDelayMs)
            onDismiss()
        }
    }

    if (visible && message.isNotEmpty()) {
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(focusable = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = bottomBarHeight + ToastAboveBottomBarSpacing),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(AnimationDurationMs)) +
                        slideInVertically(
                            animationSpec = androidx.compose.animation.core.tween(AnimationDurationMs),
                            initialOffsetY = { it },
                        ),
                    exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(AnimationDurationMs)) +
                        slideOutVertically(
                            animationSpec = androidx.compose.animation.core.tween(AnimationDurationMs),
                            targetOffsetY = { it },
                        ),
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(min = ToastMinWidth)
                            .wrapContentWidth()
                            .background(
                                color = ToastBackgroundColor,
                                shape = ToastShape,
                            )
                            .padding(
                                start = ToastHorizontalPadding,
                                top = ToastVerticalPadding,
                                end = ToastHorizontalPadding,
                                bottom = ToastVerticalPadding,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = message,
                            style = AppTypography.Disclaimer.copy(color = Color.White),
                        )
                    }
                }
            }
        }
    }
}
