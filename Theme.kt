import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Assuming AppColors and AppTypography are defined in Color.kt and Type.kt respectively
// import com.example.cole.ui.theme.AppColors
// import com.example.cole.ui.theme.AppTypography

private val DarkColorPalette = darkColorScheme(
    primary = AppColors.Primary300,
    onPrimary = AppColors.White900,
    secondary = AppColors.Grey700,
    onSecondary = AppColors.White900,
    error = AppColors.Red300,
    onError = AppColors.White900,
    background = AppColors.Grey900,
    onBackground = AppColors.White900,
    surface = AppColors.Grey800,
    onSurface = AppColors.White900,
    // Add more colors as needed from AppColors for a complete dark theme
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
    outline = AppColors.Grey550,
    inverseOnSurface = AppColors.Grey900,
    inverseSurface = AppColors.White900,
    inversePrimary = AppColors.Primary100,
    // These colors might need further mapping to the AppColors defined.
    // For simplicity, some are directly mapped or derived.
)

private val LightColorPalette = lightColorScheme(
    primary = AppColors.Primary300,
    onPrimary = AppColors.White900,
    secondary = AppColors.Grey700,
    onSecondary = AppColors.White900,
    error = AppColors.Red300,
    onError = AppColors.White900,
    background = AppColors.White900,
    onBackground = AppColors.Grey900,
    surface = AppColors.White900,
    onSurface = AppColors.Grey900,
    // Add more colors as needed from AppColors for a complete light theme
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
    outline = AppColors.Grey350,
    inverseOnSurface = AppColors.White900,
    inverseSurface = AppColors.Grey800,
    inversePrimary = AppColors.Primary900,
    // These colors might need further mapping to the AppColors defined.
    // For simplicity, some are directly mapped or derived.
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
        typography = AppTypography.toMaterialTypography(), // Assuming a conversion function
        content = content
    )
}

// Extension function to convert AppTypography to MaterialTheme Typography
// This assumes you want to map your custom text styles to MaterialTheme's typography system.
// You might need to adjust this mapping based on your exact design requirements.
fun AppTypography.toMaterialTypography() = androidx.compose.material3.Typography(
    displayLarge = Display1,
    displayMedium = Display2,
    displaySmall = Display3,
    headlineLarge = HeadingH1,
    headlineMedium = HeadingH2,
    headlineSmall = HeadingH3,
    bodyLarge = BodyRegular, // Or BodyMedium, choose the most appropriate default
    bodyMedium = BodyMedium,
    bodySmall = Caption1,
    labelLarge = ButtonLarge,
    labelMedium = ButtonSmall,
    labelSmall = Label,
    titleLarge = HeadingH1, // Re-using, adjust if specific title styles exist
    titleMedium = HeadingH2, // Re-using, adjust if specific title styles exist
    titleSmall = HeadingH3, // Re-using, adjust if specific title styles exist
)