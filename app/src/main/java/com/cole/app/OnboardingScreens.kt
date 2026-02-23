package com.cole.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 온보딩 4개 화면 (OB-01 ~ OB-04)
// 이미지: 가로 100%, 하단 고정 | 인디케이터: 자리 고정
// ─────────────────────────────────────────────

private data class OnboardingPage(
    val titleLine1: String,
    val titleLine2: String,
    val subtitleLine1: String,
    val subtitleLine2: String,
    val imageResId: Int,
    val subtitleUsesColeHighlight: Boolean = false,
)

// Figma OB-01: 텍스트~인디케이터 gap 28dp, 이미지 top 480px, 가로 100% 하단 고정
private val TitleSubtitleGap = 8.dp
private val MessageIndicatorGap = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingHost(
    onSkipClick: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { 4 })

    val pages = listOf(
        OnboardingPage(
            "스마트폰에 빼앗긴",
            "당신의 소중한 일상",
            "무심코 확인하는 알림이",
            "당신의 몰입을 방해하고 있습니다",
            R.drawable.ob_01_image,
        ),
        OnboardingPage(
            "잠들지 못하는 밤,",
            "화면 속의 고립",
            "늦은 밤 무분별한 시청은",
            "내일의 활력까지 소모시킵니다",
            R.drawable.ob_02_image,
        ),
        OnboardingPage(
            "함께하는 식사조차",
            "단절의 시간이 되었나요?",
            "스마트폰에 가려진 서로의 눈맞춤과 온전한 소통을",
            "다시 회복해보세요",
            R.drawable.ob_03_image,
        ),
        OnboardingPage(
            "스마트폰, ",
            "이제 거리를 둘 때예요",
            "cole은 당신이 과하게 소비하는 앱을",
            "효과적으로 제한해드려요",
            R.drawable.ob_04_image,
            subtitleUsesColeHighlight = true,
        ),
    )

    // statusBars만 적용 - 이미지는 하단 끝까지 (navigationBars 미적용)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true,
            ) { page ->
                val p = pages[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TitleSubtitleGap),
                    ) {
                        Text(
                            text = "${p.titleLine1}\n${p.titleLine2}",
                            style = AppTypography.Display2.copy(color = AppColors.TextPrimary),
                            textAlign = TextAlign.Center,
                        )
                        if (p.subtitleUsesColeHighlight) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = AppColors.TextHighlight)) {
                                        append("cole")
                                    }
                                    append("은 당신이 과하게 소비하는 앱을\n효과적으로 제한해드려요")
                                },
                                style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary),
                                textAlign = TextAlign.Center,
                            )
                        } else {
                            Text(
                                text = "${p.subtitleLine1}\n${p.subtitleLine2}",
                                style = AppTypography.BodyBold.copy(color = AppColors.TextSecondary),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(MessageIndicatorGap))
                    ColePageIndicator(pageCount = 4, currentPage = pagerState.currentPage)
                }
            }

            Image(
                painter = painterResource(id = pages[pagerState.currentPage].imageResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.FillWidth,
            )
        }

        if (pagerState.currentPage == 3) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColePrimaryButton(
                    text = "계속 진행",
                    onClick = onStartClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                ColeGhostButton(
                    text = "돌아가기",
                    onClick = onSkipClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
