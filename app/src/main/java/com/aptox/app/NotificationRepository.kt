package com.aptox.app

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await

/**
 * Firestore users/{userId}/notifications 컬렉션.
 * 알림 내역 저장 및 실시간 스트림.
 */
class NotificationRepository(
    private val context: Context? = null,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val prefs by lazy {
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 뱃지 획득 알림을 users/{userId}/notifications에 추가.
     * @param body 예: "지금 바로 메달을 확인하세요"
     */
    suspend fun saveBadgeNotification(userId: String, badgeId: String, title: String, body: String) = runCatching {
        firestore.collection("users").document(userId).collection("notifications")
            .add(
                mapOf(
                    "type" to "badge",
                    "title" to title,
                    "body" to body,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "badgeId" to badgeId,
                ),
            )
            .await()
    }

    /**
     * 주간 리포트 알림을 users/{userId}/notifications에 추가.
     * @param body Brief AI 요약 타이틀
     */
    suspend fun saveWeeklyReportNotification(userId: String, title: String, body: String) = runCatching {
        firestore.collection("users").document(userId).collection("notifications")
            .add(
                mapOf(
                    "type" to "weekly_report",
                    "title" to title,
                    "body" to body,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "navTarget" to "statistics_weekly",
                ),
            )
            .await()
    }

    /**
     * 알림 목록 실시간 스트림. 최신순 정렬.
     */
    fun getNotificationsFlow(userId: String): Flow<List<NotificationHistoryItem>> = callbackFlow {
        val listener = firestore.collection("users").document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toNotificationHistoryItem()
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toNotificationHistoryItem(): NotificationHistoryItem? {
        val type = getString("type") ?: return null
        val title = getString("title") ?: return null
        val body = getString("body")
        val timestamp = getTimestamp("timestamp")?.toDate()?.time ?: 0L
        val badgeId = getString("badgeId")
        val navTarget = getString("navTarget")

        val typeLabel = when (type) {
            "badge" -> "챌린지 성공"
            "weekly_report" -> "주간 리포트"
            else -> type
        }
        val timeText = formatTimeAgo(timestamp)
        return NotificationHistoryItem(
            type = type,
            typeLabel = typeLabel,
            timeText = timeText,
            title = title,
            body = body,
            badgeId = badgeId,
            navTarget = navTarget,
            timestampMs = timestamp,
        )
    }

    /**
     * 알림 내역 화면에서 "전부 확인" 시 호출.
     * 마지막 확인 시각을 저장하여 이후 hasUnreadNotificationsFlow에서 미확인 알림 판별.
     */
    fun markAsChecked(userId: String?, maxTimestampMs: Long) {
        if (userId.isNullOrBlank() || prefs == null) return
        val key = "$KEY_LAST_CHECKED_PREFIX$userId"
        prefs!!.edit().putLong(key, maxTimestampMs).commit()
        Companion.markCheckedTrigger.tryEmit(Unit)
    }

    /**
     * 미확인 알림 존재 여부. 알림 내역 화면에서 새 알림까지 전부 확인하면 false.
     *
     * 로그인 직후 등 `last_checked_{userId}`가 없을 때(0) Firestore에 쌓여 있던 기존 알림만으로
     * 배지가 켜지지 않도록, 첫 목록 수신 시 확인 시각을 그 목록의 최신 타임스탬프로 한 번 맞춘다.
     * 이후 도착하는 알림만 `timestampMs > last_checked`로 미읽음 처리된다.
     */
    fun hasUnreadNotificationsFlow(userId: String?): Flow<Boolean> {
        if (userId.isNullOrBlank() || prefs == null) return kotlinx.coroutines.flow.flowOf(false)
        val key = "$KEY_LAST_CHECKED_PREFIX$userId"
        return combine(
            getNotificationsFlow(userId),
            Companion.markCheckedTrigger.onStart { emit(Unit) },
        ) { items, _ ->
            var lastCheckedMs = prefs!!.getLong(key, 0L)
            if (lastCheckedMs == 0L && items.isNotEmpty()) {
                val maxTs = items.maxOf { it.timestampMs }
                prefs!!.edit().putLong(key, maxTs).commit()
                lastCheckedMs = maxTs
            }
            items.any { it.timestampMs > lastCheckedMs }
        }
    }

    companion object {
        private const val PREFS_NAME = "aptox_notification_check"
        private const val KEY_LAST_CHECKED_PREFIX = "last_checked_"
        private val markCheckedTrigger = MutableSharedFlow<Unit>(replay = 0)
    }

    private fun formatTimeAgo(timestampMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMs
        return when {
            diff < 60_000 -> "방금"
            diff < 3600_000 -> "${diff / 60_000}분 전"
            diff < 86400_000 -> "${diff / 3600_000}시간 전"
            diff < 604800_000 -> "${diff / 86400_000}일 전"
            else -> {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMs }
                "${cal.get(java.util.Calendar.MONTH) + 1}월 ${cal.get(java.util.Calendar.DAY_OF_MONTH)}일"
            }
        }
    }
}
