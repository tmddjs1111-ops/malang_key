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

package dev.malangkey.ime.nlp.japanese

import dev.malangkey.ime.editor.EditorRange
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JapaneseLanguageProviderTest : FunSpec({
    test("tracks the trailing hiragana reading") {
        determineJapaneseComposingRange("前の文 にほん", 0).shouldBe(EditorRange(4, 7))
    }

    test("tracks katakana and the prolonged sound mark") {
        determineJapaneseComposingRange("入力スーパー", 0).shouldBe(EditorRange(2, 6))
    }

    test("stops at the last committed position") {
        determineJapaneseComposingRange("かなにほん", 2).shouldBe(EditorRange(2, 5))
    }

    test("returns unspecified when the cursor is not after kana") {
        determineJapaneseComposingRange("にほん。", 0).shouldBe(EditorRange.Unspecified)
    }

    test("supports half-width katakana readings") {
        determineJapaneseComposingRange("abcﾆﾎﾝ", 0).shouldBe(EditorRange(3, 6))
    }
})
