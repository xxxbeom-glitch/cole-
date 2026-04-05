package com.aptox.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * 홈 화면 **4×1** 위젯 (한 줄: 제목 + 첫 앱·남은 시간 / 빈 상태).
 */
class AptoxHomeWidget4x1Provider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_aptox_home_4x1)
            AptoxHomeWidgetBinder.bindDailyWithSampleRows4x1(context, views)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
