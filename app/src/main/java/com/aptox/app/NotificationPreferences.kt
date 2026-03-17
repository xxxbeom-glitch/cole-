package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 알림 설정 SharedPreferences 저장.
 * - 각 토글 상태 저장
 * - 앱 재시작 후에도 유지
 */
object NotificationPreferences {

    private const val PREFS_NAME = "aptox_notification_prefs"
    private const val KEY_WEEKLY_REPORT = "weekly_report_enabled"
    private const val KEY_DEADLINE_IMMINENT = "deadline_imminent_enabled"
    private const val KEY_GOAL_ACHIEVED = "goal_achieved_enabled"
    private const val KEY_COUNT_REMINDER = "count_reminder_enabled"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isWeeklyReportEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WEEKLY_REPORT, true)

    fun setWeeklyReportEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_WEEKLY_REPORT, enabled).apply()
    }

    fun isDeadlineImminentEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DEADLINE_IMMINENT, true)

    fun setDeadlineImminentEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DEADLINE_IMMINENT, enabled).apply()
    }

    fun isGoalAchievedEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_GOAL_ACHIEVED, true)

    fun setGoalAchievedEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_GOAL_ACHIEVED, enabled).apply()
    }

    fun isCountReminderEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COUNT_REMINDER, true)

    fun setCountReminderEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COUNT_REMINDER, enabled).apply()
    }
}
