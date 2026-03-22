package com.aptox.app

import android.content.Context
import android.content.SharedPreferences

/**
 * 비로그인 상태에서 달성한 뱃지 ID를 임시 저장.
 * 로그인 시 Firestore로 동기화 후 초기화.
 */
object PendingBadgesPreferences {

    private const val PREFS = "aptox_badge_auto_stats"
    private const val KEY_PENDING_BADGES = "pending_badges"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** pendingBadges에 badgeId 추가. 중복 자동 방지. */
    fun addPendingBadge(ctx: Context, badgeId: String) {
        val current = getPendingBadges(ctx).toMutableSet()
        current.add(badgeId)
        prefs(ctx).edit().putStringSet(KEY_PENDING_BADGES, current).apply()
    }

    /** 대기 중인 뱃지 ID 목록 (수정하지 말 것) */
    fun getPendingBadges(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_PENDING_BADGES, emptySet())?.toSet() ?: emptySet()

    /** 동기화 완료 후 초기화 */
    fun clearPendingBadges(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PENDING_BADGES).apply()
    }

    /** 남은 대기 뱃지로 교체 (동기화 실패 분만 유지) */
    fun setPendingBadges(ctx: Context, badgeIds: Set<String>) {
        if (badgeIds.isEmpty()) {
            clearPendingBadges(ctx)
        } else {
            prefs(ctx).edit().putStringSet(KEY_PENDING_BADGES, badgeIds).apply()
        }
    }
}
