package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 회원가입 플로우 간격 (스크린샷 기준 재조정)
// ─────────────────────────────────────────────
internal object SignUpSpacing {
    val headerToContent: Dp = 20.dp     // 헤더 ~ 콘텐츠
    val titleToSubtitle: Dp = 8.dp      // 메인 타이틀 ~ 서브타이틀 (타이트)
    val subtitleToContent: Dp = 36.dp  // 서브타이틀 ~ 첫 입력 섹션
    val labelToInput: Dp = 8.dp         // 라벨 ~ 입력 필드 (작은)
    val inputToInput: Dp = 16.dp        // 입력 필드 사이 (기본)
    val inputGroupGap: Dp = 20.dp       // 입력 폼 그룹 간 (이름/생년월일/휴대전화, +4px)
    val passwordFieldGap: Dp = 7.dp   // 비밀번호 2개 입력 필드 사이
    val errorToNext: Dp = 6.dp          // 에러 메시지 ~ 다음 요소
    val resendToButton: Dp = 16.dp      // 재발송 문구 ~ 버튼
    val contentPaddingTop: Dp = 24.dp
    val contentPaddingBottom: Dp = 24.dp
    val contentPaddingHorizontal: Dp = 16.dp
    val buttonGap: Dp = 12.dp
    val iconToText: Dp = 24.dp          // MB-08 완료 아이콘~텍스트
    val bottomButtonPadding: Dp = 24.dp // 하단 버튼 영역 패딩 (WindowInsets 외)
}

/** 비밀번호 유효성: 8자 이상, 영문 대문자·소문자·숫자 각 최소 1개 */
internal fun isPasswordValid(password: String): Boolean {
    if (password.length < 8) return false
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    return hasUpperCase && hasLowerCase && hasDigit
}

// ─────────────────────────────────────────────
// 회원가입 공통 헤더 (앱바: 뒤로가기 + 타이틀 + 알림 36dp)
// ─────────────────────────────────────────────

@Composable
fun SignUpHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    showNotification: Boolean = false, // 회원가입·비밀번호재설정 플로우는 알림 숨김
    modifier: Modifier = Modifier,
) {
    ColeHeaderSub(
        title = title,
        backIcon = painterResource(id = R.drawable.ic_back),
        onBackClick = onBackClick,
        showNotification = showNotification,
        hasNotification = true,
        modifier = modifier.fillMaxWidth(),
    )
}

// ─────────────────────────────────────────────
// MB-01: 이메일 입력
// ─────────────────────────────────────────────

@Composable
fun SignUpEmailScreen(
    onNextClick: (email: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    val isValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

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
        SignUpHeader(title = "회원 가입", onBackClick = onBackClick)

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
                Text("회원 가입", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "원활한 이용을 위해 아래 내용들을 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.subtitleToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                Text(
                    "사용하실 이메일 주소를 적어주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                ColeTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "user@mail.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
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
                text = "다음",
                onClick = { onNextClick(email) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// MB-02: 비밀번호 설정
// ─────────────────────────────────────────────

@Composable
fun SignUpPasswordScreen(
    onNextClick: (password: String) -> Unit,
    onBackClick: () -> Unit,
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
        SignUpHeader(title = "회원 가입", onBackClick = onBackClick)

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
                Text("회원 가입", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "원활한 이용을 위해 아래 내용들을 입력해주세요",
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
                        onValueChange = { password = it },
                        placeholder = "영문 대문자, 소문자, 숫자 각 1개 이상 포함 8자리",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = !isPasswordValid && password.isNotEmpty(),
                        errorText = if (!isPasswordValid && password.isNotEmpty()) "영문 대문자, 소문자, 숫자 각 1개 이상 포함 8자리 이상" else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ColeTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
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
                text = "다음",
                onClick = { onNextClick(password) },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// MB-05: 이름 / 생년월일 / 휴대전화 입력
// ─────────────────────────────────────────────

@Composable
fun SignUpNameBirthPhoneScreen(
    onNextClick: (name: String, birth: String, phone: String) -> Unit,
    onBackClick: () -> Unit,
    errorMessage: String? = null,
    onClearError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var birth by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    val canProceed = name.isNotBlank() && birth.isNotBlank() && phone.isNotBlank()

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
        SignUpHeader(title = "회원 가입", onBackClick = onBackClick)

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
                Text("회원 가입", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
                Text(
                    "원활한 이용을 위해 아래 내용들을 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.subtitleToContent))
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.inputGroupGap)) {
                Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                    Text("이름을 입력해주세요", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
                    ColeTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "예) 장원영",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                    Text("생년월일을 입력해주세요", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
                    ColeTextField(
                        value = birth,
                        onValueChange = { birth = it },
                        placeholder = "예) 19880101",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.labelToInput)) {
                    Text("휴대전화 번호를 입력해주세요", style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel))
                    ColeTextField(
                        value = phone,
                        onValueChange = { phone = it; onClearError() },
                        placeholder = "예) 01012341234",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                text = "인증문자 발송",
                onClick = { onNextClick(name, birth, phone) },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// MB-06: 인증번호 입력
// ─────────────────────────────────────────────

@Composable
fun SignUpVerificationCodeScreen(
    onNextClick: (code: String) -> Unit,
    onBackClick: () -> Unit,
    onResendClick: () -> Unit,
    isLoading: Boolean = false,
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
        SignUpHeader(title = "회원 가입", onBackClick = onBackClick)

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
                Text("회원 가입", style = AppTypography.Display3.copy(color = AppColors.TextPrimary))
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
            Spacer(modifier = Modifier.height(SignUpSpacing.resendToButton))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "혹시 인증번호가 오지 않았나요? ",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                )
                Text(
                    if (isResendLoading) "발송 중..." else "재발송",
                    style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
                    modifier = Modifier.clickable(enabled = !isResendLoading) { onResendClick() },
                )
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
                text = if (isLoading) "가입 중..." else "다음",
                onClick = { onNextClick(code) },
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─────────────────────────────────────────────
// MB-08: 회원가입 완료
// ─────────────────────────────────────────────

@Composable
fun SignUpCompleteScreen(
    onStartClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SignUpSpacing.iconToText),
        ) {
            IcoCompleted()
            Text(
                "회원가입을 완료했어요",
                style = AppTypography.HeadingH3.copy(color = AppColors.TextPrimary),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SignUpSpacing.contentPaddingHorizontal,
                    end = SignUpSpacing.contentPaddingHorizontal,
                    bottom = SignUpSpacing.bottomButtonPadding,
                ),
        ) {
            ColePrimaryButton(
                text = "시작하기",
                onClick = onStartClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
