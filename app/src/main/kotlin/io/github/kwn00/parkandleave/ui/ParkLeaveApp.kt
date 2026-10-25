package io.github.kwn00.parkandleave.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ParkLeaveApp(viewModel: ParkLeaveViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state.screen) {
        AppScreen.SUMMARY -> SummaryScreen(
            state = state,
            onOpenDebug = viewModel::openDebug,
            onManualCheck = viewModel::requestManualCheck,
        )
        AppScreen.DEBUG -> DebugScreen(
            state = state,
            onBack = viewModel::closeDebug,
            onLive = viewModel::useLive,
            onFake = viewModel::useFake,
            onRefresh = viewModel::refresh,
            onPreset = viewModel::applyPreset,
            onGear = viewModel::setFakeGear,
            onCloseWindows = viewModel::closeFakeWindows,
            onReadingOff = viewModel::turnOffFakeReadingLights,
            onCloseTrunk = viewModel::closeFakeTrunk,
        )
    }
}
