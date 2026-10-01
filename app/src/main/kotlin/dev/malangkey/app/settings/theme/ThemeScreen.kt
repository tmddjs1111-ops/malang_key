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

package dev.malangkey.app.settings.theme

import androidx.compose.ui.platform.LocalContext
import dev.malangkey.app.setup.DefaultPreviewPalette
import dev.malangkey.app.setup.KeyboardPreview
import dev.malangkey.app.setup.loadPreviewPalette
import dev.malangkey.app.setup.rememberCustomPreviewPalette
import dev.malangkey.subtypeManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.R
import dev.malangkey.app.LocalNavController
import dev.malangkey.ime.theme.ThemeManager
import dev.malangkey.app.FlorisPreferenceModel
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.ime.theme.ThemeMode
import dev.malangkey.lib.compose.FlorisScreen
import dev.malangkey.lib.ext.ExtensionComponentName
import dev.malangkey.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.malangkey.app.apptheme.MalangText
import dev.malangkey.app.apptheme.MalangBg
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import androidx.compose.foundation.lazy.grid.GridItemSpan

val MalangJuaFont = JuaFontFamily

data class MalangThemeInfo(
    val extId: String,
    val compId: String,
    val name: String,
    val displayColor: Color,
    val isNight: Boolean = false
)

val malangThemes = listOf(
    MalangThemeInfo("dev.malangkey.pastel", "pink", "파스텔 핑크", Color(0xFFFFD1DC)),
    MalangThemeInfo("dev.malangkey.pastel", "mint", "민트 그린", Color(0xFFC1F0D4)),
    MalangThemeInfo("dev.malangkey.pastel", "yellow", "바나나 옐로우", Color(0xFFFFF59D)),
    MalangThemeInfo("dev.malangkey.pastel", "sky", "스카이 블루", Color(0xFFBCE3FA)),
    MalangThemeInfo("dev.malangkey.pastel", "chocolate", "다크 초콜릿", Color(0xFF4E342E), true),
    MalangThemeInfo("dev.malangkey.colors", "black", "기본 블랙", Color(0xFF212121), true),
    MalangThemeInfo("dev.malangkey.colors", "white", "기본 화이트", Color(0xFFFAFAFA)),
    MalangThemeInfo("dev.malangkey.colors", "custom", "커스텀", Color(0xFFE0E0E0))
)

/** 테마를 키보드에 적용한다. 커스텀이 아닌 테마는 키 모양 효과를 기본값으로 되돌린다. */
suspend fun applyMalangTheme(prefs: FlorisPreferenceModel, themeInfo: MalangThemeInfo) {
    val themeCompName = ExtensionComponentName(themeInfo.extId, themeInfo.compId)
    if (themeInfo.compId == "custom") {
        prefs.theme.mode.set(ThemeMode.ALWAYS_DAY)
        prefs.theme.dayThemeId.set(themeCompName)
        return
    }
    if (themeInfo.isNight) {
        prefs.theme.mode.set(ThemeMode.ALWAYS_NIGHT)
        prefs.theme.nightThemeId.set(themeCompName)
    } else {
        prefs.theme.mode.set(ThemeMode.ALWAYS_DAY)
        prefs.theme.dayThemeId.set(themeCompName)
    }
}

/** 현재 적용된 테마인지 여부. */
fun isMalangThemeSelected(
    themeInfo: MalangThemeInfo,
    mode: ThemeMode,
    dayThemeId: ExtensionComponentName,
    nightThemeId: ExtensionComponentName,
): Boolean {
    val themeCompName = ExtensionComponentName(themeInfo.extId, themeInfo.compId)
    return if (themeInfo.isNight) {
        (mode == ThemeMode.ALWAYS_NIGHT || mode == ThemeMode.FOLLOW_SYSTEM) && nightThemeId == themeCompName
    } else {
        (mode == ThemeMode.ALWAYS_DAY || mode == ThemeMode.FOLLOW_SYSTEM) && dayThemeId == themeCompName
    }
}

val ColorCreamBeige = Color(0xFFFFF5ED)
val ColorDarkChocolate = Color(0xFF4E342E)

@Composable
fun ThemeScreen() = MalangSettingsScreen(title = "키보드 테마", subtitle = "Keyboard Theme", scrollable = false) {
    val florisPrefs by FlorisPreferenceStore
    run {
        val dayThemeId by florisPrefs.theme.dayThemeId.collectAsState()
        val nightThemeId by florisPrefs.theme.nightThemeId.collectAsState()
        val currentMode by florisPrefs.theme.mode.collectAsState()
        val coroutineScope = rememberCoroutineScope()
        // 간단 설정처럼 지금 쓰는 자판에 각 테마 색을 입혀 미리 보여 준다.
        val context = LocalContext.current
        val subtypeManager by context.subtypeManager()
        val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
        val customPalette = rememberCustomPreviewPalette()
        val palettes = remember(context) {
            malangThemes.filter { it.compId != "custom" }
                .associateWith { loadPreviewPalette(context, it.extId, it.compId) ?: DefaultPreviewPalette }
        }

        run {
            run {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(malangThemes) { themeInfo ->
                        val isSelected = isMalangThemeSelected(themeInfo, currentMode, dayThemeId, nightThemeId)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isSelected) MalangSettingsSection else MalangSettingsCard)
                                .clickable {
                                    coroutineScope.launch { applyMalangTheme(florisPrefs, themeInfo) }
                                }
                                .border(1.dp, if (isSelected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(24.dp))
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = themeInfo.name,
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MalangSettingsCard else MalangSettingsTitle
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MalangSettingsCard,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            KeyboardPreview(
                                subtype = activeSubtype,
                                palette = if (themeInfo.compId == "custom") customPalette else palettes.getValue(themeInfo),
                                height = 96.dp,
                            )
                        }
                    }
                    
                    val isCustomThemeSelected = dayThemeId.componentId == "custom"
                    if (isCustomThemeSelected) {
                        item(span = { GridItemSpan(2) }) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "커스텀 색상",
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                    color = MalangSettingsSection,
                                    fontFamily = MalangJuaFont,
                                    fontSize = 23.sp
                                )
                            }
                        }
    
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ColorSwatchCard(
                                    title = "배경 색상",
                                    pref = florisPrefs.malang.customKeyboardBgColor,
                                    modifier = Modifier.weight(1f)
                                )
                                ColorSwatchCard(
                                    title = "키캡 색상",
                                    pref = florisPrefs.malang.customKeyBgColor,
                                    modifier = Modifier.weight(1f)
                                )
                                ColorSwatchCard(
                                    title = "글자 색상",
                                    pref = florisPrefs.malang.customKeyTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
    
                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ColorSwatchCard(
                                    title = "특수 키캡",
                                    pref = florisPrefs.malang.customEnterKeyBgColor,
                                    modifier = Modifier.weight(1f)
                                )
                                ColorSwatchCard(
                                    title = "특수 글자",
                                    pref = florisPrefs.malang.customEnterKeyTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ColorSwatchCard(
                                    title = "엔터 키 색상",
                                    pref = florisPrefs.malang.customRealEnterKeyBgColor,
                                    modifier = Modifier.weight(1f)
                                )
                                ColorSwatchCard(
                                    title = "엔터 글자 색상",
                                    pref = florisPrefs.malang.customRealEnterKeyTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item(span = { GridItemSpan(2) }) {
                            MalangButton(
                                "기본 커스텀 색으로 되돌리기",
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                primary = false,
                                onClick = {
                                    coroutineScope.launch {
                                        florisPrefs.malang.customKeyboardBgColor.set(Color(0xFFFCF5D6))
                                        florisPrefs.malang.customKeyBgColor.set(Color(0xFFFCF5D6))
                                        florisPrefs.malang.customKeyTextColor.set(Color(0xFF311D18))
                                        florisPrefs.malang.customEnterKeyBgColor.set(Color(0xFF311D18))
                                        florisPrefs.malang.customEnterKeyTextColor.set(Color(0xFFFCF5D6))
                                        florisPrefs.malang.customRealEnterKeyBgColor.set(Color(0xFF5D4037))
                                        florisPrefs.malang.customRealEnterKeyTextColor.set(Color(0xFFFFFFFF))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(dev.patrickgold.jetpref.material.ui.ExperimentalJetPrefMaterial3Ui::class)
@Composable
fun ColorSwatchCard(
    title: String,
    pref: PreferenceData<Color>,
    modifier: Modifier = Modifier
) {
    val color by pref.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MalangSettingsCard)
            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
            .clickable { showDialog = true }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (color == Color.Unspecified) Color(0xFFEEEEEE) else color)
                    .border(2.dp, MalangBg, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (color == Color.Unspecified) {
                    Text("🎨", fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontFamily = MalangJuaFont,
                fontSize = 14.sp,
                color = MalangText,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    if (showDialog) {
        var selectedColor by remember { mutableStateOf(if (color == Color.Unspecified) Color.White else color) }
        val colorPickerState = rememberJetPrefColorPickerState(initColor = selectedColor)
        JetPrefAlertDialog(
            title = title,
            confirmLabel = "선택",
            onConfirm = {
                scope.launch {
                    pref.set(selectedColor)
                }
                showDialog = false
            },
            dismissLabel = "취소",
            onDismiss = { showDialog = false }
        ) {
            JetPrefColorPicker(
                state = colorPickerState,
                onColorChange = { selectedColor = it }
            )
        }
    }
}
