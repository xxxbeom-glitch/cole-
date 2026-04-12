package com.aptox.app.widget

import android.content.Context

/** 제한 저장/삭제 직후 두 홈 위젯을 모두 한 번 갱신합니다. */
object RestrictionsWidgetUpdateHelper {

    fun updateAll(context: Context) {
        AptoxDailyLimitWidgetProvider.updateAll(context)
        AptoxTimeRestrictionWidgetProvider.updateAll(context)
    }
}
