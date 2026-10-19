package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class KalmanFilterTest {

    private val start = KalmanState(estimate = -60.0, variance = 16.0)

    @Test
    fun given_SampelPertama_when_Kalman_then_EstimateSamaDenganRssiDanVarianR() {
        val result = kalman(previous = null, rssi = -60, dtMillis = 0)

        assertEquals(-60.0, result.estimate, 1e-9)
        assertEquals(KALMAN_R, result.variance, 1e-9)
    }

    @Test
    fun given_SampelKedua_when_Kalman1Detik_then_NilaiTerhitung() {
        val result = kalman(start, rssi = -70, dtMillis = 1_000)

        assertEquals(-65.0769, result.estimate, 1e-3)
        assertEquals(8.1231, result.variance, 1e-3)
    }

    @Test
    fun given_JedaPanjang_when_Kalman_then_LebihMengikutiPengukuran() {
        val short = kalman(start, rssi = -70, dtMillis = 100)
        val long = kalman(start, rssi = -70, dtMillis = 60_000)

        assertTrue(abs(long.estimate + 70) < abs(short.estimate + 70))
    }

    @Test
    fun given_DtNol_when_Kalman_then_BukanNaNDanDalamRentang() {
        val result = kalman(start, rssi = -70, dtMillis = 0)

        assertTrue(result.estimate.isFinite())
        assertTrue(result.estimate in -70.0..-60.0)
    }

    @Test
    fun given_DtNegatif_when_Kalman_then_DiperlakukanSebagaiNol() {
        val negative = kalman(start, rssi = -70, dtMillis = -500)
        val zero = kalman(start, rssi = -70, dtMillis = 0)

        assertEquals(zero, negative)
    }
}
