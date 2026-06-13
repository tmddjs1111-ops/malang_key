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

package dev.patrickgold.florisboard.app.settings.gestures

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.text.gestures.SwipeAction
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.florisboard.app.apptheme.MalangPreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import org.florisboard.lib.compose.FlorisInfoCard
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun GesturesScreen() = FlorisScreen {
    title = stringRes(R.string.settings__gestures__title)
    previewFieldVisible = true

    content {
        FlorisInfoCard(
            modifier = Modifier.padding(8.dp),
            text = "글라이드 입력(슬라이드 입력)은 현재 사용할 수 없으며, 향후 단어 제안 및 새로운 레이아웃 엔진과 함께 다시 구현될 예정입니다.",
        )

        MalangPreferenceGroup(title = "일반 제스처") {
            ListPreference(
                prefs.gestures.swipeUp,
                title = "위로 스와이프",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE, "이전 키보드 모드")
                    entry(SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE, "다음 키보드 모드")
                    entry(SwipeAction.HIDE_KEYBOARD, "키보드 숨기기")
                    entry(SwipeAction.INSERT_SPACE, "스페이스 입력")
                    entry(SwipeAction.SHIFT, "Shift")
                    entry(SwipeAction.SHOW_INPUT_METHOD_PICKER, "키보드 선택")
                    entry(SwipeAction.SHOW_SUBTYPE_PICKER, "언어 선택")
                    entry(SwipeAction.TOGGLE_SMARTBAR_VISIBILITY, "스마트바 표시 전환")
                    entry(SwipeAction.UNDO, "실행 취소")
                    entry(SwipeAction.REDO, "다시 실행")
                },
                enabledIf = { prefs.glide.enabled isEqualTo false },
            )
            ListPreference(
                prefs.gestures.swipeDown,
                title = "아래로 스와이프",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.CYCLE_TO_PREVIOUS_KEYBOARD_MODE, "이전 키보드 모드")
                    entry(SwipeAction.CYCLE_TO_NEXT_KEYBOARD_MODE, "다음 키보드 모드")
                    entry(SwipeAction.HIDE_KEYBOARD, "키보드 숨기기")
                    entry(SwipeAction.INSERT_SPACE, "스페이스 입력")
                    entry(SwipeAction.SHIFT, "Shift")
                    entry(SwipeAction.SHOW_INPUT_METHOD_PICKER, "키보드 선택")
                    entry(SwipeAction.SHOW_SUBTYPE_PICKER, "언어 선택")
                    entry(SwipeAction.TOGGLE_SMARTBAR_VISIBILITY, "스마트바 표시 전환")
                    entry(SwipeAction.UNDO, "실행 취소")
                    entry(SwipeAction.REDO, "다시 실행")
                },
                enabledIf = { prefs.glide.enabled isEqualTo false },
            )
            ListPreference(
                prefs.gestures.swipeLeft,
                title = "왼쪽으로 스와이프",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.DELETE_CHARACTER, "글자 삭제")
                    entry(SwipeAction.DELETE_WORD, "단어 삭제")
                    entry(SwipeAction.MOVE_CURSOR_LEFT, "커서 왼쪽 이동")
                    entry(SwipeAction.SWITCH_TO_PREV_SUBTYPE, "이전 언어로 전환")
                    entry(SwipeAction.UNDO, "실행 취소")
                },
                enabledIf = { prefs.glide.enabled isEqualTo false },
            )
            ListPreference(
                prefs.gestures.swipeRight,
                title = "오른쪽으로 스와이프",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.INSERT_SPACE, "스페이스 입력")
                    entry(SwipeAction.MOVE_CURSOR_RIGHT, "커서 오른쪽 이동")
                    entry(SwipeAction.SWITCH_TO_NEXT_SUBTYPE, "다음 언어로 전환")
                    entry(SwipeAction.REDO, "다시 실행")
                },
                enabledIf = { prefs.glide.enabled isEqualTo false },
            )
        }

        MalangPreferenceGroup(title = "특수 키 제스처") {
            ListPreference(
                prefs.gestures.deleteKeySwipeLeft,
                title = "삭제 키 왼쪽 스와이프",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.DELETE_CHARACTERS_PRECISELY, "글자 단위 정밀 삭제")
                    entry(SwipeAction.DELETE_WORD, "단어 삭제")
                    entry(SwipeAction.DELETE_WORDS_PRECISELY, "단어 단위 정밀 삭제")
                },
            )
            ListPreference(
                prefs.gestures.deleteKeyLongPress,
                title = "삭제 키 길게 누르기",
                entries = listPrefEntries {
                    entry(SwipeAction.NO_ACTION, "동작 없음")
                    entry(SwipeAction.DELETE_CHARACTER, "글자 계속 삭제")
                    entry(SwipeAction.DELETE_WORD, "단어 삭제")
                },
            )
            DialogSliderPreference(
                prefs.gestures.swipeVelocityThreshold,
                title = "스와이프 속도 임계값",
                valueLabel = { "${it} px/s" },
                min = 400,
                max = 4000,
                stepIncrement = 100,
            )
            DialogSliderPreference(
                prefs.gestures.swipeDistanceThreshold,
                title = "스와이프 거리 임계값",
                valueLabel = { "${it} dp" },
                min = 12,
                max = 72,
                stepIncrement = 1,
            )
        }
    }
}
