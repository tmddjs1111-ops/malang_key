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

package dev.patrickgold.florisboard.app.settings.smartbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.enumDisplayEntriesOf
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.smartbar.CandidatesDisplayMode
import dev.patrickgold.florisboard.ime.smartbar.ExtendedActionsPlacement
import dev.patrickgold.florisboard.ime.smartbar.SmartbarLayout
import dev.patrickgold.florisboard.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.florisboard.ime.smartbar.quickaction.keyData
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference
import dev.patrickgold.florisboard.app.apptheme.MalangPreferenceGroup
import kotlinx.coroutines.launch
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggIcon

@Composable
fun SmartbarScreen() = FlorisScreen {
    title = stringRes(R.string.settings__smartbar__title)
    previewFieldVisible = true

    content {
        MalangPreferenceGroup {
            DialogSliderPreference(
                pref = prefs.smartbar.malangSlotsCount,
                title = "상단바 슬롯 개수",
                summary = { _ -> "3개에서 6개 사이로 설정할 수 있습니다." },
                min = 3,
                max = 6,
                stepIncrement = 1,
                valueLabel = { "$it 칸" },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "슬롯 편집",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                text = "아래 슬롯을 클릭하여 기능을 제거하거나 추가하세요.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            MalangSlotsEditor()
        }
    }
}

@Composable
fun MalangSlotsEditor() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()
    val scope = rememberCoroutineScope()

    val malangSlots by prefs.smartbar.malangSlots.collectAsState()
    val malangSlotsCount by prefs.smartbar.malangSlotsCount.collectAsState()

    val availableActions = remember {
        listOf(
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_COPY),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_CUT),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_PASTE),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_CLEAR_HISTORY),
            QuickAction.InsertKey(TextKeyData.UNDO),
            QuickAction.InsertKey(TextKeyData.REDO),
            QuickAction.InsertKey(TextKeyData.ARROW_LEFT),
            QuickAction.InsertKey(TextKeyData.ARROW_RIGHT),
            QuickAction.InsertKey(TextKeyData.ARROW_UP),
            QuickAction.InsertKey(TextKeyData.ARROW_DOWN),
            QuickAction.InsertKey(TextKeyData.MOVE_START_OF_LINE),
            QuickAction.InsertKey(TextKeyData.MOVE_END_OF_LINE),
            QuickAction.InsertKey(TextKeyData(code = KeyCode.ENTER, label = "Enter")),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_CLIPBOARD),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
            QuickAction.InsertKey(TextKeyData.SYSTEM_INPUT_METHOD_PICKER),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Current Slots Preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until malangSlotsCount) {
                val action = malangSlots.getOrNull(i)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (action != null && action.keyData().code != 0) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                        .clickable {
                            val newList = malangSlots.toMutableList()
                            if (i < newList.size) {
                                newList[i] = QuickAction.InsertKey(TextKeyData.UNSPECIFIED)
                                scope.launch { prefs.smartbar.malangSlots.set(newList) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (action != null && action.keyData().code != 0) {
                        ActionIcon(action, evaluator, tint = Color.White)
                    } else {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "기능 팔레트",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        // Action Palette (Grid Layout)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunkedActions = availableActions.chunked(4)
            for (rowActions in chunkedActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (action in rowActions) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable {
                                    val newList = malangSlots.toMutableList()
                                    var index = -1
                                    for (i in 0 until malangSlotsCount) {
                                        if (i >= newList.size || newList[i].keyData().code == 0) {
                                            index = i
                                            break
                                        }
                                    }
                                    if (index != -1) {
                                        while (newList.size <= index) newList.add(QuickAction.InsertKey(TextKeyData.UNSPECIFIED))
                                        newList[index] = action
                                        scope.launch { prefs.smartbar.malangSlots.set(newList) }
                                    }
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            ActionIcon(action, evaluator, tint = Color.Black.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = getActionLabel(action, evaluator),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                    // Fill empty slots in the last row to maintain grid alignment
                    if (rowActions.size < 4) {
                        for (i in 0 until (4 - rowActions.size)) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getActionLabel(action: QuickAction, evaluator: dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator): String {
    val keyData = action.keyData() as? TextKeyData ?: return "미정"
    val label = evaluator.computeLabel(keyData)
    if (!label.isNullOrEmpty()) return label
    
    // Fallback labels for specific codes if computeLabel fails or is too technical
    return when (keyData.code) {
        TextKeyData.CLIPBOARD_COPY.code -> "복사"
        TextKeyData.CLIPBOARD_CUT.code -> "잘라내기"
        TextKeyData.CLIPBOARD_PASTE.code -> "붙여넣기"
        TextKeyData.CLIPBOARD_SELECT.code -> "선택"
        TextKeyData.CLIPBOARD_SELECT_ALL.code -> "전체선택"
        TextKeyData.CLIPBOARD_CLEAR_HISTORY.code -> "클립보드 삭제"
        TextKeyData.UNDO.code -> "실행취소"
        TextKeyData.REDO.code -> "다시실행"
        TextKeyData.ARROW_LEFT.code -> "왼쪽이동"
        TextKeyData.ARROW_RIGHT.code -> "오른쪽이동"
        TextKeyData.ARROW_UP.code -> "위로이동"
        TextKeyData.ARROW_DOWN.code -> "아래이동"
        TextKeyData.MOVE_START_OF_LINE.code -> "줄 시작"
        TextKeyData.MOVE_END_OF_LINE.code -> "줄 끝"
        KeyCode.DELETE -> "지우기"
        KeyCode.FORWARD_DELETE -> "앞글자 삭제"
        KeyCode.ENTER -> "엔터"
        KeyCode.SPACE -> "스페이스"
        TextKeyData.IME_UI_MODE_CLIPBOARD.code -> "클립보드"
        TextKeyData.IME_UI_MODE_MEDIA.code -> "이모지"
        TextKeyData.IME_SUBTYPE_PICKER.code -> "언어변경"
        TextKeyData.SYSTEM_INPUT_METHOD_PICKER.code -> "키보드 선택"
        else -> label ?: "기능"
    }
}

@Composable
fun ActionIcon(
    action: QuickAction,
    evaluator: dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator,
    tint: Color = Color.Unspecified
) {
    val keyData = action.keyData() as? TextKeyData ?: return
    var imageVector = evaluator.computeImageVector(keyData)
    val label = evaluator.computeLabel(keyData)

    // Manual icon fallbacks for Malang Key
    if (imageVector == null) {
        imageVector = when (keyData.code) {
            TextKeyData.CLIPBOARD_SELECT.code -> Icons.Default.SelectAll
            TextKeyData.CLIPBOARD_SELECT_ALL.code -> Icons.Default.SelectAll
            TextKeyData.CLIPBOARD_CLEAR_HISTORY.code -> Icons.Default.DeleteSweep
            TextKeyData.MOVE_START_OF_LINE.code -> Icons.Default.FirstPage
            TextKeyData.MOVE_END_OF_LINE.code -> Icons.Default.LastPage
            KeyCode.SPACE -> Icons.Default.SpaceBar
            TextKeyData.IME_SUBTYPE_PICKER.code -> Icons.Default.Language
            TextKeyData.SYSTEM_INPUT_METHOD_PICKER.code -> Icons.Default.Keyboard
            else -> null
        }
    }

    if (imageVector != null) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    } else if (label != null) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (tint != Color.Unspecified) tint else Color.Unspecified
        )
    }
}
