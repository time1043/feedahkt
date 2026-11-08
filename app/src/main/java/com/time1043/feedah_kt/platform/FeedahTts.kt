package com.time1043.feedah_kt.platform

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * expo-speech stand-in: stop-before-speak, en-US, rate 1.15 — the source's
 * speech.ts constants. Init is callback-based; requests arriving before the
 * engine reports ready are dropped (documented difference: expo-speech
 * queues them internally).
 */
class FeedahTts(context: Context) {
    private var engine: TextToSpeech? = null

    @Volatile
    private var ready = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.language = Locale.US
            }
        }
    }

    val isAvailable: Boolean get() = ready

    /** Stops any queued utterance, then speaks [text] at the fixed rate. */
    fun speakWord(text: String) {
        val tts = engine ?: return
        if (!ready) return
        tts.stop()
        tts.setSpeechRate(1.15f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "feedah-word")
    }

    fun stop() {
        engine?.stop()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }
}

/** The single speak entry point used by screens (source lib/speech.ts). */
fun speakWord(tts: FeedahTts, text: String) {
    tts.speakWord(text)
}
