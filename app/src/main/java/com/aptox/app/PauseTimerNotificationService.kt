package com.aptox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat

/**
 * 일시정지 중 노티바에 남은 시간을 실시간으로 표시하는 포그라운드 서비스.
 * - 일시정지 시작 시 BlockOverlayService에서 startForegroundService로 시작
 * - 1초마다 남은 시간 업데이트
 * - 1분 전 별도 알림 발송
 * - 일시정지 종료 시 자동 종료
 */
class PauseTimerNotificationService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var pauseUntilMs = 0L
    private var appName = ""
    private var packageName_ = ""
    private var warningFired = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 제한 앱 삭제 시 해당 앱의 일시정지 알림 즉시 중지
        if (intent?.action == ACTION_CANCEL_IF_PACKAGE) {
            val targetPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
            if (targetPkg == packageName_) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.apply {
                    cancel(NOTIFICATION_ID)
                    cancel(NOTIFICATION_WARNING_ID)
                }
                stopSelf()
            } else {
                stopSelf()
            }
            return START_NOT_STICKY
        }

        pauseUntilMs = intent?.getLongExtra(EXTRA_PAUSE_UNTIL_MS, 0L) ?: 0L
        appName = intent?.getStringExtra(EXTRA_APP_NAME) ?: ""
        packageName_ = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        warningFired = false

        if (pauseUntilMs <= System.currentTimeMillis()) {
            stopSelf()
            return START_NOT_STICKY
        }

        createChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildTimerNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildTimerNotification())
        }
        scheduleUpdate()
        return START_NOT_STICKY
    }

    private fun scheduleUpdate() {
        handler.postDelayed({
            val now = System.currentTimeMillis()
            val remaining = pauseUntilMs - now

            if (remaining <= 0) {
                // 일시정지 종료 → AppMonitorService에 즉시 체크 요청
                triggerAppMonitorCheck()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@postDelayed
            }

            // 1분 전 경고 알림
            if (remaining <= 60_000L && !warningFired) {
                warningFired = true
                sendWarningNotification()
            }

            updateTimerNotification(remaining)
            scheduleUpdate()
        }, 1_000L)
    }

    /**
     * 일시정지 종료 시 AppMonitorService를 재시작해 포그라운드 앱 차단 체크를 유도한다.
     * 딜레이를 줘서 UsageStats에 앱 전환 이벤트가 반영된 뒤 체크하도록 한다.
     * (딜레이 없이 즉시 체크하면 제한 앱이 아직 포그라운드로 기록되어 오버레이가 반복 표시됨)
     */
    private fun triggerAppMonitorCheck() {
        handler.postDelayed({
            val repo = AppRestrictionRepository(this)
            AppMonitorService.start(this, repo.toRestrictionMap(), true)
        }, 3_000L)
    }

    private fun updateTimerNotification(remainingMs: Long) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        nm.notify(NOTIFICATION_ID, buildTimerNotification(remainingMs))
    }

    private fun buildTimerNotification(remainingMs: Long = pauseUntilMs - System.currentTimeMillis()): Notification {
        val totalSec = (remainingMs / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        val timeText = String.format("%d:%02d", min, sec)

        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(this.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return androidx.core.app.NotificationCompat.Builder(this, CHANNEL_TIMER_ID)
            .setContentTitle("$appName 일시정지 중")
            .setContentText("남은 시간: $timeText")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    private fun sendWarningNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(this.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_WARNING_ID)
            .setContentTitle(appName)
            .setContentText("1분 후 다시 사용이 제한됩니다")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIFICATION_WARNING_ID, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_TIMER_ID, "일시정지 타이머", NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                },
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_WARNING_ID, "일시정지 종료 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "일시정지 1분 전 알림"
                },
            )
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val ACTION_CANCEL_IF_PACKAGE = "com.aptox.app.CANCEL_IF_PACKAGE"
        const val EXTRA_PAUSE_UNTIL_MS = "pause_until_ms"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_PACKAGE_NAME = "package_name"

        private const val CHANNEL_TIMER_ID = "pause_timer"
        private const val CHANNEL_WARNING_ID = "pause_warning"
        private const val NOTIFICATION_ID = 1003
        private const val NOTIFICATION_WARNING_ID = 1004

        fun start(context: Context, packageName: String, appName: String, pauseUntilMs: Long) {
            val intent = Intent(context, PauseTimerNotificationService::class.java).apply {
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_PAUSE_UNTIL_MS, pauseUntilMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
