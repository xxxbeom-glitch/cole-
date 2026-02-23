package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ColeHeaderSub(
    title: String,
    modifier: Modifier = Modifier,
    backIcon: Painter? = null,
    onBackClick: (() -> Unit)? = null,
    actionIcon: Painter? = null,
    onActionClick: (() -> Unit)? = null,
    showNotification: Boolean = true,
    hasNotification: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppColors.SurfaceBackgroundBackground)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 왼쪽 뒤로가기 버튼
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            if (backIcon != null) {
                Icon(
                    painter = backIcon,
                    contentDescription = "뒤로가기",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(enabled = onBackClick != null) { onBackClick?.invoke() },
                    tint = AppColors.TextPrimary,
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }

        // 가운데 타이틀
        Text(
            text = title,
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center),
        )

        // 오른쪽 액션 버튼 (Figma: 36x36 터치 영역, 24x24 아이콘)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(36.dp)
                .then(
                    if (onActionClick != null) Modifier.clickable { onActionClick.invoke() }
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                showNotification -> Icon(
                    painter = painterResource(
                        id = if (hasNotification) R.drawable.ic_notification_on else R.drawable.ic_notification_off
                    ),
                    contentDescription = "알림",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified,
                )
                actionIcon != null -> Icon(
                    painter = actionIcon,
                    contentDescription = "액션",
                    modifier = Modifier.size(24.dp),
                    tint = AppColors.TextPrimary,
                )
                else -> Spacer(modifier = Modifier.size(36.dp))
            }
        }
        } // Box 닫기
    } // Column 닫기
}

@Composable
fun ColePageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .then(
                        if (isActive) Modifier.width(22.dp) else Modifier.size(6.dp)
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isActive) AppColors.TextHighlight else AppColors.Grey250
                    ),
            )
        }
    }
}
