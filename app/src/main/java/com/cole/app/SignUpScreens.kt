package com.cole.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// 회원가입 플로우 간격 (Figma MB-01~MB-08 기준)
// ─────────────────────────────────────────────
private val SignUpSpacing = object {
    val headerToContent: Dp = 26.dp     // 헤더 ~ 콘텐츠 (gap)
    val titleToSubtitle: Dp = 40.dp    // 메인 타이틀 ~ 서브타이틀
    val subtitleToContent: Dp = 48.dp  // 서브타이틀 ~ 첫 입력 섹션
    val labelToInput: Dp = 32.dp       // 라벨 ~ 입력 필드
    val inputToInput: Dp = 32.dp       // 입력 필드 사이
    val errorToNext: Dp = 6.dp         // 에러 메시지 ~ 다음 요소 (gap)
    val resendToButton: Dp = 16.dp     // 재발송 문구 ~ 버튼
    val contentPaddingTop: Dp = 48.dp
    val contentPaddingBottom: Dp = 34.dp
    val contentPaddingHorizontal: Dp = 16.dp
    val buttonGap: Dp = 12.dp
    val iconToText: Dp = 22.dp         // MB-08 완료 아이콘~텍스트
}

// ─────────────────────────────────────────────
// 회원가입 공통 헤더 (앱바: 뒤로가기 + 타이틀 + 알림 36dp)
// ─────────────────────────────────────────────

@Composable
fun SignUpHeader(
    title: String,
    onBackClick: (() -> Unit)? = null,
    showNotification: Boolean = true,
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
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(SignUpSpacing.buttonGap),
        ) {
            ColePrimaryButton(
                text = "다음",
                onClick = { onNextClick(email) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
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

    val isPasswordValid = password.length >= 8
    val isConfirmValid = password == confirmPassword && confirmPassword.isNotBlank()
    val canProceed = isPasswordValid && isConfirmValid
    val showPasswordMismatch = !isConfirmValid && confirmPassword.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
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
                ColeTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "영어 대/소문자 및 숫자 포함 8자리 이상",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = !isPasswordValid && password.isNotEmpty(),
                    errorText = if (!isPasswordValid && password.isNotEmpty()) "영어 대/소문자 및 숫자 포함 8자리 이상" else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(SignUpSpacing.inputToInput))
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
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(SignUpSpacing.buttonGap),
        ) {
            ColePrimaryButton(
                text = "다음",
                onClick = { onNextClick(password) },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
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
            Column(verticalArrangement = Arrangement.spacedBy(SignUpSpacing.inputToInput)) {
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
                        onValueChange = { phone = it },
                        placeholder = "예) 01012341234",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
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
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(SignUpSpacing.buttonGap),
        ) {
            ColePrimaryButton(
                text = "인증문자 발송",
                onClick = { onNextClick(name, birth, phone) },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
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
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    val isValid = code.length == 6

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
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
                    "받으신 인증번호를 입력해주세요",
                    style = AppTypography.BodyMedium.copy(color = AppColors.FormTextLabel),
                )
                ColeTextField(
                    value = code,
                    onValueChange = { code = it },
                    placeholder = "인증번호 6자리",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(SignUpSpacing.resendToButton))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "혹시 인증문자가 오지 않았나요? ",
                    style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
                )
                Text(
                    "재발송",
                    style = AppTypography.BodyBold.copy(color = AppColors.TextHighlight),
                    modifier = Modifier.clickable { onResendClick() },
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
                    bottom = SignUpSpacing.contentPaddingBottom,
                ),
            verticalArrangement = Arrangement.spacedBy(SignUpSpacing.buttonGap),
        ) {
            ColePrimaryButton(
                text = "다음",
                onClick = { onNextClick(code) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth(),
            )
            ColeGhostButton(text = "돌아가기", onClick = onBackClick, modifier = Modifier.fillMaxWidth())
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
            .background(AppColors.SurfaceBackgroundBackground),
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
                    bottom = 36.dp,
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
