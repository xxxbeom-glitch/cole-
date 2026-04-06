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
     * @param debugSimulateFailure true면 네트워크 호출 없이 실패만 반환 (디버그 화면용)
     * @return Result에 results: List<ClassifyResult>
     */
    suspend fun classifyApps(
        apps: List<Pair<String, String>>,
        debugSimulateFailure: Boolean = false,
    ): Result<List<ClassifyResult>> = runCatching {
        if (debugSimulateFailure) {
            error("[디버그] API 실패 시뮬레이션")
        }
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
}
