/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.malangkey.ime.smartbar.quickaction

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import dev.malangkey.R
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.FlorisImeSizing
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKeyData
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.keyboardManager
import kotlinx.coroutines.runBlocking
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

private val NoopAction = QuickAction.InsertKey(TextKeyData(code = KeyCode.NOOP))

@Composable
fun QuickActionsEditorPanel() {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val evaluator by keyboardManager.activeSmartbarEvaluator.collectAsState()

    var malangSlotsCount by remember { mutableStateOf(prefs.smartbar.malangSlotsCount.get().coerceIn(3, 6)) }
    val malangSlots = remember {
        val list = prefs.smartbar.malangSlots.get().toMutableStateList()
        while (list.size < 6) list.add(NoopAction)
        list as androidx.compose.runtime.snapshots.SnapshotStateList<QuickAction>
    }

    val availableActions = remember {
        listOf(
            QuickAction.InsertKey(TextKeyData.UNDO),
            QuickAction.InsertKey(TextKeyData.REDO),
            QuickAction.InsertKey(TextKeyData.SETTINGS),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_CLIPBOARD),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_MEDIA),
            QuickAction.InsertKey(TextKeyData.IME_UI_MODE_EDITING),
            QuickAction.InsertKey(TextKeyData.VOICE_INPUT),
            QuickAction.InsertKey(TextKeyData.LANGUAGE_SWITCH),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_COPY),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_PASTE),
            QuickAction.InsertKey(TextKeyData.CLIPBOARD_SELECT_ALL),
            QuickAction.InsertKey(TextKeyData.ARROW_LEFT),
            QuickAction.InsertKey(TextKeyData.ARROW_RIGHT),
            QuickAction.QuickPhrase(""), // Lightning bolt template
        )
    }

    var activeDragAction by remember { mutableStateOf<QuickAction?>(null) }
    var activeDragPosition by remember { mutableStateOf(IntOffset.Zero) }
    var activeDragSize by remember { mutableStateOf(IntSize.Zero) }

    var phraseToEditIndex by remember { mutableStateOf<Int?>(null) }
    var phraseText by remember { mutableStateOf("") }

    val slotPositions = remember { mutableMapOf<Int, Offset>() }
    var slotSize by remember { mutableStateOf(IntSize.Zero) }
    val gridState = rememberLazyGridState()

    DisposableEffect(Unit) {
        onDispose {
            runBlocking {
                prefs.smartbar.malangSlots.set(malangSlots.toList())
                prefs.smartbar.malangSlotsCount.set(malangSlotsCount)
            }
        }
    }

    SnyggColumn(FlorisImeUi.SmartbarActionsEditor.elementName, modifier = Modifier.safeDrawingPadding().fillMaxSize()) {
        // Header
        SnyggRow(
            elementName = FlorisImeUi.SmartbarActionsEditorHeader.elementName,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                SnyggIconButton(
                    elementName = FlorisImeUi.SmartbarActionsEditorHeaderButton.elementName,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                    onClick = {
                        keyboardManager.activeState.isActionsEditorVisible = false
                    },
                ) {
                    SnyggIcon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft)
                }
            }
            SnyggText(
                modifier = Modifier.weight(1f),
                text = "상단바 편집 (말랑키 스타일)",
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SnyggIconButton(onClick = { if (malangSlotsCount > 3) malangSlotsCount-- }) {
                    SnyggText(text = "-")
                }
                SnyggText(text = malangSlotsCount.toString())
                SnyggIconButton(onClick = { if (malangSlotsCount < 6) malangSlotsCount++ }) {
                    SnyggText(text = "+")
                }
            }
        }

        // Preview Area (Drop Target)
        SnyggBox(
            elementName = FlorisImeUi.Smartbar.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight)
                .padding(horizontal = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0 until malangSlotsCount) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned {
                                slotPositions[i] = it.positionInRoot()
                                slotSize = it.size
                            }
                            .pointerInput(i) {
                                detectTapGestures(
                                    onLongPress = {
                                        activeDragAction = malangSlots[i]
                                        malangSlots[i] = NoopAction
                                        activeDragSize = slotSize
                                        activeDragPosition = slotPositions[i]?.let { IntOffset(it.x.toInt(), it.y.toInt()) } ?: IntOffset.Zero
                                    },
                                    onTap = {
                                        val action = malangSlots[i]
                                        if (action is QuickAction.QuickPhrase) {
                                            phraseToEditIndex = i
                                            phraseText = action.text
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        QuickActionButton(malangSlots[i], evaluator, type = QuickActionBarType.EDITOR_TILE)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Subheader(text = "사용 가능한 기능 (드래그하여 위로 올리세요)")

        // Available Actions Area
        LazyVerticalGrid(
            columns = GridCells.Adaptive(FlorisImeSizing.smartbarHeight * 1.5f),
            modifier = Modifier.weight(1f),
            state = gridState,
        ) {
            itemsIndexed(availableActions) { index, action ->
                Box(
                    modifier = Modifier
                        .pointerInput(action) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    activeDragAction = action
                                    activeDragPosition = gridState.layoutInfo.visibleItemsInfo.find { it.index == index }?.offset ?: IntOffset.Zero
                                    activeDragSize = gridState.layoutInfo.visibleItemsInfo.find { it.index == index }?.size ?: IntSize.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    activeDragPosition += IntOffset(dragAmount.x.toInt(), dragAmount.y.toInt())
                                },
                                onDragEnd = {
                                    // Check if dropped in a slot
                                    var dropped = false
                                    for (i in 0 until malangSlotsCount) {
                                        val pos = slotPositions[i] ?: continue
                                        val rectX = pos.x .. pos.x + slotSize.width
                                        val rectY = pos.y .. pos.y + slotSize.height
                                        if (activeDragPosition.x.toFloat() in rectX && activeDragPosition.y.toFloat() in rectY) {
                                            malangSlots[i] = activeDragAction!!
                                            if (activeDragAction is QuickAction.QuickPhrase) {
                                                phraseToEditIndex = i
                                                phraseText = ""
                                            }
                                            dropped = true
                                            break
                                        }
                                    }
                                    activeDragAction = null
                                },
                                onDragCancel = { activeDragAction = null }
                            )
                        }
                ) {
                    QuickActionButton(action, evaluator, type = QuickActionBarType.EDITOR_TILE)
                }
            }
        }

        // Phrase Edit Dialog
        if (phraseToEditIndex != null) {
            AlertDialog(
                onDismissRequest = { phraseToEditIndex = null },
                title = { Text("상용구 입력") },
                text = {
                    OutlinedTextField(
                        value = phraseText,
                        onValueChange = { phraseText = it },
                        label = { Text("입력할 문구") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val i = phraseToEditIndex!!
                        malangSlots[i] = QuickAction.QuickPhrase(phraseText)
                        phraseToEditIndex = null
                    }) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { phraseToEditIndex = null }) {
                        Text("취소")
                    }
                }
            )
        }

        // Dragging Shadow
        if (activeDragAction != null) {
            val size = with(LocalDensity.current) {
                activeDragSize.toSize().toDpSize()
            }
            Box(modifier = Modifier.offset { activeDragPosition }) {
                QuickActionButton(
                    modifier = Modifier.size(size),
                    action = activeDragAction!!,
                    evaluator = evaluator,
                    type = QuickActionBarType.EDITOR_TILE
                )
            }
        }
    }
}

@Composable
private fun Subheader(text: String, modifier: Modifier = Modifier) {
    SnyggText(
        elementName = FlorisImeUi.SmartbarActionsEditorSubheader.elementName,
        modifier = modifier.fillMaxWidth().padding(8.dp),
        text = text,
    )
}
