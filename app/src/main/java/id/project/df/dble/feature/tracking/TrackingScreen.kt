package id.project.df.dble.feature.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.project.df.dble.app.navigation.TrackingRoute
import id.project.df.dble.core.common.mvi.collectAsEffect
import id.project.df.dble.core.data.audio.AndroidProximityBeeper
import id.project.df.dble.core.data.audio.ProximityBeeper
import id.project.df.dble.core.designsystem.color
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.beepIntervalMsFor
import id.project.df.dble.core.model.formatDistance
import id.project.df.dble.core.data.scan.RssiRecorder
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.ui.hasSettingsAction
import id.project.df.dble.core.ui.openSettingsFor
import id.project.df.dble.core.ui.rememberPermissionRequester
import id.project.df.dble.core.ui.shareCsv
import id.project.df.dble.feature.scanner.bannerText
import id.project.df.dble.ui.theme.DBLETheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import java.util.Locale

private val ChartLine = Color(0xFF3B82F6)
private val ChartKalman = Color(0xFFF97316)
private const val ChartMinRssi = -90f
private const val ChartMaxRssi = -30f
private const val ChartWindowMs = 60_000L
private val YAxisWidth = 36.dp
private val BarZones = listOf(
    ProximityZone.SANGAT_LEMAH,
    ProximityZone.LEMAH,
    ProximityZone.CUKUP,
    ProximityZone.KUAT,
    ProximityZone.SANGAT_KUAT,
)

@Composable
fun TrackingScreen(
    route: TrackingRoute,
    onBack: () -> Unit,
    viewModel: TrackingViewModel = hiltViewModel<TrackingViewModel, TrackingViewModel.Factory>(
        creationCallback = { factory -> factory.create(route) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val requestPermissions = rememberPermissionRequester { viewModel.sendIntent(Intent.PermissionsGranted) }
    LifecycleResumeEffect(Unit) {
        viewModel.sendIntent(Intent.ScreenResumed)
        onPauseOrDispose { }
    }
    viewModel.effect.collectAsEffect { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.RequestPermissions -> requestPermissions(effect.permissions)
            is Effect.ShareFile -> context.shareCsv(effect.uri)
            Effect.ExportFailed -> Toast.makeText(context, "Gagal mengekspor data", Toast.LENGTH_SHORT).show()
        }
    }
    val beeper = remember { AndroidProximityBeeper() }
    DisposableEffect(beeper) { onDispose { beeper.release() } }
    BeepEffect(zone = state.zone, isLost = state.isLost, beeper = beeper)
    TrackingContent(
        state = state,
        onBack = { viewModel.sendIntent(Intent.BackClicked) },
        onBannerClick = { context.openSettingsFor(it) },
        onExport = { viewModel.sendIntent(Intent.ExportClicked) },
    )
}

@Composable
fun TrackingContent(
    state: State,
    onBack: () -> Unit,
    onBannerClick: (StopReason) -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackingHeader(name = state.name, mac = state.mac, onBack = onBack)
        state.banner?.let { reason ->
            ScanBanner(
                text = bannerText(reason),
                modifier = Modifier.clickable(enabled = reason.hasSettingsAction()) { onBannerClick(reason) },
            )
        }
        AnimatedVisibility(visible = state.isLost) { LostBanner() }
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val contentModifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
        val dimmed = Modifier.alpha(if (state.isLost) 0.4f else 1f)
        if (isLandscape) {
            Row(modifier = contentModifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ZoneSegmentBar(activeZone = state.zone.takeUnless { state.isLost })
                    EstimateCard(state = state, modifier = dimmed)
                }
                RssiLineChart(points = state.points, modifier = Modifier.weight(1f).fillMaxHeight().then(dimmed))
            }
        } else {
            Column(modifier = contentModifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ZoneSegmentBar(activeZone = state.zone.takeUnless { state.isLost })
                EstimateCard(state = state, modifier = dimmed)
                RssiLineChart(points = state.points, modifier = Modifier.weight(1f).then(dimmed))
            }
        }
        ExportButton(
            sampleCount = state.sampleCount,
            isExporting = state.isExporting,
            onClick = onExport,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun TrackingHeader(name: String?, mac: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
        }
        Column {
            Text(
                text = name ?: "(Tanpa nama)",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = mac,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun ScanBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun LostBanner() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "⚠ Perangkat ini tidak terdeteksi — menunggu sinyal kembali",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun ZoneSegmentBar(activeZone: ProximityZone?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BarZones.forEach { zone ->
            val active = zone == activeZone
            val shape = RoundedCornerShape(6.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .then(
                        if (active) {
                            Modifier.shadow(8.dp, shape, ambientColor = zone.color(), spotColor = zone.color())
                        } else {
                            Modifier
                        },
                    )
                    .alpha(if (active) 1f else 0.28f)
                    .background(zone.color(), shape),
            )
        }
    }
}

@Composable
fun EstimateCard(state: State, modifier: Modifier = Modifier) {
    val zone = state.zone
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (zone == null) "—" else formatDistance(state.distanceMeters, zone),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
            if (zone != null) {
                Text(
                    text = "${zone.label.uppercase()} — ${zoneRangeLabel(zone)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = zone.color(),
                )
            }
            Text(
                text = state.rawRssi?.let { "RSSI ${it.toString().replace('-', '−')} dBm" } ?: "RSSI — dBm",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "EMA ${dbm(state.smoothedRssi)} · Kalman ${dbm(state.kalmanRssi)} dBm",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
fun RssiLineChart(points: ImmutableList<RssiPoint>, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val rawColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val textMeasurer = rememberTextMeasurer()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Kekuatan sinyal (dBm)",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "${ChartWindowMs / 1000} detik terakhir",
                style = labelStyle,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(rawColor, "Raw")
            LegendItem(ChartLine, "EMA")
            LegendItem(ChartKalman, "Kalman")
        }
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val gutter = YAxisWidth.toPx()
            val plotWidth = size.width - gutter
            listOf(-30f, -50f, -70f, -90f).forEach { level ->
                val y = yFor(level, size.height)
                drawLine(gridColor, Offset(gutter, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                val label = textMeasurer.measure("−${-level.toInt()}", labelStyle)
                val top = chartLabelTop(y, label.size.height.toFloat(), size.height)
                drawText(label, topLeft = Offset(gutter - label.size.width - 6.dp.toPx(), top))
            }
            if (points.size < 2) return@Canvas
            val tStart = points.last().tMillis - ChartWindowMs
            fun line(color: Color, width: Float, value: (RssiPoint) -> Float) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = gutter + ((point.tMillis - tStart).toFloat() / ChartWindowMs).coerceIn(0f, 1f) * plotWidth
                    val y = yFor(value(point), size.height)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
            }
            line(rawColor, 1.dp.toPx()) { it.rssi.toFloat() }
            line(ChartLine, 2.dp.toPx()) { it.emaRssi.toFloat() }
            line(ChartKalman, 2.dp.toPx()) { it.kalmanRssi.toFloat() }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = YAxisWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "−${ChartWindowMs / 1000} dtk", style = labelStyle)
            Text(text = "sekarang", style = labelStyle)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ExportButton(sampleCount: Int, isExporting: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = sampleCount > 0 && !isExporting, modifier = modifier.fillMaxWidth()) {
        Text("Ekspor data ($sampleCount sampel, ${RssiRecorder.WINDOW_MS / 60_000} menit terakhir)")
    }
}

private fun dbm(value: Double?): String =
    value?.let { String.format(Locale.US, "%.1f", it).replace('-', '−') } ?: "—"

internal fun chartLabelTop(y: Float, labelHeight: Float, canvasHeight: Float): Float =
    (y - labelHeight / 2f).coerceIn(0f, (canvasHeight - labelHeight).coerceAtLeast(0f))

private fun yFor(rssi: Float, height: Float): Float {
    val clamped = rssi.coerceIn(ChartMinRssi, ChartMaxRssi)
    return (ChartMaxRssi - clamped) / (ChartMaxRssi - ChartMinRssi) * height
}

@Composable
fun BeepEffect(zone: ProximityZone?, isLost: Boolean, beeper: ProximityBeeper) {
    LaunchedEffect(zone, isLost) {
        if (zone == null || isLost) return@LaunchedEffect
        val interval = beepIntervalMsFor(zone) ?: return@LaunchedEffect
        while (true) {
            beeper.beep()
            delay(interval)
        }
    }
}

private fun zoneRangeLabel(zone: ProximityZone): String = when (zone) {
    ProximityZone.SANGAT_KUAT -> "Sangat Dekat (<1 m)"
    ProximityZone.KUAT -> "Dekat (1–3 m)"
    ProximityZone.CUKUP -> "Cukup (3–10 m)"
    ProximityZone.LEMAH -> "Lemah (10–20 m)"
    ProximityZone.SANGAT_LEMAH -> "Sangat Lemah (>20 m)"
    ProximityZone.LOST -> "Di luar jangkauan"
}

private val PreviewState = State(
    mac = "AA:BB:CC:DD:EE:FF",
    name = "Tag Kunci",
    rawRssi = -46,
    smoothedRssi = -47.0,
    kalmanRssi = -46.0,
    adKalmanRssi = -45.0,
    sampleCount = 1234,
    zone = ProximityZone.KUAT,
    distanceMeters = 2.4,
    isLost = false,
    points = persistentListOf(
        RssiPoint(0, -60, -60.0, -60.0, -60.0),
        RssiPoint(10_000, -52, -57.0, -56.0, -55.0),
        RssiPoint(20_000, -48, -53.0, -52.0, -51.0),
        RssiPoint(30_000, -55, -54.0, -53.0, -54.0),
        RssiPoint(40_000, -46, -51.0, -50.0, -49.0),
    ),
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun TrackingContentLightPreview() {
    DBLETheme(darkTheme = false) { TrackingContent(PreviewState, onBack = {}, onBannerClick = {}, onExport = {}) }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TrackingContentDarkPreview() {
    DBLETheme(darkTheme = true) { TrackingContent(PreviewState.copy(isLost = true), onBack = {}, onBannerClick = {}, onExport = {}) }
}
