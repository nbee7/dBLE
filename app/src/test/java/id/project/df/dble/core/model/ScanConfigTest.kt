package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ScanConfigTest {

    @Test
    fun given_Default_when_Dibuat_then_CepatTanpaExtended() {
        val config = ScanConfig()

        assertEquals(ScanMode.CEPAT, config.mode)
        assertFalse(config.extended)
    }

    @Test
    fun given_ModeTersimpanValid_when_Parse_then_ModeDipakai() {
        assertEquals(ScanConfig(ScanMode.HEMAT, true), scanConfigFrom("HEMAT", true))
    }

    @Test
    fun given_ModeTidakDikenal_when_Parse_then_KembaliKeDefault() {
        assertEquals(ScanMode.CEPAT, scanConfigFrom("RUSAK", false).mode)
        assertEquals(ScanMode.CEPAT, scanConfigFrom(null, false).mode)
    }
}
