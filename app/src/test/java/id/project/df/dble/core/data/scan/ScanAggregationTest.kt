package id.project.df.dble.core.data.scan

import id.project.df.dble.core.model.BleAdvertise
import id.project.df.dble.core.model.KALMAN_R
import id.project.df.dble.core.model.ProximityZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanAggregationTest {

    @Test
    fun given_AdPertama_when_Upsert_then_FilterEqualRawDanNamaTerisi() {
        val ad = BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = "Tag A", rssi = -60)

        val result = ScanAggregation.upsert(emptyMap(), ad, nowMillis = 1_000L)

        val device = requireNotNull(result["AA:BB:CC:DD:EE:01"])
        assertEquals(-60.0, device.smoothedRssi, 1e-9)
        assertEquals(KALMAN_R, device.filterVariance, 1e-9)
        assertEquals(0.0, device.filterInnovation, 1e-9)
        assertEquals(-60, device.rawRssi)
        assertEquals("Tag A", device.name)
        assertEquals(ProximityZone.KUAT, device.zone)
        assertEquals(Math.pow(10.0, 1.0 / 24.0), requireNotNull(device.distanceMeters), 1e-9)
        assertEquals(1_000L, device.lastSeenMillis)
    }

    @Test
    fun given_AdBerikutnya_when_Upsert_then_FilterDiperbaruiDanNamaTidakDitimpaNull() {
        val first = ScanAggregation.upsert(
            emptyMap(),
            BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = "Tag A", rssi = -60),
            nowMillis = 1_000L,
        )

        val result = ScanAggregation.upsert(
            first,
            BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = null, rssi = -70),
            nowMillis = 1_500L,
        )

        val device = requireNotNull(result["AA:BB:CC:DD:EE:01"])
        // Hand-computed: innovation -10, mean -2, q = 0.05 + 3.95 * (2/6), predicted = 16 + q * 0.5
        assertEquals(-65.1046, device.smoothedRssi, 1e-3)
        assertEquals(8.1673, device.filterVariance, 1e-3)
        assertEquals(-2.0, device.filterInnovation, 1e-9)
        assertEquals(-70, device.rawRssi)
        assertEquals("Tag A", device.name)
        assertEquals(ProximityZone.KUAT, device.zone)
        assertEquals(1_500L, device.lastSeenMillis)
        assertEquals(1, result.size)
    }

    @Test
    fun given_Iklan30DetikLalu_when_Prune_then_Dihapus() {
        val map = ScanAggregation.upsert(
            emptyMap(),
            BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = "Tag A", rssi = -60),
            nowMillis = 1_000L,
        )

        val result = ScanAggregation.prune(map, nowMillis = 31_000L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun given_Iklan29DetikLalu_when_Prune_then_Tetap() {
        val map = ScanAggregation.upsert(
            emptyMap(),
            BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = "Tag A", rssi = -60),
            nowMillis = 1_000L,
        )

        val result = ScanAggregation.prune(map, nowMillis = 30_000L)

        assertTrue(result.containsKey("AA:BB:CC:DD:EE:01"))
    }
}
