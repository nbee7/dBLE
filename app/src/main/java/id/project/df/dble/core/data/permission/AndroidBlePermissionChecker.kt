package id.project.df.dble.core.data.permission

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidBlePermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context,
) : BlePermissionChecker {

    override fun check(): PermissionCheck {
        val missing = blePermissionsFor(Build.VERSION.SDK_INT).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) return PermissionCheck.Missing(missing)

        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) return PermissionCheck.BluetoothOff

        val lm = context.getSystemService(LocationManager::class.java)
        val providerOn = lm?.isLocationEnabled == true
        if (!providerOn) return PermissionCheck.LocationServiceOff
        return PermissionCheck.Granted
    }
}
