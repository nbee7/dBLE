package id.project.df.dble.core.domain

import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedDevice
import id.project.df.dble.core.model.distanceMetersFor
import id.project.df.dble.core.model.zoneFor
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterDevicesUseCaseTest {

    private val useCase = FilterDevicesUseCase()

    private fun device(
        mac: String,
        name: String?,
        rawRssi: Int = -60,
        smoothedRssi: Double = rawRssi.toDouble(),
    ) = TrackedDevice(
        mac = mac,
        name = name,
        rawRssi = rawRssi,
        smoothedRssi = smoothedRssi,
        zone = zoneFor(smoothedRssi.roundToInt()),
        distanceMeters = distanceMetersFor(smoothedRssi.roundToInt()),
        lastSeenMillis = 0L,
    )

    @Test
    fun given_TanpaNama_when_QueryNama_then_TidakTerpilih() {
        val devices = listOf(device("0F:24:D7:33:9E:C8", name = null))

        val result = useCase(devices, query = "JBL", field = FilterField.NAME, minRssiEnabled = false, minRssi = -80)

        assertTrue(result.isEmpty())
    }

    @Test
    fun given_TanpaNama_when_QueryKosong_then_Tampil() {
        val anonymous = device("0F:24:D7:33:9E:C8", name = null)

        val result = useCase(listOf(anonymous), query = "", field = FilterField.NAME, minRssiEnabled = false, minRssi = -80)

        assertEquals(listOf(anonymous), result)
    }

    @Test
    fun given_QueryMacHurufKecil_when_Filter_then_MatchIgnoreCase() {
        val target = device("8C:1D:96:4F:2A:71", name = "JBL Flip 6")

        val result = useCase(listOf(target), query = "8c:1d", field = FilterField.MAC, minRssiEnabled = false, minRssi = -80)

        assertEquals(listOf(target), result)
    }

    @Test
    fun given_MinRssiAktif_when_Filter_then_RawRssiDiBawahAmbangBuang() {
        val lemah = device("AA:00:00:00:00:01", name = "Lemah", rawRssi = -85)
        val kuat = device("AA:00:00:00:00:02", name = "Kuat", rawRssi = -70)

        val result = useCase(listOf(lemah, kuat), query = "", field = FilterField.NAME, minRssiEnabled = true, minRssi = -80)

        assertEquals(listOf(kuat), result)
    }

    @Test
    fun given_ListCampur_when_Filter_then_UrutSmoothedRssiDesc() {
        val sedang = device("AA:00:00:00:00:01", name = "Sedang", smoothedRssi = -55.0)
        val terkuat = device("AA:00:00:00:00:02", name = "Terkuat", smoothedRssi = -40.0)
        val terlemah = device("AA:00:00:00:00:03", name = "Terlemah", smoothedRssi = -70.0)

        val result = useCase(
            listOf(sedang, terkuat, terlemah),
            query = "",
            field = FilterField.NAME,
            minRssiEnabled = false,
            minRssi = -80,
        )

        assertEquals(listOf(terkuat, sedang, terlemah), result)
        assertEquals(ProximityZone.KUAT, result.first().zone)
    }
}
