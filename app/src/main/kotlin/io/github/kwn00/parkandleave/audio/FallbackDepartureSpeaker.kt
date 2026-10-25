package io.github.kwn00.parkandleave.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import io.github.kwn00.parkandleave.future.OnDeviceSpeech

/**
 * Visual copy is the primary summary. Audio is a short beep because the
 * on-device speech SDK is not linked.
 */
class FallbackDepartureSpeaker(
    context: Context,
    private val onDeviceSpeech: OnDeviceSpeech,
) {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private var tone: ToneGenerator? = null
    private var focusRequest: AudioFocusRequest? = null

    val modeLabel: String = if (onDeviceSpeech.available) "온디바이스 음성" else "알림음"

    fun speak(line: String) {
        if (onDeviceSpeech.speak(line)) return
        stop()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .build()
        focusRequest = request
        audioManager?.requestAudioFocus(request)
        val generator = ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
        tone = generator
        generator.startTone(ToneGenerator.TONE_PROP_ACK, TONE_DURATION_MS)
    }

    fun stop() {
        onDeviceSpeech.stop()
        tone?.stopTone()
        tone?.release()
        tone = null
        focusRequest?.let { request -> audioManager?.abandonAudioFocusRequest(request) }
        focusRequest = null
    }

    private companion object {
        const val TONE_VOLUME = 70
        const val TONE_DURATION_MS = 180
    }
}
