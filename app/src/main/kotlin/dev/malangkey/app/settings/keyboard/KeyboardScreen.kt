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
import dev.malangkey.ime.keyboard.GridWideAlignment
import dev.malangkey.ime.keyboard.SpaceBarMode
import dev.malangkey.ime.keyboard.WideScreenCondition
import dev.malangkey.ime.text.key.KeyHintMode
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

private val HintModeEntries = listOf(
    KeyHintMode.HINT_PRIORITY to "힌트 우선",
    KeyHintMode.ACCENT_PRIORITY to "악센트 우선",
    KeyHintMode.SMART_PRIORITY to "스마트 우선",
)

/** 천지인 등으로 치다가 폴더블을 펼치거나 가로로 돌렸을 때 바꿔 쓸 한글 자판. */
private val WideScreenKoreanLayoutEntries = listOf(
    "" to "바꾸지 않음",
    "korean" to "두벌식 (쿼티)",
    "korean_cheonjiin_16" to "천지인",
    "korean_sky_vega" to "연타형",
    "korean_naratgul" to "획추가형",
)

@Composable
fun KeyboardScreen() = MalangSettingsScreen(title = "키보드 설정") {
    val prefs by FlorisPreferenceStore
    val navController = LocalNavController.current
    val numberRow by prefs.keyboard.numberRow.collectAsState()
    val wideScreenKoreanLayout by prefs.keyboard.wideScreenKoreanLayout.collectAsState()
    val gridWideCompact by prefs.keyboard.gridWideCompact.collectAsState()

    MalangSettingsSection(
        title = "키보드 언어 및 레이아웃",
        items = listOf(
            {
                MalangNavRow(
                    title = "키보드 언어 및 레이아웃",
                    onClick = { navController.navigate(Routes.Settings.KeyboardSelection) },
                )
            },
            {
                MalangNavRow(
                    title = "내 키보드 순서",
                    onClick = { navController.navigate(Routes.Settings.KeyboardOrder) },
                )
            },
            {
                MalangNavRow(
                    title = "입력 및 수정",
                    summary = "자동 대문자, 단어 추천, 사용자 사전",
                    onClick = { navController.navigate(Routes.Settings.Typing) },
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.numberRow,
                    title = "숫자 행",
                    summary = "맨 위에 1~0 줄을 보여 줘요.",
                )
            },
            {
                HintModeRow(
                    title = "힌트 숫자 행",
                    enabledPref = prefs.keyboard.hintedNumberRowEnabled,
                    modePref = prefs.keyboard.hintedNumberRowMode,
                    enabled = !numberRow,
                )
            },
            {
                HintModeRow(
                    title = "힌트 기호",
                    enabledPref = prefs.keyboard.hintedSymbolsEnabled,
                    modePref = prefs.keyboard.hintedSymbolsMode,
                )
            },
            {
                MalangChoiceRow(
                    prefs.keyboard.spaceBarMode,
                    title = "스페이스바에 표시",
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
        title = "레이아웃",
        items = listOf(
            {
                MalangNavRow(
                    title = "레이아웃",
                    summary = "키보드·스마트 바 높이, 글자 크기, 키 간격",
                    onClick = { navController.navigate(Routes.Settings.KeyboardLayout) },
                )
            },
        ),
    )

    MalangSettingsSection(
        title = "기타 설정",
        items = listOf(
            {
                MalangSwitchRow(
                    prefs.keyboard.popupEnabled,
                    title = "키 팝업",
                    summary = "누른 글자를 키 위에 크게 보여 줘요.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.mergeHintPopupsEnabled,
                    title = "힌트 팝업 합치기",
                    summary = "힌트가 있는 키를 길게 누르면 비슷한 기호도 함께 보여 줘요. " +
                        "예: ㅂ(1)을 길게 누르면 1과 함께 ¹ ½ ⅓ 도 나와요.",
                )
            },
            {
                MalangValueDialogRow(
                    prefs.keyboard.longPressDelay,
                    title = "길게 누르기 시간",
                    min = 100,
                    max = 700,
                    step = 10,
                    unit = "ms",
                )
            },
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
                    title = "펼쳤을 때 쿼티 자판 나누기",
                    summary = "가운데에서 나눠 양손으로 치기 쉽게 해요. 태블릿에도 적용돼요.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.keyboard.splitWhenFolded,
                    title = "접었을 때도 나누기",
                    summary = "일반 폰 화면에서도 나눠요.",
                )
            },
            { MalangSliderRow(prefs.keyboard.splitGapPercent, "가운데 간격", min = 10, max = 35) },
            {
                MalangSwitchRow(
                    prefs.keyboard.gridWideCompact,
                    title = "격자 자판 좁히기 (천지인·20키)",
                    summary = "펼치거나 가로로 돌리면 한 손 키보드처럼 좁혀서 한쪽에 둬요. " +
                        "빈 쪽의 화살표 버튼으로 옮기고, 넓히기 버튼으로 원래 너비로 돌릴 수 있어요.",
                )
            },
            {
                MalangSliderRow(
                    prefs.keyboard.gridWideWidthPercent,
                    "좁힌 자판 너비",
                    min = 30,
                    max = 90,
                    step = 5,
                    enabled = gridWideCompact,
                )
            },
            {
                MalangChoiceRow(
                    prefs.keyboard.gridWideAlignment,
                    title = "좁힌 자판 위치",
                    entries = listOf(
                        GridWideAlignment.LEFT to "왼쪽",
                        GridWideAlignment.CENTER to "가운데",
                        GridWideAlignment.RIGHT to "오른쪽",
                    ),
                    enabled = gridWideCompact,
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
                    summary = "키보드·테마 설정을 파일로 저장해요.",
                    onClick = { navController.navigate(Routes.Settings.Backup) },
                )
            },
            {
                MalangNavRow(
                    title = "설정 복원",
                    summary = "백업 파일에서 설정을 불러와요.",
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
