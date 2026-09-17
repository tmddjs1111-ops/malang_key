/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.ime.text.keyboard

import dev.malangkey.ime.keyboard.KeyboardMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextKeyHintMappingTest {
    @Test
    fun `English symbol hints follow the reference rows`() {
        val expected = mapOf(
            'a' to '!', 's' to '\'', 'd' to '"', 'f' to '^', 'g' to '#',
            'h' to '&', 'j' to '*', 'k' to '(', 'l' to ')',
            'z' to '~', 'x' to '-', 'c' to '+', 'v' to '=',
            'b' to ':', 'n' to ';', 'm' to '?',
        )

        expected.forEach { (key, hint) ->
            assertEquals(
                hint.code,
                symbolHintCodeForKey(KeyboardMode.CHARACTERS, key.code),
                "Unexpected hint for $key",
            )
            assertEquals(
                hint.code,
                symbolHintCodeForKey(KeyboardMode.CHARACTERS, key.uppercaseChar().code),
                "Unexpected shifted hint for $key",
            )
        }
    }

    @Test
    fun `English number row letters do not also show symbol hints`() {
        "qwertyuiop".forEach { key ->
            assertEquals(null, symbolHintCodeForKey(KeyboardMode.CHARACTERS, key.code))
        }
    }

    @Test
    fun `Japanese grid hint mapping remains unchanged`() {
        assertEquals('@'.code, symbolHintCodeForKey(KeyboardMode.GRID_16KEY, 12593))
        assertEquals('?'.code, symbolHintCodeForKey(KeyboardMode.GRID_16KEY, 12615))
    }
}
