package com.cole.app.subscription

import android.content.Context
import com.cole.app.BuildConfig

// ============================================================
// 구독 상태 관리 - 유료/무료 기능 분기점
// ============================================================

object SubscriptionManager {

    // 디버그 강제 설정 (개발/테스트용)
    // 릴리즈 빌드에선 이 값이 무시됨
    var debugForceSubscribed: Boolean = false

    /**
     * 현재 구독 상태 반환
     * - DEBUG 빌드: debugForceSubscribed 값 사용
     * - RELEASE 빌드: 실제 Play Store 구독 여부 확인
     */
    fun isSubscribed(context: Context): Boolean {
        if (BuildConfig.DEBUG) {
            return debugForceSubscribed
        }
        return checkRealSubscription(context)
    }

    // TODO: Play Store Billing 연동 후 실제 구독 확인 로직 구현
    private fun checkRealSubscription(context: Context): Boolean {
        return false // 기본값: 미구독
    }
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

    // TODO: 기획 확정 후 추가 기능 여기에 추가
    // fun canUseXxx(context: Context): Boolean { ... }
}
