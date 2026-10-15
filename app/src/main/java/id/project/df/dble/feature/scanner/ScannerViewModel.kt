package id.project.df.dble.feature.scanner

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dble.core.common.mvi.BaseViewModel
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.data.scan.ScanController
import id.project.df.dble.core.data.scan.ScanState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.domain.FilterDevicesUseCase
import id.project.df.dble.core.domain.ObserveDevicesUseCase
import id.project.df.dble.core.model.TrackedDevice
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val filterDevices: FilterDevicesUseCase,
    observeDevices: ObserveDevicesUseCase,
    private val scanController: ScanController,
) : BaseViewModel<State, Intent, Effect>(State()) {

    private var allDevices: List<TrackedDevice> = emptyList()

    init {
        viewModelScope.launch {
            observeDevices().collect { devices ->
                allDevices = devices
                refreshDevices()
            }
        }
        viewModelScope.launch {
            scanController.scanState.collect { state ->
                updateState {
                    copy(
                        isScanning = state is ScanState.Scanning,
                        banner = bannerFor(state),
                    )
                }
            }
        }
    }

    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.StartScanClicked -> startScan()
            Intent.StopScanClicked -> scanController.stopScanByUser()
            is Intent.QueryChanged -> {
                updateState { copy(query = intent.value) }
                refreshDevices()
            }
            is Intent.FilterFieldChanged -> {
                updateState { copy(filterField = intent.field) }
                refreshDevices()
            }
            is Intent.MinRssiToggled -> {
                updateState { copy(minRssiEnabled = intent.enabled) }
                refreshDevices()
            }
            is Intent.DeviceClicked -> {
                Timber.d("[NAV][TO] mac=%s", intent.mac)
                sendEffect(Effect.NavigateToTracking(intent.mac))
            }
        }
    }

    private fun startScan() {
        when (val check = scanController.requestScan()) {
            PermissionCheck.Granted -> Unit
            is PermissionCheck.Missing -> sendEffect(Effect.RequestPermissions(check.permissions))
            PermissionCheck.LocationServiceOff -> {
                updateState { copy(banner = StopReason.LOCATION_OFF) }
                sendEffect(Effect.OpenLocationSettings)
            }
        }
    }

    private fun refreshDevices() {
        val state = uiState.value
        val filtered = filterDevices(
            devices = allDevices,
            query = state.query,
            field = state.filterField,
            minRssiEnabled = state.minRssiEnabled,
            minRssi = state.minRssi,
        )
        updateState { copy(devices = filtered.toPersistentList()) }
    }

    private fun bannerFor(state: ScanState): StopReason? = when (state) {
        is ScanState.Stopped -> if (state.reason == StopReason.USER_STOPPED) null else state.reason
        ScanState.Idle, ScanState.Scanning -> null
    }
}
