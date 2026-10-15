package id.project.df.dble.core.data.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class BlePermissionsTest {

    @Test
    fun given_Api30_when_RequiredPermissions_then_HanyaLokasi() {
        assertEquals(listOf(Manifest.permission.ACCESS_FINE_LOCATION), blePermissionsFor(30))
    }

    @Test
    fun given_Api31_when_RequiredPermissions_then_ScanDanLokasi() {
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION),
            blePermissionsFor(31),
        )
    }

    @Test
    fun given_Api34_when_RequiredPermissions_then_ScanDanLokasi() {
        assertEquals(
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.ACCESS_FINE_LOCATION),
            blePermissionsFor(34),
        )
    }
}
