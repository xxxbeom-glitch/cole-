package com.aptox.app.subscription

import android.content.Context
import com.aptox.app.BuildConfig

// ============================================================
// 구독 상태 관리 - 유료/무료 기능 분기점
// ============================================================

object SubscriptionManager {

    /**
     * Google Play 프리미엄 구독을 사용자에게 공개할 때 `true`로 바꿉니다.
     * `false`인 동안에는 스토어·DataStore와 무관하게 **무료 플랜만** 적용합니다(DEBUG의 [debugForceSubscribed] 제외).
     */
    const val PREMIUM_OFFERING_LIVE: Boolean = true

    // 디버그 강제 설정 (개발/테스트용) — true면 항상 구독으로 간주
    var debugForceSubscribed: Boolean = false

    @Volatile
    private var subscribedFromDataStore: Boolean = false

    /** [PremiumStatusRepository] 플로우에서 동기 갱신 (UI·기능 분기용) */
    internal fun setSubscribedFromDataStore(value: Boolean) {
        subscribedFromDataStore = value
    }

    /**
     * Compose 등에서 DataStore 플로우 값과 동일한 규칙으로 구독 여부를 계산할 때 사용.
     */
    fun isSubscribedWithStore(storePremium: Boolean, context: Context): Boolean {
        if (!PREMIUM_OFFERING_LIVE) {
            if (BuildConfig.DEBUG && debugForceSubscribed) return true
            return false
        }
        if (BuildConfig.DEBUG && debugForceSubscribed) return true
        return storePremium
    }

    /**
     * 설정 > 구독관리 진입·해당 화면에서 "구독 중" UI를 쓸지.
     * Play/DataStore에 구독 기록이 있으면 [PREMIUM_OFFERING_LIVE]와 무관하게 true (실결제 사용자는 관리 화면으로 보냄).
     */
    fun hasActiveSubscriptionForManagement(storePremium: Boolean, context: Context): Boolean =
        storePremium || isSubscribedWithStore(storePremium, context)

    /**
     * 현재 구독 상태 (기능 게이트)
     * - [PREMIUM_OFFERING_LIVE]가 false면 (디버그 강제 제외) 항상 미구독
     * - DEBUG + [debugForceSubscribed]: true
     * - 그 외: Billing 복원 후 [PremiumStatusRepository] 값
     */
    fun isSubscribed(context: Context): Boolean =
        isSubscribedWithStore(subscribedFromDataStore, context)
}


// ============================================================
// 유료/무료 기능 제한 정의
// 여기서 한눈에 전체 구분 관리
// ============================================================

object SubscriptionFeature {

    // ── 앱 차단 ─────────────────────────
    const val FREE_BLOCK_APP_LIMIT = 3        // 무료: 최대 3개
    const val PRO_BLOCK_APP_LIMIT = Int.MAX_VALUE  // 유료: 무제한

    fun getBlockAppLimit(context: Context): Int {
        return if (SubscriptionManager.isSubscribed(context))
            PRO_BLOCK_APP_LIMIT else FREE_BLOCK_APP_LIMIT
    }

    // ── 통계 ─────────────────────────────
    // 무료: 주간 사용시간, 요일별 그래프, 차단 성공률
    // 유료: 또래 비교, 월별 트렌드 추가
    fun canAccessProStats(context: Context): Boolean {
        return SubscriptionManager.isSubscribed(context)
    }

    // ── 차단 시간 설정 ────────────────────
    // 무료: 기본 시간대만
    // 유료: 커스텀 시간 설정
    fun canUseCustomSchedule(context: Context): Boolean {
        return SubscriptionManager.isSubscribed(context)
    }

    // ── 하루 사용량 제한(드럼롤) ────────────
    // 무료: 30·60분만 선택 가능. 그 외(31~59·61~180분)는 잠금 + 구독 유도.
    // 유료: 전체 옵션
    fun isDailyLimitDrumrollOptionLockedForFreeUser(optionLabel: String): Boolean {
        return optionLabel != "30분" && optionLabel != "60분"
    }

    // ── 데이터 백업 ────────────────────────
    /** 유료만 서버(자동) 백업 플로우 사용. 무료는 기기 수동만. */
    fun canUseServerBackup(context: Context): Boolean =
        SubscriptionManager.isSubscribed(context)

    fun canUseLocalDeviceBackup(context: Context): Boolean = true

    // ── 홈 화면 위젯(핀/안내) ───────────────
    @Suppress("UnusedParameter")
    fun canUseWidget(context: Context): Boolean = true

    // ── AdMob 홈 배너 ──────────────────────
    fun shouldShowHomeBannerAd(context: Context): Boolean =
        !SubscriptionManager.isSubscribed(context)

    // ── 하단 구독 유도 배너(내비 하단) ─────
    fun shouldShowPremiumUpsellNavBanner(context: Context): Boolean =
        !SubscriptionManager.isSubscribed(context)
}
