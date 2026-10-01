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

package dev.malangkey.ime.input

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.KeyData
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKeyData
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemServiceOrNull
import org.florisboard.lib.android.systemVibratorOrNull
import org.florisboard.lib.android.vibrate
import org.florisboard.lib.android.vibrateClick
import dev.malangkey.lib.devtools.flogDebug
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

    private val keySoundPlayer = KeySoundPlayer(ims)

    private var systemAudioEnabled: Boolean = false
    private var systemHapticEnabled: Boolean = false

    init {
        scope.launch(Dispatchers.IO) {
            try {
                keySoundPlayer.load()
            } catch (e: Exception) {
                flogDebug { "Key sound load failed: ${e.message}" }
            }
        }
    }

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
        if (!prefs.inputFeedback.audioEnabled.get()) return
        if (prefs.inputFeedback.audioActivationMode.get() ==
            InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS && !systemAudioEnabled) return

        // 무음·진동 모드에서는 키 소리를 내지 않는다.
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        // 누르는 순간 바로 들리도록 코루틴으로 넘기지 않고 이 자리에서 재생한다.
        // '말랑'은 키 소리의 종류 중 하나다. 크기는 기본 소리와 같은 설정을 따른다.
        val isMalang = prefs.malang.malangSoundEnabled.get()
        val volume = (prefs.inputFeedback.audioVolume.get() * factor) / 100.0
        if (volume !in 0.01..1.00) return
        val sound = when {
            isMalang -> KeySoundPlayer.Sound.MALANG
            data.code == KeyCode.DELETE -> KeySoundPlayer.Sound.DELETE
            data.code == KeyCode.ENTER -> KeySoundPlayer.Sound.ENTER
            data.code == KeyCode.SPACE || data.code == KeyCode.CJK_SPACE -> KeySoundPlayer.Sound.SPACE
            else -> KeySoundPlayer.Sound.STANDARD
        }
        if (keySoundPlayer.play(sound, volume.toFloat())) return

        // 소리 파일을 올리는 중이면 시스템 효과음으로 대신한다.
        val effect = when (sound) {
            KeySoundPlayer.Sound.DELETE -> AudioManager.FX_KEYPRESS_DELETE
            KeySoundPlayer.Sound.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            KeySoundPlayer.Sound.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        flogDebug { "Perform system audio with volume=$volume and effect=$effect" }
        audioManager.playSoundEffect(effect, volume.toFloat())
    }

    private fun performHapticFeedback(data: KeyData, factor: Double) {
        if (vibrator == null) {
            flogDebug { "Haptic skipped: vibrator is null" }
            return
        }
        if (!prefs.inputFeedback.hapticEnabled.get()) {
            flogDebug { "Haptic skipped: hapticEnabled is false" }
            return
        }
        if (prefs.inputFeedback.hapticActivationMode.get() ==
            InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS && !systemHapticEnabled) {
            flogDebug { "Haptic skipped: respect system settings and system haptic is disabled" }
            return
        }

        flogDebug { "Performing haptic feedback (factor=$factor)" }
        scope.launch {
            try {
                val primitive = prefs.inputFeedback.hapticVibrationPrimitive.get()
                val intensity = (prefs.inputFeedback.hapticVibrationIntensity.get() / 100f) * factor.toFloat()
                flogDebug { "Using haptic interface: primitive=${primitive.name}, intensity=$intensity" }
                vibrator.vibrateClick(primitive.androidId, intensity)
            } catch (e: Exception) {
                flogDebug { "Haptic execution failed: ${e.message}" }
            }
        }
    }
}
