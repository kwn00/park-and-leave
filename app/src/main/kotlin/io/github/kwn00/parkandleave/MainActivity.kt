package io.github.kwn00.parkandleave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.kwn00.parkandleave.designsystem.ParkLeaveTheme
import io.github.kwn00.parkandleave.ui.ParkLeaveApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParkLeaveTheme {
                ParkLeaveApp()
            }
        }
    }
}
