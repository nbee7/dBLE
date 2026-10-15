package id.project.df.dble.core.data.scan

import id.project.df.dble.core.model.TrackedDevice
import kotlinx.coroutines.flow.StateFlow

interface DeviceScanRepository {
    val devices: StateFlow<List<TrackedDevice>>
    val scanState: StateFlow<ScanState>
    fun startScan()
    fun stopScan(reason: StopReason = StopReason.USER_STOPPED)
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Stopped(val reason: StopReason) : ScanState
}

enum class StopReason {
    USER_STOPPED,
    BLUETOOTH_OFF,
    PERMISSION_REVOKED,
    LOCATION_OFF,
    SCAN_FAILED_ALREADY_STARTED,
    SCAN_FAILED_REGISTRATION,
    SCAN_FAILED_TOO_FREQUENTLY,
    SCAN_FAILED_OTHER,
}
