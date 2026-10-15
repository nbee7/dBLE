package id.project.df.dble.core.data.scan

import android.Manifest
import id.project.df.dble.core.data.history.DeviceHistoryRepository
import id.project.df.dble.core.data.permission.BlePermissionChecker
import id.project.df.dble.core.data.permission.PermissionCheck
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.ScanMode
import id.project.df.dble.core.model.TrackedDevice
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanControllerTest {

    // Mock jawab meniru repo nyata: start/stop memutari state flow
    private class Fixture(scope: CoroutineScope) {
        val devices = MutableStateFlow<List<TrackedDevice>>(emptyList())
        val state = MutableStateFlow<ScanState>(ScanState.Idle)
        val scan = mockk<DeviceScanRepository>()
        val history = mockk<DeviceHistoryRepository>(relaxUnitFun = true)
        val checker = mockk<BlePermissionChecker>()
        val store = FakeScanConfigStore()

        init {
            every { scan.devices } returns devices
            every { scan.scanState } returns state
            every { scan.startScan() } answers { state.value = ScanState.Scanning }
            every { scan.stopScan(any()) } answers { state.value = ScanState.Stopped(firstArg()) }
            every { checker.check() } returns PermissionCheck.Granted
        }

        // init dulu, baru controller — konstruktornya membaca scanState
        val controller = ScanController(scan, history, checker, store, scope)

        fun izinKurang() = every {
            checker.check()
        } returns PermissionCheck.Missing(listOf(Manifest.permission.BLUETOOTH_SCAN))
    }

    private fun device(mac: String = "AA:BB:CC:DD:EE:01") = TrackedDevice(
        mac = mac,
        name = "Tag",
        rawRssi = -60,
        smoothedRssi = -60.0,
        zone = ProximityZone.KUAT,
        distanceMeters = 3.0,
        lastSeenMillis = 1_000L,
    )

    @Test
    fun given_IzinKurang_when_RequestScan_then_HasilMissingDanStateIdle() = runTest {
        val f = Fixture(backgroundScope)
        f.izinKurang()

        val result = f.controller.requestScan()
        runCurrent()

        assertTrue(result is PermissionCheck.Missing)
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN),
            (result as PermissionCheck.Missing).permissions,
        )
        assertEquals(ScanState.Idle, f.state.value)
        verify(exactly = 0) { f.scan.startScan() }
        // collector tidak boleh hidup saat izin kurang
        f.devices.value = listOf(device())
        runCurrent()
        coVerify(exactly = 0) { f.history.record(any()) }
    }

    @Test
    fun given_Scanning_when_StopByUser_then_FlushDanScanDesiredFalse() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        f.devices.value = listOf(device())
        runCurrent()
        coVerify(exactly = 1) { f.history.record(any()) }

        f.controller.stopScanByUser()
        runCurrent()

        verify { f.scan.stopScan(StopReason.USER_STOPPED) }
        coVerify(exactly = 1) { f.history.flush() }
        assertEquals(ScanState.Stopped(StopReason.USER_STOPPED), f.state.value)
        // scanDesired=false: resume berikutnya tidak menghidupkan ulang scan
        f.izinKurang()
        f.controller.onResume()
        verify(exactly = 1) { f.scan.startScan() }
        // collector dibatalkan: device baru tidak terekam
        f.devices.value = listOf(device("AA:BB:CC:DD:EE:02"))
        runCurrent()
        coVerify(exactly = 1) { f.history.record(any()) }
    }

    @Test
    fun given_Scanning_when_Background_then_StopUserDanFlush() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        runCurrent()

        f.controller.onBackground()
        runCurrent()

        verify { f.scan.stopScan(StopReason.USER_STOPPED) }
        coVerify(exactly = 1) { f.history.flush() }
        assertEquals(ScanState.Stopped(StopReason.USER_STOPPED), f.state.value)
    }

    @Test
    fun given_BackgroundLaluResumeIzinAda_when_Resume_then_StartScanLagi() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        f.controller.onBackground()
        runCurrent()
        verify(exactly = 1) { f.scan.startScan() }

        f.controller.onResume()
        runCurrent()

        verify(exactly = 2) { f.scan.startScan() }
        assertEquals(ScanState.Scanning, f.state.value)
        // collector hidup kembali dan merekam device baru
        f.devices.value = listOf(device())
        runCurrent()
        coVerify(exactly = 1) { f.history.record(any()) }
    }

    @Test
    fun given_IzinDicabutSaBackground_when_Resume_then_BannerTanpaScan() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        f.controller.onBackground()
        runCurrent()
        f.izinKurang()

        f.controller.onResume()
        runCurrent()

        verify(exactly = 1) { f.scan.startScan() }
        verify { f.scan.stopScan(StopReason.PERMISSION_REVOKED) }
        assertEquals(ScanState.Stopped(StopReason.PERMISSION_REVOKED), f.state.value)
    }

    @Test
    fun given_BluetoothMati_when_RequestScan_then_HasilBluetoothOffTanpaStart() = runTest {
        val f = Fixture(backgroundScope)
        every { f.checker.check() } returns PermissionCheck.BluetoothOff

        val result = f.controller.requestScan()

        assertEquals(PermissionCheck.BluetoothOff, result)
        verify(exactly = 0) { f.scan.startScan() }
    }

    @Test
    fun given_BluetoothMatiSaatBackground_when_ResumeSetelahBtOff_then_TidakAutoResume() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.requestScan()
        f.controller.onBackground()
        runCurrent()

        f.controller.onBluetoothStateChanged(false)
        f.controller.onResume()
        runCurrent()

        verify(exactly = 1) { f.scan.startScan() }
    }

    @Test
    fun given_BluetoothMatiSaatResume_when_Resume_then_StoppedBluetoothOff() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.requestScan()
        f.controller.onBackground()
        runCurrent()
        every { f.checker.check() } returns PermissionCheck.BluetoothOff

        f.controller.onResume()
        runCurrent()

        verify(exactly = 1) { f.scan.startScan() }
        assertEquals(ScanState.Stopped(StopReason.BLUETOOTH_OFF), f.state.value)
    }

    @Test
    fun given_Scanning_when_ApplyConfigBerbeda_then_SaveDanRestart() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.requestScan()
        runCurrent()
        val baru = ScanConfig(ScanMode.HEMAT, extended = true)

        f.controller.applyScanConfig(baru)
        runCurrent()

        assertEquals(listOf(baru), f.store.saved)
        verify(exactly = 2) { f.scan.startScan() }
        assertEquals(ScanState.Scanning, f.state.value)
    }

    @Test
    fun given_Scanning_when_ApplyConfigSama_then_TidakRestart() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.requestScan()
        runCurrent()

        f.controller.applyScanConfig(ScanConfig())
        runCurrent()

        assertTrue(f.store.saved.isEmpty())
        verify(exactly = 1) { f.scan.startScan() }
    }

    @Test
    fun given_Idle_when_ApplyConfig_then_SaveTanpaStart() = runTest {
        val f = Fixture(backgroundScope)
        val baru = ScanConfig(ScanMode.SEIMBANG, extended = false)

        f.controller.applyScanConfig(baru)
        runCurrent()

        assertEquals(listOf(baru), f.store.saved)
        verify(exactly = 0) { f.scan.startScan() }
    }

    @Test
    fun given_Scanning_when_BluetoothMati_then_StoppedBluetoothOffDanFlush() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        runCurrent()

        f.controller.onBluetoothStateChanged(false)
        runCurrent()

        verify { f.scan.stopScan(StopReason.BLUETOOTH_OFF) }
        coVerify(exactly = 1) { f.history.flush() }
        assertEquals(ScanState.Stopped(StopReason.BLUETOOTH_OFF), f.state.value)
    }

    @Test
    fun given_RepoSelfStop_when_StartUlang_then_RecordTidakDuplikat() = runTest {
        val f = Fixture(backgroundScope)

        f.controller.requestScan()
        f.devices.value = listOf(device())
        runCurrent()
        coVerify(exactly = 1) { f.history.record(any()) }

        // repo self-stop karena source gagal — transisi tanpa lewat controller
        f.state.value = ScanState.Stopped(StopReason.SCAN_FAILED_OTHER)
        runCurrent()

        f.controller.requestScan()
        f.devices.value = listOf(device("AA:BB:CC:DD:EE:02"))
        runCurrent()

        // kolektor lama yang bocor akan mencatat emission kedua dua kali
        coVerify(exactly = 1) { f.history.record(match { it.mac == "AA:BB:CC:DD:EE:02" }) }
        coVerify(exactly = 2) { f.history.record(any()) }
    }
}
