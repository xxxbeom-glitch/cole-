package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 매일 00:00에 발동. 일일 사용시간(ManualTimerRepository)의 자정 이전 활성 세션 정리.
 * 자정을 넘긴 채로 앱이 켜져 있던 세션을 초기화하여 새 날 사용량이 0부터 시작되도록 함.
 */
class DailyUsageMidnightResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MIDNIGHT_RESET) return
        try {
            val timerRepo = ManualTimerRepository(context)
            timerRepo.resetStaleActiveSessionsAtMidnight()
            // 시간 지정 제한: 만료된 항목을 다음날 같은 시각으로 갱신
            val renewed = AppRestrictionRepository(context).renewExpiredTimeSpecifiedRestrictions()
            if (renewed) {
                val map = AppRestrictionRepository(context).toRestrictionMap()
                AppMonitorService.start(context, map)
                Log.d(TAG, "시간지정 제한 갱신 완료 → AppMonitorService 재시작")
            }
            BadgeAutoGrant.onMidnightReset(context.applicationContext)
            Log.d(TAG, "00:00 일일 사용시간 초기화 완료")
            DailyUsageMidnightResetScheduler.scheduleNextMidnight(context)
            TimeSpecifiedRestrictionAlarmScheduler.scheduleAll(context)

            // 자정에 포그라운드로 사용 중인 제한 앱: 차단 없이 카운트 자동 시작 + 노티 발송
            tryHandleMidnightForegroundApp(context.applicationContext)
        } catch (e: Throwable) {
            Log.e(TAG, "자정 리셋 실패", e)
        }
    }

    private fun foregroundPackageFromRecentUsageEvents(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000L, now) ?: return null
        val selfPkg = context.packageName
        val foregroundMap = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == selfPkg) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> foregroundMap[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> foregroundMap.remove(pkg)
            }
        }
        return foregroundMap.maxByOrNull { it.value }?.key
    }

    private fun tryHandleMidnightForegroundApp(context: Context) {
        val pkg = foregroundPackageFromRecentUsageEvents(context) ?: return
        val restriction = AppRestrictionRepository(context).getAll().find { it.packageName == pkg } ?: return
        // 시간 지정 제한 앱은 제외 (일일 사용량 제한 앱만 처리)
        if (restriction.blockUntilMs > 0L) return
        if (PauseRepository(context).isPaused(pkg)) return

        // 카운트 자동 시작 (알림 권한 여부와 무관하게 항상)
        ManualTimerRepository(context).startSession(pkg)
        Log.d(TAG, "자정 포그라운드 일일제한앱 카운트 자동 시작: $pkg")

        // 노티바 알림
        ensureDailyUsageLimitChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MIDNIGHT_FG_USAGE_RESET,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, pkg)
        }
        val stopPi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MIDNIGHT_FG_USAGE_RESET + 77,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val shortText = "카운트가 자동으로 시작되었어요."
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_USAGE_LIMIT_ID)
            .setContentTitle("${restriction.appName}의 하루 사용량이 초기화되었습니다")
            .setContentText(shortText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "카운트 중지",
                stopPi,
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0),
            )
            .build()
        nm.notify(NOTIFICATION_ID_MIDNIGHT_FG_USAGE_RESET, notification)
        Log.d(TAG, "자정 포그라운드 일일제한앱 노티: $pkg")
    }

    private fun ensureDailyUsageLimitChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DAILY_USAGE_LIMIT_ID,
                CHANNEL_DAILY_USAGE_LIMIT_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "하루 사용량 리셋, 제한 경고 알림" },
        )
    }

    companion object {
        private const val TAG = "DailyUsageMidnight"
        const val ACTION_MIDNIGHT_RESET = "com.aptox.app.MIDNIGHT_DAILY_USAGE_RESET"
        private const val CHANNEL_DAILY_USAGE_LIMIT_ID = "daily_usage_limit"
        private const val CHANNEL_DAILY_USAGE_LIMIT_NAME = "하루 사용량 알림"
        private const val NOTIFICATION_ID_MIDNIGHT_FG_USAGE_RESET = 2405
    }
}
