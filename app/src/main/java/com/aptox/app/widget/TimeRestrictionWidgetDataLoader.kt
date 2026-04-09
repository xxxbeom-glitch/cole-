package com.aptox.app.widget

import android.content.Context
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.PauseRepository
import java.util.Calendar

/** 위젯 2 (지정 시간 제한) 표시 데이터 */
data class TimeRestrictionWidgetData(
    /** 메인 텍스트 — 상황에 따라 달라짐 */
    val mainText: String,
    /** 등록된 스케줄 수 */
    val scheduleCount: Int,
    /** 하루 24시간 중 제한 구간 비율 0~100 (단일 제한 기준, 없으면 0) */
    val timelineProgressPercent: Int,
)

private const val ONE_DAY_MS = 24L * 60 * 60 * 1000

/** 만료된 시간 지정 창을 다음 반복 시각으로 롤링 */
private fun rollWindow(startMs: Long, endMs: Long, now: Long): Pair<Long, Long> {
    var s = startMs
    var e = endMs
    while (e <= now) {
        s += ONE_DAY_MS
        e += ONE_DAY_MS
    }
    return s to e
}

private fun formatDuration(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}

object TimeRestrictionWidgetDataLoader {

    fun empty() = TimeRestrictionWidgetData(
        mainText = "등록된 시간 제한 없음",
        scheduleCount = 0,
        timelineProgressPercent = 0,
    )

    fun load(context: Context): TimeRestrictionWidgetData {
        val app = context.applicationContext
        val repo = AppRestrictionRepository(app)
        val pauseRepo = PauseRepository(app)
        val now = System.currentTimeMillis()

        val todayStartMs = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEndMs = todayStartMs + ONE_DAY_MS

        val timeRestrictions = repo.getAll().filter { it.startTimeMs > 0 }
        if (timeRestrictions.isEmpty()) return empty()

        // 각 항목을 오늘 기준으로 롤링
        data class WindowEntry(
            val packageName: String,
            val winStart: Long,
            val winEnd: Long,
            val isPaused: Boolean,
        )

        val windows = timeRestrictions.mapNotNull { r ->
            val (ws, we) = rollWindow(r.startTimeMs, r.blockUntilMs, now)
            WindowEntry(r.packageName, ws, we, pauseRepo.isPaused(r.packageName))
        }

        // 현재 제한 중인 항목 파악
        val activeWindows = windows.filter { !it.isPaused && now >= it.winStart && now < it.winEnd }

        // 타임라인 퍼센트: 오늘 하루 중 모든 제한 구간의 합 비율
        var totalRestrictionMs = 0L
        for (w in windows) {
            val overlapStart = maxOf(w.winStart, todayStartMs)
            val overlapEnd = minOf(w.winEnd, todayEndMs)
            if (overlapEnd > overlapStart) totalRestrictionMs += (overlapEnd - overlapStart)
        }
        val timelinePercent = ((totalRestrictionMs.toFloat() / ONE_DAY_MS) * 100)
            .toInt().coerceIn(0, 100)

        val mainText: String
        if (activeWindows.isNotEmpty()) {
            // 제한 중 — 가장 빨리 끝나는 시각 기준
            val soonestEnd = activeWindows.minOf { it.winEnd }
            val remaining = soonestEnd - now
            mainText = "제한 중 · ${formatDuration(remaining)} 후 해제"
        } else {
            // 다음 제한 시작까지
            val upcoming = windows
                .filter { it.winStart > now }
                .minByOrNull { it.winStart }
            mainText = if (upcoming != null) {
                val until = upcoming.winStart - now
                "${formatDuration(until)} 후 제한 시작"
            } else {
                "오늘 제한 일정 없음"
            }
        }

        return TimeRestrictionWidgetData(
            mainText = mainText,
            scheduleCount = windows.size,
            timelineProgressPercent = timelinePercent,
        )
    }
}
