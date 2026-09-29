/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.keyboard.LayoutType
import dev.malangkey.keyboardManager
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangInfoCard
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.subtypeManager

private val ItemHeight = 68.dp

/**
 * Lists the enabled keyboards; dragging the handle changes the order in which the language
 * switch key and swipe gestures cycle through them.
 */
@Composable
fun KeyboardOrderScreen() = MalangSettingsScreen(title = "내 키보드 순서", subtitle = "Keyboard Order") {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val subtypeManager by context.subtypeManager()

    run {
        val subtypes by subtypeManager.subtypesFlow.collectAsState()
        val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
        val layouts by keyboardManager.resources.layouts.collectAsState()
        val itemHeightPx = with(LocalDensity.current) { (ItemHeight + 8.dp).toPx() }

        // Local copy that follows the finger; saved once the drag ends.
        var order by remember(subtypes) { mutableStateOf(subtypes) }
        var draggingId by remember { mutableStateOf<Long?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }

        fun finishDrag() {
            if (draggingId != null && order.map { it.id } != subtypes.map { it.id }) {
                subtypeManager.reorderSubtypes(order)
            }
            draggingId = null
            dragOffset = 0f
        }

        MalangInfoCard("오른쪽 ≡ 를 끌어서 순서를 바꾸세요. 지구본 키와 스페이스바 좌우 밀기로 이 순서대로 전환됩니다.")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            order.forEachIndexed { index, subtype ->
                key(subtype.id) {
                    val isDragging = draggingId == subtype.id
                    KeyboardOrderItem(
                        position = index + 1,
                        label = subtypeLabel(subtype, layouts),
                        isActive = subtype.id == activeSubtype.id,
                        isDragging = isDragging,
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .graphicsLayer { translationY = if (isDragging) dragOffset else 0f },
                        handleModifier = Modifier.pointerInput(subtype.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingId = subtype.id
                                    dragOffset = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    // Swap with the neighbour once the item is dragged past half its height.
                                    val current = order.indexOfFirst { it.id == draggingId }
                                    if (current < 0) return@detectDragGestures
                                    if (dragOffset > itemHeightPx / 2 && current < order.lastIndex) {
                                        order = order.swapped(current, current + 1)
                                        dragOffset -= itemHeightPx
                                    } else if (dragOffset < -itemHeightPx / 2 && current > 0) {
                                        order = order.swapped(current, current - 1)
                                        dragOffset += itemHeightPx
                                    }
                                },
                                onDragEnd = { finishDrag() },
                                onDragCancel = { finishDrag() },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardOrderItem(
    position: Int,
    label: String,
    isActive: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ItemHeight)
            .shadow(if (isDragging) 6.dp else 0.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MalangSettingsCard)
            .border(1.dp, if (isDragging) MalangSettingsSection else MalangSettingsBorder, RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$position",
            fontSize = 22.sp,
            fontFamily = JuaFontFamily,
            color = MalangSettingsSection,
            modifier = Modifier.padding(start = 18.dp).width(30.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MalangSettingsTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isActive) {
                Text(
                    text = "사용 중",
                    fontSize = 12.sp,
                    color = MalangSettingsSummary,
                )
            }
        }
        Box(
            modifier = handleModifier
                .fillMaxHeight()
                .width(56.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "순서 바꾸기",
                tint = MalangSettingsSummary,
            )
        }
    }
}

private fun subtypeLabel(
    subtype: Subtype,
    layouts: Map<LayoutType, Map<dev.malangkey.lib.ext.ExtensionComponentName, dev.malangkey.ime.keyboard.LayoutArrangementComponent>>,
): String {
    val layout = layouts[LayoutType.CHARACTERS]?.get(subtype.layoutMap.characters)
    return keyboardDisplayName(subtype.primaryLocale, layout?.label)
}

private fun <T> List<T>.swapped(i: Int, j: Int): List<T> = toMutableList().apply {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}
