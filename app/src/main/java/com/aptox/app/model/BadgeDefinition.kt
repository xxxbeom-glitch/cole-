package com.aptox.app.model

import com.aptox.app.R

/**
 * 배지 마스터 데이터 (Firestore badges 컬렉션과 동기화 예정)
 */
data class BadgeDefinition(
    val id: String,
    val order: Int,
    val title: String,
    val description: String,
    val condition: String,
    val icon: String,
    val message: String? = null,
) {
    val iconResId: Int get() = when (icon) {
        "ico_level1" -> R.drawable.ico_level1
        "ico_level2" -> R.drawable.ico_level2
        "ico_level3" -> R.drawable.ico_level3
        "ico_level4" -> R.drawable.ico_level4
        "ico_level5" -> R.drawable.ico_level5
        "ico_level6" -> R.drawable.ico_level6
        "ico_level7" -> R.drawable.ico_level7
        "ico_level8" -> R.drawable.ico_level8
        "ico_level9" -> R.drawable.ico_level9
        "ico_level10" -> R.drawable.ico_level10
        "ico_level11" -> R.drawable.ico_level11
        "ico_level12" -> R.drawable.ico_level12
        "ico_level13" -> R.drawable.ico_level13
        "ico_level14" -> R.drawable.ico_level14
        "ico_level15" -> R.drawable.ico_level15
        "ico_level16" -> R.drawable.ico_level16
        "ico_level17" -> R.drawable.ico_level17
        "ico_level18" -> R.drawable.ico_level18
        "ico_lock_challange" -> R.drawable.ico_lock_challange
        else -> R.drawable.ico_level1
    }
}

/**
 * 배지 마스터 데이터 (Firestore badges 컬렉션 시드)
 */
object BadgeMasterData {
    val badges: List<BadgeDefinition> = listOf(
        BadgeDefinition("badge_001", 1, "첫 걸음", "디지털 디톡스의 시작", "제한 앱 처음 등록했을 때 자동 지급", "ico_level1"),
        BadgeDefinition("badge_002", 2, "첫 시작", "첫 번째 카운트를 시작했어요", "카운트 시작 버튼 처음 눌렀을 때", "ico_level2"),
        BadgeDefinition("badge_003", 3, "절제 완성", "절제가 습관이 된 사람", "30일 연속 목표 달성", "ico_level3"),
        BadgeDefinition("badge_004", 4, "첫 성취", "오늘 하루를 지켜냈어요", "목표 달성한 날 누적 5일", "ico_level4", message = "오늘 하루를 지켜냈어요. 내일도 할 수 있어요!"),
        BadgeDefinition("badge_005", 5, "꾸준한 실천", "반복이 실력이 되는 중", "목표 달성한 날 누적 10일", "ico_level5"),
        BadgeDefinition("badge_006", 6, "불굴의 의지", "의지력이 증명된 사람", "목표 달성한 날 누적 30일", "ico_level6"),
        BadgeDefinition("badge_007", 7, "첫 절약", "오늘의 첫 절약 성공", "오늘 제한 시간 안에 처음 끝냈을 때", "ico_level7"),
        BadgeDefinition("badge_008", 8, "여유로운 절제", "여유있게 절제하고 있어요", "제한 시간의 10% 이상 남기고 끝냈을 때", "ico_level8"),
        BadgeDefinition("badge_009", 9, "절제의 달인", "절제의 달인이 됐어요", "제한 시간의 20% 이상 남기고 끝냈을 때", "ico_level9"),
        BadgeDefinition("badge_010", 10, "첫 연속", "3일을 연속으로 지켜냈어요", "3일 연속 목표 달성", "ico_level10"),
        BadgeDefinition("badge_011", 11, "일주일의 기적", "일주일을 해냈어요", "7일 연속 목표 달성", "ico_level11"),
        BadgeDefinition("badge_012", 12, "두 주의 습관", "습관이 만들어지고 있어요", "14일 연속 목표 달성", "ico_level12"),
        BadgeDefinition("badge_013", 13, "단단한 의지", "의지가 단단해졌어요", "목표 달성한 날 누적 20일", "ico_level13"),
        BadgeDefinition("badge_014", 14, "철저한 절제", "철저하게 절제하고 있어요", "제한 시간의 30% 이상 남기고 끝냈을 때", "ico_level14"),
        BadgeDefinition("badge_015", 15, "난공불락", "어떤 유혹도 통하지 않아요", "21일 연속 목표 달성", "ico_level15"),
        BadgeDefinition("badge_016", 16, "첫 도전", "첫 번째 도전을 완료했어요", "다이아/별 브론즈+실버 4개 달성 시 자동 지급", "ico_level16"),
        BadgeDefinition("badge_017", 17, "도전 계속", "도전을 멈추지 않고 있어요", "번개/방패 6개 달성 시 자동 지급", "ico_level17"),
        BadgeDefinition("badge_018", 18, "도전 완주", "모든 도전을 완주했어요", "달 3개 + 다이아골드 + 별골드 달성 시 자동 지급", "ico_level18"),
    )
}
