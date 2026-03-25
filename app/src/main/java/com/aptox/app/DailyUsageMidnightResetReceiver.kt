package com.aptox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 매일 00:00에 발동. 일일 사용시간(ManualTimerRepository)의 자정 이전 활성 세션 정리.
 * 자정을 넘긴 채로 앱이 켜져 있던 세션을 초기화하여 새 날 사용량이 0부터 시작되도록 함.
 */
class DailyUsageMidnightResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MIDNIGHT_RESET) return
        try {
            val timerRepo = ManualTimerRepository(context)
            // 자정 리셋 전에 활성 세션 목록 저장 → 리셋 후 다이얼로그 표시
            val activeSessions = timerRepo.getAllActiveSessions()
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

            // 자정 직전까지 카운트 중이던 앱에 대해 다이얼로그 표시
            val repo = AppRestrictionRepository(context)
            for ((pkg, _) in activeSessions) {
                val restriction = repo.getAll().find { it.packageName == pkg } ?: continue
                BlockDialogActivity.start(
                    context,
                    pkg,
                    restriction.appName,
                    0L,
                    BlockDialogActivity.OVERLAY_STATE_MIDNIGHT_RESET,
                )
                Log.d(TAG, "자정 카운트 종료 다이얼로그: $pkg")
                break // 한 번에 하나만 표시 (다중 앱이면 첫 번째만)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "자정 리셋 실패", e)
        }
    }

    companion object {
        private const val TAG = "DailyUsageMidnight"
        const val ACTION_MIDNIGHT_RESET = "com.aptox.app.MIDNIGHT_DAILY_USAGE_RESET"
    }
}
