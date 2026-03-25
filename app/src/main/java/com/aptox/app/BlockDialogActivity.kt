package com.aptox.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

/**
 * 앱 차단 시 AlertDialog를 띄우는 투명 Activity.
 * 기존 SYSTEM_ALERT_WINDOW 오버레이를 대체.
 *
 * 케이스 1 (COUNT_NOT_STARTED): 사용 시간이 남아있음 - 카운트 시작 / 닫기
 * 케이스 2 (USAGE_EXCEEDED, blockUntilMs <= 0): 사용시간 전부 소진 - 닫기
 * 케이스 3 (blockUntilMs > 0): 시간 지정 차단 - 일시정지 / 홈으로 이동
 * 케이스 4 (MIDNIGHT_RESET): 00:00 자정에 카운트 세션이 종료됨 - 카운트 시작 / 닫기
 */
class BlockDialogActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DISMISS 액션: 제한 해제 시 해당 앱 다이얼로그 닫기
        if (intent?.action == ACTION_DISMISS_IF_PACKAGE) {
            val pkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (pkg != null && pkg == currentPackageName) {
                isRunning = false
                currentPackageName = null
                finish()
            }
            // 기존 인스턴스 없이 새로 시작된 경우 (닫을 다이얼로그 없음) 바로 종료
            else if (currentPackageName == null) {
                finish()
            }
            return
        }

        val packageName = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val blockUntilMs = intent?.getLongExtra(EXTRA_BLOCK_UNTIL_MS, 0L) ?: 0L
        val overlayState = intent?.getStringExtra(EXTRA_OVERLAY_STATE) ?: OVERLAY_STATE_USAGE_EXCEEDED
        val appName = intent?.getStringExtra(EXTRA_APP_NAME)
            ?: try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
            } catch (_: Exception) {
                packageName
            }

        currentPackageName = packageName
        isRunning = true

        when {
            blockUntilMs > 0 -> showTimeSpecifiedDialog(packageName, appName, blockUntilMs)
            overlayState == OVERLAY_STATE_COUNT_NOT_STARTED -> showCountNotStartedDialog(packageName, appName)
            overlayState == OVERLAY_STATE_MIDNIGHT_RESET -> showMidnightResetDialog(packageName, appName)
            else -> showUsageExceededDialog(packageName, appName)
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(newIntent)
        newIntent?.let { setIntent(it) }
        if (newIntent?.action == ACTION_DISMISS_IF_PACKAGE) {
            val pkg = newIntent.getStringExtra(EXTRA_PACKAGE_NAME)
            if (pkg != null && pkg == currentPackageName) {
                finish()
            }
        }
    }

    /** 케이스 1: 사용 시간이 남아있는 경우 - 카운트 시작 / 닫기 */
    private fun showCountNotStartedDialog(packageName: String, appName: String) {
        AlertDialog.Builder(this)
            .setTitle("앱을 사용하려면 카운트 시작 버튼을 눌러주세요")
            .setMessage("앱을 필요한 만큼 사용하신 후에는 반드시 카운트 중지를 눌러주셔야 해요")
            .setCancelable(false)
            .setPositiveButton("카운트 시작") { _, _ ->
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, packageName)
                }
                startActivity(i)
                goHomeAndFinish(skipHome = true)
            }
            .setNegativeButton("닫기") { _, _ ->
                goHomeAndFinish()
            }
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    goHomeAndFinish()
                    true
                } else false
            }
            .create()
            .also { it.setCanceledOnTouchOutside(false) }
            .show()
    }

    /** 케이스 4: 00:00 자정에 카운트 세션이 종료됨 - 카운트 시작 / 닫기 */
    private fun showMidnightResetDialog(packageName: String, appName: String) {
        AlertDialog.Builder(this)
            .setTitle("자정이 지나 카운트가 종료됐어요")
            .setMessage("새 날이 시작됐어요. 오늘도 $appName 사용을 시작하려면 카운트를 눌러주세요.")
            .setCancelable(false)
            .setPositiveButton("카운트 시작") { _, _ ->
                val i = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(AppMonitorService.EXTRA_OPEN_BOTTOM_SHEET, packageName)
                }
                startActivity(i)
                goHomeAndFinish(skipHome = true)
            }
            .setNegativeButton("닫기") { _, _ ->
                goHomeAndFinish()
            }
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    goHomeAndFinish()
                    true
                } else false
            }
            .create()
            .also { it.setCanceledOnTouchOutside(false) }
            .show()
    }

    /** 케이스 2: 사용시간 전부 소진 - 닫기 */
    private fun showUsageExceededDialog(packageName: String, appName: String) {
        BadgeAutoGrant.onBlockDefenseOverlayShown(applicationContext)

        AlertDialog.Builder(this)
            .setTitle("사용 시간을 모두 소진했어요")
            .setMessage("오늘 사용가능한 시간을 전부 사용하셨어요.")
            .setCancelable(false)
            .setPositiveButton("닫기") { _, _ ->
                goHomeAndFinish()
            }
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    goHomeAndFinish()
                    true
                } else false
            }
            .create()
            .also { it.setCanceledOnTouchOutside(false) }
            .show()
    }

    /** 케이스 3: 시간 지정 차단 - 닫기 */
    private fun showTimeSpecifiedDialog(packageName: String, appName: String, blockUntilMs: Long) {
        BadgeAutoGrant.onBlockDefenseOverlayShown(applicationContext)

        val remainingTimeText = formatBlockRemainingTime(blockUntilMs)
        val message = "${remainingTimeText} 후에 사용 가능해요"

        AlertDialog.Builder(this)
            .setTitle("지금은 사용하실 수 없어요")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("닫기") { _, _ ->
                goHomeAndFinish()
            }
            .setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                    goHomeAndFinish()
                    true
                } else false
            }
            .create()
            .also { it.setCanceledOnTouchOutside(false) }
            .show()
    }

    private fun goHomeAndFinish(skipHome: Boolean = false) {
        isRunning = false
        currentPackageName = null

        if (!skipHome) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }

        AppMonitorService.startAndClearForeground(this)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        currentPackageName = null
    }

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

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_BLOCK_UNTIL_MS = "block_until_ms"
        const val EXTRA_OVERLAY_STATE = "overlay_state"
        const val OVERLAY_STATE_USAGE_EXCEEDED = "USAGE_EXCEEDED"
        const val OVERLAY_STATE_COUNT_NOT_STARTED = "COUNT_NOT_STARTED"
        const val OVERLAY_STATE_MIDNIGHT_RESET = "MIDNIGHT_RESET"
        const val ACTION_PAUSE_FLOW_FROM_OVERLAY = "com.aptox.app.PAUSE_FLOW_FROM_OVERLAY"
        const val ACTION_DISMISS_IF_PACKAGE = "com.aptox.app.DISMISS_IF_PACKAGE"

        @JvmField
        var isRunning: Boolean = false

        private var currentPackageName: String? = null

        fun start(
            context: android.content.Context,
            packageName: String,
            appName: String,
            blockUntilMs: Long,
            overlayState: String,
        ) {
            val intent = Intent(context, BlockDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_APP_NAME, appName)
                putExtra(EXTRA_BLOCK_UNTIL_MS, blockUntilMs)
                putExtra(EXTRA_OVERLAY_STATE, overlayState)
            }
            context.startActivity(intent)
        }

        fun dismissIfPackage(context: android.content.Context, packageName: String) {
            val intent = Intent(context, BlockDialogActivity::class.java).apply {
                action = ACTION_DISMISS_IF_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }
            context.startActivity(intent)
        }
    }
}
