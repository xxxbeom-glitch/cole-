package com.aptox.app.widget

import android.content.Context
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.DailyUsageLimitConstants
import com.aptox.app.ManualTimerRepository
import com.aptox.app.PauseRepository
import com.aptox.app.RestrictionDeleteHelper
import java.util.Calendar

/**
 * 홈 위젯 4×2 집계 데이터.
 * 지정 시간 제한 / 하루 사용량 제한 두 섹션의 표시값을 담는다.
 */
data class WidgetSummaryData(
    // ── 지정 시간 제한 섹션 ──
    /** 등록된 지정 시간 제한 앱 수 */
    val timeSpecifiedTotal: Int,
    /** 현재 제한 창(window) 안에서 활성 중인 앱 수 */
    val timeSpecifiedActive: Int,
    /** 제한 중 / 전체 비율 0~100 */
    val timeSpecifiedProgressPercent: Int,

    // ── 하루 사용량 제한 섹션 ──
    /** 등록된 하루 사용량 제한 앱 수 (만료 제거 후) */
    val dailyUsageTotal: Int,
    /** 오늘 사용량 합계 (분, 비무제한 앱 기준) */
    val dailyUsageTotalMinutes: Int,
    /** 한도 합계 (분, 비무제한 앱 기준); 0 이면 모두 무제한 */
    val dailyLimitTotalMinutes: Int,
    /** 사용량 / 한도 비율 0~100 */
    val dailyUsageProgressPercent: Int,
    /** 한도 합계 초과 여부 (진행색 전환용) */
    val dailyIsExceeded: Boolean,
)

private const val TIME_SPEC_ONE_DAY_MS = 24L * 60 * 60 * 1000

private fun todayDayIndex(): Int {
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    return if (dow == Calendar.SUNDAY) 6 else dow - 2
}

private fun parseRepeatDays(repeatDays: String): Set<Int> =
    repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..6 }.toSet()

private fun isDurationExpired(baselineTimeMs: Long, durationWeeks: Int): Boolean {
    if (durationWeeks <= 0) return false
    val endMs = baselineTimeMs + durationWeeks * 7L * 24 * 60 * 60 * 1000
    return System.currentTimeMillis() >= endMs
}

private fun isTodayOnlyExpired(baselineTimeMs: Long): Boolean {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return baselineTimeMs < cal.timeInMillis
}

private fun rollToNextTimeSpecifiedWindow(startTimeMs: Long, blockUntilMs: Long, now: Long): Pair<Long, Long> {
    var s = startTimeMs
    var e = blockUntilMs
    while (e <= now) {
        s += TIME_SPEC_ONE_DAY_MS
        e += TIME_SPEC_ONE_DAY_MS
    }
    return s to e
}

object RestrictionWidgetDataLoader {

    /** 로딩 실패·폴백용 — 전부 0, 미초과 */
    fun emptySummary(): WidgetSummaryData = WidgetSummaryData(
        timeSpecifiedTotal = 0,
        timeSpecifiedActive = 0,
        timeSpecifiedProgressPercent = 0,
        dailyUsageTotal = 0,
        dailyUsageTotalMinutes = 0,
        dailyLimitTotalMinutes = 0,
        dailyUsageProgressPercent = 0,
        dailyIsExceeded = false,
    )

    fun loadSummary(context: Context): WidgetSummaryData {
        val app = context.applicationContext
        val repo = AppRestrictionRepository(app)
        val restrictions = repo.getAll()

        if (restrictions.isEmpty()) return emptySummary()

        val pauseRepo = PauseRepository(app)
        val timerRepo = ManualTimerRepository(app)
        val now = System.currentTimeMillis()

        var timeSpecTotal = 0
        var timeSpecActive = 0

        var dailyTotal = 0
        var dailySumUsageMsActual = 0L  // 표시용 실제 사용량
        var dailySumUsageMsCapped = 0L  // 프로그레스용 (한도 상한)
        var dailySumLimitMs = 0L

        for (orig in restrictions) {
            val isTimeSpecified = orig.startTimeMs > 0

            if (isTimeSpecified) {
                val restriction = if (orig.blockUntilMs <= now) {
                    repo.renewExpiredTimeSpecifiedRestrictions()
                    repo.getAll().find { it.packageName == orig.packageName } ?: continue
                } else orig

                val isPaused = pauseRepo.isPaused(restriction.packageName)
                val (winStart, winEnd) = rollToNextTimeSpecifiedWindow(
                    restriction.startTimeMs,
                    restriction.blockUntilMs,
                    now,
                )
                val isActive = !isPaused && now >= winStart && now < winEnd

                timeSpecTotal++
                if (isActive) timeSpecActive++
            } else {
                // 만료 체크
                val repeatDaySet = parseRepeatDays(orig.repeatDays)
                if (repeatDaySet.isEmpty()) {
                    if (isTodayOnlyExpired(orig.baselineTimeMs)) {
                        RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                        continue
                    }
                } else {
                    if (isDurationExpired(orig.baselineTimeMs, orig.durationWeeks)) {
                        RestrictionDeleteHelper.deleteRestrictedApp(app, orig.packageName, logRelease = false)
                        continue
                    }
                }

                dailyTotal++

                val isDailyUnlimited =
                    orig.limitMinutes >= DailyUsageLimitConstants.UNLIMITED_MINUTES_SENTINEL
                if (!isDailyUnlimited) {
                    val limitMs = orig.limitMinutes * 60L * 1000L
                    val usageMs = timerRepo.getTodayUsageMs(orig.packageName)
                    dailySumLimitMs += limitMs
                    dailySumUsageMsActual += usageMs
                    dailySumUsageMsCapped += usageMs.coerceAtMost(limitMs)
                }
            }
        }

        val timeSpecProgress = if (timeSpecTotal > 0)
            (timeSpecActive * 100 / timeSpecTotal).coerceIn(0, 100)
        else 0

        val dailyUsageMinutes = (dailySumUsageMsActual / 60_000L).toInt()
        val dailyLimitMinutes = (dailySumLimitMs / 60_000L).toInt()
        val dailyProgress = if (dailySumLimitMs > 0)
            ((dailySumUsageMsCapped.toFloat() / dailySumLimitMs) * 100).toInt().coerceIn(0, 100)
        else 0
        val dailyExceeded = dailySumLimitMs > 0 && dailySumUsageMsActual > dailySumLimitMs

        return WidgetSummaryData(
            timeSpecifiedTotal = timeSpecTotal,
            timeSpecifiedActive = timeSpecActive,
            timeSpecifiedProgressPercent = timeSpecProgress,
            dailyUsageTotal = dailyTotal,
            dailyUsageTotalMinutes = dailyUsageMinutes,
            dailyLimitTotalMinutes = dailyLimitMinutes,
            dailyUsageProgressPercent = dailyProgress,
            dailyIsExceeded = dailyExceeded,
        )
    }

}
