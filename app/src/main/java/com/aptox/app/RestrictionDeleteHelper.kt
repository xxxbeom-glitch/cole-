package com.aptox.app

import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * 제한 앱 삭제 시 필요한 처리 로직을 한 곳에서 수행.
 * - AppRestrictionRepository: 제한 설정 삭제
 * - ManualTimerRepository: 사용량 데이터는 통계용으로 유지 (삭제하지 않음)
 * - PauseRepository: 일시정지 데이터 삭제
 * - BlockDialogActivity: 해당 앱 차단 다이얼로그 즉시 닫기
 * - AppMonitorService: restriction map 갱신 후 감시 재시작
 * - PauseTimerNotificationService: 해당 앱 일시정지 알림 즉시 중지
 * - Firestore users/{userId}/appLimitLogs: 사용자 제한 해제 이벤트 기록
 */
object RestrictionDeleteHelper {

    /**
     * 제한 앱 삭제. 경고 없이 바로 처리.
     * @param logRelease true면 사용자 제한 해제 이벤트 Firestore 기록 (바텀시트 등 사용자 탭 시)
     */
    fun deleteRestrictedApp(context: Context, packageName: String, logRelease: Boolean = true) {
        val restrictionRepo = AppRestrictionRepository(context)
        val appName = restrictionRepo.getAll().find { it.packageName == packageName }?.appName

        if (logRelease) {
            (context.applicationContext as? AptoxApplication)?.applicationScope?.launch {
                AppLimitLogRepository().saveEvent(
                    context.applicationContext,
                    FirebaseAuth.getInstance().currentUser?.uid,
                    packageName,
                    "release",
                    appName,
                )
            }
        }

        // 1. PauseRepository: 해당 앱 일시정지 데이터 삭제
        PauseRepository(context).clearForPackage(packageName)

        // 2. ManualTimerRepository: 해당 앱 카운트 세션 종료 (타이머 노티 제거)
        ManualTimerRepository(context).endSession(packageName)

        // 3. AppRestrictionRepository: 제한 설정 삭제
        restrictionRepo.delete(packageName)

        // 4. BlockDialogActivity: 해당 앱 차단 다이얼로그 즉시 닫기
        BlockDialogActivity.dismissIfPackage(context, packageName)

        // 5. PauseTimerNotificationService: 해당 앱 일시정지 알림 즉시 제거
        val cancelIntent = Intent(context, PauseTimerNotificationService::class.java).apply {
            action = PauseTimerNotificationService.ACTION_CANCEL_IF_PACKAGE
            putExtra(PauseTimerNotificationService.EXTRA_PACKAGE_NAME, packageName)
        }
        // cancel 인텐트는 이미 실행 중인 서비스에 전달. startService 사용
        // (startForegroundService 시 5초 내 startForeground 필수라 여기선 부적합)
        context.startService(cancelIntent)

        // 6. AppMonitorService: restriction map 갱신 후 감시 재시작
        //    releasedPackage 전달 시 해당 앱 타이머 세션 즉시 종료 후 노티 갱신 (apply race 방지)
        AppMonitorService.start(context, restrictionRepo.toRestrictionMap(), true, packageName)

        // 7. 일일 사용량 알람 스케줄 갱신
        DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(context)

        // 8. 시간 지정 제한 시작/종료 알람 취소
        TimeSpecifiedRestrictionAlarmScheduler.cancelForPackage(context, packageName)
    }
}
