package id.project.df.dble.feature.scanner

import androidx.compose.runtime.Immutable
import id.project.df.dble.core.common.mvi.MviEffect
import id.project.df.dble.core.common.mvi.MviIntent
import id.project.df.dble.core.domain.FilterField
import id.project.df.dble.core.data.scan.StopReason
import id.project.df.dble.core.common.mvi.MviState
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.TrackedDevice
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class State(
    val isScanning: Boolean = false,
    val devices: ImmutableList<TrackedDevice> = persistentListOf(),
    val query: String = "",
    val filterField: FilterField = FilterField.NAME,
    val minRssiEnabled: Boolean = false,
    val minRssi: Int = -80,
    val banner: StopReason? = null,
    val scanConfig: ScanConfig = ScanConfig(),
    val showSettings: Boolean = false,
) : MviState

sealed interface Intent : MviIntent {
    data object StartScanClicked : Intent
    data object StopScanClicked : Intent
    data class QueryChanged(val value: String) : Intent
    data class FilterFieldChanged(val field: FilterField) : Intent
    data class MinRssiToggled(val enabled: Boolean) : Intent
    data class DeviceClicked(val mac: String) : Intent
    data object SettingsClicked : Intent
    data object SettingsDismissed : Intent
    data class ScanConfigApplied(val config: ScanConfig) : Intent
}

sealed interface Effect : MviEffect {
    data class NavigateToTracking(val mac: String) : Effect
    data class RequestPermissions(val permissions: List<String>) : Effect
    data object OpenLocationSettings : Effect
}
