package com.aptox.app

import android.content.Context
import com.aptox.app.usage.UsageStatsLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * 통계 화면 상단 Daily Brief — 어제 하루 기준 템플릿 문구, 친근한 ~했어요 톤 (Claude 미사용).
 */
object DailyBriefGenerator {

    /** 캐시 키: DAILY_yyyyMMdd (어제 날짜) */
    fun cacheKey(yesterdayStartMs: Long): String =
        "DAILY_${UsageStatsLocalRepository.msToYyyyMmDd(yesterdayStartMs)}"

    /** loadTimeSlot12Minutes 슬롯(2시간) → "오전/오후 XX시~XX시" */
    private val peakSlotLabels = listOf(
        "오전 12시~2시",
        "오전 2시~4시",
        "오전 4시~6시",
        "오전 6시~8시",
        "오전 8시~10시",
        "오전 10시~12시",
        "오후 12시~2시",
        "오후 2시~4시",
        "오후 4시~6시",
        "오후 6시~8시",
        "오후 8시~10시",
        "오후 10시~12시",
    )

    suspend fun generate(context: Context): Pair<String, String> = withContext(Dispatchers.IO) {
        if (!StatisticsData.hasUsageAccess(context)) {
            return@withContext emptyBrief()
        }
        val (yStart, yEnd, _) = StatisticsData.getYesterdayRange()
        val yesterdayTotalMin = StatisticsData.loadDayOfWeekMinutes(context, yStart, yEnd).sum()

        val (sevenStart, sevenEnd, _) = StatisticsData.getLastNDaysRange(7, 0)
        val sevenSumMin = sumEachCalendarDayMinutes(context, sevenStart, sevenEnd, dayCount = 7)
        val avg7Min = (sevenSumMin / 7.0).roundToLong().coerceAtLeast(0L)

        val allAppsYesterday = StatisticsData.loadAppUsage(context, yStart, yEnd)
        val topApp = allAppsYesterday.firstOrNull { it.usageMs > 0 }

        val allowedList = StatisticsData.loadAppUsageForAllowedCategories(context, yStart, yEnd)
        val usageByCategory = allowedList
            .filter { it.categoryTag != null }
            .groupBy { it.categoryTag!! }
            .mapValues { (_, apps) -> apps.sumOf { it.usageMs } }
        val categoryTotalMs = usageByCategory.values.sum()
        val segments = if (categoryTotalMs > 0L) {
            usageByCategory
                .toList()
                .sortedByDescending { it.second }
                .map { (cat, ms) -> cat to (ms.toFloat() / categoryTotalMs * 100f) }
        } else {
            emptyList()
        }
        val topSegment = segments.firstOrNull()
        val repApp = topSegment?.let { (cat, _) ->
            allowedList
                .filter { it.categoryTag == cat && it.usageMs > 0 }
                .maxWithOrNull(
                    compareByDescending<StatisticsData.StatsAppItem> { it.usageMs }
                        .thenBy { it.packageName },
                )
        }

        val timeSlotMinutes = StatisticsData.loadTimeSlot12Minutes(context, yStart, yEnd, 0)
        val peakIdx = timeSlotMinutes.indices.maxByOrNull { timeSlotMinutes[it] } ?: 0
        val peakMin = timeSlotMinutes.getOrElse(peakIdx) { 0L }
        val peakLabel = peakSlotLabels.getOrElse(peakIdx) { "" }

        val title = buildTitle(
            topApp = topApp,
            yesterdayTotalMin = yesterdayTotalMin,
            avg7Min = avg7Min,
        )
        val body = buildBody(
            yesterdayTotalMin = yesterdayTotalMin,
            avg7Min = avg7Min,
            topSegment = topSegment,
            repApp = repApp,
            peakLabel = peakLabel,
            peakMin = peakMin,
            timeSlotMinutes = timeSlotMinutes,
        )
        title to body
    }

    private fun emptyBrief(): Pair<String, String> =
        "어제 하루는 아직 살펴보지 못했어요" to "사용 통계 권한이 없어서 어제 이야기를 꺼내지 못했어요. 설정에서 허용해 주시면 바로 채워드릴게요."

    /** [rangeStart]~[rangeEnd] 구간에서 연속 [dayCount]일 각각의 일합계(분) 합산 */
    private fun sumEachCalendarDayMinutes(
        context: Context,
        rangeStart: Long,
        rangeEnd: Long,
        dayCount: Int,
    ): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = rangeStart
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        var sum = 0L
        repeat(dayCount) {
            val dStart = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dEnd = cal.timeInMillis.coerceAtMost(rangeEnd)
            sum += StatisticsData.loadDayOfWeekMinutes(context, dStart, dEnd).sum()
            cal.add(Calendar.MILLISECOND, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        return sum
    }

    private fun buildTitle(
        topApp: StatisticsData.StatsAppItem?,
        yesterdayTotalMin: Long,
        avg7Min: Long,
    ): String {
        if (topApp != null) {
            return "어제는 ${topApp.name}에 꽤 마음이 갔던 하루였어요"
        }
        val diff = abs(yesterdayTotalMin - avg7Min)
        return if (yesterdayTotalMin > avg7Min) {
            "어제는 평소보다 스마트폰을 ${diff}분 더 붙잡고 있었어요"
        } else {
            "어제는 평소보다 스마트폰을 ${diff}분 덜 보셨어요"
        }
    }

    private fun buildBody(
        yesterdayTotalMin: Long,
        avg7Min: Long,
        topSegment: Pair<String, Float>?,
        repApp: StatisticsData.StatsAppItem?,
        peakLabel: String,
        peakMin: Long,
        timeSlotMinutes: List<Long>,
    ): String {
        val diffVsAvg = yesterdayTotalMin - avg7Min
        val comparePhrase = when {
            diffVsAvg > 0 -> "최근 일주일 평균보다 ${diffVsAvg}분 더 썼어요"
            diffVsAvg < 0 -> "최근 일주일 평균보다 ${abs(diffVsAvg)}분 덜 썼어요"
            else -> "최근 일주일 평균이랑 비슷했어요"
        }
        val s1 =
            "스마트폰은 총 ${formatDurationKo(yesterdayTotalMin)} 썼고, $comparePhrase."

        val s2 = if (topSegment != null && repApp != null) {
            val pctStr = "%.1f".format(topSegment.second).trimEnd('0').trimEnd('.')
            "그중에서도 ${topSegment.first}가 하루의 ${pctStr}%를 차지했어요. ${repApp.name}에서 시간이 특히 길었네요."
        } else {
            "카테고리별로 보면 눈에 띄게 몰린 앱은 없었어요."
        }

        val s3 = if (timeSlotMinutes.all { it == 0L }) {
            "시간대로 쪼개 보면 특별히 튀는 구간은 없었어요."
        } else {
            "${peakLabel}에 ${peakMin}분 모여 있었어요. 하루 중 가장 붐볐던 때였어요."
        }

        return listOf(s1, s2, s3).joinToString(separator = "")
    }

    private fun formatDurationKo(totalMinutes: Long): String {
        if (totalMinutes <= 0L) return "0분"
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h <= 0L) {
            "${m}분"
        } else if (m == 0L) {
            "${h}시간"
        } else {
            "${h}시간 ${m}분"
        }
    }
}
