package dev.malangkey.app.apptheme

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.R
import dev.malangkey.app.LocalNavController
import dev.malangkey.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// 피그마 설정 화면 공통 색상 (Malang Key / Keyboard Settings 등)
val MalangSettingsBg = Color(0xFFFBF0D1)
val MalangSettingsCard = Color(0xFFFFFDFB)
val MalangSettingsBorder = Color(0xFFE5D8C6)
val MalangSettingsDivider = Color(0xFFE9DCCB)
val MalangSettingsSection = Color(0xFF68463B)
val MalangSettingsTitle = Color(0xFF4A3028)
val MalangSettingsSummary = Color(0xFF866A5F)

/**
 * 피그마 설정 화면 틀: 뒤로가기 버튼과 한/영 제목이 있는 헤더, 스크롤되는 본문,
 * 하단에 고정된 키보드 테스트 입력창.
 */
@Composable
fun MalangSettingsScreen(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) = FlorisScreen {
    this.title = title
    topBarVisible = false
    previewFieldVisible = false
    scrollable = false

    content {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MalangSettingsBg)
        ) {
            MalangSettingsHeader(title, subtitle)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = content,
            )
            MalangTestInputBar()
        }
    }
}

@Composable
private fun MalangSettingsHeader(title: String, subtitle: String) {
    val navController = LocalNavController.current
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.mk_back_button),
                contentDescription = "뒤로",
                modifier = Modifier
                    .size(37.4.dp, 36.dp)
                    .clip(CircleShape)
                    .clickable { navController.popBackStack() },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = MalangDarkCard, fontSize = 24.sp, lineHeight = 28.sp, fontFamily = JuaFontFamily, maxLines = 1)
                Text(subtitle, color = MalangSettingsSummary, fontSize = 12.sp, lineHeight = 14.sp, maxLines = 1)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MalangSettingsBorder)
        )
    }
}

@Composable
fun MalangTestInputBar() {
    var text by remember { mutableStateOf("") }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MalangDarkCard)
            .padding(horizontal = 29.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MalangDarkCardTitle)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) {
                Text("여기에 입력해 보세요…", color = MalangSettingsSummary, fontSize = 13.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = MalangSettingsTitle, fontSize = 14.sp),
                cursorBrush = SolidColor(MalangSettingsSection),
            )
        }
    }
}

/** 섹션 제목 + 흰 카드. 카드 안 항목 사이에는 구분선이 자동으로 들어간다. */
@Composable
fun MalangSettingsSection(
    title: String? = null,
    items: List<@Composable () -> Unit>,
) {
    Column {
        if (title != null) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                color = MalangSettingsSection,
                fontSize = 23.sp,
                fontFamily = JuaFontFamily,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MalangSettingsCard)
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 6.dp)
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MalangSettingsDivider)
                    )
                }
                item()
            }
        }
    }
}

@Composable
private fun RowTexts(title: String, summary: String?, modifier: Modifier, summaryBold: Boolean = false) {
    Column(modifier = modifier) {
        Text(title, color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (summary != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                summary,
                color = if (summaryBold) MalangSettingsSection else MalangSettingsSummary,
                fontSize = if (summaryBold) 14.sp else 13.sp,
                fontWeight = if (summaryBold) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

/** 제목/설명과 오른쪽 화살표. [summaryBold]는 "300ms"처럼 현재 값을 보여줄 때 쓴다. */
@Composable
fun MalangNavRow(
    title: String,
    summary: String? = null,
    summaryBold: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, summary, Modifier.weight(1f), summaryBold)
        Image(
            painter = painterResource(R.drawable.mk_chevron_right),
            contentDescription = null,
            modifier = Modifier.padding(start = 12.dp, end = 10.dp).size(9.2.dp, 16.2.dp),
        )
    }
}

@Composable
fun MalangToggle(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val thumbOffset = if (checked) 25.dp else 5.dp
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .size(52.dp, 32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (checked) MalangSettingsSection else Color(0xFFE3DEE8))
            .then(if (checked) Modifier else Modifier.border(2.dp, Color.White, RoundedCornerShape(16.dp)))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) MalangDarkCardTitle else Color(0xFF8A8590))
        )
    }
}

@Composable
fun MalangSwitchRow(
    pref: PreferenceData<Boolean>,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onRowClick: (() -> Unit)? = null,
) {
    val checked by pref.collectAsState()
    val scope = rememberCoroutineScope()
    val setChecked: (Boolean) -> Unit = { scope.launch { pref.set(it) } }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled) { onRowClick?.invoke() ?: setChecked(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowTexts(title, summary, Modifier.weight(1f))
        MalangToggle(checked = checked, enabled = enabled, onCheckedChange = setChecked)
    }
}

@Composable
fun MalangSliderRow(
    pref: PreferenceData<Int>,
    title: String,
    min: Int,
    max: Int,
    step: Int = 1,
    unit: String = "%",
    enabled: Boolean = true,
) {
    val stored by pref.collectAsState()
    val scope = rememberCoroutineScope()
    // 드래그 중에는 로컬 값만 바꾸고, 손을 뗄 때 저장한다.
    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableFloatStateOf(0f) }
    val shown = if (dragging) local.roundToInt() else stored.coerceIn(min, max)

    Column(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$shown$unit", color = MalangSettingsSection, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        MalangSlider(
            value = shown.toFloat(),
            enabled = enabled,
            onValueChange = { dragging = true; local = snap(it, min, step) },
            onValueChangeFinished = {
                val v = local.roundToInt()
                scope.launch { pref.set(v) }
                dragging = false
            },
            range = min.toFloat()..max.toFloat(),
        )
        Row {
            Text("$min$unit", modifier = Modifier.weight(1f), color = MalangSettingsSummary, fontSize = 13.sp)
            Text("$max$unit", color = MalangSettingsSummary, fontSize = 13.sp, textAlign = TextAlign.End)
        }
    }
}

private fun snap(value: Float, min: Int, step: Int): Float =
    if (step <= 1) value else (min + ((value - min) / step).roundToInt() * step).toFloat()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MalangSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = range,
        enabled = enabled,
        modifier = Modifier.height(32.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MalangDarkCardTitle)
                    .border(4.dp, MalangSettingsSection, CircleShape)
            )
        },
        track = { state ->
            val fraction = (state.value - state.valueRange.start) /
                (state.valueRange.endInclusive - state.valueRange.start)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MalangSettingsBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(MalangSettingsSection)
                )
            }
        },
        colors = SliderDefaults.colors(),
    )
}

/** 목록에서 하나를 고르는 다이얼로그. */
@Composable
fun <T> MalangChoiceDialog(
    title: String,
    entries: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MalangSettingsCard,
        title = { Text(title, color = MalangSettingsTitle, fontFamily = JuaFontFamily, fontSize = 20.sp) },
        text = {
            Column {
                entries.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = value == selected) { onSelect(value); onDismiss() }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MalangSettingsSection),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(label, color = MalangSettingsTitle, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기", color = MalangSettingsSection) }
        },
    )
}

/** 누르면 [MalangChoiceDialog]가 뜨고, 설명 줄에 현재 값을 보여주는 행. */
@Composable
fun <T : Any> MalangChoiceRow(
    pref: PreferenceData<T>,
    title: String,
    entries: List<Pair<T, String>>,
    enabled: Boolean = true,
) {
    val value by pref.collectAsState()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    MalangNavRow(
        title = title,
        summary = entries.firstOrNull { it.first == value }?.second,
        enabled = enabled,
        onClick = { open = true },
    )
    if (open) {
        MalangChoiceDialog(
            title = title,
            entries = entries,
            selected = value,
            onSelect = { scope.launch { pref.set(it) } },
            onDismiss = { open = false },
        )
    }
}

/** 설명 줄에 값(예: 300ms)을 보여주고, 누르면 슬라이더 다이얼로그가 뜨는 행. */
@Composable
fun MalangValueDialogRow(
    pref: PreferenceData<Int>,
    title: String,
    min: Int,
    max: Int,
    step: Int,
    unit: String,
    enabled: Boolean = true,
    valueLabel: (Int) -> String = { "$it$unit" },
    onPreview: ((Int) -> Unit)? = null,
) {
    val stored by pref.collectAsState()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    MalangNavRow(title = title, summary = valueLabel(stored), summaryBold = true, enabled = enabled, onClick = { open = true })
    if (open) {
        var local by remember { mutableFloatStateOf(stored.coerceIn(min, max).toFloat()) }
        AlertDialog(
            onDismissRequest = { open = false },
            containerColor = MalangSettingsCard,
            title = { Text(title, color = MalangSettingsTitle, fontFamily = JuaFontFamily, fontSize = 20.sp) },
            text = {
                Column {
                    Text(
                        valueLabel(local.roundToInt()),
                        modifier = Modifier.fillMaxWidth(),
                        color = MalangSettingsSection,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    MalangSlider(
                        value = local,
                        onValueChange = { local = snap(it, min, step) },
                        onValueChangeFinished = { onPreview?.invoke(local.roundToInt()) },
                        range = min.toFloat()..max.toFloat(),
                    )
                    Row {
                        Text(valueLabel(min), modifier = Modifier.weight(1f), color = MalangSettingsSummary, fontSize = 13.sp)
                        Text(valueLabel(max), color = MalangSettingsSummary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = local.roundToInt()
                    scope.launch { pref.set(v) }
                    open = false
                }) { Text("확인", color = MalangSettingsSection) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) { Text("취소", color = MalangSettingsSummary) }
            },
        )
    }
}

/** 섹션 위에 두는 안내 문구 카드. */
@Composable
fun MalangInfoCard(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MalangSettingsCard.copy(alpha = 0.6f))
            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MalangSettingsSummary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
}
