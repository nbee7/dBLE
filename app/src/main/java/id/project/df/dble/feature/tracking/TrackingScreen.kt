package id.project.df.dble.feature.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import id.project.df.dble.feature.scanner.bannerText
import id.project.df.dble.ui.theme.DBLETheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay

private val ChartLine = Color(0xFF3B82F6)
private const val ChartMinRssi = -90f
private const val ChartMaxRssi = -30f
private const val ChartWindowMs = 60_000L
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
    viewModel.effect.collectAsEffect { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            // Wiring izin runtime + settings lokasi: Task 9.
            is Effect.RequestPermissions -> Unit
            Effect.OpenLocationSettings -> Unit
        }
    }
    val beeper = remember { AndroidProximityBeeper() }
    DisposableEffect(beeper) { onDispose { beeper.release() } }
    BeepEffect(zone = state.zone, isLost = state.isLost, beeper = beeper)
    TrackingContent(state = state, onBack = { viewModel.sendIntent(Intent.BackClicked) })
}

@Composable
fun TrackingContent(state: State, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackingHeader(name = state.name, mac = state.mac, onBack = onBack)
        state.banner?.let { reason -> ScanBanner(bannerText(reason)) }
        AnimatedVisibility(visible = state.isLost) { LostBanner() }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ZoneSegmentBar(activeZone = state.zone.takeUnless { state.isLost })
            EstimateCard(state = state, modifier = Modifier.alpha(if (state.isLost) 0.4f else 1f))
            RssiLineChart(
                points = state.points,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (state.isLost) 0.4f else 1f),
            )
        }
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
private fun ScanBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}

@Composable
fun RssiLineChart(points: ImmutableList<RssiPoint>, modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier.fillMaxWidth(),
    ) {
        listOf(-30f, -50f, -70f, -90f).forEach { level ->
            val y = yFor(level, size.height)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (points.size < 2) return@Canvas
        val tStart = points.last().tMillis - ChartWindowMs
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = ((point.tMillis - tStart).toFloat() / ChartWindowMs).coerceIn(0f, 1f) * size.width
            val y = yFor(point.rssi.toFloat(), size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, ChartLine, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

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
    zone = ProximityZone.KUAT,
    distanceMeters = 2.4,
    isLost = false,
    points = persistentListOf(
        RssiPoint(0, -60),
        RssiPoint(10_000, -52),
        RssiPoint(20_000, -48),
        RssiPoint(30_000, -55),
        RssiPoint(40_000, -46),
    ),
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun TrackingContentLightPreview() {
    DBLETheme(darkTheme = false) { TrackingContent(PreviewState, onBack = {}) }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun TrackingContentDarkPreview() {
    DBLETheme(darkTheme = true) { TrackingContent(PreviewState.copy(isLost = true), onBack = {}) }
}
