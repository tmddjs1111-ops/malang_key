package dev.malangkey.app.setup

import dev.malangkey.app.settings.theme.KeyboardFontPicker
import dev.malangkey.app.settings.keyboard.KeySoundPicker
import dev.malangkey.app.settings.clipboard.QuickPhraseGridEditor
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.remember
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
import androidx.compose.ui.draw.alpha
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
import android.content.ClipboardManager
import android.os.SystemClock
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.malangkey.ime.smartbar.SmartbarHighlight
import dev.malangkey.ime.text.key.KeyCode
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import dev.malangkey.app.apptheme.KoreanWordBreak
import dev.malangkey.app.apptheme.MalangDarkCardSub
import dev.malangkey.app.apptheme.MalangDarkCardTitle
import dev.malangkey.ime.keyboard.LayoutArrangementComponent
import dev.malangkey.lib.ext.ExtensionComponentName
import kotlinx.serialization.json.Json
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
import dev.malangkey.app.settings.keyboard.KeyboardLanguageTabs
import dev.malangkey.app.settings.keyboard.keyboardDisplayName
import android.content.Context
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.core.SubtypePreset
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

/**
 * 자판 목록이 비어 있으면 두벌식을 넣는다. 비어 있어도 키보드는 기본 자판으로 동작하지만,
 * 자판 선택 화면에 아무것도 체크되지 않고 홈에 설정 경고가 뜬다.
 */
internal fun ensureDefaultSubtype(context: Context) {
    val subtypeManager by context.subtypeManager()
    if (subtypeManager.subtypes.isNotEmpty()) return
    val keyboardManager by context.keyboardManager()
    val default = Subtype.DEFAULT
    val preset = keyboardManager.resources.subtypePresets.value.find {
        it.locale == default.primaryLocale && it.preferred.characters == default.layoutMap.characters
    }
    subtypeManager.addSubtypeAndActivate(preset?.toSubtype() ?: default)
    keyboardManager.resources.anyChangedVersion.value += 1
}

/** 키보드를 켜고 고른 뒤 이어지는 간단 설정 단계. */
private enum class QuickStep(val title: String, val subtitle: String) {
    LANGUAGE("언어·자판", "어떤 자판으로 입력할까요?"),
    FEEDBACK("소리·진동", "키를 누를 때의 느낌을 골라주세요."),
    SMARTBAR("스마트 바", "키보드 위 버튼 줄에 넣을 기능이에요."),
    CLIPBOARD("클립보드·상용구", "자주 쓰는 문구를 빠르게 입력해요."),
    THEME("테마·글꼴", "키보드 색과 글꼴을 골라주세요."),
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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val steps = QuickStep.entries
        var stepIndex by rememberSaveable { mutableIntStateOf(0) }
        val step = steps[stepIndex]
        val isLast = stepIndex == steps.lastIndex

        val subtypeManager by context.subtypeManager()
        val keyboardManager by context.keyboardManager()
        val subtypes by subtypeManager.subtypesFlow.collectAsState()
        val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
        val layouts by keyboardManager.resources.layouts.collectAsState()
        val quickPhrasesJson by prefs.clipboard.quickPhrases.collectPrefAsState()
        val triggerKey by prefs.clipboard.quickPhraseTriggerKey.collectPrefAsState()
        val phrases = remember(quickPhrasesJson) {
            runCatching { Json.decodeFromString<List<String>>(quickPhrasesJson) }
                .getOrDefault(emptyList())
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        // 단계마다 아래 입력창에서 직접 해 보는 미션. 끝낸 단계는 진행 바에 체크가 찍힌다.
        val doneSteps = remember { mutableStateListOf<QuickStep>() }
        // 스마트 바 단계는 틀린 문장을 미리 써 둔다. 사용자는 고치기·복사·붙여넣기만 해 본다.
        var testText by remember(step) {
            val initial = if (step == QuickStep.SMARTBAR) TypoSentence else ""
            mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
        }
        var lastToggleAt by remember { mutableLongStateOf(0L) }
        var seenSubtypeId by remember(step) { mutableLongStateOf(activeSubtype.id) }
        var layoutSwitched by remember { mutableStateOf(false) }
        var typoFixed by remember { mutableStateOf(false) }
        var copied by remember { mutableStateOf(false) }
        var pasteCount by remember { mutableIntStateOf(0) }
        var phraseInserted by remember { mutableStateOf(false) }

        LaunchedEffect(step, activeSubtype.id) {
            if (step != QuickStep.LANGUAGE || activeSubtype.id == seenSubtypeId) return@LaunchedEffect
            // 카드를 눌러 자판을 더하거나 빼도 활성 자판이 바뀌므로, 그 직후의 변화는 세지 않는다.
            if (subtypes.size >= 2 && SystemClock.uptimeMillis() - lastToggleAt > 1500L) layoutSwitched = true
            seenSubtypeId = activeSubtype.id
        }

        val onTestTextChange: (TextFieldValue) -> Unit = { newValue ->
            val new = newValue.text
            val inserted = insertedText(testText.text, new)
            testText = newValue
            when (step) {
                QuickStep.SMARTBAR -> {
                    val fixed = FixedSentence.replace(" ", "")
                    if (!typoFixed && new.replace(" ", "").contains(fixed) && newValue.composition == null) {
                        typoFixed = true
                        // 고친 문장을 골라 둔다. 바로 복사 버튼만 누르면 된다.
                        testText = newValue.copy(selection = TextRange(0, new.length))
                    } else if (typoFixed && !copied &&
                        clipboardText(context)?.replace(" ", "")?.contains(fixed) == true
                    ) {
                        copied = true
                    }
                    if (copied && inserted.replace(" ", "").contains(fixed)) pasteCount++
                }
                QuickStep.CLIPBOARD -> {
                    if (inserted.isNotBlank() && phrases.any { inserted.contains(it) }) phraseInserted = true
                }
                else -> Unit
            }
        }

        val triggerName = when (triggerKey) {
            QuickPhraseTriggerKey.PERIOD -> "마침표(.)"
            QuickPhraseTriggerKey.COMMA -> "쉼표(,)"
            QuickPhraseTriggerKey.ENTER -> "엔터"
        }
        val mission: StepMission? = when (step) {
            QuickStep.LANGUAGE -> when {
                subtypes.size < 2 -> StepMission(
                    "자판을 하나 더 골라주세요",
                    "두 개 이상이어야 스페이스바로 바꿔 쓸 수 있어요. 예: 영어 쿼티",
                )
                layoutSwitched -> StepMission("자판 바꾸기 성공!", "스페이스바를 밀 때마다 다음 자판으로 넘어가요", "1 / 1", done = true)
                else -> StepMission(
                    "스페이스바를 옆으로 밀어 자판을 바꿔 보세요",
                    "지금 자판: ${subtypeName(activeSubtype, layouts)}",
                    "0 / 1",
                )
            }
            QuickStep.SMARTBAR -> when {
                !typoFixed -> StepMission(
                    "‘말랑킬’을 ‘말랑키’로 고쳐 보세요",
                    "반짝이는 ‹ 버튼으로 ‘킬’ 뒤로 가서 지우고 ‘키’를 써요",
                    "1 / 3",
                )
                !copied -> StepMission("반짝이는 복사 버튼을 눌러 보세요", "고친 문장을 골라 뒀어요", "2 / 3")
                pasteCount < 5 -> StepMission(
                    "반짝이는 붙여넣기 버튼을 5번 눌러 보세요",
                    "복사한 문장이 뒤에 계속 붙어요",
                    "$pasteCount / 5",
                )
                else -> StepMission("커서 · 복사 · 붙여넣기 완료!", "스마트 바 버튼은 아래에서 바꿀 수 있어요", "3 / 3", done = true)
            }
            QuickStep.CLIPBOARD -> if (phraseInserted) {
                StepMission("상용구 넣기 성공!", "문구는 아래 표에서 바로 바꿀 수 있어요", "1 / 1", done = true)
            } else {
                StepMission("$triggerName 키를 꾹 눌러 상용구를 넣어 보세요", "표가 뜨면 원하는 칸으로 밀고 손을 떼요", "0 / 1")
            }
            else -> null
        }
        LaunchedEffect(step, mission?.done) {
            if (mission?.done == true && step !in doneSteps) doneSteps.add(step)
        }

        // 지금 눌러 볼 스마트 바 버튼을 키보드에서 반짝이게 한다.
        val highlight = when {
            step != QuickStep.SMARTBAR -> emptySet()
            !typoFixed -> setOf(KeyCode.ARROW_LEFT, KeyCode.ARROW_RIGHT)
            !copied -> setOf(KeyCode.CLIPBOARD_COPY)
            pasteCount < 5 -> setOf(KeyCode.CLIPBOARD_PASTE)
            else -> emptySet()
        }
        LaunchedEffect(highlight) { SmartbarHighlight.keyCodes.value = highlight }
        DisposableEffect(Unit) { onDispose { SmartbarHighlight.keyCodes.value = emptySet() } }

        val finish: () -> Unit = {
            ensureDefaultSubtype(context)
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
                // 건너뛰기는 이 단계만 넘긴다. 자판을 두 개 이상 고르기 전에는 넘길 수 없다.
                val canSkip = subtypes.size >= 2
                Text(
                    "건너뛰기",
                    modifier = Modifier
                        .alpha(if (canSkip) 1f else 0.35f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = canSkip) { if (isLast) finish() else stepIndex++ }
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
                steps.forEachIndexed { index, s ->
                    Box(
                        modifier = Modifier.weight(1f).height(16.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index <= stepIndex) MalangSettingsSection else MalangSettingsBorder)
                        )
                        if (s in doneSteps) {
                            Box(
                                modifier = Modifier.size(16.dp).clip(CircleShape).background(MissionGreen),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                        }
                    }
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
                    QuickStep.LANGUAGE -> LanguageStep(onToggle = { lastToggleAt = SystemClock.uptimeMillis() })
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
                    // 자판은 두 개 이상 골라야 넘어간다. 미션은 해 보지 않아도 넘어갈 수 있다.
                    enabled = !(step == QuickStep.LANGUAGE && subtypes.size < 2),
                    modifier = Modifier.weight(if (stepIndex > 0) 2f else 1f),
                ) {
                    if (isLast) finish() else stepIndex++
                }
            }
            MalangTestInputBar(
                value = testText,
                onValueChange = onTestTextChange,
                header = mission?.let { { MissionRow(it) } },
            )
        }
    }
}

@Composable
private fun ColumnScope.LanguageStep(onToggle: () -> Unit) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val presets by keyboardManager.resources.subtypePresets.collectAsState()

    // 처음 들어오면 두벌식을 미리 체크해 둔다. 마지막 하나는 지울 수 없으니 이후로도 비지 않는다.
    LaunchedEffect(presets, subtypes.isEmpty()) {
        if (presets.isNotEmpty()) ensureDefaultSubtype(context)
    }

    MalangInfoCard("자판을 두 개 이상 골라주세요. 스페이스바를 좌우로 밀어서 바꿔 쓸 수 있어요.")

    KeyboardLayoutPicker(onToggle)
}

/** 언어 탭과 자판 카드(실제 키보드 미리보기). 간단 설정과 '키보드 언어 및 레이아웃' 화면이 같이 쓴다. */
@Composable
internal fun ColumnScope.KeyboardLayoutPicker(onToggle: () -> Unit = {}) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()
    val subtypes by subtypeManager.subtypesFlow.collectAsState()
    val presets by keyboardManager.resources.subtypePresets.collectAsState()
    val layouts by keyboardManager.resources.layouts.collectAsState()
    val activePalette = rememberActivePreviewPalette()

    val isEnabled = { preset: SubtypePreset -> subtypes.any { it.equalsExcludingId(preset.toSubtype()) } }
    fun toggle(preset: SubtypePreset) {
        onToggle()
        val subtype = preset.toSubtype()
        val existing = subtypes.find { it.equalsExcludingId(subtype) }
        if (existing != null) {
            // 마지막 하나는 지우지 않는다.
            if (subtypes.size > 1) subtypeManager.removeSubtype(existing)
        } else {
            subtypeManager.addSubtypeAndActivate(subtype)
        }
        keyboardManager.resources.anyChangedVersion.value += 1
    }

    fun layoutName(preset: SubtypePreset): String {
        val layoutLabel = layouts[LayoutType.CHARACTERS]?.get(preset.preferred.characters)?.label
        return keyboardDisplayName(preset.locale, layoutLabel).substringAfterLast(" - ")
    }

    KeyboardLanguageTabs(presets, isEnabled) { group ->
        if (group.title != null) {
            Text(group.title, color = MalangSettingsSection, fontSize = 20.sp, fontFamily = JuaFontFamily)
        }
        if (group.isLongList) {
            // 항목이 많은 목록은 미리보기 없이 켜고 끄는 줄만 보여준다.
            MalangSettingsSection(
                items = group.presets.map { preset ->
                    { CheckRow(group.label(preset, layoutName(preset)), isEnabled(preset)) { toggle(preset) } }
                },
            )
            return@KeyboardLanguageTabs
        }
        PreviewGrid(group.presets) { preset, modifier ->
            val subtype = remember(preset) { preset.toSubtype() }
            PreviewCard(
                title = group.label(preset, layoutName(preset)),
                checked = isEnabled(preset),
                modifier = modifier,
                onClick = { toggle(preset) },
            ) {
                KeyboardPreview(subtype = subtype, palette = activePalette)
            }
        }
    }
}

/** 카드를 두 줄씩 배치한다. */
@Composable
private fun <T> PreviewGrid(items: List<T>, card: @Composable (T, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in items.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (item in row) {
                    card(item, Modifier.weight(1f))
                }
                if (row.size < 2) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 제목·체크 표시와 키보드 미리보기가 들어간 선택 카드. */
@Composable
private fun PreviewCard(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MalangSettingsCard)
            .border(
                width = if (checked) 2.dp else 1.dp,
                color = if (checked) MalangSettingsSection else MalangSettingsBorder,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = MalangSettingsTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) MalangSettingsSection else Color.Transparent)
                    .border(1.5.dp, if (checked) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MalangSettingsCard, modifier = Modifier.size(16.dp))
                }
            }
        }
        preview()
    }
}

@Composable
private fun FeedbackStep() {
    val prefs by FlorisPreferenceStore
    val audioEnabled by prefs.inputFeedback.audioEnabled.collectPrefAsState()
    MalangSettingsSection(
        items = listOf(
            { MalangSwitchRow(prefs.inputFeedback.audioEnabled, "키 소리", "키를 누를 때 소리를 냅니다.") },
            { KeySoundPicker(enabled = audioEnabled) },
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

    MalangInfoCard("처음에는 붙여넣기 · 복사 · 전체 선택 · 이모지 · ← · → 가 들어 있어요. 대부분 이대로 쓰시는 걸 추천해요!")
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
                MalangNavRow(title = "추천 구성으로 되돌리기", summary = "붙여넣기 · 복사 · 전체 선택 · 이모지 · ← · →") {
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
            "원하는 칸에서 손을 떼면 바로 입력됩니다. 문구는 아래 표에서 바로 바꿀 수 있어요."
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
            { QuickPhraseGridEditor() },
        ),
    )
}

@Composable
private fun ThemeStep() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mode by prefs.theme.mode.collectPrefAsState()
    val dayThemeId by prefs.theme.dayThemeId.collectPrefAsState()
    val nightThemeId by prefs.theme.nightThemeId.collectPrefAsState()
    val subtypeManager by context.subtypeManager()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
    val themes = remember { malangThemes.filter { it.compId != "custom" } }
    val palettes = remember(context) {
        themes.associateWith { loadPreviewPalette(context, it.extId, it.compId) ?: DefaultPreviewPalette }
    }

    PreviewGrid(themes) { theme, modifier ->
        PreviewCard(
            title = theme.name,
            checked = isMalangThemeSelected(theme, mode, dayThemeId, nightThemeId),
            modifier = modifier,
            onClick = { scope.launch { applyMalangTheme(prefs, theme) } },
        ) {
            KeyboardPreview(subtype = activeSubtype, palette = palettes.getValue(theme))
        }
    }
    MalangSettingsSection(items = listOf({ KeyboardFontPicker() }))
    Text(
        "배경 사진, 외곽선 같은 자세한 꾸미기는 메인 화면의 테마 탭에서 할 수 있어요.",
        color = MalangSettingsSummary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
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

private const val TypoSentence = "안녕하세요 말랑킬 입니다"
private const val FixedSentence = "안녕하세요 말랑키 입니다"
private val MissionGreen = Color(0xFF3E9B5F)

/** 단계 아래 입력창에서 해 보는 미션 한 줄. [badge]는 오른쪽의 진행 표시("2 / 5"). */
private data class StepMission(
    val title: String,
    val summary: String,
    val badge: String? = null,
    val done: Boolean = false,
)

/** [old]가 [new]로 바뀔 때 새로 들어온 글자. 지우거나 조합 중인 글자만 바뀌었으면 빈 문자열. */
private fun insertedText(old: String, new: String): String {
    if (new.length <= old.length) return ""
    var start = 0
    while (start < old.length && old[start] == new[start]) start++
    var end = 0
    while (end < old.length - start && old[old.length - 1 - end] == new[new.length - 1 - end]) end++
    return new.substring(start, new.length - end)
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = clipboard.primaryClip?.takeIf { it.itemCount > 0 } ?: return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}

private fun subtypeName(
    subtype: Subtype,
    layouts: Map<LayoutType, Map<ExtensionComponentName, LayoutArrangementComponent>>,
): String {
    val label = layouts[LayoutType.CHARACTERS]?.get(subtype.layoutMap.characters)?.label
    return keyboardDisplayName(subtype.primaryLocale, label)
}

@Composable
private fun MissionRow(mission: StepMission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (mission.done) MissionGreen else Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (mission.done) Color.White else MalangDarkCardTitle.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (mission.done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MissionGreen, modifier = Modifier.size(16.dp))
            } else {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MalangDarkCardTitle))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                mission.title,
                color = if (mission.done) Color.White else MalangDarkCardTitle,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                style = KoreanWordBreak,
            )
            Text(
                mission.summary,
                color = if (mission.done) Color.White.copy(alpha = 0.85f) else MalangDarkCardSub,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                style = KoreanWordBreak,
            )
        }
        if (mission.badge != null) {
            Text(
                mission.badge,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (mission.done) Color.White else MalangDarkCardTitle)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (mission.done) MissionGreen else MalangDarkCard,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
