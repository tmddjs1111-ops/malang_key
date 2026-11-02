/*
 * Copyright (C) 2026 The MalangKey Contributors
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

package dev.malangkey.app.settings.about

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.clipboardManager
import dev.malangkey.ime.editor.InputDiagnostics
import dev.patrickgold.jetpref.datastore.model.collectAsState as collectPrefAsState

/** 실험실 입력칸 종류. PASS 같은 보안 입력칸이 할 수 있는 행동을 하나씩 흉내 낸다. */
private enum class LabField(val label: String, val description: String) {
    NORMAL("보통 칸", "평범한 입력칸. 여기서 되는지 먼저 보세요."),
    BLOCKED_READ("글 읽기 막은 칸", "앞 글자를 읽지 못하게 막은 칸 (보안 입력칸이 흔히 이렇게 해요)"),
    PASSWORD("비밀번호 칸", "비밀번호 종류로 표시된 칸"),
    BLOCKED_PASSWORD("글 읽기 막은 비밀번호 칸", "둘 다 한 칸"),
    RAW("특수 칸 (TYPE_NULL)", "입력 종류를 알려 주지 않는 칸 (터미널·일부 보안 키패드)"),
}

/** 앞뒤 글을 읽지 못하게 막는 입력 연결. */
private class BlockedReadConnection(target: InputConnection) : InputConnectionWrapper(target, true) {
    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? = null
    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? = null
    override fun getSelectedText(flags: Int): CharSequence? = null
    override fun getExtractedText(request: ExtractedTextRequest?, flags: Int): ExtractedText? = null
}

private class LabEditText(context: Context, private val blockRead: Boolean) : EditText(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs) ?: return null
        return if (blockRead) BlockedReadConnection(ic) else ic
    }
}

private fun createLabField(context: Context, field: LabField): EditText {
    val view = LabEditText(
        context,
        blockRead = field == LabField.BLOCKED_READ || field == LabField.BLOCKED_PASSWORD,
    )
    view.hint = "여기에 '손', '간' 을 쳐 보세요"
    view.textSize = 18f
    view.contentDescription = "lab_${field.name.lowercase()}"
    when (field) {
        LabField.PASSWORD, LabField.BLOCKED_PASSWORD -> {
            // 비밀번호여도 글자가 보여야 실험할 수 있어서 '보이는 비밀번호' 종류를 쓴다.
            view.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        LabField.RAW -> view.inputType = InputType.TYPE_NULL
        else -> view.inputType = InputType.TYPE_CLASS_TEXT
    }
    return view
}

/**
 * 앱 정보 > 입력칸 실험실.
 * 위: 진단 기록 켜기와 기록 보기. 아래: 보안 입력칸을 흉내 낸 실험용 입력칸들.
 */
@Composable
fun InputLabScreen() = MalangSettingsScreen(title = "입력칸 실험실", subtitle = "Input Lab", showTestInput = false) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()
    val diagnosticsOn by prefs.devtools.inputDiagnostics.collectPrefAsState()
    val lines by InputDiagnostics.lines.collectAsState()
    LaunchedEffect(diagnosticsOn) { InputDiagnostics.enabled = diagnosticsOn }

    MalangSettingsSection(
        title = "진단 기록",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.devtools.inputDiagnostics,
                    title = "진단 기록 켜기",
                    summary = "켜 두고 다른 앱(PASS 등)에서 몇 글자 친 뒤 여기로 돌아와 기록을 보내 주세요. " +
                        "친 글자가 기록에 남으니 진짜 이름·비밀번호 말고 '손' 같은 글자로 시험하세요. " +
                        "기록은 이 폰 안에만 있고, 앱을 끄면 사라져요.",
                )
            },
            {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            try {
                                clipboardManager.addNewPlaintext(lines.joinToString("\n"))
                                Toast.makeText(context, "기록을 복사했어요.", Toast.LENGTH_SHORT).show()
                            } catch (e: Throwable) {
                                Toast.makeText(context, "복사하지 못했어요: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) { Text("기록 복사") }
                        TextButton(onClick = { InputDiagnostics.clear() }) { Text("지우기") }
                    }
                    Text(
                        "기록 ${lines.size}줄 (최근 것이 아래)",
                        color = MalangSettingsSummary,
                        fontSize = 12.sp,
                    )
                    SelectionContainer {
                        Text(
                            text = if (lines.isEmpty()) "아직 기록이 없어요." else lines.takeLast(80).joinToString("\n"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MalangSettingsCard)
                                .heightIn(min = 60.dp)
                                .padding(8.dp),
                            color = MalangSettingsTitle,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 13.sp,
                        )
                    }
                }
            },
        ),
    )

    MalangSettingsSection(
        title = "실험용 입력칸",
        items = LabField.entries.map { field ->
            {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(field.label, color = MalangSettingsTitle, fontSize = 15.sp)
                    Text(field.description, color = MalangSettingsSummary, fontSize = 12.sp)
                    AndroidView(
                        factory = { createLabField(it, field) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
    )
}
