package com.aptox.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

data class AppDetailUiState(
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val totalMinutes: Long,
    val launchCount: Int,
    val avgSessionMinutes: Long,
)

class StatisticsViewModel(
    private val appContext: Context,
) : ViewModel() {

    private val _selectedAppDetail = MutableStateFlow<AppDetailUiState?>(null)
    val selectedAppDetail: StateFlow<AppDetailUiState?> = _selectedAppDetail.asStateFlow()

    private var categoryStatsStartMs: Long = 0L
    private var categoryStatsEndMs: Long = 0L

    /**
     * 카테고리 통계 카드의 기간(주간/월간/연간 오프셋)과 동기화.
     * [onAppItemClick] 시점에 사용할 조회 구간.
     */
    fun syncCategoryStatsPeriod(
        tabEnum: StatisticsData.Tab,
        weekOffset: Int,
        monthOffset: Int,
        yearOffset: Int,
    ) {
        val (start, end) = when (tabEnum) {
            StatisticsData.Tab.WEEKLY ->
                StatisticsData.getWeekRange(weekOffset).let { it.first to it.second }
            StatisticsData.Tab.MONTHLY ->
                StatisticsData.getSingleMonthRange(monthOffset).let { it.first to it.second }
            StatisticsData.Tab.YEARLY ->
                StatisticsData.getYearRange(yearOffset).let { it.first to it.second }
            else -> 0L to 0L
        }
        categoryStatsStartMs = start
        categoryStatsEndMs = end
    }

    fun onAppItemClick(packageName: String) {
        val startMs = categoryStatsStartMs
        val endMs = categoryStatsEndMs
        if (packageName.isBlank() || endMs <= startMs) return

        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                loadAppDetailUiState(appContext, packageName, startMs, endMs)
            }
            _selectedAppDetail.value = state
        }
    }

    fun onBottomSheetDismiss() {
        _selectedAppDetail.value = null
    }

    private fun loadAppDetailUiState(
        context: Context,
        packageName: String,
        startMs: Long,
        endMs: Long,
    ): AppDetailUiState {
        val pm = context.packageManager
        val appName = try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }

        val appIcon = loadAppIconBitmap(pm, packageName)

        val totalMs = queryTotalTimeInForeground(context, packageName, startMs, endMs)
        val totalMinutes = (totalMs / 60_000L).coerceAtLeast(0L)

        val launchCount = countActivityResumedEvents(context, packageName, startMs, endMs)
        val avgSessionMinutes = if (launchCount > 0) {
            ((totalMs.toDouble() / launchCount) / 60_000.0).roundToLong().coerceAtLeast(0L)
        } else {
            0L
        }

        return AppDetailUiState(
            packageName = packageName,
            appName = appName,
            appIcon = appIcon,
            totalMinutes = totalMinutes,
            launchCount = launchCount,
            avgSessionMinutes = avgSessionMinutes,
        )
    }

    companion object {
        private fun loadAppIconBitmap(pm: PackageManager, packageName: String): Bitmap? =
            runCatching {
                val drawable = pm.getApplicationIcon(packageName).mutate()
                val w = drawable.intrinsicWidth.coerceAtLeast(1)
                val h = drawable.intrinsicHeight.coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, w, h)
                drawable.draw(canvas)
                bitmap
            }.getOrNull()

        private fun queryTotalTimeInForeground(
            context: Context,
            packageName: String,
            startMs: Long,
            endMs: Long,
        ): Long {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startMs, endMs)
                ?: return 0L
            return stats
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }
        }

        private fun activityResumedEventType(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                UsageEvents.Event.MOVE_TO_FOREGROUND
            }

        private const val RESUME_SESSION_GAP_MS = 180_000L

        private fun countActivityResumedEvents(
            context: Context,
            packageName: String,
            startMs: Long,
            endMs: Long,
        ): Int {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0
            val events = usm.queryEvents(startMs, endMs) ?: return 0
            val event = UsageEvents.Event()
            val targetType = activityResumedEventType()
            var sessionCount = 0
            var lastResumeTimeMs: Long? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.packageName != packageName || event.eventType != targetType) continue
                val t = event.timeStamp
                if (lastResumeTimeMs == null || t - lastResumeTimeMs > RESUME_SESSION_GAP_MS) {
                    sessionCount++
                }
                lastResumeTimeMs = t
            }
            return sessionCount
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val appCtx = context.applicationContext
            return StatisticsViewModel(appCtx) as T
        }
    }
}
