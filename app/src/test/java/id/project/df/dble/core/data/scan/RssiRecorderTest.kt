package id.project.df.dble.core.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RssiRecorderTest {

    private val mac = "AA:BB:CC:DD:EE:01"
    private val other = "AA:BB:CC:DD:EE:02"

    private fun sample(t: Long, mac: String = this.mac) =
        RssiSample(tMillis = t, mac = mac, rawRssi = -60, emaRssi = -60.0, kalmanRssi = -60.0)

    @Test
    fun given_TidakAdaTrack_when_OnSample_then_Diabaikan() {
        val recorder = RssiRecorder()

        recorder.onSample(sample(0))

        assertTrue(recorder.snapshot().isEmpty())
        assertEquals(0, recorder.sampleCount.value)
    }

    @Test
    fun given_TrackMac_when_OnSampleMacLain_then_Diabaikan() {
        val recorder = RssiRecorder().apply { track(mac) }

        recorder.onSample(sample(0, mac = other))

        assertTrue(recorder.snapshot().isEmpty())
    }

    @Test
    fun given_TrackMac_when_OnSample_then_TersimpanDanCountNaik() {
        val recorder = RssiRecorder().apply { track(mac) }

        recorder.onSample(sample(0))
        recorder.onSample(sample(500))

        assertEquals(listOf(0L, 500L), recorder.snapshot().map { it.tMillis })
        assertEquals(2, recorder.sampleCount.value)
    }

    @Test
    fun given_SampelLebihTuaDariWindow_when_OnSample_then_Dibuang() {
        val recorder = RssiRecorder().apply { track(mac) }

        recorder.onSample(sample(0))
        recorder.onSample(sample(RssiRecorder.WINDOW_MS + 1))

        assertEquals(listOf(RssiRecorder.WINDOW_MS + 1), recorder.snapshot().map { it.tMillis })
        assertEquals(1, recorder.sampleCount.value)
    }

    @Test
    fun given_SampelTepatDiBatasWindow_when_OnSample_then_Tetap() {
        val recorder = RssiRecorder().apply { track(mac) }

        recorder.onSample(sample(0))
        recorder.onSample(sample(RssiRecorder.WINDOW_MS))

        assertEquals(2, recorder.snapshot().size)
    }

    @Test
    fun given_TrackMacBerbeda_when_Track_then_BufferDikosongkan() {
        val recorder = RssiRecorder().apply { track(mac) }
        recorder.onSample(sample(0))

        recorder.track(other)

        assertTrue(recorder.snapshot().isEmpty())
        assertEquals(0, recorder.sampleCount.value)
    }

    @Test
    fun given_TrackMacSama_when_TrackUlang_then_BufferTetap() {
        val recorder = RssiRecorder().apply { track(mac) }
        recorder.onSample(sample(0))

        recorder.track(mac)

        assertEquals(1, recorder.snapshot().size)
    }

    @Test
    fun given_Release_when_OnSample_then_DiabaikanDanBufferKosong() {
        val recorder = RssiRecorder().apply { track(mac) }
        recorder.onSample(sample(0))

        recorder.release()
        recorder.onSample(sample(500))

        assertTrue(recorder.snapshot().isEmpty())
        assertEquals(0, recorder.sampleCount.value)
    }

    @Test
    fun given_Snapshot_when_OnSampleBaru_then_SnapshotLamaTidakBerubah() {
        val recorder = RssiRecorder().apply { track(mac) }
        recorder.onSample(sample(0))
        val snapshot = recorder.snapshot()

        recorder.onSample(sample(500))

        assertEquals(1, snapshot.size)
    }
}
