package com.aptox.app

/**
 * 홈 화면 제한 앱 목록 위험/주의 라벨 기준.
 * 카테고리별 일평균 사용시간(분) 기준.
 * - 일평균 = 지난 7일 총 사용시간 ÷ 7
 * - 주의: 기준 이상 시 "주의" 라벨
 * - 위험: 기준 이상 시 "위험" 라벨 (주의보다 우선)
 * - 기타/미분류: 라벨 없음
 */
object AppRiskLabelThresholds {

    /** 주의 라벨 기준 (분) */
    const val SNS_CAUTION_MIN = 60
    const val SNS_DANGER_MIN = 120

    const val OTT_CAUTION_MIN = 120
    const val OTT_DANGER_MIN = 240

    const val GAME_CAUTION_MIN = 60
    const val GAME_DANGER_MIN = 120

    const val SHOPPING_CAUTION_MIN = 30
    const val SHOPPING_DANGER_MIN = 60

    const val WEBTOON_CAUTION_MIN = 60
    const val WEBTOON_DANGER_MIN = 120

    const val STOCK_COIN_CAUTION_MIN = 60
    const val STOCK_COIN_DANGER_MIN = 120

    /** 카테고리 키 → (주의분, 위험분). 캐시 키: "주식,코인" 등 */
    private val THRESHOLDS = mapOf(
        "SNS" to (SNS_CAUTION_MIN to SNS_DANGER_MIN),
        "OTT" to (OTT_CAUTION_MIN to OTT_DANGER_MIN),
        "게임" to (GAME_CAUTION_MIN to GAME_DANGER_MIN),
        "쇼핑" to (SHOPPING_CAUTION_MIN to SHOPPING_DANGER_MIN),
        "웹툰" to (WEBTOON_CAUTION_MIN to WEBTOON_DANGER_MIN),
        "주식,코인" to (STOCK_COIN_CAUTION_MIN to STOCK_COIN_DANGER_MIN),
    )

    /**
     * @param category 캐시의 카테고리 문자열 (SNS, OTT, 게임, 쇼핑, 웹툰, 주식,코인, 기타 등)
     * @param dailyAvgMinutes 지난 7일 일평균 사용시간(분)
     * @return Pair(showDangerLabel, showWarningLabel)
     */
    fun computeLabels(category: String?, dailyAvgMinutes: Double): Pair<Boolean, Boolean> {
        val (cautionMin, dangerMin) = THRESHOLDS[category] ?: return false to false
        return when {
            dailyAvgMinutes >= dangerMin -> true to false   // 위험
            dailyAvgMinutes >= cautionMin -> false to true // 주의
            else -> false to false                          // 없음
        }
    }
}
