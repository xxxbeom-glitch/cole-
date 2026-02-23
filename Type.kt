import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define the SUIT Variable font family
val SuitVariable = FontFamily(
    Font(res.font.suit_variable, FontWeight.Normal), // Regular
    Font(res.font.suit_variable, FontWeight.Medium),
    Font(res.font.suit_variable, FontWeight.SemiBold),
    Font(res.font.suit_variable, FontWeight.Bold),
    Font(res.font.suit_variable, FontWeight.ExtraBold)
)

// Define the text styles based on Figma
object AppTypography {
    val Display1 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 62.sp
    )
    val Display2 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 44.sp
    )
    val Display3 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    )
    val HeadingH1 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    )
    val HeadingH2 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 32.sp
    )
    val HeadingH3 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    )
    val BodyMedium = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )
    val BodyRegular = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )
    val BodyBold = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 24.sp
    )
    val Caption1 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 19.sp
    )
    val Caption2 = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 22.sp
    )
    val Label = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 12.sp,
        lineHeight = 12.sp
    )
    val ButtonLarge = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )
    val ButtonSmall = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val Disclaimer = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
    val Input = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Normal, // Assuming Regular maps to Normal
        fontSize = 15.sp,
        lineHeight = 20.sp
    )
    val TabSelected = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
    val TabUnselected = TextStyle(
        fontFamily = SuitVariable,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
}