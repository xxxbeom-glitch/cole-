package com.aptox.app.widget

import android.content.Context
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.DailyUsageLimitConstants
import com.aptox.app.ManualTimerRepository
import com.aptox.app.RestrictionDeleteHelper
import java.util.Calendar

/** 위젯 1 (하루 사용량 제한) 단일 앱 행 데이터 */
data class DailyLimitWidgetRow(
    val appName: String,
    val usageMinutes: Int,
    val limitMinutes: Int,
    /** 0~100 */
    val progressPercent: Int,
    val isExceeded: Boolean,
)

/** 위젯 1 (하루 사용량 제한) 표시 데이터 */
data class DailyLimitWidgetData(
    /** 상위 최대 3개 행 */
    val rows: List<DailyLimitWidgetRow>,
    /** 전체 등록 앱 수 */
    val totalCount: Int,
)

private fun isTodayOnlyExpiredDaily(baselineTimeMs: Long): Boolean {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return baselineTimeMs < cal.timeInMillis
}

private fun isDurationExpiredDaily(baselineTimeMs: Long, durationWeeks: Int): Boolean {
    if (durationWeeks <= 0) return false
    return System.currentTimeMillis() >= baselineTimeMs + durationWeeks * 7L * 24 * 60 * 60 * 1000
}

private fun parseRepeatDaysDaily(repeatDays: String): Set<Int> =
    repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.toSet()

object DailyLimitWidgetDataLoader {

    fun empty() = DailyLimitWidgetData(rows = emptyList(), totalCount = 0)

    fun load(context: Context): DailyLimitWidgetData {
        val app = context.applicationContext
        val repo = AppRestrictionRepository(app)
        val timerRepo = ManualTimerRepository(app)
        val allRestrictions = repo.getAll()

        val dailyApps = mutableListOf<DailyLimitWidgetRow>()

        for (orig in allRestrictions) {
            val isTimeSpecified = orig.startTimeMs > 0
            if (isTimeSpecified) continue

            val repeatDaySet = parseRepeatDaysDaily(orig.repeatDays)
            if (repeatDaySet.isEmpty()) {
                if (isTodayOnlyExpiredDaily(orig.baselineTimeMs)) {
                    RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                    continue
                }
            } else {
                if (isDurationExpiredDaily(orig.baselineTimeMs, orig.durationWeeks)) {
                    RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                    continue
                }
            }

            val isUnlimited = orig.limitMinutes >= DailyUsageLimitConstants.UNLIMITED_MINUTES_SENTINEL
            val usageMs = timerRepo.getTodayUsageMs(orig.packageName)
            val usageMin = (usageMs / 60_000L).toInt().coerceAtLeast(0)

            if (isUnlimited) {
                dailyApps.add(
                    DailyLimitWidgetRow(
                        appName = orig.appName,
                        usageMinutes = usageMin,
                        limitMinutes = 0,
                        progressPercent = 0,
                        isExceeded = false,
                    ),
                )
            } else {
                val limitMin = orig.limitMinutes.coerceAtLeast(1)
                val pct = ((usageMin.toFloat() / limitMin) * 100).toInt().coerceIn(0, 100)
                dailyApps.add(
                    DailyLimitWidgetRow(
                        appName = orig.appName,
                        usageMinutes = usageMin,
                        limitMinutes = limitMin,
                        progressPercent = pct,
                        isExceeded = usageMin > limitMin,
                    ),
                )
            }
        }

        // 초과 앱 우선 정렬
        val sorted = dailyApps.sortedByDescending { it.isExceeded }
        return DailyLimitWidgetData(
            rows = sorted.take(3),
            totalCount = sorted.size,
        )
    }
}
