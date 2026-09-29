package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.ime.core.SubtypePreset

/**
 * 키보드 선택 화면의 언어 탭. 영어는 미국 영어만 두고, 영국·캐나다 등 다른 지역 영어와
 * 한국어·영어·일본어·중국어 외의 언어는 기타에 모은다.
 */
enum class KeyboardLanguageTab(val label: String, val languageCode: String?, val country: String? = null) {
    KOREAN("한국어", "ko"),
    ENGLISH("영어", "en", country = "US"),
    JAPANESE("일본어", "ja"),
    CHINESE("중국어", "zh"),
    OTHER("기타", null);

    companion object {
        fun of(preset: SubtypePreset): KeyboardLanguageTab = entries.firstOrNull { tab ->
            tab.languageCode == preset.locale.language &&
                (tab.country == null || tab.country.equals(preset.locale.country, ignoreCase = true))
        } ?: OTHER
    }
}

/**
 * 자판 목록을 언어 탭으로 나눠 보여준다. 탭을 누르면 그 언어의 자판이 열린다.
 * 한 탭 안에 지역이 여러 개면(예: 영어 미국/영국) 지역별로 [group]을 한 번씩 그리되, 지역마다
 * 자판이 하나뿐이면 한 묶음으로 그리고 각 자판 이름에 지역을 붙이게 한다.
 * 기타 탭은 언어마다 눌러서 펼치는 목록으로 보여준다.
 *
 * @param isEnabled 자판이 켜져 있는지. 탭 이름 옆에 켜진 개수를 표시하는 데 쓴다.
 * @param group 자판 묶음을 그린다. 제목이 null이면 묶음 제목 없이, showRegion이면 자판 이름에
 *  [presetRegionLabel]을 붙여 그린다.
 */
@Composable
fun KeyboardLanguageTabs(
    presets: List<SubtypePreset>,
    isEnabled: (SubtypePreset) -> Boolean,
    group: @Composable (title: String?, presets: List<SubtypePreset>, showRegion: Boolean) -> Unit,
) {
    var selectedName by rememberSaveable { mutableStateOf(KeyboardLanguageTab.KOREAN.name) }
    val selected = KeyboardLanguageTab.valueOf(selectedName)
    val byTab = presets.groupBy { KeyboardLanguageTab.of(it) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (tab in KeyboardLanguageTab.entries) {
                val enabledCount = byTab[tab].orEmpty().count(isEnabled)
                LanguageTabChip(
                    label = tab.label,
                    count = enabledCount,
                    selected = tab == selected,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedName = tab.name },
                )
            }
        }

        val tabPresets = byTab[selected].orEmpty()
        val byRegion = tabPresets.groupBy { it.locale.displayName() }
        when {
            tabPresets.isEmpty() -> Text("이 언어의 자판이 아직 없어요.", color = MalangSettingsSummary, fontSize = 14.sp)
            selected == KeyboardLanguageTab.OTHER -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for ((language, languagePresets) in byRegion.toSortedMap()) {
                    ExpandableLanguage(
                        title = language,
                        count = languagePresets.count(isEnabled),
                    ) {
                        group(null, languagePresets, false)
                    }
                }
            }
            byRegion.size == 1 -> group(null, tabPresets, false)
            byRegion.values.all { it.size == 1 } -> group(null, tabPresets, true)
            else -> for ((region, regionPresets) in byRegion) {
                group(region, regionPresets, false)
            }
        }
    }
}

/** 자판 이름 앞에 붙이는 지역 이름 (예: 미국). 지역이 없으면 언어 이름. */
fun presetRegionLabel(preset: SubtypePreset): String =
    preset.locale.displayCountry().ifBlank { preset.locale.displayName() }

@Composable
private fun LanguageTabChip(
    label: String,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // 글꼴 위아래 여백을 잘라내서 글자가 칸 한가운데 오게 한다.
    val centered = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MalangSettingsSection else MalangSettingsCard)
            .border(1.dp, if (selected) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(
            label,
            color = if (selected) MalangSettingsCard else MalangSettingsTitle,
            fontFamily = JuaFontFamily,
            maxLines = 1,
            style = TextStyle(fontSize = 15.sp, lineHeight = 15.sp, lineHeightStyle = centered),
        )
        if (count > 0) {
            Text(
                "${count}개",
                color = if (selected) MalangSettingsCard.copy(alpha = 0.8f) else MalangSettingsSummary,
                maxLines = 1,
                style = TextStyle(fontSize = 10.sp, lineHeight = 10.sp, lineHeightStyle = centered),
            )
        }
    }
}

/** 눌러서 펼치고 접는 언어 한 줄 (기타 탭). */
@Composable
private fun ExpandableLanguage(
    title: String,
    count: Int,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MalangSettingsCard)
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(18.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = MalangSettingsTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MalangSettingsSection)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("${count}개 사용", color = MalangSettingsCard, fontSize = 11.sp)
                }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "접기" else "펼치기",
                tint = MalangSettingsSummary,
            )
        }
        if (expanded) {
            content()
        }
    }
}
