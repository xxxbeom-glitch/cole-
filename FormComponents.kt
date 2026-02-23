import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text

// ─────────────────────────────────────────────
// TextField 상태 enum
// ─────────────────────────────────────────────

enum class TextFieldState {
    Default,
    Disabled,
    Error,
    Focus
}

// ─────────────────────────────────────────────
// 내부 스타일 헬퍼
// ─────────────────────────────────────────────

private data class TextFieldColors(
    val background: Color,
    val textColor: Color,
    val borderColor: Color?,
    val borderWidth: Float = 0f
)

private fun resolveColors(state: TextFieldState): TextFieldColors = when (state) {
    TextFieldState.Default -> TextFieldColors(
        background = AppColors.FormInputBgDefault,
        textColor = AppColors.FormTextPlaceholder,
        borderColor = null
    )
    TextFieldState.Disabled -> TextFieldColors(
        background = AppColors.FormInputBgDisabled,
        textColor = AppColors.FormTextDisabled,
        borderColor = null
    )
    TextFieldState.Error -> TextFieldColors(
        background = AppColors.FormInputBgError,
        textColor = AppColors.FormTextError,
        borderColor = null
    )
    TextFieldState.Focus -> TextFieldColors(
        background = AppColors.FormInputBgFocus,
        textColor = AppColors.FormTextActive,
        borderColor = AppColors.FormBorderFocus,
        borderWidth = 1.5f
    )
}

// ─────────────────────────────────────────────
// ColeTextField — 재사용 가능한 공통 텍스트 필드
//
// 사용 예시:
//   ColeTextField(value = text, onValueChange = { text = it }, placeholder = "이메일 입력")
//   ColeTextField(value = text, onValueChange = {}, isError = true)
//   ColeTextField(value = text, onValueChange = {}, enabled = false)
// ─────────────────────────────────────────────

@Composable
fun ColeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    isError: Boolean = false,
    helperText: String? = null,
    errorText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val state = when {
        !enabled -> TextFieldState.Disabled
        isError  -> TextFieldState.Error
        isFocused -> TextFieldState.Focus
        else     -> TextFieldState.Default
    }

    val colors = resolveColors(state)
    val shape = RoundedCornerShape(6.dp)

    val textColor = when {
        value.isEmpty() -> colors.textColor
        isError -> AppColors.FormTextError
        !enabled -> AppColors.FormTextDisabled
        else -> AppColors.FormTextValue
    }

    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = AppTypography.Input.copy(color = textColor),
            cursorBrush = SolidColor(AppColors.FormBorderFocus),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(shape)
                        .background(colors.background)
                        .then(
                            if (colors.borderColor != null) {
                                Modifier.border(
                                    width = colors.borderWidth.dp,
                                    color = colors.borderColor,
                                    shape = shape
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && !isFocused) {
                        Text(
                            text = placeholder,
                            style = AppTypography.Input.copy(color = colors.textColor)
                        )
                    }
                    innerTextField()
                }
            }
        )

        // 에러 메시지 또는 헬퍼 텍스트
        val subText = if (isError && errorText != null) errorText else helperText
        if (subText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subText,
                style = AppTypography.Disclaimer.copy(
                    color = if (isError) AppColors.FormTextError else AppColors.FormTextHelper
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
// 미리 정의된 상태별 래퍼 (선택적으로 사용)
// ─────────────────────────────────────────────

@Composable
fun ColeTextFieldDefault(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    ColeTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = true,
        isError = false,
    )
}

@Composable
fun ColeTextFieldDisabled(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    ColeTextField(
        value = value,
        onValueChange = {},
        modifier = modifier,
        placeholder = placeholder,
        enabled = false,
    )
}

@Composable
fun ColeTextFieldError(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
) {
    ColeTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        isError = true,
        errorText = errorText,
    )
}
