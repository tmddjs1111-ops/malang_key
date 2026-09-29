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

package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangToggle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.ime.keyboard.LayoutType
import dev.malangkey.keyboardManager
import dev.malangkey.lib.FlorisLocale
import dev.malangkey.subtypeManager

@Composable
fun KeyboardSelectionScreen() = MalangSettingsScreen(title = "키보드 언어 및 레이아웃", subtitle = "Keyboards") {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val presets by keyboardManager.resources.subtypePresets.collectAsState()
    val layouts by keyboardManager.resources.layouts.collectAsState()

    MalangInfoCard("사용할 자판을 켜세요. 여러 개를 켜면 지구본 키나 스페이스바 좌우 밀기로 바꿔 쓸 수 있어요. 마지막 하나는 끌 수 없어요.")

    // 언어별로 묶고, 한국어를 맨 위에 둔다.
    val byLanguage = presets
        .groupBy { it.locale.displayName() }
        .toList()
        .sortedByDescending { (_, list) -> list.first().locale.language == "ko" }
    for ((language, languagePresets) in byLanguage) {
        MalangSettingsSection(
            title = language,
            items = languagePresets.map { preset ->
                {
                    val subtype = preset.toSubtype()
                    val isChecked = subtypes.any { it.equalsExcludingId(subtype) }
                    val charactersLayout = layouts[LayoutType.CHARACTERS]?.get(preset.preferred.characters)
                    val name = keyboardDisplayName(preset.locale, charactersLayout?.label)
                        .removePrefix(preset.locale.displayName()).removePrefix(" - ")
                        .ifEmpty { language }
                    val toggle = {
                        if (isChecked) {
                            val existingSubtype = subtypes.find { it.equalsExcludingId(subtype) }
                            if (existingSubtype != null && subtypes.size > 1) {
                                subtypeManager.removeSubtype(existingSubtype)
                            }
                        } else {
                            subtypeManager.addSubtypeAndActivate(subtype)
                        }
                        // Force refresh keyboard cache
                        keyboardManager.resources.anyChangedVersion.value += 1
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = toggle)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            color = MalangSettingsTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        MalangToggle(checked = isChecked, onCheckedChange = { toggle() })
                    }
                }
            },
        )
    }
}

/**
 * "Language - Layout" label for a keyboard, without repeating the language inside the layout name.
 */
internal fun keyboardDisplayName(locale: FlorisLocale, layoutLabel: String?): String {
    val localeName = locale.displayName()
    val langOnly = locale.displayLanguage()
    var layoutName = layoutLabel ?: "Unknown"

    // Remove redundant language name from layout name if it exists
    if (layoutName.startsWith(localeName, ignoreCase = true)) {
        layoutName = layoutName.substring(localeName.length).trim().removePrefix("-").trim()
    } else if (layoutName.startsWith(langOnly, ignoreCase = true)) {
        layoutName = layoutName.substring(langOnly.length).trim().removePrefix("-").trim()
    }

    return if (layoutName.isEmpty() || layoutName.equals("Unknown", ignoreCase = true)) {
        localeName
    } else {
        "$localeName - $layoutName"
    }
}
