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

package dev.malangkey.app.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import dev.malangkey.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.lib.io.FlorisRef
import dev.malangkey.lib.io.loadTextAsset
import org.florisboard.lib.compose.florisVerticalScroll
import org.florisboard.lib.compose.stringRes

@Composable
fun ProjectLicenseScreen() = MalangSettingsScreen(
    title = "말랑키 라이선스",
    subtitle = "Apache License 2.0",
    scrollable = false,
    showTestInput = false,
) {
    val context = LocalContext.current

    run {
        // Forcing LTR because the Apache 2.0 License shipped and displayed
        // is hard to read if rendered in RTL. Also it is in English so forcing
        // LTR here makes most sense.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MalangSettingsCard)
                    .padding(12.dp)
                    .florisVerticalScroll(),
            ) {
                val licenseText = FlorisRef.assets("license/project_license.txt").loadTextAsset(
                    context
                ).getOrElse {
                    stringRes(R.string.about__project_license__error_license_text_failed, "error_message" to (it.message ?: ""))
                }
                Text(
                    text = remember(licenseText) { reflowLicenseText(licenseText) },
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MalangSettingsTitle,
                )
            }
        }
    }
}

/**
 * 라이선스 원문은 80자마다 줄이 끊겨 있어서 휴대폰 폭에서는 줄이 두 번씩 꺾인다.
 * 빈 줄로 나뉜 문단을 한 줄로 이어 화면 폭에 맞춰 줄바꿈되게 한다. 가운데 정렬된 머리말은 그대로 둔다.
 */
private fun reflowLicenseText(text: String): String {
    return text.replace("\r\n", "\n").split(Regex("\n\\s*\n")).joinToString("\n\n") { paragraph ->
        val lines = paragraph.lines().filter { it.isNotBlank() }
        if ((lines.firstOrNull()?.takeWhile { it == ' ' }?.length ?: 0) > 12) {
            lines.joinToString("\n") { it.trim() }
        } else {
            lines.joinToString(" ") { it.trim() }
        }
    }.trim()
}
