package com.aptox.app

import android.content.Context
import android.content.Intent

/**
 * 제한 앱 삭제 시 필요한 처리 로직을 한 곳에서 수행.
 * - AppRestrictionRepository: 제한 설정 삭제
 * - ManualTimerRepository: 세션/누적 데이터 삭제
 * - PauseRepository: 일시정지 데이터 삭제
 * - AppMonitorService: restriction map 갱신 후 감시 재시작
 * - PauseTimerNotificationService: 해당 앱 일시정지 알림 즉시 중지
 * - Firestore users/{userId}/badges 는 건드리지 않음
 */
object RestrictionDeleteHelper {

    /**
     * 제한 앱 삭제. 경고 없이 바로 처리.
     * 삭제 후 홈 화면 갱신은 호출 측에서 onDeleted 콜백으로 처리.
     */
    fun deleteRestrictedApp(context: Context, packageName: String) {
        // 1. ManualTimerRepository: 해당 앱 세션·누적 데이터 삭제
        ManualTimerRepository(context).deleteAppData(packageName)

        // 2. PauseRepository: 해당 앱 일시정지 데이터 삭제
        PauseRepository(context).clearForPackage(packageName)

        // 3. AppRestrictionRepository: 제한 설정 삭제
        val restrictionRepo = AppRestrictionRepository(context)
        restrictionRepo.delete(packageName)

        // 4. PauseTimerNotificationService: 해당 앱 일시정지 알림 즉시 제거
        val cancelIntent = Intent(context, PauseTimerNotificationService::class.java).apply {
            action = PauseTimerNotificationService.ACTION_CANCEL_IF_PACKAGE
            putExtra(PauseTimerNotificationService.EXTRA_PACKAGE_NAME, packageName)
        }
        // cancel 인텐트는 이미 실행 중인 서비스에 전달. startService 사용
        // (startForegroundService 시 5초 내 startForeground 필수라 여기선 부적합)
        context.startService(cancelIntent)

        // 5. AppMonitorService: restriction map 갱신 후 감시 재시작
        //    (삭제된 앱은 map에서 제외, getActiveSession()이 null이면 알림바 제거)
        AppMonitorService.start(context, restrictionRepo.toRestrictionMap(), true)

        // 6. 일일 사용량 알람 스케줄 갱신
        DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(context)
    }
}
