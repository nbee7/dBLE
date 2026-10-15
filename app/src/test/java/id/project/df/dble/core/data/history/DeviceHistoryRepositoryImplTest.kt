package id.project.df.dble.core.data.history

import app.cash.turbine.test
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.model.DetectedDevice
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedDevice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceHistoryRepositoryImplTest {

    private val MacA = "AA:BB:CC:DD:EE:01"
    private val MacB = "AA:BB:CC:DD:EE:02"

    private fun dao(): DetectedDeviceDao = mockk(relaxed = true)

    private fun TestScope.virtualTime(): TimeProvider = object : TimeProvider {
        override fun nowMillis(): Long = testScheduler.currentTime
    }

    private fun tracked(mac: String, rssi: Int = -60, lastSeen: Long) = TrackedDevice(
        mac = mac,
        name = "Tag $mac",
        rawRssi = rssi,
        smoothedRssi = rssi.toDouble(),
        zone = ProximityZone.KUAT,
        distanceMeters = 3.0,
        lastSeenMillis = lastSeen,
    )

    private fun entity(mac: String, rssi: Int = -60, firstSeen: Long, lastSeen: Long) = DetectedDeviceEntity(
        mac = mac,
        name = "Tag $mac",
        lastRssi = rssi,
        lastZone = ProximityZone.KUAT.name,
        firstSeenMillis = firstSeen,
        lastSeenMillis = lastSeen,
    )

    @Test
    fun given_RekorPertama_when_Record_then_LangsungTulis() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 1_000L))

        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 1_000L, lastSeen = 1_000L)))
        }
    }

    @Test
    fun given_Barutulis2DetikLalu_when_Record_then_MasukPendingTidakTulis() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 1_000L))
        advanceTimeBy(2_000L)
        repo.record(tracked(MacA, rssi = -70, lastSeen = 3_000L))

        coVerify(exactly = 1) { dao.upsertAll(any()) }
        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 1_000L, lastSeen = 1_000L)))
        }
    }

    @Test
    fun given_PendingTerisi_when_Flush_then_UpsertPendingDanKosong() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 1_000L))
        advanceTimeBy(2_000L)
        repo.record(tracked(MacA, rssi = -70, lastSeen = 3_000L))

        repo.flush()

        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, rssi = -70, firstSeen = 3_000L, lastSeen = 3_000L)))
        }
        repo.flush()
        coVerify(exactly = 2) { dao.upsertAll(any()) }
    }

    @Test
    fun given_Lebih5Detik_when_Record_then_TulisLagi() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 0L))
        advanceTimeBy(5_001L)
        repo.record(tracked(MacA, lastSeen = 5_001L))

        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 0L, lastSeen = 0L)))
        }
        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 5_001L, lastSeen = 5_001L)))
        }
    }

    @Test
    fun given_ObserveHistory_when_DaoEmit_then_TermappingKeDetectedDeviceUrutDb() = runTest {
        val dao = dao()
        every { dao.observeHistory() } returns flowOf(
            listOf(
                entity(MacB, rssi = -55, firstSeen = 2_000L, lastSeen = 2_000L),
                entity(MacA, rssi = -60, firstSeen = 1_000L, lastSeen = 1_000L),
            )
        )
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.observeHistory().test {
            assertEquals(
                listOf(
                    DetectedDevice(MacB, "Tag $MacB", -55, ProximityZone.KUAT, 2_000L, 2_000L),
                    DetectedDevice(MacA, "Tag $MacA", -60, ProximityZone.KUAT, 1_000L, 1_000L),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun given_RecordUlangDiWindowBeda_when_Upsert_then_FirstSeenPertamaBertahan() = runTest {
        val dao = dao()
        val stored = mutableMapOf<String, DetectedDeviceEntity>()
        coEvery { dao.upsertAll(any()) } answers {
            firstArg<List<DetectedDeviceEntity>>().forEach { row ->
                stored.merge(row.mac, row) { old, new -> new.copy(firstSeenMillis = old.firstSeenMillis) }
            }
        }
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 1_000L))
        advanceTimeBy(10_000L)
        repo.record(tracked(MacA, lastSeen = 11_000L))

        assertEquals(1_000L, stored.getValue(MacA).firstSeenMillis)
        assertEquals(11_000L, stored.getValue(MacA).lastSeenMillis)
    }

    @Test
    fun given_PendingUsang_when_TulisLangsungBaru_lalu_Flush_then_SnapshotTerbaruYangTersimpan() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 0L))
        advanceTimeBy(2_000L)
        repo.record(tracked(MacA, rssi = -80, lastSeen = 2_000L))
        advanceTimeBy(3_001L)
        repo.record(tracked(MacA, rssi = -60, lastSeen = 5_001L))
        repo.flush()

        val tulisan = mutableListOf<List<DetectedDeviceEntity>>()
        coVerify(exactly = 2) { dao.upsertAll(capture(tulisan)) }
        assertEquals(
            entity(MacA, rssi = -60, firstSeen = 5_001L, lastSeen = 5_001L),
            tulisan.last().single(),
        )
    }

    @Test
    fun given_FlushGagal_when_Retry_then_PendingTertulisDanThrottleTidakNumpangNaik() = runTest {
        val dao = dao()
        var writes = 0
        coEvery { dao.upsertAll(any()) } answers {
            writes++
            if (writes == 2) throw RuntimeException("Room gagal")
        }
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 0L))
        advanceTimeBy(3_000L)
        repo.record(tracked(MacA, rssi = -70, lastSeen = 3_000L))
        try {
            repo.flush()
            fail("flush pertama harus gagal")
        } catch (expected: RuntimeException) {
        }

        repo.flush()

        advanceTimeBy(5_001L)
        repo.record(tracked(MacA, lastSeen = 8_001L))

        coVerify(exactly = 4) { dao.upsertAll(any()) }
        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 0L, lastSeen = 0L)))
        }
        coVerify(exactly = 2) {
            dao.upsertAll(listOf(entity(MacA, rssi = -70, firstSeen = 3_000L, lastSeen = 3_000L)))
        }
        coVerify(exactly = 1) {
            dao.upsertAll(listOf(entity(MacA, firstSeen = 8_001L, lastSeen = 8_001L)))
        }
    }

    @Test
    fun given_RekorPending_when_DeleteMac_then_FlushTidakMenghidupkanBalik() = runTest {
        val dao = dao()
        val repo = DeviceHistoryRepositoryImpl(dao, virtualTime())

        repo.record(tracked(MacA, lastSeen = 0L))
        advanceTimeBy(1_000L)
        repo.record(tracked(MacA, rssi = -70, lastSeen = 1_000L))

        repo.delete(MacA)
        repo.flush()

        coVerify(exactly = 1) { dao.delete(MacA) }
        coVerify(exactly = 1) { dao.upsertAll(any()) }
    }
}
