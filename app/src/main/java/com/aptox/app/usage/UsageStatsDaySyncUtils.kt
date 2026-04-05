package com.aptox.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.aptox.app.AppDetailTimeSegmentRepository
import com.aptox.app.AppCategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 일 단위 UsageEvents 집계 — [UsageStatsSyncWorker], [AppDataPreloadRepository] 공통.
 */
object UsageStatsDaySyncUtils {

    const val TIME_SLOT_COUNT = 12

    fun getUserInstalledPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolves.map { it.activityInfo.packageName }.toSet()
    }

    fun aggregateDailyUsageFromEvents(
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

    /**
     * 하루 구간에서 패키지별 12슬롯(2시간) 누적 ms. [AppDetailTimeSegmentRepository]와 동일 FG/BG·activity 키 규칙.
     */
    fun aggregateTimeSegmentSlotsFromEvents(
        usm: UsageStatsManager,
        startMs: Long,
        endMs: Long,
        userPackages: Set<String>,
        dateStr: String,
    ): List<DailyTimeSegmentEntity> {
        val events = usm.queryEvents(startMs, endMs) ?: return emptyList()
        val perPackageSlots = mutableMapOf<String, LongArray>()
        fun slots(pkg: String): LongArray = perPackageSlots.getOrPut(pkg) { LongArray(TIME_SLOT_COUNT) }
        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        fun sessionKey(packageName: String, className: String?): String =
            "$packageName:${className ?: ""}"

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in userPackages) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val key = sessionKey(event.packageName, event.className)
                    sessionStarts[key] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = sessionKey(event.packageName, event.className)
                    val st = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - st).coerceAtLeast(0)
                    AppDetailTimeSegmentRepository.addDurationToTwelveTwoHourSlots(
                        slots(event.packageName),
                        st,
                        durationMs,
                    )
                }
                else -> {}
            }
        }
        for ((key, st) in sessionStarts.toList()) {
            val pkg = key.substringBefore(":", missingDelimiterValue = "")
            if (pkg.isEmpty() || pkg !in userPackages) continue
            val durationMs = (endMs - st).coerceAtLeast(0)
            if (durationMs > 0) {
                AppDetailTimeSegmentRepository.addDurationToTwelveTwoHourSlots(slots(pkg), st, durationMs)
            }
        }
        return perPackageSlots.map { (pkg, arr) ->
            DailyTimeSegmentEntity(date = dateStr, packageName = pkg, slotMs = arr.copyOf())
        }
    }

    suspend fun buildCategoryStatsForDay(
        context: Context,
        entities: List<DailyUsageEntity>,
    ): List<DailyCategoryStatEntity> {
        if (entities.isEmpty()) return emptyList()
        val date = entities.first().date
        val repo = AppCategoryRepository(context)
        return withContext(Dispatchers.IO) {
            val byCat = mutableMapOf<String, Long>()
            for (e in entities) {
                val cat = repo.getCategory(e.packageName)
                byCat[cat] = (byCat[cat] ?: 0L) + e.usageMs
            }
            byCat.map { (c, ms) -> DailyCategoryStatEntity(date = date, category = c, usageMs = ms) }
        }
    }
}
