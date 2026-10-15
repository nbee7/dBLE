package id.project.df.dble.core.data.history

import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.model.DetectedDevice
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val WriteThrottleMs = 5_000L

@Singleton
class DeviceHistoryRepositoryImpl @Inject constructor(
    private val dao: DetectedDeviceDao,
    private val time: TimeProvider,
) : DeviceHistoryRepository {

    override fun observeHistory(): Flow<List<DetectedDevice>> =
        dao.observeHistory().map { rows -> rows.map(DetectedDeviceEntity::toModel) }

    private val mutex = Mutex()
    private val lastWriteAt = mutableMapOf<String, Long>()
    private val pending = mutableMapOf<String, DetectedDeviceEntity>()

    override suspend fun record(device: TrackedDevice) {
        val entity = device.toEntity()
        mutex.withLock {
            val now = time.nowMillis()
            val last = lastWriteAt[device.mac]
            if (last != null && now - last < WriteThrottleMs) {
                pending[device.mac] = entity
                return
            }
            dao.upsertAll(listOf(entity))
            lastWriteAt[device.mac] = now
            pending.remove(device.mac)
            if (last == null) Timber.d("[HISTORY][NEW] mac=%s", device.mac)
        }
    }

    override suspend fun flush() {
        mutex.withLock {
            if (pending.isEmpty()) return
            val now = time.nowMillis()
            val toWrite = pending.values.toList()
            dao.upsertAll(toWrite)
            toWrite.forEach { lastWriteAt[it.mac] = now }
            pending.clear()
            Timber.d("[HISTORY][FLUSH] pending=%d", toWrite.size)
        }
    }

    override suspend fun delete(mac: String) {
        mutex.withLock {
            dao.delete(mac)
            pending.remove(mac)
            Timber.d("[HISTORY][DELETE] mac=%s", mac)
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            dao.clearAll()
            pending.clear()
            Timber.d("[HISTORY][CLEAR]")
        }
    }
}

private fun DetectedDeviceEntity.toModel() = DetectedDevice(
    mac = mac,
    name = name,
    lastRssi = lastRssi,
    lastZone = ProximityZone.valueOf(lastZone),
    firstSeenMillis = firstSeenMillis,
    lastSeenMillis = lastSeenMillis,
)

private fun TrackedDevice.toEntity() = DetectedDeviceEntity(
    mac = mac,
    name = name,
    lastRssi = rawRssi,
    lastZone = zone.name,
    firstSeenMillis = lastSeenMillis,
    lastSeenMillis = lastSeenMillis,
)
