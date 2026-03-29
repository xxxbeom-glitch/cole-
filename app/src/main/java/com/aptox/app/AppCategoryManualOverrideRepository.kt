package com.aptox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 사용자가 수동으로 수정한 앱 카테고리를 DataStore에 저장/조회.
 * - 저장 키: "manual_category_overrides"
 * - 형식: Map<packageName, categoryString> → JSON 직렬화
 * - AI 분류 캐시보다 항상 우선 적용됨
 */
class AppCategoryManualOverrideRepository(private val context: Context) {

    /** 전체 수동 오버라이드 맵 반환 */
    suspend fun getAll(): Map<String, String> = withContext(Dispatchers.IO) {
        context.manualOverrideDataStore.data
            .map { prefs ->
                val json = prefs[OVERRIDES_KEY] ?: return@map emptyMap<String, String>()
                runCatching {
                    val obj = JSONObject(json)
                    obj.keys().asSequence().associateWith { obj.getString(it) }
                }.getOrElse { emptyMap() }
            }
            .first()
    }

    /** 특정 앱의 수동 오버라이드 카테고리 반환 (없으면 null) */
    suspend fun get(packageName: String): String? = withContext(Dispatchers.IO) {
        getAll()[packageName]
    }

    /** 특정 앱의 카테고리를 수동으로 저장/업데이트 */
    suspend fun save(packageName: String, category: String) = withContext(Dispatchers.IO) {
        context.manualOverrideDataStore.edit { prefs ->
            val current = prefs[OVERRIDES_KEY]?.let { json ->
                runCatching {
                    val obj = JSONObject(json)
                    obj.keys().asSequence().associateWith { obj.getString(it) }
                }.getOrElse { emptyMap() }
            } ?: emptyMap()
            val updated = current + (packageName to category)
            prefs[OVERRIDES_KEY] = JSONObject(updated as Map<*, *>).toString()
        }
    }

    companion object {
        private val Context.manualOverrideDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "app_category_manual_overrides"
        )
        private val OVERRIDES_KEY = stringPreferencesKey("manual_category_overrides")
    }
}
