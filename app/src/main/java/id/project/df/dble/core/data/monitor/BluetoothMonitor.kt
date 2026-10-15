package id.project.df.dble.core.data.monitor

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import id.project.df.dble.core.data.scan.ScanController
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothMonitor @Inject constructor(
    @ApplicationContext context: Context,
    private val controller: ScanController,
) {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            controller.onBluetoothStateChanged(state == BluetoothAdapter.STATE_ON)
        }
    }

    init {
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }
}
