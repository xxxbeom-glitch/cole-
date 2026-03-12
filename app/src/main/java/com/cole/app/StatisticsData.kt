package com.cole.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import com.cole.app.usage.UsageStatsLocalRepository
import kotlin.random.Random
import java.text.DecimalFormat
import java.util.Calendar

/**
 * 통계 화면용 데이터 로딩.
 *
 * ## x축 구성 (3시간 간격)
 * - 24 = 00:00~02:59 (자정)
 * - 3  = 03:00~05:59
 * - 6  = 06:00~08:59
 * - 9  = 09:00~11:59
 * - 12 = 12:00~14:59
 * - 15 = 15:00~17:59
 * - 18 = 18:00~20:59
 * - 21 = 21:00~23:59
 *
 * ## 탭별 데이터 기간
 * - 오늘: 오늘 00:00 ~ 현재 시각
 * - 주간: 이번 주 월요일 00:00 ~ 현재 시각
 * - 월간: 이번 달 1일 00:00 ~ 현재 시각
 * - 연간: 올해 1월 1일 00:00 ~ 현재 시각
 *
 * ## 그래프 규칙
 * - 현재 시각이 속한 구간까지만 바 표시, 미래 구간은 빈 칸
 */
object StatisticsData {

    /** true: 2024-01-01~현재 랜덤 더미 데이터로 주간/월간/연간 전체 채움 (개발용) */
    private const val USE_DUMMY_FULL = true

    private const val DUMMY_START_DATE = "20240101"

    /** 날짜별 더미 분단위 사용량. 2024-01-01~오늘 범위 내에서만 유효 */
    private fun dummyMinutesForDate(dateStr: String): Long {
        if (dateStr < DUMMY_START_DATE) return 0L
        val today = UsageStatsLocalRepository.msToYyyyMmDd(System.currentTimeMillis())
        if (dateStr > today) return 0L
        val seed = dateStr.hashCode().toLong().and(0xFFFF_FFFFL)
        return 80 + (Random(seed).nextLong(0, 350))
    }

    /** 탭 인덱스: 0=오늘, 1=주간, 2=월간, 3=연간 */
    enum class Tab { TODAY, WEEKLY, MONTHLY, YEARLY }

    /** 8개 슬롯 라벨 [3, 6, 9, 12, 15, 18, 21, 24] 순 */
    val SlotLabels = listOf(3, 6, 9, 12, 15, 18, 21, 24)

    /** 시간대별 사용량 4구간: X축 "0~6시", "6~12시", "12~18시", "18~24시" */
    val TimeSlot4SectionLabels = listOf("0~6시", "6~12시", "12~18시", "18~24시")

    /** 지난 N일 범위 (오늘 제외, 완료된 기간만). offset 0=최근 N일, -1=그 이전 N일 */
    fun getLastNDaysRange(dayCount: Int, offset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val off = (-offset).coerceAtLeast(0)
        val startMs = todayStart - (off * dayCount + dayCount) * 24L * 60 * 60 * 1000
        cal.timeInMillis = todayStart - (off * dayCount + 1) * 24L * 60 * 60 * 1000
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val endMs = cal.timeInMillis
        cal.timeInMillis = startMs
        val startCal = cal.clone() as Calendar
        cal.timeInMillis = endMs
        val fmt = java.text.SimpleDateFormat("MM.dd", java.util.Locale.KOREAN)
        val label = when (dayCount) {
            7 -> if (off == 0) "지난 7일" else "${fmt.format(startCal.time)} ~ ${fmt.format(cal.time)}"
            30 -> if (off == 0) "지난 30일" else "${fmt.format(startCal.time)} ~ ${fmt.format(cal.time)}"
            365 -> if (off == 0) "지난 365일" else "${startCal.get(Calendar.YEAR)}.${startCal.get(Calendar.MONTH) + 1} ~ ${cal.get(Calendar.YEAR)}.${cal.get(Calendar.MONTH) + 1}"
            else -> "${fmt.format(startCal.time)} ~ ${fmt.format(cal.time)}"
        }
        return Triple(startMs, endMs, label)
    }

    data class StatsAppItem(
        val packageName: String,
        val name: String,
        val usageMinutes: String,
        val sessionCount: String,
        val isRestricted: Boolean,
        /** 카테고리 태그 (SNS, OTT, 게임, 쇼핑, 웹툰, 주식/코인, 기타) — 매핑 없으면 기타 */
        val categoryTag: String? = null,
        /** 카테고리 비율 계산용 (분) */
        val usageMs: Long = 0L,
        /** 주의 배지 표시 (디자인용) */
        val isWarning: Boolean = false,
    )

    /** 주간 탭용: weekOffset 0=이번 주, -1=저번 주, 1=다음 주 */
    fun getWeekRange(weekOffset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 일=1, 월=2, ..., 토=7
        val daysFromMonday = (dayOfWeek + 5) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday + (weekOffset * 7))
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        val startCal = Calendar.getInstance().apply { timeInMillis = startMs }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMs }
        val fmt = java.text.SimpleDateFormat("MM.dd", java.util.Locale.KOREAN)
        val displayText = "${fmt.format(startCal.time)} ~ ${fmt.format(endCal.time)}"
        return Triple(startMs, endMs, displayText)
    }

    /** 요일별(월~일) 사용량 분. DB 우선, 오늘은 queryEvents 사용 */
    fun loadDayOfWeekMinutes(context: Context, startMs: Long, endMs: Long): List<Long> {
        if (USE_DUMMY_FULL) {
            val dayMinutes = LongArray(7)
            val cal = Calendar.getInstance()
            var t = startMs
            while (t <= endMs) {
                cal.timeInMillis = t
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(cal.timeInMillis)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                val idx = (dow + 5) % 7
                dayMinutes[idx] += dummyMinutesForDate(dateStr)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                t = cal.timeInMillis
            }
            return dayMinutes.toList()
        }
        if (!hasUsageAccess(context)) return List(7) { 0L }
        val repo = UsageStatsLocalRepository(context)
        val todayStr = UsageStatsLocalRepository.msToYyyyMmDd(System.currentTimeMillis())
        val dayMinutes = LongArray(7)
        val cal = Calendar.getInstance()

        var t = startMs
        while (t <= endMs) {
            cal.timeInMillis = t
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(dayStart)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val idx = (dow + 5) % 7
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            val mins = if (dateStr == todayStr) {
                aggregateOneDayFromEvents(context, dayStart, minOf(dayEnd, System.currentTimeMillis())) / 60_000
            } else {
                repo.getDayTotalsForDatesBlocking(listOf(dateStr))[dateStr] ?: 0L
            }
            dayMinutes[idx] += mins
            cal.timeInMillis = t
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            t = cal.timeInMillis
        }
        return dayMinutes.toList()
    }

    /** 일별(1~31일) 사용량 분. 선택한 한 달 범위. DB 우선, 오늘은 queryEvents 사용 */
    fun loadDayOfMonthMinutes(context: Context, startMs: Long, endMs: Long): List<Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (USE_DUMMY_FULL) {
            val dayMinutes = LongArray(daysInMonth)
            var t = startMs
            for (d in 0 until daysInMonth) {
                cal.timeInMillis = t
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(cal.timeInMillis)
                dayMinutes[d] = dummyMinutesForDate(dateStr)
                cal.add(Calendar.DAY_OF_YEAR, 1)
                t = cal.timeInMillis
            }
            return dayMinutes.toList()
        }
        if (!hasUsageAccess(context)) return List(daysInMonth) { 0L }
        val repo = UsageStatsLocalRepository(context)
        val todayStr = UsageStatsLocalRepository.msToYyyyMmDd(System.currentTimeMillis())
        val dayMinutes = LongArray(daysInMonth)
        var t = startMs
        for (d in 0 until daysInMonth) {
            cal.timeInMillis = t
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(dayStart)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis
            val mins = if (dateStr == todayStr) {
                aggregateOneDayFromEvents(context, dayStart, minOf(dayEnd, System.currentTimeMillis())) / 60_000
            } else {
                repo.getDayTotalsForDatesBlocking(listOf(dateStr))[dateStr] ?: 0L
            }
            dayMinutes[d] = mins
            cal.timeInMillis = t
            cal.add(Calendar.DAY_OF_YEAR, 1)
            t = cal.timeInMillis
        }
        return dayMinutes.toList()
    }

    /** 하루 구간의 총 사용량(ms). queryEvents 기반 */
    private fun aggregateOneDayFromEvents(context: Context, startMs: Long, endMs: Long): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0L
        val events = usm.queryEvents(startMs, endMs) ?: return 0L
        var total = 0L
        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    sessionStarts[event.packageName + ":" + event.className] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = event.packageName + ":" + event.className
                    val start = sessionStarts.remove(key) ?: continue
                    total += (event.timeStamp - start).coerceAtLeast(0)
                }
                else -> {}
            }
        }
        return total
    }

    /** 구간 내 앱별 (usageMs, sessionCount). userAppPackages 필터 적용 */
    private fun aggregateAppUsageFromEvents(
        context: Context,
        startMs: Long,
        endMs: Long,
        userAppPackages: Set<String>,
    ): Map<String, Pair<Long, Int>> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val events = usm.queryEvents(startMs, endMs) ?: return emptyMap()
        val usageMs = mutableMapOf<String, Long>()
        val sessionCounts = mutableMapOf<String, Int>()
        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName !in userAppPackages) continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    sessionStarts[event.packageName] = event.timeStamp
                    sessionCounts[event.packageName] = (sessionCounts[event.packageName] ?: 0) + 1
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = sessionStarts.remove(event.packageName) ?: continue
                    val duration = (event.timeStamp - start).coerceAtLeast(0)
                    usageMs[event.packageName] = (usageMs[event.packageName] ?: 0) + duration
                }
                else -> {}
            }
        }
        return usageMs.mapValues { Pair(it.value, sessionCounts[it.key] ?: 0) }
    }

    /** 단일 월 범위: monthOffset 0=이번 달, -1=지난달. (startMs, endMs, "N월") */
    fun getSingleMonthRange(monthOffset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, monthOffset)
        val month = cal.get(Calendar.MONTH) + 1
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        val label = if (monthOffset == 0) "이번 달" else "${month}월"
        return Triple(startMs, endMs, label)
    }

    /** 월간: yearOffset 0=올해, -1=작년. 해당 연도의 (startMs, endMs, "YYYY") */
    fun getMonthRange(yearOffset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, yearOffset)
        val year = cal.get(Calendar.YEAR)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        return Triple(startMs, endMs, "$year")
    }

    /** 연간: yearOffset 0=올해. 선택 연도 포함 최대 6년 (selectedYear-5 ~ selectedYear) */
    fun getYearRange(yearOffset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        val selectedYear = cal.get(Calendar.YEAR) + yearOffset
        val startYear = (selectedYear - 5).coerceAtLeast(2020)
        cal.set(Calendar.YEAR, startYear)
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startMs = cal.timeInMillis
        cal.set(Calendar.YEAR, selectedYear)
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis
        return Triple(startMs, endMs, "$selectedYear")
    }

    /** 연간 차트용: 연도별 (startMs,endMs) 리스트. 최대 6개. firstYear~selectedYear */
    fun getYearRanges(yearOffset: Int, maxYears: Int = 6): Pair<List<Pair<Long, Long>>, List<String>> {
        val cal = Calendar.getInstance()
        val selectedYear = cal.get(Calendar.YEAR) + yearOffset
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val firstYear = (currentYear - maxYears + 1).coerceAtLeast(2020)
        val startYear = (selectedYear - maxYears + 1).coerceAtLeast(firstYear)
        val count = (selectedYear - startYear + 1).coerceIn(1, maxYears)
        val ranges = (0 until count).map { i ->
            val y = startYear + i
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endMs = cal.timeInMillis
            Pair(startMs, endMs)
        }
        val labels = (0 until count).map { "${startYear + it}" }
        return Pair(ranges, labels)
    }

    /** 월별(1~12월) 사용량 분. DB 우선, 현재 연도면 오늘분을 queryEvents로 추가 */
    fun loadMonthMinutes(context: Context, startMs: Long, endMs: Long): List<Long> {
        if (USE_DUMMY_FULL) {
            val cal = Calendar.getInstance().apply { timeInMillis = startMs }
            val year = cal.get(Calendar.YEAR)
            val monthTotals = LongArray(12)
            for (month in 0..11) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                var t = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val monthEndMs = cal.timeInMillis
                while (t < monthEndMs) {
                    val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(t)
                    monthTotals[month] += dummyMinutesForDate(dateStr)
                    cal.timeInMillis = t
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    t = cal.timeInMillis
                }
            }
            return monthTotals.toList()
        }
        if (!hasUsageAccess(context)) return List(12) { 0L }
        val repo = UsageStatsLocalRepository(context)
        val cal = Calendar.getInstance().apply { timeInMillis = startMs }
        val year = cal.get(Calendar.YEAR)
        val monthMinutes = repo.getMonthTotalsForYearBlocking(year).toMutableList()
        val now = Calendar.getInstance()
        if (year == now.get(Calendar.YEAR)) {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayMs = aggregateOneDayFromEvents(context, todayStart, System.currentTimeMillis())
            val currentMonth = now.get(Calendar.MONTH)
            monthMinutes[currentMonth] = monthMinutes[currentMonth] + todayMs / 60_000
        }
        return monthMinutes
    }

    /** 연도별 사용량 분. DB 우선, 범위에 오늘 포함 시 queryEvents로 추가 */
    fun loadYearsMinutes(context: Context, yearRanges: List<Pair<Long, Long>>): List<Long> {
        if (USE_DUMMY_FULL) {
            return yearRanges.map { (startMs, endMs) ->
                val cal = Calendar.getInstance()
                var t = startMs
                var total = 0L
                while (t <= endMs) {
                    cal.timeInMillis = t
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val dateStr = UsageStatsLocalRepository.msToYyyyMmDd(cal.timeInMillis)
                    total += dummyMinutesForDate(dateStr)
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    t = cal.timeInMillis
                }
                total
            }
        }
        if (!hasUsageAccess(context)) return yearRanges.map { 0L }
        val repo = UsageStatsLocalRepository(context)
        val todayStr = UsageStatsLocalRepository.msToYyyyMmDd(System.currentTimeMillis())
        return yearRanges.map { (startMs, endMs) ->
            val startDate = UsageStatsLocalRepository.msToYyyyMmDd(startMs)
            val endDate = UsageStatsLocalRepository.msToYyyyMmDd(endMs)
            var total = repo.getTotalForDateRangeBlocking(startDate, endDate)
            if (endDate >= todayStr && startDate <= todayStr) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val todayMs = aggregateOneDayFromEvents(context, cal.timeInMillis, System.currentTimeMillis())
                total += todayMs / 60_000
            }
            total
        }
    }

    /**
     * 지난 1주일간 제한 앱별 "제한된 분" 합산.
     * @param isTimeSpecified true=시간 지정 제한 탭, false=일일 사용량 제한 탭
     * - 일일 사용량 제한: restricted = max(0, limitMinutes*7 - actualUsage). 제한 덕분에 사용하지 않은 시간.
     * - 시간 지정 제한: block 구간 이력 없음, 0 반환 (추후 block 이벤트 추적 시 구현)
     */
    fun loadRestrictedMinutesPerApp(
        context: Context,
        startMs: Long,
        endMs: Long,
        restrictions: List<com.cole.app.model.AppRestriction>,
        isTimeSpecified: Boolean,
    ): Map<String, Long> {
        val filtered = restrictions.filter { r ->
            if (isTimeSpecified) r.blockUntilMs > 0 else r.blockUntilMs == 0L
        }
        if (filtered.isEmpty()) return emptyMap()

        val appList = loadAppUsage(context, startMs, endMs)
        val usageMsByPkg = appList.associate { it.packageName to it.usageMs }

        return filtered.associate { r ->
            val actualMinutes = (usageMsByPkg[r.packageName] ?: 0L) / 60_000
            val restricted = if (isTimeSpecified) {
                // 시간 지정: block 이력 없음, 추후 구현
                0L
            } else {
                (r.limitMinutes * 7L - actualMinutes).coerceAtLeast(0)
            }
            r.packageName to restricted
        }
    }

    /** 동일 주 전주 대비 요일별 비교: (이번주 7값, 저번주 7값) */
    fun loadWeekComparisonMinutes(context: Context, weekOffset: Int): Pair<List<Long>, List<Long>> {
        val (thisStart, thisEnd, _) = getWeekRange(weekOffset)
        val (prevStart, prevEnd, _) = getWeekRange(weekOffset - 1)
        val thisWeek = loadDayOfWeekMinutes(context, thisStart, thisEnd)
        val prevWeek = loadDayOfWeekMinutes(context, prevStart, prevEnd)
        return Pair(thisWeek, prevWeek)
    }

    /** 선택 기간 vs 현재(진행중): (선택 7값, 현재 7값) */
    fun loadWeekSelectedVsCurrent(context: Context, weekOffset: Int): Pair<List<Long>, List<Long>> {
        val (selStart, selEnd, _) = getWeekRange(weekOffset)
        val (curStart, curEnd, _) = getWeekRange(0)
        return Pair(
            loadDayOfWeekMinutes(context, selStart, selEnd),
            loadDayOfWeekMinutes(context, curStart, curEnd),
        )
    }

    /** 선택 연도 vs 현재 연도: (선택 12값, 현재 12값) */
    fun loadMonthSelectedVsCurrent(context: Context, yearOffset: Int): Pair<List<Long>, List<Long>> {
        val (selStart, selEnd, _) = getMonthRange(yearOffset)
        val (curStart, curEnd, _) = getMonthRange(0)
        return Pair(
            loadMonthMinutes(context, selStart, selEnd),
            loadMonthMinutes(context, curStart, curEnd),
        )
    }

    /** 선택 연도 vs 현재 연도: (선택 N값, 현재 N값) */
    fun loadYearSelectedVsCurrent(context: Context, yearOffset: Int): Pair<List<Long>, List<Long>> {
        val (selRanges, _) = getYearRanges(yearOffset)
        val (curRanges, _) = getYearRanges(0)
        return Pair(
            loadYearsMinutes(context, selRanges),
            loadYearsMinutes(context, curRanges),
        )
    }

    /** 월간: 해당 연도 12개월 vs 전년 12개월 (yearOffset 0=올해) */
    fun loadMonthComparisonMinutes(context: Context, yearOffset: Int): Pair<List<Long>, List<Long>> {
        if (USE_DUMMY_FULL) {
            val (thisStart, thisEnd, _) = getMonthRange(yearOffset)
            val (prevStart, prevEnd, _) = getMonthRange(yearOffset - 1)
            return Pair(
                loadMonthMinutes(context, thisStart, thisEnd),
                loadMonthMinutes(context, prevStart, prevEnd),
            )
        }
        val (thisStart, thisEnd, _) = getMonthRange(yearOffset)
        val (prevStart, prevEnd, _) = getMonthRange(yearOffset - 1)
        val thisYear = loadMonthMinutes(context, thisStart, thisEnd)
        val prevYear = loadMonthMinutes(context, prevStart, prevEnd)
        return Pair(thisYear, prevYear)
    }

    /** 연간: 선택 연도 포함 6년 각 연도 vs 직전 연도 (yearOffset 0=올해) */
    fun loadYearComparisonMinutes(context: Context, yearOffset: Int): Pair<List<Long>, List<Long>> {
        if (USE_DUMMY_FULL) {
            val (thisRanges, _) = getYearRanges(yearOffset)
            val (prevRanges, _) = getYearRanges(yearOffset - 1)
            return Pair(
                loadYearsMinutes(context, thisRanges),
                loadYearsMinutes(context, prevRanges),
            )
        }
        val cal = Calendar.getInstance()
        val selectedYear = cal.get(Calendar.YEAR) + yearOffset
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val firstYear = (currentYear - 5).coerceAtLeast(2020)
        val startYear = (selectedYear - 5).coerceAtLeast(firstYear)
        val count = (selectedYear - startYear + 1).coerceIn(1, 6)
        val thisRanges = (0 until count).map { i ->
            val y = startYear + i
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(startMs, cal.timeInMillis)
        }
        val prevRanges = (0 until count).map { i ->
            val y = startYear + i - 1
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startMs = cal.timeInMillis
            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            Pair(startMs, cal.timeInMillis)
        }
        return Pair(loadYearsMinutes(context, thisRanges), loadYearsMinutes(context, prevRanges))
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** (startMs, endMs) 범위 반환 */
    fun getTimeRange(context: Context, tab: Tab): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endMs = System.currentTimeMillis()
        val startMs = when (tab) {
            Tab.TODAY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.WEEKLY -> {
                cal.timeInMillis = endMs
                // 한국 로케일 무관하게 월요일 기준으로 계산
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 일=1, 월=2, ..., 토=7
                val daysFromMonday = (dayOfWeek + 5) % 7 // 월=0, 화=1, ..., 일=6
                cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.MONTHLY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Tab.YEARLY -> {
                cal.timeInMillis = endMs
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
        }
        return Pair(startMs, endMs)
    }

    /**
     * 현재 시각이 속한 슬롯 인덱스 (0~7).
     * 그래프에서 이 인덱스까지 바 표시, 이후 구간은 빈 칸.
     */
    fun getCurrentSlotIndex(): Int {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return when {
            hour in 3..5 -> 0   // 3
            hour in 6..8 -> 1   // 6
            hour in 9..11 -> 2  // 9
            hour in 12..14 -> 3 // 12
            hour in 15..17 -> 4 // 15
            hour in 18..20 -> 5 // 18
            hour in 21..23 -> 6 // 21
            else -> 7           // 24 (00:00~02:59) → 맨 마지막
        }
    }

    /** 8개 슬롯(24,3,6,9,12,15,18,21)별 사용량(분). SlotLabels 참고 */
    fun loadTimeSlotMinutes(context: Context, tab: Tab): List<Long> {
        val (startMs, endMs) = getTimeRange(context, tab)
        return loadTimeSlotMinutes(context, startMs, endMs)
    }

    /** 지정 기간(startMs~endMs)에 대한 8개 슬롯별 사용량(분) */
    fun loadTimeSlotMinutes(context: Context, startMs: Long, endMs: Long): List<Long> {
        if (!hasUsageAccess(context)) return List(8) { 0L }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return List(8) { 0L }
        val events = usm.queryEvents(startMs, endMs) ?: return List(8) { 0L }
        val event = UsageEvents.Event()
        val slotMinutes = LongArray(8)
        val sessionStarts = mutableMapOf<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    sessionStarts[event.packageName + ":" + event.className] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = event.packageName + ":" + event.className
                    val start = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - start).coerceAtLeast(0)
                    addDurationToSlots(slotMinutes, start, durationMs)
                }
            }
        }
        return slotMinutes.toList()
    }

    /**
     * 시간대별 사용량 4구간 (0~6, 6~12, 12~18, 18~24시).
     * @param divideByDays 주간=0(합산), 월간=30(일평균), 연간=365(일평균)
     */
    fun loadTimeSlot4Minutes(context: Context, startMs: Long, endMs: Long, divideByDays: Int = 0): List<Long> {
        if (!hasUsageAccess(context)) return List(4) { 0L }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return List(4) { 0L }
        val events = usm.queryEvents(startMs, endMs) ?: return List(4) { 0L }
        val slotMinutes = LongArray(4)
        val sessionStarts = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    sessionStarts[event.packageName + ":" + event.className] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = event.packageName + ":" + event.className
                    val start = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - start).coerceAtLeast(0)
                    addDurationToSlots4(slotMinutes, start, durationMs)
                }
            }
        }
        val list = slotMinutes.toList()
        return if (divideByDays > 0) list.map { it / divideByDays } else list
    }

    private fun addDurationToSlots4(slots: LongArray, startMs: Long, durationMs: Long) {
        var remaining = durationMs
        var t = startMs
        val cal = Calendar.getInstance()
        while (remaining > 0) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = when {
                hour in 0..5 -> 0   // 0~6시
                hour in 6..11 -> 1  // 6~12시
                hour in 12..17 -> 2 // 12~18시
                else -> 3           // 18~24시
            }
            val slotEndHour = when (slotIndex) {
                0 -> 6
                1 -> 12
                2 -> 18
                else -> 24
            }
            @Suppress("UNCHECKED_CAST")
            val slotEndCal = cal.clone() as Calendar
            if (slotEndHour == 24) {
                slotEndCal.add(Calendar.DAY_OF_YEAR, 1)
                slotEndCal.set(Calendar.HOUR_OF_DAY, 0)
            } else {
                slotEndCal.set(Calendar.HOUR_OF_DAY, slotEndHour)
            }
            slotEndCal.set(Calendar.MINUTE, 0)
            slotEndCal.set(Calendar.SECOND, 0)
            slotEndCal.set(Calendar.MILLISECOND, 0)
            val slotEndMs = slotEndCal.timeInMillis
            val chunkMs = minOf(remaining, slotEndMs - t).coerceAtLeast(0)
            slots[slotIndex] += chunkMs / 60_000
            remaining -= chunkMs
            t += chunkMs
        }
    }

    /**
     * 12개 2시간 슬롯 (0~2,2~4,4~6, 6~8,8~10,10~12, 12~14,14~16,16~18, 18~20,20~22,22~24).
     * @param divideByDays 주간=0(합산), 월간=30(일평균), 연간=365(일평균)
     */
    fun loadTimeSlot12Minutes(context: Context, startMs: Long, endMs: Long, divideByDays: Int = 0): List<Long> {
        if (!hasUsageAccess(context)) return List(12) { 0L }
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return List(12) { 0L }
        val events = usm.queryEvents(startMs, endMs) ?: return List(12) { 0L }
        val event = UsageEvents.Event()
        val slotMinutes = LongArray(12)
        val sessionStarts = mutableMapOf<String, Long>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    sessionStarts[event.packageName + ":" + event.className] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val key = event.packageName + ":" + event.className
                    val start = sessionStarts.remove(key) ?: continue
                    val durationMs = (event.timeStamp - start).coerceAtLeast(0)
                    addDurationToSlots12(slotMinutes, start, durationMs)
                }
            }
        }
        val list = slotMinutes.toList()
        return if (divideByDays > 0) list.map { it / divideByDays } else list
    }

    /** @deprecated loadTimeSlot12Minutes 사용 */
    fun loadTimeSlotMinutes12(context: Context, startMs: Long, endMs: Long): List<Long> =
        loadTimeSlot12Minutes(context, startMs, endMs, 0)

    private fun addDurationToSlots12(slots: LongArray, startMs: Long, durationMs: Long) {
        var remaining = durationMs
        var t = startMs
        val cal = Calendar.getInstance()
        while (remaining > 0) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = (hour / 2).coerceIn(0, 11)
            val slotEndHour = if (slotIndex == 11) 24 else (slotIndex + 1) * 2
            @Suppress("UNCHECKED_CAST")
            val slotEndCal = cal.clone() as Calendar
            if (slotEndHour == 24) {
                slotEndCal.add(Calendar.DAY_OF_YEAR, 1)
                slotEndCal.set(Calendar.HOUR_OF_DAY, 0)
            } else {
                slotEndCal.set(Calendar.HOUR_OF_DAY, slotEndHour)
            }
            slotEndCal.set(Calendar.MINUTE, 0)
            slotEndCal.set(Calendar.SECOND, 0)
            slotEndCal.set(Calendar.MILLISECOND, 0)
            val slotEndMs = slotEndCal.timeInMillis
            val chunkMs = minOf(remaining, slotEndMs - t).coerceAtLeast(0)
            slots[slotIndex] += chunkMs / 60_000
            remaining -= chunkMs
            t += chunkMs
        }
    }

    /** durationMs를 startMs 시각 기준으로 슬롯에 분배. 24=00~02:59, 3=03~05:59, ..., 21=21~23:59 */
    private fun addDurationToSlots(slots: LongArray, startMs: Long, durationMs: Long) {
        var remaining = durationMs
        var t = startMs
        val cal = Calendar.getInstance()
        while (remaining > 0) {
            cal.timeInMillis = t
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val slotIndex = when {
                hour in 3..5 -> 0   // 3:  03:00~05:59
                hour in 6..8 -> 1   // 6:  06:00~08:59
                hour in 9..11 -> 2  // 9:  09:00~11:59
                hour in 12..14 -> 3 // 12: 12:00~14:59
                hour in 15..17 -> 4 // 15: 15:00~17:59
                hour in 18..20 -> 5 // 18: 18:00~20:59
                hour in 21..23 -> 6 // 21: 21:00~23:59
                else -> 7           // 24: 00:00~02:59
            }
            val slotEndHour = when (slotIndex) {
                0 -> 6   // 3→6
                1 -> 9   // 6→9
                2 -> 12  // 9→12
                3 -> 15  // 12→15
                4 -> 18  // 15→18
                5 -> 21  // 18→21
                6 -> 24  // 21→다음날 00:00
                else -> 3 // 24(00~02)→03:00
            }
            @Suppress("UNCHECKED_CAST")
            val slotEndCal = cal.clone() as Calendar
            if (slotEndHour == 24) {
                slotEndCal.add(Calendar.DAY_OF_YEAR, 1)
                slotEndCal.set(Calendar.HOUR_OF_DAY, 0)
                slotEndCal.set(Calendar.MINUTE, 0)
                slotEndCal.set(Calendar.SECOND, 0)
                slotEndCal.set(Calendar.MILLISECOND, 0)
            } else {
                slotEndCal.set(Calendar.HOUR_OF_DAY, slotEndHour)
                slotEndCal.set(Calendar.MINUTE, 0)
                slotEndCal.set(Calendar.SECOND, 0)
                slotEndCal.set(Calendar.MILLISECOND, 0)
            }
            val slotEndMs = slotEndCal.timeInMillis
            val chunkMs = minOf(remaining, slotEndMs - t).coerceAtLeast(0)
            slots[slotIndex] += chunkMs / 60_000
            remaining -= chunkMs
            t += chunkMs
        }
    }

    fun loadAppUsage(context: Context, tab: Tab): List<StatsAppItem> {
        val (startMs, endMs) = getTimeRange(context, tab)
        return loadAppUsage(context, startMs, endMs)
    }

    fun loadAppUsage(context: Context, startMs: Long, endMs: Long): List<StatsAppItem> {
        if (!hasUsageAccess(context)) return emptyList()
        val repo = UsageStatsLocalRepository(context)
        val pm = context.packageManager
        val restrictedPkgs = AppRestrictionRepository(context).getAll().map { it.packageName }.toSet()
        val userAppPackages = getUserInstalledPackages(pm)
        val startDate = UsageStatsLocalRepository.msToYyyyMmDd(startMs)
        val endDate = UsageStatsLocalRepository.msToYyyyMmDd(endMs)
        val todayStr = UsageStatsLocalRepository.msToYyyyMmDd(System.currentTimeMillis())

        val usageMs = repo.getAppUsageForRangeBlocking(startDate, endDate).mapValues { it.value.first }.toMutableMap()
        val sessionCounts = repo.getAppUsageForRangeBlocking(startDate, endDate).mapValues { it.value.second }.toMutableMap()

        if (endDate >= todayStr && startDate <= todayStr) {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val todayStart = cal.timeInMillis
            aggregateAppUsageFromEvents(context, todayStart, System.currentTimeMillis(), userAppPackages).forEach { (pkg, pair) ->
                usageMs[pkg] = (usageMs[pkg] ?: 0L) + pair.first
                sessionCounts[pkg] = (sessionCounts[pkg] ?: 0) + pair.second
            }
        }

        val categoryByPackage = mapOf(
            "com.instagram.android" to "SNS",
            "com.facebook.katana" to "SNS",
            "com.kakao.talk" to "SNS",
            "com.netflix.mediaclient" to "OTT",
            "com.google.android.youtube" to "OTT",
            "com.wavve.android" to "OTT",
            "com.tving.v2" to "OTT",
            "com.ncsoft.lineagew" to "게임",
            "com.airbnb.android" to "쇼핑",
            "com.banhala.android" to "쇼핑",
            "com.nhn.android.band" to "쇼핑",
            "com.nhn.android.webtoon" to "웹툰",
            "com.shinhan.sbanking" to "주식,코인",
            "com.samsung.android.spay" to "주식,코인",
        )
        return usageMs
            .filter { it.value > 0 }
            .map { (packageName, totalMs) ->
                val name = try {
                    pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
                } catch (_: PackageManager.NameNotFoundException) { packageName }
                val categoryTag = categoryByPackage[packageName] ?: "기타"
                Pair(
                    StatsAppItem(
                        packageName = packageName,
                        name = name,
                        usageMinutes = "${DecimalFormat("#,###").format(totalMs / 60_000)}분",
                        sessionCount = "${sessionCounts[packageName] ?: 0}회",
                        isRestricted = packageName in restrictedPkgs,
                        categoryTag = categoryTag,
                        usageMs = totalMs,
                    ),
                    totalMs,
                )
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    /**
     * 진행 중인 앱이 없을 때 추천용: 최근 1주일 사용기록 → 카테고리별 합산 → 1순위 카테고리 → 해당 카테고리 최다 사용 앱.
     * @return 추천 앱 (카테고리 매핑된 앱이 없으면 null)
     */
    fun loadRecommendedAppForRestriction(context: Context): StatsAppItem? {
        val (startMs, endMs, _) = getWeekRange(-1)
        val apps = loadAppUsage(context, startMs, endMs).filter { it.categoryTag != null }
        if (apps.isEmpty()) return null
        val byCategory = apps.groupBy { it.categoryTag!! }.mapValues { (_, list) -> list.sumOf { it.usageMs } }
        val topCategory = byCategory.maxByOrNull { it.value }?.key ?: return null
        return apps.filter { it.categoryTag == topCategory }.maxByOrNull { it.usageMs }
    }

    private fun getUserInstalledPackages(pm: PackageManager): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolves = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return resolves.map { it.activityInfo.packageName }.toSet()
    }

}
