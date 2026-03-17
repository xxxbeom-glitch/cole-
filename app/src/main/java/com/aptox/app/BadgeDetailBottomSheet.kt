package com.aptox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aptox.app.model.BadgeDefinition
import com.aptox.app.model.BadgeMasterData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 획득한 뱃지 탭 시 표시되는 상세 바텀시트 (Figma 1227-4458)
 * - badges/{badgeId}, users/{userId}/badges/{badgeId} Firestore 조회
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeDetailBottomSheet(
    badgeId: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val badgeRepo = remember { BadgeRepository() }
    var badge by mutableStateOf<BadgeDefinition?>(null)
    var userBadge by mutableStateOf<UserBadgeInfo?>(null)
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(badgeId) {
        badge = withContext(Dispatchers.IO) {
            badgeRepo.getBadge(badgeId) ?: BadgeMasterData.badges.find { it.id == badgeId }
        }
        if (userId != null) {
            userBadge = withContext(Dispatchers.IO) { badgeRepo.getUserBadge(userId, badgeId) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 56.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(38.dp),
        ) {
            if (badge != null) {
                val b = badge!!
                val isEarned = userBadge?.earned == true
                val achievedAtMs = userBadge?.achievedAtMs

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Figma 1227-4527: 메달 영역 80dp
                    val iconBgResId = if (isEarned) R.drawable.bg_active else R.drawable.bg_disable
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(iconBgResId),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                        )
                        if (isEarned) {
                            BadgeDetailMedalWithRotation(
                                iconResId = b.iconResId,
                                size = 52.dp,
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.ico_lock_challange),
                                contentDescription = null,
                                modifier = Modifier.size(width = 24.dp, height = 28.dp),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }

                    // Figma 1227-4540: 타이틀·조건·날짜 column, gap 8dp
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = b.title,
                            style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = b.condition,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                            textAlign = TextAlign.Center,
                        )
                        if (isEarned && achievedAtMs != null) {
                            val sdf = SimpleDateFormat("yyyy. M. d", Locale.KOREAN)
                            Text(
                                text = sdf.format(Date(achievedAtMs)),
                                style = AppTypography.Caption2.copy(color = AppColors.TextDisclaimer),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // Figma 1227-4544: primary-50 배경, primary-400 텍스트, padding 16h 18v, radius 12dp
                    val purpleBoxText = if (isEarned) {
                        (b.message ?: b.description).takeIf { it.isNotBlank() }
                    } else {
                        "아직 획득하지 못한 뱃지예요"
                    }
                    if (purpleBoxText != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.Primary50)
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                        ) {
                            Text(
                                text = purpleBoxText,
                                style = AppTypography.Disclaimer.copy(color = AppColors.Primary400),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "뱃지 정보를 불러오는 중…",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            AptoxPrimaryButton(
                text = "닫기",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * 바텀시트 내 메달 아이콘 - 등장 시 LaunchedEffect로 회전 애니메이션 실행 (요청 2)
 */
@Composable
private fun BadgeDetailMedalWithRotation(
    iconResId: Int,
    size: androidx.compose.ui.unit.Dp,
) {
    val scaleAnim = remember { Animatable(1f) }
    val rotationAnim = remember { Animatable(0f) }
    val sparkleProgress = remember { Animatable(0f) }
    val density = LocalDensity.current.density

    LaunchedEffect(Unit) {
        scaleAnim.snapTo(1f)
        rotationAnim.snapTo(0f)
        sparkleProgress.snapTo(0f)
        scaleAnim.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
        )
        rotationAnim.animateTo(
            targetValue = 720f,
            animationSpec = tween(500, easing = FastOutSlowInEasing),
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(150, easing = FastOutSlowInEasing),
        )
        sparkleProgress.snapTo(0f)
        sparkleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(250, easing = FastOutSlowInEasing),
        )
        sparkleProgress.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
                rotationY = rotationAnim.value
                transformOrigin = TransformOrigin.Center
                cameraDistance = (8f * density).coerceAtLeast(1f)
            },
    ) {
        Image(
            painter = painterResource(iconResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
        if (sparkleProgress.value in 0.01f..0.99f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = this.size.width
                val stripW = w * 0.35f
                val sweepX = sparkleProgress.value * 1.4f - 0.2f
                val left = (sweepX * w).coerceIn(-stripW, w)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.8f),
                            Color.Transparent,
                        ),
                        startX = left,
                        endX = left + stripW,
                    ),
                )
            }
        }
    }
}
