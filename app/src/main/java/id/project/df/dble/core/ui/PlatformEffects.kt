package id.project.df.dble.core.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import id.project.df.dble.core.data.scan.StopReason

@Composable
fun rememberPermissionRequester(onGranted: () -> Unit): (List<String>) -> Unit {
    val currentOnGranted by rememberUpdatedState(onGranted)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.isNotEmpty() && result.values.all { it }) currentOnGranted()
    }
    return { permissions -> launcher.launch(permissions.toTypedArray()) }
}

fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.openAppSettings() {
    val uri = Uri.fromParts("package", packageName, null)
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.openBluetoothSettings() {
    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun StopReason.hasSettingsAction(): Boolean =
    this == StopReason.PERMISSION_REVOKED || this == StopReason.LOCATION_OFF || this == StopReason.BLUETOOTH_OFF

fun Context.openSettingsFor(reason: StopReason) {
    when (reason) {
        StopReason.PERMISSION_REVOKED -> openAppSettings()
        StopReason.LOCATION_OFF -> openLocationSettings()
        StopReason.BLUETOOTH_OFF -> openBluetoothSettings()
        else -> Unit
    }
}
