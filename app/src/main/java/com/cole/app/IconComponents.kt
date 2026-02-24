package com.cole.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.LayerDrawable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 넷플릭스 앱 패키지 — 기본 앱 아이콘으로 사용 */
private const val DEFAULT_APP_PACKAGE = "com.netflix.mediaclient"

/**
 * 폰에 설치된 앱 아이콘을 사각형으로 반환 (AdaptiveIcon 마스크 없이).
 * getBackground/getForeground 레이어를 직접 그려 시스템 스쿼클/원형 마스크를 제거.
 */
@Composable
fun rememberDefaultAppIconPainter(): Painter {
    val context = LocalContext.current
    val fallback = painterResource(R.drawable.ic_app_placeholder)
    return remember {
        try {
            val drawable = context.packageManager.getApplicationIcon(DEFAULT_APP_PACKAGE)
            val toDraw = when (drawable) {
                is AdaptiveIconDrawable -> {
                    val bg = drawable.background
                    val fg = drawable.foreground
                    if (bg != null && fg != null) {
                        LayerDrawable(arrayOf(bg, fg))
                    } else {
                        drawable
                    }
                }
                else -> drawable
            }
            val w = toDraw.intrinsicWidth.coerceAtLeast(1)
            val h = toDraw.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            toDraw.setBounds(0, 0, w, h)
            toDraw.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } catch (_: Exception) {
            null
        }
    } ?: fallback
}

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
    tint: Color = Color.Unspecified,
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
    tint: Color = Color.Unspecified,
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
    tint: Color = Color.Unspecified,
    size: Dp = 18.dp,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_disclaimer_info),
        contentDescription = "안내",
        tint = tint,
        modifier = modifier.size(size),
    )
}

/** 앱 아이콘 클리핑용 — 사각형 6dp 라운드 (Figma 가이드) */
private val AppIconShape = RoundedCornerShape(6.dp)

/**
 * 제한 진행중인 앱 아이콘: 기기 앱 아이콘 + 우측 하단 흰색 자물쇠 오버레이
 * 가이드: 앱 아이콘은 6dp 라운드 사각형으로 강제, 자물쇠는 우측 하단
 */
@Composable
fun RestrictedAppIconBox(
    appIcon: Painter,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(AppIconShape),
    ) {
        Icon(
            painter = appIcon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(size)
                .clip(AppIconShape),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_lock_app),
            contentDescription = "사용 제한 중",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
                .size(20.dp),
        )
    }
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
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.TextDisabled),
    ) {
        LabelDanger(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 4.dp, start = 4.dp),
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_lock_app),
            contentDescription = "잠금",
            tint = AppColors.TextInvert,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 4.dp, end = 4.dp)
                .size(24.dp),
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
            style = AppTypography.Label,
            color = AppColors.TextInvert,
            fontSize = 9.sp,
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
            style = AppTypography.Label,
            color = AppColors.TextInvert,
            fontSize = 9.sp,
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
            style = AppTypography.Label,
            color = AppColors.TextInvert,
            fontSize = 9.sp,
        )
    }
}
