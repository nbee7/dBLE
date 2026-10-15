package id.project.df.dble.app

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import id.project.df.dble.core.data.monitor.BluetoothMonitor
import id.project.df.dble.core.data.scan.ScanLifecycleObserver
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class DbleApplication : Application() {

    @Inject lateinit var scanLifecycleObserver: ScanLifecycleObserver

    @Inject lateinit var bluetoothMonitor: BluetoothMonitor

    override fun onCreate() {
        super.onCreate()
        if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Timber.plant(Timber.DebugTree())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(scanLifecycleObserver)
    }
}
