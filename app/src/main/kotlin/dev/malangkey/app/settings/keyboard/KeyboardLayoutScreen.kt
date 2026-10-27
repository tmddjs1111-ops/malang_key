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

package dev.malangkey.app.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSliderRow
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.ime.keyboard.WideScreenCondition
import dev.patrickgold.jetpref.datastore.model.collectAsState

/** 천지인 등으로 치다가 폴더블을 펼치거나 가로로 돌렸을 때 바꿔 쓸 한글 자판. */
private val WideScreenKoreanLayoutEntries = listOf(
    "" to "바꾸지 않음",
    "korean" to "두벌식 (쿼티)",
    "korean_cheonjiin_16" to "천지인",
    "korean_sky_vega" to "연타형",
    "korean_naratgul" to "획추가형",
)

/** 키보드 설정 > 레이아웃: 높이·글자 크기·키 간격과 폴더블·가로 화면 자판. */
@Composable
fun KeyboardLayoutScreen() = MalangSettingsScreen(title = "레이아웃", subtitle = "Layout") {
    val prefs by FlorisPreferenceStore
    val wideScreenKoreanLayout by prefs.keyboard.wideScreenKoreanLayout.collectAsState()

    MalangSettingsSection(
        title = "레이아웃 및 크기",
        items = listOf(
            { MalangSliderRow(prefs.keyboard.heightFactorPortrait, "키보드 높이 (세로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.heightFactorLandscape, "키보드 높이 (가로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.fontSizeMultiplierPortrait, "전체 글자 배율 (세로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.fontSizeMultiplierLandscape, "전체 글자 배율 (가로 화면)", min = 50, max = 150) },
        ),
    )

    MalangSettingsSection(
        title = "쿼티 자판 (두벌식·영어)",
        items = listOf(
            { MalangSliderRow(prefs.malang.keyFontSizeMultiplier, "글자 크기", min = 50, max = 150) },
            { MalangSliderRow(prefs.malang.keyHintFontSizeMultiplier, "힌트 크기 (숫자·기호)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.keySpacingHorizontal, "키 가로 간격", min = 0, max = 200, step = 5) },
            { MalangSliderRow(prefs.keyboard.keySpacingVertical, "키 세로 간격", min = 0, max = 200, step = 5) },
        ),
    )

    MalangSettingsSection(
        title = "격자 자판 (천지인·연타형·20키)",
        items = listOf(
            { MalangSliderRow(prefs.malang.gridKeyFontSizeMultiplier, "글자 크기", min = 50, max = 150) },
            { MalangSliderRow(prefs.malang.gridKeyHintFontSizeMultiplier, "힌트 크기 (숫자·기호)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.gridKeySpacingHorizontal, "키 가로 간격", min = 0, max = 200, step = 5) },
            { MalangSliderRow(prefs.keyboard.gridKeySpacingVertical, "키 세로 간격", min = 0, max = 200, step = 5) },
        ),
    )

    MalangSettingsSection(
        title = "폴더블·가로 화면",
        items = listOf(
            {
                MalangChoiceRow(
                    prefs.keyboard.wideScreenKoreanLayout,
                    title = "펼쳤을 때 한글 자판",
                    entries = WideScreenKoreanLayoutEntries,
                )
            },
            {
                MalangChoiceRow(
                    prefs.keyboard.wideScreenCondition,
                    title = "바꿔 쓸 때",
                    entries = listOf(
                        WideScreenCondition.UNFOLDED_OR_LANDSCAPE to "펼쳤을 때·가로 화면일 때",
                        WideScreenCondition.UNFOLDED_ONLY to "펼쳤을 때만",
                        WideScreenCondition.LANDSCAPE_ONLY to "가로 화면일 때만",
                    ),
                    enabled = wideScreenKoreanLayout.isNotEmpty(),
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.splitWhenUnfolded,
                    title = "펼쳤을 때 자판 나누기",
                    summary = "폴더블 폰을 펼치면 쿼티 자판을 가운데에서 나눠 양손으로 치기 쉽게 합니다. 태블릿에도 적용돼요.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.splitWhenFolded,
                    title = "접었을 때도 나누기",
                    summary = "일반 폰 화면에서도 자판을 나눕니다.",
                )
            },
            { MalangSliderRow(prefs.keyboard.splitGapPercent, "가운데 간격", min = 10, max = 35) },
        ),
    )
}
