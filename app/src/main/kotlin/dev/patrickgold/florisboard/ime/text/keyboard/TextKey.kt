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

package dev.patrickgold.florisboard.ime.text.keyboard

import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.ime.keyboard.AbstractKeyData
import dev.patrickgold.florisboard.ime.keyboard.ComputingEvaluator
import dev.patrickgold.florisboard.ime.keyboard.Key
import dev.patrickgold.florisboard.ime.keyboard.KeyData
import dev.patrickgold.florisboard.ime.keyboard.KeyboardMode
import dev.patrickgold.florisboard.ime.keyboard.computeImageVector
import dev.patrickgold.florisboard.ime.keyboard.computeLabel
import dev.patrickgold.florisboard.ime.popup.MutablePopupSet
import dev.patrickgold.florisboard.ime.popup.PopupMapping
import dev.patrickgold.florisboard.ime.popup.PopupSet
import dev.patrickgold.florisboard.ime.text.key.KeyCode
import dev.patrickgold.florisboard.ime.text.key.KeyType
import dev.patrickgold.florisboard.ime.text.key.KeyVariation
import dev.patrickgold.florisboard.lib.lowercase

class TextKey(override val data: AbstractKeyData) : Key(data) {
    var computedData: KeyData = TextKeyData.UNSPECIFIED
        private set
    val computedPopups: MutablePopupSet<KeyData> = MutablePopupSet()
    var computedSymbolHint: KeyData? = null
    var computedNumberHint: KeyData? = null
    var computedHintData: KeyData = TextKeyData.UNSPECIFIED

    // This should exclusively be set and used by the TextKeyboardLayout
    var computedDataOnDown: KeyData = TextKeyData.UNSPECIFIED

    fun compute(evaluator: ComputingEvaluator) {
        val keyboard = evaluator.keyboard as? TextKeyboard ?: return
        val keyboardMode = keyboard.mode
        val computed = data.compute(evaluator)

        if (computed == null || !evaluator.evaluateVisible(computed)) {
            computedData = TextKeyData.UNSPECIFIED
            computedPopups.clear()
            isEnabled = false
            isVisible = false

            flayShrink = 0.0f
            flayGrow = 0.0f
            flayWidthFactor = 0.0f
        } else {
            computedData = computed
            computedPopups.clear()
            mergePopups(computed, evaluator, computedPopups::merge)
            if (keyboardMode == KeyboardMode.CHARACTERS || keyboardMode == KeyboardMode.NUMERIC_ADVANCED ||
                keyboardMode == KeyboardMode.SYMBOLS || keyboardMode == KeyboardMode.SYMBOLS2) {
                val computedLabel = computed.label.lowercase(evaluator.subtype.primaryLocale)
                val extLabel = when (computed.groupId) {
                    KeyData.GROUP_ENTER -> {
                        "~enter"
                    }
                    KeyData.GROUP_LEFT -> {
                        "~left"
                    }
                    KeyData.GROUP_RIGHT -> {
                        "~right"
                    }
                    KeyData.GROUP_KANA -> {
                        "~kana"
                    }
                    else -> {
                        computedLabel
                    }
                }
                val extendedPopupsDefault = keyboard.extendedPopupMappingDefault
                val extendedPopups = keyboard.extendedPopupMapping
                var popupSet: PopupSet<AbstractKeyData>? = null
                val kv = evaluator.state.keyVariation
                if (popupSet == null && kv == KeyVariation.PASSWORD) {
                    popupSet = extendedPopups?.get(KeyVariation.PASSWORD)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.PASSWORD)?.get(extLabel)
                }
                if (popupSet == null && (kv == KeyVariation.NORMAL || kv == KeyVariation.PASSWORD)) {
                    popupSet = extendedPopups?.get(KeyVariation.NORMAL)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.NORMAL)?.get(extLabel)
                }
                if (popupSet == null && kv == KeyVariation.EMAIL_ADDRESS) {
                    popupSet = extendedPopups?.get(KeyVariation.EMAIL_ADDRESS)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.EMAIL_ADDRESS)?.get(extLabel)
                }
                if (popupSet == null && (kv == KeyVariation.EMAIL_ADDRESS || kv == KeyVariation.URI)) {
                    popupSet = extendedPopups?.get(KeyVariation.URI)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.URI)?.get(extLabel)
                }
                if (popupSet == null) {
                    popupSet = extendedPopups?.get(KeyVariation.ALL)?.get(extLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.ALL)?.get(extLabel)
                }
                var keySpecificPopupSet: PopupSet<AbstractKeyData>? = null
                if (extLabel != computedLabel) {
                    keySpecificPopupSet = extendedPopups?.get(KeyVariation.ALL)?.get(computedLabel) ?:
                        extendedPopupsDefault?.get(KeyVariation.ALL)?.get(computedLabel)
                }
                computedPopups.apply {
                    keySpecificPopupSet?.let { merge(it, evaluator) }
                    popupSet?.let { merge(it, evaluator) }
                }
                if (computed.type == KeyType.CHARACTER) {
                    addComputedHints(computed.code, evaluator, extendedPopups, extendedPopupsDefault)
                }
            }
            isEnabled = evaluator.evaluateEnabled(computed)
            isVisible = true

            flayShrink = when (keyboardMode) {
                KeyboardMode.GRID_16KEY -> 0.0f
                KeyboardMode.NUMERIC,
                KeyboardMode.NUMERIC_ADVANCED,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 1.0f
                else -> when (computed.code) {
                    KeyCode.SHIFT,
                    KeyCode.DELETE -> 1.5f
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2,
                    KeyCode.ENTER -> 0.0f
                    else -> 1.0f
                }
            }
            flayGrow = when (keyboardMode) {
                KeyboardMode.GRID_16KEY -> (computed as? TextKeyData)?.grow ?: 0.0f
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 0.0f
                KeyboardMode.NUMERIC_ADVANCED -> when (computed.type) {
                    KeyType.NUMERIC -> 1.0f
                    else -> 0.0f
                }
                else -> when (computed.code) {
                    KeyCode.SPACE, KeyCode.CJK_SPACE -> 1.0f
                    else -> 0.0f
                }
            }
            flayWidthFactor = when (keyboardMode) {
                KeyboardMode.GRID_16KEY -> (computed as? TextKeyData)?.weight ?: 1.0f
                KeyboardMode.NUMERIC,
                KeyboardMode.PHONE,
                KeyboardMode.PHONE2 -> 2.68f
                KeyboardMode.NUMERIC_ADVANCED -> when (computed.code) {
                    44, 46 -> 1.00f
                    KeyCode.VIEW_SYMBOLS, 61 -> 1.26f
                    else -> 1.56f
                }
                else -> when (computed.code) {
                    KeyCode.SHIFT,
                    KeyCode.DELETE -> 1.56f
                    KeyCode.VIEW_CHARACTERS,
                    KeyCode.VIEW_SYMBOLS,
                    KeyCode.VIEW_SYMBOLS2,
                    KeyCode.ENTER -> 1.56f
                    else -> 1.00f
                }
            }
        }
    }

    inline fun setPressed(state: Boolean, blockIfChanged: () -> Unit) {
        if (isPressed != state) {
            isPressed = state
            blockIfChanged()
        }
    }

    private fun addComputedHints(
        keyCode: Int,
        evaluator: ComputingEvaluator,
        extendedPopups: PopupMapping?,
        extendedPopupsDefault: PopupMapping?
    ) {
        if (evaluator.keyboard.mode == KeyboardMode.GRID_16KEY || evaluator.keyboard.mode == KeyboardMode.CHARACTERS) {
            if (computedNumberHint == null) {
                val hintCode = if (evaluator.keyboard.mode == KeyboardMode.GRID_16KEY) {
                    when (keyCode) {
                        12593 -> 49 // ㄱㅋ -> 1
                        12643 -> 50 // ㅣㅡ -> 2
                        12623 -> 51 // ㅏㅑ -> 3
                        12599 -> 52 // ㄷㅌ -> 4
                        12596 -> 53 // ㄴㄹ -> 5
                        12627 -> 54 // ㅓㅕ -> 6
                        12609 -> 55 // ㅁㅅ -> 7
                        12610 -> 56 // ㅂㅍ -> 8
                        12631 -> 57 // ㅗㅛ -> 9
                        12616 -> 61 // ㅈㅊ -> =
                        12615 -> 48 // ㅇㅎ -> 0
                        12636 -> 63 // ㅜㅠ -> ?
                        else -> null
                    }
                } else {
                    when (keyCode) {
                        12610 -> 49 // ㅂ -> 1
                        12616 -> 50 // ㅈ -> 2
                        12599 -> 51 // ㄷ -> 3
                        12593 -> 52 // ㄱ -> 4
                        12613 -> 53 // ㅅ -> 5
                        12635 -> 54 // ㅛ -> 6
                        12629 -> 55 // ㅕ -> 7
                        12625 -> 56 // ㅑ -> 8
                        12624 -> 57 // ㅐ -> 9
                        12626 -> 48 // ㅔ -> 0
                        else -> null
                    }
                }
                if (hintCode != null) {
                    computedNumberHint = TextKeyData(
                        code = hintCode,
                        label = hintCode.toChar().toString(),
                        type = KeyType.NUMERIC
                    )
                }
            }
            if (computedSymbolHint == null) {
                val symbolHintCode = if (evaluator.keyboard.mode == KeyboardMode.GRID_16KEY) {
                    when (keyCode) {
                        12593 -> 64 // ㄱㅋ -> @
                        12643 -> 35 // ㅣㅡ -> #
                        12623 -> 36 // ㅏㅑ -> $
                        12599 -> 37 // ㄷㅌ -> %
                        12596 -> 38 // ㄴㄹ -> &
                        12627 -> 42 // ㅓㅕ -> *
                        12609 -> 40 // ㅁㅅ -> (
                        12610 -> 41 // ㅂㅍ -> )
                        12631 -> 33 // ㅗㅛ -> !
                        12615 -> 63 // ㅇㅎ -> ?
                        else -> null
                    }
                } else {
                    when (keyCode) {
                        12610 -> 33 // ㅂ -> !
                        12616 -> 64 // ㅈ -> @
                        12599 -> 35 // ㄷ -> #
                        12593 -> 36 // ㄱ -> $
                        12613 -> 37 // ㅅ -> %
                        12635 -> 94 // ㅛ -> ^
                        12629 -> 38 // ㅕ -> &
                        12625 -> 42 // ㅑ -> *
                        12624 -> 40 // ㅐ -> (
                        12626 -> 41 // ㅔ -> )
                        else -> null
                    }
                }
                if (symbolHintCode != null) {
                    computedSymbolHint = TextKeyData(
                        code = symbolHintCode,
                        label = symbolHintCode.toChar().toString(),
                        type = KeyType.CHARACTER
                    )
                }
            }
        }
        val symbolHint = computedSymbolHint
        if (symbolHint != null) {
            val evaluatedSymbolHint = symbolHint.compute(evaluator)
            if (symbolHint.code != keyCode) {
                computedPopups.symbolHint = evaluatedSymbolHint
                mergePopups(evaluatedSymbolHint, evaluator, computedPopups::mergeSymbolHint)
                val hintSpecificPopupSet =
                    extendedPopups?.get(KeyVariation.ALL)?.get(symbolHint.label) ?: extendedPopupsDefault?.get(
                        KeyVariation.ALL
                    )?.get(symbolHint.label)
                hintSpecificPopupSet?.let { computedPopups.mergeSymbolHint(it, evaluator) }
            }
        }
        val numericHint = computedNumberHint
        if (numericHint != null) {
            val evaluatedNumberHint = numericHint.compute(evaluator)
            if (numericHint.code != keyCode) {
                computedPopups.numberHint = evaluatedNumberHint
                mergePopups(evaluatedNumberHint, evaluator, computedPopups::mergeNumberHint)
                val hintSpecificPopupSet =
                    extendedPopups?.get(KeyVariation.ALL)?.get(numericHint.label) ?: extendedPopupsDefault?.get(
                        KeyVariation.ALL
                    )?.get(numericHint.label)
                hintSpecificPopupSet?.let { computedPopups.mergeNumberHint(it, evaluator) }
            }
        }
    }

    private fun mergePopups(
        keyData: KeyData?,
        evaluator: ComputingEvaluator,
        merge: (popups: PopupSet<AbstractKeyData>, evaluator: ComputingEvaluator) -> Unit,
    ) {
        if (keyData?.popup != null) {
            merge(keyData.popup!!, evaluator)
        }
    }

    /**
     * Computes the label, hintedLabel and iconResId for [computedData] based on given [evaluator].
     */
    fun computeLabelsAndDrawables(evaluator: ComputingEvaluator) {
        label = evaluator.computeLabel(computedData)
        hintedLabel = null
        foregroundImageVector = evaluator.computeImageVector(computedData)

        val data = computedData
        if (data.type == KeyType.NUMERIC && evaluator.keyboard.mode == KeyboardMode.PHONE) {
            hintedLabel = when (data.code) {
                48 /* 0 */ -> "+"
                49 /* 1 */ -> ""
                50 /* 2 */ -> "ABC"
                51 /* 3 */ -> "DEF"
                52 /* 4 */ -> "GHI"
                53 /* 5 */ -> "JKL"
                54 /* 6 */ -> "MNO"
                55 /* 7 */ -> "PQRS"
                56 /* 8 */ -> "TUV"
                57 /* 9 */ -> "WXYZ"
                else -> null
            }
        } else if (!data.isSpaceKey() || data.type == KeyType.NUMERIC) {
            val prefs by FlorisPreferenceStore
            computedPopups.getPopupKeys(prefs.keyboard.keyHintConfiguration()).hint.let { hintData ->
                if (hintData?.isSpaceKey() == false) {
                    hintedLabel = hintData.asString(isForDisplay = true)
                    computedHintData = hintData
                } else {
                    hintedLabel = null
                    computedHintData = TextKeyData.UNSPECIFIED
                }
            }
            if (evaluator.keyboard.mode == KeyboardMode.GRID_16KEY && data.type == KeyType.CHARACTER) {
                val numHint = computedPopups.main?.asString(isForDisplay = true)?.takeIf { it.length == 1 && (it[0].isDigit() || "=*#?".contains(it[0])) }
                    ?: computedPopups.relevant.find { it.asString(isForDisplay = true).length == 1 && (it.asString(isForDisplay = true)[0].isDigit() || "=*#?".contains(it.asString(isForDisplay = true)[0])) }?.asString(isForDisplay = true)
                    ?: computedNumberHint?.asString(isForDisplay = true)
                if (numHint != null) {
                    hintedLabel = numHint
                }
            }
        }
    }

    override fun toString(): String {
        return computedData.toString()
    }
}
