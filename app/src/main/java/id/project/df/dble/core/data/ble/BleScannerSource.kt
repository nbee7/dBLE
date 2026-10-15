package id.project.df.dble.core.data.ble

import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.project.df.dble.core.model.BleAdvertise
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface BleScannerSource {
    fun scan(): Flow<BleAdvertise>
}

class AndroidBleScannerSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : BleScannerSource {

    override fun scan(): Flow<BleAdvertise> = callbackFlow {
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            close(BleScanException(ScanCallback.SCAN_FAILED_INTERNAL_ERROR))
            return@callbackFlow
        }
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(BleAdvertise(result.device.address, result.scanRecord?.deviceName?.takeIf { it.isNotBlank() }, result.rssi))
            }

            override fun onScanFailed(errorCode: Int) {
                close(BleScanException(errorCode))
            }
        }
        try {
            scanner.startScan(callback)
        } catch (e: SecurityException) {
            close(e)
            return@callbackFlow
        }
        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: SecurityException) {
                // Izin sudah dicabut; scan berhenti bersama proses izin, tidak ada yang perlu dibersihkan.
            }
        }
    }
}
