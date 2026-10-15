package id.project.df.dble.feature.scanner

import app.cash.turbine.test
import id.project.df.dble.core.common.mvi.MainDispatcherRule
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.data.scan.ScanController
import id.project.df.dble.core.data.scan.ScanState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.domain.FilterDevicesUseCase
import id.project.df.dble.core.domain.ObserveDevicesUseCase
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.ScanMode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeDevices: ObserveDevicesUseCase = mockk()
    private val scanController: ScanController = mockk()
    private val filterDevices = FilterDevicesUseCase()

    private lateinit var scanStateFlow: MutableStateFlow<ScanState>
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setup() {
        scanStateFlow = MutableStateFlow(ScanState.Idle)
        every { observeDevices() } returns MutableStateFlow(emptyList())
        every { scanController.scanState } returns scanStateFlow
        every { scanController.scanConfig } returns MutableStateFlow(ScanConfig())
        every { scanController.applyScanConfig(any()) } returns Unit
        viewModel = ScannerViewModel(filterDevices, observeDevices, scanController)
    }

    @Test
    fun given_StartScanDiizinkan_when_Click_then_StateScanning() = runTest {
        every { scanController.requestScan() } answers {
            scanStateFlow.value = ScanState.Scanning
            PermissionCheck.Granted
        }

        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.StartScanClicked)
            assertTrue(awaitItem().isScanning)
        }
        verify(exactly = 1) { scanController.requestScan() }
    }

    @Test
    fun given_IzinKurang_when_StartScan_then_EffectRequestPermissions() = runTest {
        val missing = listOf("android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT")
        every { scanController.requestScan() } returns PermissionCheck.Missing(missing)

        viewModel.sendIntent(Intent.StartScanClicked)

        viewModel.effect.test {
            val effect = awaitItem()
            assertTrue(effect is Effect.RequestPermissions)
            assertEquals(missing, (effect as Effect.RequestPermissions).permissions)
        }
        assertFalse(viewModel.uiState.value.isScanning)
        verify(exactly = 0) { scanController.stopScanByUser() }
    }

    @Test
    fun given_IzinKurang_when_StartScan_then_BannerPermissionRevoked() = runTest {
        every { scanController.requestScan() } returns PermissionCheck.Missing(listOf("p1"))

        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.StartScanClicked)
            assertEquals(StopReason.PERMISSION_REVOKED, awaitItem().banner)
        }
    }

    @Test
    fun given_BluetoothOff_when_StartScan_then_BannerBluetoothOff() = runTest {
        every { scanController.requestScan() } returns PermissionCheck.BluetoothOff

        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.StartScanClicked)
            assertEquals(StopReason.BLUETOOTH_OFF, awaitItem().banner)
        }
    }

    @Test
    fun given_SettingsClicked_when_Processed_then_PopupTampilLaluTutup() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.SettingsClicked)
            assertTrue(awaitItem().showSettings)
            viewModel.sendIntent(Intent.SettingsDismissed)
            assertFalse(awaitItem().showSettings)
        }
    }

    @Test
    fun given_ConfigDiterapkan_when_Processed_then_ControllerDipanggilDanPopupTutup() = runTest {
        val baru = ScanConfig(ScanMode.SEIMBANG, extended = true)
        viewModel.sendIntent(Intent.SettingsClicked)

        viewModel.sendIntent(Intent.ScanConfigApplied(baru))

        verify(exactly = 1) { scanController.applyScanConfig(baru) }
        assertFalse(viewModel.uiState.value.showSettings)
    }

    @Test
    fun given_LocationOff_when_StartScan_then_BannerLocationOff() = runTest {
        every { scanController.requestScan() } returns PermissionCheck.LocationServiceOff

        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.StartScanClicked)
            assertEquals(StopReason.LOCATION_OFF, awaitItem().banner)
        }
        viewModel.effect.test {
            assertEquals(Effect.OpenLocationSettings, awaitItem())
        }
    }

    @Test
    fun given_DeviceClicked_when_Processed_then_EffectNavigate() = runTest {
        viewModel.sendIntent(Intent.DeviceClicked("AA:BB:CC:DD:EE:FF"))

        viewModel.effect.test {
            assertEquals(
                Effect.NavigateToTracking("AA:BB:CC:DD:EE:FF"),
                awaitItem(),
            )
        }
    }

    @Test
    fun given_StopScanUser_when_Click_then_BannerNull() = runTest {
        scanStateFlow.value = ScanState.Stopped(StopReason.BLUETOOTH_OFF)
        assertEquals(StopReason.BLUETOOTH_OFF, viewModel.uiState.value.banner)

        every { scanController.stopScanByUser() } answers {
            scanStateFlow.value = ScanState.Stopped(StopReason.USER_STOPPED)
        }

        viewModel.uiState.test {
            skipItems(1)
            viewModel.sendIntent(Intent.StopScanClicked)
            assertNull(awaitItem().banner)
        }
        verify(exactly = 1) { scanController.stopScanByUser() }
    }
}
