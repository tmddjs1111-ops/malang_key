package dev.malangkey.app.setup

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.theme.ThemeMode
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKey
import dev.malangkey.ime.text.keyboard.TextKeyboard
import dev.malangkey.keyboardManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val CheonjiinVowelCodes = setOf(12643, 183, 12641)

/** 미리보기에 쓰는 테마 색. [byCode]는 특정 키(엔터, 시프트 등)의 배경·글자색이다. */
data class PreviewPalette(
    val background: Color,
    val keyBackground: Color,
    val keyForeground: Color,
    val byCode: Map<Int, Pair<Color, Color>> = emptyMap(),
) {
    fun colorsFor(code: Int, isCheonjiin: Boolean = true): Pair<Color, Color> {
        // 테마의 모음키 강조(ㅣ·ㆍ·ㅡ)는 천지인 전용이라, 같은 코드를 쓰는 연타형 ㅣㅡ 키에는 주지 않는다.
        if (!isCheonjiin && code in CheonjiinVowelCodes) return keyBackground to keyForeground
        return byCode[code] ?: (keyBackground to keyForeground)
    }
}

/** 말랑키 기본 크림색 테마. */
val DefaultPreviewPalette = PreviewPalette(
    background = Color(0xFFFCF5D6),
    keyBackground = Color(0xFFFCF5D6),
    keyForeground = Color(0xFF311D18),
    byCode = mapOf(
        KeyCode.ENTER to (Color(0xFF311D18) to Color(0xFFFCF5D6)),
        KeyCode.SHIFT to (Color(0xFFE8E1C3) to Color(0xFF311D18)),
        KeyCode.DELETE to (Color(0xFFE8E1C3) to Color(0xFF311D18)),
    ),
)

/**
 * 테마 스타일시트(assets/ime/theme/[extId]/stylesheets/[compId].json)에서 키보드 배경과
 * 키 색을 읽는다. 읽지 못하면 null.
 */
fun loadPreviewPalette(context: Context, extId: String, compId: String): PreviewPalette? = runCatching {
    val text = context.assets.open("ime/theme/$extId/stylesheets/$compId.json").bufferedReader().use { it.readText() }
    val root = Json.parseToJsonElement(text).jsonObject
    val defines = root["@defines"]?.jsonObject
        ?.mapValues { it.value.jsonPrimitive.content }
        ?: emptyMap()

    fun resolve(value: String?): Color? {
        val raw = value?.trim() ?: return null
        val resolved = if (raw.startsWith("var(")) defines[raw.removePrefix("var(").removeSuffix(")").trim()] else raw
        return resolved?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    }

    fun JsonObject.prop(name: String): String? = this[name]?.jsonPrimitive?.content

    val keyboard = root["keyboard"]?.jsonObject
    val key = root["key"]?.jsonObject
    val background = resolve(keyboard?.prop("background")) ?: return@runCatching null
    val keyBackground = resolve(key?.prop("background")) ?: return@runCatching null
    val keyForeground = resolve(key?.prop("foreground")) ?: return@runCatching null
    val codeRule = Regex("""^key\[code=(-?\d+)]$""")
    val byCode = root.entries.mapNotNull { (selector, rule) ->
        val code = codeRule.find(selector)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
        val obj = rule.jsonObject
        code to ((resolve(obj.prop("background")) ?: keyBackground) to (resolve(obj.prop("foreground")) ?: keyForeground))
    }.toMap()
    PreviewPalette(background, keyBackground, keyForeground, byCode)
}.getOrNull()

/** 사용자가 고른 커스텀 색으로 만든 미리보기 색. 고르지 않은 색은 기본 크림 테마를 따른다. */
@Composable
fun rememberCustomPreviewPalette(): PreviewPalette {
    val prefs by FlorisPreferenceStore
    val customBg by prefs.malang.customKeyboardBgColor.collectAsState()
    val customKey by prefs.malang.customKeyBgColor.collectAsState()
    val customText by prefs.malang.customKeyTextColor.collectAsState()
    val customSpecial by prefs.malang.customEnterKeyBgColor.collectAsState()
    val customSpecialText by prefs.malang.customEnterKeyTextColor.collectAsState()
    val customEnter by prefs.malang.customRealEnterKeyBgColor.collectAsState()
    val customEnterText by prefs.malang.customRealEnterKeyTextColor.collectAsState()
    return remember(customBg, customKey, customText, customSpecial, customSpecialText, customEnter, customEnterText) {
        fun Color.or(fallback: Color) = if (this == Color.Unspecified) fallback else this
        val d = DefaultPreviewPalette
        val special = customSpecial.or(Color(0xFF311D18)) to customSpecialText.or(d.keyBackground)
        PreviewPalette(
            background = customBg.or(d.background),
            keyBackground = customKey.or(d.keyBackground),
            keyForeground = customText.or(d.keyForeground),
            byCode = mapOf(
                KeyCode.ENTER to (customEnter.or(Color(0xFF5D4037)) to customEnterText.or(Color.White)),
                KeyCode.SHIFT to special,
                KeyCode.DELETE to special,
                KeyCode.VIEW_SYMBOLS to special,
                KeyCode.VIEW_CHARACTERS to special,
            ),
        )
    }
}

/** 지금 키보드에 적용된 테마의 미리보기 색. 커스텀 테마면 사용자가 고른 색을 쓴다. */
@Composable
fun rememberActivePreviewPalette(): PreviewPalette {
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val mode by prefs.theme.mode.collectAsState()
    val dayThemeId by prefs.theme.dayThemeId.collectAsState()
    val nightThemeId by prefs.theme.nightThemeId.collectAsState()
    val isNight = mode == ThemeMode.ALWAYS_NIGHT ||
        (mode == ThemeMode.FOLLOW_SYSTEM && isSystemInDarkTheme())
    val themeId = if (isNight) nightThemeId else dayThemeId
    val custom = rememberCustomPreviewPalette()

    return remember(themeId, custom) {
        if (themeId.componentId == "custom") {
            custom
        } else {
            loadPreviewPalette(context, themeId.extensionId, themeId.componentId) ?: DefaultPreviewPalette
        }
    }
}

/**
 * [subtype]의 실제 글자 자판을 작게 그린다. 자판 계산이 끝나기 전에는 빈 판만 보인다.
 */
@Composable
fun KeyboardPreview(
    subtype: Subtype,
    palette: PreviewPalette,
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val keyboard by produceState<TextKeyboard?>(initialValue = null, subtype) {
        value = runCatching { keyboardManager.computePreviewKeyboard(subtype) }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.background)
            .padding(3.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val isCheonjiin = subtype.layoutMap.characters.componentId.contains("cheonjiin")
        val rows = keyboard?.arrangement
            ?.map { row -> row.filter { it.flayWidthFactor > 0f } }
            ?.filter { it.isNotEmpty() }
            ?: return@Column
        // 가장 넓은 줄을 기준으로, 좁은 줄은 가운데로 모은다 (실제 자판 배치와 같은 방식).
        val fullWidth = rows.maxOf { row -> row.sumOf { it.flayWidthFactor.toDouble() } }.toFloat()
        for (row in rows) {
            val rowWidth = row.sumOf { it.flayWidthFactor.toDouble() }.toFloat()
            val growSum = row.sumOf { it.flayGrow.toDouble() }.toFloat()
            val extra = fullWidth - rowWidth
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (growSum == 0f && extra > 0.01f) Spacer(modifier = Modifier.weight(extra / 2))
                for (key in row) {
                    val width = key.flayWidthFactor + if (growSum > 0f) extra * key.flayGrow / growSum else 0f
                    PreviewKey(key, palette, isCheonjiin, Modifier.weight(width))
                }
                if (growSum == 0f && extra > 0.01f) Spacer(modifier = Modifier.weight(extra / 2))
            }
        }
    }
}

@Composable
private fun PreviewKey(key: TextKey, palette: PreviewPalette, isCheonjiin: Boolean, modifier: Modifier) {
    val (bg, fg) = remember(key, palette, isCheonjiin) { palette.colorsFor(key.computedData.code, isCheonjiin) }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            // 키 배경이 판 배경과 같은 테마에서도 키 경계가 보이게 한다.
            .border(0.5.dp, fg.copy(alpha = 0.12f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val icon = key.foregroundImageVector
        val label = key.label
        when {
            icon != null -> Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(10.dp))
            !label.isNullOrBlank() -> Text(
                label,
                color = fg,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
