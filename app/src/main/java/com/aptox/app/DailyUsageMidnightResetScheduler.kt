package com.aptox.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * 매일 00:00에 일일 사용시간(ManualTimerRepository) 초기화 알람 스케줄링.
 * 자정을 넘긴 활성 세션을 정리하여 새 날 0분부터 카운트되도록 함.
 */
object DailyUsageMidnightResetScheduler {

    private const val TAG = "DailyUsageMidnight"
    private const val REQUEST_CODE_MIDNIGHT = 2400

    fun scheduleNextMidnight(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMs = nextMidnightMillis()

        val intent = Intent(context, DailyUsageMidnightResetReceiver::class.java).apply {
            action = DailyUsageMidnightResetReceiver.ACTION_MIDNIGHT_RESET
        }
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT,
            intent,
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
            Log.d(TAG, "00:00 일일 사용시간 초기화 알람 예약")
        } catch (e: SecurityException) {
            Log.w(TAG, "정확 알람 권한 없음", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        }
    }

    /** 다음 00:00 시각(ms). 이미 지났으면 내일 00:00 */
    private fun nextMidnightMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var target = cal.timeInMillis
        if (target <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            target = cal.timeInMillis
        }
        return target
    }
}
