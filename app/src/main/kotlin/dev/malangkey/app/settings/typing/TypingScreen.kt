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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.ime.keyboard.IncognitoMode
import dev.malangkey.ime.smartbar.CandidatesDisplayMode
import dev.malangkey.ime.smartbar.IncognitoDisplayMode
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.android.AndroidVersion

@Composable
fun TypingScreen() = MalangSettingsScreen(title = "입력 및 수정", subtitle = "Typing") {
    val prefs by FlorisPreferenceStore
    val navController = LocalNavController.current
    val suggestionEnabled by prefs.suggestion.enabled.collectAsState()
    val autoSpacePunctuation by prefs.correction.autoSpacePunctuation.collectAsState()

    MalangSettingsSection(
        title = "입력 수정",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.correction.autoCapitalization,
                    title = "자동 대문자",
                    summary = "영어 문장의 첫 글자를 대문자로 바꿉니다.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.correction.doubleSpacePeriod,
                    title = "스페이스 두 번으로 마침표",
                    summary = "스페이스바를 빠르게 두 번 누르면 마침표와 띄어쓰기를 넣습니다.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.correction.autoSpacePunctuation,
                    title = "문장 부호 뒤 자동 띄어쓰기",
                    summary = if (autoSpacePunctuation) {
                        "실험 기능이라 가끔 의도와 다르게 띄어질 수 있어요."
                    } else {
                        "마침표·쉼표 뒤에 자동으로 띄어씁니다. (실험 기능)"
                    },
                )
            },
            {
                MalangSwitchRow(
                    prefs.correction.rememberCapsLockState,
                    title = "Caps Lock 상태 기억",
                    summary = "키보드를 다시 열어도 Caps Lock을 유지합니다.",
                )
            },
        ),
    )

    MalangInfoCard("단어 추천과 맞춤법 검사는 아직 준비 중이에요. 켜두셔도 지금은 일부 기능만 동작합니다.")

    MalangSettingsSection(
        title = "단어 추천",
        items = listOfNotNull<@Composable () -> Unit>(
            {
                MalangSwitchRow(
                    prefs.suggestion.enabled,
                    title = "단어 추천",
                    summary = "입력 중인 단어를 스마트 바에 추천합니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.suggestion.displayMode,
                    title = "추천 표시 방식",
                    entries = listOf(
                        CandidatesDisplayMode.CLASSIC to "3개 고정",
                        CandidatesDisplayMode.DYNAMIC to "길이에 맞춰 표시",
                        CandidatesDisplayMode.DYNAMIC_SCROLLABLE to "길이에 맞춰 표시 (옆으로 넘기기)",
                    ),
                    enabled = suggestionEnabled,
                )
            },
            {
                MalangSwitchRow(
                    prefs.suggestion.blockPossiblyOffensive,
                    title = "부적절한 단어 거르기",
                    enabled = suggestionEnabled,
                )
            },
            if (AndroidVersion.ATLEAST_API30_R) {
                {
                    MalangSwitchRow(
                        prefs.suggestion.api30InlineSuggestionsEnabled,
                        title = "자동 완성 추천 표시",
                        summary = "비밀번호 관리자 등 앱이 보내는 자동 완성을 스마트 바에 보여줍니다.",
                    )
                }
            } else {
                null
            },
        ),
    )

    MalangSettingsSection(
        title = "시크릿 모드",
        items = listOf(
            {
                MalangChoiceRow(
                    prefs.suggestion.incognitoMode,
                    title = "시크릿 모드",
                    entries = listOf(
                        IncognitoMode.DYNAMIC_ON_OFF to "앱이 요청할 때만 (추천)",
                        IncognitoMode.FORCE_ON to "항상 켜기",
                        IncognitoMode.FORCE_OFF to "항상 끄기",
                    ),
                )
            },
            {
                MalangChoiceRow(
                    prefs.keyboard.incognitoDisplayMode,
                    title = "시크릿 모드 표시",
                    entries = listOf(
                        IncognitoDisplayMode.DISPLAY_BEHIND_KEYBOARD to "키보드 뒤에 아이콘 표시",
                        IncognitoDisplayMode.REPLACE_SHARED_ACTIONS_TOGGLE to "스마트 바 버튼 자리에 표시",
                    ),
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "맞춤법·사전",
        items = listOf(
            { SpellCheckerRow() },
            {
                MalangNavRow(
                    title = "사용자 사전",
                    summary = "자주 쓰는 단어와 줄임말을 등록합니다.",
                    onClick = { navController.navigate(Routes.Settings.Dictionary) },
                )
            },
        ),
    )
}
