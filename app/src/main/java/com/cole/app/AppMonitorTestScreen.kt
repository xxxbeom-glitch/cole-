package com.cole.app

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "AppMonitor"

@Composable
fun AppMonitorTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var packageName by remember { mutableStateOf("") }
    var limitMinutes by remember { mutableStateOf("60") }
    var foregroundApp by remember { mutableStateOf<String?>(null) }
    var isServiceRunning by remember { mutableStateOf(false) }
    var todayUsageMinutes by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val result = withContext(Dispatchers.Default) {
                getForegroundApp(context)
            }
            foregroundApp = result

            val pkg = packageName.trim()
            if (pkg.isNotEmpty()) {
                todayUsageMinutes = withContext(Dispatchers.Default) {
                    getTodayUsageMinutes(context, pkg)
                }
                Log.d(TAG, "foreground=$result | $pkg 오늘 사용시간=${todayUsageMinutes}분 | 제한=${limitMinutes}분")
            }

            isServiceRunning = isServiceRunning(context, AppMonitorService::class.java)
            delay(1500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("앱 모니터 서비스 (30초마다 체크)")

        Text("서비스 상태: ${if (isServiceRunning) "🟢 실행중" else "⚫ 중지됨"}")

        Text("현재 foreground: ${foregroundApp ?: "-"}")

        if (packageName.trim().isNotEmpty() && todayUsageMinutes != null) {
            Text("오늘 사용시간: ${todayUsageMinutes}분 / ${limitMinutes}분")
        }

        Text("패키지명", style = AppTypography.Caption1)
        ColeTextField(
            value = packageName,
            onValueChange = { packageName = it },
            placeholder = "com.instagram.android",
            modifier = Modifier.fillMaxWidth(),
        )

        Text("제한 시간(분)", style = AppTypography.Caption1)
        ColeTextField(
            value = limitMinutes,
            onValueChange = { limitMinutes = it.filter { c -> c.isDigit() } },
            placeholder = "60",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        ColePrimaryButton(
            text = if (isServiceRunning) "서비스 중지" else "모니터링 시작",
            onClick = {
                if (isServiceRunning) {
                    Log.d(TAG, "서비스 중지")
                    AppMonitorService.stop(context)
                    isServiceRunning = false
                } else {
                    val pkg = packageName.trim()
                    val mins = limitMinutes.toIntOrNull() ?: 60
                    Log.d(TAG, "서비스 시작 | 패키지=$pkg | 제한=${mins}분")
                    val map = if (pkg.isNotEmpty()) mapOf(pkg to mins) else emptyMap()
                    AppMonitorService.start(context, map)
                    isServiceRunning = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        ColeGhostButton(text = "돌아가기", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

private fun getForegroundApp(context: Context): String? {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 5_000
    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime) ?: return null
    return stats
        .filter { it.totalTimeInForeground > 0 && it.packageName != context.packageName }
        .maxByOrNull { it.lastTimeUsed }
        ?.packageName
}

private fun getTodayUsageMinutes(context: Context, packageName: String): Long {
    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return 0
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val stats = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        cal.timeInMillis,
        System.currentTimeMillis()
    ) ?: return 0
    val ms = stats
        .filter { it.packageName == packageName }
        .sumOf { it.totalTimeInForeground }
    return ms / 60_000
}

@Suppress("DEPRECATION")
private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.getRunningServices(Int.MAX_VALUE).any { it.service.className == serviceClass.name }
}