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


package dev.malangkey.app.settings.typing

import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.lib.util.launchActivity
import org.florisboard.lib.android.AndroidSettings
import org.florisboard.lib.compose.observeAsState

/** 시스템에 설정된 맞춤법 검사기를 보여주고, 누르면 시스템 설정을 연다. */
@Composable
fun SpellCheckerRow() {
    val context = LocalContext.current

    val systemSpellCheckerId by AndroidSettings.Secure.observeAsState(
        key = "selected_spell_checker",
        foregroundOnly = true,
    )
    val systemSpellCheckerEnabled by AndroidSettings.Secure.observeAsState(
        key = "spell_checker_enabled",
        foregroundOnly = true,
    )
    val spellCheckerLabel = remember(systemSpellCheckerId) {
        runCatching {
            val pkgName = ComponentName.unflattenFromString(systemSpellCheckerId!!)!!.packageName
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkgName, 0)).toString()
        }.getOrNull()
    }

    MalangNavRow(
        title = "맞춤법 검사기",
        summary = when {
            systemSpellCheckerEnabled != "1" -> "시스템에서 꺼져 있어요. 눌러서 켤 수 있어요."
            spellCheckerLabel == null -> "선택된 검사기가 없어요. 눌러서 고를 수 있어요."
            else -> "사용 중: $spellCheckerLabel"
        },
        onClick = {
            context.launchActivity {
                it.addCategory(Intent.CATEGORY_DEFAULT)
                it.component = ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$SpellCheckersSettingsActivity",
                )
            }
        },
    )
}
