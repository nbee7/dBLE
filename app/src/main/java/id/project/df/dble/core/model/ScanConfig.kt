package id.project.df.dble.core.model

enum class ScanMode(val label: String, val description: String) {
    HEMAT("Hemat daya", "Lambat, hemat baterai"),
    SEIMBANG("Seimbang", "Kompromi kecepatan dan baterai"),
    CEPAT("Cepat", "Deteksi tercepat, paling boros"),
}

data class ScanConfig(
    val mode: ScanMode = ScanMode.CEPAT,
    val extended: Boolean = false,
)

fun scanConfigFrom(mode: String?, extended: Boolean): ScanConfig =
    ScanConfig(
        mode = ScanMode.entries.firstOrNull { it.name == mode } ?: ScanConfig().mode,
        extended = extended,
    )
