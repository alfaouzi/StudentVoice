package com.faouzi.studentvoice.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

object SoundFeedbackHelper {
    /**
     * Plays one short, gentle beep (approx 120ms) at a comfortable low volume
     * when a student finishes answering the final question.
     * Uses Android's built-in ToneGenerator.
     * Fails silently and gracefully if the device is muted or sound is unavailable.
     */
    fun playCompletionSound(context: Context? = null) {
        try {
            // Low, comfortable volume (40 out of 100)
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
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

