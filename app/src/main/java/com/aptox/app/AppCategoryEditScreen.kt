package com.aptox.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Figma 1319-4533 카드 — Shadow/Card 6dp, #0000000F */
private val CategoryEditCardShape = RoundedCornerShape(12.dp)
private val CategoryEditCardShadowColor = Color.Black.copy(alpha = 0.06f)

/**
 * 바텀시트 옵션 순서·라벨 — AddAppScreens `APP_CATEGORY_OPTIONS`와 동일 (일일 사용량 지정 플로우).
 */
private val SHEET_REST_CATEGORY_ORDER = listOf("SNS", "OTT", "게임", "쇼핑", "주식,코인", "웹툰", "기타")

private data class AppCategoryItem(
    val packageName: String,
    val appName: String,
    val currentCategory: String,
)

private fun normalizeCategoryKey(raw: String): String = when (raw) {
    "주식·코인", "주식/코인", "주식,코인" -> "STOCK"
    else -> raw
}

/** 리스트 1차 정렬 인덱스 (작을수록 위) */
private fun categorySortIndex(tag: String): Int {
    val key = normalizeCategoryKey(tag)
    return when (key) {
        "SNS" -> 0
        "OTT" -> 1
        "게임" -> 2
        "쇼핑" -> 3
        "STOCK" -> 4
        "웹툰" -> 5
        "기타" -> 6
        else -> 7
    }
}

/** 시트용: 저장/표시 라벨을 AddApp과 동일한 `주식,코인` 형태로 맞춤 */
private fun canonicalSheetLabel(currentRaw: String): String {
    val key = normalizeCategoryKey(currentRaw)
    if (key == "STOCK") return "주식,코인"
    return SHEET_REST_CATEGORY_ORDER.find { it == currentRaw } ?: currentRaw
}

/** 시트: 선택 항목을 맨 앞에, 나머지는 SNS → … → 기타 */
private fun buildSheetCategoryOptions(currentRaw: String): List<String> {
    val first = canonicalSheetLabel(currentRaw)
    val firstKey = normalizeCategoryKey(first)
    val rest = SHEET_REST_CATEGORY_ORDER.filter { normalizeCategoryKey(it) != firstKey }
    return listOf(first) + rest
}

/**
 * 앱 카테고리 수동 수정 화면 (Figma 1319-4533, List / App Status Data View)
 * 상단 타이틀/뒤로가기는 호스트(StubScreens Scaffold)의 AptoxHeaderSub만 사용.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun AppCategoryEditScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var appList by remember { mutableStateOf<List<AppCategoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedApp by remember { mutableStateOf<AppCategoryItem?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val items = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val selfPkg = context.packageName
            val categoryRepo = AppCategoryRepository(context)
            val endTime = System.currentTimeMillis()
            val start30d = endTime - 30L * 24 * 60 * 60 * 1000L
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            fun usageForegroundMs(startMs: Long, endMs: Long): Map<String, Long> =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    usageStatsManager
                        .queryAndAggregateUsageStats(startMs, endMs)
                        .mapValues { (_, u) -> u.totalTimeInForeground }
                } else {
                    @Suppress("DEPRECATION")
                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_BEST,
                        startMs,
                        endMs,
                    ) ?: emptyList()
                    stats
                        .groupBy { it.packageName }
                        .mapValues { (_, list) -> list.sumOf { it.totalTimeInForeground } }
                }

            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolves = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            val launcherPackages = resolves
                .mapNotNull { runCatching { it.activityInfo.packageName }.getOrNull() }
                .filter { it != selfPkg }
                .toSet()

            val usage30d = usageForegroundMs(start30d, endTime)
            val eligiblePackages = launcherPackages.filter { pkg ->
                (usage30d[pkg] ?: 0L) >= 60_000L
            }

            val userApps = eligiblePackages
                .mapNotNull { pkg ->
                    runCatching {
                        val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(pkg, 0)
                        }
                        val label = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                        pkg to (label ?: pkg)
                    }.getOrNull()
                }
                .distinctBy { it.first }
                .map { (pkg, name) ->
                    AppCategoryItem(
                        packageName = pkg,
                        appName = name,
                        currentCategory = categoryRepo.getCategory(pkg),
                    )
                }
                .sortedWith(
                    compareBy(
                        { categorySortIndex(it.currentCategory) },
                        { it.appName.lowercase() },
                    ),
                )

            userApps
        }
        appList = items
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = AppColors.Primary400,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                appList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "표시할 앱이 없습니다.",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        contentPadding = PaddingValues(top = 24.dp, bottom = 56.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = CategoryEditCardShape,
                                        clip = false,
                                        ambientColor = CategoryEditCardShadowColor,
                                        spotColor = CategoryEditCardShadowColor,
                                    )
                                    .clip(CategoryEditCardShape)
                                    .background(AppColors.SurfaceBackgroundCard)
                                    .padding(start = 16.dp, top = 26.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                appList.forEach { item ->
                                    key(item.packageName) {
                                        AppCategoryEditListRow(
                                            item = item,
                                            onClick = { selectedApp = item },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedApp?.let { app ->
        CategorySelectBottomSheet(
            appItem = app,
            onDismiss = { selectedApp = null },
            onSave = { newCategory ->
                scope.launch(Dispatchers.IO) {
                    AppCategoryManualOverrideRepository(context).save(app.packageName, newCategory)
                    appList = appList.map {
                        if (it.packageName == app.packageName) it.copy(currentCategory = newCategory) else it
                    }.sortedWith(
                        compareBy(
                            { categorySortIndex(it.currentCategory) },
                            { it.appName.lowercase() },
                        ),
                    )
                }
                selectedApp = null
            },
        )
    }
}

/**
 * Figma 1465-4825 List / App Status Data View — 통계 카테고리 행(StatsAppRow)과 동일 토큰.
 * - 행 높이 56dp, 아이콘·텍스트 간격 12dp, 태그~앱명 2dp, Body/Medium + TextBody, 화살표 24dp
 */
@Composable
private fun AppCategoryEditListRow(
    item: AppCategoryItem,
    onClick: () -> Unit,
) {
    val appIcon = rememberAppIconPainter(item.packageName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = remember(item.packageName) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBox(
                appIcon = appIcon,
                size = 56.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryTag(tag = item.currentCategory)
                }
                Text(
                    text = item.appName,
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = AppColors.TextPrimary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectBottomSheet(
    appItem: AppCategoryItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetOptions = remember(appItem.packageName, appItem.currentCategory) {
        buildSheetCategoryOptions(appItem.currentCategory)
    }
    var selectedIndex by remember(appItem.packageName, appItem.currentCategory) {
        mutableIntStateOf(0)
    }

    BaseBottomSheet(
        title = "앱 카테고리를 수정해주세요",
        onDismissRequest = onDismiss,
        onPrimaryClick = { onSave(sheetOptions[selectedIndex]) },
        primaryButtonText = "저장",
        modifier = Modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            sheetOptions.chunked(2).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowOptions.forEach { option ->
                        val index = sheetOptions.indexOf(option)
                        Box(modifier = Modifier.weight(1f)) {
                            AptoxSelectionCardTitleOnly(
                                title = option,
                                selected = index == selectedIndex,
                                onClick = { selectedIndex = index },
                                titleStyle = AppTypography.BodyMedium,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
