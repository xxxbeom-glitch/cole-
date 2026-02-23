package com.cole.app

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
import androidx.compose.material3.Text
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

enum class TextFieldState { Default, Disabled, Error, Focus }

private data class TextFieldColors(
    val background: Color,
    val textColor: Color,
    val borderColor: Color?,
    val borderWidth: Float = 0f,
)

private fun resolveColors(state: TextFieldState): TextFieldColors = when (state) {
    TextFieldState.Default  -> TextFieldColors(AppColors.FormInputBgDefault, AppColors.FormTextPlaceholder, null)
    TextFieldState.Disabled -> TextFieldColors(AppColors.FormInputBgDisabled, AppColors.FormTextDisabled, null)
    TextFieldState.Error    -> TextFieldColors(AppColors.FormInputBgError, AppColors.FormTextError, null)
    TextFieldState.Focus    -> TextFieldColors(AppColors.FormInputBgFocus, AppColors.FormTextActive, AppColors.FormBorderFocus, 1.5f)
}

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
        !enabled  -> TextFieldState.Disabled
        isError   -> TextFieldState.Error
        isFocused -> TextFieldState.Focus
        else      -> TextFieldState.Default
    }

    val colors = resolveColors(state)
    val shape = RoundedCornerShape(6.dp)
    val textColor = if (value.isEmpty()) colors.textColor else AppColors.FormTextValue

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
                            if (colors.borderColor != null)
                                Modifier.border(colors.borderWidth.dp, colors.borderColor, shape)
                            else Modifier
                        )
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && !isFocused) {
                        Text(text = placeholder, style = AppTypography.Input.copy(color = colors.textColor))
                    }
                    innerTextField()
                }
            },
        )
        val subText = if (isError && errorText != null) errorText else helperText
        if (subText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subText,
                style = AppTypography.Disclaimer.copy(
                    color = if (isError) AppColors.FormTextError else AppColors.FormTextHelper,
                ),
            )
        }
    }
}

@Composable
fun ColeTextFieldDefault(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "") {
    ColeTextField(value = value, onValueChange = onValueChange, modifier = modifier, placeholder = placeholder)
}

@Composable
fun ColeTextFieldDisabled(value: String, modifier: Modifier = Modifier, placeholder: String = "") {
    ColeTextField(value = value, onValueChange = {}, modifier = modifier, placeholder = placeholder, enabled = false)
}

@Composable
fun ColeTextFieldError(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", errorText: String? = null) {
    ColeTextField(value = value, onValueChange = onValueChange, modifier = modifier, placeholder = placeholder, isError = true, errorText = errorText)
}
