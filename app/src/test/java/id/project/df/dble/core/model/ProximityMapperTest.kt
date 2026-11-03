package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ProximityMapperTest {

    @Test
    fun should_ZonaTepat_When_BatasPersis() {
        mapOf(
            -59 to ProximityZone.SANGAT_KUAT, -60 to ProximityZone.KUAT, -70 to ProximityZone.KUAT,
            -71 to ProximityZone.CUKUP, -78 to ProximityZone.CUKUP, -79 to ProximityZone.LEMAH,
            -85 to ProximityZone.LEMAH, -86 to ProximityZone.SANGAT_LEMAH, -90 to ProximityZone.SANGAT_LEMAH,
            -91 to ProximityZone.LOST, -100 to ProximityZone.LOST,
        ).forEach { (rssi, zone) -> assertEquals("rssi=$rssi", zone, zoneFor(rssi)) }
    }

    @Test
    fun should_BatasZonaSesuaiJarak_When_DipetakanDenganModelJarak() {
        // batas zona = ~1 m, ~3 m, ~6 m, ~12 m menurut model log-distance
        mapOf(-59 to 1.0, -70 to 3.0, -78 to 6.0, -85 to 12.0).forEach { (rssi, meters) ->
            val actual = requireNotNull(distanceMetersFor(rssi))
            assertEquals("rssi=$rssi", meters, actual, meters * 0.15)
        }
    }

    @Test
    fun should_BeepHanyaDiDalam3Meter_When_ZonaSangatKuatAtauKuat() {
        assertEquals(150L, beepIntervalMsFor(zoneFor(-55)))
        assertEquals(600L, beepIntervalMsFor(zoneFor(-70)))
        assertNull(beepIntervalMsFor(zoneFor(-71)))
    }

    @Test
    fun should_JarakLogDistance_When_RssiDiberikan() {
        // A = -59 dBm di 1 m, n = 2,4: d = 10^((A - rssi) / (10 n))
        assertEquals(1.0, requireNotNull(distanceMetersFor(-59)), 1e-9)
        assertEquals(10.0, requireNotNull(distanceMetersFor(-83)), 1e-9)
        assertEquals(10.0.pow(14.0 / 24.0), requireNotNull(distanceMetersFor(-73)), 1e-9)
        assertEquals(10.0.pow(31.0 / 24.0), requireNotNull(distanceMetersFor(-90)), 1e-9)
    }

    @Test
    fun should_JarakMinimal_When_RssiSangatKuat() {
        assertEquals(0.1, requireNotNull(distanceMetersFor(-35)), 1e-9)
        assertEquals(0.1, requireNotNull(distanceMetersFor(-20)), 1e-9)
    }

    @Test
    fun should_JarakNull_When_RssiDiBawahMinus90() {
        assertNull(distanceMetersFor(-91))
        assertNull(distanceMetersFor(-95))
    }

    @Test
    fun should_JarakNaikMonoton_When_RssiMelemah() {
        val distances = (-30 downTo -90).map { requireNotNull(distanceMetersFor(it)) }
        distances.zipWithNext().forEach { (near, far) -> assertTrue("near=$near far=$far", far >= near) }
    }

    @Test
    fun should_BeepDanFormat_When_ZonaDiberikan() {
        assertEquals(150L, beepIntervalMsFor(ProximityZone.SANGAT_KUAT))
        assertEquals(600L, beepIntervalMsFor(ProximityZone.KUAT))
        assertNull(beepIntervalMsFor(ProximityZone.CUKUP))
        assertNull(beepIntervalMsFor(ProximityZone.LOST))
        assertEquals("~2,4 m", formatDistance(2.44, ProximityZone.KUAT))
        assertEquals("<1 m", formatDistance(0.5, ProximityZone.SANGAT_KUAT))
        assertEquals("—", formatDistance(null, ProximityZone.LOST))
    }
}
