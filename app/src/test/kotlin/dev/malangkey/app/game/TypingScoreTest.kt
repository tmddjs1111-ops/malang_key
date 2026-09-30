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

package dev.malangkey.app.game

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TypingScoreTest : FunSpec({
    test("counts Hangul keystrokes by jamo") {
        TypingScore.strokes('가').shouldBe(2) // ㄱ ㅏ
        TypingScore.strokes('한').shouldBe(3) // ㅎ ㅏ ㄴ
        TypingScore.strokes('화').shouldBe(3) // ㅎ ㅗ ㅏ
        TypingScore.strokes('닭').shouldBe(4) // ㄷ ㅏ ㄹ ㄱ
        TypingScore.strokes(' ').shouldBe(1)
    }

    test("counts typos as edits, so one missing letter is one error") {
        TypingScore.errors("동해 물과", "동해 물과").shouldBe(0)
        TypingScore.errors("동해물과", "동해 물과").shouldBe(1)
        TypingScore.errors("동헤 물과", "동해 물과").shouldBe(1)
        TypingScore.errors("", "동해").shouldBe(2)
    }

    test("a perfect run scores its speed; typos cut the score by accuracy squared") {
        val lines = listOf("대한 사람")
        val perfect = TypingScore.result(lines, lines, elapsedMillis = 60_000)
        perfect.accuracy.shouldBe(1f)
        perfect.score.shouldBe(perfect.strokesPerMinute)

        val typo = TypingScore.result(listOf("대한 사랑"), lines, elapsedMillis = 60_000)
        typo.errorCount.shouldBe(1)
        (typo.score < perfect.score).shouldBe(true)
    }

    test("anthem modes have the right number of lines") {
        AnthemLyrics.lines(full = false).size.shouldBe(4)
        AnthemLyrics.lines(full = true).size.shouldBe(16)
    }
})
