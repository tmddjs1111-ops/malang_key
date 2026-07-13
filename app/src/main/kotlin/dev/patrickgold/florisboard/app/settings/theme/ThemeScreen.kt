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

package dev.patrickgold.florisboard.app.settings.theme

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
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.ime.theme.ThemeManager
import dev.patrickgold.florisboard.ime.theme.ThemeMode
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.ext.ExtensionComponentName
import dev.patrickgold.florisboard.themeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import dev.patrickgold.florisboard.app.apptheme.MalangText
import dev.patrickgold.florisboard.app.apptheme.MalangBg
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import androidx.compose.foundation.lazy.grid.GridItemSpan

val MalangJuaFont = FontFamily(Font(R.font.jua))

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

val ColorCreamBeige = Color(0xFFFFF5ED)
val ColorDarkChocolate = Color(0xFF4E342E)

@Composable
fun ThemeScreen() = FlorisScreen {
    title = "키보드 테마 선택"
    previewFieldVisible = true
    scrollable = false

    content {
        val florisPrefs = prefs
        val dayThemeId by florisPrefs.theme.dayThemeId.collectAsState()
        val nightThemeId by florisPrefs.theme.nightThemeId.collectAsState()
        val currentMode by florisPrefs.theme.mode.collectAsState()
        val coroutineScope = rememberCoroutineScope()

        // 덮어쓰는 전체 배경 (Cream Beige)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ColorCreamBeige
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "원하는 디자인을 선택하세요! 🎨",
                    color = ColorDarkChocolate,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(malangThemes) { themeInfo ->
                        val themeCompName = ExtensionComponentName(themeInfo.extId, themeInfo.compId)
                        // 테마 모드에 따라 선택된 테마 판별
                        val isSelected = if (themeInfo.isNight) {
                            (currentMode == ThemeMode.ALWAYS_NIGHT || currentMode == ThemeMode.FOLLOW_SYSTEM) && nightThemeId == themeCompName
                        } else {
                            (currentMode == ThemeMode.ALWAYS_DAY || currentMode == ThemeMode.FOLLOW_SYSTEM) && dayThemeId == themeCompName
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(30.dp))
                                .background(if (isSelected) ColorDarkChocolate else Color.White)
                                .clickable {
                                    coroutineScope.launch {
                                        if (themeInfo.compId == "custom") {
                                            florisPrefs.theme.mode.set(ThemeMode.ALWAYS_DAY)
                                            florisPrefs.theme.dayThemeId.set(ExtensionComponentName("dev.malangkey.colors", "custom"))
                                        } else {
                                            if (themeInfo.isNight) {
                                                florisPrefs.theme.mode.set(ThemeMode.ALWAYS_NIGHT)
                                                florisPrefs.theme.nightThemeId.set(themeCompName)
                                            } else {
                                                florisPrefs.theme.mode.set(ThemeMode.ALWAYS_DAY)
                                                florisPrefs.theme.dayThemeId.set(themeCompName)
                                            }
                                            
                                            florisPrefs.malang.keyCornerRadius.set(6)
                                            florisPrefs.malang.isGlassmorphismEnabled.set(false)
                                            florisPrefs.malang.glassmorphismTransparency.set(0.3f)
                                            florisPrefs.malang.isNeumorphismEnabled.set(false)
                                            florisPrefs.malang.squircleShapeEnabled.set(false)
                                        }
                                    }
                                }
                                .border(
                                    width = if (isSelected) 3.dp else 2.dp,
                                    color = if (isSelected) ColorDarkChocolate else ColorDarkChocolate.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(30.dp)
                                )
                                .padding(vertical = 24.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(themeInfo.displayColor)
                                    .border(1.dp, ColorDarkChocolate.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (themeInfo.displayColor.red * 0.299 + themeInfo.displayColor.green * 0.587 + themeInfo.displayColor.blue * 0.114 > 0.5) ColorDarkChocolate else Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = themeInfo.name,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                fontSize = 16.sp,
                                color = if (isSelected) Color.White else ColorDarkChocolate
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
                                    "🎨 실시간 키보드 색상 커스텀",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                    color = MalangText.copy(alpha = 0.8f),
                                    fontFamily = MalangJuaFont,
                                    fontSize = 18.sp
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
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
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
