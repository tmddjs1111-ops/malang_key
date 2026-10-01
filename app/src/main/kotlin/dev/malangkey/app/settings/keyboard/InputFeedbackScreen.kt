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
import dev.malangkey.ime.input.HapticVibrationPrimitive
import dev.malangkey.ime.input.InputFeedbackActivationMode
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.android.systemVibratorOrNull
import org.florisboard.lib.android.vibrateClick

private val ActivationModeEntries = listOf(
    InputFeedbackActivationMode.RESPECT_SYSTEM_SETTINGS to "시스템 설정 따르기",
    InputFeedbackActivationMode.IGNORE_SYSTEM_SETTINGS to "항상 사용 (시스템 설정 무시)",
)

private val VibrationPrimitiveEntries = listOf(
    HapticVibrationPrimitive.CLICK to "클릭",
    HapticVibrationPrimitive.QUICK_CLICK to "빠른 클릭",
    HapticVibrationPrimitive.TICK to "틱",
    HapticVibrationPrimitive.LOW_TICK to "약한 틱",
    HapticVibrationPrimitive.THUD to "묵직하게",
    HapticVibrationPrimitive.SPIN to "회전",
    HapticVibrationPrimitive.QUICK_RISE to "빠르게 올라가기",
    HapticVibrationPrimitive.SLOW_RISE to "천천히 올라가기",
)

@Composable
fun InputFeedbackScreen() = MalangSettingsScreen(title = "소리·진동", subtitle = "Sound & Haptic") {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val vibrator = context.systemVibratorOrNull()
    val audioEnabled by prefs.inputFeedback.audioEnabled.collectAsState()
    val hapticEnabled by prefs.inputFeedback.hapticEnabled.collectAsState()

    MalangSettingsSection(
        title = "소리",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.inputFeedback.audioEnabled,
                    title = "키 소리",
                    summary = "키를 누를 때 소리를 냅니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.malang.malangSoundEnabled,
                    title = "소리 종류",
                    entries = listOf(false to "기본 (딸깍)", true to "말랑 (뽁)"),
                    enabled = audioEnabled,
                )
            },
            {
                MalangChoiceRow(
                    prefs.inputFeedback.audioActivationMode,
                    title = "무음 모드에서",
                    entries = ActivationModeEntries,
                    enabled = audioEnabled,
                )
            },
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
            { MalangSwitchRow(prefs.inputFeedback.audioFeatGestureSwipe, "스와이프 제스처", enabled = audioEnabled) },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.audioFeatGestureMovingSwipe,
                    title = "커서 이동 스와이프",
                    summary = "스페이스바·삭제 키를 밀어서 움직일 때",
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
            {
                MalangChoiceRow(
                    prefs.inputFeedback.hapticActivationMode,
                    title = "진동 끔 모드에서",
                    entries = ActivationModeEntries,
                    enabled = hapticEnabled,
                )
            },
            {
                MalangChoiceRow(
                    prefs.inputFeedback.hapticVibrationPrimitive,
                    title = "진동 느낌",
                    entries = VibrationPrimitiveEntries,
                    enabled = hapticEnabled,
                )
            },
            {
                MalangValueDialogRow(
                    prefs.inputFeedback.hapticVibrationIntensity,
                    title = "진동 세기",
                    min = 1,
                    max = 100,
                    step = 1,
                    unit = "%",
                    enabled = hapticEnabled,
                    onPreview = { intensity ->
                        val primitive = prefs.inputFeedback.hapticVibrationPrimitive.get()
                        vibrator?.vibrateClick(primitive.androidId, intensity / 100f)
                    },
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
                    summary = "삭제 키를 누르고 있을 때처럼 반복되는 입력",
                    enabled = hapticEnabled,
                )
            },
            { MalangSwitchRow(prefs.inputFeedback.hapticFeatGestureSwipe, "스와이프 제스처", enabled = hapticEnabled) },
            {
                MalangSwitchRow(
                    prefs.inputFeedback.hapticFeatGestureMovingSwipe,
                    title = "커서 이동 스와이프",
                    summary = "스페이스바·삭제 키를 밀어서 움직일 때",
                    enabled = hapticEnabled,
                )
            },
        ),
    )
}
