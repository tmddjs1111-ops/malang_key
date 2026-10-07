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

import androidx.compose.runtime.Composable
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.setup.KeyboardLayoutPicker
import dev.malangkey.lib.FlorisLocale

@Composable
fun KeyboardSelectionScreen() = MalangSettingsScreen(title = "키보드 언어 및 레이아웃", subtitle = "Keyboards") {
    MalangInfoCard("사용할 자판을 켜세요. 여러 개를 켜면 지구본 키나 스페이스바 좌우 밀기로 바꿔 쓸 수 있어요. 마지막 하나는 끌 수 없어요.")

    // 이름만으로는 어떤 자판인지 알기 어려워서 간단 설정처럼 실제 키보드 모양을 같이 보여준다.
    KeyboardLayoutPicker()
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
