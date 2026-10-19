package id.project.df.dble.core.data.scan

import id.project.df.dble.core.model.BleAdvertise
import id.project.df.dble.core.model.TrackedDevice
import id.project.df.dble.core.model.distanceMetersFor
import id.project.df.dble.core.model.KalmanState
import id.project.df.dble.core.model.ema
import id.project.df.dble.core.model.kalman
import id.project.df.dble.core.model.zoneFor
import kotlin.math.roundToInt

object ScanAggregation {

    fun upsert(map: Map<String, TrackedDevice>, ad: BleAdvertise, nowMillis: Long): Map<String, TrackedDevice> {
        val previous = map[ad.mac]
        val smoothed = ema(previous?.smoothedRssi, ad.rssi)
        val filtered = kalman(
            previous = previous?.let { KalmanState(it.kalmanRssi, it.kalmanVariance) },
            rssi = ad.rssi,
            dtMillis = previous?.let { nowMillis - it.lastSeenMillis } ?: 0L,
        )
        val updated = TrackedDevice(
            mac = ad.mac,
            name = previous?.name ?: ad.name,
            rawRssi = ad.rssi,
            smoothedRssi = smoothed,
            zone = zoneFor(smoothed.roundToInt()),
            distanceMeters = distanceMetersFor(smoothed.roundToInt()),
            lastSeenMillis = nowMillis,
            kalmanRssi = filtered.estimate,
            kalmanVariance = filtered.variance,
        )
        return map + (ad.mac to updated)
    }

    fun prune(map: Map<String, TrackedDevice>, nowMillis: Long, removeAfterMs: Long = 30_000): Map<String, TrackedDevice> =
        map.filterValues { nowMillis - it.lastSeenMillis < removeAfterMs }
}
