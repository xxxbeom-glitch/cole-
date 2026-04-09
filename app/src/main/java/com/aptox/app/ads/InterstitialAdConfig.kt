package com.aptox.app.ads

/**
 * 전면(Interstitial) 광고 표시 정책 상수.
 * 출시 전 간격 조정 시 [FOREGROUND_CONTINUOUS_DISPLAY_INTERVAL_MS]만 변경하면 됩니다.
 */
object InterstitialAdConfig {

    /**
     * 앱이 포그라운드([ProcessLifecycleOwner]) 상태로 **연속** 이 시간(ms) 이상 유지된 뒤
     * 전면 광고 1회 표시. 백그라운드 전환 시 타이머는 리셋됩니다.
     */
    const val FOREGROUND_CONTINUOUS_DISPLAY_INTERVAL_MS: Long = 30_000L
}
