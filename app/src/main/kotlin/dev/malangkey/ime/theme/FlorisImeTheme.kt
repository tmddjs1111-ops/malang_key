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

package dev.malangkey.ime.theme

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
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.KeyboardMode
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.window.LocalWindowController
import dev.malangkey.keyboardManager
import dev.malangkey.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggPropertySetEditor
import org.florisboard.lib.snygg.SnyggSinglePropertySetEditor
import org.florisboard.lib.snygg.SnyggAnnotationRule
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggStylesheetEditor
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.ui.ProvideSnyggTheme
import org.florisboard.lib.snygg.ui.rememberSnyggTheme
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import androidx.compose.ui.unit.sp

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

    val keyFontSizeMultiplier by prefs.malang.keyFontSizeMultiplier.collectAsState()
    val keyHintFontSizeMultiplier by prefs.malang.keyHintFontSizeMultiplier.collectAsState()
    val keyBorderThickness by prefs.malang.keyBorderThickness.collectAsState()
    val keyBorderOpacity by prefs.malang.keyBorderOpacity.collectAsState()
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
        keyFontSizeMultiplier,
        keyHintFontSizeMultiplier,
        keyBorderThickness,
        keyBorderOpacity,
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
        // 글자·힌트 크기는 자판 종류(쿼티/격자)마다 달라서 TextKeyboardLayout에서 키마다 적용한다.
        val isKeyLayoutCustom = keyBorderThickness > 0
        
        if (isKeyLayoutCustom || isKeyboardBgCustom || isKeyBgCustom || isKeyTextCustom || isEnterKeyBgCustom || isEnterKeyTextCustom || isRealEnterKeyBgCustom || isRealEnterKeyTextCustom || (isCustomThemeSelected && keyCornerRadius != 6) || isFontCustom || (isCustomThemeSelected && isGlassmorphismEnabled) || (isCustomThemeSelected && isNeumorphismEnabled) || (isCustomThemeSelected && squircleShapeEnabled)) {
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
                if (keyBorderThickness > 0) {
                    propEditor.properties["border-width"] = org.florisboard.lib.snygg.value.SnyggDpSizeValue(keyBorderThickness.toFloat().dp)
                    propEditor.properties["border-color"] = org.florisboard.lib.snygg.value.SnyggStaticColorValue(Color.White.copy(alpha = keyBorderOpacity / 100f))
                }
                if (isFontCustom) {
                    val rulesWithText = listOf("keyboard", "key", "key-hint", "key-popup-element", "smartbar-action-tile", "smartbar-action-tile-text")
                    for (element in rulesWithText) {
                        val elementRule = SnyggRule.fromOrNull(element)
                        if (elementRule != null) {
                            val propEditor = editor.rules.getOrPut(elementRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                            val typo = dev.malangkey.app.apptheme.getTypographyFor(keyboardFontFamily)
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

            // 파스텔 테마는 천지인 모음키(ㅣ·ㆍ·ㅡ)에 흰 배경과 포인트색 글자를 따로 준다.
            // 사용자가 키 색을 바꿨으면 이 키들도 같은 색을 따라가야 한 키만 튀지 않는다.
            if (isKeyBgCustom || isKeyTextCustom) {
                for (code in listOf(12643, 183, 12641)) {
                    val vowelRule = SnyggRule.fromOrNull("key[code=$code]") ?: continue
                    val propEditor = editor.rules.getOrPut(vowelRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                    if (isKeyBgCustom) {
                        val bg = if (isGlassmorphismEnabled) customKeyBgColor.copy(alpha = glassmorphismTransparency * 1.5f) else customKeyBgColor
                        propEditor.properties["background"] = SnyggStaticColorValue(bg)
                    }
                    if (isKeyTextCustom) {
                        propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
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


            val actionKeyCodes = listOf(
                10, -1, -2, -3, -4, -5, -7, -8, -11, -201, -202, -203, -204, -205, -206, -207, -212, -213, -227, -232, -301, 32, 44, 46,
                KeyCode.JAPANESE_VIEW_NUMERIC,
                KeyCode.JAPANESE_VIEW_SYMBOLS,
                KeyCode.JAPANESE_SPACE,
                KeyCode.JAPANESE_ENTER,
                KeyCode.JAPANESE_CONVERT,
                KeyCode.KANA_SWITCHER,
                KeyCode.KANA_HIRA,
                KeyCode.KANA_KATA,
                KeyCode.KANA_HALF_KATA
            )
            for (code in actionKeyCodes) {
                val actionKeyRule = SnyggRule.fromOrNull("key[code=$code]")
                if (actionKeyRule != null) {
                    val propEditor = editor.rules.getOrPut(actionKeyRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                    val isEnterCode = (code == 10 || code == KeyCode.JAPANESE_ENTER)
                    // The wide space bar is a normal keycap, while the compact Japanese
                    // 20-key space button is a side/action key.
                    val isSpaceCode = code == KeyCode.SPACE

                    if (isEnterCode) {
                        if (isRealEnterKeyBgCustom) {
                            propEditor.properties["background"] = SnyggStaticColorValue(customRealEnterKeyBgColor)
                        } else if (isEnterKeyBgCustom) {
                            propEditor.properties["background"] = SnyggStaticColorValue(customEnterKeyBgColor)
                        } else if (isCustomThemeSelected) {
                            propEditor.properties["background"] = SnyggStaticColorValue(Color(0xFF311D18))
                        }
                    } else if (isSpaceCode) {
                        if (isKeyBgCustom) {
                            val bg = if (isGlassmorphismEnabled) customKeyBgColor.copy(alpha = glassmorphismTransparency * 1.5f) else customKeyBgColor
                            propEditor.properties["background"] = SnyggStaticColorValue(bg)
                        }
                    } else if (isEnterKeyBgCustom) {
                        propEditor.properties["background"] = SnyggStaticColorValue(customEnterKeyBgColor)
                    }

                    if (isEnterCode) {
                        if (isRealEnterKeyTextCustom) {
                            propEditor.properties["foreground"] = SnyggStaticColorValue(customRealEnterKeyTextColor)
                        } else if (isEnterKeyTextCustom) {
                            propEditor.properties["foreground"] = SnyggStaticColorValue(customEnterKeyTextColor)
                        } else if (isCustomThemeSelected) {
                            propEditor.properties["foreground"] = SnyggStaticColorValue(Color(0xFFFCF5D6))
                        }
                    } else if (isSpaceCode) {
                        if (isKeyTextCustom) {
                            propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                        }
                    } else if (isEnterKeyTextCustom) {
                        propEditor.properties["foreground"] = SnyggStaticColorValue(customEnterKeyTextColor)
                    }
                }
            }
            
            baseStylesheet = editor.build()
        }
        withBaseStyleFallbacks(baseStylesheet, isKeyShapeCustom = squircleShapeEnabled || keyCornerRadius != 6)
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

/**
 * 테마 스타일시트는 기본 스타일과 합쳐지지 않아서, 테마가 적지 않은 규칙은 통째로 빠진다.
 * 그중 눈에 띄게 깨지는 기능키 글자 크기와 길게 누름 팝업을 채운다.
 */
private fun withBaseStyleFallbacks(stylesheet: SnyggStylesheet, isKeyShapeCustom: Boolean): SnyggStylesheet {
    val editor = stylesheet.edit()
    val changedKeys = applyFunctionKeyTextSizes(editor)
    val changedPopup = applyKeyPopupBubbleStyle(editor)
    val changedShape = !isKeyShapeCustom && applyQwertyKeyShape(editor)
    return if (changedKeys || changedPopup || changedShape) editor.build() else stylesheet
}

/**
 * 쿼티·기호 자판은 키 폭이 좁아서, 격자 자판에 맞춘 테마 모서리(20dp 등)를 그대로 쓰면 키가 알약처럼
 * 뭉개진다. 이 자판들의 모서리를 줄이되, 사용자가 모서리 크기·스퀘어클을 정했으면 부르지 않는다.
 */
private fun applyQwertyKeyShape(editor: SnyggStylesheetEditor): Boolean {
    var changed = false
    for (mode in QwertyShapeModes) {
        val rule = SnyggRule.fromOrNull("${FlorisImeUi.Key.elementName}[${FlorisImeUi.Attr.Mode}=`$mode`]") ?: continue
        val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: continue
        if (!propEditor.properties.containsKey("shape")) {
            propEditor.properties["shape"] = SnyggRoundedCornerDpShapeValue(QwertyKeyCorner, QwertyKeyCorner, QwertyKeyCorner, QwertyKeyCorner)
            changed = true
        }
    }
    return changed
}

private val QwertyKeyCorner = 10.dp
private val QwertyShapeModes = listOf(
    KeyboardMode.CHARACTERS,
    KeyboardMode.SYMBOLS,
    KeyboardMode.SYMBOLS2,
    KeyboardMode.NUMERIC_ADVANCED,
).map { it.toString() }

/**
 * 기능키 글자 크기를 기본 스타일과 맞춘다.
 *
 * 테마의 `key` 규칙은 글자 키에 맞춘 큰 크기(20sp 등)를 모든 키에 준다. 기본 스타일은 '1 2 / 3 4',
 * '?123', 스페이스의 언어 이름 같은 기능키를 작게 줄여 두는데, 테마를 넣으면 이 규칙이 빠져서
 * 기능키 글자가 넘치거나 잘린다. 테마가 그 키에 글자 크기를 직접 정하지 않았을 때만 채운다.
 */
private fun applyFunctionKeyTextSizes(editor: SnyggStylesheetEditor): Boolean {
    var changed = false
    for ((codes, fontSize, maxLines) in FunctionKeyTextSizes) {
        for (code in codes) {
            val rule = SnyggRule.fromOrNull("key[code=$code]") ?: continue
            val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: continue
            if (!propEditor.properties.containsKey("font-size")) {
                propEditor.properties["font-size"] = SnyggSpSizeValue(fontSize.sp)
                changed = true
            }
            if (maxLines != null && !propEditor.properties.containsKey("text-max-lines")) {
                propEditor.properties["text-max-lines"] = SnyggTextMaxLinesValue(maxLines)
                changed = true
            }
        }
    }
    return changed
}

/**
 * 길게 누름 팝업을 말풍선처럼 보이게 한다.
 *
 * 말랑키 테마에는 팝업 규칙이 없어서 배경·그림자·선택 표시 없이 글자만 떠 있었다. 테마 색 변수로
 * 테두리 있는 말풍선을 만들고, 고르고 있는 글자는 포인트색으로 채운다. 테마가 직접 정한 값은 두고,
 * 필요한 색 변수가 없는 테마(기본 테마 등)는 건드리지 않는다.
 */
private fun applyKeyPopupBubbleStyle(editor: SnyggStylesheetEditor): Boolean {
    val defines = editor.rules[SnyggAnnotationRule.Defines] as? SnyggSinglePropertySetEditor ?: return false
    if (PopupBubbleVars.any { it !in defines.properties }) return false

    var changed = false
    fun fill(ruleStr: String, vararg props: Pair<String, SnyggValue>) {
        val rule = SnyggRule.fromOrNull(ruleStr) ?: return
        val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return
        for ((name, value) in props) {
            if (!propEditor.properties.containsKey(name)) {
                propEditor.properties[name] = value
                changed = true
            }
        }
    }
    fill(
        FlorisImeUi.KeyPopupBox.elementName,
        "background" to SnyggDefinedVarValue("--bg"),
        "foreground" to SnyggDefinedVarValue("--key-fg"),
        "font-size" to SnyggSpSizeValue(20.sp),
        "shape" to SnyggRoundedCornerDpShapeValue(14.dp, 14.dp, 14.dp, 14.dp),
        "shadow-elevation" to SnyggDpSizeValue(10.dp),
        "border-width" to SnyggDpSizeValue(2.dp),
        "border-color" to SnyggDefinedVarValue("--action-bg"),
    )
    fill(
        FlorisImeUi.KeyPopupElement.elementName,
        "foreground" to SnyggDefinedVarValue("--key-fg"),
        "font-size" to SnyggSpSizeValue(20.sp),
        "shape" to SnyggRoundedCornerDpShapeValue(10.dp, 10.dp, 10.dp, 10.dp),
    )
    fill(
        "${FlorisImeUi.KeyPopupElement.elementName}:focus",
        "background" to SnyggDefinedVarValue("--action-bg"),
        "foreground" to SnyggDefinedVarValue("--action-fg"),
    )
    return changed
}

private val PopupBubbleVars = listOf("--bg", "--key-fg", "--action-bg", "--action-fg")

/** 기본 스타일(FlorisImeThemeBaseStyle)의 기능키 글자 크기. (키 코드, sp, 최대 줄 수) */
private val FunctionKeyTextSizes = listOf(
    Triple(listOf(KeyCode.SPACE, KeyCode.CJK_SPACE), 14, 1),
    Triple(listOf(
        KeyCode.VIEW_CHARACTERS,
        KeyCode.VIEW_SYMBOLS,
        KeyCode.VIEW_SYMBOLS2,
        KeyCode.JAPANESE_CONVERT,
        KeyCode.KANA_SMALL,
    ), 16, 1),
    Triple(listOf(
        KeyCode.VIEW_NUMERIC,
        KeyCode.VIEW_NUMERIC_ADVANCED,
        KeyCode.EXIT_NUMERIC,
        KeyCode.JAPANESE_VIEW_NUMERIC,
        KeyCode.JAPANESE_VIEW_SYMBOLS,
    ), 12, 2),
)
