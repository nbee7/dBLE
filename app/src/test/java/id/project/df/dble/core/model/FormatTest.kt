package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun should_FormatJarak_When_ZonaDanNilaiDiberikan() {
        assertEquals("~2,4 m", formatDistance(2.44, ProximityZone.KUAT))
        assertEquals("~1,0 m", formatDistance(1.0, ProximityZone.KUAT))
        assertEquals("~20,0 m", formatDistance(20.0, ProximityZone.LEMAH))
        assertEquals("<1 m", formatDistance(0.99, ProximityZone.KUAT))
        assertEquals("<1 m", formatDistance(0.5, ProximityZone.SANGAT_KUAT))
        assertEquals(">20 m", formatDistance(20.5, ProximityZone.SANGAT_LEMAH))
        assertEquals(">20 m", formatDistance(22.0, ProximityZone.SANGAT_LEMAH))
        assertEquals("—", formatDistance(null, ProximityZone.LOST))
        assertEquals("—", formatDistance(null, ProximityZone.KUAT))
    }
}
