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

package dev.malangkey.app.settings.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.clipboard.ClipboardSyncBehavior
import dev.malangkey.ime.clipboard.QuickPhraseTriggerKey
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val QuickPhraseSlots = 15

private val TriggerKeyEntries = listOf(
    QuickPhraseTriggerKey.PERIOD to "마침표(.)",
    QuickPhraseTriggerKey.COMMA to "쉼표(,)",
    QuickPhraseTriggerKey.ENTER to "엔터(Enter)",
)

private val SyncEntries = listOf(
    ClipboardSyncBehavior.ALL_EVENTS to "복사·지우기 모두",
    ClipboardSyncBehavior.ONLY_SET_EVENTS to "복사만",
    ClipboardSyncBehavior.ONLY_CLEAR_EVENTS to "지우기만",
    ClipboardSyncBehavior.NO_EVENTS to "동기화 안 함",
)

@Composable
fun ClipboardScreen() = MalangSettingsScreen(title = "클립보드", subtitle = "Clipboard") {
    val prefs by FlorisPreferenceStore
    val triggerKey by prefs.clipboard.quickPhraseTriggerKey.collectAsState()
    val suggestionEnabled by prefs.clipboard.suggestionEnabled.collectAsState()
    val useInternal by prefs.clipboard.useInternalClipboard.collectAsState()

    val triggerKeyName = TriggerKeyEntries.first { it.first == triggerKey }.second
    MalangInfoCard("키보드에서 $triggerKeyName 키를 길게 누르면 상용구 표가 뜨고, 원하는 칸에서 손을 떼면 바로 입력됩니다. 모든 키보드 레이아웃에서 동작해요.")

    MalangSettingsSection(
        title = "상용구",
        items = listOf(
            { MalangChoiceRow(prefs.clipboard.quickPhraseTriggerKey, "상용구 호출 키", TriggerKeyEntries) },
            {
                MalangChoiceRow(
                    prefs.clipboard.quickPhrasesGridRows,
                    title = "상용구 표 크기",
                    entries = listOf(3, 4, 5).map { it to "$it x 3" },
                )
            },
            { QuickPhraseGridEditor() },
        ),
    )

    MalangSettingsSection(
        title = "붙여넣기 추천",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.clipboard.suggestionEnabled,
                    title = "방금 복사한 내용 추천",
                    summary = "최근에 복사한 내용을 스마트바에 띄워 바로 붙여넣습니다.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.clipboard.suggestionTimeout,
                    title = "추천 유지 시간",
                    min = 10,
                    max = 300,
                    step = 10,
                    unit = "초",
                    enabled = suggestionEnabled,
                    valueLabel = { "복사 후 ${it}초" },
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "고급",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.clipboard.useInternalClipboard,
                    title = "말랑키 전용 클립보드 사용",
                    summary = "시스템 클립보드 대신 키보드 안의 클립보드를 씁니다.",
                )
            },
            {
                MalangChoiceRow(
                    prefs.clipboard.syncToFloris,
                    title = "시스템 → 말랑키 동기화",
                    entries = SyncEntries,
                    enabled = useInternal,
                )
            },
            {
                MalangChoiceRow(
                    prefs.clipboard.syncToSystem,
                    title = "말랑키 → 시스템 동기화",
                    entries = SyncEntries,
                    enabled = useInternal,
                )
            },
        ),
    )
}

/** 상용구 표 미리보기. 칸을 누르면 문구를 입력하거나 지울 수 있다. 간단 설정에서도 쓴다. */
@Composable
fun QuickPhraseGridEditor() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val columns by prefs.clipboard.quickPhrasesGridRows.collectAsState()
    val quickPhrasesJson by prefs.clipboard.quickPhrases.collectAsState()
    val quickPhrases = remember(quickPhrasesJson) {
        runCatching { Json.decodeFromString<List<String>>(quickPhrasesJson) }
            .getOrDefault(emptyList())
            .let { it + List((QuickPhraseSlots - it.size).coerceAtLeast(0)) { "" } }
    }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var editingText by remember { mutableStateOf("") }

    fun save(index: Int, text: String) {
        val updated = quickPhrases.toMutableList()
        updated[index] = text
        scope.launch { prefs.clipboard.quickPhrases.set(Json.encodeToString(updated)) }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("상용구 편집", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("칸을 눌러 문구를 넣거나 바꿀 수 있어요.", color = MalangSettingsSummary, fontSize = 13.sp)
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    val phrase = quickPhrases.getOrElse(index) { "" }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (phrase.isNotEmpty()) MalangSettingsSection else Color.Transparent)
                            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                editingIndex = index
                                editingText = phrase
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = phrase.ifEmpty { "+" },
                            color = if (phrase.isNotEmpty()) MalangSettingsCard else MalangSettingsSummary,
                            fontSize = if (phrase.isNotEmpty()) 11.sp else 16.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }

    if (editingIndex >= 0) {
        val index = editingIndex
        AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            containerColor = MalangSettingsCard,
            title = {
                Text(
                    "${index / columns + 1}행 ${index % columns + 1}열 상용구",
                    color = MalangSettingsTitle,
                    fontFamily = JuaFontFamily,
                    fontSize = 20.sp,
                )
            },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    placeholder = { Text("예: 감사합니다!, 지금 가요") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MalangSettingsSection,
                        unfocusedBorderColor = MalangSettingsBorder,
                        cursorColor = MalangSettingsSection,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    save(index, editingText.trim())
                    editingIndex = -1
                }) { Text("저장", color = MalangSettingsSection, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                Row {
                    if (quickPhrases.getOrElse(index) { "" }.isNotEmpty()) {
                        TextButton(onClick = {
                            save(index, "")
                            editingIndex = -1
                        }) { Text("지우기", color = Color(0xFFB3261E)) }
                    }
                    TextButton(onClick = { editingIndex = -1 }) { Text("취소", color = MalangSettingsSummary) }
                }
            },
        )
    }
}
