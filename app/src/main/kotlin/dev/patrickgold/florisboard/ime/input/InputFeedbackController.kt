/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.input

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.staticCompositionLocalOf
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemServiceOrNull
import org.florisboard.lib.android.systemVibratorOrNull
import org.florisboard.lib.android.vibrate
import org.florisboard.lib.android.vibrateClick
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val LocalInputFeedbackController = staticCompositionLocalOf<InputFeedbackController> { error("not init") }

/**
 * Input feedback controller is responsible to process and perform audio and haptic
 * feedback for user interactions based on the system and floris preferences.
 */
class InputFeedbackController private constructor(private val ims: InputMethodService) {
    companion object {
        fun new(ims: InputMethodService) = InputFeedbackController(ims)
    }

    private val prefs by FlorisPreferenceStore

    private val audioManager = ims.systemServiceOrNull(AudioManager::class)
    private val vibrator = ims.systemVibratorOrNull()
    private val contentResolver = ims.contentResolver
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var systemAudioEnabled: Boolean = false
    private var systemHapticEnabled: Boolean = false

    fun updateSystemPrefsState() {
        systemAudioEnabled = systemPref(Settings.System.SOUND_EFFECTS_ENABLED)
        systemHapticEnabled = systemPref(Settings.System.HAPTIC_FEEDBACK_ENABLED)
    }

    fun keyPress(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.audioFeatKeyPress.get()) performAudioFeedback(data, 1.0)
        if (prefs.inputFeedback.hapticFeatKeyPress.get()) performHapticFeedback(data, 1.0)
    }

    fun keyLongPress(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.audioFeatKeyLongPress.get()) performAudioFeedback(data, 0.7)
        if (prefs.inputFeedback.hapticFeatKeyLongPress.get()) performHapticFeedback(data, 0.4)
    }

    fun keyRepeatedAction(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.audioFeatKeyRepeatedAction.get()) performAudioFeedback(data, 0.4)
        if (prefs.inputFeedback.hapticFeatKeyRepeatedAction.get()) performHapticFeedback(data, 0.05)
    }

    fun gestureSwipe(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.audioFeatGestureSwipe.get()) performAudioFeedback(data, 0.7)
        if (prefs.inputFeedback.hapticFeatGestureSwipe.get()) performHapticFeedback(data, 0.4)
    }

    fun gestureMovingSwipe(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.audioFeatGestureMovingSwipe.get()) performAudioFeedback(data, 0.4)
        if (prefs.inputFeedback.hapticFeatGestureMovingSwipe.get()) performHapticFeedback(data, 0.05)
    }

    private fun systemPref(id: String): Boolean {
        if (contentResolver == null) return false
        return Settings.System.getInt(contentResolver, id, 0) != 0
    }

    private fun performAudioFeedback(data: KeyData, factor: Double) {
        if (audioManager == null) return
        if (!prefs.inputFeedback.audioEnabled.get() && !prefs.malang.malangSoundEnabled.get()) return
        if (!prefs.malang.malangSoundEnabled.get() && prefs.inputFeedback.audioActivationMode.get() ==
            InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS && !systemAudioEnabled) return

        scope.launch {
            val isMalang = prefs.malang.malangSoundEnabled.get()
            val volume = if (isMalang) 0.5 else (prefs.inputFeedback.audioVolume.get() * factor) / 100.0
            val effect = when {
                isMalang -> AudioManager.FX_FOCUS_NAVIGATION_UP // A softer, "pop" like system sound
                data.code == KeyCode.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                data.code == KeyCode.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                data.code == KeyCode.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                else -> AudioManager.FX_KEYPRESS_STANDARD
            }
            if (volume in 0.01..1.00) {
                flogDebug { "Perform audio with volume=$volume and effect=$effect" }
                audioManager.playSoundEffect(effect, volume.toFloat())
            }
        }
    }

    private fun performHapticFeedback(data: KeyData, factor: Double) {
        if (vibrator == null) {
            flogDebug { "Haptic skipped: vibrator is null" }
            return
        }
        val isMalang = prefs.malang.malangSoundEnabled.get()
        if (!prefs.inputFeedback.hapticEnabled.get() && !isMalang) {
            flogDebug { "Haptic skipped: hapticEnabled is false" }
            return
        }
        if (!isMalang && prefs.inputFeedback.hapticActivationMode.get() ==
            InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS && !systemHapticEnabled) {
            flogDebug { "Haptic skipped: respect system settings and system haptic is disabled" }
            return
        }

        flogDebug { "Performing haptic feedback (factor=$factor)" }
        scope.launch {
            try {
                if (isMalang) {
                    vibrator.vibrateClick(HapticFeedbackConstants.CLOCK_TICK, 0.3f * factor.toFloat())
                } else {
                    val primitive = prefs.inputFeedback.hapticVibrationPrimitive.get()
                    val intensity = (prefs.inputFeedback.hapticVibrationIntensity.get() / 100f) * factor.toFloat()
                    flogDebug { "Using haptic interface: primitive=${primitive.name}, intensity=$intensity" }
                    vibrator.vibrateClick(primitive.androidId, intensity)
                }
            } catch (e: Exception) {
                flogDebug { "Haptic execution failed: ${e.message}" }
            }
        }
    }
}
