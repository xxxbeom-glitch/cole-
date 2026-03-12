package com.cole.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Figma AA-01 앱 차단 오버레이 (node 776-2776)
 * - 배경: Grey800 (#141414)
 * - 차단 아이콘 (휴대폰 금지 표시)
 * - "{App}은 사용제한 중이에요" + "제한 해제까지 남은 시간" + remainingTime (Red300)
 * - 일시정지 버튼 240×52dp, 12dp radius, 흰색 배경
 * - 닫기 버튼 텍스트 전용
 */
class BlockOverlayService : android.app.Service() {

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isDismissing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this, NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val blockUntilMs = intent?.getLongExtra(EXTRA_BLOCK_UNTIL_MS, 0L) ?: 0L
        isRunning = true
        if (overlayView == null) {
            showOverlay(packageName, blockUntilMs)
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "앱 차단",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { setShowBadge(false) }
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(this.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("앱 사용 제한 중")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun dp(dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()


    private fun formatBlockRemainingTime(blockUntilMs: Long): String {
        if (blockUntilMs <= 0) return "자정에 해제됩니다"
        val remainingMs = (blockUntilMs - System.currentTimeMillis()).coerceAtLeast(0)
        val totalMinutes = (remainingMs / 60_000).toInt()
        return when {
            totalMinutes >= 60 -> {
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                if (mins == 0) "${hours}시간" else "${hours}시간 ${mins}분"
            }
            totalMinutes > 0 -> "${totalMinutes}분"
            else -> "곧 해제"
        }
    }

    private fun showOverlay(packageName: String, blockUntilMs: Long) {
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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }

        val appName = try {
            (packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))).toString()
        } catch (e: Exception) {
            packageName
        }

        val pauseRepo = PauseRepository(this)
        val isPremium = false
        val pauseMinutes = if (isPremium) 10 else 5
        val maxCount = Int.MAX_VALUE // TODO: 테스트용 무제한, 배포 전 2로 변경
        val remainingCount = pauseRepo.getRemainingCount(packageName, maxCount)
        val remainingTimeText = formatBlockRemainingTime(blockUntilMs)

        // font XML에 fontVariationSettings('wght') 적용됨 - Variable 폰트 weight 반영
        val fontDisplay3 = resources.getFont(R.font.suit_display3)
        val fontCaption2 = resources.getFont(R.font.suit_caption2)
        val fontHeadingH3 = resources.getFont(R.font.suit_heading_h3)
        val fontButtonLarge = resources.getFont(R.font.suit_button_large)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_bg))
            setPadding(dp(48), dp(48), dp(48), dp(48))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    true
                } else false
            }
        }

        // 차단 아이콘 (Figma 776-2776)
        val blockIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_block_overlay)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(0, 0, 0, dp(32))
        }
        root.addView(blockIcon, LinearLayout.LayoutParams(dp(120), dp(120)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(26)
        })

        // Display3: 26sp, wght 700
        val titleText = TextView(this).apply {
            text = "${appName}은\n사용제한 중이에요"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontDisplay3)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(26))
        }
        root.addView(titleText)

        // Caption2: 13sp, wght 630
        val labelText = TextView(this).apply {
            text = "제한 해제까지 남은 시간"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontCaption2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(8))
        }
        root.addView(labelText)

        // HeadingH3: 18sp, wght 720
        val remainingText = TextView(this).apply {
            text = remainingTimeText
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_remaining))
            setTypeface(fontHeadingH3)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(88))
        }
        root.addView(remainingText)

        // Button/Large: 16sp, wght 630
        val pauseButton = TextView(this).apply {
            text = "${pauseMinutes}분 일시정지 사용 (${remainingCount}회 남음)"
            isEnabled = remainingCount > 0
            setBackgroundResource(
                if (remainingCount > 0) R.drawable.bg_block_overlay_button
                else R.drawable.bg_block_overlay_button_disabled
            )
            setTextColor(
                ContextCompat.getColor(
                    this@BlockOverlayService,
                    if (remainingCount > 0) R.color.block_overlay_btn_text else R.color.block_overlay_btn_text_disabled
                )
            )
            setTypeface(fontButtonLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            gravity = Gravity.CENTER
            setOnClickListener {
                if (remainingCount > 0) {
                    launchPauseFlow(packageName, appName, blockUntilMs)
                    dismiss(skipHome = true)
                }
            }
        }
        root.addView(pauseButton, LinearLayout.LayoutParams(dp(240), dp(52)).apply {
            topMargin = 0
            bottomMargin = dp(8)
        })

        // 닫기 버튼 — Button/Large
        val closeButton = TextView(this).apply {
            text = "닫기"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontButtonLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            gravity = Gravity.CENTER
            setOnClickListener { dismiss() }
        }
        root.addView(closeButton, LinearLayout.LayoutParams(dp(240), dp(52)))

        root.requestFocus()
        overlayView = root
        windowManager?.addView(root, layoutParams)
    }

    private fun launchPauseFlow(packageName: String, appName: String, blockUntilMs: Long) {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_PAUSE_FLOW_FROM_OVERLAY
            putExtra(EXTRA_PACKAGE_NAME, packageName)
            putExtra(EXTRA_APP_NAME, appName)
            putExtra(EXTRA_BLOCK_UNTIL_MS, blockUntilMs)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun dismiss(skipHome: Boolean = false) {
        if (isDismissing) return
        isDismissing = true
        isRunning = false

        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null

        if (!skipHome) {
            // 오버레이 제거 후 홈 화면으로 이동 (제한된 앱이 다시 포그라운드로 올라오는 것 방지)
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }

        // AppMonitorService에 포그라운드 앱 초기화 요청 (홈 이동 후에도 제한 앱이 포그라운드로 남아 재차단되는 것 방지)
        AppMonitorService.startAndClearForeground(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopSelf()
    }

    override fun onDestroy() {
        isDismissing = false
        isRunning = false
        overlayView?.let { view ->
            try { windowManager?.removeView(view) } catch (_: Exception) {}
        }
        overlayView = null
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        /** 시간 지정 차단 시 제한 해제 시각(ms). 0이면 일일 사용량 제한(자정 해제) */
        const val EXTRA_BLOCK_UNTIL_MS = "block_until_ms"
        /** 일시정지 3단계 플로우 시작용 Intent action (1단계 제안 → 2단계 확인 → 3단계 완료) */
        const val ACTION_PAUSE_FLOW_FROM_OVERLAY = "com.cole.app.PAUSE_FLOW_FROM_OVERLAY"
        /** AppMonitorService에서 중복 실행 방지용 플래그 */
        @JvmField
        var isRunning: Boolean = false

        private const val CHANNEL_ID = "block_overlay"
        private const val NOTIFICATION_ID = 1002
    }
}
