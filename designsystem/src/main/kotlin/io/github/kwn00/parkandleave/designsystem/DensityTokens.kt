package io.github.kwn00.parkandleave.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Summary spacing for one band of card rows. The summary never scrolls, so as rows are
 * added the padding, gaps and icon tile give way first, then the title and headline step
 * down one size. Body stays at 34 sp and hints at 32 sp. Card text is not line-boxed:
 * Hangul still draws at about 1.45 em, and a card clips anything past its padding.
 * Horizontal card padding is fixed in [ParkLeaveSpace.cardPadding] so text width never changes.
 */
@Immutable
data class SummaryDensity(
    val paneTop: Dp,
    val paneBottom: Dp,
    val headerGap: Dp,
    val headlineGap: Dp,
    val compactHeadline: Boolean,
    val gutter: Dp,
    val cardPadding: Dp,
    /** The tile sits beside the title instead of above it, which saves a whole tile of height. */
    val iconBesideTitle: Boolean,
    val iconTile: Dp,
    val icon: Dp,
    val tileGap: Dp,
    val compactTitle: Boolean,
    /** Four-row title, one step under [compactTitle], still above the body size. */
    val denseTitle: Boolean = false,
    val titleGap: Dp,
    val bodyLineHeight: TextUnit,
    val ruleGap: Dp,
    val hintGap: Dp,
    val hintLineHeight: TextUnit,
    val panelGap: Dp,
    val panelPadding: Dp,
    /** "정상으로 확인된 항목" leads the chip row instead of sitting above it. */
    val labelBesideChips: Boolean,
    val chipHeight: Dp,
    val panelRuleGap: Dp,
    val sectionGap: Dp,
) {
    companion object {
        /** One row of cards, or none. */
        val Regular = SummaryDensity(
            paneTop = ParkLeaveSpace.x3,
            paneBottom = ParkLeaveSpace.x3,
            headerGap = ParkLeaveSpace.x3,
            headlineGap = 28.dp,
            compactHeadline = false,
            gutter = ParkLeaveSpace.x2,
            cardPadding = 20.dp,
            iconBesideTitle = false,
            iconTile = ParkLeaveDimens.iconTile,
            icon = ParkLeaveDimens.icon,
            tileGap = ParkLeaveSpace.x2,
            compactTitle = false,
            titleGap = ParkLeaveSpace.x1,
            bodyLineHeight = 44.sp,
            ruleGap = 14.dp,
            hintGap = ParkLeaveSpace.x1Half,
            hintLineHeight = 46.sp,
            panelGap = ParkLeaveSpace.x2,
            panelPadding = ParkLeaveSpace.x3,
            labelBesideChips = false,
            chipHeight = ParkLeaveDimens.chipHeight,
            panelRuleGap = 14.dp,
            sectionGap = ParkLeaveSpace.x3,
        )

        /** Two rows: five to eight cards, where up to ten chips can still wrap to a second row. */
        val Medium = Regular.copy(
            headerGap = ParkLeaveSpace.x2,
            headlineGap = ParkLeaveSpace.x3,
            cardPadding = 16.dp,
            iconTile = 52.dp,
            icon = 32.dp,
            tileGap = 10.dp,
            titleGap = 6.dp,
            ruleGap = 10.dp,
            hintGap = 8.dp,
            hintLineHeight = 46.sp,
            panelGap = 14.dp,
            panelPadding = 20.dp,
            chipHeight = 48.dp,
            panelRuleGap = ParkLeaveSpace.x1Half,
            paneBottom = 20.dp,
            sectionGap = 20.dp,
        )

        /** Three rows: nine to twelve cards. */
        val Compact = SummaryDensity(
            paneTop = ParkLeaveSpace.x3,
            paneBottom = 20.dp,
            headerGap = ParkLeaveSpace.x2,
            headlineGap = 20.dp,
            compactHeadline = false,
            gutter = 14.dp,
            cardPadding = 14.dp,
            iconBesideTitle = true,
            iconTile = 40.dp,
            icon = 26.dp,
            tileGap = ParkLeaveSpace.x1Half,
            compactTitle = true,
            titleGap = 4.dp,
            bodyLineHeight = 44.sp,
            ruleGap = 12.dp,
            hintGap = 8.dp,
            hintLineHeight = 46.sp,
            panelGap = 12.dp,
            panelPadding = 14.dp,
            labelBesideChips = true,
            chipHeight = 48.dp,
            panelRuleGap = ParkLeaveSpace.x1Half,
            sectionGap = 20.dp,
        )

        /** Four rows: thirteen or fourteen cards, every check except the grouped telltale. */
        val Dense = SummaryDensity(
            paneTop = ParkLeaveSpace.x1,
            paneBottom = ParkLeaveSpace.x1,
            headerGap = ParkLeaveSpace.x1,
            headlineGap = ParkLeaveSpace.x1,
            compactHeadline = true,
            gutter = ParkLeaveSpace.x1,
            cardPadding = 10.dp,
            iconBesideTitle = true,
            iconTile = 36.dp,
            icon = 22.dp,
            tileGap = ParkLeaveSpace.x1,
            compactTitle = true,
            denseTitle = true,
            titleGap = 2.dp,
            bodyLineHeight = 40.sp,
            ruleGap = 10.dp,
            hintGap = 8.dp,
            hintLineHeight = 46.sp,
            panelGap = ParkLeaveSpace.x1,
            panelPadding = 10.dp,
            labelBesideChips = true,
            chipHeight = 48.dp,
            panelRuleGap = 6.dp,
            sectionGap = ParkLeaveSpace.x2,
        )

        fun forRows(rows: Int): SummaryDensity = when {
            rows <= 1 -> Regular
            rows == 2 -> Medium
            rows == 3 -> Compact
            else -> Dense
        }
    }
}
