import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorPalette = darkColorScheme(
    primary = AppColors.Primary300,
    onPrimary = AppColors.White900,
    secondary = AppColors.Grey700,
    onSecondary = AppColors.White900,
    error = AppColors.Red300,
    onError = AppColors.White900,
    background = AppColors.SurfaceBackgroundOverlay,
    onBackground = AppColors.TextInvert,
    surface = AppColors.Grey800,
    onSurface = AppColors.TextInvert,
    primaryContainer = AppColors.Primary700,
    onPrimaryContainer = AppColors.White900,
    secondaryContainer = AppColors.Grey600,
    onSecondaryContainer = AppColors.White900,
    tertiary = AppColors.Green300,
    onTertiary = AppColors.White900,
    tertiaryContainer = AppColors.Green700,
    onTertiaryContainer = AppColors.White900,
    errorContainer = AppColors.Red700,
    onErrorContainer = AppColors.White900,
    surfaceVariant = AppColors.Grey700,
    onSurfaceVariant = AppColors.Grey100,
    outline = AppColors.BorderDivider,
    inverseOnSurface = AppColors.Grey900,
    inverseSurface = AppColors.White900,
    inversePrimary = AppColors.Primary100,
)

private val LightColorPalette = lightColorScheme(
    primary = AppColors.Primary300,
    onPrimary = AppColors.White900,
    secondary = AppColors.Grey700,
    onSecondary = AppColors.White900,
    error = AppColors.Red300,
    onError = AppColors.White900,
    background = AppColors.SurfaceBackgroundBackground,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.SurfaceBackgroundCard,
    onSurface = AppColors.TextPrimary,
    primaryContainer = AppColors.Primary100,
    onPrimaryContainer = AppColors.Primary900,
    secondaryContainer = AppColors.Grey200,
    onSecondaryContainer = AppColors.Grey800,
    tertiary = AppColors.Green500,
    onTertiary = AppColors.White900,
    tertiaryContainer = AppColors.Green100,
    onTertiaryContainer = AppColors.Green900,
    errorContainer = AppColors.Red100,
    onErrorContainer = AppColors.Red900,
    surfaceVariant = AppColors.Grey100,
    onSurfaceVariant = AppColors.Grey700,
    outline = AppColors.BorderDefault,
    inverseOnSurface = AppColors.White900,
    inverseSurface = AppColors.Grey800,
    inversePrimary = AppColors.Primary900,
)

@Composable
fun ColeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography.toMaterialTypography(),
        content = content
    )
}

fun AppTypography.toMaterialTypography() = androidx.compose.material3.Typography(
    displayLarge = Display1,
    displayMedium = Display2,
    displaySmall = Display3,
    headlineLarge = HeadingH1,
    headlineMedium = HeadingH2,
    headlineSmall = HeadingH3,
    titleLarge = HeadingH1,
    titleMedium = HeadingH2,
    titleSmall = HeadingH3,
    bodyLarge = BodyRegular,
    bodyMedium = BodyMedium,
    bodySmall = Caption1,
    labelLarge = ButtonLarge,
    labelMedium = ButtonSmall,
    labelSmall = Label,
)