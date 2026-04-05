package com.aptox.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * 앱 상세 바텀시트 — [startMs], [endMs] 구간 기준 **2시간 구간 12개**(0~2, 2~4, … 22~24시)별 일평균 사용량을 [queryEvents]로 집계.
 * 사용이 **있는** 구간만 서로 비교해 순위 톤(1~4위: #6C54DD 100% / 80% / 40% / 20% 알파), 사용 없음(일평균 0)은 Grey 350(MUTED).
 */
object AppDetailTimeSegmentRepository {

    const val TWO_HOUR_SLOT_COUNT = 12

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
     * 슬롯별 일평균 ms: **0이면 MUTED**. 0 초과 슬롯만 고유값 내림차순 순위로 톤(동일 ms 동일 톤).
     * 5순위 이하도 20% 알파(else).
     */
    internal fun mapAveragesToRankTones(avgMsPerSlot: LongArray): List<AppDetailAverageTimeSlotBarTone> {
        require(avgMsPerSlot.size == TWO_HOUR_SLOT_COUNT)
        if (avgMsPerSlot.all { it == 0L }) {
            return List(TWO_HOUR_SLOT_COUNT) { AppDetailAverageTimeSlotBarTone.MUTED }
        }
        val distinctSortedNonZero = avgMsPerSlot
            .asSequence()
            .filter { it > 0L }
            .distinct()
            .sortedDescending()
            .toList()
        fun toneFor(avg: Long): AppDetailAverageTimeSlotBarTone {
            if (avg <= 0L) return AppDetailAverageTimeSlotBarTone.MUTED
            val rank = distinctSortedNonZero.indexOf(avg)
            return when (rank) {
                0 -> AppDetailAverageTimeSlotBarTone.PRIMARY_FULL
                1 -> AppDetailAverageTimeSlotBarTone.PRIMARY_80
                2 -> AppDetailAverageTimeSlotBarTone.PRIMARY_40
                else -> AppDetailAverageTimeSlotBarTone.PRIMARY_20
            }
        }
        return List(TWO_HOUR_SLOT_COUNT) { i -> toneFor(avgMsPerSlot[i]) }
    }
}
