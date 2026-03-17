package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 카운트 미중지 1분 후 발동. 세션 여전히 활성 시 푸시 알림 발송.
 * 탭 시 앱 열고 해당 앱 바텀시트 자동 오픈.
 */
class CountReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val packageName = intent?.getStringExtra(CountReminderAlarmScheduler.EXTRA_PACKAGE_NAME) ?: return
        if (!NotificationPreferences.isCountReminderEnabled(context)) return
        val timerRepo = ManualTimerRepository(context)
        if (!timerRepo.isSessionActive(packageName)) return
        CountReminderNotificationHelper.send(context, packageName)
        Log.d(TAG, "카운트 미중지 알림 발송: $packageName")
    }

    companion object {
        private const val TAG = "CountReminderAlarm"
    }
}
