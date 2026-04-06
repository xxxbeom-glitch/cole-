package com.aptox.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AI 앱 카테고리 분류 디버그
 * - 설치 앱 기준 classifyApps 호출·캐시 저장·실패 시뮬레이션·캐시 덤프
 */
@Composable
fun AiAppCategoryClassificationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cacheRepo = remember { AppCategoryCacheRepository(context) }
    val categoryRepo = remember { AppCategoryRepository(context) }

    var resultNormal by remember { mutableStateOf("") }
    var resultClear by remember { mutableStateOf("") }
    var resultSim by remember { mutableStateOf("") }
    var resultCache by remember { mutableStateOf("") }

    var loadingNormal by remember { mutableStateOf(false) }
    var loadingClear by remember { mutableStateOf(false) }
    var loadingSim by remember { mutableStateOf(false) }
    var loadingCache by remember { mutableStateOf(false) }

    var progressNormal by remember { mutableStateOf("") }
    var progressClear by remember { mutableStateOf("") }
    var progressSim by remember { mutableStateOf("") }

    val anyMutatingBusy = loadingNormal || loadingClear || loadingSim

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AptoxGhostButton(text = "← 돌아가기", onClick = onBack)
        Text(
            text = "AI 앱 카테고리 분류 테스트",
            style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
        )
        Text(
            text = "배치 크기 25 · 성공 시 AppCategoryCacheRepository에 병합 저장 (앱 선택 플로우와 동일)",
            style = AppTypography.Caption1.copy(color = AppColors.TextBody),
        )

        // 1. 정상 호출
        DebugAiSubsectionTitle("1. 정상 호출", "설치 앱 전체 대상 classifyApps")
        AptoxPrimaryButton(
            text = "AI 카테고리 분류 실행",
            onClick = {
                if (anyMutatingBusy) return@AptoxPrimaryButton
                scope.launch {
                    loadingNormal = true
                    progressNormal = "앱 목록 로드 중…"
                    resultNormal = ""
                    try {
                        val apps = loadInstalledUserAppsForDebug(context)
                        if (apps.isEmpty()) {
                            resultNormal = "표시할 사용자 앱이 없습니다."
                            return@launch
                        }
                        val batches = apps.chunked(25)
                        val repo = ClaudeRepository()
                        val merged = mutableMapOf<String, String>()
                        val errors = mutableListOf<String>()
                        for ((i, batch) in batches.withIndex()) {
                            progressNormal = "분류 중… (${i + 1}/${batches.size} 배치)"
                            val r = withContext(Dispatchers.IO) {
                                repo.classifyApps(batch)
                            }
                            r.onSuccess { list -> list.forEach { merged[it.packageName] = it.category } }
                            r.onFailure { e -> errors.add("배치 ${i + 1}: ${e.message}") }
                        }
                        if (merged.isNotEmpty()) {
                            withContext(Dispatchers.IO) { cacheRepo.saveCategories(merged) }
                        }
                        resultNormal = buildClassifySummaryText(
                            totalApps = apps.size,
                            batchCount = batches.size,
                            merged = merged,
                            errors = errors,
                        )
                    } finally {
                        loadingNormal = false
                        progressNormal = ""
                    }
                }
            },
            enabled = !anyMutatingBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (loadingNormal) {
            DebugAiProgressRow(label = progressNormal.ifBlank { "진행 중…" })
        }
        DebugAiResultBox(text = resultNormal)

        // 2. 캐시 초기화 후 재분류
        DebugAiSubsectionTitle("2. 캐시 초기화 후 재분류", "DataStore AI 캐시 삭제 → API 재호출")
        AptoxPrimaryButton(
            text = "캐시 초기화 후 재분류",
            onClick = {
                if (anyMutatingBusy) return@AptoxPrimaryButton
                scope.launch {
                    loadingClear = true
                    progressClear = "캐시 삭제 중…"
                    resultClear = ""
                    try {
                        withContext(Dispatchers.IO) { cacheRepo.clearAll() }
                        progressClear = "앱 목록 로드 중…"
                        val apps = loadInstalledUserAppsForDebug(context)
                        if (apps.isEmpty()) {
                            resultClear = "캐시는 비웠으나 대상 앱이 없습니다."
                            return@launch
                        }
                        val batches = apps.chunked(25)
                        val repo = ClaudeRepository()
                        val merged = mutableMapOf<String, String>()
                        val errors = mutableListOf<String>()
                        for ((i, batch) in batches.withIndex()) {
                            progressClear = "재분류 중… (${i + 1}/${batches.size} 배치)"
                            val r = withContext(Dispatchers.IO) {
                                repo.classifyApps(batch)
                            }
                            r.onSuccess { list -> list.forEach { merged[it.packageName] = it.category } }
                            r.onFailure { e -> errors.add("배치 ${i + 1}: ${e.message}") }
                        }
                        if (merged.isNotEmpty()) {
                            withContext(Dispatchers.IO) { cacheRepo.saveCategories(merged) }
                        }
                        resultClear = buildString {
                            appendLine("(캐시 전체 삭제 후 실행)")
                            appendLine()
                            append(
                                buildClassifySummaryText(
                                    totalApps = apps.size,
                                    batchCount = batches.size,
                                    merged = merged,
                                    errors = errors,
                                ),
                            )
                        }
                    } finally {
                        loadingClear = false
                        progressClear = ""
                    }
                }
            },
            enabled = !anyMutatingBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (loadingClear) {
            DebugAiProgressRow(label = progressClear.ifBlank { "진행 중…" })
        }
        DebugAiResultBox(text = resultClear)

        // 3. API 실패 시뮬레이션
        DebugAiSubsectionTitle("3. API 실패 시뮬레이션", "호출 없이 실패 · AddApp 플로우는 배치당 getOrNull() 후 continue")
        AptoxPrimaryButton(
            text = "API 실패 시뮬레이션",
            onClick = {
                if (anyMutatingBusy) return@AptoxPrimaryButton
                scope.launch {
                    loadingSim = true
                    progressSim = "앱 목록 로드 중…"
                    resultSim = ""
                    try {
                        val apps = loadInstalledUserAppsForDebug(context)
                        if (apps.isEmpty()) {
                            resultSim = "대상 앱이 없습니다."
                            return@launch
                        }
                        val batches = apps.chunked(25)
                        val repo = ClaudeRepository()
                        val merged = mutableMapOf<String, String>()
                        val errors = mutableListOf<String>()
                        for ((i, batch) in batches.withIndex()) {
                            progressSim = "시뮬 실패 배치 (${i + 1}/${batches.size})"
                            val r = withContext(Dispatchers.IO) {
                                repo.classifyApps(batch, debugSimulateFailure = true)
                            }
                            r.onSuccess { list -> list.forEach { merged[it.packageName] = it.category } }
                            r.onFailure { e -> errors.add("배치 ${i + 1}: ${e.message}") }
                        }
                        val sample = apps.take(12)
                        val resolvedLines = sample.map { (pkg, label) ->
                            val cat = withContext(Dispatchers.IO) { categoryRepo.getCategory(pkg) }
                            "$pkg ($label) → AppCategoryRepository: $cat"
                        }
                        resultSim = buildString {
                            appendLine("시뮬레이션: Firebase classifyApps 호출 없음, 모든 배치 실패 예상.")
                            appendLine("이번 실행으로 캐시에 저장된 항목: ${merged.size}개 (0이어야 정상)")
                            appendLine("배치 오류 ${errors.size}건 (요약):")
                            errors.take(5).forEach { appendLine("  · $it") }
                            if (errors.size > 5) appendLine("  · … 외 ${errors.size - 5}건")
                            appendLine()
                            appendLine("AppCategoryRepository 조회 샘플 (수동 오버라이드가 있으면 해당 패키지만 다를 수 있음):")
                            resolvedLines.forEach { appendLine(it) }
                            appendLine()
                            appendLine("프로덕션: 캐시 미스 + API 실패 시 해당 배치는 스킵되어 캐시에 안 남음 → 조회 시 「기타」.")
                        }
                    } finally {
                        loadingSim = false
                        progressSim = ""
                    }
                }
            },
            enabled = !anyMutatingBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (loadingSim) {
            DebugAiProgressRow(label = progressSim.ifBlank { "진행 중…" })
        }
        DebugAiResultBox(text = resultSim)

        // 4. 캐시 덤프
        DebugAiSubsectionTitle("4. 캐시 상태", "DataStore app_category_cache")
        AptoxPrimaryButton(
            text = "현재 캐시 상태 보기",
            onClick = {
                if (loadingCache) return@AptoxPrimaryButton
                scope.launch {
                    loadingCache = true
                    resultCache = ""
                    try {
                        val map = withContext(Dispatchers.IO) { cacheRepo.getCache() }
                        resultCache = if (map.isEmpty()) {
                            "(비어 있음)"
                        } else {
                            buildString {
                                appendLine("총 ${map.size}개")
                                appendLine()
                                append(
                                    formatCategoryLines(
                                        map,
                                        maxLines = 200,
                                    ),
                                )
                            }
                        }
                    } finally {
                        loadingCache = false
                    }
                }
            },
            enabled = !loadingCache,
            modifier = Modifier.fillMaxWidth(),
        )
        if (loadingCache) {
            DebugAiProgressRow(label = "캐시 읽는 중…")
        }
        DebugAiResultBox(text = resultCache)
    }
}

@Composable
private fun DebugAiSubsectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = AppTypography.Caption2.copy(color = AppColors.TextHighlight),
        )
        Text(
            text = subtitle,
            style = AppTypography.Caption1.copy(color = AppColors.TextBody),
        )
    }
}

@Composable
private fun DebugAiProgressRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(
            color = AppColors.Primary400,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = AppTypography.Caption1.copy(color = AppColors.TextSecondary),
        )
    }
}

@Composable
private fun DebugAiResultBox(text: String) {
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp, max = 240.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceBackgroundCard)
            .verticalScroll(scroll)
            .padding(12.dp),
    ) {
        Text(
            text = text.ifBlank { "(버튼을 누르면 결과가 여기에 표시됩니다)" },
            style = AppTypography.Caption1.copy(color = AppColors.TextPrimary),
        )
    }
}

private fun buildClassifySummaryText(
    totalApps: Int,
    batchCount: Int,
    merged: Map<String, String>,
    errors: List<String>,
): String = buildString {
    appendLine("대상 앱: ${totalApps}개, 배치: ${batchCount}개")
    appendLine("API 성공 반영(저장 맵): ${merged.size}개")
    if (errors.isNotEmpty()) {
        appendLine("배치 오류 ${errors.size}건:")
        errors.take(8).forEach { appendLine("  · $it") }
        if (errors.size > 8) appendLine("  · … 외 ${errors.size - 8}건")
    }
    appendLine()
    appendLine("분류 결과 (패키지 → 카테고리, 최대 120줄):")
    append(formatCategoryLines(merged, maxLines = 120))
}

private fun formatCategoryLines(map: Map<String, String>, maxLines: Int): String {
    if (map.isEmpty()) return "(결과 맵 비어 있음)"
    val sorted = map.entries.sortedBy { it.key }
    if (sorted.size <= maxLines) {
        return sorted.joinToString("\n") { "${it.key} → ${it.value}" }
    }
    return sorted.take(maxLines).joinToString("\n") { "${it.key} → ${it.value}" } +
        "\n… 외 ${sorted.size - maxLines}개"
}

private suspend fun loadInstalledUserAppsForDebug(context: Context): List<Pair<String, String>> =
    withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val selfPkg = context.packageName

        val installedApps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        } ?: emptyList()

        val userApps = installedApps
            .filter { info ->
                info.packageName != selfPkg &&
                    ((info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            }
            .mapNotNull { info ->
                val name = (pm.getApplicationLabel(info) as? String)?.takeIf { it.isNotBlank() }
                name?.let { info.packageName to it }
            }
            .distinctBy { it.first }
            .sortedBy { it.second.lowercase() }

        if (userApps.isNotEmpty()) {
            userApps
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolves = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            resolves
                .mapNotNull { ri ->
                    runCatching {
                        val pkg = ri.activityInfo.packageName
                        if (pkg == selfPkg) return@mapNotNull null
                        val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getApplicationInfo(pkg, 0)
                        }
                        val name = (pm.getApplicationLabel(appInfo) as? String)?.takeIf { it.isNotBlank() }
                        name?.let { pkg to it }
                    }.getOrNull()
                }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
        }
    }
