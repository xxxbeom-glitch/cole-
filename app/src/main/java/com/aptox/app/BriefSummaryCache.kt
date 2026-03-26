package com.aptox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Brief AI 요약 캐시 (DataStore 영속 저장)
 * - 앱 재시작 후에도 유지
 * - 키: WEEKLY_yyyyMMdd, MONTHLY_yyyyMM, YEARLY_yyyy
 */
private val Context.briefSummaryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "brief_summary_cache",
)

object BriefSummaryCache {

    data class Entry(
        val title: String,
        val body: String,
    )

    private fun titleKey(key: String) = stringPreferencesKey("${key}_title")
    private fun bodyKey(key: String) = stringPreferencesKey("${key}_body")

    suspend fun get(context: Context, key: String): Entry? {
        val prefs = context.applicationContext.briefSummaryDataStore.data.first()
        val title = prefs[titleKey(key)]
        val body = prefs[bodyKey(key)]
        return if (title != null && body != null) Entry(title, body) else null
    }

    suspend fun put(context: Context, key: String, entry: Entry) {
        context.applicationContext.briefSummaryDataStore.edit { prefs ->
            prefs[titleKey(key)] = entry.title
            prefs[bodyKey(key)] = entry.body
        }
    }

    suspend fun has(context: Context, key: String): Boolean = get(context, key) != null

    suspend fun remove(context: Context, key: String) {
        context.applicationContext.briefSummaryDataStore.edit { prefs ->
            prefs.remove(titleKey(key))
            prefs.remove(bodyKey(key))
        }
    }
}
