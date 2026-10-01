package com.faouzi.studentvoice.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundFeedbackHelper {
    /**
     * Default volume level (near maximum: 90 out of 100) for the completion notification tone,
     * ensuring it is clearly audible at a normal phone listening level while remaining pleasant.
     */
    const val DEFAULT_VOLUME = 90

    /**
     * Plays one short, gentle beep (approx 120ms) at an audible volume level (90/100)
     * when a student finishes answering the final question.
     * Uses Android's built-in ToneGenerator.
     * Fails silently and gracefully if the device is muted or sound is unavailable.
     */
    fun playCompletionSound(context: Context? = null, volume: Int = DEFAULT_VOLUME) {
        try {
            // Increased from 40 to 90 (out of 100) so it is clearly audible at normal phone listening levels
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, volume.coerceIn(0, 100))
            // TONE_PROP_BEEP: standard gentle beep, 120ms duration (approx 100–150ms)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            val releaseThread = Thread {
                try {
                    Thread.sleep(250)
                    toneGenerator.release()
                } catch (_: Throwable) {
                }
            }
            releaseThread.isDaemon = true
            releaseThread.start()
        } catch (_: Throwable) {
            // Silently ignore if audio subsystem is unavailable, muted, or in headless test environment
        }
    }
}

