package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Figma: X=0, Y=0, Blur=6, Spread=0, #000000 6% - 실제 기기에서 보이도록 elevation/alpha 강화 */
private val ChallengeCardShape = RoundedCornerShape(12.dp)
private val ChallengeCardShadowElevation = 8.dp
private val ChallengeCardShadowColor = Color.Black.copy(alpha = 0.1f)

/**
 * 챌린지 화면 (Figma CH_01)
 * - 헤더: 챌린지 중앙, 알림
 * - 최근 받은 뱃지 카드: 연한 회색 플레이스홀더 + 완벽한 하루
 * - 뱃지 그리드: 3열, 균일한 간격, 획득/미획득 뱃지
 */
/** 카드 높이 144dp, 뱃지배경 56x56, gem 36x36 (사용자 스펙) */
/** Figma CH_01 (310:1865): 카드 padding 16px, 뱃지↔텍스트 gap 10px, 제목↔날짜 gap 10px */
private val BadgeCardHeight = 144.dp
private val BadgeBackgroundSize = 56.dp
private val GemSize = 36.dp
private val BadgeCardPadding = 16.dp      // Figma: p-[16px]
private val BadgeToTextGap = 10.dp       // Figma: gap-[10px] (아이콘↔제목)
private val TextToDateGap = 10.dp         // Figma: gap-[10px] (제목↔날짜)

@Composable
fun ChallengeScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .background(AppColors.SurfaceBackgroundBackground),
    ) {
        // 상단 여백 (헤더와 콘텐츠 사이) — 메인/통계/설정과 카드 시작점 맞춤
        Spacer(modifier = Modifier.height(10.dp))

        // 최근 받은 뱃지 카드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
                .clip(ChallengeCardShape)
                .background(AppColors.SurfaceBackgroundCard)
                .padding(vertical = 28.dp, horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // squicle_big 플레이스홀더 (최근 받은 뱃지)
                Image(
                    painter = painterResource(R.drawable.squicle_big),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "최근 받은 뱃지",
                        style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                    )
                    Text(
                        text = "완벽한 하루",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "하루 목표 100% 달성",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // 최근 뱃지 카드와 그리드 사이 간격
        Spacer(modifier = Modifier.height(28.dp))

        // 뱃지 그리드 (3열) - 가로/세로 균일한 간격
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            challengeBadgeItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { item ->
                        BadgeGridItem(
                            modifier = Modifier.weight(1f),
                            name = item.name,
                            earned = item.earned,
                            earnedWithGem = item.earnedWithGem,
                            date = item.date,
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        // 하단 여백
        Spacer(modifier = Modifier.height(28.dp))
    }
}

private data class ChallengeBadgeItem(
    val name: String,
    val earned: Boolean,
    val earnedWithGem: Boolean,
    val date: String,
)

private val challengeBadgeItems = listOf(
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("첫 걸음", true, true, "2026. 3.31"),
    ChallengeBadgeItem("몰입 시작", true, false, "2026. 3.31"),
    ChallengeBadgeItem("꾸준한 실천", true, false, "2026. 3.31"),
    ChallengeBadgeItem("집중 마스터", true, false, "2026. 3.31"),
    ChallengeBadgeItem("깊은 몰입", false, false, "2026. 3.31"),
    ChallengeBadgeItem("시작의 의지", false, false, "2026. 3.31"),
    ChallengeBadgeItem("습관의 씨앗", true, false, "2026. 3.31"),
    ChallengeBadgeItem("루틴 마스터", true, false, "2026. 3.31"),
    ChallengeBadgeItem("루틴 마스터", true, false, "2026. 3.31"),
    ChallengeBadgeItem("루틴 마스터", true, false, "2026. 3.31"),
)

@Composable
private fun BadgeGridItem(
    modifier: Modifier = Modifier,
    name: String,
    earned: Boolean,
    earnedWithGem: Boolean,
    date: String,
) {
    // 뱃지명: Text/Body, 날짜: Text/Caption
    val textColor = if (earned) AppColors.TextBody else AppColors.TextDisabled
    val dateColor = if (earned) AppColors.TextCaption else AppColors.TextDisabled
    // 획득: squicle_small_empty(연보라), 미획득: squicle_small(흰색)
    val badgeBgResId = if (earned) R.drawable.squicle_small_empty else R.drawable.squicle_small

    Box(
        modifier = modifier
            .height(BadgeCardHeight)
            .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
            .clip(ChallengeCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .padding(BadgeCardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BadgeToTextGap),
        ) {
            // 뱃지배경 56x56 + gem 36x36 (획득 시)
            Box(
                modifier = Modifier.size(BadgeBackgroundSize),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(badgeBgResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                if (earnedWithGem) {
                    Image(
                        painter = painterResource(R.drawable.gem_small),
                        contentDescription = null,
                        modifier = Modifier.size(GemSize),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            // 제목 + 날짜 (Figma: gap 10px)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TextToDateGap),
            ) {
                Text(
                    text = name,
                    style = AppTypography.Caption1.copy(color = textColor),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
                Text(
                    text = date,
                    style = AppTypography.Caption1.copy(color = dateColor),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
