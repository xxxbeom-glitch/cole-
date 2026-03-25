package com.aptox.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Firestore users/{userId}/appLimitLogs/{packageName}/events/{eventId}
 * 비로그인: [AppLimitLogLocalPreferences] (로그인 후 Firestore 동기화·삭제)
 * 앱 사용제한 기록 (카운트 시작/정지/사용자 제한 해제/시간 소진)
 */
data class AppLimitLogEvent(
    val id: String,
    val eventType: String, // start, stop, release, timeout
    val timestamp: Long,
)

data class AppLimitLogPackage(
    val packageName: String,
    val appName: String,
)

class AppLimitLogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * 이벤트 저장.
     * - 로그인: Firestore
     * - 비로그인: 로컬 [AppLimitLogLocalPreferences]
     */
    suspend fun saveEvent(
        context: Context,
        userId: String?,
        packageName: String,
        eventType: String,
        appName: String? = null,
    ) {
        val app = context.applicationContext
        val displayName = appName?.takeIf { it.isNotBlank() } ?: packageName
        if (userId.isNullOrBlank()) {
            AppLimitLogLocalPreferences.appendEvent(app, packageName, displayName, eventType)
            return
        }
        saveEventToFirestore(userId, packageName, eventType, displayName, timestampMs = null)
    }

    private suspend fun saveEventToFirestore(
        userId: String,
        packageName: String,
        eventType: String,
        appName: String,
        timestampMs: Long?,
    ) {
        runCatching {
            val ref = firestore.collection("users").document(userId)
                .collection("appLimitLogs").document(packageName)
                .collection("events").document()
            val ts: Any = if (timestampMs != null) {
                Timestamp(Date(timestampMs))
            } else {
                FieldValue.serverTimestamp()
            }
            ref.set(
                mapOf(
                    "eventType" to eventType,
                    "timestamp" to ts,
                ),
            ).await()
            firestore.collection("users").document(userId)
                .collection("appLimitLogs").document(packageName)
                .set(mapOf("appName" to appName), SetOptions.merge()).await()
        }
    }

    /**
     * appLimitLogs에 기록이 있는 패키지 목록.
     * userId == null 이면 로컬 데이터.
     */
    fun getPackagesWithLogsFlow(context: Context, userId: String?): Flow<List<AppLimitLogPackage>> = callbackFlow {
        val app = context.applicationContext
        if (userId.isNullOrBlank()) {
            fun emitLocal() {
                trySend(AppLimitLogLocalPreferences.getPackages(app))
            }
            emitLocal()
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppLimitLogLocalPreferences.EVENTS_KEY || key == null) {
                    emitLocal()
                }
            }
            AppLimitLogLocalPreferences.registerListener(app, listener)
            awaitClose { AppLimitLogLocalPreferences.unregisterListener(app, listener) }
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId).collection("appLimitLogs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val pkg = doc.id
                    val appName = doc.getString("appName")?.takeIf { it.isNotBlank() } ?: pkg
                    AppLimitLogPackage(packageName = pkg, appName = appName)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 해당 사용자의 앱 사용제한 기록 전체 삭제 (디버그용).
     */
    suspend fun clearAll(userId: String?) {
        if (userId.isNullOrBlank()) return
        val appLimitLogsRef = firestore.collection("users").document(userId).collection("appLimitLogs")
        val packages = appLimitLogsRef.get().await().documents
        for (pkgDoc in packages) {
            val eventsSnap = pkgDoc.reference.collection("events").get().await()
            for (eventDoc in eventsSnap.documents) {
                eventDoc.reference.delete().await()
            }
            pkgDoc.reference.delete().await()
        }
    }

    /**
     * 해당 앱의 이벤트 목록. 시간순.
     * userId == null 이면 로컬 데이터.
     */
    fun getEventsFlow(context: Context, userId: String?, packageName: String): Flow<List<AppLimitLogEvent>> = callbackFlow {
        val app = context.applicationContext
        if (packageName.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        if (userId.isNullOrBlank()) {
            fun emitLocal() {
                val list = AppLimitLogLocalPreferences.getEventsForPackage(app, packageName).map { e ->
                    AppLimitLogEvent(id = e.id, eventType = e.eventType, timestamp = e.timestampMs)
                }
                trySend(list)
            }
            emitLocal()
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == AppLimitLogLocalPreferences.EVENTS_KEY || key == null) {
                    emitLocal()
                }
            }
            AppLimitLogLocalPreferences.registerListener(app, listener)
            awaitClose { AppLimitLogLocalPreferences.unregisterListener(app, listener) }
            return@callbackFlow
        }
        val listener = firestore.collection("users").document(userId)
            .collection("appLimitLogs").document(packageName)
            .collection("events")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val eventType = doc.getString("eventType") ?: return@mapNotNull null
                    val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    AppLimitLogEvent(id = doc.id, eventType = eventType, timestamp = timestamp)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    companion object {
        private const val TAG = "AppLimitLogRepo"
        private const val PREFS_TIMEOUT = "aptox_app_limit_log_timeout"
        private const val KEY_TIMEOUT_DATE = "timeout_date"
        private const val KEY_TIMEOUT_PKGS = "timeout_pkgs"

        /**
         * 로그인 직후: 로컬에 쌓인 이벤트를 Firestore로 옮긴 뒤 로컬 삭제.
         */
        suspend fun syncPendingLocalToFirestore(context: Context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val app = context.applicationContext
            val events = AppLimitLogLocalPreferences.getAllEvents(app)
            if (events.isEmpty()) return
            val repo = AppLimitLogRepository()
            runCatching {
                for (e in events.sortedBy { it.timestampMs }) {
                    repo.saveEventToFirestore(
                        uid,
                        e.packageName,
                        e.eventType,
                        e.appName.ifBlank { e.packageName },
                        e.timestampMs,
                    )
                }
                AppLimitLogLocalPreferences.clear(app)
            }.onFailure { e ->
                Log.e(TAG, "로컬 appLimitLogs 동기화 실패", e)
            }
        }

        /**
         * 일일 사용량 한도 소진 시 timeout 이벤트 기록.
         * 같은 날 동일 앱 1회만 기록.
         */
        @JvmStatic
        fun saveTimeoutEventIfNeeded(context: Context, packageName: String, appName: String) {
            val prefs = context.getSharedPreferences(PREFS_TIMEOUT, Context.MODE_PRIVATE)
            val today = SimpleDateFormat("yyyyMMdd", Locale.KOREAN).format(Date())
            val existing = if (prefs.getString(KEY_TIMEOUT_DATE, "") == today) {
                prefs.getStringSet(KEY_TIMEOUT_PKGS, null)?.orEmpty() ?: emptySet()
            } else emptySet()
            if (existing.contains(packageName)) return
            prefs.edit()
                .putString(KEY_TIMEOUT_DATE, today)
                .putStringSet(KEY_TIMEOUT_PKGS, existing + packageName)
                .commit()
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val app = context.applicationContext
            (app as? AptoxApplication)?.applicationScope?.launch {
                AppLimitLogRepository().saveEvent(
                    app,
                    uid,
                    packageName,
                    "timeout",
                    appName,
                )
            }
        }

        @JvmStatic
        fun clearTimeoutPrefs(context: Context) {
            context.getSharedPreferences(PREFS_TIMEOUT, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
