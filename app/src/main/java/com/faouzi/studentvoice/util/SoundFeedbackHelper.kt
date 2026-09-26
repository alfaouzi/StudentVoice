package com.faouzi.studentvoice.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator

object SoundFeedbackHelper {
    /**
     * Plays one short, distinctive notification sound for the inspector
     * when a student finishes answering all questions.
     * Fails silently and gracefully if the device is muted or sound is unavailable.
     */
    fun playCompletionSound(context: Context) {
        // Attempt 1: Default Android Notification Ringtone
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (notificationUri != null) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
                if (ringtone != null) {
                    ringtone.play()
                    return
                }
            }
        } catch (_: Throwable) {
            // Fall through to ToneGenerator
        }

        // Attempt 2: Built-in ToneGenerator (standard alert beep)
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            Thread {
                try {
                    Thread.sleep(300)
                    toneGenerator.release()
                } catch (_: Throwable) {
                }
            }.start()
        } catch (_: Throwable) {
            // Silently ignore if audio subsystem is unavailable or muted
        }
    }
}
