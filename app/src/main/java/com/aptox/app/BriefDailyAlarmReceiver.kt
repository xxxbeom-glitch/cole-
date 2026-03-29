package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 매일 자정(00:00) 발동.
 * 어제 기준 Daily Brief 템플릿 캐시 워밍업 후 다음 자정 재예약.
 */
class BriefDailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "Daily Brief 캐시 알람 수신")
        val request = OneTimeWorkRequestBuilder<BriefDailyWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "brief_daily_refresh",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
        BriefDailyAlarmScheduler.schedule(context)
    }

    companion object {
        private const val TAG = "BriefDailyAlarm"
    }
}
