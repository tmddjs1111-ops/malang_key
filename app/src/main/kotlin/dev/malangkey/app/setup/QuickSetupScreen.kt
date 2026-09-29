package dev.malangkey.app.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangChoiceRow
import dev.malangkey.app.apptheme.MalangDarkCard
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.app.apptheme.MalangSettingsBg
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangSwitchRow
import dev.malangkey.app.apptheme.MalangTestInputBar
import dev.malangkey.app.apptheme.MalangToggle
import dev.malangkey.app.settings.keyboard.keyboardDisplayName
import dev.malangkey.app.settings.smartbar.MalangSlotsEditor
import dev.malangkey.app.settings.theme.applyMalangTheme
import dev.malangkey.app.settings.theme.isMalangThemeSelected
import dev.malangkey.app.settings.theme.malangThemes
import dev.malangkey.ime.clipboard.QuickPhraseTriggerKey
import dev.malangkey.ime.keyboard.LayoutType
import dev.malangkey.ime.smartbar.SmartbarLayout
import dev.malangkey.keyboardManager
import dev.malangkey.lib.compose.FlorisScreen
import dev.malangkey.subtypeManager
import kotlinx.coroutines.launch
import dev.patrickgold.jetpref.datastore.model.collectAsState as collectPrefAsState

/** 키보드를 켜고 고른 뒤 이어지는 간단 설정 단계. */
private enum class QuickStep(val title: String, val subtitle: String) {
    LANGUAGE("언어·자판", "어떤 자판으로 입력할까요?"),
    FEEDBACK("소리·진동", "키를 누를 때의 느낌을 골라주세요."),
    SMARTBAR("스마트 바", "키보드 위 버튼 줄에 넣을 기능이에요."),
    CLIPBOARD("클립보드·상용구", "자주 쓰는 문구를 빠르게 입력해요."),
    THEME("테마", "키보드 색을 골라주세요."),
}

@Composable
fun QuickSetupScreen() = FlorisScreen {
    title = ""
    topBarVisible = false
    navigationIconVisible = false
    previewFieldVisible = false
    scrollable = false

    content {
        val navController = LocalNavController.current
        val scope = rememberCoroutineScope()
        val steps = QuickStep.entries
        var stepIndex by rememberSaveable { mutableIntStateOf(0) }
        val step = steps[stepIndex]
        val isLast = stepIndex == steps.lastIndex

        val finish: () -> Unit = {
            scope.launch { prefs.internal.isImeSetUp.set(true) }
            navController.navigate(Routes.Settings.Home) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }

        BackHandler(enabled = stepIndex > 0) { stepIndex-- }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MalangSettingsBg)
                .statusBarsPadding()
        ) {
            // 진행 상태와 건너뛰기
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${stepIndex + 1} / ${steps.size}",
                    modifier = Modifier.weight(1f),
                    color = MalangSettingsSummary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "건너뛰기",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = finish)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MalangSettingsSection,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                steps.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (index <= stepIndex) MalangSettingsSection else MalangSettingsBorder)
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)) {
                Text(step.title, color = MalangDarkCard, fontSize = 28.sp, lineHeight = 32.sp, fontFamily = JuaFontFamily)
                Text(step.subtitle, color = MalangSettingsSummary, fontSize = 14.sp)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (step) {
                    QuickStep.LANGUAGE -> LanguageStep()
                    QuickStep.FEEDBACK -> FeedbackStep()
                    QuickStep.SMARTBAR -> SmartbarStep()
                    QuickStep.CLIPBOARD -> ClipboardStep()
                    QuickStep.THEME -> ThemeStep()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (stepIndex > 0) {
                    MalangButton("이전", primary = false, modifier = Modifier.weight(1f)) { stepIndex-- }
                }
                MalangButton(
                    if (isLast) "시작하기" else "다음",
                    primary = true,
                    modifier = Modifier.weight(if (stepIndex > 0) 2f else 1f),
                ) {
                    if (isLast) finish() else stepIndex++
                }
            }
            if (step == QuickStep.FEEDBACK || step == QuickStep.THEME) {
                MalangTestInputBar()
            }
        }
    }
}

@Composable
private fun ColumnScope.LanguageStep() {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val presets by keyboardManager.resources.subtypePresets.collectAsState()
    val layouts by keyboardManager.resources.layouts.collectAsState()

    MalangInfoCard("여러 개를 고르면 스페이스바를 좌우로 밀어서 바꿔 쓸 수 있어요. 나중에 키보드 설정에서 언제든 바꿀 수 있어요.")

    // 한국어 자판을 먼저 보여준다.
    val byLanguage = presets
        .groupBy { it.locale.displayName() }
        .toList()
        .sortedByDescending { (_, list) -> list.first().locale.language == "ko" }
    for ((language, languagePresets) in byLanguage) {
        MalangSettingsSection(
            title = language,
            items = languagePresets.map { preset ->
                {
                    val subtype = preset.toSubtype()
                    val checked = subtypes.any { it.equalsExcludingId(subtype) }
                    val layoutLabel = layouts[LayoutType.CHARACTERS]?.get(preset.preferred.characters)?.label
                    val name = keyboardDisplayName(preset.locale, layoutLabel)
                        .removePrefix(preset.locale.displayName()).removePrefix(" - ")
                        .ifEmpty { language }
                    CheckRow(name, checked) {
                        if (checked) {
                            val existing = subtypes.find { it.equalsExcludingId(subtype) }
                            // 마지막 하나는 지우지 않는다.
                            if (existing != null && subtypes.size > 1) subtypeManager.removeSubtype(existing)
                        } else {
                            subtypeManager.addSubtypeAndActivate(subtype)
                        }
                        keyboardManager.resources.anyChangedVersion.value += 1
                    }
                }
            },
        )
    }
}

@Composable
private fun CheckRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        MalangToggle(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun FeedbackStep() {
    val prefs by FlorisPreferenceStore
    MalangSettingsSection(
        items = listOf(
            { MalangSwitchRow(prefs.inputFeedback.audioEnabled, "키 소리", "키를 누를 때 소리를 냅니다.") },
            { MalangSwitchRow(prefs.malang.malangSoundEnabled, "말랑 효과음", "기본 소리 대신 말랑키 전용 효과음을 씁니다.") },
            { MalangSwitchRow(prefs.inputFeedback.hapticEnabled, "키 진동", "키를 누를 때 진동을 줍니다.") },
            { MalangSwitchRow(prefs.keyboard.popupEnabled, "키 팝업", "누른 글자를 키 위에 크게 보여줍니다.") },
            { MalangSwitchRow(prefs.keyboard.numberRow, "숫자 행 표시", "키보드 맨 위에 숫자 줄을 항상 보여줍니다.") },
        ),
    )
    Text(
        "아래 입력창에서 바로 눌러보며 확인해 보세요. 소리 크기와 진동 세기는 소리·진동 설정에서 조절할 수 있어요.",
        color = MalangSettingsSummary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}

@Composable
private fun SmartbarStep() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()

    // 스마트 바를 쓰면 말랑 슬롯 레이아웃을 기본으로 둔다.
    LaunchedEffect(Unit) {
        if (prefs.smartbar.layout.get() != SmartbarLayout.MALANG_SLOTS) {
            prefs.smartbar.layout.set(SmartbarLayout.MALANG_SLOTS)
        }
    }

    MalangInfoCard("처음에는 붙여넣기 · 복사 · 전체 선택 · ← · → 가 들어 있어요. 대부분 이대로 쓰시는 걸 추천해요!")
    MalangSettingsSection(
        items = listOf(
            { MalangSwitchRow(prefs.smartbar.enabled, "스마트 바 사용", "키보드 위에 기능 버튼 줄을 표시합니다.") },
            {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("버튼 바꾸기", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "위 칸을 누르면 비우고, 아래 기능을 누르면 빈 칸에 들어갑니다.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MalangSettingsSummary,
                        fontSize = 13.sp,
                    )
                    MalangSlotsEditor()
                }
            },
            {
                MalangNavRow(title = "추천 구성으로 되돌리기", summary = "붙여넣기 · 복사 · 전체 선택 · ← · →") {
                    scope.launch {
                        prefs.smartbar.malangSlots.set(prefs.smartbar.malangSlots.default)
                        prefs.smartbar.malangSlotsCount.set(prefs.smartbar.malangSlotsCount.default)
                    }
                }
            },
        ),
    )
}

@Composable
private fun ClipboardStep() {
    val prefs by FlorisPreferenceStore
    val triggerKey by prefs.clipboard.quickPhraseTriggerKey.collectPrefAsState()
    val triggerName = when (triggerKey) {
        QuickPhraseTriggerKey.PERIOD -> "마침표(.)"
        QuickPhraseTriggerKey.COMMA -> "쉼표(,)"
        QuickPhraseTriggerKey.ENTER -> "엔터"
    }

    MalangInfoCard(
        "상용구: $triggerName 키를 길게 누르면 \"감사합니다.\", \"지금 가고 있어요.\" 같은 문구 표가 떠요. " +
            "원하는 칸에서 손을 떼면 바로 입력됩니다. 문구는 클립보드 설정에서 바꿀 수 있어요."
    )
    MalangSettingsSection(
        items = listOf(
            {
                MalangChoiceRow(
                    prefs.clipboard.quickPhraseTriggerKey,
                    title = "상용구 호출 키",
                    entries = listOf(
                        QuickPhraseTriggerKey.PERIOD to "마침표(.)",
                        QuickPhraseTriggerKey.COMMA to "쉼표(,)",
                        QuickPhraseTriggerKey.ENTER to "엔터(Enter)",
                    ),
                )
            },
            {
                MalangSwitchRow(
                    prefs.clipboard.historyEnabled,
                    title = "클립보드 기록",
                    summary = "복사한 내용을 모아두고 키보드에서 다시 붙여넣습니다.",
                )
            },
            {
                MalangSwitchRow(
                    prefs.clipboard.suggestionEnabled,
                    title = "방금 복사한 내용 추천",
                    summary = "복사 직후 스마트 바에 붙여넣기 버튼을 띄웁니다.",
                )
            },
        ),
    )
}

@Composable
private fun ThemeStep() {
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val mode by prefs.theme.mode.collectPrefAsState()
    val dayThemeId by prefs.theme.dayThemeId.collectPrefAsState()
    val nightThemeId by prefs.theme.nightThemeId.collectPrefAsState()
    val themes = malangThemes.filter { it.compId != "custom" }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in themes.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (theme in row) {
                    val selected = isMalangThemeSelected(theme, mode, dayThemeId, nightThemeId)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) MalangSettingsSection else MalangSettingsCard)
                            .border(1.dp, if (selected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(20.dp))
                            .clickable { scope.launch { applyMalangTheme(prefs, theme) } }
                            .padding(horizontal = 14.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(theme.displayColor)
                                .border(1.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            theme.name,
                            color = if (selected) MalangSettingsCard else MalangSettingsTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
    Text(
        "배경 사진, 키 모양 같은 자세한 꾸미기는 메인 화면의 테마 탭에서 할 수 있어요.",
        color = MalangSettingsSummary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}
