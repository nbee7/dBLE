package id.project.df.dble.feature.history

import androidx.compose.runtime.Immutable
import id.project.df.dble.core.common.mvi.MviEffect
import id.project.df.dble.core.common.mvi.MviIntent
import id.project.df.dble.core.common.mvi.MviState
import id.project.df.dble.core.model.DetectedDevice
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class State(
    val entries: ImmutableList<DetectedDevice> = persistentListOf(),
) : MviState

sealed interface Intent : MviIntent {
    data class EntryClicked(val mac: String) : Intent
    data class EntryDeleteRequested(val mac: String) : Intent
    data object ClearAllClicked : Intent
}

sealed interface Effect : MviEffect {
    data class NavigateToTracking(val mac: String) : Effect
    data object ShowClearedMessage : Effect
}
