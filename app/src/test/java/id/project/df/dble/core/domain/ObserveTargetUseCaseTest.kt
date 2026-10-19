package id.project.df.dble.core.domain

import app.cash.turbine.test
import id.project.df.dble.core.common.time.TimeProvider
import id.project.df.dble.core.data.scan.DeviceScanRepository
import id.project.df.dble.core.model.ProximityZone
import id.project.df.dble.core.model.TrackedDevice
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveTargetUseCaseTest {

    private val mac = "AA:BB:CC:DD:EE:FF"

    private fun TestScope.virtualTime(): TimeProvider = object : TimeProvider {
        override fun nowMillis(): Long = testScheduler.currentTime
    }

    private fun device(rssi: Int, lastSeen: Long, mac: String = this.mac) = TrackedDevice(
        mac = mac,
        name = "Tag",
        rawRssi = rssi,
        smoothedRssi = rssi.toDouble(),
        zone = ProximityZone.KUAT,
        distanceMeters = 3.0,
        lastSeenMillis = lastSeen,
    )

    private fun useCase(scope: TestScope, devices: MutableStateFlow<List<TrackedDevice>>): ObserveTargetUseCase {
        val repo: DeviceScanRepository = mockk()
        every { repo.devices } returns devices
        return ObserveTargetUseCase(repo, scope.virtualTime())
    }

    @Test
    fun given_TargetBeriklan_when_Observe_then_NotLost() = runTest {
        val devices = MutableStateFlow(listOf(device(-45, lastSeen = 0)))

        useCase(this, devices)(mac).test {
            val target = awaitItem()
            assertFalse(target.isLost)
            assertEquals(mac, target.mac)
            assertEquals(-45, target.rawRssi)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun given_TargetTidakBeriklan5Detik_when_Tick_then_Lost() = runTest {
        val devices = MutableStateFlow(listOf(device(-45, lastSeen = 0)))

        useCase(this, devices)(mac).test {
            assertFalse(awaitItem().isLost)
            advanceTimeBy(6_001)
            val lost = expectMostRecentItem()
            assertTrue(lost.isLost)
            assertEquals(ProximityZone.KUAT, lost.zone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun given_Lost_when_IklanKembali_then_NotLost() = runTest {
        val devices = MutableStateFlow(listOf(device(-45, lastSeen = 0)))

        useCase(this, devices)(mac).test {
            awaitItem()
            advanceTimeBy(6_001)
            assertTrue(expectMostRecentItem().isLost)
            devices.value = listOf(device(-50, lastSeen = testScheduler.currentTime))
            advanceTimeBy(1)
            val back = expectMostRecentItem()
            assertFalse(back.isLost)
            assertEquals(-50, back.rawRssi)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun given_RssiMin94_when_Observe_then_Lost() = runTest {
        val devices = MutableStateFlow(listOf(device(-94, lastSeen = 0)))

        useCase(this, devices)(mac).test {
            assertTrue(awaitItem().isLost)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
