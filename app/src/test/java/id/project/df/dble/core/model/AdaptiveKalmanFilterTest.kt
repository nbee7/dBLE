package id.project.df.dble.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AdaptiveKalmanFilterTest {

    private val start = AdaptiveKalmanState(estimate = -60.0, variance = 16.0, innovationMean = 0.0)

    @Test
    fun given_SampelPertama_when_AdaptiveKalman_then_EstimateSamaDenganRssiDanInovasiNol() {
        val result = adaptiveKalman(previous = null, rssi = -60, dtMillis = 0)

        assertEquals(-60.0, result.estimate, 1e-9)
        assertEquals(KALMAN_R, result.variance, 1e-9)
        assertEquals(0.0, result.innovationMean, 1e-9)
    }

    @Test
    fun given_SampelKedua_when_AdaptiveKalman1Detik_then_NilaiTerhitung() {
        val result = adaptiveKalman(start, rssi = -70, dtMillis = 1_000)

        assertEquals(-65.2048, result.estimate, 1e-3)
        assertEquals(8.3277, result.variance, 1e-3)
        assertEquals(-2.0, result.innovationMean, 1e-9)
    }

    @Test
    fun given_PerubahanBerkelanjutan_when_AdaptiveKalman_then_LebihCepatDariKalmanLambat() {
        var adaptive: AdaptiveKalmanState = start
        var slow = KalmanState(start.estimate, start.variance)
        repeat(10) {
            adaptive = adaptiveKalman(adaptive, rssi = -70, dtMillis = 300)
            slow = kalman(slow, rssi = -70, dtMillis = 300, q = ADAPTIVE_KALMAN_Q_MIN)
        }

        assertTrue(abs(adaptive.estimate + 70) < abs(slow.estimate + 70))
    }

    @Test
    fun given_NoiseBolakBalikDiAtasNilaiTetap_when_AdaptiveKalman_then_EstimateTetapDekatRataRata() {
        var state: AdaptiveKalmanState? = null
        repeat(40) { i ->
            state = adaptiveKalman(state, rssi = if (i % 2 == 0) -56 else -64, dtMillis = 300)
        }

        assertTrue(abs(requireNotNull(state).estimate + 60) < 2.0)
    }

    @Test
    fun given_DtNegatif_when_AdaptiveKalman_then_DiperlakukanSebagaiNol() {
        val negative = adaptiveKalman(start, rssi = -70, dtMillis = -500)
        val zero = adaptiveKalman(start, rssi = -70, dtMillis = 0)

        assertEquals(zero, negative)
    }

    @Test
    fun given_DtNol_when_AdaptiveKalman_then_BukanNaNDanDalamRentang() {
        val result = adaptiveKalman(start, rssi = -70, dtMillis = 0)

        assertTrue(result.estimate.isFinite())
        assertTrue(result.estimate in -70.0..-60.0)
    }
}
