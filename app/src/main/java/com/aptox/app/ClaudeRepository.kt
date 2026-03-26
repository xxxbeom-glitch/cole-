package com.aptox.app

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/**
 * Claude API 호출 - Firebase Functions callClaude 경유
 */
class ClaudeRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
) {
    /**
     * Claude에 프롬프트 전달 후 응답 반환
     * @param prompt 사용자 입력
     * @return Result에 reply(응답 텍스트), usage(토큰 사용량) 포함
     */
    suspend fun chat(prompt: String): Result<ClaudeResponse> = runCatching {
        val result = functions.getHttpsCallable("callClaude")
            .call(hashMapOf("prompt" to prompt))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
            ?: throw IllegalStateException("응답 형식이 올바르지 않습니다.")

        val reply = data["reply"] as? String ?: ""
        val usage = (data["usage"] as? Map<String, Any?>)?.let { usageMap ->
            ClaudeUsage(
                inputTokens = (usageMap["input_tokens"] as? Number)?.toInt() ?: 0,
                outputTokens = (usageMap["output_tokens"] as? Number)?.toInt() ?: 0,
            )
        }

        ClaudeResponse(reply = reply, usage = usage)
    }

    data class ClaudeResponse(
        val reply: String,
        val usage: ClaudeUsage? = null,
    )

    data class ClaudeUsage(
        val inputTokens: Int,
        val outputTokens: Int,
    )

    /**
     * 앱 목록 AI 카테고리 분류 (classifyApps Cloud Function)
     * @return Result에 results: List<ClassifyResult>
     */
    suspend fun classifyApps(apps: List<Pair<String, String>>): Result<List<ClassifyResult>> = runCatching {
        val payload = apps.map { (pkg, name) ->
            hashMapOf("package" to pkg, "appName" to name)
        }
        val result = functions.getHttpsCallable("classifyApps")
            .call(hashMapOf("apps" to payload))
            .await()

        @Suppress("UNCHECKED_CAST")
        val data = result.data as? Map<String, Any?>
            ?: throw IllegalStateException("응답 형식이 올바르지 않습니다.")
        val resultsList = data["results"] as? List<Map<String, Any?>>
            ?: throw IllegalStateException("results가 없습니다.")

        resultsList.map { m ->
            ClassifyResult(
                packageName = (m["package"] as? String) ?: "",
                appName = (m["appName"] as? String) ?: "",
                category = (m["category"] as? String) ?: "기타",
            )
        }
    }

    data class ClassifyResult(
        val packageName: String,
        val appName: String,
        val category: String,
    )

    /**
     * 통계 Brief 카드 총평 생성 (주간/월간/연간 공통)
     * @param periodLabel "주간"/"월간"/"연간"
     * @param dateMinutes 기간별 사용량 분 (주간=7요일, 월간=일별, 연간=연도별)
     * @param dateLabels dateMinutes 대응 라벨 (예: 월~일, 1일~31일, 2020~2025)
     * @param segments 카테고리별 비율 (카테고리명 to 비율%)
     * @param timeSlotMinutes 12개 슬롯(0~2, 2~4, ..., 22~24시) 사용량 분
     */
    suspend fun generateBriefSummary(
        periodLabel: String,
        dateMinutes: List<Long>,
        dateLabels: List<String>,
        segments: List<Pair<String, Float>>,
        timeSlotMinutes: List<Long>,
    ): Result<String> = runCatching {
        val periodDesc = when (periodLabel) {
            "월간" -> "한 달"
            "연간" -> "한 해"
            else -> "한 주"
        }
        val prompt = buildString {
            append("다음은 디지털 디톡스 앱 사용자의 $periodDesc 통계 데이터입니다.\n\n")
            append("## 기간별 사용량 (분)\n")
            dateLabels.forEachIndexed { i, label ->
                append("$label: ${dateMinutes.getOrElse(i) { 0L }}분\n")
            }
            append("\n## 카테고리 통계 (비율%)\n")
            segments.forEach { (cat, pct) -> append("$cat: ${"%.1f".format(pct)}%\n") }
            append("\n## 시간대별 사용량 (2시간 단위, 분)\n")
            val slotLabels = listOf("0~2시", "2~4시", "4~6시", "6~8시", "8~10시", "10~12시", "12~14시", "14~16시", "16~18시", "18~20시", "20~22시", "22~24시")
            slotLabels.forEachIndexed { i, l -> append("$l: ${timeSlotMinutes.getOrElse(i) { 0L }}분\n") }
            append("\n위 데이터를 바탕으로 2~3문장의 친근하고 격려하는 말투 총평을 작성해주세요. 다른 설명 없이 총평만 출력해주세요.")
        }
        val resp = chat(prompt).getOrThrow()
        resp.reply.trim()
    }

    /**
     * Brief 카드용 타이틀+본문 생성 (주간/월간/연간)
     * @param totalUsageMinutes 기간 총 사용 시간 (분). null이면 dateMinutes 합산 사용
     * @param userGoalMinutes 사용자가 설정한 앱별 일일 목표 시간 합산 (분). null이면 미포함
     * @param goalAchievementRate 목표 달성률 0.0~1.0. null이면 미포함
     * @param dailyLimitExceededCount 제한 초과 횟수. null이면 미포함
     * @return Result<Pair<title, body>>
     */
    /**
     * Brief 카드용 타이틀+본문 생성
     * - periodLabel "일간": Daily Brief 전용. 항상 "어제" 기준 표현 사용.
     *   타이틀 35자 이내, 본문 2문장 이내.
     * - periodLabel "월간"/"연간"/"주간": 기존 동작 유지.
     */
    suspend fun generateBriefSummaryWithTitle(
        periodLabel: String,
        dateMinutes: List<Long>,
        dateLabels: List<String>,
        segments: List<Pair<String, Float>>,
        timeSlotMinutes: List<Long>,
        totalUsageMinutes: Long? = null,
        userGoalMinutes: Long? = null,
        goalAchievementRate: Float? = null,
        dailyLimitExceededCount: Int? = null,
    ): Result<Pair<String, String>> = runCatching {
        val isDaily = periodLabel == "일간"
        val (periodDesc, titlePrefix) = when (periodLabel) {
            "일간" -> "어제 하루" to "어제는"
            "월간" -> "한 달" to "지난달은"
            "연간" -> "한 해" to "지난해는"
            else -> "한 주" to "지난주는"
        }
        val totalMinutes = totalUsageMinutes ?: dateMinutes.sum()
        val prompt = buildString {
            append("당신은 디지털 디톡스 코치입니다. 이 앱의 목표는 스마트폰 사용 시간을 줄이는 것입니다.\n\n")
            append("평가 기준:\n")
            append("- 총 사용 시간이 적을수록 좋음\n")
            append("- 하루 제한 목표 달성률이 높을수록 좋음\n")
            append("- 제한 초과 횟수가 적을수록 좋음\n")
            append("- 야간(밤 9시 이후) 사용이 없을수록 좋음\n")
            append("좋은 결과는 칭찬하되, 나쁜 결과는 솔직하게 지적하고 개선을 권유하세요.\n\n")
            append("다음은 디지털 디톡스 앱 사용자의 $periodDesc 통계 데이터입니다.\n\n")
            append("## 목표 대비 현황\n")
            append("총 사용 시간: ${totalMinutes}분\n")
            if (userGoalMinutes != null) append("목표 시간: ${userGoalMinutes}분\n")
            if (goalAchievementRate != null) append("목표 달성률: ${"%.0f".format(goalAchievementRate * 100)}%\n")
            if (dailyLimitExceededCount != null) append("제한 초과 횟수: ${dailyLimitExceededCount}회\n")
            append("\n## 기간별 사용량 (분)\n")
            dateLabels.forEachIndexed { i, label ->
                append("$label: ${dateMinutes.getOrElse(i) { 0L }}분\n")
            }
            append("\n## 카테고리 통계 (비율%)\n")
            segments.forEach { (cat, pct) -> append("$cat: ${"%.1f".format(pct)}%\n") }
            append("\n## 시간대별 사용량 (2시간 단위, 분)\n")
            val slotLabels = listOf("0~2시", "2~4시", "4~6시", "6~8시", "8~10시", "10~12시", "12~14시", "14~16시", "16~18시", "18~20시", "20~22시", "22~24시")
            slotLabels.forEachIndexed { i, l -> append("$l: ${timeSlotMinutes.getOrElse(i) { 0L }}분\n") }
            append("\n위 데이터를 바탕으로 응답을 두 부분으로 작성해주세요.\n")
            if (isDaily) {
                append("1) 첫 줄: \"$titlePrefix\"으로 시작하는 타이틀.\n")
                append("   - 반드시 \"어제\" 기준 표현만 사용. \"지난주\", \"이번 주\" 등 주 단위 표현 절대 금지.\n")
                append("   - 35자 이내로 작성 (공백 포함).\n")
                append("2) 빈 줄 뒤: 친근한 말투 본문. 정확히 2문장 이내로 작성.\n")
            } else {
                append("1) 첫 줄: \"$titlePrefix\"으로 시작하는 한 문장 타이틀\n")
                append("2) 빈 줄 뒤: 2~3문장의 친근한 말투 본문 (결과가 좋으면 칭찬, 나쁘면 솔직하게 지적 후 개선 권유)\n")
            }
            append("타이틀과 본문만 출력하세요. 다른 설명 없이.")
        }
        val resp = chat(prompt).getOrThrow()
        val text = resp.reply.trim()
        val parts = text.split("\n\n", limit = 2)
        var title = parts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() } ?: titlePrefix
        val body = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: text
        // Daily Brief: 타이틀 35자 초과 시 잘라내기
        if (isDaily && title.length > 35) title = title.take(35)
        title to body
    }
}
