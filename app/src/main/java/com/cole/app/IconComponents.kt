package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    hasNotification: Boolean = true,
    tint: Color = AppColors.TextPrimary,
    size: Dp = 36.dp,
) {
    Icon(
        painter = painterResource(
            id = if (hasNotification) R.drawable.ic_notification_on else R.drawable.ic_notification_off
        ),
        contentDescription = "알림",
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun IcoDisclaimerInfo(
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextSecondary,
    size: Dp = 18.dp,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_disclaimer_info),
        contentDescription = "안내",
        tint = tint,
        modifier = modifier.size(size),
    )
}

@Composable
fun IcoAppLockOff(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.TextDisabled),
    )
}

@Composable
fun IcoAppLockOn(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.TextDisabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock_app),
            contentDescription = "잠금",
            tint = AppColors.TextInvert,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun IcoAppLabel(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(modifier = modifier.size(size)) {
        IcoAppLockOn(modifier = Modifier.matchParentSize(), size = size)
        LabelDanger(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 4.dp),
        )
    }
}

@Composable
fun LabelPro(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(Color(0xFFFF5353))
            .padding(start = 7.dp, end = 9.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock_label),
            contentDescription = null,
            tint = AppColors.TextInvert,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Pro",
            style = AppTypography.Label.copy(
                color = AppColors.TextInvert,
                fontSize = 9.sp,
            ),
        )
    }
}

@Composable
fun LabelWarning(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(AppColors.Orange300)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = "주의",
            style = AppTypography.Label.copy(
                color = AppColors.TextInvert,
                fontSize = 9.sp,
            ),
        )
    }
}

@Composable
fun LabelDanger(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(AppColors.Red300)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = "위험",
            style = AppTypography.Label.copy(
                color = AppColors.TextInvert,
                fontSize = 9.sp,
            ),
        }
    }
}
