package id.project.df.dble.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StalenessTest {

    @Test
    fun given_Iklan10DetikLalu_when_IsStale_then_False() {
        assertFalse(isStale(nowMillis = 10_000, lastSeenMillis = 0))
    }

    @Test
    fun given_Iklan10DetikLebih_when_IsStale_then_True() {
        assertTrue(isStale(nowMillis = 10_001, lastSeenMillis = 0))
    }
}
