package com.aptox.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aptox.app.model.AppRestriction

class AppRestrictionRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(restriction: AppRestriction) {
        val list = getAll().toMutableList()
        val wasEmpty = list.isEmpty()
        val index = list.indexOfFirst { it.packageName == restriction.packageName }
        if (index >= 0) list[index] = restriction else list.add(restriction)
        prefs.edit().putString(KEY_RESTRICTIONS, serialize(list)).apply()
        if (wasEmpty && list.isNotEmpty()) {
            Log.d(TAG, "badge_001 트리거: 최초 앱 제한 저장 (package=${restriction.packageName}) → onFirstRestrictionSaved 호출")
            BadgeAutoGrant.onFirstRestrictionSaved(context.applicationContext)
        }
        if (list.size >= 2) {
            Log.d(TAG, "badge_003 트리거: 제한 앱 2개 이상 등록 (package=${restriction.packageName}, count=${list.size})")
            BadgeAutoGrant.onSecondRestrictionSaved(context.applicationContext)
        }
    }

    fun getAll(): List<AppRestriction> = deserialize(prefs.getString(KEY_RESTRICTIONS, null))

    fun toRestrictionMap(): Map<String, Int> = getAll().associate { it.packageName to it.limitMinutes }

    fun delete(packageName: String) {
        val list = getAll().filter { it.packageName != packageName }
        prefs.edit().putString(KEY_RESTRICTIONS, serialize(list)).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_RESTRICTIONS).apply()
    }

    /**
     * 시간 지정 제한 앱 중 blockUntilMs가 지난 항목을 다음날 같은 시각으로 자동 갱신.
     * - startTimeMs, blockUntilMs 모두 +24시간
     * - 갱신된 항목이 있으면 true 반환
     */
    fun renewExpiredTimeSpecifiedRestrictions(): Boolean {
        val list = getAll().toMutableList()
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        var changed = false
        for (i in list.indices) {
            val r = list[i]
            if (r.startTimeMs > 0 && r.blockUntilMs <= now) {
                // 만료된 경우 다음날 같은 시각으로 갱신 (하루씩 전진하여 미래 시각이 될 때까지)
                var newStart = r.startTimeMs
                var newEnd = r.blockUntilMs
                while (newEnd <= now) {
                    newStart += oneDayMs
                    newEnd += oneDayMs
                }
                list[i] = r.copy(startTimeMs = newStart, blockUntilMs = newEnd)
                changed = true
                Log.d(TAG, "시간지정 갱신: ${r.packageName} → start=$newStart end=$newEnd")
            }
        }
        if (changed) {
            prefs.edit().putString(KEY_RESTRICTIONS, serialize(list)).apply()
        }
        return changed
    }

    private fun serialize(list: List<AppRestriction>): String {
        return list.joinToString(SEP_ITEM) {
            "${it.packageName}$SEP_FIELD${it.appName}$SEP_FIELD${it.limitMinutes}$SEP_FIELD${it.blockUntilMs}$SEP_FIELD${it.baselineTimeMs}$SEP_FIELD${it.repeatDays}$SEP_FIELD${it.durationWeeks}$SEP_FIELD${it.startTimeMs}"
        }
    }

    private fun deserialize(string: String?): List<AppRestriction> {
        if (string.isNullOrBlank()) return emptyList()
        return string.split(SEP_ITEM).mapNotNull { line ->
            val parts = line.split(SEP_FIELD)
            if (parts.size >= 3) {
                AppRestriction(
                    packageName = parts[0],
                    appName = parts[1],
                    limitMinutes = parts[2].toIntOrNull() ?: 60,
                    blockUntilMs = parts.getOrNull(3)?.toLongOrNull() ?: 0L,
                    baselineTimeMs = parts.getOrNull(4)?.toLongOrNull() ?: 0L,
                    repeatDays = parts.getOrNull(5) ?: "",
                    durationWeeks = parts.getOrNull(6)?.toIntOrNull() ?: 0,
                    startTimeMs = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                )
            } else null
        }
    }

    companion object {
        private const val TAG = "AppRestrictionRepo"
        private const val PREFS_NAME = "aptox_app_restrictions"
        private const val KEY_RESTRICTIONS = "restrictions"
        private const val SEP_ITEM = "\n"
        private const val SEP_FIELD = "|"
    }
}