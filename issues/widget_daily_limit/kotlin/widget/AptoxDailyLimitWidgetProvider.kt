package com.aptox.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aptox.app.MainActivity
import com.aptox.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 위젯 1 — 하루 사용량 제한 (2×2, Figma 1718:5871 / 1737:6029 / 1737:5982 / 1737:5952)
 */
class AptoxDailyLimitWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, AptoxDailyLimitWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                try {
                    onUpdate(app, mgr, ids)
                } catch (t: Throwable) {
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
            for (id in appWidgetIds) {
                val views = buildViews(app, appWidgetManager, id)
                appWidgetManager.updateAppWidget(id, views)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onUpdate failed", t)
            for (id in appWidgetIds) {
                try {
                    appWidgetManager.updateAppWidget(id, buildFallback(app, appWidgetManager, id))
                } catch (_: Throwable) {
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val app = context.applicationContext
        try {
            val views = buildViews(app, appWidgetManager, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (t: Throwable) {
            Log.e(TAG, "onAppWidgetOptionsChanged failed", t)
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
                for (id in ids) {
                    val views = buildViews(app, mgr, id)
                    mgr.updateAppWidget(id, views)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "updateAll failed", t)
                try {
                    for (id in ids) {
                        mgr.updateAppWidget(id, buildFallback(app, mgr, id))
                    }
                } catch (_: Throwable) {
                }
            }
        }

        fun buildViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ): RemoteViews {
            val app = context.applicationContext
            val views = RemoteViews(app.packageName, R.layout.widget_daily_limit_4x2)

            val pi = PendingIntent.getActivity(
                app,
                REQUEST_CODE,
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_daily_root, pi)
            views.setOnClickPendingIntent(R.id.widget_daily_cta, pi)

            val data = runCatching {
                runBlocking(Dispatchers.IO) { DailyLimitWidgetDataLoader.load(app) }
            }.getOrElse { DailyLimitWidgetDataLoader.empty() }

            bind(views, data, app, appWidgetManager, appWidgetId)
            return views
        }

        private fun buildFallback(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_limit_4x2)
            return try {
                bind(views, DailyLimitWidgetDataLoader.empty(), context, appWidgetManager, appWidgetId)
                views
            } catch (_: Throwable) {
                views
            }
        }

        private fun bind(
            views: RemoteViews,
            data: DailyLimitWidgetData,
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val rows = data.rows.take(3)
            val n = rows.size

            views.setViewVisibility(R.id.widget_daily_row_1, if (n >= 1) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_daily_row_2, if (n >= 2) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_daily_row_3, if (n >= 3) View.VISIBLE else View.GONE)

            val showCta = n < 3
            views.setViewVisibility(R.id.widget_daily_cta, if (showCta) View.VISIBLE else View.GONE)
            views.setViewVisibility(
                R.id.widget_daily_spacer_balance,
                if (n == 3) View.VISIBLE else View.GONE,
            )

            bindRow(views, context, rows.getOrNull(0), R.id.widget_daily_row_1_name, R.id.widget_daily_row_1_status, R.id.widget_daily_row_1_progress)
            bindRow(views, context, rows.getOrNull(1), R.id.widget_daily_row_2_name, R.id.widget_daily_row_2_status, R.id.widget_daily_row_2_progress)
            bindRow(views, context, rows.getOrNull(2), R.id.widget_daily_row_3_name, R.id.widget_daily_row_3_status, R.id.widget_daily_row_3_progress)

            try {
                DailyLimitWidgetAppearance.applyFigmaScale(context, appWidgetManager, appWidgetId, views, n)
            } catch (t: Throwable) {
                Log.e(TAG, "applyFigmaScale failed appWidgetId=$appWidgetId", t)
            }
        }

        private fun bindRow(
            views: RemoteViews,
            context: Context,
            row: DailyLimitWidgetRow?,
            nameId: Int,
            statusId: Int,
            progressId: Int,
        ) {
            if (row == null) {
                views.setTextViewText(nameId, "")
                views.setTextViewText(statusId, "")
                views.setProgressBar(progressId, 100, 0, false)
                return
            }
            views.setTextViewText(nameId, row.appName)
            val status = if (row.limitMinutes == 0) {
                context.getString(R.string.widget_daily_limit_status_unlimited, row.usageMinutes)
            } else {
                context.getString(
                    R.string.widget_daily_limit_status_limited,
                    row.usageMinutes,
                    row.limitMinutes,
                )
            }
            views.setTextViewText(statusId, status)
            val p = if (row.limitMinutes == 0) 0 else row.progressPercent.coerceIn(0, 100)
            views.setProgressBar(progressId, 100, p, false)
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
