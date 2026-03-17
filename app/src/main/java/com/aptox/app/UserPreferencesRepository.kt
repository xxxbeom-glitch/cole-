package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 사용자 설정 SharedPreferences.
 * 온보딩에서 입력한 이름 등 앱 전역 사용자 데이터 저장.
 */
class UserPreferencesRepository(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value?.trim().takeIf { it?.isNotBlank() != false } ?: "").apply()
        }

    companion object {
        private const val PREFS_NAME = "aptox_user_prefs"
        private const val KEY_USER_NAME = "user_name"
    }
}
