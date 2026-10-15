package id.project.df.dble.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.project.df.dble.core.common.mvi.collectAsEffect
import id.project.df.dble.core.designsystem.color
import id.project.df.dble.core.model.DetectedDevice
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.formatLastSeen
import id.project.df.dble.ui.theme.DBLETheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onEntryClick: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var refreshKey by remember { mutableIntStateOf(0) }
    // Label waktu relatif dihitung dari jam saat ini, bukan dari emisi Room: segarkan tiap 30 s dan saat diminta manual.
    val nowMillis by produceState(System.currentTimeMillis(), refreshKey) {
        value = System.currentTimeMillis()
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    viewModel.effect.collectAsEffect { effect ->
        when (effect) {
            is Effect.NavigateToTracking -> onEntryClick(effect.mac)
            Effect.ShowClearedMessage -> scope.launch {
                snackbarHostState.showSnackbar("Riwayat dihapus")
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        HistoryContent(
            state = state,
            nowMillis = nowMillis,
            onEntryClick = { viewModel.sendIntent(Intent.EntryClicked(it)) },
            onEntryLongClick = { viewModel.sendIntent(Intent.EntryDeleteRequested(it)) },
            onClearAll = { viewModel.sendIntent(Intent.ClearAllClicked) },
            onRefresh = { refreshKey++ },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryContent(
    state: State,
    nowMillis: Long,
    onEntryClick: (String) -> Unit,
    onEntryLongClick: (String) -> Unit,
    onClearAll: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Riwayat Perangkat", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = onRefresh) { Text("↻ Segarkan") }
                TextButton(onClick = onClearAll, enabled = state.entries.isNotEmpty()) {
                    Text("🗑 Hapus semua")
                }
            }
        }
        if (state.entries.isEmpty()) {
            Text(
                text = "Belum ada perangkat tersimpan",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.entries, key = { it.mac }) { entry ->
                HistoryRow(
                    entry = entry,
                    nowMillis = nowMillis,
                    modifier = Modifier.combinedClickable(
                        onClick = { onEntryClick(entry.mac) },
                        onLongClick = { onEntryLongClick(entry.mac) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: DetectedDevice, nowMillis: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name ?: "(Tanpa nama)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.mac,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = formatLastSeen(nowMillis, entry.lastSeenMillis),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = "● ${entry.lastZone.label}",
                style = MaterialTheme.typography.labelLarge,
                color = entry.lastZone.color(),
            )
        }
    }
}

private val PreviewState = State(
    entries = persistentListOf(
        DetectedDevice("AA:BB:CC:DD:EE:01", "Tag Kunci", -48, ProximityZone.KUAT, 0, 1_000_000),
        DetectedDevice("AA:BB:CC:DD:EE:02", null, -85, ProximityZone.SANGAT_LEMAH, 0, 400_000),
    ),
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun HistoryContentLightPreview() {
    DBLETheme(darkTheme = false) {
        HistoryContent(PreviewState, nowMillis = 1_030_000, onEntryClick = {}, onEntryLongClick = {}, onClearAll = {}, onRefresh = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun HistoryContentDarkPreview() {
    DBLETheme(darkTheme = true) {
        HistoryContent(PreviewState, nowMillis = 1_030_000, onEntryClick = {}, onEntryLongClick = {}, onClearAll = {}, onRefresh = {})
    }
}
