package id.project.df.dble.core.model

import java.util.Locale

fun formatDistance(meters: Double?, zone: ProximityZone): String {
    if (meters == null || zone == ProximityZone.LOST) return "—"
    if (zone == ProximityZone.SANGAT_KUAT || meters < 1.0) return "<1 m"
    if (meters > 20.0) return ">20 m"
    return "~" + String.format(Locale.forLanguageTag("id-ID"), "%.1f", meters) + " m"
}
