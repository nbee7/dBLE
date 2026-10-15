package id.project.df.dble.core.data.scan

import android.bluetooth.le.ScanCallback
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.ble.BleScanException
import id.project.df.dble.core.data.di.ApplicationScope
import id.project.df.dble.core.data.ble.BleScannerSource
import id.project.df.dble.core.model.BleAdvertise
import id.project.df.dble.core.model.TrackedDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceScanRepositoryImpl @Inject constructor(
    private val source: BleScannerSource,
    @ApplicationScope private val scope: CoroutineScope,
    private val time: TimeProvider,
) : DeviceScanRepository {

    private val _devices = MutableStateFlow<List<TrackedDevice>>(emptyList())
    override val devices: StateFlow<List<TrackedDevice>> = _devices.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val mutex = Mutex()
    private var tracked: Map<String, TrackedDevice> = emptyMap()
    private var job: Job? = null

    override fun startScan() {
        if (_scanState.value is ScanState.Scanning) return
        tracked = emptyMap()
        _devices.value = emptyList()
        _scanState.value = ScanState.Scanning
        job = scope.launch { monitor() }
    }

    override fun stopScan(reason: StopReason) {
        job?.cancel()
        job = null
        _scanState.value = ScanState.Stopped(reason)
    }

    private suspend fun monitor() {
        val ticker = scope.launch {
            while (true) {
                delay(1_000)
                onTick()
            }
        }
        try {
            source.scan()
                .onEach { onAdvertise(it) }
                .catch { t ->
                    Timber.w(t, "[SCAN][FAIL] code=%d", (t as? BleScanException)?.code ?: -1)
                    stop(mapError(t))
                }
                .collect()
        } finally {
            ticker.cancel()
        }
    }

    private suspend fun onAdvertise(ad: BleAdvertise) = mutex.withLock {
        tracked = ScanAggregation.upsert(tracked, ad, time.nowMillis())
        tracked = ScanAggregation.prune(tracked, time.nowMillis())
        publish()
    }

    private suspend fun onTick() = mutex.withLock {
        val pruned = ScanAggregation.prune(tracked, time.nowMillis())
        val removed = tracked.size - pruned.size
        tracked = pruned
        if (removed > 0) Timber.d("[SCAN][PRUNE] removed=%d left=%d", removed, tracked.size)
        publish()
    }

    private fun publish() {
        _devices.value = tracked.values.sortedByDescending { it.smoothedRssi }
    }

    private fun stop(reason: StopReason) {
        job?.cancel()
        job = null
        _scanState.value = ScanState.Stopped(reason)
    }

    private fun mapError(t: Throwable): StopReason = when (t) {
        is BleScanException -> when (t.code) {
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> StopReason.SCAN_FAILED_REGISTRATION
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> StopReason.SCAN_FAILED_TOO_FREQUENTLY
            else -> StopReason.SCAN_FAILED_OTHER
        }
        else -> StopReason.SCAN_FAILED_OTHER
    }
}
