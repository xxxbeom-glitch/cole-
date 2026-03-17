package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 매주 월요일 09:00에 발동. 주간 리포트 푸시 알림 발송 후 다음 월요일 재예약.
 */
class WeeklyReportAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!NotificationPreferences.isWeeklyReportEnabled(context)) return
        WeeklyReportNotificationHelper.sendWeeklyReportNotification(context)
        WeeklyReportAlarmScheduler.scheduleNext(context)
        Log.d(TAG, "주간 리포트 알림 발송 및 다음 스케줄 예약")
    }

    companion object {
        private const val TAG = "WeeklyReportAlarm"
    }
}
