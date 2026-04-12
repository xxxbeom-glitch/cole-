package com.cole.app

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.ImageView

/** 카테고리 칩 색상: SNS, 게임, OTT, 쇼핑, 웹툰, 주식·코인, 주식,코인, 기타 */
private val CategoryChipColors = mapOf(
    "OTT" to androidx.compose.ui.graphics.Color(0xFFEBCFFF),
    "SNS" to androidx.compose.ui.graphics.Color(0xFFFFC34B),
    "게임" to androidx.compose.ui.graphics.Color(0xFF818CFF),
    "쇼핑" to androidx.compose.ui.graphics.Color(0xFFA2A2A2),
    "웹툰" to androidx.compose.ui.graphics.Color(0xFF88C9FF),
    "주식·코인" to androidx.compose.ui.graphics.Color(0xFF3D9E5D),
    "주식,코인" to androidx.compose.ui.graphics.Color(0xFF3D9E5D),
    "기타" to androidx.compose.ui.graphics.Color(0xFFBDBDBD),
)

private data class AppItem(
    val packageName: String,
    val appName: String,
    val category: String? = null,
)

/**
 * AI 앱 카테고리 분류 테스트 화면
 * - 설치된 앱 전체 로드 (시스템 앱 제외)
 * - 카테고리 분류 버튼 → Claude API로 분류 (배치로 처리)
 * - 결과를 칩으로 표시
 */
@Composable
fun AiAppCategoryClassificationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var appList by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var isClassifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loadTrigger by remember { mutableStateOf(0) }

    fun loadApps() {
        errorMessage = null
        loadTrigger++
    }

    LaunchedEffect(loadTrigger) {
        isLoadingApps = true
        val items = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            val selfPkg = context.packageName

            fun fromUsageStats(): List<AppItem> {
                if (!StatisticsData.hasUsageAccess(context)) return emptyList()
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
                val endMs = System.currentTimeMillis()
                val startMs = endMs - (90L * 24 * 60 * 60 * 1000)
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startMs, endMs) ?: return emptyList()
                return stats
                    .mapNotNull { usageStats ->
                        val pkg = usageStats.packageName
                        if (pkg.isBlank() || pkg == selfPkg) return@mapNotNull null
                        runCatching {
                            val info = pm.getApplicationInfo(pkg, 0)
                            val name = (pm.getApplicationLabel(info) as? String)?.takeIf { it.isNotBlank() }
                            name?.let { AppItem(pkg, it) }
                        }.getOrNull()
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
            }

            fun toAppItems(apps: List<ApplicationInfo>, filterSystem: Boolean): List<AppItem> {
                val filterUserApp: (ApplicationInfo) -> Boolean = { info ->
                    if (filterSystem) {
                        val flags = info.flags
                        val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdated = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        !isSystem || isUpdated
                    } else true
                }
                return apps
                    .filter { it.packageName != selfPkg && filterUserApp(it) }
                    .mapNotNull { info ->
                        val name = (pm.getApplicationLabel(info) as? String)?.takeIf { it.isNotBlank() }
                        name?.let { AppItem(info.packageName, it) }
                    }
                    .distinctBy { it.packageName }
                    .sortedBy { it.appName.lowercase() }
            }

            val fromUsage = fromUsageStats()
            if (fromUsage.isNotEmpty()) {
                fromUsage
            } else {
                @Suppress("DEPRECATION")
                val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA) ?: emptyList()
                val userApps = toAppItems(apps, filterSystem = true)
                val allApps = toAppItems(apps, filterSystem = false)
                when {
                    userApps.isNotEmpty() -> userApps
                    allApps.isNotEmpty() -> allApps
                    else -> {
                        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        @Suppress("DEPRECATION")
                        val resolves = pm.queryIntentActivities(intent, 0)
                        resolves
                            .mapNotNull { ri ->
                                runCatching {
                                    val pkg = ri.activityInfo.packageName
                                    if (pkg == selfPkg) return@mapNotNull null
                                    val appInfo = pm.getApplicationInfo(pkg, 0)
                                    val name = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                                    name?.let { AppItem(pkg, it) }
                                }.getOrNull()
                            }
                            .distinctBy { it.packageName }
                            .sortedBy { it.appName.lowercase() }
                    }
                }
            }
        }
        appList = items
        isLoadingApps = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ColeGhostButton(text = "← 돌아가기", onClick = onBack)
        Text(
            text = "AI 앱 카테고리 분류 테스트",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        errorMessage?.let { msg ->
            Text(text = msg, style = AppTypography.Caption1.copy(color = AppColors.Red300))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ColePrimaryButton(
                text = "카테고리 분류",
                onClick = {
                    if (appList.isEmpty()) return@ColePrimaryButton
                    isClassifying = true
                    errorMessage = null
                },
                modifier = Modifier.weight(1f),
                enabled = !isClassifying && appList.isNotEmpty(),
            )
            ColeGhostButton(
                text = "새로고침",
                onClick = { loadApps() },
                modifier = Modifier.weight(1f),
                enabled = !isLoadingApps && !isClassifying,
            )
        }

        LaunchedEffect(isClassifying) {
            if (!isClassifying) return@LaunchedEffect
            val repo = ClaudeRepository()
            val batchSize = 25
            val batches = appList.map { it.packageName to it.appName }.chunked(batchSize)
            val allResults = mutableMapOf<String, String>()
            for ((index, batch) in batches.withIndex()) {
                val result = repo.classifyApps(batch)
                result
                    .onSuccess { results ->
                        results.forEach { allResults[it.packageName] = it.category }
                    }
                    .onFailure { e ->
                        errorMessage = e.message ?: "분류 실패 (${index + 1}/${batches.size} 배치)"
                        isClassifying = false
                        return@LaunchedEffect
                    }
            }
            appList = appList.map { it.copy(category = allResults[it.packageName] ?: "기타") }
            isClassifying = false
        }

        when {
            isLoadingApps -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = AppColors.Primary400,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            isClassifying -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AppColors.Primary400,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "AI가 분류 중...",
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                        )
                    }
                }
            }
            appList.isEmpty() -> {
                Text(
                    text = "표시할 앱이 없습니다.",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextTertiary),
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(appList) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.SurfaceBackgroundCard)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        ImageView(ctx).apply {
                                            setImageDrawable(ctx.packageManager.getApplicationIcon(item.packageName))
                                            scaleType = ImageView.ScaleType.FIT_CENTER
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.appName,
                                    style = AppTypography.BodyBold.copy(color = AppColors.TextPrimary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.packageName,
                                    style = AppTypography.Caption1.copy(color = AppColors.TextTertiary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            item.category?.let { cat ->
                                Text(
                                    text = cat,
                                    style = AppTypography.Label.copy(color = AppColors.TextPrimary),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CategoryChipColors[cat] ?: CategoryChipColors["기타"]!!)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
