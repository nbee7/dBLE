package id.project.df.dble.core.model

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis

    private val now = at(2026, Calendar.OCTOBER, 15, 15, 0)

    @Test
    fun given_30Detik_when_Format_then_Barusa() {
        assertEquals("baru saja", formatLastSeen(now, now - 30_000))
    }

    @Test
    fun given_5Menit_when_Format_then_5MntLalu() {
        assertEquals("5 mnt lalu", formatLastSeen(now, now - 5 * 60_000))
    }

    @Test
    fun given_HariSamaJamLalu_when_Format_then_HariIni() {
        assertEquals("Hari ini, 13:30", formatLastSeen(now, at(2026, Calendar.OCTOBER, 15, 13, 30)))
    }

    @Test
    fun given_Kemarin_when_Format_then_Kemarin() {
        assertEquals("Kemarin, 09:05", formatLastSeen(now, at(2026, Calendar.OCTOBER, 14, 9, 5)))
    }

    @Test
    fun given_MingguLalu_when_Format_then_Tanggal() {
        assertEquals("08 Okt, 10:00", formatLastSeen(now, at(2026, Calendar.OCTOBER, 8, 10, 0)))
    }
}
