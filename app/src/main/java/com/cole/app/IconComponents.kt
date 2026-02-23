package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IcoBack(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    size: Dp = 36.dp,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_back),
        contentDescription = "뒤로가기",
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun IcoErrorInfo(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.FormTextError,
    size: Dp = 20.dp,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_error_info),
        contentDescription = "오류",
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun IcoCompleted(
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(AppColors.Primary300, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "완료",
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
fun IcoNotification(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    size: Dp = 36.dp,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_notification_on),
        contentDescription = "알림",
        tint = tint,
        modifier = modifier.size(size),
    )
}
