package id.project.df.dble.core.data.history

import id.project.df.dble.core.model.DetectedDevice
import id.project.df.dble.core.model.TrackedDevice
import kotlinx.coroutines.flow.Flow

interface DeviceHistoryRepository {
    fun observeHistory(): Flow<List<DetectedDevice>>
    suspend fun record(device: TrackedDevice)
    suspend fun flush()
    suspend fun delete(mac: String)
    suspend fun clearAll()
}
