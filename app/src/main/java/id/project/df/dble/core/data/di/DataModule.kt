package id.project.df.dble.core.data.di

import id.project.df.dble.core.common.time.SystemTimeProvider
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.ble.AndroidBleScannerSource
import id.project.df.dble.core.data.ble.BleScannerSource
import id.project.df.dble.core.data.scan.DeviceScanRepository
import id.project.df.dble.core.data.scan.DeviceScanRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBleScannerSource(impl: AndroidBleScannerSource): BleScannerSource

    @Binds
    @Singleton
    abstract fun bindDeviceScanRepository(impl: DeviceScanRepositoryImpl): DeviceScanRepository

    @Binds
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
