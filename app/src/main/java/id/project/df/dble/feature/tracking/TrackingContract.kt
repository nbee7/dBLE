package id.project.df.dble.feature.tracking

import android.net.Uri
import androidx.compose.runtime.Immutable
import id.project.df.dble.core.common.mvi.MviEffect
import id.project.df.dble.core.common.mvi.MviIntent
import id.project.df.dble.core.common.mvi.MviState
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.model.ProximityZone
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class RssiPoint(val tMillis: Long, val rssi: Int, val emaRssi: Double, val kalmanRssi: Double, val adKalmanRssi: Double)

@Immutable
data class State(
    val mac: String,
    val name: String? = null,
    val rawRssi: Int? = null,
    val smoothedRssi: Double? = null,
    val kalmanRssi: Double? = null,
    val adKalmanRssi: Double? = null,
    val zone: ProximityZone? = null,
    val distanceMeters: Double? = null,
    val isLost: Boolean = true,
    val points: ImmutableList<RssiPoint> = persistentListOf(),
    val banner: StopReason? = null,
    val sampleCount: Int = 0,
    val isExporting: Boolean = false,
) : MviState

sealed interface Intent : MviIntent {
    data object BackClicked : Intent
    data object PermissionsGranted : Intent
    data object ScreenResumed : Intent
    data object ExportClicked : Intent
}

sealed interface Effect : MviEffect {
    data object NavigateBack : Effect
    data class ShareFile(val uri: Uri) : Effect
    data object ExportFailed : Effect
    data class RequestPermissions(val permissions: List<String>) : Effect
}
