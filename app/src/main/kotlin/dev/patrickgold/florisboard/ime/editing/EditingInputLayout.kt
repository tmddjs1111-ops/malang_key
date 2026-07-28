/*
 * Copyright (C) 2025-2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.editing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.ImeUiMode
import dev.patrickgold.florisboard.ime.keyboard.FlorisImeSizing
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.media.KeyboardLikeButton
import dev.patrickgold.florisboard.ime.text.keyboard.TextKeyData
import dev.patrickgold.florisboard.ime.theme.FlorisImeUi
import dev.patrickgold.florisboard.keyboardManager
import org.florisboard.lib.compose.autoMirrorForRtl
import org.florisboard.lib.compose.stringRes
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText

@Composable
fun EditingInputLayout(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()

    SnyggColumn(
        elementName = FlorisImeUi.Editing.elementName,
        modifier = modifier
            .fillMaxWidth()
            .height(FlorisImeSizing.imeUiHeight()),
    ) {
        SnyggRow(
            elementName = FlorisImeUi.ClipboardHeader.elementName,
            modifier = Modifier
                .fillMaxWidth()
                .height(FlorisImeSizing.smartbarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sizeModifier = Modifier
                .sizeIn(maxHeight = FlorisImeSizing.smartbarHeight)
                .aspectRatio(1f)
            SnyggIconButton(
                elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                onClick = { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT },
                modifier = sizeModifier,
            ) {
                SnyggIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                )
            }
            SnyggText(
                elementName = FlorisImeUi.ClipboardHeaderText.elementName,
                modifier = Modifier.weight(1f),
                text = stringRes(R.string.quick_action__ime_ui_mode_editing),
            )
            SnyggIconButton(
                elementName = FlorisImeUi.ClipboardHeaderButton.elementName,
                onClick = { keyboardManager.activeState.imeUiMode = ImeUiMode.TEXT },
                modifier = sizeModifier.autoMirrorForRtl(),
            ) {
                SnyggIcon(
                    imageVector = Icons.Default.Done,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(4.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                EditingButton(TextKeyData.MOVE_START_OF_LINE)
                EditingButton(TextKeyData.ARROW_UP)
                EditingButton(TextKeyData.MOVE_END_OF_LINE)
                EditingButton(TextKeyData.CLIPBOARD_SELECT_ALL)
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                EditingButton(TextKeyData.ARROW_LEFT)
                EditingButton(TextKeyData.CLIPBOARD_SELECT)
                EditingButton(TextKeyData.ARROW_RIGHT)
                EditingButton(TextKeyData.CLIPBOARD_COPY)
            }
            Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                EditingButton(TextKeyData.MOVE_START_OF_PAGE)
                EditingButton(TextKeyData.ARROW_DOWN)
                EditingButton(TextKeyData.MOVE_END_OF_PAGE)
                EditingButton(TextKeyData.CLIPBOARD_PASTE)
            }
        }
    }
}

@Composable
private fun RowScope.EditingButton(
    keyData: KeyData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val evaluator by keyboardManager.activeEvaluator.collectAsState()
    val imageVector = evaluator.computeImageVector(keyData)
    val label = evaluator.computeLabel(keyData)

    KeyboardLikeButton(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(2.dp),
        inputEventDispatcher = keyboardManager.inputEventDispatcher,
        keyData = keyData,
        elementName = FlorisImeUi.EditingButton.elementName,
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        } else if (label != null) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
