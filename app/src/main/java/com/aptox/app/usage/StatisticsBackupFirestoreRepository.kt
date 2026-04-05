package com.aptox.app.usage

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * 통계 확장 백업/복원.
 * - users/{userId}/categoryStats/{yyyy-MM-dd} — items: [{ category, usageMs }]
 * - users/{userId}/timeSegments/{packageName}/days/{yyyy-MM-dd} — slots: 12×Long(ms)
 */
class StatisticsBackupFirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    suspend fun uploadCategoryStatsForDay(userId: String, rows: List<DailyCategoryStatEntity>): Result<Unit> =
        runCatching {
            if (rows.isEmpty()) return@runCatching
            val date = rows.first().date
            val hyphen = UsageStatsDateUtils.yyyyMmDdToHyphen(date)
            val items = rows.map { mapOf("category" to it.category, "usageMs" to it.usageMs) }
            firestore.collection("users").document(userId).collection("categoryStats").document(hyphen)
                .set(mapOf("date" to hyphen, "items" to items))
                .await()
        }.onFailure { e -> Log.e(TAG, "categoryStats 업로드 실패", e) }

    suspend fun uploadTimeSegmentsForDay(userId: String, rows: List<DailyTimeSegmentEntity>): Result<Unit> =
        runCatching {
            for (r in rows) {
                val hyphen = UsageStatsDateUtils.yyyyMmDdToHyphen(r.date)
                firestore.collection("users").document(userId).collection("timeSegments").document(r.packageName)
                    .collection("days").document(hyphen)
                    .set(
                        mapOf(
                            "date" to hyphen,
                            "packageName" to r.packageName,
                            "slots" to r.slotMs.toList(),
                        ),
                    )
                    .await()
            }
        }.onFailure { e -> Log.e(TAG, "timeSegments 업로드 실패", e) }

    suspend fun restoreCategoryStats(userId: String, context: Context): Result<Unit> =
        runCatching {
            val snapshot = firestore.collection("users").document(userId).collection("categoryStats").get().await()
            val allRows = mutableListOf<DailyCategoryStatEntity>()
            for (doc in snapshot.documents) {
                val hyphen = doc.id
                val dateCompact = UsageStatsDateUtils.hyphenYyyyMmDdToCompact(hyphen)
                val items = doc.get("items") as? List<Map<String, Any>> ?: continue
                for (item in items) {
                    val cat = item["category"] as? String ?: continue
                    val ms = (item["usageMs"] as? Number)?.toLong() ?: continue
                    allRows.add(DailyCategoryStatEntity(date = dateCompact, category = cat, usageMs = ms))
                }
            }
            if (allRows.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    AppDatabaseProvider.get(context).insertAllCategoryStats(allRows)
                }
            }
        }.onFailure { e -> Log.e(TAG, "categoryStats 복원 실패", e) }

    suspend fun restoreTimeSegments(userId: String, context: Context): Result<Unit> =
        runCatching {
            val pkgsSnapshot = firestore.collection("users").document(userId).collection("timeSegments").get().await()
            val allRows = mutableListOf<DailyTimeSegmentEntity>()
            for (pkgDoc in pkgsSnapshot.documents) {
                val pkg = pkgDoc.id
                val daysSnap = pkgDoc.reference.collection("days").get().await()
                for (dayDoc in daysSnap.documents) {
                    val hyphen = dayDoc.id
                    val dateCompact = UsageStatsDateUtils.hyphenYyyyMmDdToCompact(hyphen)
                    val list = dayDoc.get("slots") as? List<*> ?: continue
                    if (list.size < DailyTimeSegmentEntity.TIME_SEGMENT_SLOT_COUNT) continue
                    val slots = LongArray(DailyTimeSegmentEntity.TIME_SEGMENT_SLOT_COUNT) { i ->
                        (list[i] as? Number)?.toLong() ?: 0L
                    }
                    allRows.add(DailyTimeSegmentEntity(date = dateCompact, packageName = pkg, slotMs = slots))
                }
            }
            if (allRows.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    AppDatabaseProvider.get(context).insertAllTimeSegments(allRows)
                }
            }
        }.onFailure { e -> Log.e(TAG, "timeSegments 복원 실패", e) }

    companion object {
        private const val TAG = "StatisticsBackupFirestore"
    }
}
