package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class AgreementItem(
    val text: String,
    val isRequired: Boolean,
    var checked: Boolean,
    val onDetailClick: (() -> Unit)? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsBottomSheet(
    onDismissRequest: () -> Unit,
    onNextClick: () -> Unit,
    agreements: List<AgreementItem>,
    allAgreedState: Boolean,
    onAllAgreedChange: (Boolean) -> Unit,
    onItemCheckedChange: (index: Int, checked: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        modifier = modifier,
        containerColor = AppColors.SurfaceBackgroundBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 36.dp),
        ) {
            // 헤더: 타이틀
            Text(
                text = "이용약관",
                style = AppTypography.HeadingH1.copy(color = AppColors.TextPrimary),
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 부제목
            Text(
                text = "서비스 이용을 위해 이용약관 동의가 필요해요",
                style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 체크박스 라인 기준 정렬: 모두 동의 + 개별 항목이 같은 Column 안에서 동일 start
            val checkboxLinePadding = 18.dp
            Column(
                modifier = Modifier.padding(horizontal = checkboxLinePadding),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppColors.Grey200, RoundedCornerShape(6.dp))
                        .clickable { onAllAgreedChange(!allAgreedState) }
                        .padding(vertical = 16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColeCheckBox(checked = allAgreedState, onCheckedChange = onAllAgreedChange)
                        Text(
                            text = "모두 동의합니다",
                            style = AppTypography.BodyBold.copy(color = AppColors.TextBody),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    agreements.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemCheckedChange(index, !item.checked) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ColeCheckBox(
                                checked = item.checked,
                                onCheckedChange = { isChecked -> onItemCheckedChange(index, isChecked) },
                            )
                            if (item.linkText != null && item.linkUrl != null) {
                                val annotatedText = buildAnnotatedString {
                                    val linkStart = item.text.indexOf(item.linkText)
                                    if (linkStart >= 0) {
                                        append(item.text.substring(0, linkStart))
                                        withStyle(
                                            SpanStyle(
                                                color = AppColors.Primary300,
                                                textDecoration = TextDecoration.Underline,
                                            )
                                        ) {
                                            append(item.linkText)
                                        }
                                        append(item.text.substring(linkStart + item.linkText.length))
                                    } else {
                                        append(item.text)
                                    }
                                }
                                Text(
                                    text = annotatedText,
                                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                    modifier = Modifier.clickable(onClick = {
                                        openWebView(context, item.linkUrl)
                                    }),
                                )
                            } else {
                                Text(
                                    text = item.text,
                                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // 다음 버튼
            ColePrimaryButton(
                text = "다음",
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onNextClick()
                    }
                },
                enabled = allAgreedState,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun openWebView(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
