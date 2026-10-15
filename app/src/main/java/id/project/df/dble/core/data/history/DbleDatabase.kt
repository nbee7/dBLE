package id.project.df.dble.core.data.history

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DetectedDeviceEntity::class], version = 1, exportSchema = false)
abstract class DbleDatabase : RoomDatabase() {
    abstract fun detectedDeviceDao(): DetectedDeviceDao
}
