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

package dev.malangkey.app.settings

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
import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
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
import dev.malangkey.R
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.*
import dev.malangkey.app.ext.ExtensionImportScreenType
import dev.malangkey.lib.compose.FlorisScreen
import dev.malangkey.lib.util.InputMethodUtils
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
import dev.malangkey.subtypeManager

val MalangJuaFont = JuaFontFamily

private enum class HomeTab { MAIN, THEME, GAME }

/** 피그마 "Malang Key / Main" 카드 한 장의 내용과 아이콘 배치 (카드 기준 dp). */
private data class MainCard(
    val title: String,
    val subtitles: List<String>,
    @DrawableRes val iconRes: Int,
    val iconWidth: Int,
    val iconHeight: Int,
    val iconTop: Float,
    val iconEnd: Float,
    val onClick: () -> Unit,
    val height: Int = 124,
    val titleSize: Int = 27,
    val titleTop: Int = 21,
)

@Composable
fun HomeScreen() = FlorisScreen {

    title = ""
    navigationIconVisible = false
    topBarVisible = false
    previewFieldVisible = false
    scrollable = false

    val navController = LocalNavController.current
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.MAIN) }

    content {
        Box(modifier = Modifier
            .fillMaxSize()
            .background(MalangCream)
        ) {
            Image(
                painter = painterResource(R.drawable.mk_main_bg_pattern),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.5f),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(top = 24.dp, bottom = 120.dp)
            ) {
                when (selectedTab) {
                    HomeTab.MAIN -> MainTabContent(navController, context)
                    HomeTab.THEME -> ComingSoonTab("테마", "Theme")
                    HomeTab.GAME -> ComingSoonTab("게임", "Game")
                }
            }

            MainBottomNav(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 24.dp),
                onMain = { selectedTab = HomeTab.MAIN },
                onTest = { navController.navigate(Routes.Settings.KeyboardTest) },
                onTheme = { selectedTab = HomeTab.THEME },
                onGame = { selectedTab = HomeTab.GAME },
            )
        }
    }
}

@Composable
private fun MainTabContent(
    navController: androidx.navigation.NavController,
    context: android.content.Context
) {
    val isFlorisBoardEnabled by dev.malangkey.lib.util.InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
    val isFlorisBoardSelected by dev.malangkey.lib.util.InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()

    val warningModifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    if (!isFlorisBoardEnabled) {
        MalangErrorCard(
            modifier = warningModifier,
            text = stringRes(R.string.settings__home__ime_not_enabled),
            onClick = { dev.malangkey.lib.util.InputMethodUtils.showImeEnablerActivity(context) },
        )
    } else if (!isFlorisBoardSelected) {
        MalangWarningCard(
            modifier = warningModifier,
            text = stringRes(R.string.settings__home__ime_not_selected),
            onClick = { dev.malangkey.lib.util.InputMethodUtils.showImePicker(context) },
        )
    } else if (subtypes.isEmpty()) {
        MalangErrorCard(
            modifier = warningModifier,
            text = "현재 말랑키 키보드 설정이 안되어있습니다\n키보드를 선택해주세요",
            onClick = { navController.navigate(Routes.Settings.Keyboard) },
        )
    }

    val cards = listOf(
        MainCard(
            title = "키보드 선택", subtitles = listOf("언어 및 종류 선택", "Keyboard Select"),
            iconRes = R.drawable.mk_main_keyboard, iconWidth = 100, iconHeight = 100, iconTop = 37f, iconEnd = 10.5f,
            onClick = { navController.navigate(Routes.Settings.Keyboard) },
            height = 170, titleSize = 32, titleTop = 27,
        ),
        MainCard(
            title = "소리,진동", subtitles = listOf("Sound & Haptic Feedback"),
            iconRes = R.drawable.mk_main_sound, iconWidth = 124, iconHeight = 124, iconTop = 0f, iconEnd = 0f,
            onClick = { navController.navigate(Routes.Settings.InputFeedback) },
        ),
        MainCard(
            title = "스마트 바", subtitles = listOf("Smart Bar Settings"),
            iconRes = R.drawable.mk_main_smartbar, iconWidth = 100, iconHeight = 51, iconTop = 35.8f, iconEnd = 14.8f,
            onClick = { navController.navigate(Routes.Settings.Smartbar) },
        ),
        MainCard(
            title = "클립 보드", subtitles = listOf("Clipboard"),
            iconRes = R.drawable.mk_main_clipboard, iconWidth = 80, iconHeight = 80, iconTop = 15.5f, iconEnd = 28.8f,
            onClick = { navController.navigate(Routes.Settings.Clipboard) },
        ),
        MainCard(
            title = "제스처", subtitles = listOf("Gesture"),
            iconRes = R.drawable.mk_main_gesture, iconWidth = 90, iconHeight = 90, iconTop = 6.8f, iconEnd = 23.8f,
            onClick = { navController.navigate(Routes.Settings.Gestures) },
        ),
        MainCard(
            title = "이모지", subtitles = listOf("Emoji"),
            iconRes = R.drawable.mk_main_emoji, iconWidth = 80, iconHeight = 80, iconTop = 19.8f, iconEnd = 24.8f,
            onClick = { navController.navigate(Routes.Settings.Media) },
        ),
        MainCard(
            title = "빠른 설정", subtitles = listOf("Quick Settings"),
            iconRes = R.drawable.mk_main_quick, iconWidth = 80, iconHeight = 80, iconTop = 20.8f, iconEnd = 28.8f,
            onClick = { navController.navigate(Routes.Setup.Screen) },
        ),
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cards.forEach { MainMenuCard(it) }

        Text(
            text = "앱 정보 · 오픈소스 라이선스",
            color = MalangDarkCard,
            fontSize = 14.sp,
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { navController.navigate(Routes.Settings.About) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MainMenuCard(card: MainCard) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 300.dp)
            .fillMaxWidth()
            .height(card.height.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MalangDarkCard)
            .clickable(onClick = card.onClick)
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = card.titleTop.dp)) {
            Text(
                text = card.title,
                color = MalangDarkCardTitle,
                fontSize = card.titleSize.sp,
                fontFamily = MalangJuaFont,
            )
            card.subtitles.forEach {
                Text(
                    text = it,
                    color = MalangDarkCardSub,
                    fontSize = 15.sp,
                    fontFamily = MalangJuaFont,
                )
            }
        }
        Image(
            painter = painterResource(card.iconRes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = card.iconTop.dp, end = card.iconEnd.dp)
                .size(card.iconWidth.dp, card.iconHeight.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun MainBottomNav(
    modifier: Modifier,
    onMain: () -> Unit,
    onTest: () -> Unit,
    onTheme: () -> Unit,
    onGame: () -> Unit,
) {
    Row(
        modifier = modifier
            .widthIn(max = 363.dp)
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MalangDarkCard),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainNavItem(R.drawable.mk_main_nav_main, 29.7f, 25.7f, "main", onMain)
        MainNavItem(R.drawable.mk_nav_test, 30.8f, 19.7f, "test", onTest)
        MainNavItem(R.drawable.mk_nav_theme, 29.5f, 22.8f, "theme", onTheme)
        MainNavItem(R.drawable.mk_nav_game, 30.6f, 17.9f, "game", onGame)
    }
}

@Composable
private fun RowScope.MainNavItem(
    @DrawableRes iconRes: Int,
    iconWidth: Float,
    iconHeight: Float,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.height(26.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(iconWidth.dp, iconHeight.dp),
                contentScale = ContentScale.FillBounds,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = MalangCream, fontSize = 10.sp, fontFamily = MalangJuaFont)
    }
}

/** 테마/게임 탭: 차후 업데이트 예정이라 자리만 잡아둔다. */
@Composable
private fun ComingSoonTab(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = MalangDarkCard, fontSize = 32.sp, fontFamily = MalangJuaFont)
        Text(subtitle, color = MalangDarkCard.copy(alpha = 0.6f), fontSize = 15.sp, fontFamily = MalangJuaFont)
        Spacer(modifier = Modifier.height(12.dp))
        Text("준비 중이에요. 곧 업데이트될 예정입니다!", color = MalangDarkCard, fontSize = 16.sp, fontFamily = MalangJuaFont)
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
