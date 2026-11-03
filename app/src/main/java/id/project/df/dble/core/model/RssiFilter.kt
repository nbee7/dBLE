package id.project.df.dble.core.model

import kotlin.math.abs

/** Measurement noise variance R in dB² (sigma ≈ 4 dB, typical BLE RSSI jitter). */
const val KALMAN_R = 16.0
/** Process noise Q in dB²/s when the signal is stable (innovation mean ≈ 0). */
const val ADAPTIVE_KALMAN_Q_MIN = 0.05
/** Process noise Q in dB²/s when the signal is changing fast (|innovation mean| ≥ threshold). */
const val ADAPTIVE_KALMAN_Q_MAX = 4.0
/** |smoothed innovation| in dB at which Q saturates at its maximum. */
const val ADAPTIVE_KALMAN_THRESHOLD_DB = 6.0
/** Smoothing factor of the innovation moving average that drives Q adaptation. */
const val ADAPTIVE_KALMAN_BETA = 0.2

/** Per-device filter state; [innovationMean] is the smoothed innovation used to adapt Q. */
data class AdaptiveKalmanState(val estimate: Double, val variance: Double, val innovationMean: Double)

fun adaptiveKalman(
    previous: AdaptiveKalmanState?,
    rssi: Int,
    dtMillis: Long,
    qMin: Double = ADAPTIVE_KALMAN_Q_MIN,
    qMax: Double = ADAPTIVE_KALMAN_Q_MAX,
    thresholdDb: Double = ADAPTIVE_KALMAN_THRESHOLD_DB,
    r: Double = KALMAN_R,
    beta: Double = ADAPTIVE_KALMAN_BETA,
): AdaptiveKalmanState {
    if (previous == null) return AdaptiveKalmanState(rssi.toDouble(), r, 0.0)
    val innovation = rssi - previous.estimate
    val innovationMean = previous.innovationMean + beta * (innovation - previous.innovationMean)
    val weight = minOf(abs(innovationMean) / thresholdDb, 1.0)
    val q = qMin + (qMax - qMin) * weight
    val predicted = previous.variance + q * dtMillis.coerceAtLeast(0) / 1_000.0
    val gain = predicted / (predicted + r)
    return AdaptiveKalmanState(
        estimate = previous.estimate + gain * innovation,
        variance = (1 - gain) * predicted,
        innovationMean = innovationMean,
    )
}
