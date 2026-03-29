package com.aptox.app

import com.aptox.app.model.BadgeDefinition
import com.aptox.app.model.BadgeMasterData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val ChallengeCardShape = RoundedCornerShape(12.dp)
private val ChallengeCardShadowElevation = 8.dp
private val ChallengeCardShadowColor = Color.Black.copy(alpha = 0.1f)

private val BadgeCardHeight = 130.dp
private val BadgeBackgroundSize = 56.dp
private val BadgeIconSize = 36.dp
private val BadgeCardPadding = 16.dp
private val BadgeToTextGap = 6.dp
private val TextToDateGap = 4.dp

private val DateFormatBadge = SimpleDateFormat("yyyy. M.d", Locale.KOREAN)

/**
 * 챌린지 화면 (Figma CH_01 310:1865)
 */
@Composable
fun ChallengeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedBadgeId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedBadgeId != null) {
        selectedBadgeId = null
    }

    val challengeBadgeItems by produceState(
        initialValue = buildChallengeBadgeItems(emptyMap()),
        context, FirebaseAuth.getInstance().currentUser?.uid,
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        value = if (userId != null) {
            withContext(Dispatchers.IO) {
                val earnedMap = BadgeRepository(FirebaseFirestore.getInstance()).getAllEarnedBadges(userId)
                buildChallengeBadgeItems(earnedMap)
            }
        } else {
            buildChallengeBadgeItems(emptyMap())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 최근 받은 뱃지 카드 (가장 최근 획득 1개 또는 미획득 시 첫 뱃지, Figma 310-2465)
        val featuredItem = challengeBadgeItems.firstOrNull { it.earned } ?: challengeBadgeItems.first()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
                .clip(ChallengeCardShape)
                .background(AppColors.SurfaceBackgroundCard)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { selectedBadgeId = featuredItem.badge.id }
                .padding(vertical = 28.dp, horizontal = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val topTextColor = if (featuredItem.earned) AppColors.TextPrimary else AppColors.TextDisabled
                val topDescText = if (featuredItem.earned) featuredItem.badge.description else "-"
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (featuredItem.earned) {
                        Image(
                            painter = painterResource(R.drawable.bg_active),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                        Image(
                            painter = painterResource(featuredItem.badge.iconResId),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        // Figma 310-1987: 획득 메달 없을 때 상단 카드 — 스쿠시클 배경+자물쇠 합성 아이콘
                        Image(
                            painter = painterResource(R.drawable.ico_lock_challange_card),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                Text(
                    text = featuredItem.badge.title,
                    style = AppTypography.HeadingH3.copy(color = topTextColor),
                )
                Text(
                    text = topDescText,
                    style = AppTypography.BodyMedium.copy(
                        color = if (featuredItem.earned) AppColors.TextTertiary else AppColors.TextDisabled,
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 뱃지 그리드 (3열)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            challengeBadgeItems.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowItems.forEach { item ->
                        key(item.badge.id) {
                            BadgeGridItem(
                                modifier = Modifier.weight(1f),
                                item = item,
                                onTap = { selectedBadgeId = item.badge.id },
                            )
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }

    if (selectedBadgeId != null) {
        BadgeDetailBottomSheet(
            badgeId = selectedBadgeId!!,
            onDismissRequest = { selectedBadgeId = null },
        )
    }
}

private data class ChallengeBadgeItem(
    val badge: BadgeDefinition,
    val earned: Boolean,
    val date: String,
)

/**
 * BadgeMasterData + Firestore 획득 정보로 챌린지 뱃지 목록 생성.
 * 정렬: 획득한 뱃지(획득 일시 최신순) → 미획득 뱃지(order순)
 */
private fun buildChallengeBadgeItems(earnedMap: Map<String, Long>): List<ChallengeBadgeItem> {
    val earnedByAchievedAt = earnedMap.entries
        .sortedByDescending { it.value }
        .map { it.key }
        .toList()
    val unearned = BadgeMasterData.badges.filter { it.id !in earnedMap.keys }.sortedBy { it.order }
    val earnedBadges = earnedByAchievedAt.mapNotNull { id -> BadgeMasterData.badges.find { it.id == id } }
    return (earnedBadges.map { badge ->
        val achievedAtMs = earnedMap[badge.id] ?: 0L
        ChallengeBadgeItem(
            badge = badge,
            earned = true,
            date = if (achievedAtMs > 0) DateFormatBadge.format(Date(achievedAtMs)) else "-",
        )
    } + unearned.map { badge ->
        ChallengeBadgeItem(badge = badge, earned = false, date = "-")
    })
}

@Composable
private fun BadgeGridItem(
    modifier: Modifier = Modifier,
    item: ChallengeBadgeItem,
    onTap: () -> Unit = {},
) {
    val textColor = if (item.earned) AppColors.TextBody else AppColors.TextDisabled
    val dateColor = if (item.earned) AppColors.TextCaption else AppColors.TextDisabled
    val bgResId = if (item.earned) R.drawable.bg_active else R.drawable.bg_disable
    val dateText = if (item.earned) item.date else "-" // Figma 310-2465: 미획득 시 "-" 표시

    Box(
        modifier = modifier
            .height(BadgeCardHeight)
            .shadow(ChallengeCardShadowElevation, ChallengeCardShape, false, ChallengeCardShadowColor, ChallengeCardShadowColor)
            .clip(ChallengeCardShape)
            .background(AppColors.SurfaceBackgroundCard)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onTap() }
            .padding(BadgeCardPadding),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BadgeToTextGap),
        ) {
            Box(
                modifier = Modifier.size(BadgeBackgroundSize),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(bgResId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                if (item.earned) {
                    Image(
                        painter = painterResource(item.badge.iconResId),
                        contentDescription = null,
                        modifier = Modifier.size(BadgeIconSize),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ico_lock_challange),
                        contentDescription = null,
                        modifier = Modifier.size(width = 18.dp, height = 22.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Text(
                text = item.badge.title,
                style = AppTypography.Caption1.copy(color = textColor),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = dateText,
                style = AppTypography.Caption1.copy(color = dateColor),
                textAlign = TextAlign.Center,
            )
        }
    }
}
