package com.aptox.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 매주 월요일 9시 주간 리포트 알림 발송.
 * - Brief AI 요약 생성(캐시 없을 시) → 로컬 푸시 발송 → Firestore 알림함 저장
 */
class WeeklyReportWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (!NotificationPreferences.isWeeklyReportEnabled(context)) return@withContext Result.success()
        val title = "지난주 리포트가 도착했어요"
        val body = BriefSummaryPreloader.ensureLastWeekAndGetTitle(context)
            ?: "지난주 스마트폰 사용 리포트를 확인하세요"
        WeeklyReportNotificationHelper.sendWeeklyReportNotification(context, title, body)
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            runCatching {
                NotificationRepository().saveWeeklyReportNotification(uid, title, body)
            }
        }
        Result.success()
    }
}
