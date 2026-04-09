package com.aptox.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aptox.app.MainActivity
import com.aptox.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 위젯 1 — 하루 사용량 제한 현황 (4×2).
 */
class AptoxDailyLimitWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, AptoxDailyLimitWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                try { onUpdate(app, mgr, ids) } catch (t: Throwable) {
                    Log.e(TAG, "onReceive refresh failed", t)
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val app = context.applicationContext
        try {
            val views = buildViews(app)
            for (id in appWidgetIds) appWidgetManager.updateAppWidget(id, views)
        } catch (t: Throwable) {
            Log.e(TAG, "onUpdate failed", t)
            val fallback = buildFallback(app)
            for (id in appWidgetIds) {
                try { appWidgetManager.updateAppWidget(id, fallback) } catch (_: Throwable) {}
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedule(context.applicationContext)
    }

    override fun onDisabled(context: Context) {
        cancel(context.applicationContext)
        super.onDisabled(context)
    }

    companion object {
        private const val TAG = "AptoxDailyWidget"
        const val ACTION_REFRESH = "com.aptox.app.action.DAILY_WIDGET_REFRESH"
        private const val REQUEST_CODE = 94010
        private const val THIRTY_MIN_MS = 30 * 60 * 1000L

        fun updateAll(context: Context) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, AptoxDailyLimitWidgetProvider::class.java))
            if (ids.isEmpty()) return
            try {
                val views = buildViews(app)
                for (id in ids) mgr.updateAppWidget(id, views)
            } catch (t: Throwable) { Log.e(TAG, "updateAll failed", t) }
        }

        fun buildViews(context: Context): RemoteViews {
            val app = context.applicationContext
            val views = RemoteViews(app.packageName, R.layout.widget_daily_limit_4x2)

            // 탭 → 메인 화면
            val pi = PendingIntent.getActivity(
                app, REQUEST_CODE,
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_daily_root, pi)

            val data = runCatching {
                runBlocking(Dispatchers.IO) { DailyLimitWidgetDataLoader.load(app) }
            }.getOrElse { DailyLimitWidgetDataLoader.empty() }

            bind(views, data)
            return views
        }

        private fun buildFallback(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_limit_4x2)
            return try { bind(views, DailyLimitWidgetDataLoader.empty()); views } catch (_: Throwable) { views }
        }

        private val rowIds = listOf(
            Triple(R.id.widget_daily_row1, R.id.widget_daily_name1, R.id.widget_daily_usage1)
                to R.id.widget_daily_progress1,
            Triple(R.id.widget_daily_row2, R.id.widget_daily_name2, R.id.widget_daily_usage2)
                to R.id.widget_daily_progress2,
            Triple(R.id.widget_daily_row3, R.id.widget_daily_name3, R.id.widget_daily_usage3)
                to R.id.widget_daily_progress3,
        )

        private fun bind(views: RemoteViews, data: DailyLimitWidgetData) {
            val colorNormal = 0xFF7C6AF7.toInt()
            val colorExceeded = 0xFFFF5252.toInt()

            if (data.rows.isEmpty()) {
                views.setViewVisibility(R.id.widget_daily_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_daily_empty, View.VISIBLE)
                rowIds.forEach { (triple, _) ->
                    views.setViewVisibility(triple.first, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.widget_daily_empty, View.GONE)
                views.setViewVisibility(R.id.widget_daily_content, View.VISIBLE)

                rowIds.forEachIndexed { i, (triple, progressId) ->
                    val row = data.rows.getOrNull(i)
                    if (row == null) {
                        views.setViewVisibility(triple.first, View.GONE)
                    } else {
                        views.setViewVisibility(triple.first, View.VISIBLE)
                        views.setTextViewText(triple.second, row.appName)
                        val usageText = if (row.limitMinutes == 0) {
                            "${row.usageMinutes}분"
                        } else {
                            "${row.usageMinutes}분 / ${row.limitMinutes}분"
                        }
                        views.setTextViewText(triple.third, usageText)
                        views.setProgressBar(progressId, 100, row.progressPercent, false)
                        // 초과 여부에 따라 색상 전환 (API 21+, minSdk=26 → 안전)
                        val tint = if (row.isExceeded) colorExceeded else colorNormal
                        views.setInt(progressId, "setProgressTintList", tint)
                    }
                }
            }

            val footerText = when {
                data.totalCount == 0 -> "제한 앱을 추가해보세요"
                else -> "총 ${data.totalCount}개 앱 관리 중"
            }
            views.setTextViewText(R.id.widget_daily_footer, footerText)
        }

        private fun schedule(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = buildRefreshPi(context)
            am.cancel(pi)
            am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + THIRTY_MIN_MS,
                THIRTY_MIN_MS,
                pi,
            )
        }

        fun cancel(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(buildRefreshPi(context))
        }

        private fun buildRefreshPi(context: Context) = PendingIntent.getBroadcast(
            context.applicationContext,
            REQUEST_CODE + 1,
            Intent(context.applicationContext, AptoxDailyLimitWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
