package id.project.df.dble.core.data.permission

sealed interface PermissionCheck {
    data object Granted : PermissionCheck
    data class Missing(val permissions: List<String>) : PermissionCheck
    data object LocationServiceOff : PermissionCheck
    data object BluetoothOff : PermissionCheck
}

interface BlePermissionChecker {
    fun check(): PermissionCheck
}
