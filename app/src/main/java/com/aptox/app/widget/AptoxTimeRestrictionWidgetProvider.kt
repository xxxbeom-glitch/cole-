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
import android.widget.RemoteViews
import com.aptox.app.MainActivity
import com.aptox.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 위젯 2 — 지정 시간 제한 현황 (2×2).
 */
class AptoxTimeRestrictionWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, AptoxTimeRestrictionWidgetProvider::class.java))
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
        private const val TAG = "AptoxTimeWidget"
        const val ACTION_REFRESH = "com.aptox.app.action.TIME_WIDGET_REFRESH"
        private const val REQUEST_CODE = 94020
        private const val THIRTY_MIN_MS = 30 * 60 * 1000L

        fun updateAll(context: Context) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val ids = mgr.getAppWidgetIds(ComponentName(app, AptoxTimeRestrictionWidgetProvider::class.java))
            if (ids.isEmpty()) return
            try {
                val views = buildViews(app)
                for (id in ids) mgr.updateAppWidget(id, views)
            } catch (t: Throwable) {
                Log.e(TAG, "updateAll failed", t)
                try {
                    val fallback = buildFallback(app)
                    for (id in ids) mgr.updateAppWidget(id, fallback)
                } catch (_: Throwable) {
                }
            }
        }

        fun buildViews(context: Context): RemoteViews {
            val app = context.applicationContext
            val views = RemoteViews(app.packageName, R.layout.widget_time_restriction_4x2)

            val pi = PendingIntent.getActivity(
                app, REQUEST_CODE,
                Intent(app, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_time_root, pi)

            val data = runCatching {
                runBlocking(Dispatchers.IO) { TimeRestrictionWidgetDataLoader.load(app) }
            }.getOrElse { TimeRestrictionWidgetDataLoader.empty() }

            bind(views, data)
            return views
        }

        private fun buildFallback(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_time_restriction_4x2)
            return try { bind(views, TimeRestrictionWidgetDataLoader.empty()); views } catch (_: Throwable) { views }
        }

        private fun bind(views: RemoteViews, data: TimeRestrictionWidgetData) {
            views.setTextViewText(R.id.widget_time_main, data.mainText)
            views.setProgressBar(R.id.widget_time_timeline, 100, data.timelineProgressPercent, false)

            val footerText = when (data.scheduleCount) {
                0 -> "등록된 스케줄 없음"
                else -> "${data.scheduleCount}개 스케줄 등록"
            }
            views.setTextViewText(R.id.widget_time_footer, footerText)
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
            Intent(context.applicationContext, AptoxTimeRestrictionWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
