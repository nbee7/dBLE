package id.project.df.dble.core.data.scan

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.project.df.dble.core.model.EMA_ALPHA
import id.project.df.dble.core.model.KALMAN_Q
import id.project.df.dble.core.model.KALMAN_R
import id.project.df.dble.core.model.distanceMetersFor
import id.project.df.dble.core.model.zoneFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

// need new file
data class RssiSample(
    val tMillis: Long,
    val mac: String,
    val rawRssi: Int,
    val emaRssi: Double,
    val kalmanRssi: Double,
)

// need new file
@Singleton
class RssiRecorder @Inject constructor() {

    private val lock = Any()
    private val buffer = ArrayDeque<RssiSample>()
    private var trackedMac: String? = null

    private val _sampleCount = MutableStateFlow(0)
    val sampleCount: StateFlow<Int> = _sampleCount.asStateFlow()

    fun track(mac: String) = synchronized(lock) {
        if (trackedMac == mac) return@synchronized
        buffer.clear()
        trackedMac = mac
        _sampleCount.value = 0
    }

    fun release() = synchronized(lock) {
        buffer.clear()
        trackedMac = null
        _sampleCount.value = 0
    }

    fun onSample(sample: RssiSample) = synchronized(lock) {
        if (sample.mac != trackedMac) return@synchronized
        buffer.addLast(sample)
        val oldestKept = sample.tMillis - WINDOW_MS
        while (buffer.first().tMillis < oldestKept) buffer.removeFirst()
        _sampleCount.value = buffer.size
    }

    fun snapshot(): List<RssiSample> = synchronized(lock) { buffer.toList() }

    companion object {
        const val WINDOW_MS = 300_000L
    }
}

// need new file
data class ExportMeta(
    val mac: String,
    val name: String?,
    val deviceModel: String,
    val appVersion: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
)

// need new file
object RssiCsv {

    private const val COLUMNS =
        "t_ms,epoch_ms,dt_ms,mac,raw_rssi,ema_rssi,kalman_rssi,raw_zone,ema_zone,kalman_zone,raw_dist_m,ema_dist_m,kalman_dist_m"

    fun build(meta: ExportMeta, samples: List<RssiSample>): String = buildString {
        appendLine("# mac=${meta.mac}")
        appendLine("# name=${singleLine(meta.name.orEmpty())}")
        appendLine("# device_model=${singleLine(meta.deviceModel)}")
        appendLine("# app_version=${singleLine(meta.appVersion)}")
        appendLine("# started_at_epoch_ms=${meta.startedAtMillis}")
        appendLine("# ended_at_epoch_ms=${meta.endedAtMillis}")
        appendLine("# sample_count=${samples.size}")
        appendLine("# ema_alpha=$EMA_ALPHA")
        appendLine("# kalman_q=$KALMAN_Q")
        appendLine("# kalman_r=$KALMAN_R")
        appendLine("# window_ms=${RssiRecorder.WINDOW_MS}")
        appendLine(COLUMNS)
        val first = samples.firstOrNull()?.tMillis ?: 0L
        var previous = first
        samples.forEach { s ->
            val emaInt = s.emaRssi.roundToInt()
            val kalmanInt = s.kalmanRssi.roundToInt()
            appendLine(
                listOf(
                    s.tMillis - first,
                    s.tMillis,
                    s.tMillis - previous,
                    s.mac,
                    s.rawRssi,
                    number(s.emaRssi),
                    number(s.kalmanRssi),
                    zoneFor(s.rawRssi).name,
                    zoneFor(emaInt).name,
                    zoneFor(kalmanInt).name,
                    distance(s.rawRssi),
                    distance(emaInt),
                    distance(kalmanInt),
                ).joinToString(","),
            )
            previous = s.tMillis
        }
    }

    fun fileName(mac: String, startedAtMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(zone).format(Instant.ofEpochMilli(startedAtMillis))
        return "dble-rssi-${mac.replace(':', '-')}-$stamp.csv"
    }

    private fun number(value: Double) = String.format(Locale.US, "%.2f", value)

    private fun distance(rssi: Int) = distanceMetersFor(rssi)?.let(::number).orEmpty()

    private fun singleLine(text: String) = text.replace('\r', ' ').replace('\n', ' ')
}

// need new file
interface RssiExporter {
    suspend fun export(mac: String, name: String?, samples: List<RssiSample>): Uri
}

// need new file
class FileRssiExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : RssiExporter {

    override suspend fun export(mac: String, name: String?, samples: List<RssiSample>): Uri =
        withContext(Dispatchers.IO) {
            require(samples.isNotEmpty()) { "Tidak ada sampel untuk diekspor" }
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            dir.listFiles { file -> file.extension == "csv" }?.forEach { it.delete() }
            val meta = ExportMeta(
                mac = mac,
                name = name,
                deviceModel = Build.MODEL,
                appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
                startedAtMillis = samples.first().tMillis,
                endedAtMillis = samples.last().tMillis,
            )
            val file = File(dir, RssiCsv.fileName(mac, meta.startedAtMillis))
            file.writeText(RssiCsv.build(meta, samples), Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
}
