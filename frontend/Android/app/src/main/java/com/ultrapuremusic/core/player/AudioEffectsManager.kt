package com.ultrapuremusic.core.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ultrapuremusic.core.datastore.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EQ presets — band levels in millibels (mB).
 * Typical Android EQ has 5 bands: ~60 Hz, ~230 Hz, ~910 Hz, ~3.6 kHz, ~14 kHz.
 * Device level range is usually ±1500 mB.
 */
enum class EqPreset(val label: String, val bandsMb: IntArray) {
    FLAT      ("Phẳng",      intArrayOf(   0,    0,    0,    0,    0)),
    BASS_BOOST("Tăng bass",  intArrayOf( 600,  400,    0,    0,    0)),
    TREBLE    ("Treble",     intArrayOf(   0,    0,    0,  300,  500)),
    VOCAL     ("Giọng hát",  intArrayOf(-200,    0,  400,  300,  100)),
    ROCK      ("Rock",       intArrayOf( 500,  300, -200,  300,  500)),
    POP       ("Pop",        intArrayOf(-100,  200,  500,  200, -100)),
    CLASSICAL ("Cổ điển",    intArrayOf( 500,  300, -200,  300,  400)),
    HIP_HOP   ("Hip-hop",   intArrayOf( 500,  400,  100,  300,  500)),
}

@Singleton
@OptIn(UnstableApi::class)
class AudioEffectsManager @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val userPreferences: UserPreferences,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        // Re-initialize effects whenever ExoPlayer opens a new audio session.
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                reinitEffects(audioSessionId)
            }
        })
        // Attempt immediate init if session is already valid on construction.
        val sessionId = exoPlayer.audioSessionId
        if (sessionId != 0) reinitEffects(sessionId)

        // Observe settings and apply on change.
        scope.launch {
            userPreferences.eqPreset.collect { name ->
                applyPreset(EqPreset.values().find { it.name == name } ?: EqPreset.FLAT)
            }
        }
        scope.launch {
            userPreferences.bassBoostStrength.collect { pct -> applyBassBoost(pct) }
        }
        scope.launch {
            userPreferences.loudnessEnabled.collect { on -> applyLoudness(on) }
        }
    }

    private fun reinitEffects(sessionId: Int) {
        releaseEffects()
        try {
            equalizer        = Equalizer(0, sessionId).also      { it.enabled = true  }
            bassBoost        = BassBoost(0, sessionId).also      { it.enabled = false }
            loudnessEnhancer = LoudnessEnhancer(sessionId).also  { it.enabled = false }
            // Re-apply saved settings after session reset.
            scope.launch {
                applyPreset(EqPreset.values().find { it.name == userPreferences.eqPreset.first() } ?: EqPreset.FLAT)
                applyBassBoost(userPreferences.bassBoostStrength.first())
                applyLoudness(userPreferences.loudnessEnabled.first())
            }
        } catch (e: Exception) {
            Timber.e(e, "Audio effects init failed (session=%d)", sessionId)
        }
    }

    private fun applyPreset(preset: EqPreset) {
        val eq = equalizer ?: return
        val count = eq.numberOfBands.toInt()
        preset.bandsMb.take(count).forEachIndexed { i, mb ->
            runCatching { eq.setBandLevel(i.toShort(), mb.toShort()) }
        }
    }

    private fun applyBassBoost(strengthPct: Int) {
        val bb = bassBoost ?: return
        runCatching {
            bb.setStrength((strengthPct * 10).coerceIn(0, 1000).toShort())
            bb.enabled = strengthPct > 0
        }
    }

    private fun applyLoudness(enabled: Boolean) {
        val le = loudnessEnhancer ?: return
        runCatching {
            le.setTargetGain(1500)  // +15 dB target
            le.enabled = enabled
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() };        equalizer        = null
        runCatching { bassBoost?.release() };        bassBoost        = null
        runCatching { loudnessEnhancer?.release() }; loudnessEnhancer = null
    }

    fun release() {
        scope.cancel()
        releaseEffects()
    }
}
