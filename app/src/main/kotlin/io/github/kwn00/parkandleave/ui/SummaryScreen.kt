package io.github.kwn00.parkandleave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kwn00.parkandleave.designsystem.LocalParkLeaveColors
import io.github.kwn00.parkandleave.domain.CheckItem
import io.github.kwn00.parkandleave.domain.Gear
import io.github.kwn00.parkandleave.domain.SessionPhase
import io.github.kwn00.parkandleave.domain.Severity

@Composable
fun SummaryScreen(
    state: ParkUiState,
    onOpenDebug: () -> Unit,
    onManualCheck: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalParkLeaveColors.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
    ) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                SummaryColumn(state, onOpenDebug, onManualCheck, Modifier.weight(1f))
                IssueColumn(state, Modifier.weight(1.1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryColumn(state, onOpenDebug, onManualCheck, Modifier.fillMaxWidth())
                IssueColumn(state, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    state: ParkUiState,
    onOpenDebug: () -> Unit,
    onManualCheck: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalParkLeaveColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Park & Leave", style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
            Text(gearLabel(state), style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        }
        when (state.session.phase) {
            SessionPhase.FROZEN -> {
                Text(
                    "주행 중에는 점검을 멈춥니다",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onWarning,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SessionPhase.IDLE -> {
                Text(
                    if (state.gear.gear == Gear.UNKNOWN) {
                        "기어를 아직 읽지 못했습니다"
                    } else {
                        "기어를 P에 두면 점검을 시작합니다"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.showManualCheck) {
                    WideButton("지금 점검", onManualCheck)
                }
            }
            SessionPhase.ACTIVE -> {
                if (state.scanning || state.report == null) {
                    Text("점검 중", style = MaterialTheme.typography.displaySmall, color = colors.onSurface)
                } else {
                    val report = state.report
                    Text(report.headline, style = MaterialTheme.typography.displaySmall, color = colors.onSurface)
                    report.clearanceLine?.let { line ->
                        Text(line, style = MaterialTheme.typography.headlineMedium, color = colors.onOk, maxLines = 1)
                    }
                    report.tomorrowLine?.let { line ->
                        Text(line, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, maxLines = 2)
                    }
                    if (report.items.isNotEmpty() && !state.controlAvailable) {
                        Text(
                            "직접 조치하면 카드가 갱신됩니다",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceMuted,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
        WideButton("신호 디버그", onOpenDebug)
    }
}

@Composable
private fun IssueColumn(state: ParkUiState, modifier: Modifier) {
    val items = if (state.session.phase == SessionPhase.ACTIVE) state.report?.items.orEmpty() else emptyList()
    if (items.isEmpty()) return
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id.name }) { item ->
            CheckCard(item)
        }
    }
}

@Composable
private fun CheckCard(item: CheckItem) {
    val colors = LocalParkLeaveColors.current
    val background = when (item.severity) {
        Severity.WARNING -> colors.warning
        Severity.CAUTION -> colors.caution
    }
    val foreground = when (item.severity) {
        Severity.WARNING -> colors.onWarning
        Severity.CAUTION -> colors.onCaution
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(item.title, style = MaterialTheme.typography.titleLarge, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.detail, style = MaterialTheme.typography.bodyLarge, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(item.guidance, style = MaterialTheme.typography.bodyMedium, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WideButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun gearLabel(state: ParkUiState): String {
    val raw = state.gear.rawValue?.toString() ?: "-"
    return "$raw ${state.gear.gear.name}"
}
