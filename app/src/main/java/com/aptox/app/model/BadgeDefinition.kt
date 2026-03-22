package com.aptox.app.model

import com.aptox.app.R

/**
 * 배지 마스터 데이터 (Firestore badges 컬렉션과 동기화)
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
        BadgeDefinition("badge_001", 1, "첫 시작", "디지털 디톡스의 시작", "제한 앱 처음 등록했을 때", "ico_level1", message = "시작이 반이에요. 잘 하고 있어요!"),
        BadgeDefinition("badge_002", 2, "절제의 길", "카운트 시작 버튼을 처음 눌렀어요", "카운트 시작 버튼 처음 눌렀을 때", "ico_level2", message = "첫 카운트를 눌렀군요. 이제 진짜 시작이에요!"),
        BadgeDefinition("badge_003", 3, "절제 완성", "제한 앱 2개 이상 등록했을 때", "제한 앱 2개 이상 등록했을 때", "ico_level3", message = "제한 앱을 하나 더 추가했어요. 절제의 폭이 넓어지고 있어요!"),
        BadgeDefinition("badge_004", 4, "첫 성취", "오늘 하루를 지켜냈어요", "목표 달성한 날 누적 5일", "ico_level4", message = "오늘 하루를 지켜냈어요. 내일도 할 수 있어요!"),
        BadgeDefinition("badge_005", 5, "꾸준한 실천", "반복이 실력이 되는 중", "목표 달성한 날 누적 10일", "ico_level5", message = "10일이 쌓였어요. 꾸준함이 가장 강한 힘이에요!"),
        BadgeDefinition("badge_006", 6, "불굴의 의지", "의지력이 증명된 사람", "목표 달성한 날 누적 30일", "ico_level6", message = "30일의 기록이 증명해요. 당신의 의지는 진짜예요!"),
        BadgeDefinition("badge_007", 7, "의지의 증명", "목표 달성을 꾸준히 이어가고 있어요", "목표 달성한 날 누적 60일", "ico_level7", message = "60일이 쌓였어요. 절제가 생활이 되었어요!"),
        BadgeDefinition("badge_008", 8, "의지의 결실", "100일의 기록을 만들어냈어요", "목표 달성한 날 누적 100일", "ico_level8", message = "100일을 해냈어요. 당신은 이제 달라졌어요!"),
        BadgeDefinition("badge_009", 9, "의지의 전설", "200일의 기록, 전설이 되었어요", "목표 달성한 날 누적 200일", "ico_level9", message = "200일! 우린 이걸 전설이라고 불러요!"),
        BadgeDefinition("badge_010", 10, "야간 절제", "밤 10시 이후 차단을 처음 완료했어요", "밤 10시 이후 제한 앱 첫 미사용", "ico_level10", message = "밤 10시 이후를 지켜냈어요. 훌륭해요!"),
        BadgeDefinition("badge_011", 11, "야간 수호", "밤 10시 이후 미사용 누적 7일", "밤 10시 이후 제한 앱 미사용 누적 7일", "ico_level11", message = "밤 10시 이후 7번을 지켜냈어요. 꾸준함이 쌓이고 있어요!"),
        BadgeDefinition("badge_012", 12, "야간 마스터", "밤 10시 이후 미사용 누적 30일", "밤 10시 이후 제한 앱 미사용 누적 30일", "ico_level12", message = "밤 10시 이후 30번을 지켜냈어요. 이제 습관이 됐어요!"),
        BadgeDefinition("badge_013", 13, "고요의 증명", "밤 9시 이후 차단을 처음 완료했어요", "밤 9시 이후 제한 앱 첫 미사용", "ico_level13", message = "밤 9시 이후를 지켜냈어요. 훌륭해요!"),
        BadgeDefinition("badge_014", 14, "고요의 결실", "밤 9시 이후 미사용 누적 7일", "밤 9시 이후 제한 앱 미사용 누적 7일", "ico_level14", message = "밤 9시 이후 7번을 지켜냈어요. 고요함이 쌓이고 있어요!"),
        BadgeDefinition("badge_015", 15, "고요의 전설", "밤 9시 이후 미사용 누적 30일", "밤 9시 이후 제한 앱 미사용 누적 30일", "ico_level15", message = "밤 9시 이후 30번을 지켜냈어요. 진정한 달인이에요!"),
        BadgeDefinition("badge_016", 16, "승리", "뱃지를 하나씩 모아가고 있어요", "다른 뱃지 6개 이상 달성 시", "ico_level16", message = "6개의 뱃지! 승리자가 되어가고 있어요!"),
        BadgeDefinition("badge_017", 17, "연승", "뱃지 수집이 계속되고 있어요", "다른 뱃지 10개 이상 달성 시", "ico_level17", message = "10개! 멈추지 않는 당신이 가장 멋져요!"),
        BadgeDefinition("badge_018", 18, "왕좌의 자리", "다른 뱃지 17개 달성", "다른 뱃지 17개 이상 달성 시", "ico_level18", message = "뱃지 17개를 달성했어요. 왕좌는 당신 것이에요!"),
    )
}
