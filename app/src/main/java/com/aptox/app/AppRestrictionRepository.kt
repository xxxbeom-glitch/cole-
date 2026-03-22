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

    private fun serialize(list: List<AppRestriction>): String {
        return list.joinToString(SEP_ITEM) {
            "${it.packageName}$SEP_FIELD${it.appName}$SEP_FIELD${it.limitMinutes}$SEP_FIELD${it.blockUntilMs}$SEP_FIELD${it.baselineTimeMs}$SEP_FIELD${it.repeatDays}$SEP_FIELD${it.durationWeeks}"
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