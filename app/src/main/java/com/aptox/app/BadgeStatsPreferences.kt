package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 배지 자동 지급용 로컬 카운터·연속일·중복 방지 플래그.
 */
object BadgeStatsPreferences {

    private const val PREFS = "aptox_badge_auto_stats"

    private const val KEY_MIDNIGHT_PROCESSED_DATE = "midnight_processed_yyyyMMdd"
    private const val KEY_RESTRICTION_STREAK = "restriction_streak"
    private const val KEY_DAILY_GOAL_CUMULATIVE = "daily_goal_cumulative_days"
    private const val KEY_TIME_BLOCK_SUCCESS_TOTAL = "time_block_success_total"
    private const val KEY_NIGHT_STREAK = "night_block_streak"
    private const val KEY_WEEKLY_REDUCTION_CHECK_WEEK_START = "weekly_reduction_check_week_start_ms"
    private const val KEY_DEFENSE_TOTAL = "block_defense_total"
    private const val KEY_COUNT_START_TOTAL = "count_start_total"
    private const val KEY_COUNT_END_WITHIN_LIMIT_TOTAL = "count_end_within_limit_total"
    private const val KEY_NIGHT_10PM_CUMULATIVE = "night_10pm_cumulative_days"
    private const val KEY_NIGHT_9PM_CUMULATIVE = "night_9pm_cumulative_days"
    private const val PREFIX_TIME_EXPIRY = "time_expiry_done_"
    private const val PREFIX_NIGHT_DAY = "night_ok_"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getMidnightProcessedDate(ctx: Context): String? =
        prefs(ctx).getString(KEY_MIDNIGHT_PROCESSED_DATE, null)

    /**
     * 동일 날짜 자정 로직 1회만 실행. 멀티캐스트/재전송 대비.
     * @return true 이면 이번에 락을 획득해 처리 진행, false 이면 이미 처리됨
     */
    fun tryMarkMidnightProcessed(ctx: Context, yyyyMMdd: String): Boolean {
        val p = prefs(ctx)
        synchronized(this) {
            if (p.getString(KEY_MIDNIGHT_PROCESSED_DATE, null) == yyyyMMdd) return false
            p.edit().putString(KEY_MIDNIGHT_PROCESSED_DATE, yyyyMMdd).commit()
            return true
        }
    }

    fun getRestrictionStreak(ctx: Context): Int =
        prefs(ctx).getInt(KEY_RESTRICTION_STREAK, 0)

    fun setRestrictionStreak(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_RESTRICTION_STREAK, value.coerceAtLeast(0)).apply()
    }

    fun getDailyGoalCumulative(ctx: Context): Int =
        prefs(ctx).getInt(KEY_DAILY_GOAL_CUMULATIVE, 0)

    fun setDailyGoalCumulative(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_DAILY_GOAL_CUMULATIVE, value.coerceAtLeast(0)).apply()
    }

    fun getTimeBlockSuccessTotal(ctx: Context): Int =
        prefs(ctx).getInt(KEY_TIME_BLOCK_SUCCESS_TOTAL, 0)

    fun setTimeBlockSuccessTotal(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_TIME_BLOCK_SUCCESS_TOTAL, value.coerceAtLeast(0)).apply()
    }

    fun getNightStreak(ctx: Context): Int =
        prefs(ctx).getInt(KEY_NIGHT_STREAK, 0)

    fun setNightStreak(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_NIGHT_STREAK, value.coerceAtLeast(0)).apply()
    }

    fun markNightSuccessForCalendarDay(ctx: Context, yyyyMMdd: String) {
        prefs(ctx).edit().putBoolean(PREFIX_NIGHT_DAY + yyyyMMdd, true).apply()
    }

    fun consumeNightSuccessForDay(ctx: Context, yyyyMMdd: String): Boolean {
        val p = prefs(ctx)
        val key = PREFIX_NIGHT_DAY + yyyyMMdd
        if (!p.getBoolean(key, false)) return false
        p.edit().remove(key).apply()
        return true
    }

    /** 동일 blockUntil 만료 이벤트에 대해 배지 로직 1회만 */
    fun tryConsumeTimeBlockExpiry(ctx: Context, packageName: String, blockUntilMs: Long): Boolean {
        val key = PREFIX_TIME_EXPIRY + packageName + "_" + blockUntilMs
        val p = prefs(ctx)
        if (p.getBoolean(key, false)) return false
        p.edit().putBoolean(key, true).apply()
        return true
    }

    fun getWeeklyReductionCheckWeekStart(ctx: Context): Long =
        prefs(ctx).getLong(KEY_WEEKLY_REDUCTION_CHECK_WEEK_START, 0L)

    fun setWeeklyReductionCheckWeekStart(ctx: Context, weekStartMs: Long) {
        prefs(ctx).edit().putLong(KEY_WEEKLY_REDUCTION_CHECK_WEEK_START, weekStartMs).apply()
    }

    fun getDefenseTotal(ctx: Context): Int =
        prefs(ctx).getInt(KEY_DEFENSE_TOTAL, 0)

    fun incrementDefenseTotal(ctx: Context): Int {
        val p = prefs(ctx)
        val next = p.getInt(KEY_DEFENSE_TOTAL, 0) + 1
        p.edit().putInt(KEY_DEFENSE_TOTAL, next).apply()
        return next
    }

    /** 카운트 시작 버튼 누른 횟수 (badge_002용) */
    fun getCountStartTotal(ctx: Context): Int =
        prefs(ctx).getInt(KEY_COUNT_START_TOTAL, 0)

    fun incrementCountStartTotal(ctx: Context): Int {
        val p = prefs(ctx)
        val next = p.getInt(KEY_COUNT_START_TOTAL, 0) + 1
        p.edit().putInt(KEY_COUNT_START_TOTAL, next).apply()
        return next
    }

    /** 제한 시간 내 카운트 종료 횟수 (badge_007용) */
    fun getCountEndWithinLimitTotal(ctx: Context): Int =
        prefs(ctx).getInt(KEY_COUNT_END_WITHIN_LIMIT_TOTAL, 0)

    fun incrementCountEndWithinLimitTotal(ctx: Context): Int {
        val p = prefs(ctx)
        val next = p.getInt(KEY_COUNT_END_WITHIN_LIMIT_TOTAL, 0) + 1
        p.edit().putInt(KEY_COUNT_END_WITHIN_LIMIT_TOTAL, next).apply()
        return next
    }

    /** 밤 10시 이후 제한 앱 미사용 누적 일수 (badge_010~012) */
    fun getNight10pmCumulative(ctx: Context): Int =
        prefs(ctx).getInt(KEY_NIGHT_10PM_CUMULATIVE, 0)

    fun setNight10pmCumulative(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_NIGHT_10PM_CUMULATIVE, value.coerceAtLeast(0)).apply()
    }

    /** 밤 9시 이후 제한 앱 미사용 누적 일수 (badge_013~015) */
    fun getNight9pmCumulative(ctx: Context): Int =
        prefs(ctx).getInt(KEY_NIGHT_9PM_CUMULATIVE, 0)

    fun setNight9pmCumulative(ctx: Context, value: Int) {
        prefs(ctx).edit().putInt(KEY_NIGHT_9PM_CUMULATIVE, value.coerceAtLeast(0)).apply()
    }

    fun resetAll(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
