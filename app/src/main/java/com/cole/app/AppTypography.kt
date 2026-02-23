package com.cole.app

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Variable 폰트는 FontVariation.weight()로 axis를 직접 지정해야 weight가 적용됨
// FontWeight 파라미터만으로는 Variable 폰트의 wght axis가 동작하지 않음
@OptIn(ExperimentalTextApi::class)
val SuitVariable = FontFamily(
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(100)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(200)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900)),
    ),
)

object AppTypography {
    val Display1  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 62.sp)
    val Display2  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 44.sp)
    val Display3  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 36.sp)
    val HeadingH1 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold,  fontSize = 24.sp, lineHeight = 32.sp)
    val HeadingH2 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold,  fontSize = 22.sp, lineHeight = 32.sp)
    val HeadingH3 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold,  fontSize = 18.sp, lineHeight = 26.sp)
    val BodyMedium  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Medium,   fontSize = 15.sp, lineHeight = 24.sp)
    val BodyRegular = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 24.sp)
    val BodyBold    = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 24.sp)
    val Caption1  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 19.sp)
    val Caption2  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Bold,      fontSize = 13.sp, lineHeight = 22.sp)
    val Label     = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 12.sp)
    val ButtonLarge = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Bold,     fontSize = 16.sp, lineHeight = 22.sp)
    val ButtonSmall = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp)
    val Disclaimer  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp)
    val Input       = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Normal,   fontSize = 15.sp, lineHeight = 20.sp)
    val TabSelected   = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Bold,   fontSize = 14.sp, lineHeight = 22.sp)
    val TabUnselected = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp)
}
