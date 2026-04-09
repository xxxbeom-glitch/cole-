package com.aptox.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.aptox.app.BuildConfig
import com.aptox.app.MainActivity
import com.aptox.app.subscription.PremiumStatusRepository
import com.aptox.app.subscription.SubscriptionManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [ProcessLifecycleOwner] 기준 포그라운드 연속 시간이 [InterstitialAdConfig.FOREGROUND_CONTINUOUS_DISPLAY_INTERVAL_MS]에
 * 도달하면 전면 광고 1회 표시. 표시 후 타이머 리셋·다음 로드 후 동일 간격으로 반복.
 * 프리미엄([SubscriptionManager]) 사용자는 표시하지 않음.
 */
object ForegroundInterstitialAdController {

    private const val TAG = "ForegroundInterstitial"

    private val installed = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var appContext: Context? = null
    private var scope: CoroutineScope? = null

    @Volatile
    private var resumedActivity: WeakReference<MainActivity>? = null

    private var loadedAd: InterstitialAd? = null
    private var loadInFlight = false

    private val showAfterContinuousForeground = Runnable { tryShowLoadedAd() }

    fun install(application: Application) {
        if (!installed.compareAndSet(false, true)) return
        appContext = application.applicationContext
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (activity is MainActivity) {
                        resumedActivity = WeakReference(activity)
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    if (activity is MainActivity && resumedActivity?.get() === activity) {
                        resumedActivity = null
                    }
                }

                override fun onActivityCreated(a: Activity, b: android.os.Bundle?) {}
                override fun onActivityStarted(a: Activity) {}
                override fun onActivityStopped(a: Activity) {}
                override fun onActivitySaveInstanceState(a: Activity, b: android.os.Bundle) {}
                override fun onActivityDestroyed(a: Activity) {}
            },
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    onProcessForeground()
                }

                override fun onStop(owner: LifecycleOwner) {
                    onProcessBackground()
                }
            },
        )

        scope?.launch {
            PremiumStatusRepository.subscribedFlow(application).collectLatest {
                handler.post {
                    if (shouldSuppressAds(application)) {
                        cancelScheduledShowAndReleaseAd()
                    } else if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                        requestLoadIfNeeded()
                        scheduleShowAfterInterval()
                    }
                }
            }
        }
    }

    private fun onProcessForeground() {
        val ctx = appContext ?: return
        if (shouldSuppressAds(ctx)) {
            cancelScheduledShowAndReleaseAd()
            return
        }
        requestLoadIfNeeded()
        scheduleShowAfterInterval()
    }

    private fun onProcessBackground() {
        handler.removeCallbacks(showAfterContinuousForeground)
    }

    private fun scheduleShowAfterInterval() {
        handler.removeCallbacks(showAfterContinuousForeground)
        handler.postDelayed(
            showAfterContinuousForeground,
            InterstitialAdConfig.FOREGROUND_CONTINUOUS_DISPLAY_INTERVAL_MS,
        )
    }

    private fun shouldSuppressAds(context: Context): Boolean =
        SubscriptionManager.isSubscribed(context)

    private fun requestLoadIfNeeded() {
        val ctx = appContext ?: return
        if (shouldSuppressAds(ctx)) return
        if (loadedAd != null || loadInFlight) return
        loadInFlight = true
        InterstitialAd.load(
            ctx,
            BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadInFlight = false
                    if (shouldSuppressAds(ctx)) {
                        ad.fullScreenContentCallback = null
                        return
                    }
                    loadedAd = ad
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            loadedAd = null
                            Log.d(TAG, "전면 광고 닫힘 → 타이머 리셋 후 재스케줄")
                            val c = appContext ?: return
                            if (!shouldSuppressAds(c)) {
                                requestLoadIfNeeded()
                                scheduleShowAfterInterval()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.w(TAG, "전면 광고 표시 실패: ${adError.message}")
                            loadedAd = null
                            val c = appContext ?: return
                            if (!shouldSuppressAds(c)) {
                                requestLoadIfNeeded()
                                scheduleShowAfterInterval()
                            }
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadInFlight = false
                    Log.w(TAG, "전면 광고 로드 실패: ${error.message}")
                }
            },
        )
    }

    private fun tryShowLoadedAd() {
        val ctx = appContext ?: return
        if (shouldSuppressAds(ctx)) {
            cancelScheduledShowAndReleaseAd()
            return
        }
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }

        val activity = resumedActivity?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.d(TAG, "표시 가능한 MainActivity 없음 → 30초 후 재시도")
            scheduleShowAfterInterval()
            return
        }

        val ad = loadedAd
        if (ad == null) {
            Log.d(TAG, "로드된 전면 광고 없음 → 로드 요청 후 30초 후 재시도")
            requestLoadIfNeeded()
            scheduleShowAfterInterval()
            return
        }

        loadedAd = null
        try {
            ad.show(activity)
        } catch (e: Throwable) {
            Log.e(TAG, "ad.show 실패", e)
            requestLoadIfNeeded()
            scheduleShowAfterInterval()
        }
    }

    private fun cancelScheduledShowAndReleaseAd() {
        handler.removeCallbacks(showAfterContinuousForeground)
        loadedAd?.fullScreenContentCallback = null
        loadedAd = null
        loadInFlight = false
    }

}
