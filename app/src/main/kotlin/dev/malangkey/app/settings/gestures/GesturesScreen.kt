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

package dev.malangkey.app.settings.gestures

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.text.gestures.SwipeAction
import dev.patrickgold.jetpref.datastore.model.collectAsState

private val VerticalSwipeEntries = listOf(
    SwipeAction.NO_ACTION to "동작 없음",
    SwipeAction.SHIFT to "Shift",
    SwipeAction.HIDE_KEYBOARD to "키보드 숨기기",
    SwipeAction.INSERT_SPACE to "스페이스 입력",
    SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE to "이전 키보드 모드",
    SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE to "다음 키보드 모드",
    SwipeAction.SHOW_INPUT_METHOD_PICKER to "키보드 선택",
    SwipeAction.SHOW_SUBTYPE_PICKER to "언어 선택",
    SwipeAction.TOGGLE_SMARTBAR_VISIBILITY to "스마트바 표시 전환",
    SwipeAction.UNDO to "실행 취소",
    SwipeAction.REDO to "다시 실행",
)

private val SwipeLeftEntries = listOf(
    SwipeAction.NO_ACTION to "동작 없음",
    SwipeAction.DELETE_CHARACTER to "글자 삭제",
    SwipeAction.DELETE_WORD to "단어 삭제",
    SwipeAction.MOVE_CURSOR_LEFT to "커서 왼쪽 이동",
    SwipeAction.SWITCH_TO_NEXT_SUBTYPE to "다음 언어로 전환",
    SwipeAction.SWITCH_TO_PREV_SUBTYPE to "이전 언어로 전환",
    SwipeAction.UNDO to "실행 취소",
)

private val SwipeRightEntries = listOf(
    SwipeAction.NO_ACTION to "동작 없음",
    SwipeAction.INSERT_SPACE to "스페이스 입력",
    SwipeAction.MOVE_CURSOR_RIGHT to "커서 오른쪽 이동",
    SwipeAction.SWITCH_TO_NEXT_SUBTYPE to "다음 언어로 전환",
    SwipeAction.SWITCH_TO_PREV_SUBTYPE to "이전 언어로 전환",
    SwipeAction.REDO to "다시 실행",
)

private val SpaceSideEntries = listOf(
    SwipeAction.NO_ACTION to "동작 없음",
    SwipeAction.SWITCH_TO_PREV_SUBTYPE to "이전 언어로 전환",
    SwipeAction.SWITCH_TO_NEXT_SUBTYPE to "다음 언어로 전환",
    SwipeAction.MOVE_CURSOR_LEFT to "커서 이동 (밀면서 움직임)",
    SwipeAction.DELETE_WORD to "단어 삭제",
    SwipeAction.UNDO to "실행 취소",
    SwipeAction.REDO to "다시 실행",
)

private val SpaceLongPressEntries = listOf(
    SwipeAction.NO_ACTION to "동작 없음",
    SwipeAction.SHOW_INPUT_METHOD_PICKER to "키보드 선택",
    SwipeAction.SHOW_SUBTYPE_PICKER to "언어 선택",
    SwipeAction.SWITCH_TO_MEDIA_CONTEXT to "이모지 열기",
    SwipeAction.SWITCH_TO_CLIPBOARD_CONTEXT to "클립보드 열기",
    SwipeAction.HIDE_KEYBOARD to "키보드 숨기기",
)

/** 좌우 밀기 선택지에서 '커서 이동'은 밀기 방향에 맞는 값으로 저장한다. */
private fun spaceSideEntries(cursorAction: SwipeAction) =
    SpaceSideEntries.map { (action, label) -> (if (action == SwipeAction.MOVE_CURSOR_LEFT) cursorAction else action) to label }

@Composable
fun GesturesScreen() = MalangSettingsScreen(title = "제스처", subtitle = "Gesture") {
    val prefs by FlorisPreferenceStore
    val glideEnabled by prefs.glide.enabled.collectAsState()

    val emoticonSearchEnabled by prefs.keyboard.emoticonSuggestionEnabled.collectAsState()

    MalangInfoCard("스페이스바와 글자 키 위에서 밀었을 때의 동작을 고를 수 있어요.")

    MalangSettingsSection(
        title = "스페이스바",
        items = listOf(
            { MalangChoiceRow(prefs.gestures.spaceBarSwipeLeft, "왼쪽으로 밀기", spaceSideEntries(SwipeAction.MOVE_CURSOR_LEFT)) },
            { MalangChoiceRow(prefs.gestures.spaceBarSwipeRight, "오른쪽으로 밀기", spaceSideEntries(SwipeAction.MOVE_CURSOR_RIGHT)) },
            { MalangChoiceRow(prefs.gestures.spaceBarSwipeUp, "위로 밀기", VerticalSwipeEntries) },
            {
                MalangChoiceRow(
                    prefs.gestures.spaceBarLongPress,
                    title = if (emoticonSearchEnabled) "길게 누르기 (이모티콘 검색을 끄면 쓸 수 있어요)" else "길게 누르기",
                    entries = SpaceLongPressEntries,
                    enabled = !emoticonSearchEnabled,
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "글자 키 스와이프",
        items = listOf(
            { MalangChoiceRow(prefs.gestures.swipeUp, "위로 밀기", VerticalSwipeEntries, enabled = !glideEnabled) },
            { MalangChoiceRow(prefs.gestures.swipeDown, "아래로 밀기", VerticalSwipeEntries, enabled = !glideEnabled) },
            { MalangChoiceRow(prefs.gestures.swipeLeft, "왼쪽으로 밀기", SwipeLeftEntries, enabled = !glideEnabled) },
            { MalangChoiceRow(prefs.gestures.swipeRight, "오른쪽으로 밀기", SwipeRightEntries, enabled = !glideEnabled) },
        ),
    )

    MalangSettingsSection(
        title = "삭제 키",
        items = listOf(
            {
                MalangChoiceRow(
                    prefs.gestures.deleteKeySwipeLeft,
                    title = "왼쪽으로 밀기",
                    entries = listOf(
                        SwipeAction.NO_ACTION to "동작 없음",
                        SwipeAction.DELETE_CHARACTERS_PRECISELY to "글자 단위로 골라서 삭제",
                        SwipeAction.DELETE_WORD to "단어 삭제",
                        SwipeAction.DELETE_WORDS_PRECISELY to "단어 단위로 골라서 삭제",
                    ),
                )
            },
            {
                MalangChoiceRow(
                    prefs.gestures.deleteKeyLongPress,
                    title = "길게 누르기",
                    entries = listOf(
                        SwipeAction.NO_ACTION to "동작 없음",
                        SwipeAction.DELETE_CHARACTER to "글자 계속 삭제",
                        SwipeAction.DELETE_WORD to "단어씩 계속 삭제",
                    ),
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "감도",
        items = listOf(
            {
                MalangValueDialogRow(
                    prefs.gestures.swipeDistanceThreshold,
                    title = "스와이프 인식 거리 (짧을수록 민감)",
                    min = 12,
                    max = 72,
                    step = 1,
                    unit = "dp",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.gestures.swipeVelocityThreshold,
                    title = "스와이프 인식 속도",
                    min = 400,
                    max = 4000,
                    step = 100,
                    unit = " px/s",
                )
            },
        ),
    )
}
