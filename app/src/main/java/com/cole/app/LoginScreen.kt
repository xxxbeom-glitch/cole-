package com.cole.app

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay

/**
 * 스플래시 → 로그인 전환 애니메이션을 포함한 통합 화면
 *
 * 동작:
 * 1. 로고가 화면 정중앙에 표시 (스플래시 상태)
 * 2. 1.5s 후 로고가 위쪽으로 이동하며 SNS 버튼들이 우→좌 순차 슬라이드 인
 */
@Composable
fun SplashLoginScreen(
    onNaverLoginClick: () -> Unit = {},
    onKakaoLoginClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    isLoading: Boolean = false,
) {
    var buttonsVisible by remember { mutableStateOf(false) }
    var logoHeightPx by remember { mutableStateOf(0f) }
    var loadingProvider by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(1500L)
        buttonsVisible = true
    }

    val density = LocalDensity.current
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        // 배경이 어두운 퍼플이므로 상태바 아이콘을 흰색으로
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary300)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        // BoxWithConstraintsScope에서 로컬 변수로 캡처해야 중첩 람다에서 접근 가능
        val usableHeight = maxHeight
        val screenHeightPx = with(density) { usableHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        // 로그인 상태에서의 자연스러운 로고 위치: 상단 32% 지점
        val logoNaturalTopPx = with(density) { (usableHeight * 0.32f).toPx() }
        // 스플래시 상태: 로고를 정중앙으로 보이게 하는 Y 오프셋
        val splashOffsetY = if (logoHeightPx > 0) ((screenHeightPx - logoHeightPx) / 2f) - logoNaturalTopPx else 0f

        val logoTranslationY by animateFloatAsState(
            targetValue = if (!buttonsVisible) splashOffsetY else 0f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            label = "logoY",
        )

        val contentAlpha by animateFloatAsState(
            targetValue = if (buttonsVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 300, delayMillis = 150),
            label = "contentAlpha",
        )

        // 버튼 슬라이드: 우→좌 순차 진입 (120ms 간격)
        val btn1OffsetX by animateFloatAsState(
            targetValue = if (buttonsVisible) 0f else screenWidthPx,
            animationSpec = tween(durationMillis = 420, delayMillis = 150, easing = FastOutSlowInEasing),
            label = "btn1",
        )
        val btn2OffsetX by animateFloatAsState(
            targetValue = if (buttonsVisible) 0f else screenWidthPx,
            animationSpec = tween(durationMillis = 420, delayMillis = 270, easing = FastOutSlowInEasing),
            label = "btn2",
        )
        val btn3OffsetX by animateFloatAsState(
            targetValue = if (buttonsVisible) 0f else screenWidthPx,
            animationSpec = tween(durationMillis = 420, delayMillis = 390, easing = FastOutSlowInEasing),
            label = "btn3",
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 로고 위쪽 여백 (로그인 상태의 자연 위치 = 32%)
            Spacer(Modifier.height(usableHeight * 0.32f))

            // 로고 (제공된 크기 그대로 사용, 리사이징 없음)
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = "cole.",
                modifier = Modifier
                    .graphicsLayer { translationY = logoTranslationY }
                    .onGloballyPositioned { logoHeightPx = it.size.height.toFloat() },
            )

            Spacer(Modifier.height(52.dp))

            // SNS 구분선
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = contentAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp,
                )
                Text(
                    text = "SNS 계정으로 간편하게!",
                    style = AppTypography.Caption1.copy(color = Color.White.copy(alpha = 0.75f)),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f),
                    thickness = 1.dp,
                )
            }

            Spacer(Modifier.height(20.dp))

            // 에러 메시지
            if (errorMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IcoErrorInfo()
                    Text(
                        text = errorMessage,
                        style = AppTypography.Disclaimer.copy(color = Color.White),
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // 카카오 버튼
            SnsLoginButton(
                text = "카카오로 시작하기",
                icon = painterResource(R.drawable.ic_kakao),
                backgroundColor = Color(0xFFFEE500),
                contentColor = Color(0xFF3C1E1E),
                isLoading = loadingProvider == "kakao",
                onClick = {
                    if (!isLoading) {
                        loadingProvider = "kakao"
                        onClearError()
                        onKakaoLoginClick()
                    }
                },
                modifier = Modifier.offset { IntOffset(btn1OffsetX.toInt(), 0) },
            )

            Spacer(Modifier.height(12.dp))

            // 네이버 버튼
            SnsLoginButton(
                text = "네이버로 시작하기",
                icon = painterResource(R.drawable.ic_naver),
                backgroundColor = Color(0xFF03C75A),
                contentColor = Color.White,
                isLoading = loadingProvider == "naver",
                onClick = {
                    if (!isLoading) {
                        loadingProvider = "naver"
                        onClearError()
                        onNaverLoginClick()
                    }
                },
                modifier = Modifier.offset { IntOffset(btn2OffsetX.toInt(), 0) },
            )

            Spacer(Modifier.height(12.dp))

            // 구글 버튼
            SnsLoginButton(
                text = "구글로 시작하기",
                icon = painterResource(R.drawable.ic_google),
                backgroundColor = Color.White,
                contentColor = Color(0xFF1D1D1F),
                isLoading = loadingProvider == "google",
                onClick = {
                    if (!isLoading) {
                        loadingProvider = "google"
                        onClearError()
                        onGoogleLoginClick()
                    }
                },
                modifier = Modifier.offset { IntOffset(btn3OffsetX.toInt(), 0) },
            )

            // 로딩 완료 시 loadingProvider 초기화
            if (!isLoading && loadingProvider != null) {
                loadingProvider = null
            }
        }
    }
}

@Composable
private fun SnsLoginButton(
    text: String,
    icon: Painter,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor.copy(alpha = if (isLoading) 0.7f else 1f))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),
            )
            Text(
                text = text,
                style = AppTypography.BodyBold.copy(color = contentColor),
            )
        }
    }
}

/** 디버그 메뉴 호환용 - SplashLoginScreen으로 위임 */
@Composable
fun LoginScreen(
    logo: Painter,
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onSignUpClick: () -> Unit = {},
    onNaverLoginClick: () -> Unit = {},
    onKakaoLoginClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    isLoading: Boolean = false,
) {
    SplashLoginScreen(
        onNaverLoginClick = onNaverLoginClick,
        onKakaoLoginClick = onKakaoLoginClick,
        onGoogleLoginClick = onGoogleLoginClick,
        errorMessage = errorMessage,
        onClearError = onClearError,
        isLoading = isLoading,
    )
}
