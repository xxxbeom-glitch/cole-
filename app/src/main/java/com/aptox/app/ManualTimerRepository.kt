package com.aptox.app

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

/**
 * 수동 타이머 기반 사용시간 저장.
 * - startSession: 카운트 시작
 * - endSession: 카운트 정지 후 경과시간 누적
 * - getTodayUsageMs: 오늘 누적 사용시간 (진행 중 세션 포함)
 * - isSessionActive: 현재 카운트 진행 중 여부
 */
class ManualTimerRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** 오늘 날짜 yyyyMMdd */
    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return String.format("%04d%02d%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun accumKey(packageName: String): String = "accum_${packageName}_${todayKey()}"
    private fun activeKey(packageName: String): String = "active_$packageName"

    /**
     * 카운트 시작. 기존 활성 세션이 있으면 무시 (덮어쓰기).
     */
    fun startSession(packageName: String) {
        prefs.edit()
            .putLong(activeKey(packageName), System.currentTimeMillis())
            .apply()
    }

    /**
     * 카운트 정지. 경과시간을 오늘 누적에 추가.
     */
    fun endSession(packageName: String) {
        val startMs = prefs.getLong(activeKey(packageName), -1L)
        if (startMs < 0) return
        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(0)
        val key = accumKey(packageName)
        val existing = prefs.getLong(key, 0L)
        prefs.edit()
            .putLong(key, existing + elapsed)
            .remove(activeKey(packageName))
            .apply()
    }

    /**
     * 오늘 누적 사용시간(ms). 진행 중 세션 있으면 현재까지 경과 포함.
     */
    fun getTodayUsageMs(packageName: String): Long {
        val accum = prefs.getLong(accumKey(packageName), 0L)
        val startMs = prefs.getLong(activeKey(packageName), -1L)
        val activeElapsed = if (startMs >= 0) (System.currentTimeMillis() - startMs).coerceAtLeast(0) else 0L
        return accum + activeElapsed
    }

    /** 현재 카운트 진행 중 여부 */
    fun isSessionActive(packageName: String): Boolean =
        prefs.getLong(activeKey(packageName), -1L) >= 0

    companion object {
        private const val PREFS_NAME = "aptox_manual_timer"
    }
}
