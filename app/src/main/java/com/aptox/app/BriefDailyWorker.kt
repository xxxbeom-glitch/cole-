package com.aptox.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 매일 자정 BriefDailyAlarmReceiver에 의해 실행.
 * 어제 00:00 ~ 23:59:59 기준 Daily Brief 템플릿 캐시를 채웁니다.
 */
class BriefDailyWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            DailyBriefCacheWarmup.ensureCached(context)
        }
        Result.success()
    }
}
