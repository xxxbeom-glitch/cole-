package com.aptox.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aptox.app.usage.DailyUsageFirestoreRepository
import com.aptox.app.usage.UsageStatsSyncWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AptoxApplication : Application() {

    /** 배지·백그라운드 Firestore 작업용 (UI 블로킹 방지) */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // dev flavor(SHOW_DEBUG_MENU)에서만 수집 비활성화 — externalTest는 release/debug 모두 활성화
        val crashlyticsEnabled = !BuildConfig.SHOW_DEBUG_MENU
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(crashlyticsEnabled)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        if (defaultHandler !is AptoxUncaughtExceptionHandler) {
            Thread.setDefaultUncaughtExceptionHandler(
                AptoxUncaughtExceptionHandler(this, defaultHandler),
            )
        }
        scheduleUsageStatsSync()
        // 미로그인 상태에서 제한만 저장한 뒤 로그인하면 badge_001 등 Firestore 배지를 줄 수 있게 함
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                BadgeAutoGrant.syncPendingBadgesToFirestore(this)
                BadgeAutoGrant.onUserSignedInTryBadge001(this)
                // 로컬 DB 비어있으면 Firestore에서 일별 사용량 복원 (재설치 시나리오)
                applicationScope.launch {
                    DailyUsageFirestoreRepository().restoreIfLocalEmpty(this@AptoxApplication)
                }
                // Brief 주기별 AI 요약 사전 생성 (월요일/1일/1월1일)
                applicationScope.launch {
                    BriefSummaryPreloader.tryPreloadScheduled(this@AptoxApplication)
                }
                // 로그인 성공 직후 즉시 1회 Firestore 백업
                if (StatisticsData.hasUsageAccess(this)) {
                    val loginSyncRequest = androidx.work.OneTimeWorkRequestBuilder<UsageStatsSyncWorker>()
                        .setInputData(androidx.work.workDataOf(UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
                        .build()
                    WorkManager.getInstance(this).enqueueUniqueWork(
                        "usage_stats_login_sync",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        loginSyncRequest,
                    )
                }
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    companion object {
        private const val TAG = "AptoxApplication"

        /** 포그라운드 상태에서 호출. 앱 실행 시 Application.onCreate는 백그라운드로 인식될 수 있어 MainActivity.onResume에서 호출
         * @param clearForegroundPkg true면 lastKnownForegroundPkg 초기화 (알림 탭으로 바텀시트 열 때 카운트 정지 시 오버레이 즉시 노출 방지) */
        fun startAppMonitorIfNeeded(context: android.content.Context, clearForegroundPkg: Boolean = false) {
            try {
                (context.applicationContext as? AptoxApplication)?.applicationScope?.launch {
                    BriefSummaryPreloader.tryPreloadScheduled(context.applicationContext)
                }
                ManualTimerRepository(context).ensureMidnightResetIfNeeded()
                DailyUsageMidnightResetScheduler.scheduleNextMidnight(context)
                val repo = AppRestrictionRepository(context)
                val map = repo.toRestrictionMap()
                if (map.isNotEmpty()) {
                    AppMonitorService.start(context, map, clearForegroundPkg)
                }
                DailyUsageAlarmScheduler.scheduleResetWarningIfNeeded(context)
                WeeklyReportAlarmScheduler.applySchedule(context, NotificationPreferences.isWeeklyReportEnabled(context))
            } catch (e: Throwable) {
                Log.e(TAG, "AppMonitor 시작 실패", e)
            }
        }
    }

    private fun scheduleUsageStatsSync() {
        val workManager = WorkManager.getInstance(this)
        // 6시간 주기. 최초 실행은 6시간 후 (즉시 1회 동기화와 중복 방지)
        val periodicRequest = PeriodicWorkRequestBuilder<UsageStatsSyncWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(6, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "usage_stats_daily_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
        // 최초 앱 실행 시 즉시 1회 동기화 + Firestore 백업
        if (StatisticsData.hasUsageAccess(this)) {
            val initialRequest = androidx.work.OneTimeWorkRequestBuilder<UsageStatsSyncWorker>()
                .setInputData(androidx.work.workDataOf(UsageStatsSyncWorker.KEY_INITIAL_SYNC to true))
                .build()
            workManager.enqueueUniqueWork(
                "usage_stats_initial_sync",
                androidx.work.ExistingWorkPolicy.KEEP,
                initialRequest,
            )
        }
    }

}
