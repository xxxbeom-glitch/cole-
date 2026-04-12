package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import com.aptox.app.R
import kotlin.math.min

/**
 * 홈 런처가 [AppWidgetManager.getAppWidgetOptions] 로 전달하는 위젯 셀 크기.
 *
 * MIN_WIDTH/HEIGHT 는 "가장 좁을 때(가로 방향)" 기준이라 실제 할당 셀보다 작을 수 있다.
 * MAX_WIDTH/HEIGHT 는 "세로 방향에서 할당된 전체 셀" 크기이므로 스케일 기준으로 더 적합하다.
 *
 * [minSideDp] 는 width · height 모두 MAX 옵션으로 읽은 뒤 작은 쪽을 반환한다.
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
    var w = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
    var h = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    // MAX 옵션이 없을 때만 ProviderInfo.minWidth/minHeight 로 폴백
    // (AppWidgetProviderInfo 에는 maxWidth/maxHeight 필드가 없으므로 min 사용)
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
