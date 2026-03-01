package com.cole.app

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import java.util.Calendar

data class AppUsageItem(val packageName: String, val appName: String, val totalTimeMs: Long)

@Composable
fun UsageStatsTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasUsageAccess(context)) {
            error = "PACKAGE_USAGE_STATS 권한이 필요합니다. 권한 요청 화면에서 허용해주세요."
        } else {
            items = loadUsageStats(context)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("앱별 사용시간 (오늘)", style = AppTypography.HeadingH3)
            androidx.compose.material3.TextButton(onClick = onBack) { Text("돌아가기") }
        }

        error?.let { Text(it, modifier = Modifier.padding(8.dp), style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary)) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.appName, style = AppTypography.BodyMedium)
                    Text("${item.totalTimeMs / 60000}분", style = AppTypography.BodyMedium)
                }
            }
        }
    }
}

private fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun loadUsageStats(context: Context): List<AppUsageItem> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        ?: return emptyList()
    val pm = context.packageManager

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis
    val endTime = System.currentTimeMillis()

    val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime,
    ) ?: return emptyList()

    return stats
        .filter { it.totalTimeInForeground > 0 }
        .groupBy { it.packageName }
        .map { (packageName, statList) ->
            val totalMs = statList.sumOf { it.totalTimeInForeground }
            val name = try {
                pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
            } catch (_: PackageManager.NameNotFoundException) {
                packageName
            }
            AppUsageItem(packageName, name, totalMs)
        }
        .sortedByDescending { it.totalTimeMs }
}