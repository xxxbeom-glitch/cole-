package com.aptox.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aptox.app.usage.AppDatabaseProvider
import com.aptox.app.usage.DailyUsageEntity
import com.aptox.app.usage.DailyUsageFirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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
        val pm = context.packageManager
        val userPackages = getUserInstalledPackages(pm)

        for (daysAgo in 1..7) {
            val dateStr = daysAgoToYyyyMmDd(daysAgo)
            val (startMs, endMs) = yyyyMmDdToRange(dateStr)
            val entities = aggregateFromEvents(usm, startMs, endMs, userPackages, dateStr)
            if (entities.isNotEmpty()) {
                db.insertAll(entities)
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    runCatching { DailyUsageFirestoreRepository().uploadDailyUsage(uid, entities) }
                }
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

    private fun getUserInstalledPackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolves.map { it.activityInfo.packageName }.toSet()
    }

    private fun aggregateFromEvents(
        usm: android.app.usage.UsageStatsManager,
        startMs: Long,
        endMs: Long,
        userPackages: Set<String>,
        dateStr: String,
    ): List<DailyUsageEntity> {
        val events = usm.queryEvents(startMs, endMs) ?: return emptyList()
        val usageMs = mutableMapOf<String, Long>()
        val sessionCounts = mutableMapOf<String, Int>()
        val sessionStarts = mutableMapOf<String, Long>()
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in userPackages) continue
            @Suppress("DEPRECATION")
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    sessionStarts[event.packageName] = event.timeStamp
                    sessionCounts[event.packageName] = (sessionCounts[event.packageName] ?: 0) + 1
                }
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = sessionStarts.remove(event.packageName) ?: continue
                    val duration = (event.timeStamp - start).coerceAtLeast(0)
                    usageMs[event.packageName] = (usageMs[event.packageName] ?: 0) + duration
                }
                else -> {}
            }
        }
        return usageMs.map { (pkg, ms) ->
            DailyUsageEntity(
                date = dateStr,
                packageName = pkg,
                usageMs = ms,
                sessionCount = sessionCounts[pkg] ?: 0,
            )
        }
    }

    private fun daysAgoToYyyyMmDd(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun yyyyMmDdToRange(dateStr: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val year = dateStr.substring(0, 4).toInt()
        val month = dateStr.substring(4, 6).toInt() - 1
        val day = dateStr.substring(6, 8).toInt()
        cal.set(year, month, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return Pair(startMs, cal.timeInMillis)
    }
}
