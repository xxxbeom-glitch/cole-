package com.aptox.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CrashLogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    var summaries by remember { mutableStateOf<Map<String, Pair<String, String>>>(emptyMap()) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var detailText by remember { mutableStateOf<String?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshKey) {
        val list = withContext(Dispatchers.IO) { CrashLogRepository.listLogFiles(context) }
        files = list
        summaries = withContext(Dispatchers.IO) {
            list.associate { f -> f.absolutePath to CrashLogRepository.summarizeForList(f) }
        }
    }

    LaunchedEffect(selectedFile) {
        val f = selectedFile
        detailText = if (f != null) {
            withContext(Dispatchers.IO) { CrashLogRepository.readLogText(f) }
        } else {
            null
        }
    }

    val detail = selectedFile
    if (detail != null && detailText != null) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.SurfaceBackgroundBackground)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                AptoxHeaderSub(
                    title = "크래시 로그",
                    backIcon = painterResource(R.drawable.ic_back),
                    onBackClick = { selectedFile = null },
                    showNotification = false,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = detail.name,
                        style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
                        modifier = Modifier.weight(1f),
                    )
                    AptoxGhostButton(
                        text = "보내기",
                        onClick = {
                            if (!CrashLogExport.shareSingleLog(context, detail)) {
                                toastMessage = "보내기에 실패했어요."
                            }
                        },
                        modifier = Modifier.height(40.dp),
                    )
                }
                Text(
                    text = detailText!!,
                    style = AppTypography.Caption1.copy(color = AppColors.TextBody),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                )
            }
            AptoxToast(
                message = toastMessage ?: "",
                visible = toastMessage != null,
                onDismiss = { toastMessage = null },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.SurfaceBackgroundBackground)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            AptoxHeaderSub(
                title = "크래시 로그",
                backIcon = painterResource(R.drawable.ic_back),
                onBackClick = onBack,
                showNotification = false,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "저장된 로그 ${files.size}건",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextSecondary),
                    modifier = Modifier.weight(1f),
                )
                AptoxGhostButton(
                    text = "ZIP보내기",
                    onClick = {
                        scope.launch {
                            val ok = CrashLogExport.shareAllLogsAsZip(context)
                            if (!ok) {
                                toastMessage = if (files.isEmpty()) {
                                    "보낼 로그가 없어요."
                                } else {
                                    "ZIP을 만들거나 보내기에 실패했어요."
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(40.dp),
                )
                AptoxGhostButton(
                    text = "전체 삭제",
                    onClick = { showClearConfirm = true },
                    modifier = Modifier.height(40.dp),
                )
            }

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "저장된 크래시 로그가 없어요.",
                        style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(files, key = { it.absolutePath }) { file ->
                        val (title, sub) = summaries[file.absolutePath] ?: (file.name to "")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.SurfaceBackgroundCard)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) { selectedFile = file }
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = title,
                                    style = AppTypography.BodyMedium.copy(color = AppColors.TextPrimary),
                                )
                                Text(
                                    text = sub,
                                    maxLines = 2,
                                    style = AppTypography.Caption1.copy(color = AppColors.TextCaption),
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (!CrashLogExport.shareSingleLog(context, file)) {
                                        toastMessage = "보내기에 실패했어요."
                                    }
                                },
                            ) {
                                Text("보내기", style = AppTypography.BodyMedium)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        AptoxToast(
            message = toastMessage ?: "",
            visible = toastMessage != null,
            onDismiss = { toastMessage = null },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("크래시 로그 전체 삭제", style = AppTypography.HeadingH3) },
            text = {
                Text(
                    "내부 저장소의 크래시 로그 파일을 모두 삭제할까요?",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        CrashLogRepository.clearAll(context)
                        refreshKey++
                    },
                ) {
                    Text("삭제", color = AppColors.Red300)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("취소")
                }
            },
        )
    }
}
