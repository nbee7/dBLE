package id.project.df.dble.core.data.permission

import android.Manifest
import android.os.Build

// Lokasi wajib di semua versi: tanpa neverForLocation, Android 12+ baru meneruskan paket beacon (iBeacon) ke app.
fun blePermissionsFor(sdkInt: Int): List<String> =
    if (sdkInt >= Build.VERSION_CODES.S) {
        listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
