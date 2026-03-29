package com.aptox.app

import android.content.Context

/**
 * Daily Brief 템플릿 캐시 워밍업.
 * 캐시 키 `DAILY_yyyyMMdd`(어제)가 없을 때만 생성합니다.
 */
object DailyBriefCacheWarmup {

    suspend fun ensureCached(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        val (yStart, _, _) = StatisticsData.getYesterdayRange()
        val key = DailyBriefGenerator.cacheKey(yStart)
        if (BriefSummaryCache.has(context, key)) return
        val (title, body) = DailyBriefGenerator.generate(context)
        BriefSummaryCache.put(context, key, BriefSummaryCache.Entry(title, body))
    }
}
