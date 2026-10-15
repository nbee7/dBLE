package id.project.df.dble.feature.scanner

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import id.project.df.dble.core.model.isStale
import id.project.df.dble.core.model.ScanConfig
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.project.df.dble.core.common.mvi.collectAsEffect
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.designsystem.color
import id.project.df.dble.core.domain.FilterField
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedDevice
import id.project.df.dble.core.model.formatDistance
import id.project.df.dble.core.ui.openLocationSettings
import id.project.df.dble.core.ui.hasSettingsAction
import id.project.df.dble.core.ui.openSettingsFor
import id.project.df.dble.core.ui.rememberPermissionRequester
import id.project.df.dble.ui.theme.DBLETheme
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ScannerScreen(
    onDeviceClick: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermissions = rememberPermissionRequester { viewModel.sendIntent(Intent.StartScanClicked) }
    val nowMillis by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1_000)
            value = System.currentTimeMillis()
        }
    }
    viewModel.effect.collectAsEffect { effect ->
        when (effect) {
            is Effect.NavigateToTracking -> onDeviceClick(effect.mac)
            is Effect.RequestPermissions -> requestPermissions(effect.permissions)
            Effect.OpenLocationSettings -> context.openLocationSettings()
        }
    }
    ScannerContent(
        state = state,
        nowMillis = nowMillis,
        onQueryChange = { viewModel.sendIntent(Intent.QueryChanged(it)) },
        onFilterFieldChange = { viewModel.sendIntent(Intent.FilterFieldChanged(it)) },
        onMinRssiToggle = { viewModel.sendIntent(Intent.MinRssiToggled(it)) },
        onStartScan = { viewModel.sendIntent(Intent.StartScanClicked) },
        onStopScan = { viewModel.sendIntent(Intent.StopScanClicked) },
        onDeviceClick = { viewModel.sendIntent(Intent.DeviceClicked(it)) },
        onBannerClick = { context.openSettingsFor(it) },
        onSettingsClick = { viewModel.sendIntent(Intent.SettingsClicked) },
        onSettingsDismiss = { viewModel.sendIntent(Intent.SettingsDismissed) },
        onScanConfigApply = { viewModel.sendIntent(Intent.ScanConfigApplied(it)) },
    )
}

@Composable
fun ScannerContent(
    state: State,
    nowMillis: Long,
    onQueryChange: (String) -> Unit,
    onFilterFieldChange: (FilterField) -> Unit,
    onMinRssiToggle: (Boolean) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceClick: (String) -> Unit,
    onBannerClick: (StopReason) -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    onScanConfigApply: (ScanConfig) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fabY = maxHeight * 2f / 3f
        Column(modifier = Modifier.fillMaxSize()) {
            state.banner?.let { reason ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = reason.hasSettingsAction()) { onBannerClick(reason) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = bannerText(reason),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    shape = CircleShape,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Cari nama / MAC…") },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = "Pengaturan pemindaian")
                }
            }
            if (state.showSettings) {
                ScanSettingsDialog(
                    config = state.scanConfig,
                    onApply = onScanConfigApply,
                    onDismiss = onSettingsDismiss,
                )
            }
            FilterChipRow(
                state = state,
                onFilterFieldChange = onFilterFieldChange,
                onMinRssiToggle = onMinRssiToggle,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.devices, key = { it.mac }) { device ->
                    DeviceRow(
                        device = device,
                        stale = isStale(nowMillis, device.lastSeenMillis),
                        onClick = { onDeviceClick(device.mac) },
                    )
                }
            }
        }
        ScanFab(
            isScanning = state.isScanning,
            fabY = fabY,
            onStartScan = onStartScan,
            onStopScan = onStopScan,
        )
    }
}

@Composable
private fun FilterChipRow(
    state: State,
    onFilterFieldChange: (FilterField) -> Unit,
    onMinRssiToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = state.filterField == FilterField.NAME,
            onClick = { onFilterFieldChange(FilterField.NAME) },
            label = { Text("Nama") },
        )
        FilterChip(
            selected = state.filterField == FilterField.MAC,
            onClick = { onFilterFieldChange(FilterField.MAC) },
            label = { Text("MAC") },
        )
        FilterChip(
            selected = state.minRssiEnabled,
            onClick = { onMinRssiToggle(!state.minRssiEnabled) },
            label = { Text("Min ${rssiText(state.minRssi)}") },
        )
    }
}

@Composable
private fun DeviceRow(device: TrackedDevice, stale: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.alpha(if (stale) 0.4f else 1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = device.name ?: "(Tanpa nama)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = device.mac,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = rssiText(device.rawRssi),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "● ${zoneLabel(device.zone)} · ${formatDistance(device.distanceMeters, device.zone)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = device.zone.color(),
                )
            }
        }
    }
}

@Composable
private fun ScanFab(
    isScanning: Boolean,
    fabY: Dp,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val fabColor = Color(0xFF16A34A)
        val ringColor = Color(0xFF4ADE80)
        if (isScanning) {
            val transition = rememberInfiniteTransition(label = "scanPulse")
            val outerAlpha by transition.animateFloat(
                initialValue = 1f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                label = "ringOuter",
            )
            val innerAlpha by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
                label = "ringInner",
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = fabY - 12.dp)
                    .size(74.dp)
                    .border(1.dp, ringColor.copy(alpha = outerAlpha * 0.3f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = fabY - 6.dp)
                    .size(62.dp)
                    .border(1.dp, ringColor.copy(alpha = innerAlpha * 0.5f), CircleShape),
            )
        }
        Surface(
            shape = CircleShape,
            color = Color(0xFF0B1220).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, ringColor.copy(alpha = 0.4f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = fabY + 40.dp),
        ) {
            Text(
                text = if (isScanning) "Ketuk untuk berhenti memindai" else "Ketuk untuk mulai memindai",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ringColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        FloatingActionButton(
            onClick = if (isScanning) onStopScan else onStartScan,
            shape = CircleShape,
            containerColor = fabColor,
            contentColor = Color(0xFF04170C),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = fabY)
                .size(50.dp),
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isScanning) "Berhenti memindai" else "Mulai memindai",
            )
        }
    }
}

private fun zoneLabel(zone: ProximityZone): String = when (zone) {
    ProximityZone.SANGAT_KUAT -> "Sangat Dekat"
    ProximityZone.KUAT -> "Dekat"
    else -> zone.label
}

private fun rssiText(rssi: Int): String = "−${abs(rssi)} dBm"

internal fun bannerText(reason: StopReason): String = when (reason) {
    StopReason.BLUETOOTH_OFF -> "Bluetooth dimatikan — ketuk untuk membuka pengaturan Bluetooth"
    StopReason.PERMISSION_REVOKED -> "Izin pemindaian belum diberikan — ketuk untuk membuka pengaturan aplikasi"
    StopReason.LOCATION_OFF -> "Layanan lokasi mati — ketuk untuk membuka pengaturan lokasi"
    StopReason.SCAN_FAILED_ALREADY_STARTED -> "Pemindaian sudah berjalan"
    StopReason.SCAN_FAILED_REGISTRATION -> "Pemindaian gagal didaftarkan — coba lagi"
    StopReason.SCAN_FAILED_TOO_FREQUENTLY -> "Tunggu beberapa detik lalu coba lagi"
    StopReason.SCAN_FAILED_OTHER -> "Pemindaian gagal — coba lagi"
    StopReason.USER_STOPPED -> ""
}

private val PreviewDevices: ImmutableList<TrackedDevice> = persistentListOf(
    TrackedDevice("8C:1D:96:4F:2A:71", "JBL Flip 6", -46, -44.0, ProximityZone.KUAT, 2.4, 0L),
    TrackedDevice("C8:8A:2B:5E:90:14", "Mi Band 8", -63, -61.0, ProximityZone.CUKUP, 5.5, 0L),
    TrackedDevice("0F:24:D7:33:9E:C8", null, -85, -83.0, ProximityZone.SANGAT_LEMAH, 22.0, 0L),
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun ScannerContentLightPreview() {
    DBLETheme(dynamicColor = false) {
        ScannerContent(
            state = State(isScanning = true, devices = PreviewDevices),
            nowMillis = 0L,
            onQueryChange = {},
            onFilterFieldChange = {},
            onMinRssiToggle = {},
            onStartScan = {},
            onStopScan = {},
            onDeviceClick = {},
            onBannerClick = {},
            onSettingsClick = {},
            onSettingsDismiss = {},
            onScanConfigApply = {},
        )
    }
}

@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ScannerContentDarkPreview() {
    DBLETheme(darkTheme = true, dynamicColor = false) {
        ScannerContent(
            state = State(isScanning = true, devices = PreviewDevices),
            nowMillis = 0L,
            onQueryChange = {},
            onFilterFieldChange = {},
            onMinRssiToggle = {},
            onStartScan = {},
            onStopScan = {},
            onDeviceClick = {},
            onBannerClick = {},
            onSettingsClick = {},
            onSettingsDismiss = {},
            onScanConfigApply = {},
        )
    }
}
