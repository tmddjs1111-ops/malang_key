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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.R
import dev.malangkey.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.malangkey.app.apptheme.MalangPreferenceGroup
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.florisboard.lib.compose.stringRes

@OptIn(ExperimentalJetPrefDatastoreUi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClipboardScreen() = FlorisScreen {
    title = stringRes(R.string.settings__clipboard__title)
    previewFieldVisible = true

    content {
        val prefs = this.prefs
        val scope = rememberCoroutineScope()
        
        // 1. 그리드 설정 데이터 읽기
        val gridRowsPref by prefs.clipboard.quickPhrasesGridRows.collectAsState()
        val quickPhrasesJson by prefs.clipboard.quickPhrases.collectAsState()
        val quickPhraseTriggerKey by prefs.clipboard.quickPhraseTriggerKey.collectAsState()
        
        val quickPhrases = remember(quickPhrasesJson) {
            try {
                Json.decodeFromString<List<String>>(quickPhrasesJson)
            } catch (e: Exception) {
                List(15) { "" }
            }
        }
        
        var showEditDialog by remember { mutableStateOf(false) }
        var editingIndex by remember { mutableIntStateOf(-1) }
        var editingText by remember { mutableStateOf("") }

        // 상용구 저장 공통 로직
        val savePhrases: (List<String>) -> Unit = { list ->
            scope.launch {
                val fullList = list.toMutableList()
                while (fullList.size < 15) {
                    fullList.add("")
                }
                prefs.clipboard.quickPhrases.set(Json.encodeToString(fullList))
            }
        }

        // 소개 카드 배너
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "상용구 기능 안내",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                val triggerKeyName = when (quickPhraseTriggerKey) {
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.PERIOD -> "마침표(.)"
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.COMMA -> "쉼표(,)"
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.ENTER -> "엔터(Enter)"
                }
                Text(
                    text = "키보드 화면에서 $triggerKeyName 키를 길게 누르면 아래 설정된 상용구 그리드 팝업이 뜨며, 터치를 떼는 순간 즉시 붙여넣어집니다. (모든 키보드 레이아웃에서 동작)",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 18.sp
                )
            }
        }

        // 1. 상용구 동작 키 설정
        MalangPreferenceGroup(title = "호출 키 설정") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.PERIOD to "마침표(.)",
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.COMMA to "쉼표(,)",
                    dev.malangkey.ime.clipboard.QuickPhraseTriggerKey.ENTER to "엔터(Enter)"
                ).forEach { (triggerKey, label) ->
                    val isSelected = quickPhraseTriggerKey == triggerKey
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.clipboard.quickPhraseTriggerKey.set(triggerKey)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                             else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 2. 그리드 구조 설정 그룹
        MalangPreferenceGroup(title = "그리드 크기 설정") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(3, 4, 5).forEach { cols ->
                    val isSelected = gridRowsPref == cols
                    Button(
                        onClick = {
                            scope.launch {
                                prefs.clipboard.quickPhrasesGridRows.set(cols)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                                             else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$cols x 3",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 2. 인터랙티브 상용구 예시 그리드 (WYSIWYG Editor)
        MalangPreferenceGroup(title = "상용구 예시 그리드 (클릭하여 편집)") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (r in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until gridRowsPref) {
                                val index = r * gridRowsPref + c
                                val phrase = quickPhrases.getOrNull(index) ?: ""

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(55.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (phrase.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer 
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (phrase.isNotEmpty()) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            editingIndex = index
                                            editingText = phrase
                                            showEditDialog = true
                                        }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (phrase.isNotEmpty()) {
                                        Text(
                                            text = phrase,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    } else {
                                        Text(
                                            text = "+",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. 인플레이스 편집 다이얼로그
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = {
                    Text(
                        text = "${(editingIndex / gridRowsPref) + 1}행 ${(editingIndex % gridRowsPref) + 1}열 문구 설정",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = { editingText = it },
                            label = { Text("상용구 입력") },
                            placeholder = { Text("감사합니다!, 지금 가요 등") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val updatedList = quickPhrases.toMutableList()
                            updatedList[editingIndex] = editingText.trim()
                            savePhrases(updatedList)
                            showEditDialog = false
                        }
                    ) {
                        Text("저장", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row {
                        if (quickPhrases.getOrNull(editingIndex)?.isNotEmpty() == true) {
                            TextButton(
                                onClick = {
                                    val updatedList = quickPhrases.toMutableList()
                                    updatedList[editingIndex] = ""
                                    savePhrases(updatedList)
                                    showEditDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("지우기")
                            }
                        }
                        TextButton(
                            onClick = { showEditDialog = false }
                        ) {
                            Text("취소")
                        }
                    }
                }
            )
        }
    }
}
