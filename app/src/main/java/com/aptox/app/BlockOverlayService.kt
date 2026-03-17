package com.aptox.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.PixelFormat
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * 앱 차단 오버레이 (Figma AA-01 776-2776, 1136-6361)
 * - blockUntilMs <= 0: 일일사용량 제한 — Grey850, "오늘 사용가능한 시간을 전부 사용하셨어요", 닫기
 * - blockUntilMs > 0: 시간지정 제한 — Grey850, "지금은 {App} 을 사용하실 수 없어요", 일시정지 + 홈으로 이동
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
        val overlayState = intent?.getStringExtra(EXTRA_OVERLAY_STATE) ?: OVERLAY_STATE_USAGE_EXCEEDED
        isRunning = true
        if (overlayView == null) {
            showOverlay(packageName, blockUntilMs, overlayState)
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

    private fun showOverlay(packageName: String, blockUntilMs: Long, overlayState: String) {
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

        val root = when {
            blockUntilMs > 0 -> buildTimeSpecifiedOverlay(packageName, appName, blockUntilMs)
            overlayState == OVERLAY_STATE_COUNT_NOT_STARTED -> buildCountNotStartedOverlay(packageName, appName)
            else -> buildDailyUsageOverlay(packageName, appName)
        }
        root.requestFocus()
        overlayView = root
        windowManager?.addView(root, layoutParams)
    }

    /** Figma 1159-4638: 카운트 미시작 — "앱을 사용하시려면 카운트 시작을 눌러주세요" + 카운트 시작 버튼 */
    private fun buildCountNotStartedOverlay(packageName: String, appName: String): View {
        val fontHeadingH2 = resources.getFont(R.font.suit_heading_h3)
        val fontBodyMedium = resources.getFont(R.font.suit_button_large)
        val fontButtonLarge = resources.getFont(R.font.suit_button_large)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_bg))
            setPadding(dp(48), dp(48), dp(48), dp(48))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    true
                } else false
            }
        }

        val titleText = TextView(this).apply {
            text = "앱을 사용하시려면\n카운트 시작을 눌러주세요"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontHeadingH2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(titleText)

        val subText = TextView(this).apply {
            text = "앱을 필요한 만큼 사용하신 후에는\n반드시 카운트 종료를 눌러주셔야 해요"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontBodyMedium)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(26))
        }
        root.addView(subText)

        val startButton = TextView(this).apply {
            text = "카운트 시작"
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_btn_bg))
                cornerRadius = dp(12).toFloat()
            }
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_btn_text))
            setTypeface(fontButtonLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            gravity = Gravity.CENTER
            setOnClickListener {
                val intent = Intent(this@BlockOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, packageName)
                }
                startActivity(intent)
                dismiss(skipHome = true)
            }
        }
        val btnParams = LinearLayout.LayoutParams(dp(246), dp(60)).apply {
            topMargin = dp(26)
        }
        btnParams.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(startButton, btnParams)
        return root
    }

    /** Figma 1136-6361: 일일사용량 제한 — "오늘 사용가능한 시간을 전부 사용하셨어요" + 닫기 */
    private fun buildDailyUsageOverlay(packageName: String, appName: String): View {
        val fontHeadingH2 = resources.getFont(R.font.suit_heading_h3)
        val fontButtonLarge = resources.getFont(R.font.suit_button_large)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_bg))
            setPadding(dp(48), dp(48), dp(48), dp(48))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    true
                } else false
            }
        }

        val titleText = TextView(this).apply {
            text = "오늘 사용가능한 시간을\n전부 사용하셨어요"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontHeadingH2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(26))
        }
        root.addView(titleText)

        val closeButton = TextView(this).apply {
            text = "닫기"
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_btn_bg))
                cornerRadius = dp(12).toFloat()
            }
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_btn_text))
            setTypeface(fontButtonLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            gravity = Gravity.CENTER
            setOnClickListener { dismiss() }
        }
        val closeParams = LinearLayout.LayoutParams(dp(246), dp(60)).apply {
            topMargin = dp(26)
        }
        closeParams.gravity = Gravity.CENTER_HORIZONTAL
        root.addView(closeButton, closeParams)
        return root
    }

    /** Figma AA-01 (776-2776): 시간 지정 차단 — 일일 오버레이와 동일 스타일, 아이콘 없음 */
    private fun buildTimeSpecifiedOverlay(packageName: String, appName: String, blockUntilMs: Long): View {
        val pauseRepo = PauseRepository(this)
        val isPremium = false
        val pauseMinutes = if (isPremium) 10 else 5
        val maxCount = Int.MAX_VALUE // TODO: 테스트용 무제한, 배포 전 2로 변경
        val remainingCount = pauseRepo.getRemainingCount(packageName, maxCount)
        val remainingTimeText = formatBlockRemainingTime(blockUntilMs)

        val fontHeadingH3 = resources.getFont(R.font.suit_heading_h3)
        val fontBodyMedium = resources.getFont(R.font.suit_button_large) // BodyMedium 대체
        val fontButtonLarge = resources.getFont(R.font.suit_button_large)
        val redColor = ContextCompat.getColor(this, R.color.block_overlay_text_remaining)
        val bodyText = "${remainingTimeText} 뒤에는 제한 해제가 되니\n조금만 더 참아봐요"
        val bodySpannable = SpannableStringBuilder(bodyText).apply {
            setSpan(
                ForegroundColorSpan(redColor),
                0, remainingTimeText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_bg))
            setPadding(dp(48), dp(48), dp(48), dp(48))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss()
                    true
                } else false
            }
        }

        val titleText = TextView(this).apply {
            text = "지금은 ${appName} 을\n사용하실 수 없어요"
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontHeadingH3)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(titleText)

        val bodyTextView = TextView(this).apply {
            text = bodySpannable
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_text_primary))
            setTypeface(fontBodyMedium)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            setPadding(0, 0, 0, dp(26))
        }
        root.addView(bodyTextView)

        val pauseButton = TextView(this).apply {
            text = "${pauseMinutes}분 일시정지 사용 (${remainingCount}회 남음)"
            isEnabled = remainingCount > 0
            background = if (remainingCount > 0) {
                GradientDrawable().apply {
                    setColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_daily_btn_bg))
                    cornerRadius = dp(12).toFloat()
                }
            } else {
                GradientDrawable().apply {
                    setColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_btn_text_disabled))
                    cornerRadius = dp(12).toFloat()
                }
            }
            setTextColor(
                ContextCompat.getColor(
                    this@BlockOverlayService,
                    if (remainingCount > 0) R.color.block_overlay_daily_btn_text else R.color.block_overlay_btn_text_disabled
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
        root.addView(pauseButton, LinearLayout.LayoutParams(dp(246), dp(60)).apply {
            topMargin = 0
            bottomMargin = dp(12)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        val closeButton = TextView(this).apply {
            text = "홈으로 이동"
            background = GradientDrawable().apply {
                setColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_ghost_bg))
                cornerRadius = dp(12).toFloat()
            }
            setTextColor(ContextCompat.getColor(this@BlockOverlayService, R.color.block_overlay_ghost_text))
            setTypeface(fontButtonLarge)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
            gravity = Gravity.CENTER
            setOnClickListener { dismiss() }
        }
        root.addView(closeButton, LinearLayout.LayoutParams(dp(246), dp(60)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })

        return root
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
        /** 일일 사용량 오버레이 상태: USAGE_EXCEEDED | COUNT_NOT_STARTED */
        const val EXTRA_OVERLAY_STATE = "overlay_state"
        const val OVERLAY_STATE_USAGE_EXCEEDED = "USAGE_EXCEEDED"
        const val OVERLAY_STATE_COUNT_NOT_STARTED = "COUNT_NOT_STARTED"
        /** 일시정지 3단계 플로우 시작용 Intent action (1단계 제안 → 2단계 확인 → 3단계 완료) */
        const val ACTION_PAUSE_FLOW_FROM_OVERLAY = "com.aptox.app.PAUSE_FLOW_FROM_OVERLAY"
        /** AppMonitorService에서 중복 실행 방지용 플래그 */
        @JvmField
        var isRunning: Boolean = false

        private const val CHANNEL_ID = "block_overlay"
        private const val NOTIFICATION_ID = 1002
    }
}
