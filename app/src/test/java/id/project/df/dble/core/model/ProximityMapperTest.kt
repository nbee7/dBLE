package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProximityMapperTest {

    @Test
    fun should_ZonaTepat_When_BatasPersis() {
        mapOf(
            -30 to ProximityZone.SANGAT_KUAT, -31 to ProximityZone.KUAT, -50 to ProximityZone.KUAT,
            -51 to ProximityZone.CUKUP, -70 to ProximityZone.CUKUP, -71 to ProximityZone.LEMAH,
            -80 to ProximityZone.LEMAH, -81 to ProximityZone.SANGAT_LEMAH, -90 to ProximityZone.SANGAT_LEMAH,
            -91 to ProximityZone.LOST, -100 to ProximityZone.LOST,
        ).forEach { (rssi, zone) -> assertEquals("rssi=$rssi", zone, zoneFor(rssi)) }
    }

    @Test
    fun should_JarakJangkarDanInterpolasi_When_RssiDiberikan() {
        assertEquals(1.0, requireNotNull(distanceMetersFor(-30)), 1e-9)
        assertEquals(3.0, requireNotNull(distanceMetersFor(-50)), 1e-9)
        assertEquals(10.0, requireNotNull(distanceMetersFor(-70)), 1e-9)
        assertEquals(20.0, requireNotNull(distanceMetersFor(-80)), 1e-9)
        // antara −70..−80: 10 + (20−10)×(3/10)
        assertEquals(13.0, requireNotNull(distanceMetersFor(-73)), 1e-9)
        // jangkar bawah rentang Sangat Lemah (−80..−90 → 20–25 m)
        assertEquals(25.0, requireNotNull(distanceMetersFor(-90)), 1e-9)
        assertEquals(0.5, requireNotNull(distanceMetersFor(-20)), 1e-9)
        assertNull(distanceMetersFor(-95))
    }

    @Test
    fun should_BeepDanFormat_When_ZonaDiberikan() {
        assertEquals(150L, beepIntervalMsFor(ProximityZone.SANGAT_KUAT))
        assertEquals(600L, beepIntervalMsFor(ProximityZone.KUAT))
        assertNull(beepIntervalMsFor(ProximityZone.CUKUP))
        assertNull(beepIntervalMsFor(ProximityZone.LOST))
        assertEquals("~2,4 m", formatDistance(2.44, ProximityZone.KUAT))
        assertEquals("<1 m", formatDistance(0.5, ProximityZone.SANGAT_KUAT))
        assertEquals(">20 m", formatDistance(22.0, ProximityZone.SANGAT_LEMAH))
        assertEquals("—", formatDistance(null, ProximityZone.LOST))
    }
}
