package io.github.kwn00.parkandleave.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class ParkLeaveType(
    val brand: TextStyle,
    val status: TextStyle,
    val support: TextStyle,
    val message: TextStyle,
    val clearance: TextStyle,
    val cardTitle: TextStyle,
    val cardTitleEmphasis: TextStyle,
    val cardBody: TextStyle,
    val cardBodyEmphasis: TextStyle,
    val cardHint: TextStyle,
    val section: TextStyle,
    val meta: TextStyle,
    val button: TextStyle,
)

private val tight = PlatformTextStyle(includeFontPadding = false)

fun parkLeaveType(): ParkLeaveType = ParkLeaveType(
    brand = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        platformStyle = tight,
    ),
    status = TextStyle(
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.4).sp,
        platformStyle = tight,
    ),
    support = TextStyle(
        fontSize = 22.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = tight,
    ),
    message = TextStyle(
        fontSize = 40.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.3).sp,
        platformStyle = tight,
    ),
    clearance = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.2).sp,
        platformStyle = tight,
    ),
    cardTitle = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.2).sp,
        platformStyle = tight,
    ),
    cardTitleEmphasis = TextStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.3).sp,
        platformStyle = tight,
    ),
    cardBody = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = tight,
    ),
    cardBodyEmphasis = TextStyle(
        fontSize = 26.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = tight,
    ),
    cardHint = TextStyle(
        fontSize = 22.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Normal,
        platformStyle = tight,
    ),
    section = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp,
        platformStyle = tight,
    ),
    meta = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.6.sp,
        platformStyle = tight,
    ),
    button = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Medium,
        platformStyle = tight,
    ),
)

fun parkTypography(type: ParkLeaveType = parkLeaveType()): Typography = Typography(
    displayLarge = type.status,
    displaySmall = type.message,
    headlineMedium = type.cardTitleEmphasis,
    titleLarge = type.cardTitle,
    bodyLarge = type.cardBody,
    bodyMedium = type.support,
    labelLarge = type.button,
)

val LocalParkLeaveType = staticCompositionLocalOf { parkLeaveType() }
