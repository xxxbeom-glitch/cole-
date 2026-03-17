package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * 일일사용량 제한 진행 중 푸시 알림 4종:
 * 1. 23:55 리셋 예고 - "5분 후 [앱이름] 일일사용량 시간이 초기화돼요"
 * 2. 5분 전 경고 - "[앱이름] 제한까지 5분 남았어요"
 * 3. 제한 도달 - "[앱이름] 오늘 사용량을 모두 소진했어요"
 * 4. 마감 임박(1분 전) - "사용 시간이 얼마 남지 않았어요 ⚠️" (설정 토글 ON, 같은 날 동일 앱 중복 없음)
 */
object DailyUsageNotificationHelper {

    private const val CHANNEL_DAILY_USAGE_ID = "daily_usage_limit"
    private const val CHANNEL_DAILY_USAGE_NAME = "일일사용량 알림"

    /** 1. 23:55 리셋 예고 알림 (내일도 동일 앱 제한 스케줄 있을 때만 발송) */
    fun sendResetWarningNotification(context: Context, appName: String, packageName: String) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val pi = launchAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_USAGE_ID)
            .setContentTitle("5분 후 $appName 일일사용량 시간이 초기화돼요")
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(notificationId(packageName, NOTIF_TYPE_RESET_WARNING), notification)
    }

    /** 오늘 5분 전 경고를 이미 보냈는지 */
    fun hasFiredFiveMinWarningToday(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FIVE_MIN_DATE, "") == todayKey()
            && prefs.getStringSet(KEY_FIVE_MIN_PKGS, emptySet())?.contains(packageName) == true
    }

    /** 5분 전 경고 발송 완료 표시 */
    fun markFiveMinWarningFired(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val existing = if (prefs.getString(KEY_FIVE_MIN_DATE, "") == today) {
            prefs.getStringSet(KEY_FIVE_MIN_PKGS, null)?.orEmpty() ?: emptySet()
        } else emptySet()
        prefs.edit()
            .putString(KEY_FIVE_MIN_DATE, today)
            .putStringSet(KEY_FIVE_MIN_PKGS, existing + packageName)
            .apply()
    }

    /** 오늘 제한 도달 알림을 이미 보냈는지 */
    fun hasFiredLimitReachedToday(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LIMIT_REACHED_DATE, "") == todayKey()
            && prefs.getStringSet(KEY_LIMIT_REACHED_PKGS, emptySet())?.contains(packageName) == true
    }

    /** 제한 도달 알림 발송 완료 표시 */
    fun markLimitReachedFired(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val existing = if (prefs.getString(KEY_LIMIT_REACHED_DATE, "") == today) {
            prefs.getStringSet(KEY_LIMIT_REACHED_PKGS, null)?.orEmpty() ?: emptySet()
        } else emptySet()
        prefs.edit()
            .putString(KEY_LIMIT_REACHED_DATE, today)
            .putStringSet(KEY_LIMIT_REACHED_PKGS, existing + packageName)
            .apply()
    }

    /** 오늘 마감 임박(1분 전) 알림을 이미 보냈는지 */
    fun hasFiredDeadlineImminentToday(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEADLINE_IMMINENT_DATE, "") == todayKey()
            && prefs.getStringSet(KEY_DEADLINE_IMMINENT_PKGS, emptySet())?.contains(packageName) == true
    }

    /** 마감 임박 알림 발송 완료 표시 */
    fun markDeadlineImminentFired(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val existing = if (prefs.getString(KEY_DEADLINE_IMMINENT_DATE, "") == today) {
            prefs.getStringSet(KEY_DEADLINE_IMMINENT_PKGS, null)?.orEmpty() ?: emptySet()
        } else emptySet()
        prefs.edit()
            .putString(KEY_DEADLINE_IMMINENT_DATE, today)
            .putStringSet(KEY_DEADLINE_IMMINENT_PKGS, existing + packageName)
            .apply()
    }

    /** 4. 마감 임박(1분 전) 알림 (해당 앱 사용 중일 때만, 토글 ON, 같은 날 동일 앱 1회) */
    fun sendDeadlineImminentNotification(context: Context, appName: String, packageName: String) {
        if (!NotificationPreferences.isDeadlineImminentEnabled(context)) return
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val pi = launchAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_USAGE_ID)
            .setContentTitle("사용 시간이 얼마 남지 않았어요 ⚠️")
            .setContentText("$appName 사용 가능 시간이 1분 남았어요")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(notificationId(packageName, NOTIF_TYPE_DEADLINE_IMMINENT), notification)
        markDeadlineImminentFired(context, packageName)
    }

    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    /** 2. 5분 전 경고 (해당 앱 사용 중일 때만) */
    fun sendFiveMinWarningNotification(context: Context, appName: String, packageName: String) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val pi = launchAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_USAGE_ID)
            .setContentTitle("$appName 제한까지 5분 남았어요")
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(notificationId(packageName, NOTIF_TYPE_FIVE_MIN), notification)
        markFiveMinWarningFired(context, packageName)
    }

    /** 3. 제한 도달 알림 (해당 앱 사용 중일 때만) */
    fun sendLimitReachedNotification(context: Context, appName: String, packageName: String) {
        ensureChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val pi = launchAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_USAGE_ID)
            .setContentTitle("$appName 오늘 사용량을 모두 소진했어요")
            .setContentText(null)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        nm.notify(notificationId(packageName, NOTIF_TYPE_LIMIT_REACHED), notification)
        markLimitReachedFired(context, packageName)
    }

    private const val PREFS_NAME = "aptox_daily_usage_notif"
    private const val KEY_FIVE_MIN_DATE = "five_min_date"
    private const val KEY_FIVE_MIN_PKGS = "five_min_pkgs"
    private const val KEY_LIMIT_REACHED_DATE = "limit_reached_date"
    private const val KEY_LIMIT_REACHED_PKGS = "limit_reached_pkgs"
    private const val KEY_DEADLINE_IMMINENT_DATE = "deadline_imminent_date"
    private const val KEY_DEADLINE_IMMINENT_PKGS = "deadline_imminent_pkgs"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_DAILY_USAGE_ID, CHANNEL_DAILY_USAGE_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "일일사용량 리셋, 제한 경고 알림" },
            )
        }
    }

    private fun launchAppPendingIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(packageName: String, type: Int): Int =
        (packageName.hashCode() and 0x7FFF) shl 8 or (type and 0xFF)

    private const val NOTIF_TYPE_RESET_WARNING = 1
    private const val NOTIF_TYPE_FIVE_MIN = 2
    private const val NOTIF_TYPE_LIMIT_REACHED = 3
    private const val NOTIF_TYPE_DEADLINE_IMMINENT = 4
}
