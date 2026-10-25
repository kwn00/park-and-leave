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
    fun oneCardDay() = shoot("summary-one-day", dark = false, count = 1)

    @Test
    fun oneCardNight() = shoot("summary-one-night", dark = true, count = 1)

    @Test
    fun twoCardsDay() = shoot("summary-two-day", dark = false, count = 2)

    @Test
    fun twoCardsNight() = shoot("summary-two-night", dark = true, count = 2)

    @Test
    fun threeCardsDay() = shoot("summary-three-day", dark = false, count = 3)

    @Test
    fun threeCardsNight() = shoot("summary-three-night", dark = true, count = 3)

    @Test
    fun fiveCardsDay() = shoot("summary-five-day", dark = false, count = 5)

    @Test
    fun fiveCardsNight() = shoot("summary-five-night", dark = true, count = 5)

    private fun shoot(name: String, dark: Boolean, count: Int) {
        val unchecked = listOf(
            "워셔액: 확인할 수 없음",
            "경고등: 이 차량에서 앱이 읽을 수 없음",
            "주행 거리: 확인할 수 없음",
        )
        paparazzi.snapshot(name = name) {
            ParkLeaveTheme(darkTheme = dark) {
                SummaryScreen(
                    previewCardCount(count, unchecked),
                    onOpenDebug = {},
                    onManualCheck = {},
                )
            }
        }
    }
}
