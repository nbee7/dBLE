package id.project.df.dble.core.common.mvi

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private data class TState(val count: Int = 0) : MviState
    private sealed interface TIntent : MviIntent {
        data object Inc : TIntent
    }

    private sealed interface TEffect : MviEffect {
        data object Ping : TEffect
    }

    private class TVm : BaseViewModel<TState, TIntent, TEffect>(TState()) {
        override suspend fun handleIntent(intent: TIntent) {
            updateState { copy(count = count + 1) }
            sendEffect(TEffect.Ping)
        }
    }

    @Test
    fun given_Inc_when_Processed_then_StateDanEffectTerima() = runTest {
        val vm = TVm()
        vm.uiState.test {
            skipItems(1)
            vm.sendIntent(TIntent.Inc)
            assertEquals(1, awaitItem().count)
        }
        vm.effect.test {
            assertEquals(TEffect.Ping, awaitItem())
        }
    }
}
