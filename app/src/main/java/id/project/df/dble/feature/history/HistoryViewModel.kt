package id.project.df.dble.feature.history

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.project.df.dble.core.common.mvi.BaseViewModel
import id.project.df.dble.core.data.history.DeviceHistoryRepository
import javax.inject.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: DeviceHistoryRepository,
) : BaseViewModel<State, Intent, Effect>(State()) {

    init {
        viewModelScope.launch {
            history.observeHistory().collect { entries ->
                updateState {
                    copy(entries = entries.sortedByDescending { it.lastSeenMillis }.toPersistentList())
                }
            }
        }
    }

    override suspend fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.EntryClicked -> {
                Timber.d("[NAV][TO] mac=%s", intent.mac)
                sendEffect(Effect.NavigateToTracking(intent.mac))
            }
            is Intent.EntryDeleteRequested -> history.delete(intent.mac)
            Intent.ClearAllClicked -> {
                history.clearAll()
                sendEffect(Effect.ShowClearedMessage)
            }
        }
    }
}
