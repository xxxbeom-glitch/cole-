package com.aptox.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aptox.app.StatisticsData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * UsageStats → 로컬 DB → Firestore 백업.
 * 실행 주기: 최초 앱 실행 시 즉시 1회, 로그인 직후 즉시 1회, 이후 6시간마다.
 * 중복 방지: SQLite CONFLICT_REPLACE (date+packageName), Firestore set() 덮어쓰기.
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
                FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                    runCatching {
                        DailyUsageFirestoreRepository().uploadDailyUsage(uid, entities)
                    }
                }
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
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val resolves = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolves.map { it.activityInfo.packageName }.toSet()
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
