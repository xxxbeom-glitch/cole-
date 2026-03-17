package com.aptox.app

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
/**
 * 버그 신고 이미지 업로드.
 * 경로: bug-reports/{userId}/{timestamp}/image_{1~n}.jpg
 */
object BugReportRepository {

    private val storage by lazy { FirebaseStorage.getInstance() }
    private const val MAX_FILE_BYTES = 2 * 1024 * 1024L // 2MB
    private val ALLOWED_MIME = setOf("image/jpeg", "image/jpg", "image/png")

    /**
     * @return 업로드된 이미지 URL 목록. 실패 시 exception.
     */
    suspend fun uploadImages(context: Context, uris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext emptyList()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference.child("bug-reports").child(userId).child("$timestamp")
        uris.mapIndexed { index, uri ->
            val fileName = "image_${index + 1}.jpg"
            val childRef = ref.child(fileName)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                childRef.putStream(stream).await()
            } ?: throw RuntimeException("이미지 읽기 실패")
            childRef.downloadUrl.await().toString()
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
