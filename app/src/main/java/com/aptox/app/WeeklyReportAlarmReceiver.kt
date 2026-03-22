package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 매주 월요일 09:00에 발동. WeeklyReportWorker enqueue 후 다음 월요일 재예약.
 */
class WeeklyReportAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!NotificationPreferences.isWeeklyReportEnabled(context)) return
        val request = androidx.work.OneTimeWorkRequestBuilder<WeeklyReportWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "weekly_report_notification",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
        WeeklyReportAlarmScheduler.scheduleNext(context)
        Log.d(TAG, "주간 리포트 Worker enqueue 및 다음 스케줄 예약")
    }

    companion object {
        private const val TAG = "WeeklyReportAlarm"
    }
}
