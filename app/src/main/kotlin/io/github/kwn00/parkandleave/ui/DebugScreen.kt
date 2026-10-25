package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kwn00.parkandleave.designsystem.LocalParkLeaveColors
import io.github.kwn00.parkandleave.domain.FakePreset
import io.github.kwn00.parkandleave.domain.Gear

@Composable
fun DebugScreen(
    state: ParkUiState,
    onBack: () -> Unit,
    onLive: () -> Unit,
    onFake: () -> Unit,
    onRefresh: () -> Unit,
    onPreset: (FakePreset) -> Unit,
    onGear: (Gear) -> Unit,
    onCloseWindows: () -> Unit,
    onReadingOff: () -> Unit,
    onClearRear: () -> Unit,
    onCloseTrunk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalParkLeaveColors.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("신호", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
            Text(
                "GEAR_SELECTION 원본과 차량 신호를 그대로 보여 줍니다",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                maxLines = 2,
            )
        }
        item {
            Text("Car: ${state.liveStatus}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 2)
            Text("Vehicle: ${state.vehicleStatus}", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, maxLines = 2)
            Text(
                "음성 ${state.speechMode} · 제어 ${if (state.controlAvailable) "가능" else "미연결"} · 장소 ${state.placeLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted,
                maxLines = 2,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("실측", onLive, Modifier.weight(1f))
                ActionButton("모의", onFake, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton("새로고침", onRefresh, Modifier.weight(1f))
                ActionButton("요약으로", onBack, Modifier.weight(1f))
            }
        }
        item {
            Text(
                "raw ${state.gear.rawValue ?: "null"}  ${state.gear.gear.name}",
                style = MaterialTheme.typography.displaySmall,
                color = colors.onSurface,
                maxLines = 1,
            )
            state.gear.error?.let { error ->
                Text(error, style = MaterialTheme.typography.bodyLarge, color = colors.onCritical, maxLines = 3)
            }
        }
        itemsIndexed(state.debugRows, key = { index, row -> "$index:${row.label}:${row.value}" }) { _, row ->
            Column(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(12.dp)) {
                Text(row.label, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceMuted, maxLines = 1)
                Text(
                    row.value,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (state.origin == DataOrigin.FAKE) {
            item { Text("모의 시나리오", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) }
            item { ActionButton("모두 정상", { onPreset(FakePreset.ALL_CLEAR) }, Modifier.fillMaxWidth()) }
            item { ActionButton("창문 + 독서등", { onPreset(FakePreset.WINDOW_AND_READING) }, Modifier.fillMaxWidth()) }
            item { ActionButton("뒷좌석 + 트렁크", { onPreset(FakePreset.REAR_SEAT_AND_TRUNK) }, Modifier.fillMaxWidth()) }
            item { ActionButton("낮은 배터리", { onPreset(FakePreset.LOW_SOC) }, Modifier.fillMaxWidth()) }
            item { ActionButton("도어 잠금 해제", { onPreset(FakePreset.DOORS_UNLOCKED) }, Modifier.fillMaxWidth()) }
            item { Text("모의 기어", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionButton("P", { onGear(Gear.PARK) }, Modifier.weight(1f))
                    ActionButton("R", { onGear(Gear.REVERSE) }, Modifier.weight(1f))
                    ActionButton("N", { onGear(Gear.NEUTRAL) }, Modifier.weight(1f))
                    ActionButton("D", { onGear(Gear.DRIVE) }, Modifier.weight(1f))
                    ActionButton("?", { onGear(Gear.UNKNOWN) }, Modifier.weight(1f))
                }
            }
            item { Text("해소", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) }
            item { ActionButton("창문 닫힘", onCloseWindows, Modifier.fillMaxWidth()) }
            item { ActionButton("독서등 끔", onReadingOff, Modifier.fillMaxWidth()) }
            item { ActionButton("뒷좌석 비움", onClearRear, Modifier.fillMaxWidth()) }
            item { ActionButton("트렁크 닫힘", onCloseTrunk, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}
