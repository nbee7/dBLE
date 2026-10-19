package id.project.df.dble.core.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RssiCsvTest {

    private val mac = "AA:BB:CC:DD:EE:01"
    private val columns =
        "t_ms,epoch_ms,dt_ms,mac,raw_rssi,ema_rssi,kalman_rssi,raw_zone,ema_zone,kalman_zone,raw_dist_m,ema_dist_m,kalman_dist_m,adkalman_rssi,adkalman_zone,adkalman_dist_m"

    private fun meta(name: String? = "Tag A") = ExportMeta(
        mac = mac,
        name = name,
        deviceModel = "Pixel",
        appVersion = "0.5.0",
        startedAtMillis = 10_000,
        endedAtMillis = 10_500,
    )

    private fun sample(t: Long, raw: Int, ema: Double, kalman: Double, adKalman: Double = kalman) =
        RssiSample(tMillis = t, mac = mac, rawRssi = raw, emaRssi = ema, kalmanRssi = kalman, adKalmanRssi = adKalman)

    private val twoSamples = listOf(
        sample(10_000, -60, -60.0, -60.0),
        sample(10_500, -70, -63.0, -65.08),
    )

    private fun dataLines(csv: String) = csv.lines().filter { it.isNotEmpty() && !it.startsWith("#") }

    @Test
    fun given_DuaSampel_when_Build_then_HeaderKolomDanBarisSesuai() {
        val lines = dataLines(RssiCsv.build(meta(), twoSamples))

        assertEquals(columns, lines[0])
        assertEquals("0,10000,0,$mac,-60,-60.00,-60.00,CUKUP,CUKUP,CUKUP,6.50,6.50,6.50,-60.00,CUKUP,6.50", lines[1])
        assertTrue(lines[2].startsWith("500,10500,500,$mac,-70,-63.00,-65.08,"))
    }

    @Test
    fun given_Sampel_when_Build_then_HeaderBerisiParameterFilter() {
        val csv = RssiCsv.build(meta(), twoSamples)

        assertTrue(csv.contains("# ema_alpha=0.3"))
        assertTrue(csv.contains("# kalman_q=0.5"))
        assertTrue(csv.contains("# kalman_r=16.0"))
        assertTrue(csv.contains("# adkalman_q_min=0.05"))
        assertTrue(csv.contains("# adkalman_q_max=4.0"))
        assertTrue(csv.contains("# adkalman_threshold_db=6.0"))
        assertTrue(csv.contains("# adkalman_r=16.0"))
        assertTrue(csv.contains("# adkalman_beta=0.2"))
        assertTrue(csv.contains("# sample_count=2"))
    }

    @Test
    fun given_RssiDiBawahMinus90_when_Build_then_JarakKosongDanZonaLost() {
        val csv = RssiCsv.build(meta(), listOf(sample(10_000, -95, -60.0, -60.0)))

        assertEquals(
            "0,10000,0,$mac,-95,-60.00,-60.00,LOST,CUKUP,CUKUP,,6.50,6.50,-60.00,CUKUP,6.50",
            dataLines(csv)[1],
        )
    }

    @Test
    fun given_NamaBerisiBarisBaruDanKoma_when_Build_then_HeaderTetapSatuBaris() {
        val csv = RssiCsv.build(meta(name = "Tag\nA,B"), twoSamples)

        assertEquals(columns, dataLines(csv)[0])
        assertEquals(1, csv.lines().count { it.startsWith("# name=") })
    }

    @Test
    fun given_ListKosong_when_Build_then_HanyaHeaderDanKolom() {
        val csv = RssiCsv.build(meta(), emptyList())

        assertEquals(listOf(columns), dataLines(csv))
        assertTrue(csv.contains("# sample_count=0"))
    }

    @Test
    fun given_MacBertitikDua_when_FileName_then_TitikDuaDiganti() {
        val started = ZonedDateTime.of(2026, 10, 19, 14, 30, 5, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()

        val name = RssiCsv.fileName(mac, started, ZoneId.of("UTC"))

        assertEquals("dble-rssi-AA-BB-CC-DD-EE-01-20261019-143005.csv", name)
        assertFalse(name.contains(":"))
    }
}
