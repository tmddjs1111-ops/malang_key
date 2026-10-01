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
        keyboardFontFamily,
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
        // 湲?먃룻엺???ш린???먰뙋 醫낅쪟(荑쇳떚/寃⑹옄)留덈떎 ?щ씪??TextKeyboardLayout?먯꽌 ?ㅻ쭏???곸슜?쒕떎.
        val isKeyLayoutCustom = keyBorderThickness > 0
        
        if (isKeyLayoutCustom || isKeyboardBgCustom || isKeyBgCustom || isKeyTextCustom || isEnterKeyBgCustom || isEnterKeyTextCustom || isRealEnterKeyBgCustom || isRealEnterKeyTextCustom || isFontCustom) {
            val editor = baseStylesheet.edit()
            
            val rootRule = SnyggRule.fromOrNull("root")
            if (rootRule != null) {
                val propEditor = editor.rules.getOrPut(rootRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = SnyggStaticColorValue(customKeyboardBgColor)
                }
            }
            
            val keyboardRule = SnyggRule.fromOrNull("keyboard")
            if (keyboardRule != null) {
                val propEditor = editor.rules.getOrPut(keyboardRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = SnyggStaticColorValue(customKeyboardBgColor)
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
            }
            
            val smartbarRule = SnyggRule.fromOrNull("smartbar")
            if (smartbarRule != null) {
                val propEditor = editor.rules.getOrPut(smartbarRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyboardBgCustom) {
                    propEditor.properties["background"] = SnyggStaticColorValue(customKeyboardBgColor)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
                }
            }
            
            val keyRule = SnyggRule.fromOrNull("key")
            if (keyRule != null) {
                val propEditor = editor.rules.getOrPut(keyRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                if (isKeyBgCustom) {
                    propEditor.properties["background"] = SnyggStaticColorValue(customKeyBgColor)
                }
                if (isKeyTextCustom) {
                    propEditor.properties["foreground"] = SnyggStaticColorValue(customKeyTextColor)
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

            // ?뚯뒪???뚮쭏??泥쒖???紐⑥쓬???Ｂ룔냽쨌??????諛곌꼍怨??ъ씤?몄깋 湲?먮? ?곕줈 以??
            // ?ъ슜?먭? ???됱쓣 諛붽엥?쇰㈃ ???ㅻ뱾??媛숈? ?됱쓣 ?곕씪媛?????ㅻ쭔 ?吏 ?딅뒗??
            if (isKeyBgCustom || isKeyTextCustom) {
                for (code in listOf(12643, 183, 12641)) {
                    val vowelRule = SnyggRule.fromOrNull("key[code=$code]") ?: continue
                    val propEditor = editor.rules.getOrPut(vowelRule) { SnyggSinglePropertySetEditor() } as SnyggSinglePropertySetEditor
                    if (isKeyBgCustom) {
                        propEditor.properties["background"] = SnyggStaticColorValue(customKeyBgColor)
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
                            propEditor.properties["background"] = SnyggStaticColorValue(customKeyBgColor)
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
 * ?뚮쭏 ?ㅽ??쇱떆?몃뒗 湲곕낯 ?ㅽ??쇨낵 ?⑹퀜吏吏 ?딆븘?? ?뚮쭏媛 ?곸? ?딆? 洹쒖튃? ?듭㎏濡?鍮좎쭊??
 * 洹몄쨷 ?덉뿉 ?꾧쾶 源⑥???湲곕뒫??湲???ш린? 湲멸쾶 ?꾨쫫 ?앹뾽??梨꾩슫??
 */
private fun withBaseStyleFallbacks(stylesheet: SnyggStylesheet): SnyggStylesheet {
    val editor = stylesheet.edit()
    val changedKeys = applyFunctionKeyTextSizes(editor)
    val changedPopup = applyKeyPopupBubbleStyle(editor)
    val changedShape = applyDefaultKeyShape(editor)
    return if (changedKeys || changedPopup || changedShape) editor.build() else stylesheet
}

/**
 * 紐⑤뱺 ?먰뙋??湲곕낯 ??紐⑥꽌由щ? 10dp濡?留욎텣?? ?뚮쭏??20dp 紐⑥꽌由щ뒗 ??씠 醫곸? 荑쇳떚 ?ㅻ? ?뚯빟泥섎읆
 * 萸됯컻怨? 20?ㅼ뿉?쒕룄 ?ㅻⅨ履????뚰듃瑜?怨≪꽑 諛뽰쑝濡?諛?대궡 媛?몃떎.
 */
private fun applyDefaultKeyShape(editor: SnyggStylesheetEditor): Boolean {
    val rule = SnyggRule.fromOrNull(FlorisImeUi.Key.elementName) ?: return false
    val propEditor = editor.rules.getOrPut(rule) { SnyggSinglePropertySetEditor() } as? SnyggSinglePropertySetEditor ?: return false
    propEditor.properties["shape"] = SnyggRoundedCornerDpShapeValue(DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner, DefaultKeyCorner)
    return true
}

private val DefaultKeyCorner = 10.dp

/**
 * 湲곕뒫??湲???ш린瑜?湲곕낯 ?ㅽ??쇨낵 留욎텣??
 *
 * ?뚮쭏??`key` 洹쒖튃? 湲???ㅼ뿉 留욎텣 ???ш린(20sp ??瑜?紐⑤뱺 ?ㅼ뿉 以?? 湲곕낯 ?ㅽ??쇱? '123',
 * '?123', ?ㅽ럹?댁뒪???몄뼱 ?대쫫 媛숈? 湲곕뒫?ㅻ? ?묎쾶 以꾩뿬 ?먮뒗?? ?뚮쭏瑜??ｌ쑝硫???洹쒖튃??鍮좎졇?? * 湲곕뒫??湲?먭? ?섏튂嫄곕굹 ?섎┛?? ?뚮쭏媛 洹??ㅼ뿉 湲???ш린瑜?吏곸젒 ?뺥븯吏 ?딆븯???뚮쭔 梨꾩슫??
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
 * 湲멸쾶 ?꾨쫫 ?앹뾽??留먰뭾?좎쿂??蹂댁씠寃??쒕떎.
 *
 * 留먮옉???뚮쭏?먮뒗 ?앹뾽 洹쒖튃???놁뼱??諛곌꼍쨌洹몃┝?먃룹꽑???쒖떆 ?놁씠 湲?먮쭔 ???덉뿀?? ?뚮쭏 ??蹂?섎줈
 * ?뚮몢由??덈뒗 留먰뭾?좎쓣 留뚮뱾怨? 怨좊Ⅴ怨??덈뒗 湲?먮뒗 ?ъ씤?몄깋?쇰줈 梨꾩슫?? ?뚮쭏媛 吏곸젒 ?뺥븳 媛믪? ?먭퀬,
 * ?꾩슂????蹂?섍? ?녿뒗 ?뚮쭏(湲곕낯 ?뚮쭏 ????嫄대뱶由ъ? ?딅뒗??
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

/** 湲곕낯 ?ㅽ???FlorisImeThemeBaseStyle)??湲곕뒫??湲???ш린. (??肄붾뱶, sp, 理쒕? 以??? */
private val FunctionKeyTextSizes = listOf(
    Triple(listOf(KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.JAPANESE_SPACE), 14, 1),
    Triple(listOf(
        KeyCode.VIEW_CHARACTERS,
        KeyCode.VIEW_SYMBOLS,
        KeyCode.VIEW_SYMBOLS2,
        KeyCode.JAPANESE_CONVERT,
        KeyCode.KANA_SMALL,
        // ?レ옄쨌湲고샇 ?꾪솚 ?ㅻ뒗 紐⑤뱺 ?먰뙋?먯꽌 '123'쨌'?123' ??以꾨줈 ?대떎.
        KeyCode.VIEW_NUMERIC,
        KeyCode.VIEW_NUMERIC_ADVANCED,
        KeyCode.EXIT_NUMERIC,
        KeyCode.JAPANESE_VIEW_NUMERIC,
        KeyCode.JAPANESE_VIEW_SYMBOLS,
    ), 16, 1),
)
