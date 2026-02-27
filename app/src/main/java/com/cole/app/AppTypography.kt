package com.cole.app

import com.cole.app.R
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
        weight = FontWeight(480),
        variationSettings = FontVariation.Settings(FontVariation.weight(480)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(560),
        variationSettings = FontVariation.Settings(FontVariation.weight(560)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(624),
        variationSettings = FontVariation.Settings(FontVariation.weight(624)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(630),
        variationSettings = FontVariation.Settings(FontVariation.weight(630)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(660),
        variationSettings = FontVariation.Settings(FontVariation.weight(660)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(705),
        variationSettings = FontVariation.Settings(FontVariation.weight(705)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(720),
        variationSettings = FontVariation.Settings(FontVariation.weight(720)),
    ),
    Font(
        resId = R.font.suit_variable,
        weight = FontWeight(760),
        variationSettings = FontVariation.Settings(FontVariation.weight(760)),
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
    /** ===== 디자인 가이드 기준 (값 변동 금지) ===== */
    val Display1  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 62.sp)
    val Display2  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold,  fontSize = 32.sp, lineHeight = 44.sp)
    val Display3  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.sp)
    val HeadingH1 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 32.sp)
    val HeadingH2 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(760), fontSize = 22.sp, lineHeight = 32.sp)
    val HeadingH3 = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(720), fontSize = 18.sp, lineHeight = 26.sp)
    val BodyMedium  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(624), fontSize = 15.sp, lineHeight = 24.sp)
    val BodyRegular = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(630), fontSize = 15.sp, lineHeight = 24.sp)
    val BodyBold    = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(705), fontSize = 15.sp, lineHeight = 24.sp)
    val Caption1  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(660),    fontSize = 12.sp, lineHeight = 19.sp)
    val Caption2  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(630), fontSize = 13.sp, lineHeight = 22.sp)
    val Label     = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 12.sp)
    val ButtonLarge = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(630), fontSize = 16.sp, lineHeight = 22.sp)
    val ButtonSmall = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(480), fontSize = 12.sp, lineHeight = 16.sp)
    val Disclaimer  = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp)
    val Input       = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight(560), fontSize = 15.sp, lineHeight = 20.sp)
    val TabSelected   = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Bold,   fontSize = 14.sp, lineHeight = 22.sp)
    val TabUnselected = TextStyle(fontFamily = SuitVariable, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 22.sp)
}
