package com.aptox.app

import android.content.Context
import com.aptox.app.usage.UsageStatsLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * 통계 화면 상단 Daily Brief — 어제 하루 기준 템플릿 문구 (Claude 미사용).
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
        "어제 사용 기록을 확인하지 못했습니다" to "사용 통계 접근 권한이 없어 Brief를 생성하지 못했습니다."

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
            return "어제는 ${topApp.name}에서 가장 많은 시간을 보냈습니다"
        }
        val diff = abs(yesterdayTotalMin - avg7Min)
        return if (yesterdayTotalMin > avg7Min) {
            "어제 사용시간이 평소보다 ${diff}분 더 많았습니다"
        } else {
            "어제 사용시간이 평소보다 ${diff}분 적었습니다"
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
        val comparePhrase = if (diffVsAvg > 0) {
            "${diffVsAvg}분 많았습니다"
        } else {
            "${abs(diffVsAvg)}분 적었습니다"
        }
        val s1 =
            "어제 총 사용시간은 ${formatDurationKo(yesterdayTotalMin)}으로, 최근 7일 평균 대비 $comparePhrase."

        val s2 = if (topSegment != null && repApp != null) {
            val pctStr = "%.1f".format(topSegment.second).trimEnd('0').trimEnd('.')
            "${topSegment.first}가 전체의 ${pctStr}%를 차지했으며, ${repApp.name}이 주를 이뤘습니다."
        } else {
            "허용 카테고리에 해당하는 앱에서 측정된 사용 비율이 없었습니다."
        }

        val s3 = if (timeSlotMinutes.all { it == 0L }) {
            "시간대별 사용량에서 두드러진 구간이 없었습니다."
        } else {
            "${peakLabel} 사용이 ${peakMin}분으로 하루 중 가장 집중된 구간이었습니다."
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
