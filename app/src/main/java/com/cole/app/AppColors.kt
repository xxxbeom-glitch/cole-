package com.cole.app

import androidx.compose.ui.graphics.Color

object AppColors {
    // Grey Scale
    val Grey50 = Color(0xFFFAFAFA)
    val Grey100 = Color(0xFFF5F5F5)
    val Grey150 = Color(0xFFF0F0F0)
    val Grey200 = Color(0xFFEEEEEE)
    val Grey250 = Color(0xFFE8E8E8)
    val Grey300 = Color(0xFFE0E0E0)
    val Grey350 = Color(0xFFD6D6D6)
    val Grey400 = Color(0xFFC2C2C2)
    val Grey450 = Color(0xFFA3A3A3)
    val Grey500 = Color(0xFF858585)
    val Grey550 = Color(0xFF666666)
    val Grey600 = Color(0xFF4D4D4D)
    val Grey650 = Color(0xFF333333)
    val Grey700 = Color(0xFF262626)
    val Grey750 = Color(0xFF1F1F1F)
    val Grey800 = Color(0xFF141414)
    val Grey850 = Color(0xFF0A0A0A)
    val Grey900 = Color(0xFF000000)

    val White900 = Color(0xFFFFFFFF)

    val Primary50 = Color(0xFFF0EEFC)
    val Primary100 = Color(0xFFE9E5FA)
    val Primary200 = Color(0xFFE9EBF8)
    val Primary300 = Color(0xFF6C54DD)
    val Primary400 = Color(0xFF614CC7)
    val Primary500 = Color(0xFF5643B1)
    val Primary600 = Color(0xFF513FA6)
    val Primary700 = Color(0xFF413285)
    val Primary800 = Color(0xFF312663)
    val Primary900 = Color(0xFF261D4D)

    val Blue100 = Color(0xFFE0E1F7)
    val Blue200 = Color(0xFFBFC1EE)
    val Blue300 = Color(0xFF3136C9)
    val Blue400 = Color(0xFF2C31B5)
    val Blue500 = Color(0xFF272BA1)
    val Blue600 = Color(0xFF252997)
    val Blue700 = Color(0xFF1D2079)
    val Blue800 = Color(0xFF16185A)
    val Blue900 = Color(0xFF111346)

    val Green100 = Color(0xFFE2F1EE)
    val Green200 = Color(0xFFC3E1DD)
    val Green300 = Color(0xFF3C9F90)
    val Green400 = Color(0xFF368F82)
    val Green500 = Color(0xFF307F73)
    val Green600 = Color(0xFF2D776C)
    val Green700 = Color(0xFF245F56)
    val Green800 = Color(0xFF1B4841)
    val Green900 = Color(0xFF153832)

    val Red100 = Color(0xFFFBE6E6)
    val Red200 = Color(0xFFF6CCCC)
    val Red300 = Color(0xFFE35959)
    val Red400 = Color(0xFFCC5050)
    val Red500 = Color(0xFFB64747)
    val Red600 = Color(0xFFAA4343)
    val Red700 = Color(0xFF883535)
    val Red800 = Color(0xFF662828)
    val Red900 = Color(0xFF4F1F1F)

    val Orange300 = Color(0xFFFF9238)
    val Orange400 = Color(0xFFFF9238)
    val Orange500 = Color(0xFFFF9238)

    val Yellow300 = Color(0xFFFF9238)
    val Yellow400 = Color(0xFFFF9238)
    val Yellow500 = Color(0xFFFF9238)

    val TextPrimary = Grey900
    val TextBody = Grey750
    val TextSecondary = Grey700
    val TextTertiary = Grey650
    val TextCaption = Grey600
    val TextDisclaimer = Grey500
    val TextPlaceholder = Grey500
    val TextDisabled = Grey400
    val TextInvert = White900
    val TextHighlight = Primary300
    val TextError = Red500

    val BorderDefault = Grey350
    val BorderDivider = Grey550
    val BorderInfoBox = Grey300

    val InteractiveTabUnselected = Grey200
    val InteractiveTabSelected = White900
    val InteractiveTabTabBg = Grey200
    val InteractiveTabTextSelected = Grey750
    val InteractiveTabTextUnselected = Grey500
    val InteractiveRadioBgSelected = Primary300
    val InteractiveRadioBgUnselected = Grey400
    val InteractiveRadioBorderSelected = White900
    val InteractiveRadioBorderUnselected = Grey350
    val InteractiveCheckBoxBgChecked = Primary300
    val InteractiveCheckBoxBgUnchecked = White900
    val InteractiveCheckBoxBorderUnchecked = Grey350
    val InteractiveCheckBoxBorderCheck = White900
    val InteractiveToggleBgOn = White900
    val InteractiveToggleBgOff = White900
    val InteractiveToggleBgHandle = White900

    val ChartLabelLabel = White900
    val ChartGuidelineGuideline = White900
    val ChartGuideline = Grey350
    val ChartBarDefault = White900
    val ChartTrackBackground = Grey200
    val ChartTrackFill = Primary300
    val ChartTrackDotActive = Primary500
    val ChartTrackDotInactive = Grey350
    val ChartHandleBorder = Grey250
    val ChartHandleBg = White900
    val ChartHandleArrow = Grey650

    val ChartLevelBad1 = Red500
    val ChartLevelBad2 = Red400
    val ChartLevelBad3 = Red300

    /** Label/Danger (Figma 258:3187) — 위험 배지 배경 #fd4949 */
    val LabelDangerBg = Color(0xFFFD4949)
    val ChartLevelWarning1 = Orange500
    val ChartLevelWarning2 = Orange400
    val ChartLevelWarning3 = Orange300
    val ChartLevelNormal1 = Yellow500
    val ChartLevelNormal2 = Yellow400
    val ChartLevelNormal3 = Yellow300
    val ChartLevelGood1 = Blue500
    val ChartLevelGood2 = Blue400
    val ChartLevelGood3 = Blue300
    val ChartLevelExcellent1 = Green500
    val ChartLevelExcellent2 = Green400
    val ChartLevelExcellent3 = Green300

    val FormInputBgError = Grey150
    val FormInputBgDisabled = Grey350
    val FormInputBgFocus = Grey100
    val FormInputBgDefault = Grey150
    val FormBorderFocus = Primary300
    val FormTextPlaceholder = Grey450
    val FormTextActive = Grey750
    val FormTextError = Red300
    val FormTextLabel = Grey700
    val FormTextDisabled = Grey400
    val FormTextValue = Grey750
    val FormTextHelper = Grey600

    val SurfaceBackgroundBackground = Grey100
    val SurfaceBackgroundLogin = Grey100
    val SurfaceBackgroundCard = White900
    val SurfaceBackgroundBottomSheet = Grey100
    val SurfaceBackgroundInfoBox = Grey150
    val SurfaceBackgroundOverlay = Grey900

    val ButtonSecondaryTextDefault = Grey600
    val ButtonSecondaryTextDisabled = Grey400
    val ButtonSecondaryBgDefault = White900
    val ButtonSecondaryBgHover = Grey200
    val ButtonSecondaryBgDisabled = Grey250
    val ButtonSecondaryBorderDefault = Grey350
    val ButtonSecondaryBorderHover = Grey350
    val ButtonSecondaryBorderDisabled = Grey400
    val ButtonGhostTextDefault = Grey600
    val ButtonGhostTextDisabled = Grey350
    val ButtonGhostTextHover = Grey650
    val ButtonGhostBgDefault = Grey100
    val ButtonGhostBgHover = Grey150
    val ButtonGhostBgDisabled = Grey100
    val ButtonPrimaryTextDefault = White900
    val ButtonPrimaryTextDisabled = Grey400
    val ButtonPrimaryBgDefault = Primary300
    val ButtonPrimaryBgPressed = Primary400
    val ButtonPrimaryBgDisabled = Grey250
}
