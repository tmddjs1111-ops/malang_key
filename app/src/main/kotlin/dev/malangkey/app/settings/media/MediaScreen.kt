/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.malangkey.app.settings.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.malangkey.app.apptheme.MalangPreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun MediaScreen() = FlorisScreen {
    title = "인스타 폰트 & 이모티콘 설정"
    previewFieldVisible = true
    iconSpaceReserved = true

    val prefs by FlorisPreferenceStore

    content {
        MalangPreferenceGroup(title = "기본 설정") {
            SwitchPreference(
                prefs.keyboard.emoticonSuggestionEnabled,
                icon = Icons.Outlined.EmojiEmotions,
                title = "스페이스바 검색창 사용",
                summary = "스페이스바를 길게 누르면 인스타 폰트 및 이모티콘 검색창이 나타납니다.",
            )
            DialogSliderPreference(
                prefs.keyboard.spaceLongPressDelay,
                icon = Icons.Outlined.Timer,
                title = "스페이스바 누름 시간",
                valueLabel = { "${it / 1000.0}초" },
                min = 500,
                max = 5000,
                stepIncrement = 500,
                enabledIf = { prefs.keyboard.emoticonSuggestionEnabled.isTrue() },
            )
        }
    }
}
