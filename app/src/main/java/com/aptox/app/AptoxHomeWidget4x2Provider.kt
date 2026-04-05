package com.aptox.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * 홈 화면 **4×2** 위젯 (2행 목록 / 빈 상태). Figma 1551-4677 등.
 */
class AptoxHomeWidget4x2Provider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_aptox_home)
            AptoxHomeWidgetBinder.bindDailyWithSampleRows4x2(context, views)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}
