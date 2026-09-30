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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.MalangChoiceDialog
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSliderRow
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangValueDialogRow
import dev.malangkey.ime.keyboard.SpaceBarMode
import dev.malangkey.ime.text.key.KeyHintMode
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

private val HintModeEntries = listOf(
    KeyHintMode.HINT_PRIORITY to "힌트 우선",
    KeyHintMode.ACCENT_PRIORITY to "악센트 우선",
    KeyHintMode.SMART_PRIORITY to "스마트 우선",
)

@Composable
fun KeyboardScreen() = MalangSettingsScreen(title = "키보드 설정", subtitle = "Keyboard Settings") {
    val prefs by FlorisPreferenceStore
    val navController = LocalNavController.current
    val numberRow by prefs.keyboard.numberRow.collectAsState()

    MalangSettingsSection(
        title = "키보드 언어 및 레이아웃",
        items = listOf(
            {
                MalangNavRow(
                    title = "키보드 언어 및 레이아웃",
                    summary = "시스템에 추가할 언어와 레이아웃을 선택합니다.",
                    onClick = { navController.navigate(Routes.Settings.KeyboardSelection) },
                )
            },
            {
                MalangNavRow(
                    title = "내 키보드 순서",
                    summary = "사용 중인 키보드를 끌어서 전환 순서를 정합니다.",
                    onClick = { navController.navigate(Routes.Settings.KeyboardOrder) },
                )
            },
            {
                MalangNavRow(
                    title = "입력 및 수정",
                    summary = "자동 교정, 대문자 자동 변환 등 입력 방식을 설정합니다.",
                    onClick = { navController.navigate(Routes.Settings.Typing) },
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.numberRow,
                    title = "숫자 행 표시",
                    summary = "키보드 상단에 숫자 키 행을 표시합니다.",
                )
            },
            {
                HintModeRow(
                    title = "힌트 숫자 행 모드",
                    enabledPref = prefs.keyboard.hintedNumberRowEnabled,
                    modePref = prefs.keyboard.hintedNumberRowMode,
                    enabled = !numberRow,
                )
            },
            {
                HintModeRow(
                    title = "힌트 기호 모드",
                    enabledPref = prefs.keyboard.hintedSymbolsEnabled,
                    modePref = prefs.keyboard.hintedSymbolsMode,
                )
            },
            {
                MalangChoiceRow(
                    prefs.keyboard.spaceBarMode,
                    title = "스페이스바 표시 모드",
                    entries = listOf(
                        SpaceBarMode.NOTHING to "빈 공간",
                        SpaceBarMode.CURRENT_LANGUAGE to "현재 언어 표시",
                        SpaceBarMode.SPACE_BAR_KEY to "스페이스바 텍스트 표시",
                    ),
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "레이아웃 및 크기",
        items = listOf(
            { MalangSliderRow(prefs.keyboard.heightFactorPortrait, "키보드 높이 (세로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.heightFactorLandscape, "키보드 높이 (가로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.fontSizeMultiplierPortrait, "키 글자 크기 (세로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.fontSizeMultiplierLandscape, "키 글자 크기 (가로 화면)", min = 50, max = 150) },
            { MalangSliderRow(prefs.keyboard.keySpacingHorizontal, "키 가로 간격", min = 0, max = 200, step = 5) },
            { MalangSliderRow(prefs.keyboard.keySpacingVertical, "키 세로 간격", min = 0, max = 200, step = 5) },
        ),
    )

    MalangSettingsSection(
        title = "폴더블·분리 자판",
        items = listOf(
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

    MalangSettingsSection(
        title = "기타 설정",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.keyboard.popupEnabled,
                    title = "키 팝업 활성화",
                    summary = "키를 누를 때 팝업을 표시합니다.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.mergeHintPopupsEnabled,
                    title = "힌트 팝업 병합",
                    summary = "힌트와 키 팝업을 하나로 합칩니다.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.keyboard.longPressDelay,
                    title = "길게 누르기 지연 시간",
                    min = 100,
                    max = 700,
                    step = 10,
                    unit = "ms",
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "백업 및 복원",
        items = listOf(
            {
                MalangNavRow(
                    title = "설정 백업",
                    summary = "키보드·테마 설정을 파일로 저장합니다.",
                    onClick = { navController.navigate(Routes.Settings.Backup) },
                )
            },
            {
                MalangNavRow(
                    title = "설정 복원",
                    summary = "백업 파일에서 설정을 불러옵니다.",
                    onClick = { navController.navigate(Routes.Settings.Restore) },
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "앱 정보",
        items = listOf(
            {
                MalangNavRow(
                    title = "앱 정보 · 라이선스",
                    summary = "버전, 개인정보처리방침, 오픈소스 라이선스",
                    onClick = { navController.navigate(Routes.Settings.About) },
                )
            },
        ),
    )
}

/**
 * 스위치로 켜고 끄며, 행을 누르면 모드를 고르는 다이얼로그가 뜬다.
 * 설명 줄에는 선택된 모드(예: 스마트 우선)를 보여준다.
 */
@Composable
private fun HintModeRow(
    title: String,
    enabledPref: PreferenceData<Boolean>,
    modePref: PreferenceData<KeyHintMode>,
    enabled: Boolean = true,
) {
    val mode by modePref.collectAsState()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    MalangSwitchRow(
        enabledPref,
        title = title,
        summary = HintModeEntries.firstOrNull { it.first == mode }?.second ?: "사용 안 함",
        enabled = enabled,
        onRowClick = { open = true },
    )
    if (open) {
        MalangChoiceDialog(
            title = title,
            entries = HintModeEntries,
            selected = mode,
            onSelect = { scope.launch { modePref.set(it) } },
            onDismiss = { open = false },
        )
    }
}
