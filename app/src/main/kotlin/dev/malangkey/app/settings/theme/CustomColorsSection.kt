package dev.malangkey.app.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.malangkey.app.apptheme.MalangText
import dev.malangkey.app.setup.KeyboardPreview
import dev.malangkey.app.setup.PreviewPalette
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.text.key.KeyCode
import dev.patrickgold.jetpref.datastore.model.PreferenceData
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefColorPicker
import dev.patrickgold.jetpref.material.ui.rememberJetPrefColorPickerState
import kotlinx.coroutines.launch

/** 표 한 줄에 놓이는 색 하나. [fallback]은 고르지 않았을 때 보이는 바탕 테마 색이다. */
private class ColorSlot(val label: String, val pref: PreferenceData<Color>, val fallback: Color)

/**
 * 커스텀 색상. 위에 지금 커스텀 모양의 미리보기를 두고, 아래에 키보드 부위별로 한 줄씩 색을 고른다.
 * 바꾼 색은 진하게, 바꾸지 않은 색은 바탕 테마 색 그대로 흐리게 보여 준다.
 */
@Composable
fun CustomColorsSection(subtype: Subtype, palette: PreviewPalette, basePalette: PreviewPalette) {
    val prefs by FlorisPreferenceStore
    val m = prefs.malang
    val (specialBg, specialFg) = basePalette.colorsFor(KeyCode.SHIFT)
    val (enterBg, enterFg) = basePalette.colorsFor(KeyCode.ENTER)
    val rows = listOf(
        "판" to listOf(ColorSlot("배경", m.customKeyboardBgColor, basePalette.background)),
        "키" to listOf(
            ColorSlot("배경", m.customKeyBgColor, basePalette.keyBackground),
            ColorSlot("글자", m.customKeyTextColor, basePalette.keyForeground),
        ),
        "특수 키" to listOf(
            ColorSlot("배경", m.customEnterKeyBgColor, specialBg),
            ColorSlot("글자", m.customEnterKeyTextColor, specialFg),
        ),
        "엔터" to listOf(
            ColorSlot("배경", m.customRealEnterKeyBgColor, enterBg),
            ColorSlot("글자", m.customRealEnterKeyTextColor, enterFg),
        ),
        "스마트바" to listOf(
            ColorSlot("배경", m.customSmartbarBgColor, basePalette.background),
            ColorSlot("아이콘", m.customSmartbarIconColor, basePalette.keyForeground),
        ),
    )

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "커스텀 색상",
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            color = MalangSettingsSection,
            fontFamily = MalangJuaFont,
            fontSize = 23.sp,
        )
        // 색을 바꾸면 바로 바뀌는 미리보기. 배경 사진도 함께 깔린다.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(20.dp)),
        ) {
            KeyboardPreview(subtype = subtype, palette = palette, height = 120.dp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MalangSettingsCard)
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp)),
        ) {
            for ((index, row) in rows.withIndex()) {
                if (index > 0) HorizontalDivider(color = MalangSettingsBorder.copy(alpha = 0.6f))
                ColorRow(title = row.first, slots = row.second)
            }
        }
        Text(
            "바꾼 색은 진하게 보여요. 안 바꾼 색은 지금 테마 색을 따라가요.",
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            color = MalangSettingsSummary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ColorRow(title: String, slots: List<ColorSlot>) {
    val changed = slots.count { it.pref.collectAsState().value != Color.Unspecified }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.width(68.dp),
            color = MalangText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            for (slot in slots) ColorChip(title = "$title ${slot.label}", slot = slot)
        }
        if (changed > 0) {
            Text("바꿈 $changed", color = MalangSettingsSummary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ColorChip(title: String, slot: ColorSlot) {
    val scope = rememberCoroutineScope()
    val picked by slot.pref.collectAsState()
    val isSet = picked != Color.Unspecified
    val shown = if (isSet) picked else slot.fallback
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSet) MalangSettingsSection else Color(0xFFF6EFE6))
            .clickable { showDialog = true }
            .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (shown == Color.Unspecified) Color(0xFFEEEEEE) else shown)
                .border(2.dp, Color.White, CircleShape),
        )
        Text(
            if (isSet) "${slot.label} ✓" else slot.label,
            modifier = Modifier.padding(start = 6.dp),
            color = if (isSet) MalangSettingsCard else MalangText,
            fontSize = 13.sp,
        )
    }

    if (showDialog) {
        var selectedColor by remember { mutableStateOf(if (shown == Color.Unspecified) Color.White else shown) }
        val colorPickerState = rememberJetPrefColorPickerState(initColor = selectedColor)
        JetPrefAlertDialog(
            title = title,
            confirmLabel = "선택",
            onConfirm = {
                scope.launch { slot.pref.set(selectedColor) }
                showDialog = false
            },
            dismissLabel = "취소",
            onDismiss = { showDialog = false },
        ) {
            Column {
                JetPrefColorPicker(
                    state = colorPickerState,
                    onColorChange = { selectedColor = it },
                )
                if (isSet) {
                    TextButton(
                        onClick = {
                            scope.launch { slot.pref.set(Color.Unspecified) }
                            showDialog = false
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) { Text("테마 색으로 되돌리기", color = MalangSettingsSummary) }
                }
            }
        }
    }
}
