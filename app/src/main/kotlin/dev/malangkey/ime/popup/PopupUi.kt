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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.malangkey.ime.keyboard.Key
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.lib.FlorisRect
import dev.malangkey.lib.toIntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.florisboard.lib.snygg.SnyggQueryAttributes
import org.florisboard.lib.snygg.SnyggSelector
import org.florisboard.lib.snygg.ui.SnyggBox
import org.florisboard.lib.snygg.ui.SnyggColumn
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggRow
import org.florisboard.lib.snygg.ui.SnyggText
import org.florisboard.lib.snygg.ui.rememberSnyggThemeQuery
import org.florisboard.lib.snygg.value.SnyggDpSizeValue
import org.florisboard.lib.snygg.value.SnyggStaticColorValue

val GlobalStateNumPopupsShowing = MutableStateFlow(0)

@Composable
fun PopupBaseBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    key: Key,
    shouldIndicateExtendedPopups: Boolean,
): Unit = with(LocalDensity.current) {
    DisposableEffect(key) {
        GlobalStateNumPopupsShowing.update { it + 1 }
        onDispose {
            GlobalStateNumPopupsShowing.update { it - 1 }
        }
    }

    SnyggBox(
        elementName = FlorisImeUi.KeyPopupBox.elementName,
        attributes = attributes,
        modifier = modifier,
    ) {
        key.label?.let { label ->
            SnyggBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(key.visibleBounds.height.toDp())
                    .align(Alignment.TopCenter),
            ) {
                SnyggText(
                    modifier = Modifier.align(Alignment.Center),
                    text = label,
                )
            }
        }
        if (shouldIndicateExtendedPopups) {
            SnyggIcon(
                elementName = FlorisImeUi.KeyPopupExtendedIndicator.elementName,
                attributes = attributes,
                modifier = Modifier.align(Alignment.CenterEnd),
                imageVector = Icons.Default.MoreHoriz,
            )
        }
    }
}

@Composable
fun PopupExtBox(
    modifier: Modifier = Modifier,
    attributes: SnyggQueryAttributes,
    elements: List<List<PopupUiController.Element>>,
    elemArrangement: Arrangement.Horizontal,
    elemWidth: Dp,
    elemHeight: Dp,
    activeElementIndex: Int,
    isClipboard: Boolean = false,
): Unit = with(LocalDensity.current) {
    SnyggColumn(FlorisImeUi.KeyPopupBox.elementName, attributes, modifier = modifier) {
        for (row in elements.asReversed()) {
            SnyggRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(elemHeight),
                horizontalArrangement = elemArrangement,
            ) {
                for (element in row) {
                    val selector = if (activeElementIndex == element.orderedIndex) {
                        SnyggSelector.FOCUS
                    } else {
                        null
                    }
                    val localAttrs = attributes.plus(FlorisImeUi.Attr.Code to element.data.code)
                    SnyggBox(
                        elementName = FlorisImeUi.KeyPopupElement.elementName,
                        attributes = localAttrs,
                        selector = selector,
                        modifier = Modifier
                            .size(elemWidth, elemHeight)
                            .padding(horizontal = 1.dp, vertical = 1.dp),
                    ) {
                        element.label?.let { label ->
                            val scaleModifier = if (isClipboard) Modifier.scale(0.66f) else Modifier
                            SnyggText(
                                modifier = Modifier.align(Alignment.Center).then(scaleModifier),
                                text = label,
                            )
                        }
                        element.icon?.let { icon ->
                            val scaleModifier = if (isClipboard) Modifier.scale(0.66f) else Modifier
                            SnyggIcon(
                                modifier = Modifier.align(Alignment.Center).then(scaleModifier),
                                imageVector = icon,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 말풍선 꼬리 높이. 팝업은 이만큼 누른 키 위로 띄운다. */
val PopupBubbleTailHeight = 8.dp
private val PopupBubbleTailWidth = 16.dp
/** 꼬리가 둥근 모서리에 걸치지 않도록 양 끝에서 비워 둘 거리. */
private val PopupBubbleCornerInset = 14.dp

/**
 * 길게 누름 팝업을 말풍선으로 감싼다. 누른 키를 가리키는 꼬리를 달고, 나타날 때 꼬리 쪽에서
 * 살짝 튀어 오르게 해서 눈에 잘 띄게 한다. [content]는 [bounds] 크기로 왼쪽 위에 놓인다.
 */
@Composable
fun PopupBubble(
    bounds: FlorisRect,
    tailCenterXPx: Float,
    attributes: SnyggQueryAttributes,
    content: @Composable () -> Unit,
): Unit = with(LocalDensity.current) {
    val width = bounds.width
    val hasTail = tailCenterXPx >= 0f
    val tailX = if (hasTail) {
        val min = (PopupBubbleCornerInset + PopupBubbleTailWidth / 2).toPx()
        val max = width - min
        if (min <= max) tailCenterXPx.coerceIn(min, max) else width / 2f
    } else {
        width / 2f
    }
    val scale = remember { Animatable(0.8f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 900f))
    }
    Box(
        modifier = Modifier
            .requiredSize(width.toDp(), bounds.height.toDp() + PopupBubbleTailHeight)
            .absoluteOffset { bounds.topLeft.toIntOffset() }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                transformOrigin = TransformOrigin((tailX / width).coerceIn(0f, 1f), 1f)
            },
    ) {
        content()
        if (hasTail) {
            PopupBubbleTail(
                attributes = attributes,
                modifier = Modifier.absoluteOffset {
                    IntOffset((tailX - PopupBubbleTailWidth.toPx() / 2f).roundToInt(), bounds.height.roundToInt())
                },
            )
        }
    }
}

/** 팝업 상자와 같은 배경·테두리 색으로 아래를 향한 삼각형 꼬리를 그린다. */
@Composable
private fun PopupBubbleTail(attributes: SnyggQueryAttributes, modifier: Modifier) {
    val style = rememberSnyggThemeQuery(FlorisImeUi.KeyPopupBox.elementName, attributes)
    val background = style.background()
    if (background == Color.Unspecified) return
    val borderColor = (style.borderColor as? SnyggStaticColorValue)?.color
    val borderWidth = (style.borderWidth as? SnyggDpSizeValue)?.dp ?: 0.dp
    Canvas(
        modifier = modifier
            // 상자의 아래 테두리를 꼬리 밑동만큼 덮어 한 몸처럼 이어지게 한다.
            .offset(y = -borderWidth)
            .size(PopupBubbleTailWidth, PopupBubbleTailHeight + borderWidth),
    ) {
        val apex = Offset(size.width / 2f, size.height)
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(apex.x, apex.y)
            close()
        }
        drawPath(path, background)
        if (borderColor != null && borderWidth > 0.dp) {
            val stroke = borderWidth.toPx()
            val top = stroke / 2f
            drawLine(borderColor, Offset(0f, top), apex, stroke, StrokeCap.Round)
            drawLine(borderColor, Offset(size.width, top), apex, stroke, StrokeCap.Round)
        }
    }
}
