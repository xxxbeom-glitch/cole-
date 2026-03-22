package com.aptox.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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
     * 이벤트 저장. 비로그인 시 스킵.
     * @param appName 표시용 앱 이름 (리스트 화면에서 사용, 선택적)
     */
    suspend fun saveEvent(userId: String?, packageName: String, eventType: String, appName: String? = null) {
        if (userId.isNullOrBlank()) return
        runCatching {
            val ref = firestore.collection("users").document(userId)
                .collection("appLimitLogs").document(packageName)
                .collection("events").document()
            ref.set(
                mapOf(
                    "eventType" to eventType,
                    "timestamp" to FieldValue.serverTimestamp(),
                ),
            ).await()
            firestore.collection("users").document(userId)
                .collection("appLimitLogs").document(packageName)
                .set(mapOf("appName" to (appName?.takeIf { it.isNotBlank() } ?: packageName)), SetOptions.merge()).await()
        }
    }

    /**
     * appLimitLogs에 기록이 있는 패키지 목록 (문서 ID = packageName).
     * 각 문서의 appName 필드로 표시 이름 조회. 없으면 packageName 사용.
     */
    fun getPackagesWithLogsFlow(userId: String?): Flow<List<AppLimitLogPackage>> = callbackFlow {
        if (userId.isNullOrBlank()) {
            trySend(emptyList())
            awaitClose { }
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
                // 최근 이벤트 있는 순으로 정렬하려면 서브컬렉션 쿼리 필요.
                // 단순히 문서 존재 기준으로 반환 (순서는 Firestore 기본)
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /**
     * 해당 사용자의 앱 사용제한 기록 전체 삭제 (디버그용).
     * users/{userId}/appLimitLogs 및 하위 events 서브컬렉션 모두 제거.
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
     * 해당 앱의 이벤트 목록. 최신순.
     */
    fun getEventsFlow(userId: String?, packageName: String): Flow<List<AppLimitLogEvent>> = callbackFlow {
        if (userId.isNullOrBlank() || packageName.isBlank()) {
            trySend(emptyList())
            awaitClose { }
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
        private const val PREFS_TIMEOUT = "aptox_app_limit_log_timeout"
        private const val KEY_TIMEOUT_DATE = "timeout_date"
        private const val KEY_TIMEOUT_PKGS = "timeout_pkgs"

        /**
         * 일일 사용량 한도 소진 시 timeout 이벤트 Firestore 기록.
         * 같은 날 동일 앱 1회만 기록.
         * Java(AppMonitorService)에서 호출 가능.
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
            (context.applicationContext as? AptoxApplication)?.applicationScope?.launch {
                AppLimitLogRepository().saveEvent(
                    FirebaseAuth.getInstance().currentUser?.uid,
                    packageName,
                    "timeout",
                    appName,
                )
            }
        }

        /** timeout 기록용 SharedPreferences 초기화 (디버그/전체 초기화 시 호출) */
        @JvmStatic
        fun clearTimeoutPrefs(context: Context) {
            context.getSharedPreferences(PREFS_TIMEOUT, Context.MODE_PRIVATE).edit().clear().apply()
        }
    }
}
