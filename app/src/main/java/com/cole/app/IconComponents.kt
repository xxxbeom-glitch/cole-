package com.cole.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
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
 * packageName으로 앱 아이콘을 로드하여 Painter로 반환.
 * 패키지가 유효하지 않거나 로드 실패 시 ic_app_placeholder 사용.
 */
@Composable
fun rememberAppIconPainter(packageName: String?): Painter {
    val context = LocalContext.current
    val fallback = painterResource(R.drawable.ic_app_placeholder)
    return remember(packageName) {
        if (packageName.isNullOrBlank()) return@remember fallback
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            BitmapPainter(bitmap.asImageBitmap())
        } catch (_: Exception) {
            fallback
        }
    }
}

/**
 * 폰에 설치된 앱 아이콘 (기기 기본 쉐이프 적용).
 * AdaptiveIcon은 시스템 마스크가 적용된 상태로 반환.
 */
@Composable
fun rememberDefaultAppIconPainter(): Painter {
    val context = LocalContext.current
    val fallback = painterResource(R.drawable.ic_app_placeholder)
    return remember {
        try {
            val drawable = context.packageManager.getApplicationIcon(DEFAULT_APP_PACKAGE)
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
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

/** 기기 AdaptiveIcon 마스크 Shape. 실패 시 RoundedCornerShape(50%) 폴백 */
@Composable
fun rememberDeviceIconMaskShape(): Shape {
    val context = LocalContext.current
    return remember {
        try {
            val drawable = context.packageManager.getApplicationIcon(DEFAULT_APP_PACKAGE)
            if (drawable is AdaptiveIconDrawable) {
                drawable.setBounds(0, 0, 108, 108)
                val maskPath = drawable.iconMask
                val srcRect = RectF().apply { maskPath.computeBounds(this, true) }
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density,
                    ): Outline {
                        val dstRect = RectF(0f, 0f, size.width, size.height)
                        val matrix = Matrix().apply {
                            setRectToRect(srcRect, dstRect, Matrix.ScaleToFit.FILL)
                        }
                        val scaledPath = AndroidPath().apply { addPath(maskPath, matrix) }
                        return Outline.Generic(scaledPath.asComposePath())
                    }
                }
            } else {
                RoundedCornerShape(percent = 50)
            }
        } catch (_: Exception) {
            RoundedCornerShape(percent = 50)
        }
    }
}

/**
 * 앱 아이콘 (기기 기본 쉐이프 + #000000 오버레이 + 중앙 자물쇠)
 * 아이콘·오버레이 모두 기기 마스크로 클리핑
 * @param overlayAlpha 오버레이 불투명도 (기본 50%)
 */
@Composable
fun AppIconSquircleLock(
    appIcon: Painter,
    modifier: Modifier = Modifier,
    iconSize: Dp = 56.dp,
    lockIconResId: Int = R.drawable.ic_lock_center,
    overlayAlpha: Float = 0.5f,
) {
    val maskShape = rememberDeviceIconMaskShape()
    Box(
        modifier = modifier
            .size(iconSize)
            .clip(maskShape),
    ) {
        Icon(
            painter = appIcon,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = overlayAlpha)),
        )
        Icon(
            painter = painterResource(id = lockIconResId),
            contentDescription = "사용 제한 중",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp),
        )
    }
}

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
