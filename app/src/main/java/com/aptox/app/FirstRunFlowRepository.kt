package com.aptox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.firstRunDataStore: DataStore<Preferences> by preferencesDataStore(name = "aptox_first_run")

private val KEY_ONBOARDING_FLOW_COMPLETED = booleanPreferencesKey("onboarding_flow_completed")

/**
 * 최초 1회 온보딩(이름·자가테스트 → 로딩 → [DiagnosisResultScreen]) 완료 여부.
 * (DataStore 키명 onboarding_flow_completed 유지)
 */
class FirstRunFlowRepository(context: Context) {

    private val appContext = context.applicationContext

    private val dataStore: DataStore<Preferences>
        get() = appContext.firstRunDataStore

    suspend fun isOnboardingFlowCompleted(): Boolean =
        dataStore.data
            .map { it[KEY_ONBOARDING_FLOW_COMPLETED] == true }
            .first()

    suspend fun setOnboardingFlowCompleted(completed: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_FLOW_COMPLETED] = completed
        }
    }
}
