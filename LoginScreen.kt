import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ─────────────────────────────────────────────
// LoginScreen
// ─────────────────────────────────────────────

/**
 * 로그인 화면
 *
 * @param onLoginClick 로그인 버튼 클릭 콜백 (이메일, 비밀번호)
 * @param onSignUpClick "이메일로 가입하기" 클릭 콜백
 * @param onNaverLoginClick 네이버 로그인 클릭 콜백
 * @param onKakaoLoginClick 카카오 로그인 클릭 콜백
 * @param onGoogleLoginClick 구글 로그인 클릭 콜백
 * @param onForgotPasswordClick "로그인 정보를 잃어버리셨나요?" 클릭 콜백
 */
@Composable
fun LoginScreen(
    onLoginClick: (email: String, password: String) -> Unit,
    onSignUpClick: () -> Unit = {},
    onNaverLoginClick: () -> Unit = {},
    onKakaoLoginClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(125.dp))

        // 로고
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = "cole.",
            modifier = Modifier
                .width(280.dp)
                .height(150.dp),
        )

        Spacer(modifier = Modifier.height(50.dp))

        // 이메일 입력
        ColeTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "가입하신 이메일을 입력해주세요",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(9.dp))

        // 비밀번호 입력
        ColeTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "비밀번호를 입력해주세요",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(26.dp))

        // 로그인 버튼
        ColePrimaryButton(
            text = "로그인",
            onClick = { onLoginClick(email, password) },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 이메일로 가입하기
        Text(
            text = "이메일로 가입하기",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { onSignUpClick() }
                .padding(vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(38.dp))

        // SNS 간편 가입 텍스트
        Text(
            text = "SNS 계정으로 간편 가입하기",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextBody),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 소셜 로그인 아이콘 행
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 네이버
            Image(
                painter = painterResource(id = R.drawable.ic_naver),
                contentDescription = "네이버 로그인",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onNaverLoginClick() },
            )
            // 카카오
            Image(
                painter = painterResource(id = R.drawable.ic_kakao),
                contentDescription = "카카오 로그인",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onKakaoLoginClick() },
            )
            // 구글
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "구글 로그인",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { onGoogleLoginClick() },
            )
        }

        Spacer(modifier = Modifier.height(38.dp))

        // 로그인 정보 분실 안내
        Text(
            text = "로그인 정보를 잃어버리셨나요?",
            style = AppTypography.Caption2.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { onForgotPasswordClick() }
                .padding(vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
