package com.cole.app

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockOverlayService : Service() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        isRunning = true
        if (overlayView == null) {
            showOverlay(packageName)
        }
        return START_NOT_STICKY
    }

    private fun showOverlay(packageName: String) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(230, 0, 0, 0))
        }

        val textView = TextView(this).apply {
            text = "사용 시간이 초과되었습니다"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(48, 48, 48, 24)
        }

        val pauseRepo = PauseRepository(this)
        // TODO: 유료 여부는 추후 구독 상태로 분기 (현재 무료 기준)
        val isPremium = false
        val pauseMinutes = if (isPremium) 10 else 5
        val maxCount = 2
        val remainingCount = pauseRepo.getRemainingCount(packageName, maxCount)

        val pauseButton = Button(this).apply {
            text = "일시정지 ${pauseMinutes}분 (${remainingCount}회 남음)"
            isEnabled = remainingCount > 0
            setOnClickListener {
                pauseRepo.startPause(packageName, pauseMinutes)
                dismiss()
            }
        }

        val closeButton = Button(this).apply {
            text = "닫기"
            setOnClickListener { dismiss() }
        }

        layout.addView(textView)
        layout.addView(pauseButton)
        layout.addView(closeButton)

        overlayView = layout
        windowManager?.addView(layout, layoutParams)
    }

    private fun dismiss() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        dismiss()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        /** AppMonitorService에서 중복 실행 방지용 플래그 */
        @JvmField
        var isRunning: Boolean = false
    }
}