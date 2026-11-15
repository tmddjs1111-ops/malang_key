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

package dev.malangkey.app.settings.dictionary

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.nlpManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import kotlinx.coroutines.launch

@Composable
fun DictionaryScreen() = MalangSettingsScreen(title = "사용자 사전", subtitle = "Dictionary") {
    val prefs by FlorisPreferenceStore
    val navController = LocalNavController.current
    val florisEnabled by prefs.dictionary.enableFlorisUserDictionary.collectAsState()
    val systemEnabled by prefs.dictionary.enableSystemUserDictionary.collectAsState()

    MalangSettingsSection(
        title = "말랑키 사전",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.dictionary.enableFlorisUserDictionary,
                    title = "말랑키 사전 사용",
                    summary = "말랑키 안에만 저장되는 나만의 단어를 추천에 씁니다.",
                )
            },
            {
                MalangNavRow(
                    title = "말랑키 사전 관리",
                    summary = "단어와 줄임말을 추가하거나 지웁니다.",
                    enabled = florisEnabled,
                    onClick = { navController.navigate(Routes.Settings.UserDictionary(UserDictionaryType.FLORIS)) },
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "시스템 사전",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.dictionary.enableSystemUserDictionary,
                    title = "시스템 사전 사용",
                    summary = "안드로이드에 저장된 개인 사전 단어도 추천에 씁니다.",
                )
            },
            {
                MalangNavRow(
                    title = "시스템 사전 관리",
                    summary = "다른 키보드와 함께 쓰는 개인 사전입니다.",
                    enabled = systemEnabled,
                    onClick = { navController.navigate(Routes.Settings.UserDictionary(UserDictionaryType.SYSTEM)) },
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "학습 기록",
        items = listOf(
            {
                JapaneseLearningResetRow()
            },
        ),
    )
}

/** 일본어 변환에서 고른 후보를 기억한 기록을 지운다. */
@Composable
private fun JapaneseLearningResetRow() {
    val context = LocalContext.current
    val nlpManager by context.nlpManager()
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    MalangNavRow(
        title = "일본어 학습 기록 지우기",
        summary = "일본어 변환에서 자주 고른 후보를 앞에 보여 주려고 기억한 내용을 지웁니다.",
        onClick = { showDialog = true },
    )

    if (showDialog) {
        JetPrefAlertDialog(
            title = "일본어 학습 기록 지우기",
            confirmLabel = "지우기",
            onConfirm = {
                showDialog = false
                scope.launch {
                    nlpManager.clearJapaneseLearning()
                    Toast.makeText(context, "일본어 학습 기록을 지웠어요.", Toast.LENGTH_SHORT).show()
                }
            },
            dismissLabel = "취소",
            onDismiss = { showDialog = false },
        ) {
            Text("변환 후보 순서가 처음 상태로 돌아가요. 사용자 사전의 단어는 지워지지 않아요.")
        }
    }
}
