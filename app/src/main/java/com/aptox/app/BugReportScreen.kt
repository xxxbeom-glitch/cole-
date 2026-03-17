package com.aptox.app

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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val remaining = MAX_ATTACHMENTS - attachments.size
        if (remaining <= 0) return@rememberLauncherForActivityResult
        uris.take(remaining).forEach { uri ->
            when (val result = BugReportRepository.validateImageUri(context, uri)) {
                BugReportRepository.ValidationResult.Ok -> attachments.add(uri)
                BugReportRepository.ValidationResult.InvalidFormat ->
                    android.widget.Toast.makeText(context, "JPG, PNG 파일만 첨부 가능해요", android.widget.Toast.LENGTH_SHORT).show()
                BugReportRepository.ValidationResult.TooLarge ->
                    android.widget.Toast.makeText(context, "파일 크기는 2MB 이하여야 해요", android.widget.Toast.LENGTH_SHORT).show()
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

                // 첨부된 파일명 텍스트 목록 (썸네일 대신 파일명으로 표시)
                attachments.forEachIndexed { index, uri ->
                    val fileName = uri.lastPathSegment?.substringAfterLast("/")
                        ?: uri.toString().substringAfterLast("/").take(40)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppColors.FormInputBgDefault)
                            .border(1.dp, AppColors.BorderDefault, RoundedCornerShape(6.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = fileName,
                            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_bug_report_delete),
                            contentDescription = "삭제",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { attachments.removeAt(index) },
                        )
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
                            isSubmitting = true
                            try {
                                onSubmit(title.trim(), content.trim(), attachments.toList())
                                onBack()
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    e.message ?: "등록에 실패했어요",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
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
    }
}
