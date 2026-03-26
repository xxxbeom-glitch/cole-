package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * 매일 오전 6시 발동.
 * 어제 데이터 기반 주간 Brief AI 요약 갱신 후 다음날 06:00 재예약.
 */
class BriefDailyAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "주간 Brief 갱신 알람 수신")
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
