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

package dev.patrickgold.florisboard.app.apptheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.patrickgold.florisboard.app.AppTheme
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.observeAsState

private val MalangColorScheme = lightColorScheme(
    primary = MalangPrimary,
    onPrimary = MalangBg,
    primaryContainer = MalangTertiary,
    onPrimaryContainer = MalangText,
    secondary = MalangSecondary,
    onSecondary = MalangBg,
    secondaryContainer = MalangTertiary,
    onSecondaryContainer = MalangText,
    tertiary = MalangTertiary,
    onTertiary = MalangText,
    tertiaryContainer = MalangTertiary,
    onTertiaryContainer = MalangText,
    background = MalangBg,
    onBackground = MalangText,
    surface = MalangBg,
    onSurface = MalangText,
    surfaceVariant = MalangTertiary,
    onSurfaceVariant = MalangText,
    surfaceTint = MalangBg,
)

private val DarkMalangColorScheme = MalangColorScheme

@Composable
fun FlorisAppTheme(
    theme: AppTheme = AppTheme.AUTO,
    content: @Composable () -> Unit
) {
    val prefs by FlorisPreferenceStore
    // 타입을 명시적으로 지정하여 컴파일 오류 방지
    val appFontFamilyId by prefs.appFontFamily.observeAsState()

    val isDark = when (theme) {
        AppTheme.AUTO, AppTheme.AUTO_AMOLED -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED_DARK -> true
    }

    val colorScheme = if (isDark) {
        DarkMalangColorScheme
    } else {
        MalangColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypographyFor(appFontFamilyId),
        shapes = Shapes,
        content = content
    )
}
