package com.aptox.app.usage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

/**
 * 앱별 일별 사용량 저장. 기기 UsageStats ~7일 삭제 전 Worker가 저장.
 * Room 대신 SQLite 직접 사용 (KSP 호환 이슈 회피).
 */
class UsageStatsDatabase(context: Context) : SQLiteOpenHelper(
    context,
    "aptox_usage.db",
    null,
    1,
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE daily_usage (
                date TEXT NOT NULL,
                packageName TEXT NOT NULL,
                usageMs INTEGER NOT NULL,
                sessionCount INTEGER NOT NULL,
                PRIMARY KEY (date, packageName)
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

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
                db.insertWithOnConflict("daily_usage", null, cv, SQLiteDatabase.CONFLICT_REPLACE) // 같은 날짜·앱 중복 upsert 방지
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
                    )
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

    /** 앱 첫 사용일(가장 오래된 레코드의 date). YYYYMMDD 형식, 데이터 없으면 null */
    fun getEarliestDate(): String? {
        return try {
            readableDatabase.rawQuery("SELECT MIN(date) FROM daily_usage", null).use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
