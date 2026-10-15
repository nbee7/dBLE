package id.project.df.dble.feature.tracking

import app.cash.turbine.test
import id.project.df.dble.app.navigation.TrackingRoute
import id.project.df.dble.core.common.mvi.MainDispatcherRule
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.data.scan.ScanController
import id.project.df.dble.core.data.scan.ScanState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.domain.ObserveTargetUseCase
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedTarget
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TrackingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mac = "AA:BB:CC:DD:EE:FF"
    private var now = 0L
    private val time = object : TimeProvider {
        override fun nowMillis(): Long = now
    }
    private val targets = MutableSharedFlow<TrackedTarget>(extraBufferCapacity = 16)
    private val observeTarget: ObserveTargetUseCase = mockk()
    private val scanController: ScanController = mockk()
    private val scanState = MutableStateFlow<ScanState>(ScanState.Scanning)

    private fun viewModel(): TrackingViewModel {
        every { observeTarget(mac) } returns targets
        every { scanController.scanState } returns scanState
        every { scanController.requestScan() } returns PermissionCheck.Granted
        return TrackingViewModel(TrackingRoute(mac), observeTarget, time, scanController)
    }

    private fun target(rssi: Int, isLost: Boolean = false) = TrackedTarget(
        mac = mac,
        name = "Tag",
        rawRssi = rssi,
        smoothedRssi = rssi.toDouble(),
        zone = ProximityZone.KUAT,
        distanceMeters = 3.0,
        isLost = isLost,
    )

    @Test
    fun given_MacDariRoute_when_Init_then_StateBerisiMac() = runTest {
        val vm = viewModel()

        val state = vm.uiState.value
        assertEquals(mac, state.mac)
        assertEquals(null, state.rawRssi)
        assertEquals(true, state.isLost)
    }

    @Test
    fun given_UpdateTarget_when_Collect_then_PointsTertrim60Detik() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            now = 0
            targets.emit(target(-50))
            awaitItem()
            now = 30_000
            targets.emit(target(-55))
            assertEquals(2, awaitItem().points.size)
            now = 70_000
            targets.emit(target(-60))
            val state = awaitItem()
            assertEquals(listOf(30_000L, 70_000L), state.points.map { it.tMillis })
            assertEquals(-60, state.rawRssi)
            assertFalse(state.isLost)
        }
    }

    @Test
    fun given_TargetLost_when_Collect_then_TidakAdaTitikBaru() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            now = 1_000
            targets.emit(target(-50))
            assertEquals(1, awaitItem().points.size)
            now = 9_000
            targets.emit(target(-50, isLost = true))
            val state = awaitItem()
            assertEquals(1, state.points.size)
            assertEquals(true, state.isLost)
        }
    }

    @Test
    fun given_ScanIdle_when_Init_then_RequestScan() = runTest {
        scanState.value = ScanState.Idle

        viewModel()

        verify(exactly = 1) { scanController.requestScan() }
    }

    @Test
    fun given_SudahScanning_when_Init_then_TidakRequestScan() = runTest {
        viewModel()

        verify(exactly = 0) { scanController.requestScan() }
    }

    @Test
    fun given_IzinKurang_when_Init_then_EffectRequestPermissionsDanBanner() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.Missing(listOf("p1"))
        val vm = viewModelWithoutDefaultScan()

        vm.effect.test {
            assertEquals(Effect.RequestPermissions(listOf("p1")), awaitItem())
        }
        assertEquals(StopReason.PERMISSION_REVOKED, vm.uiState.value.banner)
    }

    @Test
    fun given_LocationOff_when_Init_then_BannerLocationOff() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.LocationServiceOff
        val vm = viewModelWithoutDefaultScan()

        assertEquals(StopReason.LOCATION_OFF, vm.uiState.value.banner)
    }

    private fun viewModelWithoutDefaultScan(): TrackingViewModel {
        every { observeTarget(mac) } returns targets
        every { scanController.scanState } returns scanState
        return TrackingViewModel(TrackingRoute(mac), observeTarget, time, scanController)
    }
}
