package dev.malangkey.ime.text.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.input.LocalInputFeedbackController
import dev.malangkey.ime.keyboard.ComputingEvaluator
import dev.malangkey.ime.keyboard.FlorisImeSizing
import dev.malangkey.ime.keyboard.GridWideAlignment
import dev.malangkey.ime.keyboard.KeyboardMode
import dev.malangkey.ime.theme.FlorisImeUi
import dev.malangkey.ime.window.FoldState
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.launch
import org.florisboard.lib.snygg.ui.SnyggIcon
import org.florisboard.lib.snygg.ui.SnyggIconButton

/**
 * 글자 자판. 넓은 화면(펼침·가로·태블릿)의 격자 자판(천지인·20키 등)은 삼성 키보드의 '한 손 키보드'처럼
 * 좁혀서 한쪽에 두고, 빈 쪽에 자판을 옮기거나(◀ ▶) 원래 너비로 넓히는(⤢) 버튼을 둔다.
 * 그 밖에는 [TextKeyboardLayout]을 그대로 그린다.
 */
@Composable
fun TextKeyboardWithWideGrid(evaluator: ComputingEvaluator) {
    val prefs by FlorisPreferenceStore
    val configuration = LocalConfiguration.current
    val isUnfolded by FoldState.isUnfolded.collectAsState()
    val compact by prefs.keyboard.gridWideCompact.collectAsState()
    val widthPercent by prefs.keyboard.gridWideWidthPercent.collectAsState()
    val alignment by prefs.keyboard.gridWideAlignment.collectAsState()

    val isGrid = evaluator.keyboard.mode == KeyboardMode.GRID_16KEY
    val isWide = isUnfolded ||
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
        configuration.smallestScreenWidthDp >= 600
    if (!isGrid || !isWide || !compact) {
        TextKeyboardLayout(evaluator = evaluator)
        return
    }

    val scope = rememberCoroutineScope()
    val inputFeedbackController = LocalInputFeedbackController.current
    fun moveTo(target: GridWideAlignment) {
        inputFeedbackController.keyPress()
        scope.launch { prefs.keyboard.gridWideAlignment.set(target) }
    }
    fun expand() {
        inputFeedbackController.keyPress()
        scope.launch { prefs.keyboard.gridWideCompact.set(false) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val keyboardWidth = maxWidth * (widthPercent.coerceIn(30, 90) / 100f)
        val freeWidth = maxWidth - keyboardWidth
        val height = FlorisImeSizing.keyboardUiHeight()
        Row {
            when (alignment) {
                GridWideAlignment.LEFT -> {
                    TextKeyboardLayout(modifier = Modifier.width(keyboardWidth), evaluator = evaluator)
                    SideControls(freeWidth, height) {
                        SideButton(Icons.AutoMirrored.Default.KeyboardArrowRight, "가운데로") { moveTo(GridWideAlignment.CENTER) }
                        SideButton(Icons.Default.OpenInFull, "넓게") { expand() }
                    }
                }
                GridWideAlignment.CENTER -> {
                    SideControls(freeWidth / 2, height) {
                        SideButton(Icons.AutoMirrored.Default.KeyboardArrowLeft, "왼쪽으로") { moveTo(GridWideAlignment.LEFT) }
                    }
                    TextKeyboardLayout(modifier = Modifier.width(keyboardWidth), evaluator = evaluator)
                    SideControls(freeWidth / 2, height) {
                        SideButton(Icons.AutoMirrored.Default.KeyboardArrowRight, "오른쪽으로") { moveTo(GridWideAlignment.RIGHT) }
                        SideButton(Icons.Default.OpenInFull, "넓게") { expand() }
                    }
                }
                GridWideAlignment.RIGHT -> {
                    SideControls(freeWidth, height) {
                        SideButton(Icons.AutoMirrored.Default.KeyboardArrowLeft, "가운데로") { moveTo(GridWideAlignment.CENTER) }
                        SideButton(Icons.Default.OpenInFull, "넓게") { expand() }
                    }
                    TextKeyboardLayout(modifier = Modifier.width(keyboardWidth), evaluator = evaluator)
                }
            }
        }
    }
}

@Composable
private fun SideControls(width: Dp, height: Dp, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.width(width).height(height),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun SideButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    SnyggIconButton(
        elementName = FlorisImeUi.SmartbarActionKey.elementName,
        modifier = Modifier.size(48.dp),
        onClick = onClick,
    ) {
        SnyggIcon(
            FlorisImeUi.SmartbarActionKey.elementName,
            imageVector = icon,
            contentDescription = description,
        )
    }
}
