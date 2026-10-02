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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.media.emoji.EmojiHistory
import dev.patrickgold.jetpref.datastore.model.collectAsState

private fun maxSizeLabel(size: Int): String =
    if (size == EmojiHistory.MaxSizeUnlimited) "무제한" else "${size}개"

/**
 * 이모지 설정. 이모지를 쓰는 곳이 세 군데라 쓰는 순서대로(패널 → 단어 추천 → 스페이스바 검색) 나누고,
 * 각 묶음 이름에 어디서 쓰는지를 적는다. 정렬 방식·시작 글자 수 같은 세부 설정은 기본값 그대로 두고 숨긴다.
 */
@Composable
fun MediaScreen() = MalangSettingsScreen(title = "이모지", subtitle = "Emoji") {
    val prefs by FlorisPreferenceStore
    val searchEnabled by prefs.keyboard.emoticonSuggestionEnabled.collectAsState()
    val historyEnabled by prefs.emoji.historyEnabled.collectAsState()
    val suggestionEnabled by prefs.emoji.suggestionEnabled.collectAsState()

    MalangInfoCard(
        "이모지는 세 곳에서 쓸 수 있어요.\n" +
            "① 키보드 위 😊 버튼: 이모지 패널이 열려요.\n" +
            "② 단어를 치면: 위 줄에 어울리는 이모지가 떠요. (사랑 → 😍)\n" +
            "③ 스페이스바를 길게 누르면: 이모티콘·꾸밈 글씨 검색창이 열려요."
    )

    MalangSettingsSection(
        title = "① 이모지 패널 (😊 버튼)",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.emoji.historyEnabled,
                    title = "최근 쓴 이모지 모아 보기",
                    summary = "패널 맨 앞 탭에 최근에 쓴 이모지를 모아 둬요. 길게 누르면 고정하거나 지울 수 있어요.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.emoji.historyRecentMaxSize,
                    title = "최근 이모지 개수",
                    min = 0,
                    max = 180,
                    step = 10,
                    unit = "개",
                    enabled = historyEnabled,
                    valueLabel = ::maxSizeLabel,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "② 단어로 이모지 추천",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.emoji.suggestionEnabled,
                    title = "이모지 추천",
                    summary = "단어를 치면 키보드 위 줄에 어울리는 이모지가 떠요. 누르면 단어 뒤에 붙어요. (사랑 → 사랑😍)",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.emoji.suggestionCandidateMaxCount,
                    title = "추천 개수",
                    min = 1,
                    max = 10,
                    step = 1,
                    unit = "개",
                    enabled = suggestionEnabled,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "③ 스페이스바 길게 누르기",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.keyboard.emoticonSuggestionEnabled,
                    title = "이모티콘·꾸밈 글씨 검색",
                    summary = "스페이스바를 길게 누르면 (｡•̀ᴗ-)✧ 같은 이모티콘과 꾸밈 글씨를 찾는 검색창이 열려요.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.keyboard.spaceLongPressDelay,
                    title = "길게 누르는 시간",
                    min = 500,
                    max = 5000,
                    step = 500,
                    unit = "",
                    enabled = searchEnabled,
                    valueLabel = { "${it / 1000.0}초" },
                )
            },
        ),
    )
}
