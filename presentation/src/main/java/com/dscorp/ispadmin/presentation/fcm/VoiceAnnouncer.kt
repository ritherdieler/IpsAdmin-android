package com.dscorp.ispadmin.presentation.fcm

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.UUID

private const val TAG = "VoiceAnnouncer"

/** Phrase for tickets from a Core that does not send `speech` yet. */
const val DEFAULT_TICKET_SPEECH = "Se ha registrado un nuevo ticket de asistencia"

/** What the staff app says aloud for a new ticket: the Core's summary, or the fixed phrase. */
fun ticketSpeech(message: FcmMessage): String =
    message.speech?.takeIf { it.isNotBlank() } ?: DEFAULT_TICKET_SPEECH

/** A text-to-speech engine that reports through `onReady` once it can speak. */
interface SpeechEngine {
    /** Queues [text] after whatever is being spoken; false when the engine no longer works. */
    fun speak(text: String): Boolean
    fun shutdown()
}

/**
 * One engine for the whole process. FCM recreates its service for almost every push, and an engine
 * built per push was asked to speak before it had started, so the phrase was lost. Texts that
 * arrive while the engine starts wait in order; an engine that fails to start is rebuilt next time.
 */
class VoiceAnnouncer(
    private val log: (String) -> Unit = { Log.e(TAG, it) },
    private val createEngine: (onReady: (Boolean) -> Unit) -> SpeechEngine,
) {
    private val lock = Any()
    private val pending = ArrayDeque<String>()
    private var engine: SpeechEngine? = null
    private var ready = false
    /** Tells a stale engine's late callback apart from the current engine's. */
    private var generation = 0

    fun announce(text: String) {
        synchronized(lock) {
            if (ready) {
                if (engine?.speak(text) == true) return
                // The TTS service went away (killed or updated): start a new engine for this text.
                log("Text-to-speech stopped working; starting it again")
                dropEngine()
            }
            pending.addLast(text)
            if (engine == null) {
                val current = ++generation
                // TextToSpeech always reports its start later, never from inside its constructor.
                engine = createEngine { success -> onEngineReady(current, success) }
            }
        }
    }

    private fun onEngineReady(engineGeneration: Int, success: Boolean) {
        synchronized(lock) {
            if (engineGeneration != generation) return
            if (success) {
                ready = true
                while (pending.isNotEmpty()) engine?.speak(pending.removeFirst())
            } else {
                log("Text-to-speech did not start; dropping ${pending.size} text(s)")
                pending.clear()
                dropEngine()
            }
        }
    }

    private fun dropEngine() {
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
    }

    companion object {
        @Volatile
        private var instance: VoiceAnnouncer? = null

        fun get(context: Context): VoiceAnnouncer = instance ?: synchronized(this) {
            instance ?: VoiceAnnouncer { onReady -> AndroidSpeechEngine(context.applicationContext, onReady) }
                .also { instance = it }
        }
    }
}

/** Android [TextToSpeech] in Spanish (Peru, then Spain, then any Spanish voice). */
private class AndroidSpeechEngine(context: Context, onReady: (Boolean) -> Unit) : SpeechEngine {
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        onReady(status == TextToSpeech.SUCCESS && useSpanish())
    }

    private fun useSpanish(): Boolean {
        val voice = listOf(Locale("es", "PE"), Locale("es", "ES"), Locale("es"))
            .firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        if (voice == null) {
            Log.e(TAG, "No Spanish text-to-speech voice is installed")
            return false
        }
        return tts.setLanguage(voice) >= TextToSpeech.LANG_AVAILABLE
    }

    override fun speak(text: String): Boolean =
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString()) == TextToSpeech.SUCCESS

    override fun shutdown() = tts.shutdown()
}
