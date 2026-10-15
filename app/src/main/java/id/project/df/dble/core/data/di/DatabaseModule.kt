package id.project.df.dble.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.project.df.dble.core.data.history.DbleDatabase
import id.project.df.dble.core.data.history.DetectedDeviceDao
import id.project.df.dble.core.data.history.DeviceHistoryRepository
import id.project.df.dble.core.data.history.DeviceHistoryRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindDeviceHistoryRepository(impl: DeviceHistoryRepositoryImpl): DeviceHistoryRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): DbleDatabase =
            Room.databaseBuilder(context, DbleDatabase::class.java, "dble.db").build()

        @Provides
        fun provideDetectedDeviceDao(db: DbleDatabase): DetectedDeviceDao = db.detectedDeviceDao()
    }
}
