package com.aptox.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 매일 오전 6시 BriefDailyAlarmReceiver에 의해 실행.
 * 어제 00:00 ~ 23:59:59 데이터 기반으로 주간 Brief AI 요약 캐시를 갱신.
 */
class BriefDailyWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            BriefSummaryPreloader.tryPreloadLastWeek(context)
        }
        Result.success()
    }
}
