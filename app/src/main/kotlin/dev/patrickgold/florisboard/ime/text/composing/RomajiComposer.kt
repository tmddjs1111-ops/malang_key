/*
 * Copyright (C) 2026 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.text.composing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("romaji")
class RomajiComposer : Composer {
    override val id: String = "romaji"
    override val label: String = "Romaji"
    override val toRead: Int = 3

    companion object {
        private val romajiMap = mapOf(
            "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
            "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
            "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
            "sa" to "さ", "shi" to "し", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
            "za" to "ざ", "ji" to "じ", "zi" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
            "ta" to "た", "chi" to "ち", "ti" to "ち", "tsu" to "つ", "tu" to "つ", "te" to "て", "to" to "と",
            "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
            "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の", "nn" to "ん",
            "ha" to "は", "hi" to "ひ", "fu" to "ふ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
            "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
            "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
            "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
            "ya" to "や", "yu" to "ゆ", "yo" to "よ",
            "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
            "wa" to "わ", "wo" to "を", "n'" to "ん",
            "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
            "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
            "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
            "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
            "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
            "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
            "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
            "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
            "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
            "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
            "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ"
        )
    }

    override fun getActions(precedingText: String, toInsert: String, layoutId: String?): Pair<Int, String> {
        if (toInsert.isEmpty()) return 0 to ""
        val c = toInsert.lowercase()
        
        var buffer = precedingText.takeLast(2) + c
        while (buffer.isNotEmpty()) {
            val hira = romajiMap[buffer]
            if (hira != null) {
                return (buffer.length - 1) to hira
            }
            buffer = buffer.substring(1)
        }
        
        if (precedingText.isNotEmpty()) {
            val lastChar = precedingText.last().lowercaseChar()
            if (lastChar == c[0] && lastChar in "bcdfghjklmnpqrstvwxyz" && lastChar != 'n') {
                return 1 to ("っ" + c)
            }
        }
        
        return 0 to toInsert
    }
}
