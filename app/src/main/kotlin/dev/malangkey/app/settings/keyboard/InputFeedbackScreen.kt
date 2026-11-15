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

package dev.malangkey.app.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.input.HapticKind
import dev.malangkey.ime.input.InputFeedbackActivationMode
import dev.malangkey.ime.input.playKeyHaptic
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.android.systemVibratorOrNull

private val ActivationModeEntries = listOf(
    InputFeedbackActivationMode.IGNORE_SYSTEM_SETTINGS to "그래도 진동",
    InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS to "진동 안 함",
)


@Composable
fun InputFeedbackScreen() = MalangSettingsScreen(title = "소리·진동", subtitle = "Sound & Haptic") {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val vibrator = context.systemVibratorOrNull()
    val audioEnabled by prefs.inputFeedback.audioEnabled.collectAsState()
    val hapticEnabled by prefs.inputFeedback.hapticEnabled.collectAsState()
    val isCustomHaptic = hapticEnabled

    MalangSettingsSection(
        title = "소리",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.inputFeedback.audioEnabled,
                    title = "키 소리",
                    summary = "키를 누를 때 소리를 냅니다. 휴대폰이 무음·진동 모드면 소리가 나지 않아요.",
                )
            },
            { KeySoundPicker(enabled = audioEnabled) },
            {
                MalangValueDialogRow(
                    prefs.inputFeedback.audioVolume,
                    title = "소리 크기",
                    min = 1,
                    max = 100,
                    step = 1,
                    unit = "%",
                    enabled = audioEnabled,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "소리가 나는 동작",
        items = listOf(
            { MalangSwitchRow(prefs.inputFeedback.audioFeatKeyPress, "키 누르기", enabled = audioEnabled) },
            { MalangSwitchRow(prefs.inputFeedback.audioFeatKeyLongPress, "키 길게 누르기", enabled = audioEnabled) },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.audioFeatKeyRepeatedAction,
                    title = "반복 입력",
                    summary = "삭제 키를 누르고 있을 때처럼 반복되는 입력",
                    enabled = audioEnabled,
                )
            },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.audioFeatGestureMovingSwipe,
                    title = "삭제 키 밀기",
                    summary = "삭제 키를 밀어서 글자를 고를 때",
                    enabled = audioEnabled,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "진동",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.inputFeedback.hapticEnabled,
                    title = "키 진동",
                    summary = "키를 누를 때 진동을 줍니다.",
                )
            },
            { HapticFeelPicker(enabled = isCustomHaptic, vibrator = vibrator) },
            {
                MalangValueDialogRow(
                    prefs.inputFeedback.hapticVibrationIntensity,
                    title = "진동 세기",
                    min = 1,
                    max = 100,
                    step = 1,
                    unit = "%",
                    enabled = isCustomHaptic,
                    onPreview = { intensity ->
                        vibrator?.playKeyHaptic(prefs.inputFeedback.hapticFeel.get(), intensity / 100f, HapticKind.KEY)
                    },
                )
            },
            {
                MalangChoiceRow(
                    prefs.inputFeedback.hapticActivationMode,
                    title = "휴대폰 '터치 시 진동'이 꺼져 있으면",
                    entries = ActivationModeEntries,
                    enabled = hapticEnabled,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "진동이 울리는 동작",
        items = listOf(
            { MalangSwitchRow(prefs.inputFeedback.hapticFeatKeyPress, "키 누르기", enabled = hapticEnabled) },
            { MalangSwitchRow(prefs.inputFeedback.hapticFeatKeyLongPress, "키 길게 누르기", enabled = hapticEnabled) },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.hapticFeatKeyRepeatedAction,
                    title = "반복 입력",
                    summary = "삭제 키를 누르고 있을 때 지울 때마다 톡",
                    enabled = hapticEnabled,
                )
            },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.hapticFeatGestureMovingSwipe,
                    title = "삭제 키 밀기",
                    summary = "삭제 키를 밀어서 글자를 고를 때 한 칸마다 톡",
                    enabled = hapticEnabled,
                )
            },
        ),
    )
}
