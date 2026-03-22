package com.aptox.app

import android.content.Context
import com.aptox.app.usage.UsageStatsLocalRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Brief AI 요약 주기별 사전 생성
 * - 매일 앱 실행 시 캐시 없으면 생성 (주간/월간/연간)
 */
object BriefSummaryPreloader {

    private val inProgressDeferreds = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    /** 매일 앱 실행 시 주간/월간/연간 캐시 없으면 생성 */
    suspend fun tryPreloadScheduled(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        tryPreloadLastWeek(context)
        tryPreloadLastMonth(context)
        tryPreloadLastYear(context)
    }

    /**
     * 해당 캐시 키 preload 진행 중이면 완료까지 대기 후 캐시 재확인.
     * @return 캐시 존재 여부
     */
    suspend fun waitForPreloadIfNeeded(context: Context, cacheKey: String, timeoutMs: Long = 30_000L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val deferred = inProgressDeferreds[cacheKey]
            if (deferred != null) {
                val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
                withTimeoutOrNull(remaining) { deferred.await() }
                return BriefSummaryCache.has(context, cacheKey)
            }
            if (BriefSummaryCache.has(context, cacheKey)) return true
            delay(200)
        }
        return BriefSummaryCache.has(context, cacheKey)
    }

    suspend fun tryPreloadLastWeek(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        val (startMs, endMs, _) = StatisticsData.getWeekRange(-1)
        val cacheKey = "WEEKLY_${UsageStatsLocalRepository.msToYyyyMmDd(startMs)}"
        if (BriefSummaryCache.has(context, cacheKey)) return

        val deferred = CompletableDeferred<Unit>()
        val existing = inProgressDeferreds.putIfAbsent(cacheKey, deferred)
        if (existing != null) {
            existing.await()
            return
        }
        try {
            withContext(Dispatchers.IO) {
                runCatching { preloadWeekly(context, startMs, endMs, cacheKey) }
            }
        } finally {
            inProgressDeferreds.remove(cacheKey)
            deferred.complete(Unit)
        }
    }

    suspend fun tryPreloadLastMonth(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        val (startMs, endMs, _) = StatisticsData.getSingleMonthRange(-1)
        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
        val cacheKey = "MONTHLY_%04d%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        if (BriefSummaryCache.has(context, cacheKey)) return

        val deferred = CompletableDeferred<Unit>()
        val existing = inProgressDeferreds.putIfAbsent(cacheKey, deferred)
        if (existing != null) {
            existing.await()
            return
        }
        try {
            withContext(Dispatchers.IO) {
                runCatching { preloadMonthly(context, startMs, endMs, cacheKey) }
            }
        } finally {
            inProgressDeferreds.remove(cacheKey)
            deferred.complete(Unit)
        }
    }

    suspend fun tryPreloadLastYear(context: Context) {
        if (!StatisticsData.hasUsageAccess(context)) return
        val (startMs, endMs, _) = StatisticsData.getYearRange(-1)
        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
        val cacheKey = "YEARLY_${cal.get(Calendar.YEAR)}"
        if (BriefSummaryCache.has(context, cacheKey)) return

        val deferred = CompletableDeferred<Unit>()
        val existing = inProgressDeferreds.putIfAbsent(cacheKey, deferred)
        if (existing != null) {
            existing.await()
            return
        }
        try {
            withContext(Dispatchers.IO) {
                runCatching { preloadYearly(context, -1, cacheKey) }
            }
        } finally {
            inProgressDeferreds.remove(cacheKey)
            deferred.complete(Unit)
        }
    }

    /** Worker 등에서 호출: 지난주 Brief 생성 후 타이틀 반환 */
    suspend fun ensureLastWeekAndGetTitle(context: Context): String? {
        if (!StatisticsData.hasUsageAccess(context)) return null
        val (startMs, endMs, _) = StatisticsData.getWeekRange(-1)
        val cacheKey = "WEEKLY_${UsageStatsLocalRepository.msToYyyyMmDd(startMs)}"
        BriefSummaryCache.get(context, cacheKey)?.let { return it.title }

        withContext(Dispatchers.IO) {
            runCatching { preloadWeekly(context, startMs, endMs, cacheKey) }
        }
        return BriefSummaryCache.get(context, cacheKey)?.title
    }

    private suspend fun preloadWeekly(context: Context, startMs: Long, endMs: Long, cacheKey: String) {
        val dayMinutes = StatisticsData.loadDayOfWeekMinutes(context, startMs, endMs)
        val appList = StatisticsData.loadAppUsageForAllowedCategories(context, startMs, endMs)
        val usageByCategory = appList
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        val total = usageByCategory.values.sum()
        val segments = if (total > 0L) {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / total * 100) }
        } else emptyList()
        val timeSlotMinutes = StatisticsData.loadTimeSlot12Minutes(context, startMs, endMs, 0)
        val (title, body) = ClaudeRepository().generateBriefSummaryWithTitle(
            periodLabel = "주간",
            dateMinutes = dayMinutes,
            dateLabels = listOf("월", "화", "수", "목", "금", "토", "일"),
            segments = segments,
            timeSlotMinutes = timeSlotMinutes,
        ).getOrThrow()
        BriefSummaryCache.put(context, cacheKey, BriefSummaryCache.Entry(title, body))
    }

    private suspend fun preloadMonthly(context: Context, startMs: Long, endMs: Long, cacheKey: String) {
        val dayMinutes = StatisticsData.loadDayOfMonthMinutes(context, startMs, endMs)
        val appList = StatisticsData.loadAppUsageForAllowedCategories(context, startMs, endMs)
        val usageByCategory = appList
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        val total = usageByCategory.values.sum()
        val segments = if (total > 0L) {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / total * 100) }
        } else emptyList()
        val timeSlotMinutes = StatisticsData.loadTimeSlot12Minutes(context, startMs, endMs, 30)
        val dateLabels = dayMinutes.indices.map { "${it + 1}일" }
        val (title, body) = ClaudeRepository().generateBriefSummaryWithTitle(
            periodLabel = "월간",
            dateMinutes = dayMinutes,
            dateLabels = dateLabels,
            segments = segments,
            timeSlotMinutes = timeSlotMinutes,
        ).getOrThrow()
        BriefSummaryCache.put(context, cacheKey, BriefSummaryCache.Entry(title, body))
    }

    private suspend fun preloadYearly(context: Context, yearOffset: Int, cacheKey: String) {
        val (ranges, labels) = StatisticsData.getYearRanges(yearOffset)
        val (startMs, endMs, _) = StatisticsData.getYearRange(yearOffset)
        val yearMinutes = StatisticsData.loadYearsMinutes(context, ranges)
        val appList = StatisticsData.loadAppUsageForAllowedCategories(context, startMs, endMs)
        val usageByCategory = appList
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        val total = usageByCategory.values.sum()
        val segments = if (total > 0L) {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / total * 100) }
        } else emptyList()
        val timeSlotMinutes = StatisticsData.loadTimeSlot12Minutes(context, startMs, endMs, 365)
        val (title, body) = ClaudeRepository().generateBriefSummaryWithTitle(
            periodLabel = "연간",
            dateMinutes = yearMinutes,
            dateLabels = labels,
            segments = segments,
            timeSlotMinutes = timeSlotMinutes,
        ).getOrThrow()
        BriefSummaryCache.put(context, cacheKey, BriefSummaryCache.Entry(title, body))
    }
}
