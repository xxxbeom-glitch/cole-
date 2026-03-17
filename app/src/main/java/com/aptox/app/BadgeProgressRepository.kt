package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 뱃지 달성 조건 수치 저장 (누적/연속 달성일).
 * 디버그 테스트 및 실제 뱃지 조건 체크 시 사용.
 */
class BadgeProgressRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var accumulatedAchievementDays: Int
        get() = prefs.getInt(KEY_ACCUMULATED_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_ACCUMULATED_DAYS, value.coerceAtLeast(0)).apply()

    var consecutiveAchievementDays: Int
        get() = prefs.getInt(KEY_CONSECUTIVE_DAYS, 0)
        set(value) = prefs.edit().putInt(KEY_CONSECUTIVE_DAYS, value.coerceAtLeast(0)).apply()

    fun resetAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "aptox_badge_progress"
        private const val KEY_ACCUMULATED_DAYS = "accumulated_achievement_days"
        private const val KEY_CONSECUTIVE_DAYS = "consecutive_achievement_days"
    }
}
