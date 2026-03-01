package com.cole.app

import android.app.Application
import android.util.Log
import com.kakao.sdk.common.KakaoSdk

class ColeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            KakaoSdk.init(this, "c2b30a1e78b6936a1603d5d18fd5ea20")
        } catch (e: Throwable) {
            Log.e(TAG, "KakaoSdk init 실패", e)
        }
        try {
            startAppMonitorIfNeeded()
        } catch (e: Throwable) {
            Log.e(TAG, "AppMonitor 시작 실패", e)
        }
    }

    private fun startAppMonitorIfNeeded() {
        val repo = AppRestrictionRepository(this)
        val map = repo.toRestrictionMap()
        if (map.isNotEmpty()) {
            AppMonitorService.start(this, map)
        }
    }

    companion object {
        private const val TAG = "ColeApplication"
    }
}
