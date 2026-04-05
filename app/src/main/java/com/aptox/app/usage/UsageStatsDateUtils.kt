package com.aptox.app.usage

import java.util.Calendar

internal object UsageStatsDateUtils {

    fun yyyyMmDdToHyphen(yyyyMmDd: String): String {
        if (yyyyMmDd.length < 8) return yyyyMmDd
        return "${yyyyMmDd.take(4)}-${yyyyMmDd.substring(4, 6)}-${yyyyMmDd.substring(6, 8)}"
    }

    fun hyphenYyyyMmDdToCompact(hyphen: String): String = hyphen.replace("-", "")

    fun yyyyMmDdToRange(dateStr: String): Pair<Long, Long> {
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

    fun daysAgoToYyyyMmDd(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return String.format(
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** [startMs]~[endMs] 구간이 걸치는 각 달력 일(로컬)의 yyyyMMdd 목록 (양 끝 포함) */
    fun enumerateYyyyMmDdDaysInRange(startMs: Long, endMs: Long): List<String> {
        if (endMs < startMs) return emptyList()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val endCal = Calendar.getInstance()
        endCal.timeInMillis = endMs
        endCal.set(Calendar.HOUR_OF_DAY, 0)
        endCal.set(Calendar.MINUTE, 0)
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)
        val out = mutableListOf<String>()
        while (!cal.after(endCal)) {
            out.add(
                String.format(
                    "%04d%02d%02d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                ),
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return out
    }
}
