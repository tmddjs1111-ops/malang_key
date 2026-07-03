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
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import dev.patrickgold.jetpref.datastore.ui.*
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import org.florisboard.lib.compose.*
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import dev.patrickgold.florisboard.subtypeManager

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
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    
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
    } else if (subtypes.isEmpty()) {
        MalangErrorCard(
            modifier = Modifier.padding(16.dp),
            text = "현재 말랑키 키보드 설정이 안되어있습니다\n키보드를 선택해주세요",
            onClick = { navController.navigate(Routes.Settings.Keyboard) },
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
            emoji = "🪄",
            title = "스마트바 설정(상단바)",
            onClick = { navController.navigate(Routes.Settings.Smartbar) }
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
    
    // 1. Featured Hero Card
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(MalangSecondary, MalangPrimary)
                )
            )
            .clickable { navController.navigate(Routes.Settings.Theme) }
            .padding(24.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎨 테마 라이브러리",
                    fontFamily = MalangJuaFont,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "바로가기 ➔",
                    fontFamily = MalangJuaFont,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "키보드 기본 테마를 선택하고 스타일을 조절해보세요.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))



    // Section 3: Sliders & Reset (in one beautiful card)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        val keyCornerRadius by prefs.malang.keyCornerRadius.collectAsState()
        val isGlassmorphismEnabled by prefs.malang.isGlassmorphismEnabled.collectAsState()
        val glassmorphismTransparency by prefs.malang.glassmorphismTransparency.collectAsState()

        // Key Corner Radius Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("키 모서리 둥글기", fontFamily = MalangJuaFont, fontSize = 16.sp, color = MalangText)
            Text("${keyCornerRadius}dp", fontFamily = MalangJuaFont, fontSize = 16.sp, color = MalangSecondary)
        }
        Slider(
            value = keyCornerRadius.toFloat(),
            onValueChange = { scope.launch { prefs.malang.keyCornerRadius.set(it.toInt()) } },
            valueRange = 0f..32f,
            colors = SliderDefaults.colors(
                thumbColor = MalangPrimary,
                activeTrackColor = MalangSecondary,
                inactiveTrackColor = MalangTertiary
            )
        )

        // Glassmorphism Transparency Slider (Visible if Glassmorphism is enabled)
        if (isGlassmorphismEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("유리 투명도 설정", fontFamily = MalangJuaFont, fontSize = 16.sp, color = MalangText)
                Text("${(glassmorphismTransparency * 100).toInt()}%", fontFamily = MalangJuaFont, fontSize = 16.sp, color = MalangSecondary)
            }
            Slider(
                value = glassmorphismTransparency,
                onValueChange = { scope.launch { prefs.malang.glassmorphismTransparency.set(it) } },
                valueRange = 0.1f..0.9f,
                colors = SliderDefaults.colors(
                    thumbColor = MalangPrimary,
                    activeTrackColor = MalangSecondary,
                    inactiveTrackColor = MalangTertiary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFFF0EBE1)))
        Spacer(modifier = Modifier.height(12.dp))

        // Reset Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        prefs.malang.customKeyboardBgColor.set(Color.Unspecified)
                        prefs.malang.customKeyBgColor.set(Color.Unspecified)
                        prefs.malang.customKeyTextColor.set(Color.Unspecified)
                        prefs.malang.customEnterKeyBgColor.set(Color.Unspecified)
                        prefs.malang.customEnterKeyTextColor.set(Color.Unspecified)
                        prefs.malang.keyCornerRadius.set(6)
                        prefs.malang.isGlassmorphismEnabled.set(false)
                        prefs.malang.glassmorphismTransparency.set(0.3f)
                        prefs.malang.isNeumorphismEnabled.set(false)
                        prefs.malang.squircleShapeEnabled.set(false)
                        prefs.malang.malangSoundEnabled.set(false)
                    }
                },
                border = BorderStroke(1.dp, MalangPrimary),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MalangPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔄 커스텀 테마 초기화", fontFamily = MalangJuaFont, fontSize = 14.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Section 4: Aesthetic Toggles Grid
    Text(
        "✨ 감성 스타일링 설정",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = MalangJuaFont,
        fontSize = 18.sp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleCard(
                title = "입체감 (뉴모피즘)",
                description = "키캡에 부드러운 음영 효과",
                emoji = "⛰️",
                pref = prefs.malang.isNeumorphismEnabled,
                modifier = Modifier.weight(1f)
            )
            ToggleCard(
                title = "투명 감성 (글래스)",
                description = "유리 느낌의 반투명 효과",
                emoji = "❄️",
                pref = prefs.malang.isGlassmorphismEnabled,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleCard(
                title = "스쿼클 키캡 모양",
                description = "둥근 애플 스타일 곡선",
                emoji = "⏹️",
                pref = prefs.malang.squircleShapeEnabled,
                modifier = Modifier.weight(1f)
            )
            ToggleCard(
                title = "말랑 효과음 사용",
                description = "키를 누를 때 귀여운 소리",
                emoji = "🔊",
                pref = prefs.malang.malangSoundEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Section 5: Typography & Advanced Features
    Text(
        "📝 서체 및 테마 가져오기",
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = MalangText.copy(alpha = 0.8f),
        fontFamily = MalangJuaFont,
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

        Box(modifier = Modifier.padding(vertical = 4.dp)) {
            ListPreference(
                listPref = prefs.malang.keyboardFontFamily,
                title = "키보드 글꼴 설정",
                modifier = Modifier.padding(horizontal = 8.dp),
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

        Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(horizontal = 24.dp).background(Color(0xFFF0EBE1)))

        MalangMenuItem(
            emoji = "📥",
            title = "공유된 테마 가져오기",
            onClick = { navController.navigate(Routes.Ext.Import(ExtensionImportScreenType.EXT_THEME)) },
            showDivider = false
        )
    }
}


@Composable
fun ToggleCard(
    title: String,
    description: String,
    emoji: String,
    pref: PreferenceData<Boolean>,
    modifier: Modifier = Modifier
) {
    val checked by pref.collectAsState()
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(if (checked) Color(0xFFF5EEDC) else Color.White)
            .clickable {
                scope.launch {
                    pref.set(!checked)
                }
            }
            .border(
                width = 2.dp, 
                color = if (checked) MalangPrimary else Color.Transparent, 
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(emoji, fontSize = 24.sp)
                Switch(
                    checked = checked,
                    onCheckedChange = { value ->
                        scope.launch {
                            pref.set(value)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MalangPrimary,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.LightGray
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = MalangJuaFont,
                fontSize = 15.sp,
                color = MalangText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = MalangText.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )
        }
    }
}

@OptIn(dev.patrickgold.jetpref.material.ui.ExperimentalJetPrefMaterial3Ui::class)
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
