package com.aptox.app.usage

import android.content.Context

object AppDatabaseProvider {

    @Volatile
    private var instance: UsageStatsDatabase? = null

    fun get(context: Context): UsageStatsDatabase {
        return instance ?: synchronized(this) {
            instance ?: UsageStatsDatabase(context.applicationContext).also { instance = it }
        }
    }

    /** 탈퇴 등 로컬 DB 완전 초기화 전: 연결 닫기 및 싱글톤 해제 */
    fun clearAndClose(context: Context) {
        synchronized(this) {
            instance?.close()
            instance = null
            context.applicationContext.deleteDatabase("aptox_usage.db")
        }
    }
}
