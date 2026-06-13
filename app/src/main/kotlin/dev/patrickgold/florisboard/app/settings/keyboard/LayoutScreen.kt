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

package dev.patrickgold.florisboard.app.settings.keyboard

import androidx.compose.runtime.Composable
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.landscapeinput.LandscapeInputUiMode
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.compose.stringRes
import dev.patrickgold.florisboard.app.apptheme.MalangPreferenceGroup
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun LayoutScreen() = FlorisScreen {
    title = "레이아웃"
    previewFieldVisible = true

    content {
        MalangPreferenceGroup(title = "키보드 높이") {
            DialogSliderPreference(
                primaryPref = prefs.keyboard.heightFactorPortrait,
                secondaryPref = prefs.keyboard.heightFactorLandscape,
                title = "키보드 높이 조절",
                primaryLabel = "세로 모드",
                secondaryLabel = "가로 모드",
                valueLabel = { "${it}%" },
                min = 50,
                max = 150,
                stepIncrement = 5,
            )
        }

        MalangPreferenceGroup(title = "화면 구성 (가로 모드)") {
            ListPreference(
                prefs.keyboard.landscapeInputUiMode,
                title = "가로 모드 입력 방식",
                entries = listPrefEntries {
                    entry(LandscapeInputUiMode.NEVER_SHOW, "항상 키보드만 표시")
                    entry(LandscapeInputUiMode.DYNAMICALLY_SHOW, "입력창 자동 조절")
                    entry(LandscapeInputUiMode.ALWAYS_SHOW, "항상 전체화면 입력창 표시")
                },
            )
        }

        MalangPreferenceGroup(title = "키 크기 및 간격") {
            DialogSliderPreference(
                primaryPref = prefs.keyboard.keySpacingVertical,
                secondaryPref = prefs.keyboard.keySpacingHorizontal,
                title = "키 간격 조절",
                primaryLabel = "세로 간격",
                secondaryLabel = "가로 간격",
                valueLabel = { "${it}%" },
                min = 50,
                max = 150,
                stepIncrement = 5,
            )
            DialogSliderPreference(
                primaryPref = prefs.keyboard.fontSizeMultiplierPortrait,
                secondaryPref = prefs.keyboard.fontSizeMultiplierLandscape,
                title = "글자 크기 조절",
                primaryLabel = "세로 모드",
                secondaryLabel = "가로 모드",
                valueLabel = { "${it}%" },
                min = 50,
                max = 150,
                stepIncrement = 5,
            )
        }
    }
}
