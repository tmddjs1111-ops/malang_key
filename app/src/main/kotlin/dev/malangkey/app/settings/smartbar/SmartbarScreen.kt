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

package dev.malangkey.app.settings.smartbar

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
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.computeImageVector
import dev.malangkey.ime.keyboard.computeLabel
import dev.malangkey.ime.smartbar.ExtendedActionsPlacement
import dev.malangkey.ime.smartbar.SmartbarLayout
import dev.malangkey.ime.smartbar.quickaction.QuickAction
import dev.malangkey.ime.smartbar.quickaction.keyData
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKeyData
import dev.malangkey.keyboardManager
import dev.malangkey.app.apptheme.MalangRowPaddingH
import dev.malangkey.app.apptheme.MalangRowPaddingV
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsBg
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.snygg.ui.SnyggIcon

@Composable
fun SmartbarScreen() = MalangSettingsScreen(title = "스마트 바", subtitle = "Smart Bar") {
    val prefs by FlorisPreferenceStore
    val enabled by prefs.smartbar.enabled.collectAsState()
    val layout by prefs.smartbar.layout.collectAsState()

    MalangSettingsSection(
        title = "스마트 바",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.smartbar.enabled,
                    title = "스마트 바 사용",
                    summary = "키보드 위에 자주 쓰는 기능 버튼 줄을 표시합니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.smartbar.layout,
                    title = "표시 방식",
                    entries = listOf(
                        SmartbarLayout.MALANG_SLOTS to "말랑 슬롯 (추천)",
                        SmartbarLayout.SUGGESTIONS_ONLY to "추천 단어만",
                        SmartbarLayout.ACTIONS_ONLY to "기능 버튼만",
                        SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED to "추천 단어 + 기능 버튼 (한 줄)",
                        SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED to "추천 단어 + 기능 버튼 (두 줄)",
                    ),
                    enabled = enabled,
                )
            },
        ),
    )

    when (layout) {
        SmartbarLayout.MALANG_SLOTS -> MalangSettingsSection(
            title = "말랑 슬롯",
            items = listOf(
                {
                    MalangValueDialogRow(
                        prefs.smartbar.malangSlotsCount,
                        title = "슬롯 개수",
                        min = 3,
                        max = 6,
                        step = 1,
                        unit = "칸",
                        enabled = enabled,
                    )
                },
                {
                    Column(modifier = Modifier.padding(horizontal = MalangRowPaddingH, vertical = MalangRowPaddingV)) {
                        Text("슬롯 편집", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "위 칸을 누르면 비우고, 아래 기능을 누르면 빈 칸에 들어갑니다.",
                            modifier = Modifier.padding(top = 6.dp),
                            color = MalangSettingsSummary,
                            fontSize = 13.sp,
                        )
                        MalangSlotsEditor()
                    }
                },
            ),
        )
        SmartbarLayout.SUGGESTIONS_ACTIONS_SHARED -> MalangSettingsSection(
            title = "표시 방식 옵션",
            items = listOf(
                { MalangSwitchRow(prefs.smartbar.flipToggles, "펼치기 버튼 위치 바꾸기", enabled = enabled) },
                { MalangSwitchRow(prefs.smartbar.sharedActionsExpandWithAnimation, "펼칠 때 애니메이션", enabled = enabled) },
            ),
        )
        SmartbarLayout.SUGGESTIONS_ACTIONS_EXTENDED -> MalangSettingsSection(
            title = "표시 방식 옵션",
            items = listOf(
                { MalangSwitchRow(prefs.smartbar.flipToggles, "펼치기 버튼 위치 바꾸기", enabled = enabled) },
                {
                    MalangChoiceRow(
                        prefs.smartbar.extendedActionsPlacement,
                        title = "기능 버튼 줄 위치",
                        entries = listOf(
                            ExtendedActionsPlacement.ABOVE_CANDIDATES to "추천 단어 위",
                            ExtendedActionsPlacement.BELOW_CANDIDATES to "추천 단어 아래",
                            ExtendedActionsPlacement.OVERLAY_APP_UI to "앱 화면 위에 겹치기",
                        ),
                        enabled = enabled,
                    )
                },
            ),
        )
        else -> Unit
    }

    if (layout != SmartbarLayout.MALANG_SLOTS && layout != SmartbarLayout.SUGGESTIONS_ONLY) {
        MalangInfoCard("기능 버튼의 종류와 순서는 키보드 스마트 바의 기능 편집 버튼에서 바꿀 수 있어요.")
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
            QuickAction.InsertKey(TextKeyData.UNDO),
            QuickAction.InsertKey(TextKeyData.REDO),
            QuickAction.InsertKey(TextKeyData.ARROW_LEFT),
            QuickAction.InsertKey(TextKeyData.ARROW_RIGHT),
            QuickAction.InsertKey(TextKeyData.ARROW_UP),
            QuickAction.InsertKey(TextKeyData.ARROW_DOWN),
            QuickAction.InsertKey(TextKeyData.MOVE_START_OF_LINE),
            QuickAction.InsertKey(TextKeyData.MOVE_END_OF_LINE),
            QuickAction.InsertKey(TextKeyData(code = KeyCode.ENTER, label = "Enter")),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_EDITING),
            QuickAction.InsertKey(TextKeyData.SYSTEM_INPUT_METHOD_PICKER),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        // Current Slots Preview
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(MalangSettingsSection, RoundedCornerShape(12.dp))
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
                                .background(MalangSettingsBg)
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
fun getActionLabel(action: QuickAction, evaluator: dev.malangkey.ime.keyboard.ComputingEvaluator): String {
    val keyData = action.keyData() as? TextKeyData ?: return "미정"
    // 한국어 이름이 있는 기능은 그것을 먼저 쓴다 (키보드가 주는 이름은 영어일 때가 있다).
    koreanActionLabel(keyData.code)?.let { return it }
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
        TextKeyData.IME_UI_MODE_EDITING.code -> "커서 이동"
        TextKeyData.IME_SUBTYPE_PICKER.code -> "언어변경"
        TextKeyData.SYSTEM_INPUT_METHOD_PICKER.code -> "키보드 선택"
        else -> label ?: "기능"
    }
}

private fun koreanActionLabel(code: Int): String? = when (code) {
    TextKeyData.CLIPBOARD_COPY.code -> "복사"
    TextKeyData.CLIPBOARD_CUT.code -> "잘라내기"
    TextKeyData.CLIPBOARD_PASTE.code -> "붙여넣기"
    TextKeyData.CLIPBOARD_SELECT.code -> "선택"
    TextKeyData.CLIPBOARD_SELECT_ALL.code -> "전체 선택"
    TextKeyData.CLIPBOARD_CLEAR_HISTORY.code -> "기록 삭제"
    TextKeyData.UNDO.code -> "실행 취소"
    TextKeyData.REDO.code -> "다시 실행"
    TextKeyData.ARROW_LEFT.code -> "왼쪽"
    TextKeyData.ARROW_RIGHT.code -> "오른쪽"
    TextKeyData.ARROW_UP.code -> "위"
    TextKeyData.ARROW_DOWN.code -> "아래"
    TextKeyData.MOVE_START_OF_LINE.code -> "줄 처음"
    TextKeyData.MOVE_END_OF_LINE.code -> "줄 끝"
    KeyCode.ENTER -> "엔터"
    TextKeyData.IME_UI_MODE_CLIPBOARD.code -> "클립보드"
    TextKeyData.IME_UI_MODE_MEDIA.code -> "이모지"
    TextKeyData.IME_UI_MODE_EDITING.code -> "커서 이동"
    TextKeyData.SYSTEM_INPUT_METHOD_PICKER.code -> "키보드 선택"
    else -> null
}

@Composable
fun ActionIcon(
    action: QuickAction,
    evaluator: dev.malangkey.ime.keyboard.ComputingEvaluator,
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
