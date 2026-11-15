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

import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.ime.input.HapticFeel
import dev.malangkey.ime.input.HapticKind
import dev.malangkey.ime.input.playKeyHaptic
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 설정 화면에서 [feel] 진동을 타닥타닥 세 번 울려 타건하는 느낌을 바로 느껴 보게 한다. */
internal suspend fun previewHaptic(vibrator: Vibrator?, feel: HapticFeel, strength: Float) {
    vibrator ?: return
    repeat(3) { i ->
        if (i > 0) delay(110)
        vibrator.playKeyHaptic(feel, strength, HapticKind.KEY)
    }
}

/** 진동 느낌 카드. 누르면 고르면서 바로 그 느낌으로 울린다. (소리 종류 고르기와 같은 방식) */
@Composable
fun HapticFeelPicker(enabled: Boolean, vibrator: Vibrator?) {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val selected by prefs.inputFeedback.hapticFeel.collectAsState()

    fun choose(feel: HapticFeel) {
        scope.launch {
            prefs.inputFeedback.hapticFeel.set(feel)
            previewHaptic(
                vibrator = vibrator,
                feel = feel,
                strength = prefs.inputFeedback.hapticVibrationIntensity.get() / 100f,
            )
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.4f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("진동 느낌", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("눌러서 느껴 보고 고를 수 있어요.", color = MalangSettingsSummary, fontSize = 13.sp)
        for (pair in HapticFeel.entries.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (feel in pair) {
                    val isSelected = feel == selected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MalangSettingsSection else Color.Transparent)
                            .border(1.dp, if (isSelected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(12.dp))
                            .clickable(enabled = enabled) { choose(feel) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = feel.label,
                            color = if (isSelected) MalangSettingsCard else MalangSettingsTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = feel.description,
                            modifier = Modifier.fillMaxWidth(),
                            color = if (isSelected) MalangSettingsCard.copy(alpha = 0.85f) else MalangSettingsSummary,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
