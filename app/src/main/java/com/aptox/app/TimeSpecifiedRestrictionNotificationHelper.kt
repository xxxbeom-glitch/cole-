package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 시간 지정 제한: 시작/종료 시점 알림
 */
object TimeSpecifiedRestrictionNotificationHelper {

    private const val CHANNEL_ID = "time_specified_restriction"
    private const val CHANNEL_NAME = "시간 지정 제한"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        nm.createNotificationChannel(ch)
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * @param isStart true: 시작 알림, false: 종료(해제) 알림
     */
    fun show(context: Context, appName: String, packageName: String, isStart: Boolean) {
        if (isStart && !NotificationPreferences.isTimeSpecifiedStartEnabled(context)) return
        if (!isStart && !NotificationPreferences.isTimeSpecifiedEndEnabled(context)) return
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val title = if (isStart) {
            "지금부터 $appName 사용 제한이 시작됩니다"
        } else {
            "$appName 사용 제한이 해제됐습니다"
        }
        // 제목 한 줄만 두어 BigText/2줄 템플릿 확장 화살표 유발 최소화 (액션 없음)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .setContentIntent(launchAppPendingIntent(context))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(notificationId(packageName, isStart), notification)
    }

    private fun notificationId(packageName: String, isStart: Boolean): Int {
        val h = packageName.hashCode() xor if (isStart) 0x1111 else 0x2222
        return (h and 0x7fffffff)
    }
}
