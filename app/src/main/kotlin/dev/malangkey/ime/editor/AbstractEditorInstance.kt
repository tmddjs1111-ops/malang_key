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

package dev.malangkey.ime.editor

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.text.TextUtils
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import dev.malangkey.FlorisImeService
import dev.malangkey.ime.nlp.BreakIteratorGroup
import dev.malangkey.ime.text.composing.Composer
import dev.malangkey.keyboardManager
import dev.malangkey.lib.ext.ExtensionComponentName
import dev.malangkey.nlpManager
import dev.malangkey.lib.devtools.flogDebug
import dev.malangkey.subtypeManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.florisboard.lib.kotlin.guardedByLock
import kotlin.math.max
import kotlin.math.min

enum class OperationUnit {
    CHARACTERS,
    WORDS;
}

enum class OperationScope {
    BEFORE_CURSOR,
    AFTER_CURSOR;
}

@Suppress("BlockingMethodInNonBlockingContext")
abstract class AbstractEditorInstance(context: Context) {
    companion object {
        private const val NumCharsBeforeCursor: Int = 256
        private const val NumCharsAfterCursor: Int = 128
        private const val NumCharsSafeMarginBeforeCursor: Int = 128
        /** 캐시가 실제 입력칸과 같은지 볼 때 비교하는 커서 앞 글자 수. */
        private const val StaleCheckChars: Int = 8
        //private const val NumCharsSafeMarginAfterCursor: Int = 0

        private const val CursorUpdateAll: Int =
            InputConnection.CURSOR_UPDATE_MONITOR or InputConnection.CURSOR_UPDATE_IMMEDIATE
        private const val CursorUpdateNone: Int = 0
    }

    private val keyboardManager by context.keyboardManager()
    private val subtypeManager by context.subtypeManager()
    private val nlpManager by context.nlpManager()
    private val scope = MainScope()
    protected val breakIterators = BreakIteratorGroup()

    private val _activeInfoFlow = MutableStateFlow(FlorisEditorInfo.Unspecified)
    val activeInfoFlow = _activeInfoFlow.asStateFlow()
    inline var activeInfo: FlorisEditorInfo
        get() = activeInfoFlow.value
        private set(v) {
            _activeInfoFlow.value = v
        }

    private val _activeCursorCapsModeFlow = MutableStateFlow(InputAttributes.CapsMode.NONE)
    val activeCursorCapsModeFlow = _activeCursorCapsModeFlow.asStateFlow()
    inline var activeCursorCapsMode: InputAttributes.CapsMode
        get() = activeCursorCapsModeFlow.value
        private set(v) {
            _activeCursorCapsModeFlow.value = v
        }

    private val _activeContentFlow = MutableStateFlow(EditorContent.Unspecified)
    val activeContentFlow = _activeContentFlow.asStateFlow()
    inline var activeContent: EditorContent
        get() = expectedContent() ?: activeContentFlow.value
        private set(v) {
            _activeContentFlow.value = v
        }
    private val expectedContentQueue = ExpectedContentQueue()
    private val _lastCommitPosition = LastCommitPosition()
    val lastCommitPosition
        get() = LastCommitPosition(_lastCommitPosition)

    fun expectedContent(): EditorContent? {
        return runBlocking { expectedContentQueue.peekNewestOrNull() }
    }

    private fun currentInputConnection() = FlorisImeService.currentInputConnection()

    open fun handleStartInput(editorInfo: FlorisEditorInfo) {
        activeInfo = editorInfo
        activeCursorCapsMode = editorInfo.initialCapsMode
        activeContent = EditorContent.Unspecified
        currentInputConnection()?.requestCursorUpdates(CursorUpdateAll)
    }

    open fun handleStartInputView(editorInfo: FlorisEditorInfo, isRestart: Boolean) {
        if (isRestart) {
            reset() // Just to make sure our state is correct after a restart
        }
        val ic = currentInputConnection()
        activeInfo = editorInfo
        var selection = editorInfo.initialSelection
        InputDiagnostics.log {
            val type = editorInfo.base.inputType
            "START app=${editorInfo.packageName} inputType=0x${Integer.toHexString(type)}" +
                " class=${type and android.text.InputType.TYPE_MASK_CLASS}" +
                " variation=0x${Integer.toHexString(type and android.text.InputType.TYPE_MASK_VARIATION)}" +
                " imeOptions=0x${Integer.toHexString(editorInfo.base.imeOptions)}" +
                " raw=${editorInfo.isRawInputEditor} sel=$selection ic=${ic != null}" +
                " readBefore=${ic?.getTextBeforeCursor(8, 0)?.length ?: "null"}"
        }
        if (ic == null || selection.isNotValid || editorInfo.isRawInputEditor) {
            activeCursorCapsMode = InputAttributes.CapsMode.NONE
            activeContent = EditorContent.Unspecified
            keyboardManager.reevaluateInputShiftState()
            return
        }

        // Get text (ignore initial text of EditorInfo because some apps like to provide an old or invalid state)
        val textBeforeSelection = ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = ic.getSelectedText(0) ?: ""

        // Adjust initial selection issues as some apps like to do everything but provide the correct initial selection
        if (selection.length != selectedText.length) {
            selection = EditorRange(textBeforeSelection.length, textBeforeSelection.length + selectedText.length)
        } else if (selection.start > 0 || selection.end > 0) {
            if (textBeforeSelection.isEmpty() && textAfterSelection.isEmpty() && selectedText.isEmpty()) {
                selection = EditorRange(0, 0)
            }
        }

        scope.launch {
            val content = generateContent(
                editorInfo,
                selection,
                textBeforeSelection,
                textAfterSelection,
                selectedText,
            )
            activeCursorCapsMode = content.cursorCapsMode()
            activeContent = content
            keyboardManager.reevaluateInputShiftState()
            ic.setComposingRegion(content.composing)
        }
    }

    protected fun handleMassSelectionUpdate(newSelection: EditorRange, composing: EditorRange) {
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.selectionOnly(newSelection)
        if (composing.isValid) {
            currentInputConnection()?.setComposingRegion(EditorRange.Unspecified)
        }
        _lastCommitPosition.handleUpdateSelection(newSelection)
    }

    open fun handleSelectionUpdate(oldSelection: EditorRange, newSelection: EditorRange, composing: EditorRange) {
        val ic = currentInputConnection()
        val editorInfo = activeInfo
        InputDiagnostics.log {
            "SEL $oldSelection -> $newSelection composing=$composing own='$ownComposing'" +
                " readBefore=${ic?.getTextBeforeCursor(8, 0)?.let { "'$it'" } ?: "null"}"
        }
        if (ic == null || newSelection.isNotValid || editorInfo.isRawInputEditor) {
            activeCursorCapsMode = InputAttributes.CapsMode.NONE
            activeContent = EditorContent.Unspecified
            keyboardManager.reevaluateInputShiftState()
            return
        }

        _lastCommitPosition.handleUpdateSelection(newSelection)
        if (ownComposing.isNotEmpty()) {
            val canRead = newSelection.start == 0 || !ic.getTextBeforeCursor(1, 0).isNullOrEmpty()
            if (composing.isValid && !canRead) {
                // 자체 조합 중이고, 입력칸은 조합 중인 글자를 그대로 들고 있지만 글은 읽을 수 없다(보안 입력칸).
                // 글을 다시 읽어 조합 범위를 고치면 조합 중인 글자가 풀려 다음 글자가 덧붙으므로 그대로 둔다.
                activeContent = EditorContent.selectionOnly(newSelection)
                return
            }
            // 글을 읽을 수 있는 보통 칸이거나 입력칸이 조합을 풀었다(커서를 옮겼거나 앱이 확정했다):
            // 자체 조합을 그만두고, 아래에서 입력칸의 글과 조합 범위를 다시 읽어 보통 방식으로 이어 간다.
            ownComposing = ""
        }
        val expected = runBlocking {
            expectedContentQueue.popUntilOrNull {
                it.selection == newSelection && it.composing == composing &&
                    it.textBeforeSelection.length >= NumCharsSafeMarginBeforeCursor.coerceAtMost(it.selection.start)
            }
        }
        if (expected != null) {
            activeCursorCapsMode = expected.cursorCapsMode()
            activeContent = expected
            keyboardManager.reevaluateInputShiftState()
            return
        }

        // Get Text
        val textBeforeSelection =
            if (newSelection.start > 0) ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: "" else ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = if (newSelection.isSelectionMode) ic.getSelectedText(0) ?: "" else ""

        scope.launch {
            val content = generateContent(
                editorInfo,
                newSelection,
                textBeforeSelection,
                textAfterSelection,
                selectedText,
            )
            activeCursorCapsMode = content.cursorCapsMode()
            activeContent = content
            keyboardManager.reevaluateInputShiftState()
            if (content.composing != composing) {
                ic.setComposingRegion(content.composing)
            }
        }
    }

    open fun handleFinishInputView() {
        reset()
    }

    open fun handleFinishInput() {
        reset()
        currentInputConnection()?.requestCursorUpdates(CursorUpdateNone)
    }

    protected open fun reset() {
        ownComposing = ""
        activeInfo = FlorisEditorInfo.Unspecified
        activeCursorCapsMode = InputAttributes.CapsMode.NONE
        activeContent = EditorContent.Unspecified
        runBlocking { expectedContentQueue.clear() }
        _lastCommitPosition.reset()
    }

    private suspend fun generateContent(
        editorInfo: FlorisEditorInfo,
        selection: EditorRange,
        textBeforeSelection: CharSequence,
        textAfterSelection: CharSequence,
        selectedText: CharSequence,
    ): EditorContent {
        // Calculate offset and local selection
        val offset = selection.start - textBeforeSelection.length
        val localSelection = EditorRange(
            start = textBeforeSelection.length,
            end = textBeforeSelection.length + selectedText.length,
        )

        // Check consistency and exit if necessary
        if (offset < 0 || selection.translatedBy(-offset) != localSelection) {
            return EditorContent.Unspecified
        }

        // Determine local composing word range, if any
        val localCurrentWord =
            if (shouldDetermineComposingRegion(editorInfo) && localSelection.isCursorMode && textBeforeSelection.isNotEmpty()) {
                determineLocalComposing(textBeforeSelection, _lastCommitPosition.pos - offset)
            } else {
                EditorRange.Unspecified
            }
        val localComposing = if (determineComposingEnabled()) localCurrentWord else EditorRange.Unspecified

        // Build and publish text and content
        val text = buildString {
            append(textBeforeSelection)
            append(selectedText)
            append(textAfterSelection)
        }
        return EditorContent(text, offset, localSelection, localComposing, localCurrentWord)
    }

    private suspend fun EditorContent.generateCopy(
        editorInfo: FlorisEditorInfo = activeInfo,
        selection: EditorRange = this.selection,
        textBeforeSelection: CharSequence = this.textBeforeSelection,
        textAfterSelection: CharSequence = this.textAfterSelection,
        selectedText: CharSequence = this.selectedText,
    ): EditorContent {
        return generateContent(
            editorInfo, selection, textBeforeSelection, textAfterSelection, selectedText
        )
    }

    private fun EditorContent.cursorCapsMode(): InputAttributes.CapsMode {
        return when {
            localSelection.isNotValid -> InputAttributes.CapsMode.NONE
            else -> {
                InputAttributes.CapsMode.fromFlags(
                    TextUtils.getCapsMode(text, localSelection.start, activeInfo.inputAttributes.raw)
                )
            }
        }
    }

    abstract fun determineComposingEnabled(): Boolean

    abstract fun determineComposer(composerName: ExtensionComponentName): Composer

    protected open fun shouldDetermineComposingRegion(editorInfo: FlorisEditorInfo): Boolean {
        // 일본어·중국어는 변환이 입력의 일부라, 검색창처럼 '추천 없음'을 건 입력칸에서도 조합 중인 단어를 잡는다.
        return editorInfo.isRichInputEditor && (
            !editorInfo.inputAttributes.flagTextNoSuggestions ||
                nlpManager.providerForcesSuggestionOn(subtypeManager.activeSubtype)
            )
    }

    private suspend fun determineLocalComposing(
        textBeforeSelection: CharSequence, localLastCommitPosition: Int
    ): EditorRange {
        return nlpManager.determineLocalComposing(textBeforeSelection, breakIterators, localLastCommitPosition)
    }

    private fun InputConnection.setComposingRegion(composing: EditorRange) {
        if (composing.isValid) {
            this.setComposingRegion(composing.start, composing.end)
        } else {
            this.finishComposingText()
        }
    }

    protected fun setSelection(selection: EditorRange): Boolean {
        if (activeInfo.isRawInputEditor) return false
        val content = activeContent
        if (content.selection == selection) return true
        val ic = currentInputConnection() ?: return false
        ic.beginBatchEdit()
        runBlocking {
            val newContent = content
                .copy(localSelection = selection.translatedBy(-content.offset))
                .generateCopy(selection = selection)
            expectedContentQueue.push(newContent)
            ic.setSelection(selection.start, selection.end)
            ic.setComposingRegion(newContent.composing)
        }
        ic.endBatchEdit()
        return true
    }

    /**
     * 키보드가 들고 있는 입력칸 내용이 실제와 다르면 지금 바로 다시 읽는다.
     *
     * 메신저가 전송하면서 입력칸을 비우는 것처럼 앱이 글을 직접 바꾸면, 키보드의 사본은 한 박자 늦게(비동기로)
     * 갱신된다. 그 사이에 키를 누르면 이미 지워진 글을 기준으로 조합해서 앞 글자가 다시 붙거나, 조합 중이던
     * 단어(때로는 보낸 문장 전체)가 다시 써지거나, 누른 글자가 사라진다. 그래서 글을 바꾸기 직전에 커서 앞
     * 몇 글자만 실제와 비교하고, 다르면 사본을 새로 만든다.
     *
     * @return 사본을 새로 만들었으면 true.
     */
    fun syncContentIfStale(): Boolean {
        val ic = currentInputConnection() ?: return false
        val content = activeContent
        if (activeInfo.isRawInputEditor || content.selection.isNotValid || content.selection.isSelectionMode) return false
        val realTail = ic.getTextBeforeCursor(StaleCheckChars, 0)?.toString() ?: return false
        val cachedTail = content.textBeforeSelection.takeLast(StaleCheckChars).toString()
        if (realTail == cachedTail) return false

        flogDebug { "Editor cache is stale (cached='$cachedTail', real='$realTail'), re-reading" }
        runBlocking { expectedContentQueue.clear() }
        val extracted = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
        if (extracted == null || extracted.selectionStart < 0) {
            // 위치를 알 수 없는 입력칸: 조합 상태만 버리고 다음 선택 변경 알림에서 다시 읽게 둔다.
            activeContent = EditorContent.Unspecified
            ic.finishComposingText()
            return true
        }
        val selection = EditorRange(
            extracted.startOffset + extracted.selectionStart,
            extracted.startOffset + extracted.selectionEnd,
        )
        val textBeforeSelection = if (selection.start > 0) ic.getTextBeforeCursor(NumCharsBeforeCursor, 0) ?: "" else ""
        val textAfterSelection = ic.getTextAfterCursor(NumCharsAfterCursor, 0) ?: ""
        val selectedText = if (selection.isSelectionMode) ic.getSelectedText(0) ?: "" else ""
        val fresh = runBlocking {
            generateContent(activeInfo, selection, textBeforeSelection, textAfterSelection, selectedText)
        }
        activeContent = fresh
        ic.setComposingRegion(fresh.composing)
        return true
    }

    /**
     * 자체 조합: 입력칸의 글을 읽을 수 없을 때(글 읽기를 막은 보안 입력칸, 커서 위치를 알려 주지 않는 칸)
     * 키보드가 직접 들고 있는 '조합 중인 글자'. 입력칸에는 같은 글자가 조합 중(밑줄)으로 들어가 있다.
     *
     * 한글 조합은 앞 글자를 읽어서 고쳐 쓰는데(ㄱ+ㄱ→ㅋ, 가+ㄴ→간), 앞 글자를 못 읽으면 ㄱㄱ처럼 그냥
     * 붙어 버린다. 그래서 그런 칸에서는 앞 글자 대신 이 값을 보고 조합한 뒤, 조합 중인 글자만 바꿔 끼운다.
     */
    protected var ownComposing: String = ""

    /**
     * 입력칸의 글을 읽을 수 없어 [ownComposing]으로 조합해야 하는지.
     *
     * TYPE_NULL 입력칸은 뺀다. 그런 칸은 보통 조합 중인 글자를 바로 키 입력으로 보내 버려서,
     * 조합 글자를 바꿔 끼우면 ㄱ을 ㅋ으로 고치는 대신 ㄱㅋ이 된다.
     */
    protected fun needsOwnComposing(content: EditorContent = activeContent): Boolean {
        if (activeInfo.isRawInputEditor) return false
        val selection = content.selection
        return selection.isNotValid ||
            // 커서는 글 뒤에 있다는데 앞 글을 하나도 못 읽었으면, 앱이 글 읽기를 막은 것이다.
            (selection.start > 0 && content.textBeforeSelection.isEmpty())
    }

    /** 자체 조합 중인 글자를 확정한다. 다른 키를 누르거나 커서가 움직일 때 부른다. */
    fun finishOwnComposing() {
        if (ownComposing.isEmpty()) return
        ownComposing = ""
        currentInputConnection()?.finishComposingText()
    }

    /** [updated]를 자체 조합 결과로 반영한다. 아직 바뀔 수 있는 마지막 [keepCount] 글자만 조합 중으로 남긴다. */
    protected fun applyOwnComposing(updated: String, keepCount: Int): Boolean {
        val ic = currentInputConnection() ?: return false
        val keep = updated.takeLast(keepCount.coerceAtLeast(0))
        val done = updated.dropLast(keep.length)
        ic.beginBatchEdit()
        if (done.isNotEmpty()) {
            // 지금 조합 중인 글자를 확정할 앞부분으로 바꿔 끼우고 확정한 뒤, 남은 부분으로 새로 조합을 시작한다.
            ic.setComposingText(done, 1)
            ic.finishComposingText()
        }
        if (keep.isNotEmpty()) {
            ic.setComposingText(keep, 1)
        } else if (done.isEmpty()) {
            // 다 지워졌으면 조합 중인 글자를 빈 글로 바꾼다.
            ic.setComposingText("", 1)
            ic.finishComposingText()
        }
        ic.endBatchEdit()
        ownComposing = keep
        return true
    }

    private fun commitCharWithOwnComposing(char: String, composer: Composer, layoutId: String): Boolean {
        val (rm, text) = composer.getActions(ownComposing.takeLast(composer.toRead), char, layoutId)
        InputDiagnostics.log { "  OWN '$ownComposing' + '$char' -> rm=$rm text='$text'" }
        if (rm > ownComposing.length) {
            // 들고 있는 것보다 앞 글자를 고치려 하면 손댈 수 없으니 확정하고 그냥 붙인다.
            finishOwnComposing()
            return applyOwnComposing(text, composer.toRead)
        }
        return applyOwnComposing(ownComposing.dropLast(rm) + text, composer.toRead)
    }

    open fun commitChar(char: String): Boolean {
        return commitChar(
            char = char,
            deletePreviousSpace = false,
            insertSpaceBeforeChar = false,
            insertSpaceAfterChar = false,
        )
    }

    protected fun commitChar(
        char: String,
        deletePreviousSpace: Boolean,
        insertSpaceBeforeChar: Boolean,
        insertSpaceAfterChar: Boolean,
    ): Boolean {
        syncContentIfStale()
        val content = activeContent
        val selection = content.selection
        InputDiagnostics.log {
            "CHAR '$char' sel=$selection cachedBefore='${content.textBeforeSelection.takeLast(4)}'" +
                " own='$ownComposing' needsOwn=${needsOwnComposing(content)} raw=${activeInfo.isRawInputEditor}"
        }
        if (selection.isSelectionMode || activeInfo.isRawInputEditor) {
            return commitTextInternal(char)
        }
        val ic = currentInputConnection() ?: return false
        val composer = determineComposer(subtypeManager.activeSubtype.composer)
        val layoutId = subtypeManager.activeSubtype.layoutMap.characters.componentId
        if (needsOwnComposing(content)) {
            return commitCharWithOwnComposing(char, composer, layoutId)
        }
        ownComposing = ""
        if (composer.toRead > 0 && content.textBeforeSelection.isEmpty() && !deletePreviousSpace) {
            // 앞 글을 하나도 모른다(칸 맨 앞이거나, 앱이 글 읽기를 막았다). 첫 글자를 그냥 확정하면 글 읽기를
            // 막은 칸에서는 다음 글자와 조합할 수 없다(ㄱ+ㄱ이 ㅋ 대신 ㄱㄱ). 그래서 첫 글자부터 조합 중으로
            // 들고 있다가, 다음 선택 변경 알림에서 글을 읽을 수 있는 칸으로 확인되면 보통 방식으로 돌아간다.
            return commitCharWithOwnComposing(char, composer, layoutId)
        }
        val previous = content.textBeforeSelection.takeLast(composer.toRead.coerceAtLeast(if (deletePreviousSpace) 1 else 0))
        val (tempRm, tempText) = composer.getActions(previous, char, layoutId)
        val rm = if (deletePreviousSpace && previous.isNotEmpty() && previous.last() == ' ') tempRm + 1 else tempRm
        val finalText = buildString(tempText.length + 2) {
            if (insertSpaceBeforeChar) append(' ')
            append(tempText)
            if (insertSpaceAfterChar) append(' ')
        }
        if (rm <= 0) {
            commitTextInternal(finalText)
        } else runBlocking {
            ic.beginBatchEdit()
            val newSelection = EditorRange.cursor(selection.start - rm + finalText.length)
            val newContent = content.generateCopy(
                selection = newSelection,
                textBeforeSelection = buildString {
                    append(content.textBeforeSelection.dropLast(rm))
                    append(finalText)
                },
                selectedText = "",
            )
            expectedContentQueue.push(newContent)
            // Utilize composing region to replace previous chars without using delete. This avoids flickering in the
            // target editor and improves the UX
            ic.setComposingRegion(content.selection.start - rm, content.selection.start)
            ic.setComposingText(finalText, 1)
            // Now set the proper composing region we expect
            ic.setComposingRegion(newContent.composing)
            activeContent = newContent
            ic.endBatchEdit()
        }
        return true
    }

    open fun commitText(text: String): Boolean = commitTextInternal(text)

    private fun commitTextInternal(text: String): Boolean {
        val ic = currentInputConnection() ?: return false
        val content = activeContent
        val selection = content.selection
        ownComposing = ""
        ic.beginBatchEdit()
        ic.finishComposingText()
        if (activeInfo.isRawInputEditor || selection.isNotValid) {
            ic.commitText(text, 1)
        } else runBlocking {
            val newSelection = EditorRange.cursor(selection.start + text.length)
            val newContent = content.generateCopy(
                selection = newSelection,
                textBeforeSelection = buildString {
                    append(content.textBeforeSelection)
                    append(text)
                },
                selectedText = "",
            )
            expectedContentQueue.push(newContent)
            ic.commitText(text, 1)
            ic.setComposingRegion(newContent.composing)
            activeContent = newContent
        }
        ic.endBatchEdit()
        return true
    }

    open fun finalizeComposingText(text: String): Boolean {
        val ic = currentInputConnection() ?: return false
        // 앱이 이미 글을 바꿨는데 예전 조합 단어를 다시 쓰면, 보낸 말이 통째로 다시 들어간다.
        if (syncContentIfStale()) return false
        val content = activeContent
        val composing = content.composing
        // 예전에는 여기서 beginBatchEdit()를 먼저 열고 그냥 돌아가서, 입력칸이 편집을 붙잡고 있다가
        // 나중에 한꺼번에 반영하는 일이 생겼다.
        if (activeInfo.isRawInputEditor || composing.isNotValid) return false
        ic.beginBatchEdit()
        runBlocking {
            val newSelection = EditorRange.cursor(composing.end + (text.length - content.composingText.length))
            val newContent = content.generateCopy(
                selection = newSelection,
                textBeforeSelection = buildString {
                    append(content.textBeforeSelection.removeSuffix(content.composingText))
                    append(text)
                },
                selectedText = "",
            )
            expectedContentQueue.push(newContent)
            ic.setComposingText(text, 1)
            ic.finishComposingText()
            _lastCommitPosition.handleCommit(newContent.selection)
            activeContent = newContent
        }
        ic.endBatchEdit()
        return true
    }

    protected suspend fun deleteAroundCursor(unit: OperationUnit, scope: OperationScope, n: Int = 0): Boolean {
        val ic = currentInputConnection()
        if (ic == null || n < 1) return false
        val content = activeContent
        // Cannot perform below check due to editors which lie about their correct selection
        //if (content.selection.isValid && content.selection.start == 0) return true
        val scopeText = when (scope) {
            OperationScope.BEFORE_CURSOR -> content.textBeforeSelection
            OperationScope.AFTER_CURSOR -> content.textAfterSelection
        }
        return (if (activeInfo.isRawInputEditor || scopeText.isEmpty()) {
            // If editor is rich and text before selection is empty we seem to have an invalid state here, so we fall
            // back to emulating a hardware backspace/forward delete.
            val keyEventCode = when (scope) {
                OperationScope.BEFORE_CURSOR -> KeyEvent.KEYCODE_DEL
                OperationScope.AFTER_CURSOR -> KeyEvent.KEYCODE_FORWARD_DEL
            }
            val metaState = when (unit) {
                OperationUnit.CHARACTERS -> meta()
                OperationUnit.WORDS -> meta(ctrl = true)
            }
            sendDownUpKeyEvent(keyEventCode, metaState, count = n)
        } else {
            val locale = subtypeManager.activeSubtype.primaryLocale
            when (scope) {
                OperationScope.BEFORE_CURSOR -> {
                    val length = when (unit) {
                        OperationUnit.CHARACTERS -> breakIterators.measureLastUChars(scopeText, n, locale)
                        OperationUnit.WORDS -> breakIterators.measureLastUWords(scopeText, n, locale)
                    }
                    val selection = content.selection
                    val newSelection = selection.translatedBy(-length)
                    val newContent = content.generateCopy(
                        selection = newSelection,
                        textBeforeSelection = scopeText.dropLast(length),
                    )
                    expectedContentQueue.push(newContent)
                    ic.beginBatchEdit()
                    ic.finishComposingText()
                    ic.deleteSurroundingText(length, 0)
                    ic.setComposingRegion(newContent.composing)
                    ic.endBatchEdit()
                }
                OperationScope.AFTER_CURSOR -> {
                    val length = when (unit) {
                        OperationUnit.CHARACTERS -> breakIterators.measureUChars(scopeText, n, locale)
                        OperationUnit.WORDS -> breakIterators.measureUWords(scopeText, n, locale)
                    }
                    val selection = content.selection
                    val newSelection = selection.translatedBy(length)
                    val newContent = content.generateCopy(
                        selection = newSelection,
                        textAfterSelection = scopeText.drop(length),
                    )
                    expectedContentQueue.push(newContent)
                    ic.beginBatchEdit()
                    ic.finishComposingText()
                    ic.deleteSurroundingText(0, length)
                    ic.setComposingRegion(newContent.composing)
                    ic.endBatchEdit()
                }
            }
            true
        }).also {
            deleteMoveLastCommitPosition()
        }
    }

    fun refreshComposing() {
        val content = activeContent
        val ic = currentInputConnection()
        if (activeInfo.isRawInputEditor || ic == null) return
        runBlocking {
            val newContent = content.generateCopy()
            if (newContent.composing != content.composing) {
                expectedContentQueue.push(newContent)
                ic.setComposingRegion(newContent.composing)
            }
        }
    }

    /**
     * Gets [n] characters before the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get before the cursor. Must be greater than 0 or this
     *  method will fail. This number indicates the number of Unicode chars, so the returned string
     *  length may be greater than n, due to Java char encoding.
     *
     * @return [n] or less characters before the cursor.
     */
    fun EditorContent.getTextBeforeCursor(n: Int): String {
        if (n < 1 || text.isEmpty()) return ""
        return runBlocking {
            val text = textBeforeSelection
            val length = breakIterators.measureLastUChars(text, n, subtypeManager.activeSubtype.primaryLocale)
            text.takeLast(length)
        }
    }

    /**
     * Gets [n] characters after the cursor's current position. The resulting string may be any
     * length ranging from 0 to n.
     *
     * @param n The number of characters to get after the cursor. Must be greater than 0 or this
     *  method will fail. This number indicates the number of Unicode chars, so the returned string
     *  length may be greater than n, due to Java char encoding.
     *
     * @return [n] or less characters after the cursor.
     */
    fun EditorContent.getTextAfterCursor(n: Int): String {
        if (n < 1 || text.isEmpty()) return ""
        return runBlocking {
            val text = textAfterSelection
            val length = breakIterators.measureUChars(text, n, subtypeManager.activeSubtype.primaryLocale)
            text.take(length)
        }
    }

    /**
     * Constructs a meta state integer flag which can be used for setting the `metaState` field when sending a KeyEvent
     * to the input connection. If this method is called without a meta modifier set to true, the default value `0` is
     * returned.
     *
     * @param ctrl Set to true to enable the CTRL meta modifier. Defaults to false.
     * @param alt Set to true to enable the ALT meta modifier. Defaults to false.
     * @param shift Set to true to enable the SHIFT meta modifier. Defaults to false.
     *
     * @return An integer containing all meta flags passed and formatted for use in a [KeyEvent].
     */
    fun meta(
        ctrl: Boolean = false,
        alt: Boolean = false,
        shift: Boolean = false,
    ): Int {
        var metaState = 0
        if (ctrl) {
            metaState = metaState or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        }
        if (alt) {
            metaState = metaState or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (shift) {
            metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        return metaState
    }

    private fun InputConnection.sendDownKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int, repeat: Int = 0): Boolean {
        return this.sendKeyEvent(
            KeyEvent(
                eventTime,
                eventTime,
                KeyEvent.ACTION_DOWN,
                keyEventCode,
                repeat,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            )
        )
    }

    private fun InputConnection.sendUpKeyEvent(eventTime: Long, keyEventCode: Int, metaState: Int): Boolean {
        return this.sendKeyEvent(
            KeyEvent(
                eventTime,
                SystemClock.uptimeMillis(),
                KeyEvent.ACTION_UP,
                keyEventCode,
                0,
                metaState,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE,
                InputDevice.SOURCE_KEYBOARD,
            )
        )
    }

    /**
     * Same as [InputMethodService.sendDownUpKeyEvents] but also allows to set meta state.
     *
     * @param keyEventCode The key code to send, use a key code defined in Android's [KeyEvent].
     * @param metaState Flags indicating which meta keys are currently pressed.
     * @param count How often the key is pressed while the meta keys passed are down. Must be greater than or equal to
     *  `1`, else this method will immediately return false.
     *
     * @return True on success, false if an error occurred or the input connection is invalid.
     */
    fun sendDownUpKeyEvent(keyEventCode: Int, metaState: Int = meta(), count: Int = 1): Boolean {
        // 엔터·화살표·지우기 같은 키 이벤트 앞에서 자체 조합 중인 글자를 먼저 확정한다.
        finishOwnComposing()
        if (count < 1) return false
        val ic = currentInputConnection() ?: return false
        ic.beginBatchEdit()
        val eventTime = SystemClock.uptimeMillis()
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            ic.sendDownKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        for (n in 0 until count) {
            ic.sendDownKeyEvent(eventTime, keyEventCode, metaState, n)
        }
        ic.sendUpKeyEvent(eventTime, keyEventCode, metaState)
        if (metaState and KeyEvent.META_SHIFT_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_SHIFT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_ALT_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_ALT_LEFT, 0)
        }
        if (metaState and KeyEvent.META_CTRL_ON != 0) {
            ic.sendUpKeyEvent(eventTime, KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
        ic.endBatchEdit()
        return true
    }

    private class ExpectedContentQueue {
        private val list = guardedByLock { mutableListOf<EditorContent>() }

        suspend fun popUntilOrNull(predicate: (EditorContent) -> Boolean): EditorContent? {
            return list.withLock { list ->
                while (list.isNotEmpty()) {
                    val item = list.removeAt(0)
                    if (predicate(item)) return@withLock item
                }
                return@withLock null
            }
        }

        suspend fun push(item: EditorContent) {
            list.withLock { list ->
                list.add(item)
            }
        }

        suspend fun peekNewestOrNull(): EditorContent? {
            return list.withLock { list ->
                list.lastOrNull()
            }
        }

        suspend fun clear() {
            list.withLock { list ->
                list.clear()
            }
        }
    }

    fun updateLastCommitPosition() = _lastCommitPosition.handleCommit(activeContent.selection)
    fun deleteMoveLastCommitPosition() = _lastCommitPosition.handleDelete(activeContent.selection)

    /**
     * Class for handling history of last commit position.
     */
    data class LastCommitPosition(var pos: Int = -1) {

        constructor(other: LastCommitPosition): this(other.pos)

        fun reset() {
            pos = -1
        }

        fun handleCommit(selection: EditorRange) {
            if (selection.isValid) {
                pos = max(selection.start, selection.end)
            } else {
                reset()
            }
        }

        fun handleUpdateSelection(selection: EditorRange) {
            val start = min(selection.start, selection.end)
            if (start < pos) {
                reset()
            }
        }

        fun handleDelete(selection: EditorRange) {
            val start = min(selection.start, selection.end)
            if (start < pos) {
                pos = start
            }
        }
    }
}
