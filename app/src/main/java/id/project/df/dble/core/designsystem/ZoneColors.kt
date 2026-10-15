package id.project.df.dble.core.designsystem

import androidx.compose.ui.graphics.Color
import id.project.df.dble.core.model.ProximityZone

fun ProximityZone.color(): Color = when (this) {
    ProximityZone.SANGAT_KUAT -> Color(0xFF16A34A)
    ProximityZone.KUAT -> Color(0xFF84CC16)
    ProximityZone.CUKUP -> Color(0xFFD97706)
    ProximityZone.LEMAH -> Color(0xFFEA580C)
    ProximityZone.SANGAT_LEMAH -> Color(0xFFDC2626)
    ProximityZone.LOST -> Color(0xFF525252)
}
