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
import dev.malangkey.R
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.enumDisplayEntriesOf
import dev.malangkey.ime.input.CapitalizationBehavior
import dev.malangkey.ime.keyboard.SpaceBarMode
import dev.malangkey.ime.landscapeinput.LandscapeInputUiMode
import dev.malangkey.ime.smartbar.IncognitoDisplayMode
import dev.malangkey.ime.text.key.KeyHintMode
import dev.malangkey.ime.text.key.UtilityKeyAction
import dev.malangkey.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.compose.stringRes
import dev.malangkey.app.apptheme.MalangPreferenceGroup

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun KeyboardScreen() = FlorisScreen {
    title = stringRes(R.string.settings__keyboard__title)
    previewFieldVisible = true

    val navController = LocalNavController.current

    content {
        MalangPreferenceGroup {
            Preference(
                title = "키보드 언어 및 레이아웃",
                summary = "시스템에 추가할 키보드 언어와 레이아웃을 선택합니다.",
                onClick = { navController.navigate(Routes.Settings.KeyboardSelection) },
            )
            SwitchPreference(
                prefs.keyboard.numberRow,
                title = "숫자 행 표시",
                summary = "키보드 상단에 숫자 키 행을 항상 표시합니다.",
            )
            ListPreference(
                listPref = prefs.keyboard.hintedNumberRowMode,
                switchPref = prefs.keyboard.hintedNumberRowEnabled,
                title = "힌트 숫자 행 모드",
                summarySwitchDisabled = "비활성화됨",
                entries = listPrefEntries {
                    entry(KeyHintMode.DISABLED, "사용 안 함")
                    entry(KeyHintMode.HINT_PRIORITY, "힌트 우선")
                    entry(KeyHintMode.ACCENT_PRIORITY, "악센트 우선")
                    entry(KeyHintMode.SMART_PRIORITY, "스마트 우선")
                },
                enabledIf = { prefs.keyboard.numberRow.isFalse() }
            )
            ListPreference(
                listPref = prefs.keyboard.hintedSymbolsMode,
                switchPref = prefs.keyboard.hintedSymbolsEnabled,
                title = "힌트 기호 모드",
                summarySwitchDisabled = "비활성화됨",
                entries = listPrefEntries {
                    entry(KeyHintMode.DISABLED, "사용 안 함")
                    entry(KeyHintMode.HINT_PRIORITY, "힌트 우선")
                    entry(KeyHintMode.ACCENT_PRIORITY, "악센트 우선")
                    entry(KeyHintMode.SMART_PRIORITY, "스마트 우선")
                },
            )

            ListPreference(
                prefs.keyboard.spaceBarMode,
                title = "스페이스바 표시 모드",
                entries = listPrefEntries {
                    entry(SpaceBarMode.NOTHING, "빈 공간")
                    entry(SpaceBarMode.CURRENT_LANGUAGE, "현재 언어 표시")
                    entry(SpaceBarMode.SPACE_BAR_KEY, "스페이스바 텍스트 표시")
                },
            )

        }

        MalangPreferenceGroup(title = "레이아웃 및 크기") {
            DialogSliderPreference(
                prefs.keyboard.heightFactorPortrait,
                title = "키보드 높이 (세로 화면)",
                valueLabel = { "$it%" },
                min = 50,
                max = 150,
                stepIncrement = 1,
            )
            DialogSliderPreference(
                prefs.keyboard.heightFactorLandscape,
                title = "키보드 높이 (가로 화면)",
                valueLabel = { "$it%" },
                min = 50,
                max = 150,
                stepIncrement = 1,
            )
            DialogSliderPreference(
                prefs.keyboard.fontSizeMultiplierPortrait,
                title = "키 글자 크기 (세로 화면)",
                valueLabel = { "$it%" },
                min = 50,
                max = 150,
                stepIncrement = 1,
            )
            DialogSliderPreference(
                prefs.keyboard.fontSizeMultiplierLandscape,
                title = "키 글자 크기 (가로 화면)",
                valueLabel = { "$it%" },
                min = 50,
                max = 150,
                stepIncrement = 1,
            )
            DialogSliderPreference(
                prefs.keyboard.keySpacingHorizontal,
                title = "키 가로 간격",
                valueLabel = { "$it%" },
                min = 0,
                max = 200,
                stepIncrement = 5,
            )
            DialogSliderPreference(
                prefs.keyboard.keySpacingVertical,
                title = "키 세로 간격",
                valueLabel = { "$it%" },
                min = 0,
                max = 200,
                stepIncrement = 5,
            )
        }

        MalangPreferenceGroup(title = "기타 설정") {
            SwitchPreference(
                prefs.keyboard.popupEnabled,
                title = "키 팝업 활성화",
                summary = "키를 누를 때 팝업을 표시합니다.",
            )
            SwitchPreference(
                prefs.keyboard.mergeHintPopupsEnabled,
                title = "힌트 팝업 병합",
                summary = "힌트와 키 팝업을 하나로 합칩니다.",
            )
            DialogSliderPreference(
                prefs.keyboard.longPressDelay,
                title = "길게 누르기 지연 시간",
                valueLabel = { "${it}ms" },
                min = 100,
                max = 700,
                stepIncrement = 10,
            )

        }
    }
}
