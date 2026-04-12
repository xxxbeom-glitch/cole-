package com.aptox.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import com.aptox.app.R
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Figma 110×110 캔버스 수치를 [AppWidgetManager]가 주는 실제 위젯 dp에 맞춰 스케일한다.
 *
 * 스케일 기준: [AppWidgetSizeDp.heightDp] / canvas
 * - 2×2 위젯은 세로가 콘텐츠 배치의 제약 축이므로 height 기준으로 스케일한다.
 * - width 는 수평 패딩·여백에 의해 자동 조정된다.
 */
internal object DailyLimitWidgetAppearance {

    fun applyFigmaScale(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        views: RemoteViews,
        visibleRowCount: Int,
    ) {
        val res = context.resources
        val canvas = res.readDesignFloat(R.dimen.widget_daily_limit_design_canvas_dp).coerceAtLeast(1f)
        val scale = appWidgetManager.getWidgetSizeDp(context, appWidgetId).heightDp / canvas

        fun sd(id: Int): Float = res.readDesignFloat(id) * scale
        fun sdp(id: Int): Int = max(0, sd(id).toPx(res))

        val pad = sdp(R.dimen.widget_daily_limit_design_padding_dp)
        views.setViewPadding(R.id.widget_daily_root, pad, pad, pad, pad)

        val hHeader = sdp(R.dimen.widget_daily_limit_design_header_height_dp)
        val icon = sdp(R.dimen.widget_daily_limit_design_header_icon_dp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutHeight(R.id.widget_daily_header, hHeader.toFloat(), TypedValue.COMPLEX_UNIT_PX)
            views.setViewLayoutWidth(R.id.widget_daily_header_icon, icon.toFloat(), TypedValue.COMPLEX_UNIT_PX)
            views.setViewLayoutHeight(R.id.widget_daily_header_icon, icon.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        } else {
            views.setInt(R.id.widget_daily_header, "setMinimumHeight", hHeader)
            views.setInt(R.id.widget_daily_header_icon, "setMinimumWidth", icon)
            views.setInt(R.id.widget_daily_header_icon, "setMinimumHeight", icon)
            views.setInt(R.id.widget_daily_header_icon, "setMaxWidth", icon)
            views.setInt(R.id.widget_daily_header_icon, "setMaxHeight", icon)
        }

        views.setTextViewTextSize(
            R.id.widget_daily_headline,
            TypedValue.COMPLEX_UNIT_SP,
            max(1f, sd(R.dimen.widget_daily_limit_design_title_sp)),
        )

        val spacerHeader = sdp(R.dimen.widget_daily_limit_design_spacer_after_header_dp)
        setHeightPx(views, R.id.widget_daily_spacer_after_header, spacerHeader)

        val lineH = sdp(R.dimen.widget_daily_limit_design_row_text_line_height_dp)
        val textToBar = sdp(R.dimen.widget_daily_limit_design_row_text_to_bar_gap_dp)
        val barH = sdp(R.dimen.widget_daily_limit_design_progress_height_dp)
        val rowGap = sdp(R.dimen.widget_daily_limit_design_row_gap_dp)
        val rowTextSp = max(1f, sd(R.dimen.widget_daily_limit_design_row_text_sp))
        val ctaSp = max(1f, sd(R.dimen.widget_daily_limit_design_cta_text_sp))

        listOf(
            R.id.widget_daily_row_1_line,
            R.id.widget_daily_row_2_line,
            R.id.widget_daily_row_3_line,
        ).forEach { setHeightPx(views, it, lineH) }

        listOf(
            R.id.widget_daily_row_1_name,
            R.id.widget_daily_row_1_status,
            R.id.widget_daily_row_2_name,
            R.id.widget_daily_row_2_status,
            R.id.widget_daily_row_3_name,
            R.id.widget_daily_row_3_status,
        ).forEach {
            views.setTextViewTextSize(it, TypedValue.COMPLEX_UNIT_SP, rowTextSp)
        }

        views.setTextViewTextSize(R.id.widget_daily_cta_text, TypedValue.COMPLEX_UNIT_SP, ctaSp)

        listOf(
            R.id.widget_daily_row_1_progress,
            R.id.widget_daily_row_2_progress,
            R.id.widget_daily_row_3_progress,
        ).forEach {
            setBarMarginTop(views, it, textToBar)
            setHeightPx(views, it, barH)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutMargin(
                R.id.widget_daily_row_2,
                RemoteViews.MARGIN_TOP,
                rowGap.toFloat(),
                TypedValue.COMPLEX_UNIT_PX,
            )
            views.setViewLayoutMargin(
                R.id.widget_daily_row_3,
                RemoteViews.MARGIN_TOP,
                rowGap.toFloat(),
                TypedValue.COMPLEX_UNIT_PX,
            )
            // CTA 위 간격 = 행 간격과 동일
            views.setViewLayoutMargin(
                R.id.widget_daily_cta,
                RemoteViews.MARGIN_TOP,
                rowGap.toFloat(),
                TypedValue.COMPLEX_UNIT_PX,
            )
        }

        val balance = sdp(R.dimen.widget_daily_limit_design_balance_spacer_dp)
        setHeightPx(views, R.id.widget_daily_spacer_balance, balance)

        when (visibleRowCount) {
            3 -> {
                views.setInt(R.id.widget_daily_cta, "setMinimumHeight", 0)
            }
            2 -> {
                val m = sdp(R.dimen.widget_daily_limit_design_cta_min_height_2_dp)
                views.setInt(R.id.widget_daily_cta, "setMinimumHeight", m)
            }
            1 -> {
                val m = sdp(R.dimen.widget_daily_limit_design_cta_min_height_1_dp)
                views.setInt(R.id.widget_daily_cta, "setMinimumHeight", m)
            }
            else -> {
                views.setInt(R.id.widget_daily_cta, "setMinimumHeight", 0)
            }
        }
    }

    /**
     * API 31+ : [RemoteViews.setViewLayoutHeight] 로 레이아웃 height 를 픽셀로 고정.
     * API 30- : setMinimumHeight + setMaxHeight 를 함께 호출해 height 를 고정.
     *           setMaxHeight 만 있으면 wrap_content 가 더 커질 수 있고,
     *           setMinimumHeight 만 있으면 shrink 를 막지 못하므로 둘 다 필요.
     */
    private fun setHeightPx(views: RemoteViews, viewId: Int, px: Int) {
        if (px <= 0) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutHeight(viewId, px.toFloat(), TypedValue.COMPLEX_UNIT_PX)
        } else {
            views.setInt(viewId, "setMinimumHeight", px)
            views.setInt(viewId, "setMaxHeight", px)
        }
    }

    /**
     * 진행 바 위쪽 마진.
     * API 31+ : [RemoteViews.setViewLayoutMargin] 으로 동적 적용.
     * API 30- : RemoteViews 에 margin API 없음 → no-op.
     *           XML `android:layout_marginTop="4dp"` 고정값에 의존한다.
     */
    private fun setBarMarginTop(views: RemoteViews, progressId: Int, px: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setViewLayoutMargin(
                progressId,
                RemoteViews.MARGIN_TOP,
                px.toFloat(),
                TypedValue.COMPLEX_UNIT_PX,
            )
        }
        // API 30 이하: XML layout_marginTop="4dp" 그대로 유지 (변경 불가)
    }

    private fun Float.toPx(res: android.content.res.Resources): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, res.displayMetrics).roundToInt()
}
