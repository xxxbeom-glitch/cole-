package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 비밀번호 재설정 RS-01 ~ RS-03 (회원가입 플로우와 동일 가이드)
// ─────────────────────────────────────────────

// RS-01: 본인 확인 (휴대폰 번호 입력)
@Composable
fun PasswordResetPhoneScreen(
    onNextClick: (phone: String) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var phone by remember { mutableStateOf("") }
    val normalizedPhone = phone.replace(Regex("[^0-9]"), "")
    val isValid = normalizedPhone.length >= 10

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignUpHeader(title = "비밀번호 재설정", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = SignUpSpacing.contentPaddingTop,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(SignUpSpacing.headerToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.titleToSubtitle)) {
                Text("본인 확인", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "가입 시 입력한 전화번호로 인증번호를 보내드릴게요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.subtitleToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                Text(
                    "가입 시 입력한 전화번호를 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                ColeTextField(
                    value = phone,
                    onValueChange = { phone = it; onClearError() },
                    placeholder = "예) 01012345678",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(SignUpSpacing.errorToNext))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IcoErrorInfo()
                        Text(
                            errorMessage,
                            style = AppTypography.Disclaimer.copy(color = AppColors.FormTextError),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = 0.dp,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.bottomButtonPadding,
                ),
        ) {
            ColePrimaryButton(
                text = if (isLoading) "발송 중..." else "인증번호 발송",
                onClick = { onNextClick(if (normalizedPhone.startsWith("0")) normalizedPhone else "0$normalizedPhone") },
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// RS-02: 인증번호 입력
@Composable
fun PasswordResetCodeScreen(
    onNextClick: (code: String) -> Unit,
    onBackClick: () -> Unit,
    onResendClick: () -> Unit,
    isResendLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    var remainSeconds by remember { mutableIntStateOf(180) } // 3분
    val isValid = code.length == 6

    LaunchedEffect(Unit) {
        while (remainSeconds > 0) {
            delay(1000)
            remainSeconds -= 1
        }
    }

    val minutes = remainSeconds / 60
    val seconds = remainSeconds % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignUpHeader(title = "비밀번호 재설정", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = SignUpSpacing.contentPaddingTop,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(SignUpSpacing.headerToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.titleToSubtitle)) {
                Text("인증번호 입력", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "수신된 6자리 코드를 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.subtitleToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                Text(
                    "수신된 6자리 코드를 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                // 인풋박스 안에 타이머 (우측)
                val shape = RoundedCornerShape(6.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(shape)
                        .background(AppColors.FormInputBgDefault)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = code,
                            onValueChange = { code = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = AppTypography.Input.copy(
                                color = if (code.isEmpty()) AppColors.FormTextPlaceholder else AppColors.FormTextValue,
                            ),
                            cursorBrush = SolidColor(AppColors.FormBorderFocus),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box {
                                    if (code.isEmpty()) {
                                        Text(
                                            "인증번호 6자리",
                                            style = AppTypography.Input.copy(color = AppColors.FormTextPlaceholder),
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        Text(
                            timerText,
                            style = AppTypography.Caption1.copy(color = AppColors.FormTextError),
                        )
                    }
                }
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(SignUpSpacing.errorToNext))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IcoErrorInfo()
                    Text(
                        errorMessage,
                        style = AppTypography.Disclaimer.copy(color = AppColors.FormTextError),
                    )
                }
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.resendToButton))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "코드를 받지 못했나요? ",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                )
                Text(
                    if (isResendLoading) "발송 중..." else "재발송",
                    style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
                    modifier = Modifier.clickable(enabled = !isResendLoading) { onResendClick() },
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = 0.dp,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.bottomButtonPadding,
                ),
        ) {
            ColePrimaryButton(
                text = "계속 진행",
                onClick = { onNextClick(code) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// RS-03: 새 비밀번호 설정
@Composable
fun PasswordResetNewPasswordScreen(
    onNextClick: (password: String) -> Unit,
    onBackClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isPasswordValid = isPasswordValid(password)
    val isConfirmValid = password == confirmPassword && confirmPassword.isNotBlank()
    val canProceed = isPasswordValid && isConfirmValid
    val showPasswordMismatch = !isConfirmValid && confirmPassword.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SignUpHeader(title = "비밀번호 재설정", onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = SignUpSpacing.contentPaddingTop,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(modifier = Modifier.height(SignUpSpacing.headerToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.titleToSubtitle)) {
                Text("새 비밀번호 설정", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "새로 사용할 비밀번호를 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.subtitleToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                Text(
                    "비밀번호를 적어주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.passwordFieldGap)) {
                    ColeTextField(
                        value = password,
                        onValueChange = { password = it; onClearError() },
                        placeholder = "영문 대문자, 소문자, 숫자 각 1개 이상 포함 8자리",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = !isPasswordValid && password.isNotEmpty(),
                        errorText = if (!isPasswordValid && password.isNotEmpty()) "영문 대문자, 소문자, 숫자 각 1개 이상 포함 8자리 이상" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ColeTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; onClearError() },
                        placeholder = "한번 더 입력해주세요",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = showPasswordMismatch,
                        errorText = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (showPasswordMismatch) {
                    Spacer(modifier = Modifier.height(SignUpSpacing.errorToNext))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IcoErrorInfo()
                        Text(
                            "비밀번호가 일치하지 않습니다",
                            style = AppTypography.Disclaimer.copy(color = AppColors.FormTextError),
                        )
                    }
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(SignUpSpacing.errorToNext))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IcoErrorInfo()
                        Text(
                            errorMessage,
                            style = AppTypography.Disclaimer.copy(color = AppColors.FormTextError),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    top = 0.dp,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.bottomButtonPadding,
                ),
        ) {
            ColePrimaryButton(
                text = if (isLoading) "변경 중..." else "변경 완료",
                onClick = { onNextClick(password) },
                enabled = canProceed && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
