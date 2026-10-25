package io.github.kwn00.parkandleave.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Summary spacing for one band of card rows. The summary never scrolls, so as rows are
 * added the padding, gaps and icon tile give way first, then the title and headline step
 * down one size. Body and hint sizes never change; only their line boxes tighten.
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
            hintLineHeight = 40.sp,
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
            cardPadding = 18.dp,
            iconTile = 56.dp,
            icon = 36.dp,
            tileGap = 14.dp,
            ruleGap = ParkLeaveSpace.x1Half,
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
            cardPadding = 18.dp,
            iconBesideTitle = true,
            iconTile = 40.dp,
            icon = 26.dp,
            tileGap = ParkLeaveSpace.x1Half,
            compactTitle = true,
            titleGap = 6.dp,
            bodyLineHeight = 44.sp,
            ruleGap = ParkLeaveSpace.x1Half,
            hintGap = 10.dp,
            hintLineHeight = 40.sp,
            panelGap = 14.dp,
            panelPadding = 18.dp,
            labelBesideChips = true,
            chipHeight = 48.dp,
            panelRuleGap = ParkLeaveSpace.x1Half,
            sectionGap = 20.dp,
        )

        /** Four rows: thirteen or fourteen cards, every check except the grouped telltale. */
        val Dense = SummaryDensity(
            paneTop = ParkLeaveSpace.x2,
            paneBottom = ParkLeaveSpace.x1Half,
            headerGap = ParkLeaveSpace.x1,
            headlineGap = 14.dp,
            compactHeadline = true,
            gutter = ParkLeaveSpace.x1Half,
            cardPadding = ParkLeaveSpace.x1Half,
            iconBesideTitle = true,
            iconTile = 40.dp,
            icon = 26.dp,
            tileGap = ParkLeaveSpace.x1Half,
            compactTitle = true,
            titleGap = 2.dp,
            bodyLineHeight = 40.sp,
            ruleGap = ParkLeaveSpace.x1,
            hintGap = ParkLeaveSpace.x1,
            hintLineHeight = 36.sp,
            panelGap = ParkLeaveSpace.x1Half,
            panelPadding = 14.dp,
            labelBesideChips = true,
            chipHeight = 44.dp,
            panelRuleGap = ParkLeaveSpace.x1,
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
