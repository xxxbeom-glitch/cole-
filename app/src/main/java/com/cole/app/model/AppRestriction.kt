package com.cole.app.model

data class AppRestriction(
    val packageName: String,
    val appName: String,
    val limitMinutes: Int,
    /** 시간 지정 차단 종료 시각 (ms). 0이면 일일 사용량 제한 방식 */
    val blockUntilMs: Long = 0L,
)