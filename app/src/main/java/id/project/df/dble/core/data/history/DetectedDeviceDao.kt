package id.project.df.dble.core.data.history

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DetectedDeviceDao {

    @Query(
        """
        INSERT INTO detected_devices (mac, name, lastRssi, lastZone, firstSeenMillis, lastSeenMillis)
        VALUES (:mac, :name, :lastRssi, :lastZone, :firstSeenMillis, :lastSeenMillis)
        ON CONFLICT(mac) DO UPDATE SET
            name = excluded.name,
            lastRssi = excluded.lastRssi,
            lastZone = excluded.lastZone,
            lastSeenMillis = excluded.lastSeenMillis
        """,
    )
    abstract suspend fun upsert(
        mac: String,
        name: String?,
        lastRssi: Int,
        lastZone: String,
        firstSeenMillis: Long,
        lastSeenMillis: Long,
    )

    @Transaction
    open suspend fun upsertAll(devices: List<DetectedDeviceEntity>) {
        devices.forEach { upsert(it.mac, it.name, it.lastRssi, it.lastZone, it.firstSeenMillis, it.lastSeenMillis) }
    }

    @Query("SELECT * FROM detected_devices ORDER BY lastSeenMillis DESC")
    abstract fun observeHistory(): Flow<List<DetectedDeviceEntity>>

    @Query("DELETE FROM detected_devices WHERE mac = :mac")
    abstract suspend fun delete(mac: String)

    @Query("DELETE FROM detected_devices")
    abstract suspend fun clearAll()
}
