package com.cole.app

import android.content.Context

/**
 * 일시정지 상태 저장소
 * - 오늘 사용 횟수 (최대 2회)
 * - 일시정지 종료 시각 (ms)
 */
class PauseRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 오늘 일시정지 사용 횟수 */
    fun getTodayCount(packageName: String): Int {
        resetIfNewDay()
        return prefs.getInt(keyCount(packageName), 0)
    }

    /** 일시정지 1회 사용 & 종료 시각 저장 */
    fun startPause(packageName: String, pauseMinutes: Int) {
        resetIfNewDay()
        val count = getTodayCount(packageName)
        val until = System.currentTimeMillis() + pauseMinutes * 60L * 1000L
        prefs.edit()
            .putInt(keyCount(packageName), count + 1)
            .putLong(keyUntil(packageName), until)
            .apply()
    }

    /** 현재 일시정지 중인지 */
    fun isPaused(packageName: String): Boolean {
        return System.currentTimeMillis() < prefs.getLong(keyUntil(packageName), 0L)
    }

    /** 일시정지 종료 시각 (ms) */
    fun getPauseUntilMs(packageName: String): Long {
        return prefs.getLong(keyUntil(packageName), 0L)
    }

    /** 남은 일시정지 횟수 */
    fun getRemainingCount(packageName: String, maxCount: Int = 2): Int {
        return (maxCount - getTodayCount(packageName)).coerceAtLeast(0)
    }

    private fun resetIfNewDay() {
        val today = todayKey()
        if (prefs.getString(KEY_DATE, "") != today) {
            prefs.edit().clear().putString(KEY_DATE, today).apply()
        }
    }

    private fun todayKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    private fun keyCount(pkg: String) = "count_$pkg"
    private fun keyUntil(pkg: String) = "until_$pkg"

    companion object {
        private const val PREFS_NAME = "cole_pause"
        private const val KEY_DATE = "date"
    }
}