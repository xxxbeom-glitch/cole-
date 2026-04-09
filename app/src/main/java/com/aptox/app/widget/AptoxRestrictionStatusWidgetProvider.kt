package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 4×2 홈 화면 제한 앱 현황 위젯 (하루 사용량 + 시간 지정).
 */
class AptoxRestrictionStatusWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val app = context.applicationContext
            val mgr = AppWidgetManager.getInstance(app)
            val cn = android.content.ComponentName(app, AptoxRestrictionStatusWidgetProvider::class.java)
            val ids = mgr.getAppWidgetIds(cn)
            if (ids.isNotEmpty()) {
                try {
                    onUpdate(app, mgr, ids)
                } catch (t: Throwable) {
                    Log.e(TAG, "onReceive ACTION_REFRESH onUpdate failed", t)
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
            val views = AptoxRestrictionWidgetBinder.buildUpdate(app)
            for (id in appWidgetIds) {
                appWidgetManager.updateAppWidget(id, views)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onUpdate failed, applying per-widget fallback", t)
            val fallback = AptoxRestrictionWidgetBinder.buildFallbackRemoteViews(app)
            for (id in appWidgetIds) {
                try {
                    appWidgetManager.updateAppWidget(id, fallback)
                } catch (inner: Throwable) {
                    Log.e(TAG, "updateAppWidget failed appWidgetId=$id", inner)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        try {
            RestrictionsWidgetRefreshScheduler.schedule(context.applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "onEnabled schedule failed", t)
        }
    }

    override fun onDisabled(context: Context) {
        try {
            RestrictionsWidgetRefreshScheduler.cancel(context.applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "onDisabled cancel failed", t)
        }
        super.onDisabled(context)
    }

    companion object {
        private const val TAG = "AptoxRestrictionWidget"
        const val ACTION_REFRESH = "com.aptox.app.action.WIDGET_REFRESH_RESTRICTIONS"
    }
}
