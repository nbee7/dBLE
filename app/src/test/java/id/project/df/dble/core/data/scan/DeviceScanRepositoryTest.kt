package id.project.df.dble.core.data.scan

import android.bluetooth.le.ScanCallback
import id.project.df.dble.core.data.ble.BleScanException
import id.project.df.dble.core.data.ble.BleScannerSource
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.model.BleAdvertise
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.ScanMode
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSource(
    private val script: suspend FlowCollector<BleAdvertise>.() -> Unit,
) : BleScannerSource {
    var collections = 0
        private set
    var lastConfig: ScanConfig? = null
        private set

    override fun scan(config: ScanConfig): Flow<BleAdvertise> = flow {
        collections++
        lastConfig = config
        script()
    }
}

private val FixedTime = object : TimeProvider {
    override fun nowMillis(): Long = 1_000L
}

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceScanRepositoryTest {

    @Test
    fun given_ConfigTersimpan_when_StartScan_then_SourceMenerimaConfig() = runTest {
        val source = FakeSource { awaitCancellation() }
        val config = ScanConfig(ScanMode.SEIMBANG, extended = true)
        val repo = DeviceScanRepositoryImpl(source, backgroundScope, FixedTime, FakeScanConfigStore(config))

        repo.startScan()
        runCurrent()

        assertEquals(config, source.lastConfig)
    }

    @Test
    fun given_SecurityException_when_Scan_then_StopPermissionRevoked() = runTest {
        val source = FakeSource { throw SecurityException("izin dicabut") }
        val repo = DeviceScanRepositoryImpl(source, backgroundScope, FixedTime, FakeScanConfigStore())

        repo.startScan()
        runCurrent()

        assertEquals(ScanState.Stopped(StopReason.PERMISSION_REVOKED), repo.scanState.value)
    }

    @Test
    fun given_StartDuaKali_when_StartScan_then_TetapSatuCollector() = runTest {
        val source = FakeSource { awaitCancellation() }
        val repo = DeviceScanRepositoryImpl(source, backgroundScope, FixedTime, FakeScanConfigStore())

        repo.startScan()
        runCurrent()
        repo.startScan()
        runCurrent()

        assertEquals(1, source.collections)
        assertEquals(ScanState.Scanning, repo.scanState.value)
    }

    @Test
    fun given_SourceLemparTooFrequent_when_Collect_then_StateStoppedTooFrequentlyDanDevicesTidakBertambah() = runTest {
        val source = FakeSource {
            emit(BleAdvertise(mac = "AA:BB:CC:DD:EE:02", name = "Tag B", rssi = -60))
            throw BleScanException(ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY)
        }
        val repo = DeviceScanRepositoryImpl(source, backgroundScope, FixedTime, FakeScanConfigStore())

        repo.startScan()
        runCurrent()

        assertEquals(ScanState.Stopped(StopReason.SCAN_FAILED_TOO_FREQUENTLY), repo.scanState.value)
        // Hanya iklan pra-error yang tersimpan; collector sudah berhenti, tidak ada pertambahan lagi.
        assertEquals(1, repo.devices.value.size)
        assertEquals("AA:BB:CC:DD:EE:02", repo.devices.value.first().mac)
        assertEquals(1, source.collections)
    }

    @Test
    fun given_StartScan_when_AdDiterima_then_DevicesTerisiDanPruneTickerJalan() = runTest {
        // Waktu mengikuti jam virtual scheduler agar ticker prune bisa diuji dengan advanceTimeBy.
        val time = object : TimeProvider {
            override fun nowMillis(): Long = testScheduler.currentTime
        }
        val source = FakeSource {
            emit(BleAdvertise(mac = "AA:BB:CC:DD:EE:03", name = "Tag C", rssi = -60))
            awaitCancellation()
        }
        val repo = DeviceScanRepositoryImpl(source, backgroundScope, time, FakeScanConfigStore())

        repo.startScan()
        runCurrent()

        assertEquals(1, repo.devices.value.size)
        assertEquals("Tag C", repo.devices.value.first().name)

        // Iklan terakhir pada t=0; setelah 31.000 ms wajib ter-prune dari daftar.
        advanceTimeBy(31_000)
        runCurrent()

        assertTrue(repo.devices.value.isEmpty())
        assertEquals(ScanState.Scanning, repo.scanState.value)
    }
}
