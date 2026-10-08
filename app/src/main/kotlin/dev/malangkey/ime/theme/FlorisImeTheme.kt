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
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.TextStyle
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.window.LocalWindowController
import dev.malangkey.keyboardManager
import dev.malangkey.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.snygg.SnyggRule
import org.florisboard.lib.snygg.SnyggPropertySetEditor
import org.florisboard.lib.snygg.SnyggSinglePropertySetEditor
import org.florisboard.lib.snygg.SnyggAnnotationRule
import org.florisboard.lib.snygg.SnyggElementRule
import org.florisboard.lib.snygg.SnyggSelector
import dev.malangkey.ime.keyboard.KeyboardMode
import org.florisboard.lib.snygg.SnyggStylesheet
import org.florisboard.lib.snygg.SnyggStylesheetEditor
import org.florisboard.lib.snygg.value.SnyggDefinedVarValue
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggPaddingValue
import org.florisboard.lib.snygg.value.SnyggValue
import org.florisboard.lib.snygg.value.SnyggTextMaxLinesValue
import org.florisboard.lib.snygg.ui.ProvideSnyggTheme
import org.florisboard.lib.snygg.ui.rememberSnyggTheme
import org.florisboard.lib.snygg.value.SnyggRoundedCornerDpShapeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue
import org.florisboard.lib.snygg.value.SnyggUriValue
import org.florisboard.lib.snygg.value.SnyggContentScaleValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.florisboard.lib.snygg.value.SnyggCustomFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggGenericFontFamilyValue
import org.florisboard.lib.snygg.value.SnyggSpSizeValue
import androidx.compose.ui.unit.sp

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
    val customBgImageFile by prefs.malang.customBgImageFile.collectAsState()
    val customBgImageDim by prefs.malang.customBgImageDim.collectAsState()
    val customBgImageKeyOpacity by prefs.malang.customBgImageKeyOpacity.collectAsState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bgImageRevision by CustomThemeImage.revision.collectAsState()

    val keyFontSizeMultiplier by prefs.malang.keyFontSizeMultiplier.collectAsState()
    val keyHintFontSizeMultiplier by prefs.malang.keyHintFontSizeMultiplier.collectAsState()
    val keyBorderThickness by prefs.malang.keyBorderThickness.collectAsState()
    val keyBorderOpacity by prefs.malang.keyBorderOpacity.collectAsState()


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
        customBgImageFile,
        customBgImageDim,
        customBgImageKeyOpacity,
        keyboardFontFamily,
        keyFontSizeMultiplier,
        keyHintFontSizeMultiplier,
        keyBorderThickness,
        keyBorderOpacity,
        dayThemeId,
        themeMode,
        isLandscape,
        bgImageRevision,
    ) {
        var baseStylesheet = activeThemeInfo.stylesheet
        val isCustomThemeSelected = dayThemeId.componentId == "custom" && (themeMode == ThemeMode.ALWAYS_DAY || themeMode == ThemeMode.FOLLOW_SYSTEM)
        // 백업 복원 등으로 파일이 없어졌으면 사진 없이 색만 쓴다.
        val hasBgImage = isCustomThemeSelected && customBgImageFile.isNotEmpty() &&
            CustomThemeImage.file(context, customBgImageFile) != null

        // 배경 사진이 있으면 고르지 않은 색도 기본 커스텀 색으로 채운다. 그래야 판과 키를 반투명하게 만들 수 있다.
        fun Color.orImageDefault(default: Color) = if (hasBgImage && this == Color.Unspecified) default else this
        val customKeyboardBgColor = customKeyboardBgColor.orImageDefault(prefs.malang.customKeyboardBgColor.default)
        val customKeyBgColor = customKeyBgColor.orImageDefault(prefs.malang.customKeyBgColor.default)
        val customKeyTextColor = customKeyTextColor.orImageDefault(prefs.malang.customKeyTextColor.default)
        val customEnterKeyBgColor = customEnterKeyBgColor.orImageDefault(prefs.malang.customEnterKeyBgColor.default)
        val customEnterKeyTextColor = customEnterKeyTextColor.orImageDefault(prefs.malang.customEnterKeyTextColor.default)
        // 사진이 있으면 판(자판·상단바)은 비우고, 덮개는 window-inner 한 장으로만 얹는다.
        // 그래야 상단바와 자판 사이에 경계 없이 사진이 이어진다. 키캡은 사진이 비쳐 보이게 투명도를 준다.
        val keyAlpha = if (hasBgImage) customBgImageKeyOpacity / 100f else 1f
        fun boardColor(color: Color) = SnyggStaticColorValue(if (hasBgImage) Color.Transparent else color)
        fun keyColor(color: Color) = SnyggStaticColorValue(color.copy(alpha = color.alpha * keyAlpha))

        val isKeyboardBgCustom = isCustomThemeSelected && customKeyboardBgColor != Color.Unspecified
        val isKeyBgCustom = isCustomThemeSelected && customKeyBgColor != Color.Unspecified
        val isKeyTextCustom = isCustomThemeSelected && customKeyTextColor != Color.Unspecified
        val isEnterKeyBgCustom = isCustomThemeSelected && customEnterKeyBgColor != Color.Unspecified
        val isEnterKeyTextCustom = isCustomThemeSelected && customEnterKeyTextColor != Color.Unspecified
        val isRealEnterKeyBgCustom = isCustomThemeSelected && customRealEnterKeyBgColor != Color.Unspecified
        val isRealEnterKeyTextCustom = isCustomThemeSelected && customRealEnterKeyTextColor != Color.Unspecified
        val isFontCustom = keyboardFontFamily != "system"
        // 글자·힌트 크기는 자판 종류(쿼티/격자)마다 달라서 TextKeyboardLayout에서 키마다 적용한다.
        // 외곽선은 0(없음)일 때도 테마에 있던 외곽선을 지워야 하므로 항상 고친다.
        val isKeyLayoutCustom = true
        
        if (hasBgImage || isKeyLayoutCustom || isKeyboardBgCustom || isKeyBgCustom || isKeyTextCustom || isEnterKeyBgCustom || isEnterKeyTextCustom || isRealEnterKeyBgCustom || isRealEnterKeyTextCustom || isFontCustom) {
            val editor = baseStylesheet.edit()
            
            val rootRule = SnyggRule.fromOrNull("root")
            if (rootRule != null) {
                val propEditor = editor.rules.getOrPut(rootRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = boardColor(customKeyboardBgColor)
                }
            }
            
            val keyboardRule = SnyggRule.fromOrNull("keyboard")
            if (keyboardRule != null) {
                val propEditor = editor.rules.getOrPut(keyboardRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = boardColor(customKeyboardBgColor)
                }
            }
            
            val windowRule = SnyggRule.fromOrNull("window")
            if (windowRule != null) {
                val propEditor = editor.rules.getOrPut(windowRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = SnyggStaticColorValue(customKeyboardBgColor)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
                if (hasBgImage) {
                    propEditor.properties["background-image"] = SnyggUriValue(CustomThemeImage.uriFor(customBgImageFile))
                    // 세로 화면은 가로에 맞춰 크기를 고정해야, 창 높이가 바뀌어도 사진이 확대되거나 움직이지 않는다.
                    // 가로 화면은 키보드가 납작해서 가로에 맞추면 사진 아래쪽만 보이므로 꽉 채워 자른다.
                    val scale = if (isLandscape) ContentScale.Crop else ContentScale.FillWidth
                    propEditor.properties["content-scale"] = SnyggContentScaleValue(scale)
                }
            }
            
            val smartbarRule = SnyggRule.fromOrNull("smartbar")
            if (smartbarRule != null) {
                val propEditor = editor.rules.getOrPut(smartbarRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = boardColor(customKeyboardBgColor)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
            }
            
            val keyRule = SnyggRule.fromOrNull("key")
            if (keyRule != null) {
                val propEditor = editor.rules.getOrPut(keyRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyBgCustom) {
                    propEditor.properties["background"] = keyColor(customKeyBgColor)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
                if (hasBgImage) {
                    // 반투명 키 아래로 그림자가 비치면 지저분해 보인다.
                    propEditor.properties["shadow-elevation"] = SnyggDpSizeValue(0.dp)
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
                            propEditor.properties["background"] = keyColor(customRealEnterKeyBgColor)
                        } else if (isEnterKeyBgCustom) {
                            propEditor.properties["background"] = keyColor(customEnterKeyBgColor)
                        } else if (isCustomThemeSelected) {
                            propEditor.properties["background"] = keyColor(Color(0xFF311D18))
                        }
                    } else if (isSpaceCode) {
                        if (isKeyBgCustom) {
                            propEditor.properties["background"] = keyColor(customKeyBgColor)
                        }
                    } else if (isEnterKeyBgCustom) {
                        propEditor.properties["background"] = keyColor(customEnterKeyBgColor)
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

            if (hasBgImage) {
                // 눌린 키는 테마의 불투명한 색 대신, 그 키 색을 글자색 쪽으로 조금 당기고 조금 더 진하게 칠한다.
                val pressedAlpha = (keyAlpha + 0.25f).coerceAtMost(1f)
                fun pressed(background: Color, foreground: Color) =
                    SnyggStaticColorValue(lerp(background, foreground, 0.25f).copy(alpha = pressedAlpha))
                fun setPressed(selector: String, background: Color, foreground: Color) {
                    val rule = SnyggRule.fromOrNull(selector) ?: return
                    val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return
                    propEditor.properties["background"] = pressed(background, foreground)
                }
                val enterBackground = customRealEnterKeyBgColor.takeIf { isRealEnterKeyBgCustom } ?: customEnterKeyBgColor
                val enterForeground = customRealEnterKeyTextColor.takeIf { isRealEnterKeyTextCustom } ?: customEnterKeyTextColor
                setPressed("key:pressed", customKeyBgColor, customKeyTextColor)
                for (code in actionKeyCodes) {
                    when (code) {
                        10, KeyCode.JAPANESE_ENTER -> setPressed("key[code=$code]:pressed", enterBackground, enterForeground)
                        KeyCode.SPACE -> setPressed("key[code=$code]:pressed", customKeyBgColor, customKeyTextColor)
                        else -> setPressed("key[code=$code]:pressed", customEnterKeyBgColor, customEnterKeyTextColor)
                    }
                }

                applyBgImagePanels(
                    editor = editor,
                    overlay = customKeyboardBgColor.copy(alpha = customKeyboardBgColor.alpha * customBgImageDim / 100f),
                    solidBackground = customKeyboardBgColor,
                    itemBackground = customKeyBgColor.copy(alpha = customKeyBgColor.alpha * keyAlpha),
                    foreground = customKeyTextColor,
                )
            }

            applyGroupedKeyCodeRules(editor)
            applyKeyBorder(editor, keyBorderThickness, keyBorderOpacity)

            baseStylesheet = editor.build()
        }
        withBaseStyleFallbacks(baseStylesheet)
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
private fun withBaseStyleFallbacks(stylesheet: SnyggStylesheet): SnyggStylesheet {
    val editor = stylesheet.edit()
    val changedKeys = applyFunctionKeyTextSizes(editor)
    val changedPopup = applyKeyPopupBubbleStyle(editor)
    val changedMedia = applyEmojiPanelStyle(editor)
    val changedShape = applyDefaultKeyShape(editor)
    return if (changedKeys || changedPopup || changedShape || changedMedia) editor.build() else stylesheet
}

/**
 * 기본 스타일은 여러 키 코드를 한 규칙으로 묶어 둔다 (예: 엔터 `key[code=10,-9717]`). 그 묶음 규칙이
 * 코드 하나짜리 규칙보다 먼저 적용되어, 커스텀 테마에서 코드마다 정한 색이 묻힌다.
 * 묶인 코드들이 모두 같은 색을 받았으면 그 색을 묶음 규칙에도 넣는다.
 */
private fun applyGroupedKeyCodeRules(editor: SnyggStylesheetEditor) {
    fun SnyggElementRule.codes() = attributes[FlorisImeUi.Attr.Code].orEmpty()
    val keyRules = editor.rules.keys.filterIsInstance<SnyggElementRule>()
        .filter { it.elementName == FlorisImeUi.Key.elementName && it.attributes.size == 1 }
    fun singleCodeProps(code: String, selector: SnyggSelector) = keyRules
        .firstOrNull { it.selector == selector && it.codes() == listOf(code) }
        ?.let { editor.rules[it] as? SnyggSinglePropertySetEditor }
    for (rule in keyRules) {
        val codes = rule.codes()
        if (codes.size < 2) continue
        val target = editor.rules[rule] as? SnyggSinglePropertySetEditor ?: continue
        for (property in listOf("background", "foreground")) {
            val values = codes.map { singleCodeProps(it, rule.selector)?.properties?.get(property) }
            val shared = values.firstOrNull() ?: continue
            if (values.all { it == shared }) target.properties[property] = shared
        }
    }
}

/** 자판 자리를 대신하는 화면(이모지·클립보드·편집 등)과 상단바. 배경 사진이 그대로 비치게 비운다. */
private val BgImageTransparentElements = listOf(
    "root",
    "keyboard",
    "smartbar",
    "smartbar-shared-actions-row",
    "smartbar-extended-actions-row",
    "smartbar-candidates-row",
    "smartbar-actions-overflow",
    "media",
    "media-bottom-row",
    "media-emoji-subheader",
    "media-emoji-tab",
    "clipboard-header",
    "clipboard-subheader",
    "clipboard-content",
    "clipboard-filter-row",
    "clipboard-grid",
    "editing",
    "one-handed-panel",
)

/** 자판 위로 올라오는 창과 팝업. 사진 없이 단색으로 둔다. */
private val BgImageSolidElements = listOf(
    "subtype-panel",
    "smartbar-actions-editor",
    "clipboard-item-popup",
    "clipboard-clear-all-dialog",
    "key-popup-box",
    "media-emoji-key-popup-box",
)

/** 자판 위에 놓이는 항목. 키캡처럼 반투명하게 칠한다. */
private val BgImageItemElements = listOf(
    "editing-button",
    "clipboard-item",
    "clipboard-filter-chip",
    "media-bottom-row-button",
)

/**
 * 커스텀 테마에 배경 사진이 있을 때 패널 배경을 정리한다. 사진 위 덮개는 [overlay] 한 장을
 * window-inner에만 깔아서, 상단바·자판·이모지 화면이 바뀌어도 배경이 끊기지 않게 한다.
 */
private fun applyBgImagePanels(
    editor: SnyggStylesheetEditor,
    overlay: Color,
    solidBackground: Color,
    itemBackground: Color,
    foreground: Color,
) {
    fun set(element: String, background: Color) {
        val rule = SnyggRule.fromOrNull(element) ?: return
        val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return
        propEditor.properties["background"] = SnyggStaticColorValue(background)
        propEditor.properties["foreground"] = SnyggStaticColorValue(foreground)
    }
    set("window-inner", overlay)
    BgImageTransparentElements.forEach { set(it, Color.Transparent) }
    BgImageSolidElements.forEach { set(it, solidBackground) }
    BgImageItemElements.forEach { set(it, itemBackground) }
}

/** 커스텀 테마가 배경 사진을 쓰는 중인지. 스타일시트 밖에서 색을 직접 칠하는 화면이 쓴다. */
@Composable
fun rememberIsCustomBgImageActive(): Boolean {
    val prefs by FlorisPreferenceStore
    val dayThemeId by prefs.theme.dayThemeId.collectAsState()
    val themeMode by prefs.theme.mode.collectAsState()
    val bgImageFile by prefs.malang.customBgImageFile.collectAsState()
    val context = LocalContext.current
    val revision by CustomThemeImage.revision.collectAsState()
    val hasFile = remember(bgImageFile, revision) { CustomThemeImage.file(context, bgImageFile) != null }
    return dayThemeId.componentId == "custom" &&
        (themeMode == ThemeMode.ALWAYS_DAY || themeMode == ThemeMode.FOLLOW_SYSTEM) &&
        hasFile
}

/** 외곽선 한 칸(0~10)의 굵기. 격자 자판은 키가 커서 두 배로 둔다. */
internal const val QwertyKeyBorderStepDp = 0.2f
internal const val GridKeyBorderStepDp = 0.4f
private const val KeyBorderTextAlpha = 0.3f

/**
 * 키 외곽선을 테마 대신 사용자 설정으로 정한다. [thickness]가 0이면 테마에 있던 외곽선까지 없앤다.
 *
 * 색은 키마다 자기 글자색의 30%에 [opacity]를 곱한다. 굵기는 쿼티 한 칸 0.2dp, 격자 자판 한 칸
 * 0.4dp이고, 키 사이 간격 쪽으로 넓혀 그리는 건 TextKeyboardLayout이 맡는다.
 */
private fun applyKeyBorder(editor: SnyggStylesheetEditor, thickness: Int, opacity: Int) {
    val defines = editor.rules[SnyggAnnotationRule.Defines] as? SnyggSinglePropertySetEditor
    fun resolveColor(value: SnyggValue?): Color? = when (value) {
        is SnyggStaticColorValue -> value.color
        is SnyggDefinedVarValue -> resolveColor(defines?.properties?.get(value.key))
        else -> null
    }
    for ((rule, set) in editor.rules) {
        if (rule !is SnyggElementRule || rule.elementName != FlorisImeUi.Key.elementName) continue
        val props = (set as? SnyggSinglePropertySetEditor)?.properties ?: continue
        val isBase = rule.attributes.isEmpty() && rule.selector == SnyggSelector.NONE
        props.remove("border-width")
        props.remove("border-color")
        if (thickness <= 0) {
            if (isBase) {
                props["border-width"] = SnyggDpSizeValue(0.dp)
                props["border-color"] = SnyggStaticColorValue(Color.Transparent)
            }
            continue
        }
        // 글자색을 정하지 않은 규칙은 외곽선 색도 정하지 않아, 같은 키의 다른 규칙 색을 그대로 받는다.
        val textColor = resolveColor(props["foreground"]) ?: if (isBase) Color.Black else continue
        props["border-color"] = SnyggStaticColorValue(
            textColor.copy(alpha = textColor.alpha * KeyBorderTextAlpha * opacity / 100f)
        )
        if (isBase) {
            props["border-width"] = SnyggDpSizeValue((thickness * QwertyKeyBorderStepDp).dp)
        }
    }
    if (thickness > 0) {
        val gridRule = SnyggRule.fromOrNull("${FlorisImeUi.Key.elementName}[mode=`${KeyboardMode.GRID_16KEY}`]") ?: return
        val props = editor.rules.getOrPut(gridRule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return
        props.properties["border-width"] = SnyggDpSizeValue((thickness * GridKeyBorderStepDp).dp)
    }
}

/**
 * 모든 자판의 기본 키 모서리를 10dp로 맞춘다. 테마의 20dp 모서리는 폭이 좁은 쿼티 키를 알약처럼
 * 뭉개고, 20키에서도 오른쪽 위 힌트를 곡선 밖으로 밀어내 가렸다.
 */
private fun applyDefaultKeyShape(editor: SnyggStylesheetEditor): Boolean {
    val rule = SnyggRule.fromOrNull(FlorisImeUi.Key.elementName) ?: return false
    val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return false
    propEditor.properties["shape"] = SnyggRoundedCornerDpShapeValue(DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner)
    return true
}

private val DefaultKeyCorner = 10.dp

/**
 * 기능키 글자 크기를 기본 스타일과 맞춘다.
 *
 * 테마의 `key` 규칙은 글자 키에 맞춘 큰 크기(20sp 등)를 모든 키에 준다. 기본 스타일은 '123',
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

/**
 * 이모지 패널에 테마 색을 입힌다. 말랑키 테마에는 이모지 패널 규칙이 없어서 분류 아이콘이 검은색이고,
 * 아래 'ABC'·지우기 버튼은 키 모양 없이 글자만 떠 있었다. 테마가 직접 정한 값은 두고, 필요한 색 변수가
 * 없는 테마는 건드리지 않는다.
 */
private fun applyEmojiPanelStyle(editor: SnyggStylesheetEditor): Boolean {
    val defines = editor.rules[SnyggAnnotationRule.Defines] as? SnyggSinglePropertySetEditor ?: return false
    // 말랑키 테마의 색 변수 이름을 먼저 쓰고, 없으면 기본 스타일(커스텀 테마가 쓰는)의 이름을 쓴다.
    fun pick(vararg names: String) = names.firstOrNull { it in defines.properties }?.let { SnyggDefinedVarValue(it) }
    val keyBg = pick("--key-bg", "--surface") ?: return false
    val keyFg = pick("--key-fg", "--on-surface") ?: return false
    val keyPressed = pick("--key-bg-pressed", "--surface-variant") ?: return false
    val accent = pick("--action-bg", "--primary") ?: return false

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
    val keyShape = SnyggRoundedCornerDpShapeValue(DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner)
    fill(FlorisImeUi.MediaEmojiTab.elementName, "foreground" to keyFg)
    fill("${FlorisImeUi.MediaEmojiTab.elementName}:focus", "foreground" to accent)
    fill(FlorisImeUi.MediaEmojiSubheader.elementName, "foreground" to keyFg)
    fill(
        "${FlorisImeUi.MediaEmojiKey.elementName}:pressed",
        "background" to keyPressed,
        "shape" to keyShape,
    )
    fill(
        FlorisImeUi.MediaBottomRowButton.elementName,
        "background" to keyBg,
        "foreground" to keyFg,
        "shape" to keyShape,
        // 글자에 딱 붙지 않고 키처럼 보이도록 안팎 여백을 준다.
        "padding" to SnyggPaddingValue(androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)),
        "margin" to SnyggPaddingValue(androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 5.dp)),
    )
    fill("${FlorisImeUi.MediaBottomRowButton.elementName}:pressed", "background" to keyPressed)
    return changed
}

/** 기본 스타일(FlorisImeThemeBaseStyle)의 기능키 글자 크기. (키 코드, sp, 최대 줄 수) */
private val FunctionKeyTextSizes = listOf(
    Triple(listOf(KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.JAPANESE_SPACE), 14, 1),
    Triple(listOf(
        KeyCode.VIEW_CHARACTERS,
        KeyCode.VIEW_SYMBOLS,
        KeyCode.VIEW_SYMBOLS2,
        KeyCode.JAPANESE_CONVERT,
        KeyCode.KANA_SMALL,
        // 숫자·기호 전환 키는 모든 자판에서 '123'·'?123' 한 줄로 쓴다.
        KeyCode.VIEW_NUMERIC,
        KeyCode.VIEW_NUMERIC_ADVANCED,
        KeyCode.EXIT_NUMERIC,
        KeyCode.JAPANESE_VIEW_NUMERIC,
        KeyCode.JAPANESE_VIEW_SYMBOLS,
    ), 16, 1),
)
