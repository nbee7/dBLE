package id.project.df.dble.core.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detected_devices")
data class DetectedDeviceEntity(
    @PrimaryKey val mac: String,
    val name: String?,
    val lastRssi: Int,
    val lastZone: String,
    val firstSeenMillis: Long,
    val lastSeenMillis: Long,
)
