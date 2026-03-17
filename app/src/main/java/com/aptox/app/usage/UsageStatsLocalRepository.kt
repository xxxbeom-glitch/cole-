package com.aptox.app.usage

import android.content.Context
import java.util.Calendar

/**
 * 앱 DB에 저장된 일별 사용량 조회.
 * Worker가 매일 동기화하므로, 과거 기간은 DB에서 읽음.
 */
class UsageStatsLocalRepository(private val context: Context) {

    private val db = AppDatabaseProvider.get(context)

    fun getByDateRange(startDate: String, endDate: String): List<DailyUsageEntity> =
        db.getByDateRange(startDate, endDate)

    /** 날짜별 총 사용량(분). date -> totalMinutes */
    fun getDayTotalsForDatesBlocking(dates: List<String>): Map<String, Long> {
        if (dates.isEmpty()) return emptyMap()
        val start = dates.minOrNull()!!
        val end = dates.maxOrNull()!!
        val list = db.getByDateRange(start, end)
        return list.groupBy { it.date }.mapValues { (_, entities) ->
            entities.sumOf { it.usageMs } / 60_000
        }
    }

    fun getMonthTotalsForYearBlocking(year: Int): List<Long> {
        val startDate = "${year}0101"
        val endDate = "${year}1231"
        val list = db.getByDateRange(startDate, endDate)
        val monthTotals = LongArray(12)
        for (e in list) {
            val month = e.date.substring(4, 6).toIntOrNull() ?: continue
            if (month in 1..12) monthTotals[month - 1] += e.usageMs / 60_000
        }
        return monthTotals.toList()
    }

    fun getTotalForDateRangeBlocking(startDate: String, endDate: String): Long =
        db.getByDateRange(startDate, endDate).sumOf { it.usageMs } / 60_000

    fun getAppUsageForRangeBlocking(startDate: String, endDate: String): Map<String, Pair<Long, Int>> {
        val list = db.getByDateRange(startDate, endDate)
        return list.groupBy { it.packageName }.mapValues { (_, entities) ->
            Pair(entities.sumOf { it.usageMs }, entities.sumOf { it.sessionCount })
        }
    }

    fun hasDataForDateBlocking(date: String): Boolean = db.hasDataForDate(date)

    /** 앱 첫 사용일(DB 최소 date) 기준 오늘까지의 누적 일수. 데이터 없으면 0 */
    fun getCumulativeDaysSinceFirstUseBlocking(): Int {
        return try {
            val earliest = db.getEarliestDate() ?: return 0
            if (earliest.length < 8) return 0
            val year = earliest.substring(0, 4).toIntOrNull() ?: return 0
            val month = (earliest.substring(4, 6).toIntOrNull() ?: return 0) - 1
            val day = earliest.substring(6, 8).toIntOrNull() ?: return 0
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val firstMs = cal.timeInMillis
            cal.timeInMillis = System.currentTimeMillis()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStartMs = cal.timeInMillis
            ((todayStartMs - firstMs) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }

    companion object {
        fun msToYyyyMmDd(ms: Long): String {
            val cal = Calendar.getInstance()
            cal.timeInMillis = ms
            return String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        }
    }
}
