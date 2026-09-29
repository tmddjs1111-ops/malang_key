package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 한 탭 안에 지역이 여러 개면 지역별로 [group]을 한 번씩 그리고, 기타 탭은 언어 이름을 붙인
 * 한 목록으로 그린다.
 *
 * @param isEnabled 자판이 켜져 있는지. 탭 이름 아래에 켜진 개수를 표시하는 데 쓴다.
 * @param group 자판 묶음을 그린다. [PresetGroup.prefixOf]가 주는 글자를 자판 이름 앞에 붙인다.
 */
@Composable
fun KeyboardLanguageTabs(
    presets: List<SubtypePreset>,
    isEnabled: (SubtypePreset) -> Boolean,
    group: @Composable (PresetGroup) -> Unit,
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
            // 기타: 언어가 많고 대부분 자판이 하나뿐이라 펼치기 없이 "언어 · 자판" 한 목록으로 보여준다.
            selected == KeyboardLanguageTab.OTHER -> group(
                PresetGroup(
                    title = null,
                    presets = tabPresets.sortedBy { it.locale.displayName() },
                    isLongList = true,
                    prefixOf = { it.locale.displayName() },
                )
            )
            byRegion.size == 1 -> group(PresetGroup(null, tabPresets))
            else -> for ((region, regionPresets) in byRegion) {
                group(PresetGroup(region, regionPresets))
            }
        }
    }
}

/**
 * 한 번에 그릴 자판 묶음.
 *
 * @param title 묶음 제목. 없으면 null.
 * @param isLongList 기타 탭처럼 항목이 아주 많은 목록. 미리보기처럼 무거운 표시는 생략한다.
 * @param prefixOf 자판 이름 앞에 붙일 글자(예: 언어 이름). 붙이지 않으면 null.
 */
class PresetGroup(
    val title: String?,
    val presets: List<SubtypePreset>,
    val isLongList: Boolean = false,
    val prefixOf: (SubtypePreset) -> String? = { null },
) {
    /** "언어 · 자판" 또는 자판 이름만. */
    fun label(preset: SubtypePreset, layoutName: String): String =
        prefixOf(preset)?.let { "$it · $layoutName" } ?: layoutName
}

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
