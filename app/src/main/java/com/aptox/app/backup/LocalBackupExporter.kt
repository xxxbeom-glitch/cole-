package com.aptox.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import com.aptox.app.AppRestrictionRepository
import com.aptox.app.BadgeStatsPreferences
import com.aptox.app.BuildConfig
import com.aptox.app.NotificationPreferences
import com.aptox.app.PendingBadgesPreferences
import com.aptox.app.BadgeRepository
import com.aptox.app.usage.AppDatabaseProvider
import com.aptox.app.usage.DailyCategoryStatEntity
import com.aptox.app.usage.DailyTimeSegmentEntity
import com.aptox.app.usage.DailyUsageEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 로컬 ZIP 백업 생성·공유 ([LocalBackupImporter]와 동일 스키마).
 */
object LocalBackupExporter {

    private const val CACHE_SUBDIR = "aptox_backup_exports"

    fun fileProviderAuthority(context: Context): String =
        "${context.packageName}.crashlogfileprovider"

    suspend fun exportToZipFile(context: Context): File = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val dir = File(app.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val zipFile = File(dir, "aptox_backup_$stamp.zip")
        val userBadgesCsv = buildUserBadgesCsvSuspend(app)
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            writeEntry(zos, LocalBackupFormat.ENTRY_MANIFEST, buildManifestJson())
            writeEntry(zos, LocalBackupFormat.ENTRY_RESTRICTIONS, buildRestrictionsCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_DAILY_USAGE, buildDailyUsageCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_CATEGORY_DAILY, buildCategoryCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_TIME_SEGMENTS, buildTimeSegmentCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_NOTIFICATION_PREFS, buildNotificationCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_BADGE_STATS, buildBadgeStatsCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_PENDING_BADGES, buildPendingBadgesCsv(app))
            writeEntry(zos, LocalBackupFormat.ENTRY_USER_BADGES, userBadgesCsv)
        }
        zipFile
    }

    /**
     * [Intent.ACTION_CREATE_DOCUMENT] — Files 앱 등 SAF로 저장 위치 선택.
     * API 29+ 에서 [MediaStore.Downloads]를 초기 위치로 힌트합니다.
     */
    fun createSaveBackupZipIntent(suggestedFileName: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, suggestedFileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, MediaStore.Downloads.EXTERNAL_CONTENT_URI)
            }
        }

    /** SAF로 받은 [destinationUri]에 ZIP 바이트를 복사합니다. */
    fun copyZipFileToUri(context: Context, sourceZip: File, destinationUri: Uri): Boolean =
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                FileInputStream(sourceZip).use { input -> input.copyTo(out) }
            } != null
        } catch (_: Exception) {
            false
        }

    fun shareZip(activity: ComponentActivity, zipFile: File): Boolean = try {
        val uri = FileProvider.getUriForFile(activity, fileProviderAuthority(activity), zipFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Aptox 데이터 백업")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, "백업 파일 보내기"))
        true
    } catch (_: Exception) {
        false
    }

    private fun writeEntry(zos: ZipOutputStream, name: String, utf8Body: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(utf8Body.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildManifestJson(): String =
        JSONObject().apply {
            put("formatVersion", LocalBackupFormat.FORMAT_VERSION)
            put("exportAppVersion", BuildConfig.VERSION_NAME)
            put("exportedAt", System.currentTimeMillis())
        }.toString()

    private fun buildRestrictionsCsv(ctx: Context): String {
        val list = AppRestrictionRepository(ctx).getAll()
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_RESTRICTIONS))
        for (r in list) {
            lines.add(
                LocalBackupCsv.formatRow(
                    listOf(
                        r.packageName,
                        r.appName,
                        r.limitMinutes.toString(),
                        r.blockUntilMs.toString(),
                        r.baselineTimeMs.toString(),
                        r.repeatDays,
                        r.durationWeeks.toString(),
                        r.startTimeMs.toString(),
                    ),
                ),
            )
        }
        return lines.joinToString("\n")
    }

    private fun buildDailyUsageCsv(ctx: Context): String {
        val rows = AppDatabaseProvider.get(ctx).getAllDailyUsageRows()
        return buildDailyUsageCsvFromRows(rows)
    }

    internal fun buildDailyUsageCsvFromRows(rows: List<DailyUsageEntity>): String {
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_DAILY_USAGE))
        for (e in rows) {
            lines.add(
                LocalBackupCsv.formatRow(
                    listOf(e.date, e.packageName, e.usageMs.toString(), e.sessionCount.toString()),
                ),
            )
        }
        return lines.joinToString("\n")
    }

    private fun buildCategoryCsv(ctx: Context): String {
        val rows = AppDatabaseProvider.get(ctx).getAllCategoryDailyRows()
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_CATEGORY_DAILY))
        for (e in rows) {
            lines.add(LocalBackupCsv.formatRow(listOf(e.date, e.category, e.usageMs.toString())))
        }
        return lines.joinToString("\n")
    }

    private fun buildTimeSegmentCsv(ctx: Context): String {
        val rows = AppDatabaseProvider.get(ctx).getAllTimeSegmentRows()
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_TIME_SEGMENT))
        for (e in rows) {
            val fields = mutableListOf(e.date, e.packageName)
            for (i in 0 until DailyTimeSegmentEntity.TIME_SEGMENT_SLOT_COUNT) {
                fields.add(e.slotMs[i].toString())
            }
            lines.add(LocalBackupCsv.formatRow(fields))
        }
        return lines.joinToString("\n")
    }

    private fun buildNotificationCsv(ctx: Context): String {
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_NOTIFICATION_PREFS))
        for ((k, v) in NotificationPreferences.exportKeyValuePairs(ctx)) {
            lines.add(LocalBackupCsv.formatRow(listOf(k, v)))
        }
        return lines.joinToString("\n")
    }

    private fun buildBadgeStatsCsv(ctx: Context): String {
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_BADGE_STATS))
        for ((k, v) in BadgeStatsPreferences.exportEntriesForBackup(ctx)) {
            lines.add(LocalBackupCsv.formatRow(listOf(k, v)))
        }
        return lines.joinToString("\n")
    }

    private fun buildPendingBadgesCsv(ctx: Context): String {
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_PENDING_BADGES))
        for (id in PendingBadgesPreferences.getPendingBadges(ctx)) {
            lines.add(LocalBackupCsv.formatRow(listOf(id)))
        }
        return lines.joinToString("\n")
    }

    private suspend fun buildUserBadgesCsvSuspend(ctx: Context): String {
        val lines = mutableListOf<String>()
        lines.add(LocalBackupCsv.formatRow(LocalBackupFormat.HEADER_USER_BADGES))
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val repo = BadgeRepository(context = null)
            val earned = repo.getAllEarnedBadges(uid)
            for ((badgeId, at) in earned.entries.sortedBy { it.key }) {
                lines.add(LocalBackupCsv.formatRow(listOf(badgeId, at.toString())))
            }
        }
        return lines.joinToString("\n")
    }
}
