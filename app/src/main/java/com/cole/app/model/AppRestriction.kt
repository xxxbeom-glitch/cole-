package com.cole.app.model

data class AppRestriction(
    val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    /** 시간 지정 차단 종료 시각 (ms). 0이면 일일 사용량 제한 방식 */
    val blockUntilMs: Long = 0L,
    /** 일일 사용량: 이 시각 이후 사용량만 카운트. 0이면 당일 전체 사용 */
    val baselineTimeMs: Long = 0L,
    /** 일일 사용량 반복 요일. ""=오늘 하루만, "0,1,2,3,4,5,6"=매일, "0,2,4"=월수금 등 (0=월..6=일) */
    val repeatDays: String = "",
    /** 일일 사용량 적용 기간(주). 0=오늘하루만/무제한, 1=1주, 2=2주, 3=3주, 4=4주 */
    val durationWeeks: Int = 0,
)