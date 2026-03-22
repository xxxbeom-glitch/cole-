package com.aptox.app

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 프로덕션 배지 자동 지급 (Firestore 중복 방지 + [GoalAchievementNotificationHelper] 푸시).
 *
 * 뱃지 18개 최종 확정:
 * - 001: 제한 앱 처음 등록 | 002: 카운트 시작 버튼 처음 | 003: 제한 앱 2개 등록
 * - 004~009: 목표 달성 누적 5/10/30/60/100/200일
 * - 010~012: 밤 10시 이후 미사용 첫/7일/30일 | 013~015: 밤 9시 이후 미사용 첫/7일/30일
 * - 016~018: 다른 뱃지 6/10/17개 달성
 */
object BadgeAutoGrant {

    private const val TAG = "BadgeAutoGrant"

    private fun yyyyMMdd(cal: Calendar): String {
        return String.format(
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun yesterdayYyyyMMdd(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return yyyyMMdd(cal)
    }

    private suspend fun grantIfNew(context: Context, userId: String, badgeId: String): Boolean {
        val repo = BadgeRepository(FirebaseFirestore.getInstance(), context.applicationContext)
        val existing = runCatching { repo.getUserBadge(userId, badgeId) }.getOrNull()
        if (existing != null) return false
        val result = repo.grantBadge(userId, badgeId)
        if (result.isFailure) return false
        checkBadgeCountBadgesAfterGrant(context, userId)
        return true
    }

    /** badge_016~018: badge_001~015 중 달성 개수로 판단 (016~018 제외) */
    private val BADGE_001_TO_015 = (1..15).map { "badge_%03d".format(it) }.toSet()

    private suspend fun checkBadgeCountBadgesAfterGrant(context: Context, userId: String) {
        val repo = BadgeRepository(FirebaseFirestore.getInstance(), context.applicationContext)
        val earned = repo.getAllEarnedBadges(userId).keys
        val count = earned.count { it in BADGE_001_TO_015 }
        if (count >= 6) grantIfNew(context, userId, "badge_016")
        if (count >= 10) grantIfNew(context, userId, "badge_017")
        if (count >= 17) grantIfNew(context, userId, "badge_018")
    }

    private fun runAsync(context: Context, block: suspend () -> Unit) {
        val ac = context.applicationContext
        val app = ac as? AptoxApplication ?: return
        app.applicationScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                Log.e(TAG, "배지 처리 실패", e)
            }
        }
    }

    /** badge_001: 최초 제한 등록 */
    fun onFirstRestrictionSaved(context: Context) {
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                PendingBadgesPreferences.addPendingBadge(context, "badge_001")
                return@runAsync
            }
            grantIfNew(context, uid, "badge_001")
        }
    }

    /** badge_003: 제한 앱 2개 등록 */
    fun onSecondRestrictionSaved(context: Context) {
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                PendingBadgesPreferences.addPendingBadge(context, "badge_003")
                return@runAsync
            }
            grantIfNew(context, uid, "badge_003")
        }
    }

    /** badge_002: 카운트 시작 버튼 처음 눌렀을 때 */
    fun onCountStartButtonPressed(context: Context) {
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val count = BadgeStatsPreferences.incrementCountStartTotal(context)
            if (count >= 1) {
                if (uid == null) {
                    PendingBadgesPreferences.addPendingBadge(context, "badge_002")
                    return@runAsync
                }
                grantIfNew(context, uid, "badge_002")
            }
        }
    }

    /**
     * badge_004~009: 목표 달성 누적일.
     * 자정 리셋 시 어제 날짜가 달성했는지 확인 후 cum 증가, 조건 체크.
     */
    fun onMidnightReset(context: Context) {
        val yesterday = yesterdayYyyyMMdd()
        runAsync(context) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (!BadgeStatsPreferences.tryMarkMidnightProcessed(context, yesterday)) return@runAsync

            val progressRepo = BadgeProgressRepository(context)
            val success = restrictionDaySucceeded(context, yesterday)
            if (success) {
                val cum = BadgeStatsPreferences.getDailyGoalCumulative(context) + 1
                BadgeStatsPreferences.setDailyGoalCumulative(context, cum)
                progressRepo.accumulatedAchievementDays = cum
                val streak = BadgeStatsPreferences.getRestrictionStreak(context) + 1
                BadgeStatsPreferences.setRestrictionStreak(context, streak)
                progressRepo.consecutiveAchievementDays = streak

                if (uid == null) {
                    if (cum >= 5) PendingBadgesPreferences.addPendingBadge(context, "badge_004")
                    if (cum >= 10) PendingBadgesPreferences.addPendingBadge(context, "badge_005")
                    if (cum >= 30) PendingBadgesPreferences.addPendingBadge(context, "badge_006")
                    if (cum >= 60) PendingBadgesPreferences.addPendingBadge(context, "badge_007")
                    if (cum >= 100) PendingBadgesPreferences.addPendingBadge(context, "badge_008")
                    if (cum >= 200) PendingBadgesPreferences.addPendingBadge(context, "badge_009")
                } else {
                    if (cum >= 5) grantIfNew(context, uid, "badge_004")
                    if (cum >= 10) grantIfNew(context, uid, "badge_005")
                    if (cum >= 30) grantIfNew(context, uid, "badge_006")
                    if (cum >= 60) grantIfNew(context, uid, "badge_007")
                    if (cum >= 100) grantIfNew(context, uid, "badge_008")
                    if (cum >= 200) grantIfNew(context, uid, "badge_009")
                }
            } else {
                BadgeStatsPreferences.setRestrictionStreak(context, 0)
                progressRepo.consecutiveAchievementDays = 0
            }

            // badge_010~015: 밤 10시/9시 이후 제한 앱 미사용
            val restrictedPkgs = AppRestrictionRepository(context).getAll().map { it.packageName }.toSet()
            if (restrictedPkgs.isNotEmpty()) {
                val usage10pm = StatisticsData.getRestrictedAppUsageAfterHour(context, yesterday, 22)
                val usage9pm = StatisticsData.getRestrictedAppUsageAfterHour(context, yesterday, 21)
                if (usage10pm == 0L) {
                    val n10 = BadgeStatsPreferences.getNight10pmCumulative(context) + 1
                    BadgeStatsPreferences.setNight10pmCumulative(context, n10)
                    if (uid == null) {
                        if (n10 >= 1) PendingBadgesPreferences.addPendingBadge(context, "badge_010")
                        if (n10 >= 7) PendingBadgesPreferences.addPendingBadge(context, "badge_011")
                        if (n10 >= 30) PendingBadgesPreferences.addPendingBadge(context, "badge_012")
                    } else {
                        if (n10 >= 1) grantIfNew(context, uid, "badge_010")
                        if (n10 >= 7) grantIfNew(context, uid, "badge_011")
                        if (n10 >= 30) grantIfNew(context, uid, "badge_012")
                    }
                }
                if (usage9pm == 0L) {
                    val n9 = BadgeStatsPreferences.getNight9pmCumulative(context) + 1
                    BadgeStatsPreferences.setNight9pmCumulative(context, n9)
                    if (uid == null) {
                        if (n9 >= 1) PendingBadgesPreferences.addPendingBadge(context, "badge_013")
                        if (n9 >= 7) PendingBadgesPreferences.addPendingBadge(context, "badge_014")
                        if (n9 >= 30) PendingBadgesPreferences.addPendingBadge(context, "badge_015")
                    } else {
                        if (n9 >= 1) grantIfNew(context, uid, "badge_013")
                        if (n9 >= 7) grantIfNew(context, uid, "badge_014")
                        if (n9 >= 30) grantIfNew(context, uid, "badge_015")
                    }
                }
            }

            // badge_016~018: 로그인 시에만 Firestore 기반으로 재확인
            if (uid != null) {
                checkBadgeCountBadgesOnAppOpen(context, uid)
            }
        }
    }

    /**
     * 앱 기동/자정 리셋 시 badge_016~018 조건 재확인.
     */
    private suspend fun checkBadgeCountBadgesOnAppOpen(context: Context, userId: String) {
        val repo = BadgeRepository(FirebaseFirestore.getInstance(), context.applicationContext)
        val earned = repo.getAllEarnedBadges(userId).keys
        val count = earned.count { it in BADGE_001_TO_015 }
        if (count >= 6) grantIfNew(context, userId, "badge_016")
        if (count >= 10) grantIfNew(context, userId, "badge_017")
        if (count >= 17) grantIfNew(context, userId, "badge_018")
    }

    /** 해당 날짜에 '제한 설정 달성': 제한이 1개 이상 있고, 일일 제한 앱은 모두 그날 누적 사용량 ≤ 제한 */
    fun restrictionDaySucceeded(context: Context, dateYyyyMMdd: String): Boolean {
        val repo = AppRestrictionRepository(context)
        val list = repo.getAll()
        if (list.isEmpty()) return false
        val timer = ManualTimerRepository(context)
        for (r in list) {
            if (r.blockUntilMs == 0L) {
                val used = timer.getAccumMsForDate(r.packageName, dateYyyyMMdd)
                val limitMs = r.limitMinutes * 60L * 1000L
                if (used > limitMs) return false
            }
        }
        return true
    }

    /**
     * 로그인 성공 직후: pendingBadges를 Firestore에 일괄 지급 후 초기화.
     * grantIfNew로 중복 지급 방지. 실패해도 앱 동작에는 영향 없음.
     */
    fun syncPendingBadgesToFirestore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runAsync(context) {
            runCatching {
                val pending = PendingBadgesPreferences.getPendingBadges(context)
                if (pending.isEmpty()) return@runCatching
                for (badgeId in pending) {
                    grantIfNew(context, uid, badgeId)
                }
                PendingBadgesPreferences.clearPendingBadges(context)
            }.onFailure { e ->
                Log.e(TAG, "pendingBadges 동기화 실패", e)
            }
        }
    }

    /**
     * 로그인 직후·앱 기동 시: 제한이 1개 이상이면 badge_001 지급 재시도.
     */
    fun onUserSignedInTryBadge001(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val hasRestriction = AppRestrictionRepository(context.applicationContext).getAll().isNotEmpty()
        if (!hasRestriction) return
        runAsync(context) {
            grantIfNew(context, uid, "badge_001")
            checkBadgeCountBadgesOnAppOpen(context, uid)
        }
    }

    /** 앱 기동 시(이미 로그인) badge_016~018 재확인 */
    fun onAppLaunchCheckBadgeCountBadges(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runAsync(context) { checkBadgeCountBadgesOnAppOpen(context, uid) }
    }

    // --- 레거시 유지: 기존 호출처 제거 시까지
    @Deprecated("badge_007~009, 014는 누적일 기준으로 변경됨. 호출만 유지.")
    @Suppress("UNUSED_PARAMETER")
    fun onCountEnded(context: Context, packageName: String, limitMinutes: Int, totalUsageMs: Long) {
        // no-op
    }

    @Deprecated("badge_007~009는 누적일 기준으로 변경됨")
    @JvmStatic
    fun onTimeBlockWindowEnded(context: Context, packageName: String, blockUntilMs: Long) {
        // no-op
    }

    @Deprecated("badge_014는 누적일 기준으로 변경됨")
    fun onBlockDefenseOverlayShown(context: Context) {
        // no-op
    }

    @Deprecated("badge_016~018은 뱃지 개수 기준으로 변경됨")
    suspend fun checkWeeklyUsageReductionOnStatsOpen(context: Context) {
        // no-op
    }

    /** 디버그: 누적/연속 수치 반영 후 조건에 맞는 배지 지급 */
    suspend fun debugApplyProgressAndGrant(context: Context, userId: String, accum: Int, consec: Int): List<String> {
        BadgeStatsPreferences.setDailyGoalCumulative(context, accum)
        BadgeStatsPreferences.setRestrictionStreak(context, consec)
        val progressRepo = BadgeProgressRepository(context)
        progressRepo.accumulatedAchievementDays = accum
        progressRepo.consecutiveAchievementDays = consec
        val granted = mutableListOf<String>()
        if (accum >= 5 && grantIfNew(context, userId, "badge_004")) granted.add("badge_004")
        if (accum >= 10 && grantIfNew(context, userId, "badge_005")) granted.add("badge_005")
        if (accum >= 30 && grantIfNew(context, userId, "badge_006")) granted.add("badge_006")
        if (accum >= 60 && grantIfNew(context, userId, "badge_007")) granted.add("badge_007")
        if (accum >= 100 && grantIfNew(context, userId, "badge_008")) granted.add("badge_008")
        if (accum >= 200 && grantIfNew(context, userId, "badge_009")) granted.add("badge_009")
        return granted
    }
}
