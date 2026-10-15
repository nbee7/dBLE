package id.project.df.dble.feature.history

import app.cash.turbine.test
import id.project.df.dble.core.common.mvi.MainDispatcherRule
import id.project.df.dble.core.data.history.DeviceHistoryRepository
import id.project.df.dble.core.model.DetectedDevice
import id.project.df.dble.core.model.ProximityZone
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val history = MutableStateFlow<List<DetectedDevice>>(emptyList())
    private val repository: DeviceHistoryRepository = mockk {
        every { observeHistory() } returns history
        coEvery { delete(any()) } returns Unit
        coEvery { clearAll() } returns Unit
    }

    private fun entry(mac: String, lastSeen: Long) = DetectedDevice(
        mac = mac,
        name = "Tag",
        lastRssi = -60,
        lastZone = ProximityZone.CUKUP,
        firstSeenMillis = 0,
        lastSeenMillis = lastSeen,
    )

    @Test
    fun given_DaoEmit_when_Observe_then_StateUrutLastSeenDesc() = runTest {
        val vm = HistoryViewModel(repository)

        vm.uiState.test {
            skipItems(1)
            history.value = listOf(entry("A", 100), entry("B", 300), entry("C", 200))
            assertEquals(listOf("B", "C", "A"), awaitItem().entries.map { it.mac })
        }
    }

    @Test
    fun given_EntryClicked_when_Processed_then_EffectNavigate() = runTest {
        val vm = HistoryViewModel(repository)

        vm.effect.test {
            vm.sendIntent(Intent.EntryClicked("A"))
            assertEquals(Effect.NavigateToTracking("A"), awaitItem())
        }
    }

    @Test
    fun given_DeleteRequested_when_Processed_then_RepoDelete() = runTest {
        val vm = HistoryViewModel(repository)

        vm.sendIntent(Intent.EntryDeleteRequested("A"))

        coVerify(exactly = 1) { repository.delete("A") }
    }

    @Test
    fun given_ClearAll_when_Processed_then_RepoClearDanEffect() = runTest {
        val vm = HistoryViewModel(repository)

        vm.effect.test {
            vm.sendIntent(Intent.ClearAllClicked)
            assertEquals(Effect.ShowClearedMessage, awaitItem())
        }
        coVerify(exactly = 1) { repository.clearAll() }
    }
}
