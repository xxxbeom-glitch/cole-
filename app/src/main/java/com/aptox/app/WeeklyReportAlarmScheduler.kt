package com.aptox.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * 매주 월요일 오전 9시 주간 리포트 푸시 알림 스케줄링.
 * 토글 OFF 시 스케줄 취소.
 */
object WeeklyReportAlarmScheduler {

    private const val TAG = "WeeklyReportAlarm"
    private const val REQUEST_CODE = 3001

    fun applySchedule(context: Context, enabled: Boolean) {
        if (enabled) {
            scheduleNext(context)
        } else {
            cancel(context)
        }
    }

    fun scheduleNext(context: Context) {
        if (!NotificationPreferences.isWeeklyReportEnabled(context)) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMs = nextMonday9amMillis()
        val intent = Intent(context, WeeklyReportAlarmReceiver::class.java)
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
            Log.d(TAG, "주간 리포트 알림 예약: 다음 월요일 09:00")
        } catch (e: SecurityException) {
            Log.w(TAG, "주간 리포트 알람 예약 실패", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, WeeklyReportAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
        Log.d(TAG, "주간 리포트 알림 스케줄 취소")
    }

    /** 다음 월요일 오전 9시 (이미 지났으면 다음 주 월요일) */
    private fun nextMonday9amMillis(): Long {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = (dow - Calendar.MONDAY + 7) % 7
        val daysUntilMonday = when {
            daysFromMonday == 0 -> {
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                if (hour < 9 || (hour == 9 && min == 0)) 0 else 7
            }
            else -> 7 - daysFromMonday
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntilMonday)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
