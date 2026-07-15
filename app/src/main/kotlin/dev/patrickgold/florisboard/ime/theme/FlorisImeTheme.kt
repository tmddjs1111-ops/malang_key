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

package dev.patrickgold.florisboard.ime.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.window.LocalWindowController
import dev.patrickgold.florisboard.keyboardManager
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggPropertySetEditor
import org.florisboard.lib.snygg.SnyggSinglePropertySetEditor
import org.florisboard.lib.snygg.ui.ProvideSnyggTheme
import org.florisboard.lib.snygg.ui.rememberSnyggTheme
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue

data class MalangConfig(
    val isGlassmorphismEnabled: Boolean = false,
    val glassmorphismTransparency: Float = 0.3f,
    val isNeumorphismEnabled: Boolean = false,
    val squircleShapeEnabled: Boolean = false,
    val malangSoundEnabled: Boolean = false,
)

val LocalMalangConfig = compositionLocalOf { MalangConfig() }

@Composable
fun FlorisImeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val windowController = LocalWindowController.current

    val keyboardManager by context.keyboardManager()
    val themeManager by context.themeManager()

    val prefs by FlorisPreferenceStore
    val accentColor by prefs.theme.accentColor.collectAsState()
    val keyboardFontFamily by prefs.malang.keyboardFontFamily.collectAsState()

    val activeThemeInfo by themeManager.activeThemeInfo.collectAsState()
    val dayThemeId by prefs.theme.dayThemeId.collectAsState()
    val themeMode by prefs.theme.mode.collectAsState()

    val customKeyboardBgColor by prefs.malang.customKeyboardBgColor.collectAsState()
    val customKeyBgColor by prefs.malang.customKeyBgColor.collectAsState()
    val customKeyTextColor by prefs.malang.customKeyTextColor.collectAsState()
    val customEnterKeyBgColor by prefs.malang.customEnterKeyBgColor.collectAsState()
    val customEnterKeyTextColor by prefs.malang.customEnterKeyTextColor.collectAsState()
    val customRealEnterKeyBgColor by prefs.malang.customRealEnterKeyBgColor.collectAsState()
    val customRealEnterKeyTextColor by prefs.malang.customRealEnterKeyTextColor.collectAsState()
    val keyCornerRadius by prefs.malang.keyCornerRadius.collectAsState()
    
    val isGlassmorphismEnabled by prefs.malang.isGlassmorphismEnabled.collectAsState()
    val glassmorphismTransparency by prefs.malang.glassmorphismTransparency.collectAsState()
    val isNeumorphismEnabled by prefs.malang.isNeumorphismEnabled.collectAsState()
    val squircleShapeEnabled by prefs.malang.squircleShapeEnabled.collectAsState()
    val malangSoundEnabled by prefs.malang.malangSoundEnabled.collectAsState()

    val malangConfig = remember(isGlassmorphismEnabled, glassmorphismTransparency, isNeumorphismEnabled, squircleShapeEnabled, malangSoundEnabled) {
        MalangConfig(
            isGlassmorphismEnabled,
            glassmorphismTransparency,
            isNeumorphismEnabled,
            squircleShapeEnabled,
            malangSoundEnabled
        )
    }

    val assetResolver = remember(activeThemeInfo) {
        FlorisAssetResolver(context, activeThemeInfo)
    }
    
    val stylesheet = remember(
        activeThemeInfo,
        customKeyboardBgColor,
        customKeyBgColor,
        customKeyTextColor,
        customEnterKeyBgColor,
        customEnterKeyTextColor,
        customRealEnterKeyBgColor,
        customRealEnterKeyTextColor,
        keyCornerRadius,
        keyboardFontFamily,
        isGlassmorphismEnabled,
        glassmorphismTransparency,
        isNeumorphismEnabled,
        squircleShapeEnabled,
        dayThemeId,
        themeMode
    ) {
        var baseStylesheet = activeThemeInfo.stylesheet
        val isCustomThemeSelected = dayThemeId.componentId == "custom" && (themeMode == ThemeMode.ALWAYS_DAY || themeMode == ThemeMode.FOLLOW_SYSTEM)
        
        val isKeyboardBgCustom = isCustomThemeSelected && customKeyboardBgColor != Color.Unspecified
        val isKeyBgCustom = isCustomThemeSelected && customKeyBgColor != Color.Unspecified
        val isKeyTextCustom = isCustomThemeSelected && customKeyTextColor != Color.Unspecified
        val isEnterKeyBgCustom = isCustomThemeSelected && customEnterKeyBgColor != Color.Unspecified
        val isEnterKeyTextCustom = isCustomThemeSelected && customEnterKeyTextColor != Color.Unspecified
        val isRealEnterKeyBgCustom = isCustomThemeSelected && customRealEnterKeyBgColor != Color.Unspecified
        val isRealEnterKeyTextCustom = isCustomThemeSelected && customRealEnterKeyTextColor != Color.Unspecified
        val isFontCustom = keyboardFontFamily != "system"
        
        if (isKeyboardBgCustom || isKeyBgCustom || isKeyTextCustom || isEnterKeyBgCustom || isEnterKeyTextCustom || isRealEnterKeyBgCustom || isRealEnterKeyTextCustom || (isCustomThemeSelected && keyCornerRadius != 6) || isFontCustom || (isCustomThemeSelected && isGlassmorphismEnabled) || (isCustomThemeSelected && isNeumorphismEnabled) || (isCustomThemeSelected && squircleShapeEnabled)) {
            val editor = baseStylesheet.edit()
            
            val rootRule = SnyggRule.fromOrNull("root")
            if (rootRule != null) {
                val propEditor = editor.rules.getOrPut(rootRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    val bg = if (isGlassmorphismEnabled) customKeyboardBgColor.copy(alpha = glassmorphismTransparency) else customKeyboardBgColor
                    propEditor.properties["background"] = SnyggStaticColorValue(bg)
                } else if (isGlassmorphismEnabled) {
                    propEditor.properties["background"] = SnyggStaticColorValue(Color.White.copy(alpha = glassmorphismTransparency))
                }
            }
            
            val keyboardRule = SnyggRule.fromOrNull("keyboard")
            if (keyboardRule != null) {
                val propEditor = editor.rules.getOrPut(keyboardRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    val bg = if (isGlassmorphismEnabled) customKeyboardBgColor.copy(alpha = glassmorphismTransparency) else customKeyboardBgColor
                    propEditor.properties["background"] = SnyggStaticColorValue(bg)
                } else if (isGlassmorphismEnabled) {
                    propEditor.properties["background"] = SnyggStaticColorValue(Color.White.copy(alpha = glassmorphismTransparency))
                }
            }
            
            val windowRule = SnyggRule.fromOrNull("window")
            if (windowRule != null) {
                val propEditor = editor.rules.getOrPut(windowRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    val bg = if (isGlassmorphismEnabled) customKeyboardBgColor.copy(alpha = glassmorphismTransparency) else customKeyboardBgColor
                    propEditor.properties["background"] = SnyggStaticColorValue(bg)
                } else if (isGlassmorphismEnabled) {
                    propEditor.properties["background"] = SnyggStaticColorValue(Color.Transparent)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
            }
            
            val smartbarRule = SnyggRule.fromOrNull("smartbar")
            if (smartbarRule != null) {
                val propEditor = editor.rules.getOrPut(smartbarRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    val bg = if (isGlassmorphismEnabled) customKeyboardBgColor.copy(alpha = glassmorphismTransparency) else customKeyboardBgColor
                    propEditor.properties["background"] = SnyggStaticColorValue(bg)
                } else if (isGlassmorphismEnabled) {
                    propEditor.properties["background"] = SnyggStaticColorValue(Color.Transparent)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
            }
            
            val keyRule = SnyggRule.fromOrNull("key")
            if (keyRule != null) {
                val propEditor = editor.rules.getOrPut(keyRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyBgCustom) {
                    val bg = if (isGlassmorphismEnabled) customKeyBgColor.copy(alpha = glassmorphismTransparency * 1.5f) else customKeyBgColor
                    propEditor.properties["background"] = SnyggStaticColorValue(bg)
                } else if (isGlassmorphismEnabled || isNeumorphismEnabled) {
                    propEditor.properties["background"] = SnyggStaticColorValue(Color.White.copy(alpha = if (isGlassmorphismEnabled) glassmorphismTransparency * 1.5f else 1.0f))
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
                if (squircleShapeEnabled) {
                    propEditor.properties["shape"] = SnyggRoundedCornerDpShapeValue(16.dp, 16.dp, 16.dp, 16.dp)
                } else if (keyCornerRadius != 6) {
                    val radius = keyCornerRadius.toFloat().dp
                    propEditor.properties["shape"] = SnyggRoundedCornerDpShapeValue(radius, radius, radius, radius)
                }
                if (isFontCustom) {
                    val rulesWithText = listOf("keyboard", "key", "key-hint", "key-popup-element", "smartbar-action-tile", "smartbar-action-tile-text")
                    for (element in rulesWithText) {
                        val elementRule = SnyggRule.fromOrNull(element)
                        if (elementRule != null) {
                            val propEditor = editor.rules.getOrPut(elementRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                            val typo = dev.patrickgold.florisboard.app.apptheme.getTypographyFor(keyboardFontFamily)
                            val composeFontFamily = typo.bodyLarge.fontFamily
                            if (composeFontFamily != null) {
                                propEditor.properties["font-family"] = SnyggGenericFontFamilyValue(composeFontFamily)
                            } else {
                                propEditor.properties["font-family"] = SnyggCustomFontFamilyValue(keyboardFontFamily)
                            }
                        }
                    }
                }
            }
            
            if (isKeyTextCustom) {
                val smartbarElements = listOf(
                    "smartbar-action-key",
                    "smartbar-action-key:disabled",
                    "smartbar-action-key:pressed",
                    "smartbar-action-tile",
                    "smartbar-action-tile:disabled",
                    "smartbar-action-tile-icon",
                    "smartbar-action-tile-text",
                    "smartbar-shared-actions-toggle",
                    "smartbar-shared-actions-toggle:disabled",
                    "smartbar-shared-actions-toggle:pressed",
                    "smartbar-extended-actions-toggle",
                    "smartbar-extended-actions-toggle:disabled",
                    "smartbar-extended-actions-toggle:pressed"
                )
                for (element in smartbarElements) {
                    val elementRule = SnyggRule.fromOrNull(element)
                    if (elementRule != null) {
                        val elementPropEditor = editor.rules.getOrPut(elementRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                        elementPropEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                    }
                }
            }


            val actionKeyCodes = listOf(10, -1, -2, -3, -4, -5, -7, -8, -11, -201, -202, -203, -204, -205, -206, -207, -212, -213, -227, -232, -301, 32, 44, 46)
            for (code in actionKeyCodes) {
                val actionKeyRule = SnyggRule.fromOrNull("key[code=$code]")
                if (actionKeyRule != null) {
                    val propEditor = editor.rules.getOrPut(actionKeyRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                    if (code == 10 && isRealEnterKeyBgCustom) {
                        propEditor.properties["background"] = SnyggStaticColorValue(customRealEnterKeyBgColor)
                    } else if (isEnterKeyBgCustom) {
                        propEditor.properties["background"] = SnyggStaticColorValue(customEnterKeyBgColor)
                    } else if (code == 32) {
                        if (isKeyBgCustom) {
                            val bg = if (isGlassmorphismEnabled) customKeyBgColor.copy(alpha = glassmorphismTransparency * 1.5f) else customKeyBgColor
                            propEditor.properties["background"] = SnyggStaticColorValue(bg)
                        } else if (isGlassmorphismEnabled || isNeumorphismEnabled) {
                            propEditor.properties["background"] = SnyggStaticColorValue(Color.White.copy(alpha = if (isGlassmorphismEnabled) glassmorphismTransparency * 1.5f else 1.0f))
                        }
                    }
                    if (code == 10 && isRealEnterKeyTextCustom) {
                        propEditor.properties["foreground"] = SnyggStaticColorValue(customRealEnterKeyTextColor)
                    } else if (isEnterKeyTextCustom) {
                        propEditor.properties["foreground"] = SnyggStaticColorValue(customEnterKeyTextColor)
                    } else if (code == 32 && isKeyTextCustom) {
                        propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                    }
                }
            }
            
            baseStylesheet = editor.build()
        }
        baseStylesheet
    }

    val snyggTheme = rememberSnyggTheme(stylesheet, assetResolver)
    val windowSpec by windowController.activeWindowSpec.collectAsState()
    val fontScale by remember { derivedStateOf { windowSpec.fontScale } }

    val state by keyboardManager.activeState.collectAsState()
    val attributes = mapOf(
        FlorisImeUi.Attr.Mode to state.keyboardMode.toString(),
        FlorisImeUi.Attr.ShiftState to state.inputShiftState.toString(),
    )

    MaterialTheme {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle.Default,
            LocalMalangConfig provides malangConfig,
        ) {
            ProvideSnyggTheme(
                snyggTheme = snyggTheme,
                dynamicAccentColor = accentColor,
                fontSizeMultiplier = fontScale,
                assetResolver = assetResolver,
                rootAttributes = attributes,
                content = content,
                materialYouFlags = activeThemeInfo.config.materialYouFlags
            )
        }
    }
}
