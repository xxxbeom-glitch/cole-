package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 기기 재부팅 후 BOOT_COMPLETED 수신 시 AppMonitorService를 재시작합니다.
 * 제한 앱이 있을 때만 감시 서비스를 시작합니다.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        try {
            val ctx = context.applicationContext
            ManualTimerRepository(ctx).ensureMidnightResetIfNeeded()
            DailyUsageMidnightResetScheduler.scheduleNextMidnight(ctx)
            val repo = AppRestrictionRepository(ctx)
            val map = repo.toRestrictionMap()
            if (map.isNotEmpty()) {
                AppMonitorService.start(ctx, map)
                Log.d(TAG, "부팅 완료: AppMonitorService 재시작 (제한 앱 ${map.size}개)")
            }
            DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(ctx)
            WeeklyReportAlarmScheduler.applySchedule(ctx, NotificationPreferences.isWeeklyReportEnabled(ctx))
        } catch (e: Throwable) {
            Log.e(TAG, "부팅 후 AppMonitor 시작 실패", e)
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
