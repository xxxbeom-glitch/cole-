package com.aptox.app.usage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 앱별 일별 사용량 + 카테고리 일별 합산 + 앱별 12슬롯(2시간) 일별 합산.
 * Room 대신 SQLite 직접 사용 (KSP 호환 이슈 회피).
 */
class UsageStatsDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "aptox_usage.db",
    null,
    2,
) {

    override fun onCreate(db: SQLiteDatabase) {
        createV1Tables(db)
        createV2Tables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            createV2Tables(db)
        }
    }

    private fun createV1Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_usage (
                date TEXT NOT NULL,
                packageName TEXT NOT NULL,
                usageMs INTEGER NOT NULL,
                sessionCount INTEGER NOT NULL,
                PRIMARY KEY (date, packageName)
            )
            """.trimIndent(),
        )
    }

    private fun createV2Tables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS category_daily (
                date TEXT NOT NULL,
                category TEXT NOT NULL,
                usageMs INTEGER NOT NULL,
                PRIMARY KEY (date, category)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS time_segment_daily (
                date TEXT NOT NULL,
                packageName TEXT NOT NULL,
                s0 INTEGER NOT NULL,
                s1 INTEGER NOT NULL,
                s2 INTEGER NOT NULL,
                s3 INTEGER NOT NULL,
                s4 INTEGER NOT NULL,
                s5 INTEGER NOT NULL,
                s6 INTEGER NOT NULL,
                s7 INTEGER NOT NULL,
                s8 INTEGER NOT NULL,
                s9 INTEGER NOT NULL,
                s10 INTEGER NOT NULL,
                s11 INTEGER NOT NULL,
                PRIMARY KEY (date, packageName)
            )
            """.trimIndent(),
        )
    }

    fun insertAll(entities: List<DailyUsageEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (e in entities) {
                val cv = ContentValues().apply {
                    put("date", e.date)
                    put("packageName", e.packageName)
                    put("usageMs", e.usageMs)
                    put("sessionCount", e.sessionCount)
                }
                db.insertWithOnConflict("daily_usage", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getByDateRange(startDate: String, endDate: String): List<DailyUsageEntity> {
        val db = readableDatabase
        val cursor = db.query(
            "daily_usage",
            arrayOf("date", "packageName", "usageMs", "sessionCount"),
            "date >= ? AND date <= ?",
            arrayOf(startDate, endDate),
            null,
            null,
            "date, packageName",
        )
        val result = mutableListOf<DailyUsageEntity>()
        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    DailyUsageEntity(
                        date = it.getString(0),
                        packageName = it.getString(1),
                        usageMs = it.getLong(2),
                        sessionCount = it.getInt(3),
                    ),
                )
            }
        }
        return result
    }

    fun hasDataForDate(date: String): Boolean {
        readableDatabase.query(
            "daily_usage",
            arrayOf("date"),
            "date = ?",
            arrayOf(date),
            null,
            null,
            null,
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    fun getEarliestDate(): String? {
        return try {
            readableDatabase.rawQuery("SELECT MIN(date) FROM daily_usage", null).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getDistinctDateCount(): Int {
        return try {
            readableDatabase.rawQuery("SELECT COUNT(DISTINCT date) FROM daily_usage", null).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun replaceCategoryStatsForDay(date: String, rows: List<DailyCategoryStatEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("category_daily", "date = ?", arrayOf(date))
            for (r in rows) {
                val cv = ContentValues().apply {
                    put("date", r.date)
                    put("category", r.category)
                    put("usageMs", r.usageMs)
                }
                db.insert("category_daily", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun replaceTimeSegmentsForDay(date: String, rows: List<DailyTimeSegmentEntity>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("time_segment_daily", "date = ?", arrayOf(date))
            for (r in rows) {
                val cv = ContentValues().apply {
                    put("date", r.date)
                    put("packageName", r.packageName)
                    for (i in 0 until SLOT_COUNT) {
                        put("s$i", r.slotMs[i])
                    }
                }
                db.insert("time_segment_daily", null, cv)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** [startDate]~[endDate] inclusive, 카테고리별 usageMs 합계 */
    fun getCategoryTotalsForDateRange(startDate: String, endDate: String): Map<String, Long> {
        val db = readableDatabase
        val q =
            """
            SELECT category, SUM(usageMs) FROM category_daily
            WHERE date >= ? AND date <= ?
            GROUP BY category
            """.trimIndent()
        db.rawQuery(q, arrayOf(startDate, endDate)).use { c ->
            val m = mutableMapOf<String, Long>()
            while (c.moveToNext()) {
                m[c.getString(0)] = c.getLong(1)
            }
            return m
        }
    }

    fun getTimeSegmentForDay(date: String, packageName: String): LongArray? {
        readableDatabase.query(
            "time_segment_daily",
            arrayOf("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"),
            "date = ? AND packageName = ?",
            arrayOf(date, packageName),
            null,
            null,
            null,
        ).use { c ->
            if (!c.moveToFirst()) return null
            return LongArray(SLOT_COUNT) { i -> c.getLong(i) }
        }
    }

    fun insertAllCategoryStats(rows: List<DailyCategoryStatEntity>) {
        if (rows.isEmpty()) return
        val byDate = rows.groupBy { it.date }
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((date, list) in byDate) {
                db.delete("category_daily", "date = ?", arrayOf(date))
                for (r in list) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("category", r.category)
                        put("usageMs", r.usageMs)
                    }
                    db.insert("category_daily", null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertAllTimeSegments(rows: List<DailyTimeSegmentEntity>) {
        if (rows.isEmpty()) return
        val byDate = rows.groupBy { it.date }
        val db = writableDatabase
        db.beginTransaction()
        try {
            for ((date, list) in byDate) {
                db.delete("time_segment_daily", "date = ?", arrayOf(date))
                for (r in list) {
                    val cv = ContentValues().apply {
                        put("date", r.date)
                        put("packageName", r.packageName)
                        for (i in 0 until SLOT_COUNT) {
                            put("s$i", r.slotMs[i])
                        }
                    }
                    db.insert("time_segment_daily", null, cv)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun hasCategoryDataForDateRange(startDate: String, endDate: String): Boolean {
        readableDatabase.rawQuery(
            "SELECT 1 FROM category_daily WHERE date >= ? AND date <= ? LIMIT 1",
            arrayOf(startDate, endDate),
        ).use { return it.moveToFirst() }
    }

    companion object {
        private const val SLOT_COUNT = 12
    }
}
