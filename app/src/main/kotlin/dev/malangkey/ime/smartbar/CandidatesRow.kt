/*
 * Copyright (C) 2024-2025 The FlorisBoard Contributors
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

package dev.malangkey.ime.smartbar

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.FlorisImeSizing
import dev.malangkey.ime.nlp.ClipboardSuggestionCandidate
import dev.malangkey.ime.nlp.SuggestionCandidate
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.keyboardManager
import dev.malangkey.nlpManager
import dev.malangkey.subtypeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import org.florisboard.lib.compose.conditional
import org.florisboard.lib.compose.florisHorizontalScroll
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggSpacer
import org.florisboard.lib.snygg.ui.SnyggText

val CandidatesRowScrollbarHeight = 2.dp

@Composable
fun CandidatesRow(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = { },
) {
    val prefs by FlorisPreferenceStore
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val nlpManager by context.nlpManager()
    val subtypeManager by context.subtypeManager()

    val displayMode by prefs.suggestion.displayMode.collectAsState()
    val candidates by nlpManager.activeCandidatesFlow.collectAsState()

    LaunchedEffect(candidates.size) {
        if (candidates.size <= 3 && expanded) {
            onExpandedChange(false)
        }
    }

    SnyggRow(
        elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
        modifier = modifier.fillMaxSize(),
    ) {
        if (candidates.isNotEmpty()) {
            if (expanded) {
                ExpandedCandidates(
                    candidates = candidates,
                    longPressDelay = prefs.keyboard.longPressDelay.get().toLong(),
                    onCandidateClick = { candidate ->
                        onExpandedChange(false)
                        keyboardManager.commitCandidate(candidate)
                    },
                    onCandidateLongPress = { candidate ->
                        if (candidate.isEligibleForUserRemoval) {
                            nlpManager.removeSuggestion(subtypeManager.activeSubtype, candidate)
                        } else {
                            false
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                )
            } else {
                SnyggRow(
                    elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .conditional(displayMode == CandidatesDisplayMode.DYNAMIC_SCROLLABLE && candidates.size > 1) {
                            florisHorizontalScroll(scrollbarHeight = CandidatesRowScrollbarHeight)
                        },
                    horizontalArrangement = if (candidates.size > 1) Arrangement.Start else Arrangement.Center,
                ) {
                    val list = when (displayMode) {
                        CandidatesDisplayMode.CLASSIC -> candidates.take(3)
                        else -> candidates
                    }
                    for ((n, candidate) in list.withIndex()) {
                        if (n > 0) {
                            CandidateSpacer()
                        }
                        val candidateModifier = if (candidates.size == 1) {
                            Modifier
                                .fillMaxHeight()
                                .weight(1f, fill = false)
                        } else {
                            Modifier
                                .fillMaxHeight()
                                .conditional(displayMode == CandidatesDisplayMode.CLASSIC) {
                                    weight(1f)
                                }
                                .conditional(displayMode != CandidatesDisplayMode.CLASSIC) {
                                    wrapContentWidth().widthIn(max = 160.dp)
                                }
                        }
                        CandidateItem(
                            modifier = candidateModifier,
                            candidate = candidate,
                            displayMode = displayMode,
                            onClick = { keyboardManager.commitCandidate(candidates[n]) },
                            onLongPress = {
                                val candidateItem = candidates[n]
                                if (candidateItem.isEligibleForUserRemoval) {
                                    nlpManager.removeSuggestion(subtypeManager.activeSubtype, candidateItem)
                                } else {
                                    false
                                }
                            },
                            longPressDelay = prefs.keyboard.longPressDelay.get().toLong(),
                        )
                    }
                }
            }
            if (candidates.size > 3) {
                SnyggIconButton(
                    elementName = FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                    onClick = { onExpandedChange(!expanded) },
                    modifier = Modifier
                        .width(FlorisImeSizing.smartbarHeight)
                        .fillMaxHeight(),
                ) {
                    SnyggIcon(
                        elementName = FlorisImeUi.SmartbarExtendedActionsToggle.elementName,
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedCandidates(
    candidates: List<SuggestionCandidate>,
    longPressDelay: Long,
    onCandidateClick: (SuggestionCandidate) -> Unit,
    onCandidateLongPress: (SuggestionCandidate) -> Boolean,
    modifier: Modifier = Modifier,
) {
    SnyggColumn(
        elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
        modifier = modifier,
    ) {
        for (row in candidates.take(8).chunked(4)) {
            SnyggRow(
                elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.Start,
            ) {
                for ((n, candidate) in row.withIndex()) {
                    if (n > 0) {
                        CandidateSpacer()
                    }
                    CandidateItem(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        candidate = candidate,
                        displayMode = CandidatesDisplayMode.CLASSIC,
                        onClick = { onCandidateClick(candidate) },
                        onLongPress = { onCandidateLongPress(candidate) },
                        longPressDelay = longPressDelay,
                    )
                }
                repeat(4 - row.size) {
                    SnyggBox(
                        elementName = FlorisImeUi.SmartbarCandidatesRow.elementName,
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                    ) { }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CandidateSpacer() {
    SnyggSpacer(
        elementName = FlorisImeUi.SmartbarCandidateSpacer.elementName,
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.6f)
            .align(Alignment.CenterVertically),
    )
}

@Composable
private fun CandidateItem(
    candidate: SuggestionCandidate,
    displayMode: CandidatesDisplayMode,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    onLongPress: () -> Boolean = { false },
    longPressDelay: Long,
) = with(LocalDensity.current) {
    var isPressed by remember { mutableStateOf(false) }

    val elementName = if (candidate is ClipboardSuggestionCandidate) {
        FlorisImeUi.SmartbarCandidateClip
    } else {
        FlorisImeUi.SmartbarCandidateWord
    }.elementName
    val attributes = mapOf("auto-commit" to if (candidate.isEligibleForAutoCommit) 1 else 0)
    val selector = if (isPressed) SnyggSelector.PRESSED else SnyggSelector.NONE

    SnyggRow(
        elementName = elementName,
        attributes = attributes,
        selector = selector,
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    isPressed = true
                    if (down.pressed != down.previousPressed) down.consume()
                    var upOrCancel: PointerInputChange? = null
                    try {
                        upOrCancel = withTimeout(longPressDelay) {
                            waitForUpOrCancellation()
                        }
                        upOrCancel?.let { if (it.pressed != it.previousPressed) it.consume() }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        if (onLongPress()) {
                            upOrCancel = null
                            isPressed = false
                        }
                        waitForUpOrCancellation()?.let { if (it.pressed != it.previousPressed) it.consume() }
                    }
                    if (upOrCancel != null) {
                        onClick()
                    }
                    isPressed = false
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (candidate.icon != null) {
            SnyggBox(
                elementName = "$elementName-icon",
                attributes = attributes,
                selector = selector,
            ) {
                SnyggIcon(imageVector = candidate.icon!!)
            }
        }
        SnyggColumn(
            modifier = if (displayMode == CandidatesDisplayMode.CLASSIC) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SnyggText(
                elementName = "$elementName-text",
                attributes = attributes,
                selector = selector,
                text = candidate.text.toString(),
            )
            if (candidate.secondaryText != null) {
                SnyggText(
                    elementName = "$elementName-secondary-text",
                    attributes = attributes,
                    selector = selector,
                    text = candidate.secondaryText!!.toString(),
                )
            }
        }
    }
}
