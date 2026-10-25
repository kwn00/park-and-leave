package io.github.kwn00.parkandleave.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Sizes are for the right-hand app pane, about 1680 x 1200 dp, read at arm's length.
 * The pane shows at roughly 0.4x on the physical display, so 48 sp here reads like
 * a 20 sp title on a phone.
 *
 * Hangul comes from the CJK fallback font, whose fallback line spacing keeps a single
 * line at about 1.45 em even when [TextStyle.lineHeight] is smaller. Budget vertical
 * space with that height (a 48 sp title takes 70 dp), not with the line height token.
 */
@Immutable
data class ParkLeaveType(
    val brand: TextStyle,
    val gear: TextStyle,
    val headerAction: TextStyle,
    val headline: TextStyle,
    val subline: TextStyle,
    val heroTitle: TextStyle,
    val heroSubline: TextStyle,
    val status: TextStyle,
    val sectionLabel: TextStyle,
    val cardTitle: TextStyle,
    val cardBody: TextStyle,
    val cardHint: TextStyle,
    val chip: TextStyle,
    val note: TextStyle,
    val noteEmphasis: TextStyle,
    val button: TextStyle,
)

private val trimmed = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

private const val TABULAR = "tnum"

private fun style(
    size: TextUnit,
    lineHeight: TextUnit,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
    features: String? = null,
): TextStyle = TextStyle(
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = weight,
    letterSpacing = tracking,
    fontFeatureSettings = features,
    lineHeightStyle = trimmed,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

fun parkLeaveType(): ParkLeaveType = ParkLeaveType(
    brand = style(28.sp, 36.sp, FontWeight.SemiBold, tracking = (-0.2).sp),
    gear = style(28.sp, 36.sp, FontWeight.SemiBold, tracking = 1.2.sp, features = TABULAR),
    headerAction = style(28.sp, 36.sp, FontWeight.Medium),
    headline = style(80.sp, 88.sp, FontWeight.Bold, tracking = (-2).sp, features = TABULAR),
    subline = style(32.sp, 40.sp, FontWeight.Normal),
    heroTitle = style(120.sp, 128.sp, FontWeight.Bold, tracking = (-2.5).sp),
    heroSubline = style(52.sp, 64.sp, FontWeight.SemiBold, tracking = (-0.5).sp),
    status = style(72.sp, 88.sp, FontWeight.Bold, tracking = (-1.5).sp),
    sectionLabel = style(30.sp, 36.sp, FontWeight.Medium),
    cardTitle = style(48.sp, 56.sp, FontWeight.SemiBold, tracking = (-0.8).sp, features = TABULAR),
    cardBody = style(34.sp, 44.sp, FontWeight.Normal, tracking = (-0.3).sp, features = TABULAR),
    cardHint = style(32.sp, 40.sp, FontWeight.SemiBold, tracking = (-0.4).sp),
    chip = style(30.sp, 36.sp, FontWeight.Medium),
    note = style(30.sp, 36.sp, FontWeight.Normal),
    noteEmphasis = style(30.sp, 36.sp, FontWeight.Medium),
    button = style(34.sp, 44.sp, FontWeight.SemiBold),
)

internal val DefaultParkLeaveType = parkLeaveType()

val LocalParkLeaveType = staticCompositionLocalOf { DefaultParkLeaveType }

/** Material roles for screens that still use [androidx.compose.material3.MaterialTheme], such as the debug list. */
fun parkTypography(): Typography = Typography(
    displayLarge = style(96.sp, 112.sp, FontWeight.Bold),
    displaySmall = style(56.sp, 64.sp, FontWeight.Bold, features = TABULAR),
    headlineMedium = style(40.sp, 48.sp, FontWeight.SemiBold),
    titleLarge = style(32.sp, 40.sp, FontWeight.SemiBold),
    bodyLarge = style(28.sp, 36.sp, FontWeight.Normal),
    bodyMedium = style(24.sp, 32.sp, FontWeight.Normal),
    labelLarge = style(24.sp, 32.sp, FontWeight.Medium),
)
