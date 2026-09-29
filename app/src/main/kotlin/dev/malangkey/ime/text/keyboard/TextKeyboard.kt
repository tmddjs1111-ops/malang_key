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

package dev.malangkey.ime.text.keyboard

import dev.malangkey.ime.text.key.KeyCode
import dev.malangkey.ime.keyboard.Key
import dev.malangkey.ime.keyboard.Keyboard
import dev.malangkey.ime.keyboard.KeyboardMode
import dev.malangkey.ime.popup.PopupMapping
import kotlin.math.abs

class TextKeyboard(
    val arrangement: Array<Array<TextKey>>,
    override val mode: KeyboardMode,
    val extendedPopupMapping: PopupMapping?,
    val extendedPopupMappingDefault: PopupMapping?,
    val hasCompactNumberRow: Boolean = false,
) : Keyboard() {
    val rowCount: Int
        get() = arrangement.size

    val keyCount: Int
        get() = arrangement.sumOf { it.size }

    fun maxKeyCountPerRow(): Int {
        return arrangement.maxOfOrNull { it.size } ?: 10
    }

    fun heightInRows(): Float {
        val compactRowReduction = if (hasCompactNumberRow && rowCount > 0) {
            1.0f - CompactNumberRowHeightFactor
        } else {
            0.0f
        }
        return rowCount.toFloat() - compactRowReduction
    }

    fun isCompactNumberRowKey(key: TextKey): Boolean {
        return hasCompactNumberRow && arrangement.firstOrNull()?.any { it === key } == true
    }

    private fun rowHeightFactor(rowIndex: Int): Float {
        return if (hasCompactNumberRow && rowIndex == 0) CompactNumberRowHeightFactor else 1.0f
    }

    override fun getKeyForPos(pointerX: Float, pointerY: Float): TextKey? {
        for (key in keys()) {
            if (key.touchBounds.contains(pointerX, pointerY)) {
                return key
            }
        }
        return null
    }

    override fun layout(
        keyboardWidth: Float,
        keyboardHeight: Float,
        desiredKey: Key,
        extendTouchBoundariesDownwards: Boolean,
    ) {
        if (arrangement.isEmpty()) return
        for (key in keys()) {
            key.splitGapWidth = 0f
        }

        val desiredTouchBounds = desiredKey.touchBounds
        val desiredVisibleBounds = desiredKey.visibleBounds
        if (desiredTouchBounds.isEmpty() || desiredVisibleBounds.isEmpty()) return
        if (keyboardWidth.isNaN() || keyboardHeight.isNaN()) return
        val rowMarginH = abs(desiredTouchBounds.width - desiredVisibleBounds.width)
        val stretchFactor = if (heightInRows() > 0f) keyboardHeight / (desiredTouchBounds.height * heightInRows()) else 1.0f
        val rowMarginV = 0.0f
        var posY = 0.0f

        for ((r, row) in rows().withIndex()) {
            val rowHeight = desiredTouchBounds.height * rowHeightFactor(r) * stretchFactor
            val availableWidth = (keyboardWidth - rowMarginH) / desiredTouchBounds.width
            var requestedWidth = 0.0f
            var shrinkSum = 0.0f
            var growSum = 0.0f
            for (key in row) {
                requestedWidth += key.flayWidthFactor
                shrinkSum += key.flayShrink
                growSum += key.flayGrow
            }
            if (requestedWidth <= availableWidth) {
                // Requested with is smaller or equal to the available with, so we can grow
                val additionalWidth = availableWidth - requestedWidth
                var posX = rowMarginH / 2.0f
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width * when (growSum) {
                        0.0f -> when (k) {
                            0, row.size - 1 -> key.flayWidthFactor + additionalWidth / 2.0f
                            else -> key.flayWidthFactor
                        }
                        else -> key.flayWidthFactor + additionalWidth * (key.flayGrow / growSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = posX + keyWidth
                        bottom = posY + rowHeight
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left) + when {
                            growSum == 0.0f && k == 0 -> ((additionalWidth / 2.0f) * desiredTouchBounds.width)
                            else -> 0.0f
                        }
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right) - when {
                            growSum == 0.0f && k == row.size - 1 -> ((additionalWidth / 2.0f) * desiredTouchBounds.width)
                            else -> 0.0f
                        }
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0.0f
                        } else if (k == row.size - 1) {
                            right = keyboardWidth
                        }
                        if (extendTouchBoundariesDownwards && r + 1 == arrangement.size) {
                            bottom += height
                        }
                    }
                }
            } else {
                // Requested size too big, must shrink.
                val clippingWidth = requestedWidth - availableWidth
                var posX = rowMarginH / 2.0f
                for ((k, key) in row.withIndex()) {
                    val keyWidth = desiredTouchBounds.width * if (key.flayShrink == 0.0f) {
                        key.flayWidthFactor
                    } else {
                        key.flayWidthFactor - clippingWidth * (key.flayShrink / shrinkSum)
                    }
                    key.touchBounds.apply {
                        left = posX
                        top = posY
                        right = posX + keyWidth
                        bottom = posY + rowHeight
                    }
                    key.visibleBounds.apply {
                        left = key.touchBounds.left + abs(desiredTouchBounds.left - desiredVisibleBounds.left)
                        top = key.touchBounds.top + abs(desiredTouchBounds.top - desiredVisibleBounds.top)
                        right = key.touchBounds.right - abs(desiredTouchBounds.right - desiredVisibleBounds.right)
                        bottom = key.touchBounds.bottom - abs(desiredTouchBounds.bottom - desiredVisibleBounds.bottom)
                    }
                    posX += keyWidth
                    // After-adjust touch bounds for the row margin
                    key.touchBounds.apply {
                        if (k == 0) {
                            left = 0.0f
                        } else if (k == row.size - 1) {
                            right = keyboardWidth
                        }
                        if (extendTouchBoundariesDownwards && r + 1 == arrangement.size) {
                            bottom += height
                        }
                    }
                }
            }
            posY += rowHeight + rowMarginV
        }
    }

    /**
     * 자판을 가운데에서 [gap]만큼 벌려 좌우로 나눈다. [layout]을 전체 너비에서 [gap]을 뺀
     * [layoutWidth]로 부른 직후 한 번만 호출한다. 키 위치만 옮기므로 입력할 때 드는 계산은 그대로다.
     *
     * 가운데를 가로지르는 스페이스바는 빈 칸을 건너 양쪽에 걸치고, 화면에는 두 조각으로 그린다.
     * 스페이스바가 없는 줄은 빈 칸을 눌러도 가까운 쪽 키가 눌리게 가장자리 키의 터치 영역을 반씩 넓힌다.
     */
    fun applySplit(layoutWidth: Float, gap: Float) {
        if (gap <= 0f) return
        val mid = layoutWidth / 2f
        for (row in arrangement) {
            var lastLeft: TextKey? = null
            var firstRight: TextKey? = null
            var hasBridge = false
            for (key in row) {
                if (key.touchBounds.width <= 0f) continue
                val code = key.computedData.code
                val isSpace = code == KeyCode.SPACE || code == KeyCode.CJK_SPACE
                if (isSpace && key.visibleBounds.left < mid && key.visibleBounds.right > mid) {
                    key.touchBounds.right += gap
                    key.visibleBounds.right += gap
                    key.splitGapStart = mid
                    key.splitGapWidth = gap
                    hasBridge = true
                    continue
                }
                val centerX = (key.touchBounds.left + key.touchBounds.right) / 2f
                // 정확히 가운데에 걸친 키(예: 쿼티의 G, V)는 왼쪽에 둔다.
                if (centerX > mid + 0.01f * key.touchBounds.width) {
                    key.touchBounds.left += gap
                    key.touchBounds.right += gap
                    key.visibleBounds.left += gap
                    key.visibleBounds.right += gap
                    if (firstRight == null) firstRight = key
                } else {
                    lastLeft = key
                }
            }
            if (!hasBridge) {
                lastLeft?.let { it.touchBounds.right += gap / 2f }
                firstRight?.let { it.touchBounds.left -= gap / 2f }
            }
        }
    }

    override fun keys(): Iterator<TextKey> {
        return TextKeyboardIterator(arrangement)
    }

    fun rows(): Iterator<Array<TextKey>> {
        return arrangement.iterator()
    }

    companion object {
        const val CompactNumberRowHeightFactor = 0.82f
    }

    class TextKeyboardIterator internal constructor(
        private val arrangement: Array<Array<TextKey>>
    ) : Iterator<TextKey> {
        private var rowIndex: Int = 0
        private var keyIndex: Int = 0

        override fun hasNext(): Boolean {
            return rowIndex < arrangement.size && keyIndex < arrangement[rowIndex].size
        }

        override fun next(): TextKey {
            val next = arrangement[rowIndex][keyIndex]
            if (keyIndex + 1 == arrangement[rowIndex].size) {
                rowIndex++
                keyIndex = 0
            } else {
                keyIndex++
            }
            return next
        }
    }
}
