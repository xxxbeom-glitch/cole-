package com.aptox.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.aptox.app.usage.AppDatabaseProvider
import com.aptox.app.usage.UsageStatsDateUtils
import java.util.Calendar

/**
 * 앱 상세 바텀시트 — [startMs], [endMs] 구간 기준 **2시간 구간 12개**(0~2, 2~4, … 22~24시)별 일평균 사용량을 [queryEvents]로 집계.
 * 막대 색은 슬롯별 일평균을 **해당 앱 구간 내 최대값 대비 비율**로 정규화해 톤을 정함([mapAveragesToRankTones]).
 */
object AppDetailTimeSegmentRepository {

    const val TWO_HOUR_SLOT_COUNT = 12

    /**
     * 로컬 DB에 일별 12슬롯 백업이 구간 내 **모든 달력 일**에 있으면 그 합으로 일평균·톤 계산,
     * 없으면 [computeRankTonesForPackage]와 동일하게 UsageEvents 사용.
     */
    fun computeRankTonesForPackageWithLocalFallback(
        context: Context,
        packageName: String,
        startMs: Long,
        endMs: Long,
    ): List<AppDetailAverageTimeSlotBarTone>? {
        computeRankTonesFromLocalDb(context, packageName, startMs, endMs)?.let { return it }
        return computeRankTonesForPackage(context, packageName, startMs, endMs)
    }

    private fun computeRankTonesFromLocalDb(
        context: Context,
        packageName: String,
        startMs: Long,
        endMs: Long,
    ): List<AppDetailAverageTimeSlotBarTone>? {
        if (packageName.isBlank() || endMs <= startMs) return null
        val days = UsageStatsDateUtils.enumerateYyyyMmDdDaysInRange(startMs, endMs)
        if (days.isEmpty()) return null
        val db = AppDatabaseProvider.get(context)
        val totals = LongArray(TWO_HOUR_SLOT_COUNT)
        for (d in days) {
            val row = db.getTimeSegmentForDay(d, packageName) ?: return null
            for (i in 0 until TWO_HOUR_SLOT_COUNT) totals[i] += row[i]
        }
        val daysCount = inclusiveLocalDayCount(startMs, endMs).coerceAtLeast(1)
        val avgMs = LongArray(TWO_HOUR_SLOT_COUNT) { i -> totals[i] / daysCount }
        return mapAveragesToRankTones(avgMs)
    }

    /**
     * @return 권한 없음 → null, 그 외 항상 길이 12; 전 구간 0이면 전부 MUTED. [endMs] ≤ [startMs]이면 전부 MUTED.
     */
    fun computeRankTonesForPackage(
        context: Context,
        packageName: String,
        startMs: Long,
        endMs: Long,
    ): List<AppDetailAverageTimeSlotBarTone>? {
        if (!StatisticsData.hasUsageAccess(context)) return null
        if (packageName.isBlank()) return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }
        if (endMs <= startMs) return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }

        val totalsMs = LongArray(TWO_HOUR_SLOT_COUNT)
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }
        val events = usm.queryEvents(startMs, endMs)
            ?: return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }

        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val key = sessionKey(packageName, event.className)
                    sessionStarts[key] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = sessionKey(packageName, event.className)
                    val start = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - start).coerceAtLeast(0)
                    addDurationToTwelveTwoHourSlots(totalsMs, start, durationMs)
                }
                else -> {}
            }
        }

        for ((key, st) in sessionStarts.toList()) {
            if (!key.startsWith("$packageName:")) continue
            val durationMs = (endMs - st).coerceAtLeast(0)
            if (durationMs > 0) addDurationToTwelveTwoHourSlots(totalsMs, st, durationMs)
        }

        val days = inclusiveLocalDayCount(startMs, endMs).coerceAtLeast(1)
        val avgMs = LongArray(TWO_HOUR_SLOT_COUNT) { i -> totalsMs[i] / days }
        return mapAveragesToRankTones(avgMs)
    }

    /** 로컬 자정 기준, [startMs]·[endMs]가 걸치는 달력 일 수(양 끝 포함), 최소 1. */
    private fun inclusiveLocalDayCount(startMs: Long, endMs: Long): Int {
        val cal = Calendar.getInstance()
        fun startOfLocalDay(ms: Long): Long {
            cal.timeInMillis = ms
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        val s = startOfLocalDay(startMs)
        val e = startOfLocalDay(endMs)
        return (((e - s) / (24L * 60L * 60L * 1000L)).toInt() + 1).coerceAtLeast(1)
    }

    private fun sessionKey(packageName: String, className: String?): String =
        "$packageName:${className ?: ""}"

    /**
     * 로컬 시각 기준 2시간 슬롯 [0,2), [2,4), … [22,24) 에 ms 누적.
     */
    internal fun addDurationToTwelveTwoHourSlots(slots: LongArray, segmentStartMs: Long, durationMs: Long) {
        require(slots.size == TWO_HOUR_SLOT_COUNT)
        if (durationMs <= 0) return
        var remaining = durationMs
        var t = segmentStartMs
        val cal = Calendar.getInstance()
        while (remaining > 0) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = (hour / 2).coerceIn(0, TWO_HOUR_SLOT_COUNT - 1)
            val nextBoundaryHour = (slotIndex + 1) * 2
            @Suppress("UNCHECKED_CAST")
            val slotEndCal = cal.clone() as Calendar
            if (nextBoundaryHour >= 24) {
                slotEndCal.add(Calendar.DAY_OF_YEAR, 1)
                slotEndCal.set(Calendar.HOUR_OF_DAY, 0)
            } else {
                slotEndCal.set(Calendar.HOUR_OF_DAY, nextBoundaryHour)
            }
            slotEndCal.set(Calendar.MINUTE, 0)
            slotEndCal.set(Calendar.SECOND, 0)
            slotEndCal.set(Calendar.MILLISECOND, 0)
            val slotEndMs = slotEndCal.timeInMillis
            val chunkMs = minOf(remaining, slotEndMs - t).coerceAtLeast(0)
            slots[slotIndex] += chunkMs
            remaining -= chunkMs
            t += chunkMs
        }
    }

    /**
     * 슬롯별 일평균 ms를 **12슬롯 중 max** 대비 비율 `t = v/max`로 정규화해 톤 결정.
     * - `max == 0` → 전부 MUTED
     * - `max > 0`일 때 각 슬롯 `v`: `v <= 0` 또는 `t < 0.2` → MUTED
     * - `t >= 0.75` → PRIMARY_FULL, `>= 0.5` → PRIMARY_80, `>= 0.2` → PRIMARY_40
     */
    internal fun mapAveragesToRankTones(avgMsPerSlot: LongArray): List<AppDetailAverageTimeSlotBarTone> {
        require(avgMsPerSlot.size == TWO_HOUR_SLOT_COUNT)
        val max = avgMsPerSlot.maxOrNull() ?: 0L
        if (max == 0L) {
            return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }
        }
        val maxF = max.toFloat()
        fun toneFor(v: Long): AppDetailAverageTimeSlotBarTone {
            if (v <= 0L) return AppDetailAverageTimeSlotBarTone.MUTED
            val t = v.toFloat() / maxF
            if (t < 0.20f) return AppDetailAverageTimeSlotBarTone.MUTED
            return when {
                t >= 0.75f -> AppDetailAverageTimeSlotBarTone.PRIMARY_FULL
                t >= 0.50f -> AppDetailAverageTimeSlotBarTone.PRIMARY_80
                else -> AppDetailAverageTimeSlotBarTone.PRIMARY_40
            }
        }
        return List(TWO_HOUR_SLOT_COUNT) { i -> toneFor(avgMsPerSlot[i]) }
    }
}
