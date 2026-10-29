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
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.staticCompositionLocalOf
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.KeyData
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKeyData
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.android.systemServiceOrNull
import org.florisboard.lib.android.systemVibratorOrNull
import org.florisboard.lib.android.vibrateWaveform
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
        private const val MIN_TICK_INTERVAL_MS = 30L
        /** 큰 키: 스페이스·엔터·지우기. */
        private val BigKeyCodes = setOf(
            KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.JAPANESE_SPACE,
            KeyCode.ENTER, KeyCode.JAPANESE_ENTER, KeyCode.DELETE,
        )

        fun new(ims: InputMethodService) = InputFeedbackController(ims)
    }

    private val prefs by FlorisPreferenceStore

    private val audioManager = ims.systemServiceOrNull(AudioManager::class)
    private val vibrator = ims.systemVibratorOrNull()
    private val contentResolver = ims.contentResolver
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val keySoundPlayer = KeySoundPlayer(ims)

    private var systemHapticEnabled: Boolean = false

    init {
        scope.launch(Dispatchers.IO) {
            // 예전 '말랑 효과음' 스위치는 소리 종류 중 하나(말랑 뽁)가 됐다.
            if (prefs.malang.malangSoundEnabled.get()) {
                prefs.inputFeedback.keySoundStyle.set(KeySoundStyle.MALANG)
                prefs.malang.malangSoundEnabled.set(false)
            }
            loadSoundStyle(prefs.inputFeedback.keySoundStyle.get())
        }
    }

    private fun loadSoundStyle(style: KeySoundStyle) {
        scope.launch(Dispatchers.IO) {
            try {
                keySoundPlayer.load(style)
            } catch (e: Exception) {
                flogDebug { "Key sound load failed: ${e.message}" }
            }
        }
    }

    fun updateSystemPrefsState() {
        systemHapticEnabled = systemPref(Settings.System.HAPTIC_FEEDBACK_ENABLED)
    }

    // 진동은 손끝에 바로 와야 해서 소리보다 먼저, 다른 스레드로 넘기지 않고 이 자리에서 낸다.
    fun keyPress(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.hapticFeatKeyPress.get()) performHapticFeedback(HapticEvent.KEY_PRESS, data)
        if (prefs.inputFeedback.audioFeatKeyPress.get()) performAudioFeedback(data, 1.0)
    }

    fun keyLongPress(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.hapticFeatKeyLongPress.get()) performHapticFeedback(HapticEvent.LONG_PRESS)
        if (prefs.inputFeedback.audioFeatKeyLongPress.get()) performAudioFeedback(data, 0.7)
    }

    fun keyRepeatedAction(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.hapticFeatKeyRepeatedAction.get()) performHapticFeedback(HapticEvent.TICK)
        if (prefs.inputFeedback.audioFeatKeyRepeatedAction.get()) performAudioFeedback(data, 0.4)
    }

    fun gestureSwipe(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.hapticFeatGestureSwipe.get()) performHapticFeedback(HapticEvent.LONG_PRESS)
        if (prefs.inputFeedback.audioFeatGestureSwipe.get()) performAudioFeedback(data, 0.7)
    }

    fun gestureMovingSwipe(data: KeyData = TextKeyData.UNSPECIFIED) {
        if (prefs.inputFeedback.hapticFeatGestureMovingSwipe.get()) performHapticFeedback(HapticEvent.TICK)
        if (prefs.inputFeedback.audioFeatGestureMovingSwipe.get()) performAudioFeedback(data, 0.4)
    }

    private fun systemPref(id: String): Boolean {
        if (contentResolver == null) return false
        return Settings.System.getInt(contentResolver, id, 0) != 0
    }

    private fun performAudioFeedback(data: KeyData, factor: Double) {
        if (audioManager == null) return
        if (!prefs.inputFeedback.audioEnabled.get()) return
        // 켬·끔은 키 소리 스위치 하나로 정한다. 시스템 '터치음' 설정은 따르지 않고,
        // 휴대폰이 무음·진동 모드일 때만 소리를 내지 않는다.
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        // 누르는 순간 바로 들리도록 코루틴으로 넘기지 않고 이 자리에서 재생한다.
        val style = prefs.inputFeedback.keySoundStyle.get()
        val volume = (prefs.inputFeedback.audioVolume.get() * factor) / 100.0
        if (volume !in 0.01..1.00) return
        val kind = when (data.code) {
            KeyCode.DELETE -> KeySoundPlayer.Kind.DELETE
            KeyCode.ENTER, KeyCode.JAPANESE_ENTER -> KeySoundPlayer.Kind.ENTER
            KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.JAPANESE_SPACE -> KeySoundPlayer.Kind.SPACE
            else -> KeySoundPlayer.Kind.STANDARD
        }
        if (keySoundPlayer.play(style, kind, volume.toFloat())) return

        // 고른 소리를 아직 올리는 중이면(방금 종류를 바꾼 경우 등) 그동안만 시스템 효과음으로 대신한다.
        if (!keySoundPlayer.isLoaded(style)) loadSoundStyle(style)
        val effect = when (kind) {
            KeySoundPlayer.Kind.DELETE -> AudioManager.FX_KEYPRESS_DELETE
            KeySoundPlayer.Kind.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            KeySoundPlayer.Kind.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        flogDebug { "Perform system audio with volume=$volume and effect=$effect" }
        audioManager.playSoundEffect(effect, volume.toFloat())
    }

    private enum class HapticEvent {
        KEY_PRESS,
        LONG_PRESS,
        /** 삭제 반복·커서 이동처럼 잇달아 나는 가벼운 진동. */
        TICK,
    }

    private var lastTickUptime = 0L

    private fun performHapticFeedback(event: HapticEvent, data: KeyData = TextKeyData.UNSPECIFIED) {
        if (vibrator == null) return
        if (!prefs.inputFeedback.hapticEnabled.get()) return
        if (prefs.inputFeedback.hapticActivationMode.get() ==
            InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS && !systemHapticEnabled) {
            return
        }
        if (event == HapticEvent.TICK) {
            // 잇단 틱이 한데 뭉쳐 웅 하고 울리지 않도록 간격을 둔다.
            val now = SystemClock.uptimeMillis()
            if (now - lastTickUptime < MIN_TICK_INTERVAL_MS) return
            lastTickUptime = now
        }

        if (prefs.inputFeedback.hapticMode.get() == HapticMode.SYSTEM && performSystemHaptic(event)) return
        performCustomHaptic(event, data)
    }

    /**
     * 휴대폰의 키보드 진동을 쓴다. 시스템 '터치 시 진동'이 꺼져 있으면 시스템이 울려 주지 않으므로
     * false를 돌려 직접 울리게 한다.
     */
    private fun performSystemHaptic(event: HapticEvent): Boolean {
        if (!systemHapticEnabled) return false
        val view = ims.window?.window?.decorView ?: return false
        val constant = when (event) {
            HapticEvent.KEY_PRESS -> HapticFeedbackConstants.KEYBOARD_TAP
            HapticEvent.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
            HapticEvent.TICK -> if (AndroidVersion.ATLEAST_API27_O_MR1) {
                HapticFeedbackConstants.TEXT_HANDLE_MOVE
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
        }
        return view.performHapticFeedback(constant)
    }

    private fun performCustomHaptic(event: HapticEvent, data: KeyData) {
        val vibrator = vibrator ?: return
        val kind = when (event) {
            HapticEvent.KEY_PRESS -> if (prefs.inputFeedback.hapticBigKeyAccent.get() && data.code in BigKeyCodes) {
                HapticKind.BIG_KEY
            } else {
                HapticKind.KEY
            }
            HapticEvent.LONG_PRESS -> HapticKind.LONG_PRESS
            HapticEvent.TICK -> HapticKind.TICK
        }
        val pattern = KeyHaptics.pattern(
            feel = prefs.inputFeedback.hapticFeel.get(),
            strength = prefs.inputFeedback.hapticVibrationIntensity.get() / 100f,
            kind = kind,
        )
        vibrator.vibrateWaveform(pattern.timings, pattern.amplitudes)
    }
}
