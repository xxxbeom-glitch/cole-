package com.aptox.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * 카운트 미중지 알림: 카운트 진행 앱이 백그라운드 된 후 1분 뒤 알림.
 * 토글 OFF 시 예약 시도 안 함. 예약 후 토글 OFF 되어도 리시버에서 체크.
 */
object CountReminderAlarmScheduler {

    private const val TAG = "CountReminderAlarm"
    private const val REQUEST_CODE = 3002
    const val EXTRA_PACKAGE_NAME = "package_name"

    fun schedule(context: Context, packageName: String) {
        if (!NotificationPreferences.isCountReminderEnabled(context)) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMs = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1)
        val intent = Intent(context, CountReminderAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PACKAGE_NAME, packageName)
        }
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
            Log.d(TAG, "카운트 미중지 알림 예약: $packageName (1분 후)")
        } catch (e: SecurityException) {
            Log.w(TAG, "카운트 미중지 알람 예약 실패", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CountReminderAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
        Log.d(TAG, "카운트 미중지 알림 스케줄 취소")
    }
}
