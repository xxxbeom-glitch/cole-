package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.aptox.app.R
import kotlin.math.min

/**
 * Glance [androidx.glance.appwidget] 위젯도 내부적으로 홈 런처가 넘기는
 * [AppWidgetManager.getAppWidgetOptions] 기반 크기를 쓴다.
 * 2×2 셀의 실제 dp는 기기·런처마다 달라지므로, 여기서 측정한 min(width,height)을
 * Figma 캔버스 dp와 비율 스케일하는 데 쓴다.
 */
data class AppWidgetSizeDp(
    val widthDp: Float,
    val heightDp: Float,
) {
    fun minSideDp(): Float = min(widthDp, heightDp)
}

fun AppWidgetManager.getWidgetSizeDp(
    context: Context,
    appWidgetId: Int,
): AppWidgetSizeDp {
    val opts = getAppWidgetOptions(appWidgetId)
    var w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
    var h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
    if (w <= 0 || h <= 0) {
        runCatching { getAppWidgetInfo(appWidgetId) }.getOrNull()?.let { info ->
            if (w <= 0) w = info.minWidth
            if (h <= 0) h = info.minHeight
        }
    }
    val res = context.resources
    val fallback = (res.getDimension(R.dimen.widget_daily_limit_fallback_min_side_dp) /
        res.displayMetrics.density).toInt()
    if (w <= 0) w = fallback
    if (h <= 0) h = fallback
    return AppWidgetSizeDp(w.toFloat(), h.toFloat())
}
