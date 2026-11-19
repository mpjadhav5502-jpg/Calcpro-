package com.example.util

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

object FeedbackHelper {
    fun performKeyFeedback(
        view: View,
        context: Context,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean
    ) {
        if (vibrationEnabled) {
            view.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        }
        if (soundEnabled) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.playSoundEffect(SoundEffectConstants.CLICK, 0.5f)
        }
    }
}
