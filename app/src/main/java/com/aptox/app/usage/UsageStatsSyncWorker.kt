package com.aptox.app.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aptox.app.StatisticsData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UsageStats → 로컬 DB → Firestore 백업.
 * 실행 주기: 최초 앱 실행 시 즉시 1회, 로그인 직후 즉시 1회, 이후 6시간마다.
 * 중복 방지: SQLite CONFLICT_REPLACE / REPLACE, Firestore set() 덮어쓰기.
 */
class UsageStatsSyncWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!StatisticsData.hasUsageAccess(context)) return@withContext Result.failure()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext Result.failure()
        val db = AppDatabaseProvider.get(context)
        val userPackages = UsageStatsDaySyncUtils.getUserInstalledPackages(context)

        val daysToSync = if (inputData.getBoolean(KEY_INITIAL_SYNC, false)) {
            (1..7).map { daysAgo -> UsageStatsDateUtils.daysAgoToYyyyMmDd(daysAgo) }
        } else {
            listOf(UsageStatsDateUtils.daysAgoToYyyyMmDd(1))
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val statsRepo = StatisticsBackupFirestoreRepository()

        for (dateStr in daysToSync) {
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
        Result.success()
    }

    companion object {
        const val KEY_INITIAL_SYNC = "initial_sync"
    }
}
