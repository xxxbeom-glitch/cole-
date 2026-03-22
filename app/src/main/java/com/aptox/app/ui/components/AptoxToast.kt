package com.aptox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.aptox.app.AppTypography

private val ToastBackgroundColor = Color(0x3C000000) // 알파 60/255
private val ToastMinWidth = 100.dp
private val ToastHorizontalPadding = 20.dp
private val ToastVerticalPadding = 8.dp
private val ToastShape = RoundedCornerShape(999.dp)
private val AutoDismissDelayMs = 2000L
private val AnimationDurationMs = 300

/**
 * Figma 기준 커스텀 오버레이 Toast.
 * - 배경: 0x3C000000 (알파 60), RoundedCornerShape(999.dp)
 * - 텍스트: AppTypography.Disclaimer
 * - 위치: 화면 하단 중앙, bottomOffsetDp만큼 위
 * - 등장: fade + slideInVertically (300ms)
 * - 사라짐: fadeOut (300ms), 2초 후 onDismiss
 * @param bottomOffsetDp 화면 하단에서의 오프셋. 하단 앱 바 위에 두려면 (앱바 높이 + 26.dp)
 * @param replayKey 같은 문구로 연속 표시할 때마다 증가시키면 애니메이션·자동 닫힘이 다시 동작
 */
@Composable
fun AptoxToast(
    message: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    replayKey: Int = 0,
    bottomOffsetDp: Dp = 26.dp,
) {
    LaunchedEffect(visible, message, replayKey) {
        if (visible && message.isNotEmpty()) {
            kotlinx.coroutines.delay(AutoDismissDelayMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible && message.isNotEmpty(),
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
        modifier = modifier.zIndex(1000f),
    ) {
        Column(
            modifier = Modifier.padding(bottom = bottomOffsetDp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
