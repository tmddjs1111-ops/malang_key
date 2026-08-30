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

package dev.malangkey.app.settings.typing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.malangkey.R
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.enumDisplayEntriesOf
import dev.malangkey.ime.keyboard.IncognitoMode
import dev.malangkey.ime.nlp.SpellingLanguageMode
import dev.malangkey.lib.compose.FlorisHyperlinkText
import dev.malangkey.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.malangkey.app.apptheme.MalangPreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.android.AndroidVersion
import org.florisboard.lib.compose.FlorisErrorCard
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun TypingScreen() = FlorisScreen {
    title = stringRes(R.string.settings__typing__title)
    previewFieldVisible = true

    val navController = LocalNavController.current

    content {
        FlorisErrorCard(
            modifier = Modifier.padding(8.dp),
            text = "제안(시스템 자동 완성 제외) 및 맞춤법 검사는 이 릴리스에서 사용할 수 없습니다. '수정' 그룹의 설정은 제대로 구현되어 있습니다.",
        )

        MalangPreferenceGroup(title = "입력 제안") {
            SwitchPreference(
                prefs.suggestion.enabled,
                title = "입력 제안 활성화",
                summary = "입력 중 단어 제안을 표시합니다.",
            )
            SwitchPreference(
                prefs.suggestion.blockPossiblyOffensive,
                title = "부적절한 단어 차단",
                summary = "부적절할 수 있는 단어가 제안되지 않도록 합니다.",
                enabledIf = { prefs.suggestion.enabled isEqualTo true },
            )
            SwitchPreference(
                prefs.suggestion.api30InlineSuggestionsEnabled,
                title = "인라인 제안 활성화",
                summary = "지원되는 앱에서 인라인 제안을 표시합니다.",
                visibleIf = { AndroidVersion.ATLEAST_API30_R },
            )
        }

        MalangPreferenceGroup(title = "입력 수정") {
            SwitchPreference(
                prefs.correction.autoCapitalization,
                title = "자동 대문자 전환",
                summary = "문장의 첫 단어를 자동으로 대문자로 바꿉니다.",
            )
            val isAutoSpacePunctuationEnabled by prefs.correction.autoSpacePunctuation.collectAsState()
            SwitchPreference(
                prefs.correction.autoSpacePunctuation,
                icon = Icons.Default.SpaceBar,
                title = "문장 부호 뒤 자동 띄어쓰기",
                summary = "마침표나 쉼표 뒤에 자동으로 공백을 추가합니다. (실험적 기능)",
            )
            if (isAutoSpacePunctuationEnabled) {
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "문장 부호 뒤 자동 띄어쓰기는 실험적 기능이며 예상치 못한 동작이 발생할 수 있습니다. 피드백을 주시면 기능 개선에 큰 도움이 됩니다.",
                        )
                        FlorisHyperlinkText(
                            text = "피드백 스레드 (GitHub)",
                            url = "https://github.com/florisboard/florisboard/discussions/1935",
                        )
                    }
                }
            }
            SwitchPreference(
                prefs.correction.rememberCapsLockState,
                title = "Caps Lock 상태 기억",
                summary = "키보드를 다시 열었을 때 Caps Lock 상태를 유지합니다.",
            )
            SwitchPreference(
                prefs.correction.doubleSpacePeriod,
                title = "스페이스바 두 번 눌러 마침표 입력",
                summary = "스페이스바를 빠르게 두 번 누르면 마침표와 공백이 입력됩니다.",
            )
        }

        MalangPreferenceGroup(title = "맞춤법 검사") {
            val florisSpellCheckerEnabled = remember { mutableStateOf(false) }
            SpellCheckerServiceSelector(florisSpellCheckerEnabled)
            ListPreference(
                prefs.spelling.languageMode,
                icon = Icons.Default.Language,
                title = "맞춤법 검사 언어 모드",
                entries = listPrefEntries {
                    entry(SpellingLanguageMode.USE_KEYBOARD_SUBTYPES, "현재 입력 언어 따름")
                    entry(SpellingLanguageMode.USE_SYSTEM_LANGUAGES, "시스템 언어 따름")
                },
                enabledIf = { florisSpellCheckerEnabled.value },
            )
        }

        MalangPreferenceGroup(title = "사전 관리") {
            Preference(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                title = "사용자 사전",
                onClick = { navController.navigate(Routes.Settings.Dictionary) },
            )
        }
    }
}
