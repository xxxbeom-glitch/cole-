package com.cole.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

/**
 * 앱 차단을 위한 접근성 서비스.
 * 설정 > 접근성 > 설치된 앱에서 활성화할 수 있으며,
 * 포그라운드 앱 변경 감지 등 앱 차단 기능 보조에 사용됩니다.
 * getWindows()로 PiP 등 화면 가시 윈도우를 감지해 AppVisibilityRepository에 저장.
 */
class ColeAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val pipCheckRunnable = object : Runnable {
        override fun run() {
            updateVisibleWindowsFromGetWindows()
            handler.postDelayed(this, PIP_CHECK_INTERVAL_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handler.post(pipCheckRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            updateVisibleWindowsFromGetWindows()
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        // 제한 앱 목록에 있고 차단 대상이면 BlockOverlayService 트리거 (UsageStats 보완)
        val repo = AppRestrictionRepository(this)
        val restriction = repo.getAll().find { it.packageName == pkg } ?: return

        val pauseRepo = PauseRepository(this)
        if (pauseRepo.isPaused(pkg)) return

        val shouldBlock = if (restriction.blockUntilMs > 0) {
            System.currentTimeMillis() < restriction.blockUntilMs
        } else {
            val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return
            val limitMs = restriction.limitMinutes * 60L * 1000L
            val visiblePkgs = AppVisibilityRepository(this).getPackagesWithVisibleWindows()
            val usageMs = UsageStatsUtils.getUsageSinceBaselineMs(usm, pkg, restriction.baselineTimeMs, visiblePkgs)
            usageMs >= limitMs
        }

        if (shouldBlock && !BlockOverlayService.isRunning) {
            val intent = Intent(this, BlockOverlayService::class.java).apply {
                putExtra(BlockOverlayService.EXTRA_PACKAGE_NAME, pkg)
                putExtra(BlockOverlayService.EXTRA_BLOCK_UNTIL_MS, restriction.blockUntilMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        handler.removeCallbacks(pipCheckRunnable)
        return super.onUnbind(intent)
    }

    /**
     * getWindows()로 화면에 보이는 제한 앱 윈도우(PiP 포함)를 감지해 저장.
     */
    private fun updateVisibleWindowsFromGetWindows() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val restrictedPackages = AppRestrictionRepository(this).getAll().mapTo(mutableSetOf()) { it.packageName }
        if (restrictedPackages.isEmpty()) {
            AppVisibilityRepository(this).setPackagesWithVisibleWindows(emptySet())
            return
        }
        val visibleRestricted = mutableSetOf<String>()
        try {
            windows?.forEach { window ->
                val root = window.root ?: return@forEach
                try {
                    root.packageName?.toString()?.let { pkg ->
                        if (pkg != packageName && restrictedPackages.contains(pkg) &&
                            window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                            visibleRestricted.add(pkg)
                        }
                    }
                } finally {
                    root.recycle()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "getWindows 실패", e)
        }
        AppVisibilityRepository(this).setPackagesWithVisibleWindows(visibleRestricted)
    }

    companion object {
        private const val TAG = "ColeAccessibility"
        private const val PIP_CHECK_INTERVAL_MS = 2000L
    }
}
