package id.project.df.dble.core.domain

import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.scan.DeviceScanRepository
import id.project.df.dble.core.model.TrackedDevice
import id.project.df.dble.core.model.TrackedTarget
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class ObserveTargetUseCase @Inject constructor(
    private val scan: DeviceScanRepository,
    private val time: TimeProvider,
) {
    operator fun invoke(mac: String): Flow<TrackedTarget> = flow {
        var last: TrackedDevice? = null
        var wasLost = true
        combine(scan.devices, ticker()) { devices, _ -> devices.firstOrNull { it.mac == mac } }
            .collect { found ->
                if (found != null) last = found
                val device = last ?: return@collect
                val lost = time.nowMillis() - device.lastSeenMillis > LOST_AFTER_MS || device.rawRssi < LOST_RSSI
                if (lost != wasLost) {
                    if (lost) Timber.d("[TRACK][LOST] mac=%s", mac) else Timber.d("[TRACK][FOUND] mac=%s", mac)
                    wasLost = lost
                }
                emit(
                    TrackedTarget(
                        mac = device.mac,
                        name = device.name,
                        rawRssi = device.rawRssi,
                        smoothedRssi = device.smoothedRssi,
                        zone = device.zone,
                        distanceMeters = device.distanceMeters,
                        isLost = lost,
                    ),
                )
            }
    }.distinctUntilChanged()

    private fun ticker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }

    private companion object {
        const val LOST_AFTER_MS = 5_000L
        const val LOST_RSSI = -90
        const val TICK_MS = 1_000L
    }
}
