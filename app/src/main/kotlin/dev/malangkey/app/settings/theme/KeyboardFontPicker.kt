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

package dev.malangkey.app.settings.theme

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.getTypographyFor
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/** 키보드 글꼴 목록 (설정값, 화면 이름). 테마 탭과 간단 설정이 같이 쓴다. */
val KeyboardFontEntries = listOf(
    "jua" to "주아체",
    "pretendard" to "프리텐다드",
    "noto_sans" to "노토 산스 KR",
    "nanum_gothic" to "나눔고딕",
    "nanum_myeongjo" to "나눔명조",
    "gmarket_sans" to "고운돋움",
    "handwriting" to "나눔손글씨 펜",
    "tuntun" to "감자꽃",
    "tmon" to "검은고딕",
    "system" to "시스템 기본",
)

/** 글꼴 이름을 그 글꼴로 직접 써서 보여 주는 고르기 칸. 누르면 바로 바뀐다. */
@Composable
fun KeyboardFontPicker() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val selected by prefs.malang.keyboardFontFamily.collectAsState()

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("키보드 글꼴", color = MalangSettingsTitle, fontSize = 16.sp)
        Text("키에 보이는 글자 모양이에요.", color = MalangSettingsSummary, fontSize = 13.sp)
        for (pair in KeyboardFontEntries.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((id, label) in pair) {
                    val isSelected = id == selected
                    val fontFamily = if (id == "system") null else getTypographyFor(id).bodyLarge.fontFamily
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MalangSettingsSection else Color.Transparent)
                            .border(1.dp, if (isSelected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(12.dp))
                            .clickable { scope.launch { prefs.malang.keyboardFontFamily.set(id) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) MalangSettingsCard else MalangSettingsTitle,
                            fontSize = 17.sp,
                            fontFamily = fontFamily,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
