package com.aptox.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 비로그인 시 앱 사용제한 기록 이벤트를 로컬에 임시 저장.
 * 로그인 시 Firestore로 동기화 후 [clear]로 초기화.
 * (뱃지 [PendingBadgesPreferences]와 동일한 역할)
 */
object AppLimitLogLocalPreferences {

    private const val PREFS = "aptox_app_limit_log_local"
    /** [SharedPreferences.OnSharedPreferenceChangeListener] / Flow 갱신용 */
    const val EVENTS_KEY = "events_json"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun appendEvent(
        ctx: Context,
        packageName: String,
        appName: String,
        eventType: String,
        timestampMs: Long = System.currentTimeMillis(),
    ) {
        val arr = readArray(ctx)
        val o = JSONObject()
        o.put("id", UUID.randomUUID().toString())
        o.put("packageName", packageName)
        o.put("appName", appName.ifBlank { packageName })
        o.put("eventType", eventType)
        o.put("timestampMs", timestampMs)
        arr.put(o)
        prefs(ctx).edit().putString(EVENTS_KEY, arr.toString()).apply()
    }

    fun getAllEvents(ctx: Context): List<LocalAppLimitLogEvent> {
        val arr = readArray(ctx)
        val out = ArrayList<LocalAppLimitLogEvent>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                LocalAppLimitLogEvent(
                    id = o.optString("id", ""),
                    packageName = o.optString("packageName", ""),
                    appName = o.optString("appName", ""),
                    eventType = o.optString("eventType", ""),
                    timestampMs = o.optLong("timestampMs", 0L),
                ),
            )
        }
        return out
    }

    fun getEventsForPackage(ctx: Context, packageName: String): List<LocalAppLimitLogEvent> {
        return getAllEvents(ctx)
            .filter { it.packageName == packageName }
            .sortedBy { it.timestampMs }
    }

    /**
     * 이벤트가 하나라도 있는 패키지 목록 (appName은 해당 패키지 이벤트 중 가장 최근 것 사용).
     */
    fun getPackages(ctx: Context): List<AppLimitLogPackage> {
        val byPkg = getAllEvents(ctx).groupBy { it.packageName }
        return byPkg.map { (pkg, events) ->
            val latest = events.maxByOrNull { it.timestampMs } ?: return@map null
            AppLimitLogPackage(
                packageName = pkg,
                appName = latest.appName.ifBlank { pkg },
            )
        }.filterNotNull().sortedBy { it.appName }
    }

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(EVENTS_KEY).apply()
    }

    fun registerListener(ctx: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(ctx).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(ctx: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(ctx).unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readArray(ctx: Context): JSONArray {
        val s = prefs(ctx).getString(EVENTS_KEY, null) ?: return JSONArray()
        return runCatching { JSONArray(s) }.getOrElse { JSONArray() }
    }
}

data class LocalAppLimitLogEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val eventType: String,
    val timestampMs: Long,
)
