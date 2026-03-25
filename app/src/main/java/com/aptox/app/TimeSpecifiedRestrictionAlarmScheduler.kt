package com.aptox.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
/**
 * 시간 지정 제한: 시작(startTimeMs)·종료(blockUntilMs) 시각 알람 등록.
 * 매일 반복은 알림 발송 후 리시버에서 다음날 같은 시각으로 재등록.
 *
 * **시작 알람과 종료 알람은 동일한 경로**로 [AlarmManager.setAlarmClock]에 등록한다.
 * (별도의 setExact/setWindow 전용 분기 없음 — 지연 완화을 위해 시계 알람 방식 통일)
 */
object TimeSpecifiedRestrictionAlarmScheduler {

    private const val TAG = "TimeSpecAlarm"
    private const val PREFS = "aptox_time_spec_alarms"
    private const val KEY_PKGS = "scheduled_packages"

    private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

    internal fun nextFireTime(baseMs: Long, now: Long): Long {
        var t = baseMs
        while (t <= now) {
            t += ONE_DAY_MS
        }
        return t
    }

    /**
     * 저장/부팅/자정 갱신 후 호출: 모든 시간 지정 제한에 대해 시작·종료 알람을 맞춤.
     */
    fun scheduleAll(context: Context) {
        val app = context.applicationContext
        val repo = AppRestrictionRepository(app)
        val now = System.currentTimeMillis()
        val timeSpecified = repo.getAll().filter { it.startTimeMs > 0L }
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldPkgs = prefs.getStringSet(KEY_PKGS, emptySet()) ?: emptySet()
        val newPkgs = timeSpecified.map { it.packageName }.toSet()
        for (pkg in oldPkgs - newPkgs) {
            cancelForPackage(app, pkg)
        }
        prefs.edit().putStringSet(KEY_PKGS, newPkgs).apply()

        for (r in timeSpecified) {
            val startNext = nextFireTime(r.startTimeMs, now)
            val endNext = nextFireTime(r.blockUntilMs, now)
            scheduleStartAlarm(app, r.packageName, r.appName, triggerMs = startNext)
            scheduleEndAlarm(app, r.packageName, r.appName, triggerMs = endNext)
        }
        Log.d(TAG, "scheduleAll: ${timeSpecified.size}개 앱")
    }

    /**
     * 제한 삭제 시 해당 패키지의 시작/종료 알람 취소.
     */
    fun cancelForPackage(context: Context, packageName: String) {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (isStart in listOf(true, false)) {
            val bare = Intent(app, TimeSpecifiedRestrictionAlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                app,
                requestCode(packageName, isStart),
                bare,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            am.cancel(pi)
            pi.cancel()
        }
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_PKGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.remove(packageName)
        prefs.edit().putStringSet(KEY_PKGS, set).apply()
        Log.d(TAG, "cancelForPackage: $packageName")
    }

    /** 제한 시작 시각 — [scheduleEndAlarm]과 동일하게 [AlarmManager.setAlarmClock] 사용 */
    internal fun scheduleStartAlarm(
        context: Context,
        packageName: String,
        appName: String,
        triggerMs: Long,
    ) {
        scheduleSingleAlarm(context, packageName, appName, isStart = true, triggerMs = triggerMs)
    }

    /** 제한 종료 시각 — [scheduleStartAlarm]과 동일하게 [AlarmManager.setAlarmClock] 사용 */
    internal fun scheduleEndAlarm(
        context: Context,
        packageName: String,
        appName: String,
        triggerMs: Long,
    ) {
        scheduleSingleAlarm(context, packageName, appName, isStart = false, triggerMs = triggerMs)
    }

    private fun scheduleSingleAlarm(
        context: Context,
        packageName: String,
        appName: String,
        isStart: Boolean,
        triggerMs: Long,
    ) {
        val app = context.applicationContext
        val am = app.getSystemService<AlarmManager>() ?: return
        if (triggerMs <= System.currentTimeMillis()) {
            Log.w(TAG, "trigger in past, skip: $packageName isStart=$isStart")
            return
        }
        val intent = alarmIntent(app, packageName, appName, isStart, triggerMs)
        val pi = PendingIntent.getBroadcast(
            app,
            requestCode(packageName, isStart),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            setAlarmClockAlarm(app, am, triggerMs, pi, packageName, isStart)
            Log.d(TAG, "예약(alarmClock): $packageName ${if (isStart) "시작" else "종료"} at $triggerMs")
        } catch (e: Exception) {
            Log.w(TAG, "setAlarmClock 실패 → 폴백", e)
            scheduleLooseFallback(am, triggerMs, pi, packageName, isStart)
        }
    }

    /** setAlarmClock: Doze에서도 예정 시각에 가깝게 발화, 정확 알람 권한 불필요 */
    private fun setAlarmClockAlarm(
        app: Context,
        am: AlarmManager,
        triggerMs: Long,
        operation: PendingIntent,
        packageName: String,
        isStart: Boolean,
    ) {
        val showPi = PendingIntent.getActivity(
            app,
            alarmShowRequestCode(packageName, isStart),
            Intent(app, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerMs, showPi)
        am.setAlarmClock(info, operation)
    }

    private fun alarmShowRequestCode(packageName: String, isStart: Boolean): Int {
        return (packageName.hashCode() xor 0x77777777 xor if (isStart) 11 else 22) and 0x7fffffff
    }

    /**
     * 최후 수단: 긴 setWindow는 알림이 매우 늦게 올 수 있어 창을 60초로 제한.
     */
    private fun scheduleLooseFallback(
        am: AlarmManager,
        triggerMs: Long,
        operation: PendingIntent,
        packageName: String,
        isStart: Boolean,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val windowMs = 60_000L
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerMs, windowMs, operation)
                Log.d(TAG, "예약(setWindow 60s): $packageName ${if (isStart) "시작" else "종료"} at $triggerMs")
                return
            } catch (e: Exception) {
                Log.w(TAG, "setWindow 실패 → setAndAllowWhileIdle", e)
            }
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, operation)
        Log.d(TAG, "예약(setAndAllowWhileIdle): $packageName ${if (isStart) "시작" else "종료"} at $triggerMs")
    }

    private fun alarmIntent(
        context: Context,
        packageName: String,
        appName: String,
        isStart: Boolean,
        scheduledTriggerMs: Long,
    ): Intent {
        return Intent(context, TimeSpecifiedRestrictionAlarmReceiver::class.java).apply {
            putExtra(TimeSpecifiedRestrictionAlarmReceiver.EXTRA_PACKAGE_NAME, packageName)
            putExtra(TimeSpecifiedRestrictionAlarmReceiver.EXTRA_APP_NAME, appName)
            putExtra(TimeSpecifiedRestrictionAlarmReceiver.EXTRA_IS_START, isStart)
            putExtra(TimeSpecifiedRestrictionAlarmReceiver.EXTRA_TRIGGER_MS, scheduledTriggerMs)
        }
    }

    private fun requestCode(packageName: String, isStart: Boolean): Int {
        val salt = if (isStart) 0x5A5A5A5A else 0x3C3C3C3C // 둘 다 Int 범위 (0xA5A5A5A5는 Long으로 승격되어 xor 타입 오류 유발)
        return (packageName.hashCode() xor salt xor if (isStart) 1 else 2) and 0x7fffffff
    }
}
