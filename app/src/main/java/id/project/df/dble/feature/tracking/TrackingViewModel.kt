package id.project.df.dble.feature.tracking

import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dble.app.navigation.TrackingRoute
import id.project.df.dble.core.common.mvi.BaseViewModel
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.data.scan.ScanController
import id.project.df.dble.core.data.scan.ScanState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.domain.ObserveTargetUseCase
import id.project.df.dble.core.model.TrackedTarget
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel(assistedFactory = TrackingViewModel.Factory::class)
class TrackingViewModel @AssistedInject constructor(
    @Assisted val route: TrackingRoute,
    observeTarget: ObserveTargetUseCase,
    private val time: TimeProvider,
    private val scanController: ScanController,
) : BaseViewModel<State, Intent, Effect>(State(mac = route.mac)) {

    @AssistedFactory
    interface Factory {
        fun create(route: TrackingRoute): TrackingViewModel
    }

    init {
        viewModelScope.launch {
            observeTarget(route.mac).collect(::onTarget)
        }
        viewModelScope.launch {
            scanController.scanState.collect { state ->
                when (state) {
                    ScanState.Scanning -> updateState { copy(banner = null) }
                    is ScanState.Stopped -> updateState {
                        copy(banner = state.reason.takeUnless { it == StopReason.USER_STOPPED })
                    }
                    ScanState.Idle -> Unit
                }
            }
        }
        ensureScanning()
    }

    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            Intent.BackClicked -> {
                Timber.d("[NAV][BACK]")
                sendEffect(Effect.NavigateBack)
            }
            Intent.PermissionsGranted -> ensureScanning()
            Intent.ScreenResumed -> retryAfterSettingsFix()
        }
    }

    private fun ensureScanning() {
        if (scanController.scanState.value is ScanState.Scanning) return
        when (val check = scanController.requestScan()) {
            PermissionCheck.Granted -> Unit
            is PermissionCheck.Missing -> {
                updateState { copy(banner = StopReason.PERMISSION_REVOKED) }
                sendEffect(Effect.RequestPermissions(check.permissions))
            }
            PermissionCheck.LocationServiceOff -> updateState { copy(banner = StopReason.LOCATION_OFF) }
            PermissionCheck.BluetoothOff -> updateState { copy(banner = StopReason.BLUETOOTH_OFF) }
        }
    }

    private fun retryAfterSettingsFix() {
        val banner = uiState.value.banner
        if (banner != StopReason.LOCATION_OFF && banner != StopReason.BLUETOOTH_OFF) return
        when (scanController.requestScan()) {
            PermissionCheck.Granted -> Unit
            is PermissionCheck.Missing -> updateState { copy(banner = StopReason.PERMISSION_REVOKED) }
            PermissionCheck.LocationServiceOff -> updateState { copy(banner = StopReason.LOCATION_OFF) }
            PermissionCheck.BluetoothOff -> updateState { copy(banner = StopReason.BLUETOOTH_OFF) }
        }
    }

    private fun onTarget(target: TrackedTarget) {
        val now = time.nowMillis()
        updateState {
            val kept = points.filter { now - it.tMillis <= WINDOW_MS }
            copy(
                name = target.name,
                rawRssi = target.rawRssi,
                smoothedRssi = target.smoothedRssi,
                zone = target.zone,
                distanceMeters = target.distanceMeters,
                isLost = target.isLost,
                points = (if (target.isLost) kept else kept + RssiPoint(now, target.rawRssi)).toPersistentList(),
            )
        }
    }

    private companion object {
        const val WINDOW_MS = 60_000L
    }
}
