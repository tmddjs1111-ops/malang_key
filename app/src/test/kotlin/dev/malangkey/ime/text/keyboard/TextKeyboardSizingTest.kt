/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.malangkey.ime.text.keyboard

import dev.malangkey.ime.keyboard.KeyboardMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.floats.shouldBeExactly

class TextKeyboardSizingTest : FunSpec({
    fun keyboard(hasCompactNumberRow: Boolean): TextKeyboard {
        return TextKeyboard(
            arrangement = Array(5) { arrayOf(TextKey(TextKeyData(code = 49 + it))) },
            mode = KeyboardMode.CHARACTERS,
            extendedPopupMapping = null,
            extendedPopupMappingDefault = null,
            hasCompactNumberRow = hasCompactNumberRow,
        )
    }

    test("number row reduces total keyboard height proportionally") {
        keyboard(hasCompactNumberRow = true).heightInRows()
            .shouldBeExactly(4.0f + TextKeyboard.CompactNumberRowHeightFactor)
    }

    test("keyboard without number row keeps full row height") {
        keyboard(hasCompactNumberRow = false).heightInRows().shouldBeExactly(5.0f)
    }
})
