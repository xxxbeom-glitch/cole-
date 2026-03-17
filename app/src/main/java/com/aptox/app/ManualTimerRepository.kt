package com.aptox.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.Calendar

/**
 * 수동 타이머 기반 사용시간 저장.
 * - startSession: 카운트 시작
 * - endSession: 카운트 정지 후 경과시간 누적
 * - getTodayUsageMs: 오늘 누적 사용시간 (진행 중 세션 포함, 자정 기준 리셋)
 * - isSessionActive: 현재 카운트 진행 중 여부
 * - 자정을 넘긴 활성 세션은 자동으로 오늘 00:00 이후 구간만 카운트.
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

    /** 오늘 00:00 시각(ms) */
    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun accumKey(packageName: String): String = "accum_${packageName}_${todayKey()}"
    private fun activeKey(packageName: String): String = "active_$packageName"

    /**
     * 카운트 시작. 기존 활성 세션이 있으면 무시 (덮어쓰기).
     * 날짜가 바뀌었으면 기존 세션을 정리한 뒤 시작.
     */
    fun startSession(packageName: String) {
        resetStaleSessionIfDateChanged(packageName)
        prefs.edit()
            .putLong(activeKey(packageName), System.currentTimeMillis())
            .apply()
    }

    /**
     * 카운트 정지. 경과시간을 오늘 누적에 추가.
     * 자정 이전에 시작된 세션은 자정 이후 구간이 없으면(즉시 종료) 누적하지 않고 세션만 삭제.
     * 자정을 넘긴 세션은 오늘 00:00 이후 구간만 누적.
     */
    fun endSession(packageName: String) {
        val startMs = prefs.getLong(activeKey(packageName), -1L)
        if (startMs < 0) return
        val now = System.currentTimeMillis()
        val midnight = todayMidnightMs()
        // 자정 이전에 시작된 세션: active_를 그냥 삭제 (오늘 사용량에 누적하지 않음)
        if (startMs < midnight) {
            prefs.edit().remove(activeKey(packageName)).apply()
            Log.d(TAG, "자정 이전 세션 종료 - 오늘 사용량 미누적: $packageName (startMs=$startMs)")
            return
        }
        val elapsed = (now - startMs).coerceAtLeast(0)
        val key = accumKey(packageName)
        val existing = prefs.getLong(key, 0L)
        prefs.edit()
            .putLong(key, existing + elapsed)
            .remove(activeKey(packageName))
            .apply()
    }

    /**
     * 오늘 누적 사용시간(ms). 진행 중 세션 있으면 오늘 00:00 이후 경과만 포함.
     * 자정 이전에 시작된 활성 세션은 오늘 사용량에 포함하지 않음.
     */
    fun getTodayUsageMs(packageName: String): Long {
        val accum = prefs.getLong(accumKey(packageName), 0L)
        val startMs = prefs.getLong(activeKey(packageName), -1L)
        val midnight = todayMidnightMs()
        val activeElapsed = if (startMs >= 0 && startMs >= midnight) {
            (System.currentTimeMillis() - startMs).coerceAtLeast(0)
        } else {
            0L
        }
        return accum + activeElapsed
    }

    /**
     * 날짜가 바뀌었을 때 오래된 활성 세션 정리.
     * 자정 이전에 시작된 세션을 초기화 (오늘부터 새로 카운트).
     */
    fun resetStaleActiveSessionsAtMidnight() {
        val midnight = todayMidnightMs()
        val editor = prefs.edit()
        var cleared = 0
        for ((key, value) in prefs.all) {
            if (key.startsWith("active_") && value is Long && value >= 0 && value < midnight) {
                editor.remove(key)
                cleared++
            }
        }
        if (cleared > 0) {
            editor.apply()
            Log.d(TAG, "자정 리셋: ${cleared}개 활성 세션 정리")
        }
    }

    /**
     * 앱 실행 시 호출. 날짜가 바뀌었으면 자정 이전 활성 세션 정리.
     */
    fun ensureMidnightResetIfNeeded() {
        resetStaleActiveSessionsAtMidnight()
    }

    /**
     * 해당 패키지의 활성 세션이 자정 이전에 시작됐으면 정리.
     */
    private fun resetStaleSessionIfDateChanged(packageName: String) {
        val startMs = prefs.getLong(activeKey(packageName), -1L)
        if (startMs >= 0 && startMs < todayMidnightMs()) {
            prefs.edit().remove(activeKey(packageName)).apply()
        }
    }

    /** 현재 카운트 진행 중 여부 */
    fun isSessionActive(packageName: String): Boolean =
        prefs.getLong(activeKey(packageName), -1L) >= 0

    /**
     * 오늘 누적 사용량 강제 설정 (분 단위). 디버그 테스트용.
     * active 세션은 정리하고, accum만 해당 분*60*1000 ms로 설정.
     */
    fun setTodayUsageMinutes(packageName: String, minutes: Int) {
        prefs.edit()
            .remove(activeKey(packageName))
            .putLong(accumKey(packageName), minutes.toLong() * 60 * 1000)
            .apply()
    }

    /**
     * 해당 앱의 세션·누적 데이터 전체 삭제.
     * 제한 앱 삭제 시 ManualTimerRepository 정리용.
     */
    fun deleteAppData(packageName: String) {
        val editor = prefs.edit()
        val prefixActive = "active_$packageName"
        val prefixAccum = "accum_${packageName}_"
        for ((key, _) in prefs.all) {
            when {
                key == prefixActive -> editor.remove(key)
                key.startsWith(prefixAccum) -> editor.remove(key)
                else -> { /* skip */ }
            }
        }
        editor.apply()
    }

    /**
     * 현재 카운트 진행 중인 앱 정보.
     * @return (packageName, startTimeMs) 또는 없으면 null
     */
    fun getActiveSession(): Pair<String, Long>? {
        val all = prefs.all
        for ((key, value) in all) {
            if (key.startsWith("active_") && value is Long && value >= 0) {
                val pkg = key.removePrefix("active_")
                return pkg to value
            }
        }
        return null
    }

    companion object {
        private const val TAG = "ManualTimerRepository"
        private const val PREFS_NAME = "aptox_manual_timer"
    }
}
