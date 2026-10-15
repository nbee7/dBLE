package id.project.df.dble.core.data.audio

import android.media.AudioManager
import android.media.ToneGenerator
import timber.log.Timber

interface ProximityBeeper {
    fun beep()
    fun release()
}

class AndroidProximityBeeper : ProximityBeeper {
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    } catch (e: RuntimeException) {
        Timber.w(e, "[TRACK][BEEP] init failed")
        null
    }

    override fun beep() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
    }

    override fun release() {
        tone?.release()
    }

    private companion object {
        const val VOLUME = 80
        const val BEEP_MS = 100
    }
}
