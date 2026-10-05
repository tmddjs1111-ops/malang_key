/*
 * Copyright (C) 2026 The MalangKey Contributors
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

package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangRowPaddingH
import dev.malangkey.app.apptheme.MalangRowPaddingV
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.ime.input.KeySoundPlayer
import dev.malangkey.ime.input.KeySoundStyle
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 키 소리 종류 고르기. 칸을 누르면 그 소리로 바뀌고 바로 한 번 들려 준다.
 * 키 소리가 꺼져 있으면 흐리게 보이고 누를 수 없다.
 */
@Composable
fun KeySoundPicker(enabled: Boolean) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected by prefs.inputFeedback.keySoundStyle.collectAsState()
    val player = remember { KeySoundPlayer(context.applicationContext) }
    DisposableEffect(player) { onDispose { player.release() } }

    // 예전 '말랑 효과음' 스위치를 켜 둔 사용자는 키보드를 켜기 전에도 '말랑 뽁'으로 보이게 옮긴다.
    LaunchedEffect(Unit) {
        if (prefs.malang.malangSoundEnabled.get()) {
            prefs.inputFeedback.keySoundStyle.set(KeySoundStyle.MALANG)
            prefs.malang.malangSoundEnabled.set(false)
        }
    }

    fun choose(style: KeySoundStyle) {
        scope.launch {
            prefs.inputFeedback.keySoundStyle.set(style)
            withContext(Dispatchers.IO) { player.load(style) }
            val volume = prefs.inputFeedback.audioVolume.get() / 100f
            player.play(style, KeySoundPlayer.Kind.STANDARD, volume.coerceAtLeast(0.3f))
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = MalangRowPaddingH, vertical = MalangRowPaddingV)
            .alpha(if (enabled) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("소리 종류", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("눌러서 들어보고 고를 수 있어요.", color = MalangSettingsSummary, fontSize = 13.sp)
        for (pair in KeySoundStyle.entries.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (style in pair) {
                    val isSelected = style == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MalangSettingsSection else Color.Transparent)
                            .border(1.dp, if (isSelected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) { choose(style) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = style.label,
                            color = if (isSelected) MalangSettingsCard else MalangSettingsTitle,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
