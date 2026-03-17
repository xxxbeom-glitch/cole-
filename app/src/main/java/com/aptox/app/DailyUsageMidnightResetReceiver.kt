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
            ManualTimerRepository(context).resetStaleActiveSessionsAtMidnight()
            Log.d(TAG, "00:00 일일 사용시간 초기화 완료")
            DailyUsageMidnightResetScheduler.scheduleNextMidnight(context)
        } catch (e: Throwable) {
            Log.e(TAG, "자정 리셋 실패", e)
        }
    }

    companion object {
        private const val TAG = "DailyUsageMidnight"
        const val ACTION_MIDNIGHT_RESET = "com.aptox.app.MIDNIGHT_DAILY_USAGE_RESET"
    }
}
