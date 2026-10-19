package id.project.df.dble.core.data.scan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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
