package com.aptox.app

import java.util.Calendar

/**
 * 시간 지정 제한 등 UI에서 시각을 한글(오전/오후)로 통일할 때 사용.
 */
object KoreanTimeFormat {

    /**
     * epoch ms → "오전 H시" / "오후 H시". 분이 0이 아니면 "오후 9시 30분" 형태로 분까지 표시.
     */
    fun formatHourClock(timeMs: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val period = if (hour < 12) "오전" else "오후"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return if (minute == 0) {
            "$period ${hour12}시"
        } else {
            "$period ${hour12}시 ${minute}분"
        }
    }
}
