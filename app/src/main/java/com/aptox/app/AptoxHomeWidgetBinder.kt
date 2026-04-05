package com.aptox.app

import android.content.Context
import android.view.View
import android.widget.RemoteViews

/**
 * 홈 위젯 RemoteViews 바인딩 (4×2 / 4×1 레이아웃별 id 분리).
 * 데이터 연동 전 플레이스홀더 — 이후 동일 API로 실제 제한 목록 주입.
 */
object AptoxHomeWidgetBinder {

    // --- 4×2 [widget_aptox_home] ---

    fun bindDailyWithSampleRows4x2(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setTextViewText(R.id.widget_row1_name, "Youtube")
        views.setTextViewText(R.id.widget_row1_time, "00:26:33")
        views.setTextViewText(R.id.widget_row2_name, "업비트")
        views.setTextViewText(R.id.widget_row2_time, "00:15:33")
    }

    fun bindDailyEmpty4x2(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.GONE)
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty_message))
    }

    fun bindTimeWithSampleRows4x2(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_time_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.VISIBLE)
        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setTextViewText(R.id.widget_row1_name, "Youtube")
        views.setTextViewText(R.id.widget_row1_time, "00:26:33")
        views.setTextViewText(R.id.widget_row2_name, "업비트")
        views.setTextViewText(R.id.widget_row2_time, "00:15:33")
    }

    fun bindTimeEmpty4x2(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title_time_limit))
        views.setViewVisibility(R.id.widget_rows_container, View.GONE)
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, context.getString(R.string.widget_empty_message))
    }

    // --- 4×1 [widget_aptox_home_4x1] 한 줄 요약 ---

    fun bindDailyWithSampleRows4x1(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_4x1_headline, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_4x1_filled, View.VISIBLE)
        views.setViewVisibility(R.id.widget_4x1_empty, View.GONE)
        views.setTextViewText(R.id.widget_4x1_app_name, "Youtube")
        views.setTextViewText(R.id.widget_4x1_time, "00:26:33")
    }

    fun bindDailyEmpty4x1(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_4x1_headline, context.getString(R.string.widget_title_daily_limit))
        views.setViewVisibility(R.id.widget_4x1_filled, View.GONE)
        views.setViewVisibility(R.id.widget_4x1_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_4x1_empty, context.getString(R.string.widget_empty_message))
    }

    fun bindTimeWithSampleRows4x1(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_4x1_headline, context.getString(R.string.widget_title_time_limit))
        views.setViewVisibility(R.id.widget_4x1_filled, View.VISIBLE)
        views.setViewVisibility(R.id.widget_4x1_empty, View.GONE)
        views.setTextViewText(R.id.widget_4x1_app_name, "Youtube")
        views.setTextViewText(R.id.widget_4x1_time, "00:26:33")
    }

    fun bindTimeEmpty4x1(context: Context, views: RemoteViews) {
        views.setTextViewText(R.id.widget_4x1_headline, context.getString(R.string.widget_title_time_limit))
        views.setViewVisibility(R.id.widget_4x1_filled, View.GONE)
        views.setViewVisibility(R.id.widget_4x1_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_4x1_empty, context.getString(R.string.widget_empty_message))
    }
}
