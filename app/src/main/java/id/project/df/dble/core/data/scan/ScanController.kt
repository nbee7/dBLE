package id.project.df.dble.core.data.scan

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import id.project.df.dble.core.data.di.ApplicationScope
import id.project.df.dble.core.data.history.DeviceHistoryRepository
import id.project.df.dble.core.data.permission.BlePermissionChecker
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.model.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanController @Inject constructor(
    private val scan: DeviceScanRepository,
    private val history: DeviceHistoryRepository,
    private val checker: BlePermissionChecker,
    private val configStore: ScanConfigStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val scanState: StateFlow<ScanState> = scan.scanState
    val scanConfig: StateFlow<ScanConfig> = configStore.config

    private var scanDesired = false
    private var recordJob: Job? = null

    fun requestScan(): PermissionCheck = when (val result = checker.check()) {
        PermissionCheck.Granted -> {
            Timber.d("[SCAN][START]")
            scanDesired = true
            scan.startScan()
            startRecordCollector()
            result
        }
        is PermissionCheck.Missing -> {
            Timber.d("[PERM][MISS] n=%d locOff=%b", result.permissions.size, false)
            result
        }
        PermissionCheck.LocationServiceOff -> {
            Timber.d("[PERM][LOC_OFF]")
            result
        }
        PermissionCheck.BluetoothOff -> {
            Timber.d("[BT][OFF]")
            result
        }
    }

    fun applyScanConfig(config: ScanConfig) {
        if (configStore.config.value == config) return
        configStore.save(config)
        if (scanState.value != ScanState.Scanning) return
        Timber.d("[SCAN][RESTART] reason=CONFIG")
        cancelRecordCollector()
        scan.stopScan(StopReason.USER_STOPPED)
        flush()
        scan.startScan()
        startRecordCollector()
    }

    fun stopScanByUser() {
        Timber.d("[SCAN][STOP] reason=USER_STOPPED")
        scanDesired = false
        cancelRecordCollector()
        scan.stopScan(StopReason.USER_STOPPED)
        flush()
    }

    fun onBackground() {
        if (scanState.value != ScanState.Scanning) return
        Timber.d("[SCAN][BG]")
        cancelRecordCollector()
        scan.stopScan(StopReason.USER_STOPPED)
        flush()
    }

    fun onResume() {
        if (!scanDesired) return
        when (checker.check()) {
            PermissionCheck.Granted -> {
                Timber.d("[SCAN][RESUME]")
                scan.startScan()
                startRecordCollector()
            }
            is PermissionCheck.Missing -> {
                Timber.d("[SCAN][STOP] reason=PERMISSION_REVOKED")
                scan.stopScan(StopReason.PERMISSION_REVOKED)
            }
            PermissionCheck.LocationServiceOff -> {
                Timber.d("[SCAN][STOP] reason=LOCATION_OFF")
                scan.stopScan(StopReason.LOCATION_OFF)
            }
            PermissionCheck.BluetoothOff -> {
                Timber.d("[BT][OFF]")
                scanDesired = false
                scan.stopScan(StopReason.BLUETOOTH_OFF)
            }
        }
    }

    fun onBluetoothStateChanged(enabled: Boolean) {
        if (enabled) return
        scanDesired = false
        if (scanState.value != ScanState.Scanning) return
        Timber.d("[BT][OFF]")
        cancelRecordCollector()
        scan.stopScan(StopReason.BLUETOOTH_OFF)
        flush()
    }

    private fun startRecordCollector() {
        recordJob?.cancel()
        recordJob = scope.launch {
            val job = coroutineContext.job
            val watch = launch {
                scan.scanState.first { it !is ScanState.Scanning }
                job.cancel()
            }
            try {
                scan.devices.collect { devices -> devices.forEach { history.record(it) } }
            } finally {
                watch.cancel()
            }
        }
    }

    private fun cancelRecordCollector() {
        recordJob?.cancel()
        recordJob = null
    }

    private fun flush() {
        scope.launch { history.flush() }
    }
}

@Singleton
class ScanLifecycleObserver @Inject constructor(
    private val controller: ScanController,
) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = controller.onResume()
    override fun onStop(owner: LifecycleOwner) = controller.onBackground()
}
