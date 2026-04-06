package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 카운트 미중지 알림.
 * 본문 탭·「카운트 중지」 액션 → 앱 열고 해당 앱 바텀시트 (EXTRA_OPEN_BOTTOM_SHEET).
 * ongoing + 비자동취소로 스와이프로 임의 제거 어렵게 함.
 */
object CountReminderNotificationHelper {

    private const val TAG = "CountReminderNotification"
    private const val CHANNEL_ID = "count_reminder"
    private const val CHANNEL_NAME = "카운트 중지"

    private fun notificationId(packageName: String): Int =
        (packageName.hashCode() and 0x7FFF) + 5000

    fun cancel(context: Context, packageName: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(packageName))
    }

    fun send(context: Context, packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS 미허용 — 카운트 미중지 알림을 표시할 수 없습니다.")
                return
            }
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "앱 알림이 꺼져 있어 카운트 미중지 알림을 표시할 수 없습니다.")
            return
        }
        val appName = getAppName(context, packageName)
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, packageName)
        }
        val contentPi = PendingIntent.getActivity(
            context, notificationId(packageName), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPi = PendingIntent.getActivity(
            context, notificationId(packageName) + 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val particle = subjectParticleIga(appName)
        val shortText = "${appName}${particle} 아직 진행 중이에요. 사용 중이 아니시면 카운트 중지를 눌러주세요"
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "카운트 중지",
            stopPi,
        ).build()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("카운트가 진행 중이에요 ⏱")
            .setContentText(shortText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(stopAction)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId(packageName), notification)
        }.onFailure { e ->
            Log.e(TAG, "카운트 미중지 알림 notify 실패: $packageName", e)
        }
    }

    /** 한글 음절 마지막 글자 기준 주격 조사 이/가 (그 외는 가) */
    private fun subjectParticleIga(word: String): String {
        if (word.isEmpty()) return "가"
        val cp = word.last().code
        val hangulBase = 0xAC00
        val hangulEnd = 0xD7A3
        if (cp in hangulBase..hangulEnd) {
            val hasBatchim = (cp - hangulBase) % 28 != 0
            return if (hasBatchim) "이" else "가"
        }
        return "가"
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
