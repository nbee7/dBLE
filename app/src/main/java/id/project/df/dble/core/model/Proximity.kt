package id.project.df.dble.core.model

import kotlin.math.pow

enum class ProximityZone(val label: String) {
    SANGAT_KUAT("Sangat Kuat"),
    KUAT("Kuat"),
    CUKUP("Cukup"),
    LEMAH("Lemah"),
    SANGAT_LEMAH("Sangat Lemah"),
    LOST("Lost"),
}

// Batas zona dari model jarak di bawah: -59 dBm ≈ 1 m, -70 ≈ 3 m, -78 ≈ 6 m, -85 ≈ 12 m, -90 ≈ 20 m.
// Beep hanya di zona Sangat Kuat dan Kuat, yaitu sampai ±3 m.
fun zoneFor(rssi: Int): ProximityZone = when {
    rssi >= -59 -> ProximityZone.SANGAT_KUAT
    rssi >= -70 -> ProximityZone.KUAT
    rssi >= -78 -> ProximityZone.CUKUP
    rssi >= -85 -> ProximityZone.LEMAH
    rssi >= -90 -> ProximityZone.SANGAT_LEMAH
    else -> ProximityZone.LOST
}

// Model log-distance hasil kalibrasi riset (research/distance), satu HP (V2201) dan satu jenis perangkat.
// Sesi 0,5-5 m: A ≈ -59 dBm, n ≈ 2,4. Perkiraan kasar, sebaran di 3-5 m bisa 2-3x lipat.
private const val DISTANCE_REF_RSSI = -59.0
private const val PATH_LOSS_EXPONENT = 2.4
private const val MIN_DISTANCE_METERS = 0.1

fun distanceMetersFor(rssi: Int): Double? {
    if (rssi < -90) return null
    val meters = 10.0.pow((DISTANCE_REF_RSSI - rssi) / (10 * PATH_LOSS_EXPONENT))
    return meters.coerceAtLeast(MIN_DISTANCE_METERS)
}

fun beepIntervalMsFor(zone: ProximityZone): Long? = when (zone) {
    ProximityZone.SANGAT_KUAT -> 150L
    ProximityZone.KUAT -> 600L
    else -> null
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
    val filterVariance: Double = KALMAN_R,
    val filterInnovation: Double = 0.0,
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
)
