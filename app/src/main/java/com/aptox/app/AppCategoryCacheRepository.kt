package com.aptox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

/**
 * 앱 카테고리 분류(classifyApps) 결과를 DataStore에 캐시.
 * - 캐시 키: 패키지명, 값: 카테고리 문자열
 * - 앱 삭제 시 해당 패키지는 캐시에서 제거
 */
class AppCategoryCacheRepository(private val context: Context) {

    suspend fun getCache(): Map<String, String> = context.appCategoryCacheDataStore.data
        .map { prefs ->
            val json = prefs[CACHE_JSON] ?: return@map emptyMap<String, String>()
            runCatching {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            }.getOrElse { emptyMap() }
        }
        .first()

    suspend fun saveCategories(newEntries: Map<String, String>) {
        context.appCategoryCacheDataStore.edit { prefs ->
            val current = prefs[CACHE_JSON]?.let { json ->
                runCatching { JSONObject(json).keys().asSequence().associateWith { JSONObject(json).getString(it) } }.getOrElse { emptyMap() }
            } ?: emptyMap()
            val merged = current + newEntries
            prefs[CACHE_JSON] = JSONObject(merged as Map<*, *>).toString()
        }
    }

    /**
     * 현재 설치된 앱 패키지 목록에 맞춰 캐시 정리.
     * 설치되지 않은 앱(삭제됨)은 캐시에서 제거.
     */
    suspend fun pruneUninstalled(installedPackages: Set<String>) {
        context.appCategoryCacheDataStore.edit { prefs ->
            val json = prefs[CACHE_JSON] ?: return@edit
            val current = runCatching {
                val obj = JSONObject(json)
                obj.keys().asSequence().associateWith { obj.getString(it) }
            }.getOrElse { emptyMap() }
            val pruned = current.filterKeys { it in installedPackages }
            if (pruned.isEmpty()) {
                prefs.remove(CACHE_JSON)
            } else {
                prefs[CACHE_JSON] = JSONObject(pruned as Map<*, *>).toString()
            }
        }
    }

    /** AI 분류 캐시 전체 삭제 (디버그·재분류 테스트용) */
    suspend fun clearAll() {
        context.appCategoryCacheDataStore.edit { prefs ->
            prefs.remove(CACHE_JSON)
        }
    }

    companion object {
        private val Context.appCategoryCacheDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "app_category_cache"
        )
        private val CACHE_JSON = stringPreferencesKey("category_map_json")
    }
}
