package io.github.kwn00.parkandleave.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenOrientation
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import org.junit.Rule
import org.junit.Test

class SummaryScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1682,
            screenHeight = 980,
            xdpi = 160,
            ydpi = 160,
            orientation = ScreenOrientation.LANDSCAPE,
            density = Density.MEDIUM,
        ),
        showSystemUi = false,
    )

    @Test
    fun issuesDay() = shoot("summary-issues-day", dark = false, state = previewIssues())

    @Test
    fun allClearDay() = shoot("summary-all-clear-day", dark = false, state = previewAllClear(resolved = false))

    @Test
    fun resolvedDay() = shoot("summary-resolved-day", dark = false, state = previewAllClear(resolved = true))

    @Test
    fun issuesNight() = shoot("summary-issues-night", dark = true, state = previewIssues())

    private fun shoot(name: String, dark: Boolean, state: ParkUiState) {
        paparazzi.snapshot(name = name) {
            ParkLeaveTheme(darkTheme = dark) {
                SummaryScreen(state, onOpenDebug = {}, onManualCheck = {})
            }
        }
    }
}
