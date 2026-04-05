package com.aptox.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aptox.app.usage.AppDatabaseProvider
import com.aptox.app.usage.DailyUsageFirestoreRepository
import com.aptox.app.usage.StatisticsBackupFirestoreRepository
import com.aptox.app.usage.UsageStatsDateUtils
import com.aptox.app.usage.UsageStatsDaySyncUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 자가테스트 로딩 화면에서 실행하는 설치된 앱·스크린타임·AI 카테고리 캐싱.
 * 각 단계 완료 시 콜백 호출.
 */
class AppDataPreloadRepository(private val context: Context) {

    data class AppInfo(val packageName: String, val name: String)

    /**
     * Step 1: 설치된 앱 목록 로드 (캐시용 기초 데이터).
     * @return 설치된 런처 앱 목록 (pkg, name)
     */
    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.Default) {
        runCatching {
            val pm = context.packageManager
            val selfPkg = context.packageName
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            resolves
                .mapNotNull { ri ->
                    runCatching {
                        val pkg = ri.activityInfo.packageName
                        if (pkg == selfPkg) return@mapNotNull null
                        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(pkg, 0)
                        }
                        val label = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                        label?.let { AppInfo(pkg, it) }
                    }.getOrNull()
                }
                .distinctBy { it.packageName }
                .sortedBy { it.name }
                .takeIf { it.isNotEmpty() }
        }.getOrNull() ?: emptyList()
    }

    /**
     * Step 2: 스크린 타임(UsageStats) 분석 — 최근 7일 사용량을 DB에 동기화.
     */
    suspend fun syncUsageStatsToDb(): Unit = withContext(Dispatchers.IO) {
        if (!StatisticsData.hasUsageAccess(context)) return@withContext
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            ?: return@withContext
        val db = AppDatabaseProvider.get(context)
        val userPackages = UsageStatsDaySyncUtils.getUserInstalledPackages(context)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val statsRepo = StatisticsBackupFirestoreRepository()

        for (daysAgo in 1..7) {
            val dateStr = UsageStatsDateUtils.daysAgoToYyyyMmDd(daysAgo)
            val (startMs, endMs) = UsageStatsDateUtils.yyyyMmDdToRange(dateStr)
            val entities = UsageStatsDaySyncUtils.aggregateDailyUsageFromEvents(
                usm, startMs, endMs, userPackages, dateStr,
            )
            if (entities.isEmpty()) continue
            db.insertAll(entities)
            val categoryRows = UsageStatsDaySyncUtils.buildCategoryStatsForDay(context, entities)
            db.replaceCategoryStatsForDay(dateStr, categoryRows)
            val segmentRows = UsageStatsDaySyncUtils.aggregateTimeSegmentSlotsFromEvents(
                usm, startMs, endMs, userPackages, dateStr,
            )
            db.replaceTimeSegmentsForDay(dateStr, segmentRows)
            uid?.let { userId ->
                runCatching { DailyUsageFirestoreRepository().uploadDailyUsage(userId, entities) }
                runCatching { statsRepo.uploadCategoryStatsForDay(userId, categoryRows) }
                runCatching { statsRepo.uploadTimeSegmentsForDay(userId, segmentRows) }
            }
        }
    }

    /**
     * Step 3: AI 카테고리 분류 — 캐시 미스 앱만 Claude API 호출 후 캐시 저장.
     */
    suspend fun classifyAndCacheApps(installedApps: List<AppInfo>): Unit = withContext(Dispatchers.IO) {
        classifyAndCacheAppsInternal(installedApps, targetProgress = 1f) {}
    }

    /**
     * AI 카테고리 분류의 일부만 수행 (스플래시에서 ~30% 진행 후 중단).
     * @param targetProgress 0f~1f. 이 비율까지 분류 후 반환. 예: 0.3f = 30%까지만.
     * @param onProgress suspend (처리된 앱 수 / 전체 needClassify 수) 0f~1f — Main에서 호출 가능
     * @return 실제 수행된 비율
     */
    suspend fun classifyAndCacheAppsPartial(
        installedApps: List<AppInfo>,
        targetProgress: Float,
        onProgress: suspend (Float) -> Unit,
    ): Float = withContext(Dispatchers.IO) {
        classifyAndCacheAppsInternal(installedApps, targetProgress, onProgress)
    }

    private suspend fun classifyAndCacheAppsInternal(
        installedApps: List<AppInfo>,
        targetProgress: Float,
        onProgress: suspend (Float) -> Unit,
    ): Float {
        if (installedApps.isEmpty()) return 1f
        val cacheRepo = AppCategoryCacheRepository(context)
        val installedPkgs = installedApps.map { it.packageName }.toSet()
        cacheRepo.pruneUninstalled(installedPkgs)
        val cached = cacheRepo.getCache()
        val needClassify = installedApps
            .filter { it.packageName !in cached }
            .map { it.packageName to it.name }
        if (needClassify.isEmpty()) {
            onProgress(1f)
            return 1f
        }
        val totalCount = needClassify.size
        val batchSize = 25
        val batches = needClassify.chunked(batchSize)
        var processedCount = 0
        val repo = ClaudeRepository()
        for (batch in batches) {
            val results = repo.classifyApps(batch).getOrNull() ?: continue
            val newEntries = results.associate { it.packageName to it.category }
            cacheRepo.saveCategories(newEntries)
            processedCount += batch.size
            val progress = (processedCount.toFloat() / totalCount).coerceIn(0f, 1f)
            onProgress(progress)
            if (progress >= targetProgress) return progress
        }
        onProgress(1f)
        return 1f
    }

}
