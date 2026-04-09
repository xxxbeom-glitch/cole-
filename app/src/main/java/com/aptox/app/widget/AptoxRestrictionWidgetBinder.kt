package com.aptox.app.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.aptox.app.MainActivity
import com.aptox.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object AptoxRestrictionWidgetBinder {

    private const val TAG = "AptoxWidgetBinder"

    fun buildUpdate(context: Context): RemoteViews {
        val app = context.applicationContext
        return try {
            buildUpdateInternal(app)
        } catch (t: Throwable) {
            Log.e(TAG, "buildUpdate failed, fallback", t)
            buildFallbackRemoteViews(app)
        }
    }

    /**
     * 최소한의 RemoteViews (클릭 인텐트 + 기본값 UI). 어떤 단계에서도 예외가 나면 그대로 반환해
     * [updateAppWidget] 이 호출되도록 한다.
     */
    fun buildFallbackRemoteViews(context: Context): RemoteViews {
        val app = context.applicationContext
        val views = RemoteViews(app.packageName, R.layout.widget_restrictions_4x2)
        return try {
            attachClickIntent(views, app)
            bindMainContent(views, sanitize(RestrictionWidgetDataLoader.emptySummary()))
            views
        } catch (t: Throwable) {
            Log.e(TAG, "buildFallbackRemoteViews failed", t)
            views
        }
    }

    private fun buildUpdateInternal(app: Context): RemoteViews {
        val views = RemoteViews(app.packageName, R.layout.widget_restrictions_4x2)
        attachClickIntent(views, app)

        val loadResult = runCatching {
            runBlocking(Dispatchers.IO) {
                RestrictionWidgetDataLoader.loadSummary(app)
            }
        }
        val summary = sanitize(loadResult.getOrElse {
            Log.w(TAG, "loadSummary failed, using emptySummary", it)
            RestrictionWidgetDataLoader.emptySummary()
        })
        val loadSucceeded = loadResult.isSuccess

        val showEmptyState =
            loadSucceeded &&
                summary.timeSpecifiedTotal == 0 &&
                summary.dailyUsageTotal == 0

        if (showEmptyState) {
            views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
            views.setViewVisibility(R.id.widget_content, View.GONE)
            return views
        }

        views.setViewVisibility(R.id.widget_empty, View.GONE)
        views.setViewVisibility(R.id.widget_content, View.VISIBLE)
        bindMainContent(views, summary)
        return views
    }

    private fun sanitize(s: WidgetSummaryData): WidgetSummaryData {
        fun c0(n: Int) = n.coerceAtLeast(0)
        fun c100(n: Int) = n.coerceIn(0, 100)
        val tTotal = c0(s.timeSpecifiedTotal)
        val tActive = if (tTotal > 0) c0(s.timeSpecifiedActive).coerceAtMost(tTotal) else c0(s.timeSpecifiedActive)
        val tPct = if (tTotal > 0) c100(tActive * 100 / tTotal) else 0
        val dLimit = c0(s.dailyLimitTotalMinutes)
        val dUsage = c0(s.dailyUsageTotalMinutes)
        return s.copy(
            timeSpecifiedTotal = tTotal,
            timeSpecifiedActive = tActive,
            timeSpecifiedProgressPercent = tPct,
            dailyUsageTotal = c0(s.dailyUsageTotal),
            dailyUsageTotalMinutes = dUsage,
            dailyLimitTotalMinutes = dLimit,
            dailyUsageProgressPercent = c100(s.dailyUsageProgressPercent),
            dailyIsExceeded = dLimit > 0 && dUsage > dLimit,
        )
    }

    private fun attachClickIntent(views: RemoteViews, app: Context) {
        val clickIntent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            app,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root_4x2, pendingIntent)
    }

    private fun bindMainContent(views: RemoteViews, summary: WidgetSummaryData) {
        val timeMainText = if (summary.timeSpecifiedTotal == 0) {
            "0개"
        } else {
            "${summary.timeSpecifiedActive}개 제한 중"
        }
        views.setTextViewText(R.id.count_time_spec, timeMainText)

        val timeSubText = if (summary.timeSpecifiedTotal == 0) {
            "등록 없음"
        } else {
            "총 ${summary.timeSpecifiedTotal}개 등록"
        }
        views.setTextViewText(R.id.sub_time_spec, timeSubText)

        views.setProgressBar(
            R.id.progress_time_spec,
            100,
            summary.timeSpecifiedProgressPercent.coerceIn(0, 100),
            false,
        )

        views.setTextViewText(R.id.count_daily, "${summary.dailyUsageTotalMinutes}분 사용")

        val dailySubText = when {
            summary.dailyUsageTotal == 0 -> "등록 없음"
            summary.dailyLimitTotalMinutes == 0 -> "한도 없음"
            else -> "한도 ${summary.dailyLimitTotalMinutes}분"
        }
        views.setTextViewText(R.id.sub_daily, dailySubText)

        val showProgress = summary.dailyLimitTotalMinutes > 0
        if (showProgress) {
            if (summary.dailyIsExceeded) {
                views.setViewVisibility(R.id.progress_daily_normal, View.GONE)
                views.setViewVisibility(R.id.progress_daily_exceeded, View.VISIBLE)
                views.setProgressBar(R.id.progress_daily_exceeded, 100, 100, false)
            } else {
                views.setViewVisibility(R.id.progress_daily_normal, View.VISIBLE)
                views.setViewVisibility(R.id.progress_daily_exceeded, View.GONE)
                views.setProgressBar(
                    R.id.progress_daily_normal,
                    100,
                    summary.dailyUsageProgressPercent.coerceIn(0, 100),
                    false,
                )
            }
        } else {
            views.setViewVisibility(R.id.progress_daily_normal, View.GONE)
            views.setViewVisibility(R.id.progress_daily_exceeded, View.GONE)
        }
    }
}
