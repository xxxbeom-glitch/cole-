package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 시간 지정 제한 시작/종료 시각 알람 수신 → 알림 표시 후 다음날 같은 시각으로 재등록.
 */
class TimeSpecifiedRestrictionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        val isStart = intent.getBooleanExtra(EXTRA_IS_START, true)
        val firedTriggerMs = intent.getLongExtra(EXTRA_TRIGGER_MS, 0L)
        val appNameHint = intent.getStringExtra(EXTRA_APP_NAME).orEmpty()

        val app = context.applicationContext
        try {
            val repo = AppRestrictionRepository(app)
            val r = repo.getAll().find { it.packageName == packageName }
            if (r == null || r.startTimeMs <= 0L) {
                Log.d(TAG, "제한 없음 또는 일일제한만 — 알람 무시: $packageName")
                return
            }
            val appName = r.appName.ifBlank { appNameHint }
            TimeSpecifiedRestrictionNotificationHelper.show(app, appName, packageName, isStart)

            val nextTrigger = if (firedTriggerMs > 0L) {
                firedTriggerMs + ONE_DAY_MS
            } else {
                val base = if (isStart) r.startTimeMs else r.blockUntilMs
                TimeSpecifiedRestrictionAlarmScheduler.nextFireTime(base, System.currentTimeMillis())
            }
            if (nextTrigger > System.currentTimeMillis()) {
                if (isStart) {
                    TimeSpecifiedRestrictionAlarmScheduler.scheduleStartAlarm(
                        app,
                        packageName,
                        appName,
                        nextTrigger,
                    )
                } else {
                    TimeSpecifiedRestrictionAlarmScheduler.scheduleEndAlarm(
                        app,
                        packageName,
                        appName,
                        nextTrigger,
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "알람 처리 실패", e)
        }
    }

    companion object {
        private const val TAG = "TimeSpecAlarmRcvr"
        private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_IS_START = "extra_is_start"
        const val EXTRA_TRIGGER_MS = "extra_trigger_ms"
    }
}
