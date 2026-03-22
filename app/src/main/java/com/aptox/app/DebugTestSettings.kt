package com.aptox.app

/**
 * 테스트 설정용 디버그 오버라이드 (DEBUG 빌드에서만 사용)
 * - 제한앱 모두 삭제: 직접 호출
 * - 알림내역 갯수: 직접 설정
 * - 주간 챌린지 달성 일수: StatisticsScreen에서 사용
 * - 하루 사용량 3분 옵션: AddAppScreens 일일사용량 제한에서 3분 항목 표시 여부
 */
object DebugTestSettings {

    /** 주간 챌린지 달성 일수 강제 설정. null이면 기본값 사용 */
    var debugWeeklyChallengeDays: Int? = null

    /** 알림내역 테스트용 갯수. null이면 0개 (빈 상태). 0, 3, 5, 10 중 선택 */
    var debugNotificationHistoryCount: Int? = null

    /** 하루 사용량 지정에서 3분 항목 표시. true=3분 포함(테스트용), false=3분 제외 */
    var debugShow3MinDailyOption: Boolean = true
}
