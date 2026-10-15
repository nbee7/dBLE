package id.project.df.dble.core.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS

fun formatLastSeen(nowMillis: Long, seenMillis: Long): String {
    val elapsed = nowMillis - seenMillis
    if (elapsed < MINUTE_MS) return "baru saja"
    if (elapsed < HOUR_MS) return "${elapsed / MINUTE_MS} mnt lalu"

    val locale = Locale.forLanguageTag("id-ID")
    val seenDay = dayOf(seenMillis)
    val today = dayOf(nowMillis)
    val time = SimpleDateFormat("HH:mm", locale).format(Date(seenMillis))
    return when (seenDay) {
        today -> "Hari ini, $time"
        today - 1 -> "Kemarin, $time"
        else -> SimpleDateFormat("dd MMM, HH:mm", locale).format(Date(seenMillis))
    }
}

private fun dayOf(millis: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)
    return Math.floorDiv(millis + offset, 24 * HOUR_MS)
}

private const val STALE_AFTER_MS = 10_000L

fun isStale(nowMillis: Long, lastSeenMillis: Long): Boolean = nowMillis - lastSeenMillis > STALE_AFTER_MS
