package id.project.df.dble.feature.tracking

import android.net.Uri
import app.cash.turbine.test
import id.project.df.dble.app.navigation.TrackingRoute
import id.project.df.dble.core.common.mvi.MainDispatcherRule
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.data.scan.RssiExporter
import id.project.df.dble.core.data.scan.RssiRecorder
import id.project.df.dble.core.data.scan.RssiSample
import id.project.df.dble.core.data.scan.ScanController
import id.project.df.dble.core.data.scan.ScanState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.domain.ObserveTargetUseCase
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedTarget
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.io.IOException

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
    private val recorder = RssiRecorder()
    private val exporter: RssiExporter = mockk()
    private val uri: Uri = mockk()

    private fun viewModel(): TrackingViewModel {
        every { observeTarget(mac) } returns targets
        every { scanController.scanState } returns scanState
        every { scanController.requestScan() } returns PermissionCheck.Granted
        return TrackingViewModel(TrackingRoute(mac), observeTarget, time, scanController, recorder, exporter)
    }

    private fun target(
        rssi: Int,
        isLost: Boolean = false,
        smoothed: Double = rssi.toDouble(),
        kalman: Double = rssi.toDouble(),
        adKalman: Double = rssi.toDouble(),
    ) = TrackedTarget(
        mac = mac,
        name = "Tag",
        rawRssi = rssi,
        smoothedRssi = smoothed,
        zone = ProximityZone.KUAT,
        distanceMeters = 3.0,
        isLost = isLost,
        kalmanRssi = kalman,
        adKalmanRssi = adKalman,
    )

    @Test
    fun given_TargetDenganEmaDanKalman_when_Collect_then_PointDanStateMembawaKeduanya() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            now = 5_000
            targets.emit(target(-50, smoothed = -52.0, kalman = -51.0, adKalman = -50.5))
            val state = awaitItem()
            assertEquals(RssiPoint(5_000, -50, -52.0, -51.0, -50.5), state.points.last())
            assertEquals(-51.0, state.kalmanRssi!!, 1e-9)
            assertEquals(-50.5, state.adKalmanRssi!!, 1e-9)
        }
    }

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

    @Test
    fun given_IzinDiberikan_when_PermissionsGranted_then_RequestScanUlang() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.Missing(listOf("p1"))
        val vm = viewModelWithoutDefaultScan()
        every { scanController.requestScan() } answers {
            scanState.value = ScanState.Scanning
            PermissionCheck.Granted
        }

        vm.sendIntent(Intent.PermissionsGranted)

        verify(exactly = 2) { scanController.requestScan() }
        assertEquals(null, vm.uiState.value.banner)
    }

    @Test
    fun given_LocationOff_when_Init_then_TidakLangsungBukaSetelan() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.LocationServiceOff
        val vm = viewModelWithoutDefaultScan()

        vm.effect.test {
            expectNoEvents()
        }
    }

    @Test
    fun given_BluetoothOff_when_Init_then_BannerBluetoothOff() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.BluetoothOff
        val vm = viewModelWithoutDefaultScan()

        assertEquals(StopReason.BLUETOOTH_OFF, vm.uiState.value.banner)
    }

    @Test
    fun given_BannerLocationOffLaluDiperbaiki_when_ScreenResumed_then_ScanMenyalaBannerHilang() = runTest {
        scanState.value = ScanState.Idle
        every { scanController.requestScan() } returns PermissionCheck.LocationServiceOff
        val vm = viewModelWithoutDefaultScan()
        every { scanController.requestScan() } answers {
            scanState.value = ScanState.Scanning
            PermissionCheck.Granted
        }

        vm.sendIntent(Intent.ScreenResumed)

        assertEquals(null, vm.uiState.value.banner)
        verify(exactly = 2) { scanController.requestScan() }
    }

    private fun sample(t: Long = 0, sampleMac: String = mac) =
        RssiSample(tMillis = t, mac = sampleMac, rawRssi = -60, emaRssi = -60.0, kalmanRssi = -60.0)

    @Test
    fun given_Init_when_Create_then_RecorderMelacakMacRoute() = runTest {
        viewModel()

        recorder.onSample(sample())

        assertEquals(1, recorder.snapshot().size)
    }

    @Test
    fun given_RecorderMenerimaSampel_when_Collect_then_StateSampleCountUpdate() = runTest {
        val vm = viewModel()

        vm.uiState.test {
            skipItems(1)
            recorder.onSample(sample())
            assertEquals(1, awaitItem().sampleCount)
        }
    }

    @Test
    fun given_AdaSampel_when_ExportClicked_then_EffectShareFileDenganUri() = runTest {
        val vm = viewModel()
        recorder.onSample(sample())
        coEvery { exporter.export(mac, any(), any()) } returns uri

        vm.effect.test {
            vm.sendIntent(Intent.ExportClicked)
            assertEquals(Effect.ShareFile(uri), awaitItem())
        }
        assertFalse(vm.uiState.value.isExporting)
    }

    @Test
    fun given_BufferKosong_when_ExportClicked_then_ExporterTidakDipanggil() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.sendIntent(Intent.ExportClicked)
            expectNoEvents()
        }
        coVerify(exactly = 0) { exporter.export(any(), any(), any()) }
    }

    @Test
    fun given_ExporterGagal_when_ExportClicked_then_EffectExportFailedDanIsExportingFalse() = runTest {
        val vm = viewModel()
        recorder.onSample(sample())
        coEvery { exporter.export(any(), any(), any()) } throws IOException("disk penuh")

        vm.effect.test {
            vm.sendIntent(Intent.ExportClicked)
            assertEquals(Effect.ExportFailed, awaitItem())
        }
        assertFalse(vm.uiState.value.isExporting)
    }

    @Test
    fun given_ViewModelCleared_when_OnCleared_then_RecorderDilepas() = runTest {
        val vm = viewModel()

        vm.onCleared()
        recorder.onSample(sample())

        assertEquals(0, recorder.snapshot().size)
    }

    private fun viewModelWithoutDefaultScan(): TrackingViewModel {
        every { observeTarget(mac) } returns targets
        every { scanController.scanState } returns scanState
        return TrackingViewModel(TrackingRoute(mac), observeTarget, time, scanController, recorder, exporter)
    }
}
