package id.project.df.dble.core.model

enum class ProximityZone(val label: String) {
    SANGAT_KUAT("Sangat Kuat"),
    KUAT("Kuat"),
    CUKUP("Cukup"),
    LEMAH("Lemah"),
    SANGAT_LEMAH("Sangat Lemah"),
    LOST("Lost"),
}

fun zoneFor(rssi: Int): ProximityZone = when {
    rssi >= -30 -> ProximityZone.SANGAT_KUAT
    rssi >= -50 -> ProximityZone.KUAT
    rssi >= -70 -> ProximityZone.CUKUP
    rssi >= -80 -> ProximityZone.LEMAH
    rssi >= -90 -> ProximityZone.SANGAT_LEMAH
    else -> ProximityZone.LOST
}


private val DistanceAnchors = listOf(
    -20 to 0.5,
    -30 to 1.0,
    -50 to 3.0,
    -70 to 10.0,
    -80 to 20.0,
    -90 to 25.0,
)

fun distanceMetersFor(rssi: Int): Double? {
    if (rssi < -90) return null
    val r = rssi.coerceIn(-90, -20)
    val upper = DistanceAnchors.indexOfFirst { it.first <= r }
    if (upper == 0) return DistanceAnchors[0].second
    val (rssiHi, metersHi) = DistanceAnchors[upper - 1]
    val (rssiLo, metersLo) = DistanceAnchors[upper]
    val t = (rssiHi - r).toDouble() / (rssiHi - rssiLo)
    return metersHi + (metersLo - metersHi) * t
}

fun beepIntervalMsFor(zone: ProximityZone): Long? = when (zone) {
    ProximityZone.SANGAT_KUAT -> 150L
    ProximityZone.KUAT -> 600L
    else -> null
}

const val EMA_ALPHA = 0.3
const val KALMAN_Q = 0.5
const val KALMAN_R = 16.0

fun ema(previous: Double?, current: Int, alpha: Double = EMA_ALPHA): Double =
    previous?.let { alpha * current + (1 - alpha) * it } ?: current.toDouble()

// need new file
data class KalmanState(val estimate: Double, val variance: Double)

fun kalman(
    previous: KalmanState?,
    rssi: Int,
    dtMillis: Long,
    q: Double = KALMAN_Q,
    r: Double = KALMAN_R,
): KalmanState {
    if (previous == null) return KalmanState(rssi.toDouble(), r)
    val predicted = previous.variance + q * dtMillis.coerceAtLeast(0) / 1_000.0
    val gain = predicted / (predicted + r)
    return KalmanState(
        estimate = previous.estimate + gain * (rssi - previous.estimate),
        variance = (1 - gain) * predicted,
    )
}

data class BleAdvertise(val mac: String, val name: String?, val rssi: Int)

data class TrackedDevice(
    val mac: String,
    val name: String?,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val zone: ProximityZone,
    val distanceMeters: Double?,
    val lastSeenMillis: Long,
    val kalmanRssi: Double = smoothedRssi,
    val kalmanVariance: Double = KALMAN_R,
)

data class DetectedDevice(
    val mac: String,
    val name: String?,
    val lastRssi: Int,
    val lastZone: ProximityZone,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
)

data class TrackedTarget(
    val mac: String,
    val name: String?,
    val rawRssi: Int,
    val smoothedRssi: Double,
    val zone: ProximityZone,
    val distanceMeters: Double?,
    val isLost: Boolean,
    val kalmanRssi: Double = smoothedRssi,
)
