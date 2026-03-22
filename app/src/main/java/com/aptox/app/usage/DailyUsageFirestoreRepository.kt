package com.aptox.app.usage

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 일별 사용량 Firestore 백업/복원.
 * - users/{userId}/dailyUsage/{date} (date: yyyy-MM-dd)
 * - 문서 필드: items = [{ packageName, usageMs, sessionCount }, ...]
 */
class DailyUsageFirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * 로컬 DB에 저장된 일별 사용량을 Firestore에 업로드.
     * 비로그인 시 스킵. 실패해도 예외만 로그, 앱 동작에는 영향 없음.
     */
    suspend fun uploadDailyUsage(userId: String, entities: List<DailyUsageEntity>): Result<Unit> =
        runCatching {
            if (entities.isEmpty()) return@runCatching
            val byDate = entities.groupBy { yyyyMmDdToYyyyMmDd(it.date) }
            for ((dateStr, list) in byDate) {
                val items = list.map { mapOf("packageName" to it.packageName, "usageMs" to it.usageMs, "sessionCount" to it.sessionCount) }
                firestore.collection("users").document(userId).collection("dailyUsage").document(dateStr)
                    .set(mapOf("date" to dateStr, "items" to items)) // 동일 날짜 덮어쓰기로 중복 방지
                    .await()
            }
        }.onFailure { e -> Log.e(TAG, "Firestore 업로드 실패", e) }

    /**
     * Firestore dailyUsage 전체 조회 후 로컬 DB에 복원.
     */
    suspend fun restoreFromFirestore(userId: String, context: Context): Result<Unit> =
        runCatching {
            val snapshot = firestore.collection("users").document(userId).collection("dailyUsage").get().await()
            val entities = mutableListOf<DailyUsageEntity>()
            for (doc in snapshot.documents) {
                val dateStr = doc.id // yyyy-MM-dd
                val items = doc.get("items") as? List<Map<String, Any>> ?: continue
                val dateYyyyMmDd = yyyyMmDdToYyyyMmDdReverse(dateStr)
                for (item in items) {
                    val pkg = item["packageName"] as? String ?: continue
                    val usageMs = (item["usageMs"] as? Number)?.toLong() ?: continue
                    val sessionCount = (item["sessionCount"] as? Number)?.toInt() ?: 0
                    entities.add(DailyUsageEntity(date = dateYyyyMmDd, packageName = pkg, usageMs = usageMs, sessionCount = sessionCount))
                }
            }
            if (entities.isEmpty()) return@runCatching
            withContext(Dispatchers.IO) {
                AppDatabaseProvider.get(context).insertAll(entities)
            }
        }.onFailure { e -> Log.e(TAG, "Firestore 복원 실패", e) }

    /**
     * 로컬 DB가 비어있고 로그인된 상태일 때만 Firestore에서 복원.
     */
    suspend fun restoreIfLocalEmpty(context: Context): Unit = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
        val db = AppDatabaseProvider.get(context)
        if (db.getEarliestDate() != null) return@withContext // 이미 데이터 있음
        restoreFromFirestore(uid, context)
    }

    private fun yyyyMmDdToYyyyMmDd(yyyyMmDd: String): String {
        if (yyyyMmDd.length < 8) return yyyyMmDd
        return "${yyyyMmDd.take(4)}-${yyyyMmDd.substring(4, 6)}-${yyyyMmDd.substring(6, 8)}"
    }

    private fun yyyyMmDdToYyyyMmDdReverse(yyyyMmDd: String): String =
        yyyyMmDd.replace("-", "")

    companion object {
        private const val TAG = "DailyUsageFirestore"
    }
}
