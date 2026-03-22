package com.aptox.app

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore users/{userId}/notifications 컬렉션.
 * 알림 내역 저장 및 실시간 스트림.
 */
class NotificationRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

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
        )
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
