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
import dev.malangkey.app.apptheme.MalangRowPaddingH
import dev.malangkey.app.apptheme.MalangRowPaddingV
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.ime.clipboard.QuickPhraseTriggerKey
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private const val QuickPhraseSlots = 15

/**
 * 빈 상용구 칸에 흐리게 보여 줄 쓰임새 예시 (칸 이름, 입력창 예시). 보이는 빈 칸에 앞에서부터 차례로 붙는다.
 * 상용구는 암호화 없이 저장되고 팝업에 그대로 보이므로 비밀번호 같은 값은 예시로 권하지 않는다.
 */
private val QuickPhraseHints = listOf(
    "집 주소" to "예: 서울시 마포구 말랑로 12, 101동 1001호",
    "계좌번호" to "예: 말랑은행 123-456-789012",
    "이메일" to "예: malang@example.com",
    "전화번호" to "예: 010-1234-5678",
    "회사 주소" to "예: 서울시 중구 세종대로 110",
    "자주 쓰는 링크" to "예: https://open.kakao.com/…",
)

private val TriggerKeyEntries = listOf(
    QuickPhraseTriggerKey.PERIOD to "마침표(.)",
    QuickPhraseTriggerKey.COMMA to "쉼표(,)",
    QuickPhraseTriggerKey.ENTER to "엔터(Enter)",
)

@Composable
fun ClipboardScreen() = MalangSettingsScreen(title = "클립보드", subtitle = "Clipboard") {
    val prefs by FlorisPreferenceStore
    val triggerKey by prefs.clipboard.quickPhraseTriggerKey.collectAsState()

    val triggerKeyName = TriggerKeyEntries.first { it.first == triggerKey }.second
    MalangInfoCard("키보드에서 $triggerKeyName 키를 길게 누르면 상용구 표가 뜨고, 원하는 칸에서 손을 떼면 바로 입력됩니다. 모든 키보드 레이아웃에서 동작해요.")

    MalangSettingsSection(
        title = "클립보드",
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
    // 지금 표에 보이는 빈 칸마다 쓰임새 예시를 하나씩 붙인다.
    val hintsByIndex = remember(quickPhrases, columns) {
        (0 until 3 * columns)
            .filter { quickPhrases.getOrElse(it) { "" }.isEmpty() }
            .zip(QuickPhraseHints)
            .toMap()
    }

    fun save(index: Int, text: String) {
        val updated = quickPhrases.toMutableList()
        updated[index] = text
        scope.launch { prefs.clipboard.quickPhrases.set(Json.encodeToString(updated)) }
    }

    Column(
        modifier = Modifier.padding(horizontal = MalangRowPaddingH, vertical = MalangRowPaddingV),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("상용구 편집", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("칸을 눌러 문구를 넣거나 바꿀 수 있어요.", color = MalangSettingsSummary, fontSize = 13.sp)
        for (r in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    val phrase = quickPhrases.getOrElse(index) { "" }
                    val hint = hintsByIndex[index]?.first
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
                        if (phrase.isEmpty() && hint != null) {
                            Text(
                                text = "+ $hint",
                                color = MalangSettingsSummary.copy(alpha = 0.75f),
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        } else {
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
    }

    if (editingIndex >= 0) {
        val index = editingIndex
        AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            containerColor = MalangSettingsCard,
            title = {
                Text(
                    hintsByIndex[index]?.let { "${it.first} 상용구" } ?: "${index / columns + 1}행 ${index % columns + 1}열 상용구",
                    color = MalangSettingsTitle,
                    fontFamily = JuaFontFamily,
                    fontSize = 20.sp,
                )
            },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    placeholder = { Text(hintsByIndex[index]?.second ?: "예: 감사합니다!, 지금 가요") },
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
