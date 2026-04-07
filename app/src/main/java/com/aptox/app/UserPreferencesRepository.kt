package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 사용자 설정 SharedPreferences.
 * 홈 인사 등에 쓰이던 로컬 표시 이름은 제거됨 — [migrateClearLegacyHomeUserNameIfNeeded]로 구버전 데이터만 정리.
 */
class UserPreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** @deprecated 홈 인사에는 더 이상 사용하지 않음. 마이그레이션 후 항상 null에 가깝게 유지됨. */
    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value?.trim().takeIf { it?.isNotBlank() != false } ?: "").apply()
        }

    companion object {
        private const val PREFS_NAME = "aptox_user_prefs"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_LEGACY_HOME_USER_NAME_CLEARED = "legacy_home_user_name_cleared_v1"

        /**
         * 온보딩 이름 입력 제거 이후 1회: 로컬 `user_name` 제거(기존 사용자 업데이트 포함).
         * 신규 설치에서도 무해하다.
         */
        fun migrateClearLegacyHomeUserNameIfNeeded(context: Context) {
            val app = context.applicationContext
            val p = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (p.getBoolean(KEY_LEGACY_HOME_USER_NAME_CLEARED, false)) return
            p.edit()
                .remove(KEY_USER_NAME)
                .putBoolean(KEY_LEGACY_HOME_USER_NAME_CLEARED, true)
                .apply()
        }
    }
}
