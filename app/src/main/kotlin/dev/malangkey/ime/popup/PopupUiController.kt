/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.malangkey.ime.popup

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import dev.malangkey.ime.keyboard.ComputingEvaluator
import dev.malangkey.ime.keyboard.DefaultComputingEvaluator
import dev.malangkey.ime.keyboard.Key
import dev.malangkey.ime.keyboard.KeyData
import dev.malangkey.ime.keyboard.computeImageVector
import dev.malangkey.ime.keyboard.computeLabel
import dev.malangkey.ime.media.emoji.EmojiSet
import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.text.key.KeyHintConfiguration
import dev.malangkey.ime.text.keyboard.TextKey
import dev.malangkey.ime.text.keyboard.TextKeyData
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.lib.FlorisRect
import dev.malangkey.lib.toIntOffset

@Composable
fun rememberPopupUiController(
    key1: Any?,
    key2: Any?,
    boundsProvider: (key: Key) -> FlorisRect,
    isSuitableForBasicPopup: (key: Key) -> Boolean,
    isSuitableForExtendedPopup: (key: Key) -> Boolean,
): PopupUiController {
    val context = LocalContext.current
    return remember(key1, key2) {
        PopupUiController(context, boundsProvider, isSuitableForBasicPopup, isSuitableForExtendedPopup)
    }
}

val ExceptionsForKeyCodes = listOf(
    KeyCode.ENTER,
    KeyCode.LANGUAGE_SWITCH,
    KeyCode.IME_UI_MODE_TEXT,
    KeyCode.IME_UI_MODE_MEDIA,
    KeyCode.IME_UI_MODE_CLIPBOARD,
    KeyCode.IME_UI_MODE_EDITING,
    KeyCode.KANA_SWITCHER,
    KeyCode.CHAR_WIDTH_SWITCHER,
)

@Suppress("unused")
class PopupUiController(
    val context: Context,
    val boundsProvider: (key: Key) -> FlorisRect,
    val isSuitableForBasicPopup: (key: Key) -> Boolean,
    val isSuitableForExtendedPopup: (key: Key) -> Boolean,
) {
    private var baseRenderInfo by mutableStateOf<BaseRenderInfo?>(null)
    private var extRenderInfo by mutableStateOf<ExtRenderInfo?>(null)

    private var activeElementIndex by mutableIntStateOf(-1)
    private var needsInitialTouch = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    var evaluator: ComputingEvaluator = DefaultComputingEvaluator
    var keyHintConfiguration: KeyHintConfiguration = KeyHintConfiguration.HINTS_DISABLED

    /** Is true if the preview popup is visible to the user, else false */
    val isShowingPopup: Boolean
        get() = baseRenderInfo != null
    /** Is true if the extended popup is visible to the user, else false */
    val isShowingExtendedPopup: Boolean
        get() = extRenderInfo != null
    val isClipboardPopupShowing: Boolean
        get() = extRenderInfo?.isClipboard == true

    fun isSuitableForPopups(key: Key): Boolean {
        return isSuitableForBasicPopup(key) || isSuitableForExtendedPopup(key)
    }

    /**
     * Shows a preview popup for the passed [key]. Ignores show requests for keys which
     * key code is equal to or less than [KeyCode.SPACE].
     *
     * @param key Reference to the key currently controlling the popup.
     */
    fun show(key: Key) {
        if (!isSuitableForBasicPopup(key)) return

        baseRenderInfo = BaseRenderInfo(
            key = key,
            bounds = boundsProvider(key),
            shouldIndicateExtendedPopups = when (key) {
                is TextKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).isNotEmpty()
                //is EmojiKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).isNotEmpty()
                else -> false
            },
        )
    }

    /**
     * Extends the currently showing key preview popup if there are popup keys defined in the
     * key data of the passed [key]. Ignores extend requests for key views which key code
     * is equal to or less than [KeyCode.SPACE]. An exception is made for the codes defined in
     * [ExceptionsForKeyCodes], as they most likely have special keys bound to them.
     *
     * Layout of the extended key popup: (n = key.computedPopups.size)
     *   when n <= 5: single line, row0 only
     *     _ _ _ _ _
     *     K K K K K
     *   when n > 5 && n % 2 == 1: multi line, row0 has 1 more key than row1, empty space position
     *     is depending on the current anchor
     *     anchorLeft           anchorRight
     *     K K ... K _         _ K ... K K
     *     K K ... K K         K K ... K K
     *   when n > 5 && n % 2 == 0: multi line, both same length
     *     K K ... K K
     *     K K ... K K
     *
     * @param key Reference to the key currently controlling the popup.
     */
    fun extend(key: Key, size: Size) {
        if (!isSuitableForExtendedPopup(key)) return

        val baseBounds = baseRenderInfo?.bounds ?: boundsProvider(key)
        val keyPopupDiffX = (key.visibleBounds.width - baseBounds.width) / 2.0f

        // Anchor left if keyView is in left half of keyboardView, else anchor right
        val anchorLeft = key.visibleBounds.left < size.width / 2
        val anchorRight = !anchorLeft

        // Determine key counts for each row
        val n = when (key) {
            is TextKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).size
            //is EmojiKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).size
            else -> 0
        }
        if (n <= 0) return
        val (row0count, row1count) = if (n <= 6) {
            n to 0
        } else {
            ((n + 1) / 2) to (n - (n + 1) / 2)
        }

        val density = context.resources.displayMetrics.density
        val minElemWidthPx = 28f * density
        val maxTargetWidthPx = 36f * density
        val targetElemWidth = (baseBounds.width * 0.5f).coerceAtLeast(minElemWidthPx).coerceAtMost(maxTargetWidthPx)
        var elemWidth = targetElemWidth
        if (row0count * elemWidth > size.width) {
            elemWidth = size.width / row0count
        }

        val elemHeight = (baseBounds.height * 0.55f).coerceAtLeast(28f * density).coerceAtMost(34f * density)
        val extWidth = row0count * elemWidth
        val numRows = if (row1count > 0) 2 else 1
        val extHeight = numRows * elemHeight

        val keyCenterX = key.visibleBounds.left + key.visibleBounds.width / 2.0f
        val idealX = if (anchorLeft) {
            keyCenterX - (elemWidth / 2.0f)
        } else {
            keyCenterX - (extWidth - elemWidth / 2.0f)
        }
        val x = idealX.coerceIn(0f, (size.width - extWidth).coerceAtLeast(0f))
        val y = key.visibleBounds.top - extHeight - (4f * density)

        val extBounds = FlorisRect.new(
            left = x, top = y, right = x + extWidth, bottom = y + extHeight,
        )

        val initUiIndex = 0
        val popupIndices: IntArray
        if (key is TextKey) {
            popupIndices = IntArray(n) { uiIndex ->
                if (uiIndex == 0) PopupKeys.FIRST_PRIORITIZED else uiIndex - 1
            }
        } else {
            popupIndices = IntArray(n) { it }
        }

        val elements: List<MutableList<Element>> = if (row1count > 0) {
            listOf(mutableListOf(), mutableListOf())
        } else {
            listOf(mutableListOf())
        }

        val row0Indices = if (anchorRight) (row0count - 1 downTo 0) else (0 until row0count)
        for (uiIndex in row0Indices) {
            if (uiIndex >= n) continue
            val adjustedIndex = popupIndices[uiIndex]
            val keyData = when (key) {
                is TextKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).getOrNull(adjustedIndex) ?: key.computedData
                else -> TextKeyData.UNSPECIFIED
            }
            elements[0].add(Element(
                data = keyData,
                label = evaluator.computeLabel(keyData),
                icon = evaluator.computeImageVector(keyData),
                orderedIndex = uiIndex,
                adjustedIndex = adjustedIndex,
            ))
        }

        if (row1count > 0) {
            val row1Indices = if (anchorRight) (n - 1 downTo row0count) else (row0count until n)
            for (uiIndex in row1Indices) {
                if (uiIndex >= n) continue
                val adjustedIndex = popupIndices[uiIndex]
                val keyData = when (key) {
                    is TextKey -> key.computedPopups.getPopupKeys(keyHintConfiguration).getOrNull(adjustedIndex) ?: key.computedData
                    else -> TextKeyData.UNSPECIFIED
                }
                elements[1].add(Element(
                    data = keyData,
                    label = evaluator.computeLabel(keyData),
                    icon = evaluator.computeImageVector(keyData),
                    orderedIndex = uiIndex,
                    adjustedIndex = adjustedIndex,
                ))
            }
        }

        extRenderInfo = ExtRenderInfo(
            elements = elements,
            baseBounds = baseBounds,
            bounds = extBounds,
            anchorLeft = anchorLeft,
            anchorRight = anchorRight,
            anchorOffset = 0,
            row0count = row0count,
            row1count = row1count,
            elemWidthPx = elemWidth,
            elemHeightPx = elemHeight,
        )
        activeElementIndex = initUiIndex
    }

    fun showClipboardPopup(key: Key, items: List<dev.malangkey.ime.clipboard.provider.ClipboardItem>, size: Size, maxRows: Int) {
        val rowCount = 3
        val colCount = maxRows
        val n = rowCount * colCount
        if (items.isEmpty()) return

        val baseBounds = baseRenderInfo?.bounds ?: boundsProvider(key)
        val keyPopupDiffX = (key.visibleBounds.width - baseBounds.width) / 2.0f

        val elements = List(rowCount) { mutableListOf<Element>() }
        for (i in 0 until n) {
            val visualRowIndex = i / colCount
            val elementsRowIndex = rowCount - 1 - visualRowIndex
            val item = items.getOrNull(i)
            val label = item?.text?.toString() ?: ""
            val displayLabel = if (label.length > 5) label.take(4) + "…" else label
            val keyData = TextKeyData(
                code = KeyCode.MULTIPLE_CODE_POINTS,
                label = label,
                type = dev.malangkey.ime.text.key.KeyType.CHARACTER
            )
            elements[elementsRowIndex].add(Element(
                data = keyData,
                label = displayLabel,
                icon = null,
                orderedIndex = i,
                adjustedIndex = i
            ))
        }

        val extWidth = colCount * baseBounds.width
        val extHeight = rowCount * baseBounds.height * 0.4f

        var x = key.visibleBounds.right - extWidth
        x = x.coerceIn(0f, size.width - extWidth)

        val y = key.visibleBounds.top - extHeight - baseBounds.height * 0.1f

        val extBounds = FlorisRect.new(
            left = x, top = y, right = x + extWidth, bottom = y + extHeight,
        )

        extRenderInfo = ExtRenderInfo(
            elements = elements,
            baseBounds = baseBounds,
            bounds = extBounds,
            anchorLeft = false,
            anchorRight = false,
            anchorOffset = 0,
            row0count = colCount,
            row1count = (rowCount - 1) * colCount,
            isClipboard = true,
        )
        activeElementIndex = -1
        needsInitialTouch = true
        initialTouchX = 0f
        initialTouchY = 0f
    }

    /**
     * Updates the current selected key in extended popup according to the passed [xEvent] and [yEvent].
     * This function does nothing if the extended popup is not showing and will return false.
     *
     * @param key Reference to the key currently controlling the popup.
     * @param xEvent The x coordinate of the MotionEvent.
     * @param yEvent The y coordinate of the MotionEvent.
     *
     * @return True if the pointer movement is within the elements bounds, false otherwise.
     */
    fun propagateMotionEvent(key: Key, xEvent: Float, yEvent: Float): Boolean {
        if (!isShowingExtendedPopup) {
            return false
        }

        val extRenderInfo = extRenderInfo ?: return false
        val baseBounds = extRenderInfo.baseBounds
        val keyPopupDiffX = (key.visibleBounds.width - baseBounds.width) / 2.0f

        val x = xEvent - key.visibleBounds.left
        val y = yEvent - key.visibleBounds.top

        if (extRenderInfo.isClipboard) {
            if (needsInitialTouch) {
                initialTouchX = xEvent
                initialTouchY = yEvent
                needsInitialTouch = false
            }

            val dx = xEvent - initialTouchX
            val dy = yEvent - initialTouchY
            
            val colCount = extRenderInfo.elements.firstOrNull()?.size ?: 1
            
            val colDiff = (dx / (baseBounds.width * 0.6f)).toInt()
            val rowDiff = (dy / (baseBounds.height * 0.4f)).toInt()
            
            val targetCol = (colCount - 1 + colDiff).coerceIn(0, colCount - 1)
            val targetRow = (2 + rowDiff).coerceIn(0, 2)
            
            val actualRow = 2 - targetRow
            val element = extRenderInfo.elements.getOrNull(actualRow)?.getOrNull(targetCol)
            
            if (element != null && element.data.label.isNotEmpty()) {
                activeElementIndex = element.orderedIndex
            } else {
                activeElementIndex = -1
            }
            return true
        }

        val elemWidth = extRenderInfo.elemWidthPx.takeIf { it > 0f } ?: (extRenderInfo.bounds.width / extRenderInfo.row0count)
        val elemHeight = extRenderInfo.elemHeightPx.takeIf { it > 0f } ?: (extRenderInfo.bounds.height / (if (extRenderInfo.row1count > 0) 2 else 1))

        // Apply 1.3x sensitivity multiplier relative to pressed key center
        val keyCenterX = key.visibleBounds.left + key.visibleBounds.width / 2.0f
        val keyCenterY = key.visibleBounds.top + key.visibleBounds.height / 2.0f

        val effectiveX = keyCenterX + (xEvent - keyCenterX) * 1.3f
        val effectiveY = keyCenterY + (yEvent - keyCenterY) * 1.3f

        val relX = effectiveX - extRenderInfo.bounds.left
        val relY = effectiveY - extRenderInfo.bounds.top

        // Never cancel on Y-axis movement while extended popup is active.
        val rowIndex = if (extRenderInfo.row1count > 0) {
            if (relY < elemHeight) 1 else 0
        } else {
            0
        }

        val rowElements = extRenderInfo.elements.getOrNull(rowIndex) ?: extRenderInfo.elements.firstOrNull() ?: return true
        val col = (relX / elemWidth).toInt().coerceIn(0, rowElements.size - 1)
        val selectedElem = rowElements.getOrNull(col)

        if (selectedElem != null) {
            activeElementIndex = selectedElem.orderedIndex
        }

        return true
    }

    /**
     * Gets the [TextKeyData] of the currently active key. May be either the key of the popup preview
     * or one of the keys in extended popup, if shown. Returns null if [key] is not a subclass of [TextKey].
     *
     * @param key Reference to the key currently controlling the popup.
     *
     * @return The [TextKeyData] object of the currently active key or null.
     */
    fun getActiveKeyData(key: Key): KeyData? {
        return if (key is TextKey) {
            val extRenderInfo = extRenderInfo
            if (extRenderInfo != null) {
                val activeElement = extRenderInfo.elements.flatMap { it }.find { it.orderedIndex == activeElementIndex }
                if (extRenderInfo.isClipboard) {
                    activeElement?.data
                } else {
                    activeElement?.data ?: key.computedData
                }
            } else {
                key.computedData
            }
        } else {
            null
        }
    }

    /**
     * Gets the [EmojiSet] of the currently active key. May be either the key of the popup
     * preview or one of the keys in extended popup, if shown. Returns null if [key] is noz a subclass of [EmojiKey].
     *
     * @param key Reference to the key currently controlling the popup.
     * @return The [EmojiSet] object of the currently active key or null.
     */
    fun getActiveEmojiKeyData(key: Key): KeyData? {
        return null
    }

    fun hide() {
        baseRenderInfo = null
        extRenderInfo = null
        activeElementIndex = -1
    }

    private fun getElementOrNull(elements: List<List<Element>>, index: Int): Element? {
        if (index < 0) {
            return null
        }
        var cachedIndex = index
        elements.asReversed().forEach { row ->
            if (cachedIndex >= row.size) {
                cachedIndex -= row.size
            } else {
                return row[cachedIndex]
            }
        }
        return null
    }

    @Composable
    fun RenderPopups(): Unit = with(LocalDensity.current) {
        val attributes = mapOf(
            FlorisImeUi.Attr.Mode to evaluator.keyboard.mode.toString(),
            FlorisImeUi.Attr.ShiftState to evaluator.state.inputShiftState.toString(),
        )
        baseRenderInfo?.let { renderInfo ->
            PopupBaseBox(
                modifier = Modifier
                    .requiredSize(renderInfo.bounds.size.toDpSize())
                    .absoluteOffset { renderInfo.bounds.topLeft.toIntOffset() },
                attributes = attributes,
                key = renderInfo.key,
                shouldIndicateExtendedPopups = renderInfo.shouldIndicateExtendedPopups && extRenderInfo == null,
            )
        }
        extRenderInfo?.let { renderInfo ->
            val elemWidth = renderInfo.elemWidthPx.takeIf { it > 0f } ?: renderInfo.baseBounds.width
            val elemHeight = renderInfo.elemHeightPx.takeIf { it > 0f } ?: (renderInfo.baseBounds.height * 0.4f)
            PopupExtBox(
                modifier = Modifier
                    .requiredSize(renderInfo.bounds.size.toDpSize())
                    .absoluteOffset { renderInfo.bounds.topLeft.toIntOffset() },
                attributes = attributes,
                elements = renderInfo.elements,
                elemArrangement = Arrangement.Center,
                elemWidth = elemWidth.toDp(),
                elemHeight = elemHeight.toDp(),
                activeElementIndex = activeElementIndex,
                isClipboard = renderInfo.isClipboard,
            )
        }
    }

    data class BaseRenderInfo(
        val key: Key,
        val bounds: FlorisRect,
        val shouldIndicateExtendedPopups: Boolean,
    )

    data class ExtRenderInfo(
        val elements: List<List<Element>>,
        val baseBounds: FlorisRect,
        val bounds: FlorisRect,
        val anchorLeft: Boolean,
        val anchorRight: Boolean,
        val anchorOffset: Int,
        val row0count: Int,
        val row1count: Int,
        val isClipboard: Boolean = false,
        val elemWidthPx: Float = 0f,
        val elemHeightPx: Float = 0f,
    )

    data class Element(
        val data: KeyData,
        val label: String?,
        val icon: ImageVector?,
        val orderedIndex: Int,
        val adjustedIndex: Int,
    )
}
