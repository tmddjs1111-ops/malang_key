/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.ime.text.keyboard

import dev.malangkey.ime.text.key.KeyCode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JapaneseMultiTapStateTest : FunSpec({
    val vowels = listOf('あ', 'い', 'う', 'え', 'お').map(Char::code)

    test("all ten Japanese kana row keys support multi-tap") {
        listOf('あ', 'か', 'さ', 'た', 'な', 'は', 'ま', 'や', 'ら', 'わ')
            .all { isJapaneseKanaRowBase(it.code) }
            .shouldBe(true)
    }

    test("Japanese space uses the same language swipe route as standard spaces") {
        listOf(KeyCode.SPACE, KeyCode.CJK_SPACE, KeyCode.JAPANESE_SPACE)
            .all(::isLanguageSwipeSpace)
            .shouldBe(true)
        isLanguageSwipeSpace(KeyCode.JAPANESE_CONVERT).shouldBe(false)
    }

    test("same kana key advances through the row and wraps") {
        val state = JapaneseMultiTapState(timeoutMillis = 700L)
        var now = 1_000L

        vowels.forEachIndexed { expectedIndex, _ ->
            val precedingCode = if (expectedIndex == 0) null else vowels[expectedIndex - 1]
            state.next('あ'.code, vowels, precedingCode, now).shouldBe(
                JapaneseMultiTapState.Result(expectedIndex, expectedIndex > 0),
            )
            now += 100L
        }

        state.next('あ'.code, vowels, 'お'.code, now).shouldBe(
            JapaneseMultiTapState.Result(index = 0, replacePrevious = true),
        )
    }

    test("a different kana row starts from its base character") {
        val state = JapaneseMultiTapState(timeoutMillis = 700L)
        state.next('あ'.code, vowels, null, 1_000L)

        state.next('か'.code, listOf('か'.code, 'き'.code), 'あ'.code, 1_100L).shouldBe(
            JapaneseMultiTapState.Result(index = 0, replacePrevious = false),
        )
    }

    test("expired sequence inserts another base character") {
        val state = JapaneseMultiTapState(timeoutMillis = 700L)
        state.next('あ'.code, vowels, null, 1_000L)

        state.next('あ'.code, vowels, 'あ'.code, 1_701L).shouldBe(
            JapaneseMultiTapState.Result(index = 0, replacePrevious = false),
        )
    }

    test("cursor or surrounding text change prevents replacement") {
        val state = JapaneseMultiTapState(timeoutMillis = 700L)
        state.next('あ'.code, vowels, null, 1_000L)

        state.next('あ'.code, vowels, 'か'.code, 1_100L).shouldBe(
            JapaneseMultiTapState.Result(index = 0, replacePrevious = false),
        )
    }

    test("reset prevents a previous tap from continuing") {
        val state = JapaneseMultiTapState(timeoutMillis = 700L)
        state.next('あ'.code, vowels, null, 1_000L)
        state.reset()

        state.next('あ'.code, vowels, 'あ'.code, 1_100L).shouldBe(
            JapaneseMultiTapState.Result(index = 0, replacePrevious = false),
        )
    }
})
