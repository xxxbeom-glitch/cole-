package com.cole.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cole.app.StatisticsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 매일 00:10 실행. 전날 사용량을 Room에 저장.
 * 기기에서 UsageEvents는 ~7일 후 삭제되므로, 그 전에 앱 DB에 보관.
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
        val pm = context.packageManager
        val userPackages = getUserInstalledPackages(pm)

        // 동기화할 날짜들: yesterday만 (일반 실행) 또는 최근 7일 (initialSync 플래그)
        val daysToSync = if (inputData.getBoolean(KEY_INITIAL_SYNC, false)) {
            (1..7).map { daysAgo -> daysAgoToYyyyMmDd(daysAgo) }
        } else {
            listOf(daysAgoToYyyyMmDd(1))
        }

        for (dateStr in daysToSync) {
            val (startMs, endMs) = yyyyMmDdToRange(dateStr)
            val entities = aggregateFromEvents(usm, startMs, endMs, userPackages, dateStr)
            if (entities.isNotEmpty()) {
                db.insertAll(entities)
            }
        }
        Result.success()
    }

    private fun aggregateFromEvents(
        usm: UsageStatsManager,
        startMs: Long,
        endMs: Long,
        userPackages: Set<String>,
        dateStr: String,
    ): List<DailyUsageEntity> {
        val events = usm.queryEvents(startMs, endMs) ?: return emptyList()
        val usageMs = mutableMapOf<String, Long>()
        val sessionCounts = mutableMapOf<String, Int>()
        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in userPackages) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    sessionStarts[event.packageName] = event.timeStamp
                    sessionCounts[event.packageName] = (sessionCounts[event.packageName] ?: 0) + 1
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
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

    private fun getUserInstalledPackages(pm: PackageManager): Set<String> {
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA) ?: return emptySet()
        return apps
            .filter { info ->
                val flags = info.flags
                val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdated = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdated
            }
            .map { it.packageName }
            .toSet()
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
        val endMs = cal.timeInMillis
        return Pair(startMs, endMs)
    }

    companion object {
        const val KEY_INITIAL_SYNC = "initial_sync"
    }
}
