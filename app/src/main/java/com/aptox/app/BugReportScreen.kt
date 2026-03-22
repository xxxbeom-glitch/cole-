package com.aptox.app

import android.provider.OpenableColumns
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aptox.app.ui.components.AptoxToast
import kotlinx.coroutines.launch

private const val BUG_REPORT_MAX_LENGTH = 1000
private const val MAX_ATTACHMENTS = 3

/**
 * 버그 신고 화면 (Figma 1164-5019)
 * - 제목, 본문 작성 필드 분리
 * - 첨부파일: JPG/PNG 최대 3개, 파일당 2MB → 파일명 텍스트로 표시
 * - 등록하기 버튼 하단 고정
 * - Firebase Storage 업로드 후 Firestore 저장
 */
@Composable
fun BugReportScreen(
    onBack: () -> Unit,
    onSubmit: suspend (title: String, content: String, imageUris: List<android.net.Uri>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val attachments = remember { mutableStateListOf<Uri>() }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uploadError = null
        val remaining = MAX_ATTACHMENTS - attachments.size
        if (remaining <= 0) return@rememberLauncherForActivityResult
        uris.take(remaining).forEach { uri ->
            when (val result = BugReportRepository.validateImageUri(context, uri)) {
                BugReportRepository.ValidationResult.Ok -> attachments.add(uri)
                BugReportRepository.ValidationResult.InvalidFormat ->
                    toastMessage = "JPG, PNG 파일만 첨부 가능해요"
                BugReportRepository.ValidationResult.TooLarge ->
                    toastMessage = "파일 크기는 2MB 이하여야 해요"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.FormInputBgDefault)
                    .padding(20.dp),
                textStyle = AppTypography.Input.copy(color = AppColors.FormTextValue),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "제목",
                            style = AppTypography.Input.copy(color = AppColors.FormTextPlaceholder),
                        )
                    }
                    innerTextField()
                },
                singleLine = true,
            )

            BasicTextField(
                value = content,
                onValueChange = { newValue ->
                    if (newValue.length <= BUG_REPORT_MAX_LENGTH) content = newValue
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(222.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.FormInputBgDefault)
                    .padding(20.dp),
                textStyle = AppTypography.Input.copy(color = AppColors.FormTextValue),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text(
                            text = "내용을 입력해주세요",
                            style = AppTypography.Input.copy(color = AppColors.FormTextPlaceholder),
                        )
                    }
                    innerTextField()
                },
            )

            // 첨부파일 영역
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 추가 버튼: 3개 미만일 때만 표시
                if (attachments.size < MAX_ATTACHMENTS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.FormInputBgDefault)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { imagePickerLauncher.launch("image/*") }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "첨부파일 (JPG 및 PNG)",
                            style = AppTypography.Input.copy(color = AppColors.FormTextPlaceholder),
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_bug_report_add),
                            contentDescription = "파일 첨부",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // 업로드 실패 시 에러 메시지 전체 표시
                uploadError?.let { err ->
                    Text(
                        text = err,
                        style = AppTypography.Caption2.copy(color = AppColors.FormTextError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.FormTextError.copy(alpha = 0.1f))
                            .padding(12.dp),
                    )
                }

                // 첨부된 파일명 텍스트 목록 (확장자는 항상 끝에 표시)
                if (attachments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        attachments.forEachIndexed { index, uri ->
                            val (namePart, extPart) = resolveFileNameWithExtension(context, uri)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    Text(
                                        text = namePart,
                                        style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    )
                                    if (extPart.isNotEmpty()) {
                                        Text(
                                            text = extPart,
                                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                            maxLines = 1,
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_bug_report_delete),
                                    contentDescription = "삭제",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) { attachments.removeAt(index) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 등록하기 버튼 — 하단 고정
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(AppColors.SurfaceBackgroundBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            if (isSubmitting) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                AptoxPrimaryButton(
                    text = "등록하기",
                    onClick = {
                        scope.launch {
                            uploadError = null
                            isSubmitting = true
                            try {
                                onSubmit(title.trim(), content.trim(), attachments.toList())
                                onBack()
                            } catch (e: Exception) {
                                uploadError = e.stackTraceToString()
                                toastMessage = e.message?.takeIf { it.isNotBlank() } ?: "등록에 실패했어요"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = title.isNotBlank() && content.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        AptoxToast(
            message = toastMessage ?: "",
            visible = toastMessage != null,
            onDismiss = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun resolveFileNameWithExtension(context: android.content.Context, uri: Uri): Pair<String, String> {
    var raw = ""
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) raw = cursor.getString(idx) ?: ""
        }
    }
    if (raw.isEmpty()) {
        raw = uri.lastPathSegment?.substringAfterLast("/") ?: uri.toString().substringAfterLast("/")
    }
    val extFromMime = when (context.contentResolver.getType(uri)?.lowercase()) {
        "image/jpeg", "image/jpg" -> ".jpg"
        "image/png" -> ".png"
        else -> ""
    }
    return if (raw.contains(".")) {
        val lastDot = raw.lastIndexOf('.')
        raw.substring(0, lastDot) to ".${raw.substring(lastDot + 1).lowercase()}"
    } else {
        val name = raw.ifEmpty { "이미지" }
        name to extFromMime
    }
}
