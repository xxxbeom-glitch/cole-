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
 * 목표 달성(뱃지 획득) 알림.
 * 탭 시 챌린지 화면(navIndex=1)으로 이동.
 */
object GoalAchievementNotificationHelper {

    private const val CHANNEL_ID = "goal_achievement"
    private const val CHANNEL_NAME = "목표 달성"

    fun send(context: Context, badgeTitle: String) {
        if (!NotificationPreferences.isGoalAchievedEnabled(context)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAV_INDEX, 1) // 챌린지 탭
        }
        val pi = PendingIntent.getActivity(
            context, badgeTitle.hashCode() and 0x7FFF, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🏅 새로운 뱃지를 획득했어요!")
            .setContentText("$badgeTitle 뱃지를 획득했어요. 확인해보세요!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(badgeTitle.hashCode() and 0x7FFF, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "챌린지 목표 달성 알림"
                },
            )
        }
    }
}
