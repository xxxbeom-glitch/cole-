package com.aptox.app

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
/**
 * 버그 신고 이미지 업로드.
 * 경로: bug-reports/{userId}/{timestamp}/image_{1~n}.jpg
 */
object BugReportRepository {

    private const val TAG = "BugReportRepository"
    private val storage by lazy { FirebaseStorage.getInstance() }
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024L // 2MB
    private val ALLOWED_MIME = setOf("image/jpeg", "image/jpg", "image/png")
    private const val DOWNLOAD_URL_RETRY_DELAY_MS = 800L

    /**
     * @return 업로드된 이미지 URL 목록. 실패 시 exception.
     */
    suspend fun uploadImages(context: Context, uris: List<Uri>): List<String> {
        if (uris.isEmpty()) {
            Log.d(TAG, "uploadImages: uris 비어 있음, 스킵")
            return emptyList()
        }
        Log.d(TAG, "uploadImages: 시작, uris.size=${uris.size}")
        // Content URI는 메인 스레드에서 먼저 읽어 임시 파일로 저장 (권한 만료 방지)
        val tempFiles = withContext(Dispatchers.Main) {
            uris.mapIndexed { index, uri ->
                val tempFile = File(context.cacheDir, "bug_report_pre_${System.currentTimeMillis()}_$index.jpg")
                try {
                    Log.d(TAG, "uploadImages: [${index + 1}/${uris.size}] URI 읽기 시작 uri=$uri")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    } ?: throw RuntimeException("이미지를 읽을 수 없어요. 이미지를 다시 선택해주세요.")
                    Log.d(TAG, "uploadImages: [${index + 1}/${uris.size}] URI→파일 복사 완료 size=${tempFile.length()}")
                    tempFile
                } catch (e: Exception) {
                    Log.e(TAG, "uploadImages: [${index + 1}/${uris.size}] URI 읽기 실패", e)
                    throw e
                }
            }
        }
        return withContext(Dispatchers.IO) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
            val timestamp = System.currentTimeMillis()
            val ref = storage.reference.child("bug-reports").child(userId).child("$timestamp")
            Log.d(TAG, "uploadImages: Storage 경로 bug-reports/$userId/$timestamp")
            tempFiles.mapIndexed { index, tempFile ->
                try {
                    val fileName = "image_${index + 1}.jpg"
                    val childRef = ref.child(fileName)
                    Log.d(TAG, "uploadImages: [${index + 1}/${tempFiles.size}] putStream 시작")
                    tempFile.inputStream().use { stream ->
                        childRef.putStream(stream).await()
                    }
                    Log.d(TAG, "uploadImages: [${index + 1}/${tempFiles.size}] putStream 완료, downloadUrl 대기")
                    delay(DOWNLOAD_URL_RETRY_DELAY_MS)
                    val url = childRef.downloadUrl.await().toString()
                    Log.d(TAG, "uploadImages: [${index + 1}/${tempFiles.size}] downloadUrl 완료")
                    url
                } catch (e: Exception) {
                    Log.e(TAG, "uploadImages: [${index + 1}/${tempFiles.size}] Storage 업로드/다운로드URL 실패", e)
                    throw e
                } finally {
                    tempFile.delete()
                }
            }
        }
    }

    fun validateImageUri(context: Context, uri: Uri): ValidationResult {
        val mime = context.contentResolver.getType(uri)?.lowercase()
        val path = uri.toString().lowercase()
        val validMime = mime in ALLOWED_MIME || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png")
        if (!validMime) return ValidationResult.InvalidFormat
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val size = stream.readBytes().size
            if (size > MAX_FILE_BYTES) return ValidationResult.TooLarge
        } ?: return ValidationResult.InvalidFormat
        return ValidationResult.Ok
    }

    enum class ValidationResult { Ok, InvalidFormat, TooLarge }
}
