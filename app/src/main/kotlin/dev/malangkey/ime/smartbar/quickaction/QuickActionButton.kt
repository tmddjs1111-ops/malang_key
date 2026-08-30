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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.compose.tooltip.PlainTooltip
import dev.malangkey.ime.input.LocalInputFeedbackController
import dev.malangkey.ime.keyboard.ComputingEvaluator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import dev.malangkey.ime.keyboard.computeImageVector
import dev.malangkey.ime.keyboard.computeLabel
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.keyboard.TextKeyData
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.keyboardManager
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggText

enum class QuickActionBarType {
    INTERACTIVE_BUTTON,
    INTERACTIVE_TILE,
    EDITOR_TILE;
}

@Composable
fun QuickActionButton(
    action: QuickAction,
    evaluator: ComputingEvaluator,
    modifier: Modifier = Modifier,
    type: QuickActionBarType = QuickActionBarType.INTERACTIVE_BUTTON,
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val inputFeedbackController = LocalInputFeedbackController.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isEnabled = type == QuickActionBarType.EDITOR_TILE || 
        action.keyData().code == KeyCode.CLIPBOARD_COPY || 
        action.keyData().code == KeyCode.CLIPBOARD_CUT || 
        evaluator.evaluateEnabled(action.keyData())
    val elementName = when (type) {
        QuickActionBarType.INTERACTIVE_BUTTON -> FlorisImeUi.SmartbarActionKey
        QuickActionBarType.INTERACTIVE_TILE -> FlorisImeUi.SmartbarActionTile
        QuickActionBarType.EDITOR_TILE -> FlorisImeUi.SmartbarActionsEditorTile
    }.elementName
    val attributes = mapOf(FlorisImeUi.Attr.Code to action.keyData().code)
    val selector = when {
        isPressed -> SnyggSelector.PRESSED
        !isEnabled -> SnyggSelector.DISABLED
        else -> null
    }

    // Need to manually cancel an action if this composable suddenly leaves the composition to prevent the key from
    // being stuck in the pressed state
    DisposableEffect(action, isEnabled) {
        onDispose {
            if (action is QuickAction.InsertKey) {
                action.onPointerCancel(context)
            }
        }
    }

    PlainTooltip(action.computeTooltip(evaluator), enabled = false) {
        SnyggBox(
            elementName = elementName,
            attributes = attributes,
            selector = selector,
            modifier = modifier,
            clickAndSemanticsModifier = Modifier
                .aspectRatio(1f)
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(action, isEnabled, onLongClick) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        if (isEnabled && type != QuickActionBarType.EDITOR_TILE) {
                            val press = PressInteraction.Press(down.position)
                            inputFeedbackController.keyPress(TextKeyData.UNSPECIFIED)
                            interactionSource.tryEmit(press)
                            action.onPointerDown(context)
                            
                            if (action.keyData().code == KeyCode.IME_UI_MODE_EDITING) {
                                var accumulatedX = 0f
                                val stepThresholdPx = 12.dp.toPx()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (change.isConsumed || !change.pressed) {
                                        break
                                    }
                                    val deltaX = change.position.x - change.previousPosition.x
                                    accumulatedX += deltaX
                                    while (accumulatedX >= stepThresholdPx) {
                                        keyboardManager.inputEventDispatcher.sendDown(TextKeyData.ARROW_RIGHT)
                                        keyboardManager.inputEventDispatcher.sendUp(TextKeyData.ARROW_RIGHT)
                                        inputFeedbackController.keyPress(TextKeyData.ARROW_RIGHT)
                                        accumulatedX -= stepThresholdPx
                                    }
                                    while (accumulatedX <= -stepThresholdPx) {
                                        keyboardManager.inputEventDispatcher.sendDown(TextKeyData.ARROW_LEFT)
                                        keyboardManager.inputEventDispatcher.sendUp(TextKeyData.ARROW_LEFT)
                                        inputFeedbackController.keyPress(TextKeyData.ARROW_LEFT)
                                        accumulatedX += stepThresholdPx
                                    }
                                    change.consume()
                                }
                                interactionSource.tryEmit(PressInteraction.Release(press))
                                action.onPointerUp(context)
                            } else {
                                var isLongClickTriggered = false
                                val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    waitForUpOrCancellation()
                                }
                                if (up == null) {
                                    if (onLongClick != null) {
                                        onLongClick()
                                        isLongClickTriggered = true
                                        inputFeedbackController.keyLongPress(TextKeyData.UNSPECIFIED)
                                    }
                                    waitForUpOrCancellation()
                                }
                                if (isLongClickTriggered) {
                                    interactionSource.tryEmit(PressInteraction.Cancel(press))
                                    action.onPointerCancel(context)
                                } else {
                                    interactionSource.tryEmit(PressInteraction.Release(press))
                                    action.onPointerUp(context)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Render foreground
                when (action) {
                    is QuickAction.InsertKey -> {
                        val (imageVector, label) = remember(action, evaluator) {
                            evaluator.computeImageVector(action.data) to evaluator.computeLabel(action.data)
                        }
                        if (imageVector != null) {
                            SnyggBox(
                                elementName = "$elementName-icon",
                                attributes = attributes,
                                selector = selector,
                            ) {
                                SnyggIcon(imageVector = imageVector)
                            }
                        } else if (label != null) {
                            SnyggText(
                                elementName = "$elementName-text",
                                attributes = attributes,
                                selector = selector,
                                text = label,
                            )
                        }
                    }

                    is QuickAction.InsertText -> {
                        SnyggText(
                            elementName = "$elementName-text",
                            attributes = attributes,
                            selector = selector,
                            text = action.data.let { if (it.length <= 3) it else it.take(2) + "…" }.ifBlank { "T" },
                        )
                    }

                    is QuickAction.QuickPhrase -> {
                        SnyggBox(
                            elementName = "$elementName-icon",
                            attributes = attributes,
                            selector = selector,
                        ) {
                            SnyggIcon(imageVector = Icons.Default.Bolt)
                        }
                    }
                }

                // Render additional info if this is a tile
                if (type != QuickActionBarType.INTERACTIVE_BUTTON) {
                    SnyggText(
                        elementName = "$elementName-text",
                        attributes = attributes,
                        selector = selector,
                        text = action.computeDisplayName(evaluator = evaluator),
                    )
                }
            }
        }
    }
}
