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

package dev.patrickgold.florisboard.app.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.apptheme.*
import dev.patrickgold.florisboard.app.ext.ExtensionImportScreenType
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.florisboard.lib.util.InputMethodUtils
import androidx.compose.foundation.border
import dev.patrickgold.jetpref.datastore.ui.*
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import org.florisboard.lib.compose.*
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

val MalangJuaFont = FontFamily(Font(R.font.jua))

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun HomeScreen() = FlorisScreen {

    title = "" 
    navigationIconVisible = false
    topBarVisible = false
    previewFieldVisible = false
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    content {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MalangBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 80.dp, bottom = 150.dp)
            ) {


                if (selectedTabIndex == 0) {
                    SettingsTabContent(navController, context)
                } else {
                    ThemeTabContent(navController)
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MalangBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨ Malang Key ✨",
                    fontFamily = MalangJuaFont,
                    fontSize = 24.sp,
                    color = MalangText,
                    fontWeight = FontWeight.Normal // Jua is already bold
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Floating Test Field Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 96.dp) // Float above the nav bar
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    TextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("⌨️ 텍스트를 입력해 키보드 테스트...", color = Color.Gray, fontSize = 16.sp, fontFamily = MalangJuaFont) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                // Bottom Navigation Toggle
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(MalangBg)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(MalangNavBg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (selectedTabIndex == 0) Color.White else Color.Transparent)
                                .clickable { selectedTabIndex = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("설정", color = MalangText, fontSize = 18.sp, fontFamily = MalangJuaFont)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (selectedTabIndex == 1) Color.White else Color.Transparent)
                                .clickable { selectedTabIndex = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("테마", color = MalangText, fontSize = 18.sp, fontFamily = MalangJuaFont)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTabContent(
    navController: androidx.navigation.NavController,
    context: android.content.Context
) {
    val isFlorisBoardEnabled by dev.patrickgold.florisboard.lib.util.InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val isFlorisBoardSelected by dev.patrickgold.florisboard.lib.util.InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
    
    if (!isFlorisBoardEnabled) {
        MalangErrorCard(
            modifier = Modifier.padding(16.dp),
            text = stringRes(R.string.settings__home__ime_not_enabled),
            onClick = { dev.patrickgold.florisboard.lib.util.InputMethodUtils.showImeEnablerActivity(context) },
        )
    } else if (!isFlorisBoardSelected) {
        MalangWarningCard(
            modifier = Modifier.padding(16.dp),
            text = stringRes(R.string.settings__home__ime_not_selected),
            onClick = { dev.patrickgold.florisboard.lib.util.InputMethodUtils.showImePicker(context) },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        MalangMenuItem(
            iconId = R.drawable.ic_home_keyboard,
            title = "키보드 선택",
            onClick = { navController.navigate(Routes.Settings.Keyboard) }
        )
        MalangMenuItem(
            emoji = "📐",
            title = "키보드 레이아웃",
            onClick = { navController.navigate(Routes.Settings.Layout) }
        )
        MalangMenuItem(
            iconId = R.drawable.ic_home_sound_vibration,
            title = "소리 및 진동",
            onClick = { navController.navigate(Routes.Settings.InputFeedback) }
        )
        MalangMenuItem(
            emoji = "📋",
            title = "클립보드",
            onClick = { navController.navigate(Routes.Settings.Clipboard) },
            showDivider = false
        )
    }
        
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "고급 설정",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = FontFamily(Font(R.font.jua)),
        fontSize = 18.sp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        MalangMenuItem(
            iconId = R.drawable.ic_home_quick_settings,
            title = "입력 및 수정",
            onClick = { navController.navigate(Routes.Settings.Typing) }
        )
        MalangMenuItem(
            emoji = "✍️",
            title = "제스쳐",
            onClick = { navController.navigate(Routes.Settings.Gestures) }
        )
        MalangMenuItem(
            emoji = "😁",
            title = "이모티콘",
            onClick = { navController.navigate(Routes.Settings.Media) },
            showDivider = false
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
private fun ThemeTabContent(
    navController: androidx.navigation.NavController
) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    
    Text(
        "1. 테마 (차후 업데이트 진행할꺼임)",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = FontFamily(Font(R.font.jua)),
        fontSize = 18.sp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        MalangMenuItem(
            emoji = "🎨",
            title = "테마 관리",
            onClick = { navController.navigate(Routes.Settings.Theme) },
            showDivider = false
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "2. 키보드 색상 설정",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = FontFamily(Font(R.font.jua)),
        fontSize = 18.sp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {

        MalangColorPreference(
            pref = prefs.malang.customKeyboardBgColor,
            title = "키보드 배경 색상",
            icon = Icons.Default.FormatColorFill
        )

        MalangColorPreference(
            pref = prefs.malang.customKeyBgColor,
            title = "키 배경 색상",
            icon = Icons.Default.Layers
        )

        MalangColorPreference(
            pref = prefs.malang.customKeyTextColor,
            title = "키 글자 색상",
            icon = Icons.Default.TextFields
        )

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            DialogSliderPreference(
                prefs.malang.keyCornerRadius,
                title = "키 모서리 둥글기",
                valueLabel = { "${it}dp" },
                min = 0,
                max = 32,
                stepIncrement = 1,
            )
        }
        
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp).background(Color(0xFFF0EBE1)))

        MalangMenuItem(
            emoji = "🔄",
            title = "커스텀 테마 초기화",
            onClick = {
                scope.launch {
                    prefs.malang.customKeyboardBgColor.set(Color.Unspecified)
                    prefs.malang.customKeyBgColor.set(Color.Unspecified)
                    prefs.malang.customKeyTextColor.set(Color.Unspecified)
                    prefs.malang.keyCornerRadius.set(8)
                    prefs.malang.isGlassmorphismEnabled.set(false)
                    prefs.malang.glassmorphismTransparency.set(0.3f)
                    prefs.malang.isNeumorphismEnabled.set(false)
                    prefs.malang.squircleShapeEnabled.set(false)
                    prefs.malang.malangSoundEnabled.set(false)
                }
            },
            showDivider = false
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        "3. 감성 꾸미기",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = FontFamily(Font(R.font.jua)),
        fontSize = 18.sp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {
        MalangMenuItem(
            emoji = "✨",
            title = "고급 테마 설정",
            onClick = { navController.navigate(Routes.Settings.AdvancedTheme) },
            showDivider = false
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
    ) {    Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp).background(Color(0xFFF0EBE1)))

        Box(modifier = Modifier.padding(vertical = 4.dp)) {
            ListPreference(
                listPref = prefs.appFontFamily,
                title = "시스템 글꼴 설정",
                entries = listPrefEntries {
                    entry("system", "시스템 기본")
                    entry("pretendard", "프리텐다드 (추천)")
                    entry("noto_sans", "노토산스")
                    entry("nanum_gothic", "나눔고딕")
                    entry("nanum_myeongjo", "나눔명조")
                    entry("jua", "배민 주아체")
                    entry("gmarket_sans", "G마켓 산스")
                    entry("handwriting", "색연필 (교보손글씨)")
                    entry("tuntun", "강원교육튼튼체")
                    entry("tmon", "티몬 몬소리체")
                }
            )
        }

        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(start = 24.dp, end = 24.dp).background(Color(0xFFF0EBE1)))

        MalangMenuItem(
            emoji = "📥",
            title = "테마 가져오기",
            onClick = { navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_THEME)) },
            showDivider = false
        )
    }
}

@Composable
fun MalangColorPreference(
    pref: PreferenceData<Color>,
    title: String,
    icon: ImageVector
) {
    val color by pref.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { showDialog = true }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MalangText,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            color = MalangText,
            fontSize = 18.sp,
            fontFamily = MalangJuaFont,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(2.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(if (color == Color.Unspecified) Color.LightGray else color)
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        )
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

@Composable
fun MalangMenuItem(
    iconId: Int? = null,
    icon: ImageVector? = null,
    emoji: String? = null,
    title: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconId != null) {
                    Image(
                        painter = painterResource(id = iconId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else if (emoji != null) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MalangText,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = title,
                color = MalangText,
                fontSize = 18.sp,
                fontFamily = MalangJuaFont
            )
        }
        if (showDivider) {
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(start = 84.dp, end = 24.dp).background(Color(0xFFF0EBE1)))
        }
    }
}

@Composable
fun MalangWarningCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFF4CE))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFFB020),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color(0xFF6B5E43),
            fontSize = 16.sp,
            fontFamily = MalangJuaFont,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun MalangErrorCard(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFFFE5E5))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFFF4D4D),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = Color(0xFF803333),
            fontSize = 16.sp,
            fontFamily = MalangJuaFont,
            lineHeight = 22.sp
        )
    }
}
