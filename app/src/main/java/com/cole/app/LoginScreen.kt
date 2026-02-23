package com.cole.app

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Composable
fun LoginScreen(
    logo: Painter,
    onLoginClick: (email: String, password: String) -> Unit = { _, _ -> },
    onSignUpClick: () -> Unit = {},
    onNaverLoginClick: () -> Unit = {},
    onKakaoLoginClick: () -> Unit = {},
    onGoogleLoginClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.SurfaceBackgroundLogin)
            .verticalScroll(rememberScrollState())
            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Image(
            painter = logo,
            contentDescription = "cole.",
            modifier = Modifier.width(280.dp).height(150.dp),
        )

        Spacer(modifier = Modifier.height(56.dp))

        ColeTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = "가입하신 이메일을 입력해주세요",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        ColeTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "비밀번호를 입력해주세요",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        ColePrimaryButton(
            text = "로그인",
            onClick = {
                focusManager.clearFocus()
                (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
                onLoginClick(email, password)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "이메일로 가입하기",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onSignUpClick() }.padding(vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "SNS 계정으로 간편 가입하기",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_naver),
                contentDescription = "네이버 로그인",
                modifier = Modifier.size(56.dp).clip(CircleShape).clickable { onNaverLoginClick() },
            )
            Image(
                painter = painterResource(id = R.drawable.ic_kakao),
                contentDescription = "카카오 로그인",
                modifier = Modifier.size(56.dp).clip(CircleShape).clickable { onKakaoLoginClick() },
            )
            Image(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "구글 로그인",
                modifier = Modifier.size(56.dp).clip(CircleShape).clickable { onGoogleLoginClick() },
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "로그인 정보를 잃어버리셨나요?",
            style = AppTypography.BodyMedium.copy(color = AppColors.TextCaption),
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onForgotPasswordClick() }.padding(vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}
