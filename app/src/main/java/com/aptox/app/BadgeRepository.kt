package com.aptox.app

import android.content.Context
import android.util.Log
import com.aptox.app.model.BadgeDefinition
import com.aptox.app.model.BadgeMasterData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Firestore users/{userId}/badges/{badgeId} 문서의 획득 정보
 */

/**
 * Firestore badges, users/{userId}/badges 조회
 * context 제공 시 뱃지 지급 성공 후 목표 달성 알림 발송 (토글 ON일 때만)
 */
class BadgeRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val context: Context? = null,
) {

    companion object {
        private const val TAG = "BadgeRepository"
    }

    /**
     * badges/{badgeId} 문서 조회
     */
    suspend fun getBadge(badgeId: String): BadgeDefinition? = runCatching {
        val doc = firestore.collection("badges").document(badgeId).get().await()
        if (!doc.exists()) return@runCatching null
        doc.toBadgeDefinition(badgeId) ?: BadgeMasterData.badges.find { it.id == badgeId }
    }.getOrNull()

    /**
     * users/{userId}/badges/{badgeId} 문서 조회
     * @return UserBadgeInfo(획득여부, achievedAt ms) - 미획득이면 null
     */
    suspend fun getUserBadge(userId: String, badgeId: String): UserBadgeInfo? = runCatching {
        val path = "users/$userId/badges/$badgeId"
        if (badgeId == "badge_001") Log.d(TAG, "Firestore 읽기(get): $path")
        val doc = firestore.collection("users").document(userId).collection("badges").document(badgeId).get().await()
        if (!doc.exists()) return@runCatching null
        val achievedAtMs = doc.getTimestamp("achievedAt")?.toDate()?.time
        UserBadgeInfo(earned = true, achievedAtMs = achievedAtMs)
    }.onFailure { e ->
        if (badgeId == "badge_001") Log.e(TAG, "getUserBadge 실패 badge_001", e)
    }.getOrNull()

    /**
     * users/{userId}/badges/{badgeId}에 뱃지 강제 지급.
     * context가 있고 목표 달성 알림이 ON이면 푸시 발송.
     * isNotified = false 로 저장 → 앱 실행 시 미알림 뱃지 체크에 활용
     */
    suspend fun grantBadge(userId: String, badgeId: String): Result<Unit> = runCatching {
        val badge = BadgeMasterData.badges.find { it.id == badgeId }
            ?: getBadge(badgeId)
            ?: throw IllegalArgumentException("Unknown badgeId: $badgeId")
        val ref = firestore.collection("users").document(userId).collection("badges").document(badgeId)
        if (badgeId == "badge_001") {
            Log.d(TAG, "Firestore 쓰기(set merge) 요청: ${ref.path}")
        }
        ref.set(
            mapOf(
                "badgeId" to badgeId,
                "title" to badge.title,
                "achievedAt" to FieldValue.serverTimestamp(),
                "isNotified" to false,
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
        if (badgeId == "badge_001") {
            Log.d(TAG, "Firestore 쓰기 완료(await 성공): ${ref.path}")
        }
        context?.let { ctx ->
            GoalAchievementNotificationHelper.send(ctx, badge.title)
        }
        runCatching {
            NotificationRepository().saveBadgeNotification(
                userId = userId,
                badgeId = badgeId,
                title = badge.title,
                body = "지금 바로 메달을 확인하세요",
            )
        }
        Unit
    }.onFailure { e ->
        if (badgeId == "badge_001") Log.e(TAG, "grantBadge Firestore 실패 badge_001", e)
    }

    /**
     * users/{userId}/badges/{badgeId}의 isNotified = true 로 업데이트.
     * MedalAchievementBottomSheet 확인 후 호출.
     */
    suspend fun markBadgeNotified(userId: String, badgeId: String) = runCatching {
        firestore.collection("users").document(userId).collection("badges").document(badgeId)
            .update("isNotified", true)
            .await()
    }

    /**
     * users/{userId}/badges에서 획득한 뱃지 전체 조회.
     * @return badgeId to achievedAt(ms) 맵. 획득 일시 기준 정렬용.
     */
    suspend fun getAllEarnedBadges(userId: String): Map<String, Long> = runCatching {
        val snapshot = firestore.collection("users").document(userId).collection("badges").get().await()
        snapshot.documents.associate { doc ->
            val achievedAtMs = doc.getTimestamp("achievedAt")?.toDate()?.time ?: 0L
            doc.id to achievedAtMs
        }
    }.getOrElse { emptyMap() }

    /**
     * users/{userId}/badges 중 isAchieved(문서 존재) && isNotified == false 인 뱃지 목록 조회.
     * 앱 실행 시 미표시 뱃지를 순서대로 보여주기 위해 사용.
     */
    suspend fun getUnnotifiedBadges(userId: String): List<BadgeDefinition> = runCatching {
        val snapshot = firestore.collection("users").document(userId).collection("badges")
            .whereEqualTo("isNotified", false)
            .get()
            .await()
        snapshot.documents.mapNotNull { doc ->
            val badgeId = doc.id
            BadgeMasterData.badges.find { it.id == badgeId }
                ?: doc.toBadgeDefinition(badgeId)
        }.sortedBy { it.order }
    }.getOrElse { emptyList() }

    /**
     * users/{userId}/badges 전체 삭제.
     */
    suspend fun deleteAllUserBadges(userId: String): Result<Unit> = runCatching {
        val snapshot = firestore.collection("users").document(userId).collection("badges").get().await()
        if (snapshot.isEmpty) return@runCatching
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toBadgeDefinition(id: String): BadgeDefinition? {
        val title = getString("title") ?: return null
        val icon = getString("icon") ?: "ico_level1"
        return BadgeDefinition(
            id = id,
            order = (getLong("order") ?: 0).toInt(),
            title = title,
            description = getString("description") ?: "",
            condition = getString("condition") ?: "",
            icon = icon,
            message = getString("message"),
        )
    }
}

data class UserBadgeInfo(val earned: Boolean, val achievedAtMs: Long?)
