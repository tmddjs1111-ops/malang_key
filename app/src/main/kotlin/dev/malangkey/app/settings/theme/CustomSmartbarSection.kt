package dev.malangkey.app.settings.theme

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.apptheme.MalangText
import dev.malangkey.ime.keyboard.computeImageVector
import dev.malangkey.ime.smartbar.SmartbarLayout
import dev.malangkey.ime.smartbar.quickaction.QuickAction
import dev.malangkey.ime.smartbar.quickaction.computeDisplayName
import dev.malangkey.ime.theme.CustomSmartbarIcons
import dev.malangkey.ime.theme.rememberCustomSmartbarIcon
import dev.malangkey.keyboardManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch

/**
 * 커스텀 테마의 스마트바 아이콘. 버튼마다 기본 아이콘 대신 쓸 사진을 고른다.
 * 스마트바 배경·아이콘 색은 [CustomColorsSection]에서 고른다.
 */
@Composable
fun CustomSmartbarSection() {
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val scope = rememberCoroutineScope()
    val keyboardManager by context.keyboardManager()
    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()
    val layout by prefs.smartbar.layout.collectAsState()
    val slots by prefs.smartbar.malangSlots.collectAsState()
    val slotsCount by prefs.smartbar.malangSlotsCount.collectAsState()
    val arrangement by prefs.smartbar.actionArrangement.collectAsState()
    val iconsValue by prefs.malang.customSmartbarIcons.collectAsState()
    val icons = remember(iconsValue) { CustomSmartbarIcons.decode(iconsValue) }

    // 지금 스마트바에 보이는 버튼들. 말랑 칸 배치면 칸 순서대로, 아니면 고정 버튼과 나머지 버튼.
    val actions = remember(layout, slots, slotsCount, arrangement) {
        if (layout == SmartbarLayout.MALANG_SLOTS) {
            slots.take(slotsCount)
        } else {
            listOfNotNull(arrangement.stickyAction) + arrangement.dynamicActions
        }.distinct()
    }

    var importing by remember { mutableStateOf(false) }
    // 사진을 고르는 중인 버튼, 사진을 바꿀지 지울지 묻는 중인 버튼.
    var pickingFor by remember { mutableStateOf<QuickAction?>(null) }
    var askingFor by remember { mutableStateOf<QuickAction?>(null) }

    suspend fun saveIcons(newIcons: Map<String, String>) {
        prefs.malang.customSmartbarIcons.set(CustomSmartbarIcons.encode(newIcons))
        CustomSmartbarIcons.deleteAllExcept(context, newIcons.values)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val action = pickingFor
        pickingFor = null
        if (uri == null || action == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch {
            CustomSmartbarIcons.import(context, uri)
                .onSuccess { fileName ->
                    saveIcons(icons + (CustomSmartbarIcons.actionKey(action) to fileName))
                    // 처음 설치한 상태(시스템 따라가기)에서도 사진 아이콘이 보이게 커스텀으로 고정한다.
                    applyMalangTheme(prefs, malangThemes.first { it.compId == "custom" })
                }
                .onFailure { Toast.makeText(context, "사진을 불러오지 못했어요.", Toast.LENGTH_SHORT).show() }
            importing = false
        }
    }

    fun pickFor(action: QuickAction) {
        val key = CustomSmartbarIcons.actionKey(action)
        if (key !in icons && icons.size >= CustomSmartbarIcons.MaxIcons) {
            Toast.makeText(context, "사진 아이콘은 ${CustomSmartbarIcons.MaxIcons}개까지 넣을 수 있어요.", Toast.LENGTH_SHORT).show()
            return
        }
        pickingFor = action
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            "스마트바 아이콘",
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            color = MalangSettingsSection,
            fontFamily = MalangJuaFont,
            fontSize = 23.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MalangSettingsCard)
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
                .padding(16.dp),
        ) {
            Text(
                if (importing) "사진을 불러오는 중…" else "버튼을 눌러 사진 아이콘을 넣어 보세요. 사진 가운데를 정사각형으로 잘라 써요.",
                color = MalangText,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 한 줄에 다섯 개씩 놓는다.
            for (row in actions.chunked(5)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for ((index, action) in row.withIndex()) {
                        val number = actions.indexOf(action) + 1
                        SmartbarIconTile(
                            action = action,
                            number = number,
                            hasImage = CustomSmartbarIcons.actionKey(action) in icons,
                            evaluatorIcon = (action as? QuickAction.InsertKey)?.let { evaluator.computeImageVector(it.data) },
                            label = action.computeDisplayName(evaluator),
                            enabled = !importing,
                            onClick = {
                                if (CustomSmartbarIcons.actionKey(action) in icons) askingFor = action else pickFor(action)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        if (index == row.lastIndex) {
                            // 마지막 줄이 덜 차도 칸 크기를 맞춘다.
                            repeat(5 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
            if (icons.isNotEmpty()) {
                MalangButton(
                    "사진 아이콘 모두 지우기",
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    primary = false,
                    enabled = !importing,
                    onClick = { scope.launch { CustomSmartbarIcons.reset(context, prefs) } },
                )
            }
        }
    }

    askingFor?.let { action ->
        AlertDialog(
            onDismissRequest = { askingFor = null },
            containerColor = MalangSettingsCard,
            title = { Text(action.computeDisplayName(evaluator), color = MalangSettingsTitle, fontFamily = MalangJuaFont, fontSize = 20.sp) },
            text = { Text("이 버튼의 사진 아이콘을 바꾸거나 기본 아이콘으로 되돌릴 수 있어요.", color = MalangSettingsSummary) },
            confirmButton = {
                TextButton(onClick = {
                    askingFor = null
                    pickFor(action)
                }) { Text("사진 바꾸기", color = MalangSettingsSection) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askingFor = null
                    scope.launch { saveIcons(icons - CustomSmartbarIcons.actionKey(action)) }
                }) { Text("기본 아이콘으로", color = MalangSettingsSummary) }
            },
        )
    }
}

@Composable
private fun SmartbarIconTile(
    action: QuickAction,
    number: Int,
    hasImage: Boolean,
    evaluatorIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val customIcon = if (hasImage) rememberCustomSmartbarIcon(action) else null
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF6EFE6))
                .border(
                    width = if (hasImage) 2.dp else 1.dp,
                    color = if (hasImage) MalangSettingsSection else MalangSettingsBorder,
                    shape = RoundedCornerShape(12.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                customIcon != null -> Image(
                    bitmap = customIcon,
                    contentDescription = label,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)),
                )
                evaluatorIcon != null -> Icon(evaluatorIcon, contentDescription = label, tint = MalangText)
                action is QuickAction.QuickPhrase -> Icon(Icons.Default.Bolt, contentDescription = label, tint = MalangText)
                action is QuickAction.InsertText -> Text(action.data.take(2), color = MalangText, fontSize = 14.sp)
                else -> Text(label.take(2), color = MalangText, fontSize = 12.sp)
            }
        }
        Text(
            "${number}번",
            modifier = Modifier.padding(top = 4.dp),
            color = MalangSettingsSummary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
