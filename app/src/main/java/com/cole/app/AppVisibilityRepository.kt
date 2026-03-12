package com.cole.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 화면에 윈도우가 보이는 앱 패키지 저장소.
 * AccessibilityService.getWindows()로 PiP 등 가시 윈도우를 감지해 저장.
 * UsageStatsUtils, AppMonitorService에서 PiP 시 카운팅 유지를 위한 보정에 사용.
 */
class AppVisibilityRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setPackagesWithVisibleWindows(packages: Set<String>) {
        prefs.edit().putString(KEY_VISIBLE, packages.joinToString(SEP)).apply()
    }

    fun getPackagesWithVisibleWindows(): Set<String> {
        val raw = prefs.getString(KEY_VISIBLE, null) ?: return emptySet()
        return raw.split(SEP).filter { it.isNotBlank() }.toSet()
    }

    fun hasVisibleWindow(packageName: String): Boolean =
        getPackagesWithVisibleWindows().contains(packageName)

    companion object {
        private const val PREFS_NAME = "cole_app_visibility"
        private const val KEY_VISIBLE = "visible_packages"
        private const val SEP = ","
    }
}
