package com.cole.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import java.text.DecimalFormat
import java.util.Calendar

/**
 * 통계 화면용 데이터 로딩.
 *
 * ## x축 구성 (3시간 간격)
 * - 24 = 00:00~02:59 (자정)
 * - 3  = 03:00~05:59
 * - 6  = 06:00~08:59
 * - 9  = 09:00~11:59
 * - 12 = 12:00~14:59
 * - 15 = 15:00~17:59
 * - 18 = 18:00~20:59
 * - 21 = 21:00~23:59
 *
 * ## 탭별 데이터 기간
 * - 오늘: 오늘 00:00 ~ 현재 시각
 * - 주간: 이번 주 월요일 00:00 ~ 현재 시각
 * - 월간: 이번 달 1일 00:00 ~ 현재 시각
 * - 연간: 올해 1월 1일 00:00 ~ 현재 시각
 *
 * ## 그래프 규칙
 * - 현재 시각이 속한 구간까지만 바 표시, 미래 구간은 빈 칸
 */
object StatisticsData {

    /** 탭 인덱스: 0=오늘, 1=주간, 2=월간, 3=연간 */
    enum class Tab { TODAY, WEEKLY, MONTHLY, YEARLY }

    /** 8개 슬롯 라벨 [3, 6, 9, 12, 15, 18, 21, 24] 순 */
    val SlotLabels = listOf(3, 6, 9, 12, 15, 18, 21, 24)

    data class StatsAppItem(
        val packageName: String,
        val name: String,
        val usageMinutes: String,
        val sessionCount: String,
        val isRestricted: Boolean,
    )

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** (startMs, endMs) 범위 반환 */
    fun getTimeRange(context: Context, tab: Tab): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endMs = System.currentTimeMillis()
        val startMs = when (tab) {
            Tab.TODAY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.WEEKLY -> {
                cal.timeInMillis = endMs
                // 한국 로케일 무관하게 월요일 기준으로 계산
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 일=1, 월=2, ..., 토=7
                val daysFromMonday = (dayOfWeek + 5) % 7 // 월=0, 화=1, ..., 일=6
                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.MONTHLY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.YEARLY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
        return Pair(startMs, endMs)
    }

    /**
     * 현재 시각이 속한 슬롯 인덱스 (0~7).
     * 그래프에서 이 인덱스까지 바 표시, 이후 구간은 빈 칸.
     */
    fun getCurrentSlotIndex(): Int {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 3..5 -> 0   // 3
            hour in 6..8 -> 1   // 6
            hour in 9..11 -> 2  // 9
            hour in 12..14 -> 3 // 12
            hour in 15..17 -> 4 // 15
            hour in 18..20 -> 5 // 18
            hour in 21..23 -> 6 // 21
            else -> 7           // 24 (00:00~02:59) → 맨 마지막
        }
    }

    /** 8개 슬롯(24,3,6,9,12,15,18,21)별 사용량(분). SlotLabels 참고 */
    fun loadTimeSlotMinutes(context: Context, tab: Tab): List<Long> {
        if (!hasUsageAccess(context)) return List(8) { 0L }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return List(8) { 0L }
        val (startMs, endMs) = getTimeRange(context, tab)
        val events = usm.queryEvents(startMs, endMs) ?: return List(8) { 0L }
        val event = UsageEvents.Event()
        val slotMinutes = LongArray(8)
        val sessionStarts = mutableMapOf<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    sessionStarts[event.packageName + ":" + event.className] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = event.packageName + ":" + event.className
                    val start = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - start).coerceAtLeast(0)
                    addDurationToSlots(slotMinutes, start, durationMs)
                }
            }
        }
        return slotMinutes.toList()
    }

    /** durationMs를 startMs 시각 기준으로 슬롯에 분배. 24=00~02:59, 3=03~05:59, ..., 21=21~23:59 */
    private fun addDurationToSlots(slots: LongArray, startMs: Long, durationMs: Long) {
        var remaining = durationMs
        var t = startMs
        val cal = Calendar.getInstance()
        while (remaining > 0) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = when {
                hour in 3..5 -> 0   // 3:  03:00~05:59
                hour in 6..8 -> 1   // 6:  06:00~08:59
                hour in 9..11 -> 2  // 9:  09:00~11:59
                hour in 12..14 -> 3 // 12: 12:00~14:59
                hour in 15..17 -> 4 // 15: 15:00~17:59
                hour in 18..20 -> 5 // 18: 18:00~20:59
                hour in 21..23 -> 6 // 21: 21:00~23:59
                else -> 7           // 24: 00:00~02:59
            }
            val slotEndHour = when (slotIndex) {
                0 -> 6   // 3→6
                1 -> 9   // 6→9
                2 -> 12  // 9→12
                3 -> 15  // 12→15
                4 -> 18  // 15→18
                5 -> 21  // 18→21
                6 -> 24  // 21→다음날 00:00
                else -> 3 // 24(00~02)→03:00
            }
            @Suppress("UNCHECKED_CAST")
            val slotEndCal = cal.clone() as Calendar
            if (slotEndHour == 24) {
                slotEndCal.add(Calendar.DAY_OF_YEAR, 1)
                slotEndCal.set(Calendar.HOUR_OF_DAY, 0)
                slotEndCal.set(Calendar.MINUTE, 0)
                slotEndCal.set(Calendar.SECOND, 0)
                slotEndCal.set(Calendar.MILLISECOND, 0)
            } else {
                slotEndCal.set(Calendar.HOUR_OF_DAY, slotEndHour)
                slotEndCal.set(Calendar.MINUTE, 0)
                slotEndCal.set(Calendar.SECOND, 0)
                slotEndCal.set(Calendar.MILLISECOND, 0)
            }
            val slotEndMs = slotEndCal.timeInMillis
            val chunkMs = minOf(remaining, slotEndMs - t).coerceAtLeast(0)
            slots[slotIndex] += chunkMs / 60_000
            remaining -= chunkMs
            t += chunkMs
        }
    }

    fun loadAppUsage(context: Context, tab: Tab): List<StatsAppItem> {
        if (!hasUsageAccess(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val pm = context.packageManager
        val (startMs, endMs) = getTimeRange(context, tab)
        val restrictedPkgs = AppRestrictionRepository(context).getAll().map { it.packageName }.toSet()
        val userAppPackages = getUserInstalledPackages(pm)

        // INTERVAL_BEST 대신 UsageEvents로 직접 집계 → startMs/endMs 정확히 적용
        val events = usm.queryEvents(startMs, endMs) ?: return emptyList()
        val event = UsageEvents.Event()
        val usageMs = mutableMapOf<String, Long>()
        val sessionCounts = mutableMapOf<String, Int>()
        val sessionStarts = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in userAppPackages) continue
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
            }
        }

        return usageMs
            .filter { it.value > 0 }
            .map { (packageName, totalMs) ->
                val name = try {
                    pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                } catch (_: PackageManager.NameNotFoundException) { packageName }
                Pair(
                    StatsAppItem(
                        packageName = packageName,
                        name = name,
                        usageMinutes = "${DecimalFormat("#,###").format(totalMs / 60_000)}분",
                        sessionCount = "${sessionCounts[packageName] ?: 0}회",
                        isRestricted = packageName in restrictedPkgs,
                    ),
                    totalMs,
                )
            }
            .sortedByDescending { it.second }
            .map { it.first }
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

}
