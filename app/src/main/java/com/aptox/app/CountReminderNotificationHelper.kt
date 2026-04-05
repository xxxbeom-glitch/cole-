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
 * 카운트 미중지 알림.
 * 탭 시 앱 열고 해당 앱 바텀시트 자동 오픈 (EXTRA_OPEN_BOTTOM_SHEET).
 */
object CountReminderNotificationHelper {

    private const val CHANNEL_ID = "count_reminder"
    private const val CHANNEL_NAME = "카운트 중지"

    fun send(context: Context, packageName: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val appName = getAppName(context, packageName)
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, packageName)
        }
        val pi = PendingIntent.getActivity(
            context, (packageName.hashCode() and 0x7FFF) + 5000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "카운트 중지",
            pi,
        ).build()
        val shortText = "$appName 카운트가 아직 진행 중이에요. 중지해주세요"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("카운트가 진행 중이에요 ⏱")
            .setContentText(shortText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0),
            )
            .build()
        NotificationManagerCompat.from(context).notify((packageName.hashCode() and 0x7FFF) + 5000, notification)
    }

    private fun getAppName(context: Context, packageName: String): String {
        val repo = AppRestrictionRepository(context)
        repo.getAll().find { it.packageName == packageName }?.let { return it.appName }
        return try {
            val ai = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "카운트 중지 잊음 알림"
                },
            )
        }
    }
}
