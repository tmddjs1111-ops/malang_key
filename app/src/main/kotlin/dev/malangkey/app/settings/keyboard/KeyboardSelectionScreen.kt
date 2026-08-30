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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.R
import dev.malangkey.ime.keyboard.LayoutType
import dev.malangkey.keyboardManager
import dev.malangkey.lib.compose.FlorisScreen
import dev.malangkey.subtypeManager
import org.florisboard.lib.compose.stringRes

@Composable
fun KeyboardSelectionScreen() = FlorisScreen {
    title = stringRes(R.string.settings__keyboard_selection__title)
    previewFieldVisible = true

    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    content {
        val subtypes by subtypeManager.subtypesFlow.collectAsState()
        val presets by keyboardManager.resources.subtypePresets.collectAsState()
        val layouts by keyboardManager.resources.layouts.collectAsState()

        for (preset in presets) {
            val isChecked = subtypes.any { it.equalsExcludingId(preset.toSubtype()) }
            val charactersLayout = layouts[LayoutType.CHARACTERS]?.get(preset.preferred.characters)
            
            ListItem(
                modifier = Modifier.clickable {
                    val subtype = preset.toSubtype()
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
                },
                headlineContent = {
                    val localeName = preset.locale.displayName()
                    val langOnly = preset.locale.displayLanguage()
                    var layoutName = charactersLayout?.label ?: "Unknown"
                    
                    // Remove redundant language name from layout name if it exists
                    if (layoutName.startsWith(localeName, ignoreCase = true)) {
                        layoutName = layoutName.substring(localeName.length).trim().removePrefix("-").trim()
                    } else if (layoutName.startsWith(langOnly, ignoreCase = true)) {
                        layoutName = layoutName.substring(langOnly.length).trim().removePrefix("-").trim()
                    }

                    val finalLabel = if (layoutName.isEmpty() || layoutName.equals("Unknown", ignoreCase = true)) {
                        localeName
                    } else {
                        "$localeName - $layoutName"
                    }
                    Text(text = finalLabel)
                },
                trailingContent = {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = null,
                    )
                }
            )
        }
    }
}
