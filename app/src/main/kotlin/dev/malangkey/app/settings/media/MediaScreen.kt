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
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.media.emoji.EmojiHistory
import dev.malangkey.ime.media.emoji.EmojiSuggestionType
import dev.patrickgold.jetpref.datastore.model.collectAsState

private val HistoryUpdateStrategyEntries = listOf(
    EmojiHistory.UpdateStrategy.AUTO_SORT_PREPEND to "자동 정렬 (최근 것을 앞에)",
    EmojiHistory.UpdateStrategy.AUTO_SORT_APPEND to "자동 정렬 (최근 것을 뒤에)",
    EmojiHistory.UpdateStrategy.MANUAL_SORT_PREPEND to "직접 정렬 (새 항목을 앞에)",
    EmojiHistory.UpdateStrategy.MANUAL_SORT_APPEND to "직접 정렬 (새 항목을 뒤에)",
)

private fun maxSizeLabel(size: Int): String =
    if (size == EmojiHistory.MaxSizeUnlimited) "무제한" else "${size}개"

@Composable
fun MediaScreen() = MalangSettingsScreen(title = "이모지", subtitle = "Emoji") {
    val prefs by FlorisPreferenceStore
    val searchEnabled by prefs.keyboard.emoticonSuggestionEnabled.collectAsState()
    val historyEnabled by prefs.emoji.historyEnabled.collectAsState()
    val suggestionEnabled by prefs.emoji.suggestionEnabled.collectAsState()

    MalangSettingsSection(
        title = "스페이스바 검색창",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.keyboard.emoticonSuggestionEnabled,
                    title = "스페이스바 검색창 사용",
                    summary = "스페이스바를 길게 누르면 인스타 폰트·이모티콘 검색창이 열립니다.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.keyboard.spaceLongPressDelay,
                    title = "스페이스바 누름 시간",
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

    MalangSettingsSection(
        title = "이모지 패널",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.emoji.historyEnabled,
                    title = "최근 사용한 이모지 기억",
                    summary = "자주 쓰는 이모지를 맨 앞 탭에 모아 보여줍니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.emoji.historyRecentUpdateStrategy,
                    title = "최근 이모지 정렬",
                    entries = HistoryUpdateStrategyEntries,
                    enabled = historyEnabled,
                )
            },
            {
                MalangValueDialogRow(
                    prefs.emoji.historyRecentMaxSize,
                    title = "최근 이모지 최대 개수",
                    min = 0,
                    max = 180,
                    step = 10,
                    unit = "개",
                    enabled = historyEnabled,
                    valueLabel = ::maxSizeLabel,
                )
            },
            {
                MalangChoiceRow(
                    prefs.emoji.historyPinnedUpdateStrategy,
                    title = "고정한 이모지 정렬",
                    entries = HistoryUpdateStrategyEntries,
                    enabled = historyEnabled,
                )
            },
            {
                MalangValueDialogRow(
                    prefs.emoji.historyPinnedMaxSize,
                    title = "고정 이모지 최대 개수",
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
        title = "입력 중 이모지 추천",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.emoji.suggestionEnabled,
                    title = "이모지 추천",
                    summary = "입력하는 단어에 맞는 이모지를 스마트바에 추천합니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.emoji.suggestionType,
                    title = "추천 방식",
                    entries = listOf(
                        EmojiSuggestionType.LEADING_COLON to "콜론(:)으로 시작할 때만 (예: :smile)",
                        EmojiSuggestionType.INLINE_TEXT to "입력하는 모든 단어",
                    ),
                    enabled = suggestionEnabled,
                )
            },
            {
                MalangValueDialogRow(
                    prefs.emoji.suggestionQueryMinLength,
                    title = "추천 시작 글자 수",
                    min = 1,
                    max = 5,
                    step = 1,
                    unit = "글자",
                    enabled = suggestionEnabled,
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
            {
                MalangSwitchRow(
                    prefs.emoji.suggestionCandidateShowName,
                    title = "이모지 이름 함께 표시",
                    enabled = suggestionEnabled,
                )
            },
            {
                MalangSwitchRow(
                    prefs.emoji.suggestionUpdateHistory,
                    title = "추천으로 넣은 이모지도 최근 목록에 추가",
                    enabled = suggestionEnabled,
                )
            },
        ),
    )
}
