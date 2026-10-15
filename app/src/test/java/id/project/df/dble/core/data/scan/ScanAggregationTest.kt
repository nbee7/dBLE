package id.project.df.dble.core.data.scan

import id.project.df.dble.core.model.BleAdvertise
import id.project.df.dble.core.model.ProximityZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanAggregationTest {

    @Test
    fun given_AdPertama_when_Upsert_then_EmaEqualRawDanNamaTerisi() {
        val ad = BleAdvertise(mac = "AA:BB:CC:DD:EE:01", name = "Tag A", rssi = -60)

        val result = ScanAggregation.upsert(emptyMap(), ad, nowMillis = 1_000L)

        val device = requireNotNull(result["AA:BB:CC:DD:EE:01"])
        assertEquals(-60.0, device.smoothedRssi, 1e-9)
        assertEquals(-60, device.rawRssi)
        assertEquals("Tag A", device.name)
        assertEquals(ProximityZone.CUKUP, device.zone)
        assertEquals(6.5, requireNotNull(device.distanceMeters), 1e-9)
        assertEquals(1_000L, device.lastSeenMillis)
    }

    @Test
    fun given_AdBerikutnya_when_Upsert_then_EmaDiperbaruiDanNamaTidakDitimpaNull() {
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
        // 0,3×(−70) + 0,7×(−60)
        assertEquals(-63.0, device.smoothedRssi, 1e-9)
        assertEquals(-70, device.rawRssi)
        assertEquals("Tag A", device.name)
        assertEquals(ProximityZone.CUKUP, device.zone)
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
