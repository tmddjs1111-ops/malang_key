/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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
@SerialName("hangul-unicode")
class HangulUnicode : Composer {
    override val id: String = "hangul-unicode"
    override val label: String = "Hangul Unicode"
    override val toRead: Int = 1

    @kotlinx.serialization.Transient
    private var lastInputTime: Long = 0
    @kotlinx.serialization.Transient
    private var lastInputStr: String = ""

    // Initial consonants, ordered for syllable creation
    private val initials = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    // Medial vowels, ordered for syllable creation
    private val medials = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    // Final consonants (including none), ordered for syllable creation
    private val finals = "_ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ"

    private val medialComp = mapOf(
        'ㅗ' to listOfNotNull("ㅏㅐㅣ", "ㅘㅙㅚ"),
        'ㅜ' to listOfNotNull("ㅓㅔㅣ", "ㅝㅞㅟ"),
        'ㅡ' to listOfNotNull("ㅣ", "ㅢ"),
    )

    private val finalComp = mapOf(
        'ㄱ' to listOfNotNull("ㅅ", "ㄳ"),
        'ㄴ' to listOfNotNull("ㅈㅎ", "ㄵㄶ"),
        'ㄹ' to listOfNotNull("ㄱㅁㅂㅅㅌㅍㅎ", "ㄺㄻㄼㄽㄾㄿㅀ"),
        'ㅂ' to listOfNotNull("ㅅ", "ㅄ"),
    )

    private fun reverseComp(map: Map<Char, List<String>>): Map<Char, List<Char>> {
        val ret = mutableMapOf<Char, List<Char>>()
        for ((first, v) in map) {
            val (seconds, comps) = v
            for (i in seconds.indices) {
                ret[comps[i]] = listOf(first, seconds[i])
            }
        }
        return ret
    }

    private val finalCompRev = reverseComp(finalComp)
    private val medialCompRev = reverseComp(medialComp)

    private val strokeMap = mapOf(
        'ㄱ' to 'ㅋ', 'ㄴ' to 'ㄷ', 'ㄷ' to 'ㅌ', 'ㅁ' to 'ㅂ', 'ㅂ' to 'ㅍ',
        'ㅅ' to 'ㅈ', 'ㅈ' to 'ㅊ', 'ㅇ' to 'ㅎ', 'ㅏ' to 'ㅑ', 'ㅓ' to 'ㅕ',
        'ㅗ' to 'ㅛ', 'ㅜ' to 'ㅠ'
    )

    private val doubleMap = mapOf(
        'ㄱ' to 'ㄲ', 'ㄷ' to 'ㄸ', 'ㅂ' to 'ㅃ', 'ㅅ' to 'ㅆ', 'ㅈ' to 'ㅉ',
        'ㅏ' to 'ㅑ', 'ㅓ' to 'ㅕ', 'ㅗ' to 'ㅛ', 'ㅜ' to 'ㅠ', 'ㅐ' to 'ㅒ', 'ㅔ' to 'ㅖ'
    )

    private val cycleMap = mapOf(
        'ㄱ' to "ㄱㅋㄲ", 'ㅋ' to "ㅋㄲㄱ", 'ㄲ' to "ㄲㄱㅋ",
        'ㄴ' to "ㄴㄹ", 'ㄹ' to "ㄹㄴ",
        'ㄷ' to "ㄷㅌㄸ", 'ㅌ' to "ㅌㄸㄷ", 'ㄸ' to "ㄸㄷㅌ",
        'ㅂ' to "ㅂㅍㅃ", 'ㅍ' to "ㅍㅃㅂ", 'ㅃ' to "ㅃㅂㅍ",
        'ㅁ' to "ㅁㅅㅆ", 'ㅅ' to "ㅅㅆㅁ", 'ㅆ' to "ㅆㅁㅅ",
        'ㅈ' to "ㅈㅊㅉ", 'ㅊ' to "ㅊㅉㅈ", 'ㅉ' to "ㅉㅈㅊ",
        'ㅇ' to "ㅇㅎ", 'ㅎ' to "ㅎㅇ",
        'ㅣ' to "ㅣㅡㅢ", 'ㅡ' to "ㅡㅢㅣ", 'ㅢ' to "ㅢㅣㅡ",
        'ㅏ' to "ㅏㅑ", 'ㅑ' to "ㅑㅏ",
        'ㅓ' to "ㅓㅕ", 'ㅕ' to "ㅕㅓ",
        'ㅗ' to "ㅗㅛ", 'ㅛ' to "ㅛㅗ",
        'ㅜ' to "ㅜㅠ", 'ㅠ' to "ㅠㅜ"
    )

    private val cheonjiinVowels = mapOf(
        "ㅣ\u318D" to "ㅏ", "ㅏ\u318D" to "ㅑ", "ㅏㅣ" to "ㅐ", "ㅑㅣ" to "ㅒ",
        "\u318Dㅣ" to "ㅓ", "\u318Dㅓ" to "ㅕ", "ㅓㅣ" to "ㅔ", "ㅕㅣ" to "ㅖ",
        "\u318Dㅡ" to "ㅗ", "ㅗ\u318D" to "ㅛ", "ㅡ\u318D" to "ㅜ", "ㅜ\u318D" to "ㅠ",
        "ㅡㅣ" to "ㅢ"
    )

    private fun syllable(ini: Int, med: Int, fin: Int): Char {
        return (ini * 588 + med * 28 + fin + 44032).toChar()
    }

    private fun syllableBlocks(syllOrd: Int): List<Int> {
        val initial = (syllOrd-44032)/588
        val medial = (syllOrd-44032-initial*588)/28
        val fin = (syllOrd-44032)%28
        return listOf(initial, medial, fin)
    }

    override fun getActions(precedingText: String, toInsert: String): Pair<Int, String> {
        val now = System.currentTimeMillis()
        val isFastRepeat = (now - lastInputTime < 400) && (toInsert == lastInputStr)
        
        lastInputTime = now
        lastInputStr = toInsert

        if (toInsert == "STROKE_ADD" || toInsert == "DOUBLE_CONSONANT" || toInsert == "CYCLE" || isFastRepeat) {
            if (precedingText.isEmpty()) return 0 to toInsert
            val lastChar = precedingText.last()
            
            val effectiveToInsert = if (isFastRepeat) {
                // If it's a layout like Sky or Naratgul, we might want CYCLE. 
                // If it's Danmoeum, we want DOUBLE_CONSONANT.
                // For simplicity, we try CYCLE then DOUBLE.
                if (cycleMap.containsKey(lastChar)) "CYCLE" else "DOUBLE_CONSONANT"
            } else {
                toInsert
            }

            if (effectiveToInsert == "CYCLE") {
                cycleMap[lastChar]?.let { return 1 to it[1].toString() }
            } else if (effectiveToInsert == "DOUBLE_CONSONANT") {
                doubleMap[lastChar]?.let { return 1 to it.toString() }
            } else if (effectiveToInsert == "STROKE_ADD") {
                strokeMap[lastChar]?.let { return 1 to it.toString() }
            }
            
            if (lastChar.code in 44032..55203) {
                val ord = lastChar.code - 44032
                val ini = ord / 588
                val med = ord % 588 / 28
                val fin = ord % 28
                
                if (fin > 0) {
                    val finChar = finals[fin]
                    val nextChar = when (effectiveToInsert) {
                        "CYCLE" -> cycleMap[finChar]?.get(1)
                        "STROKE_ADD" -> strokeMap[finChar]
                        "DOUBLE_CONSONANT" -> doubleMap[finChar]
                        else -> null
                    }
                    nextChar?.let {
                        val newFin = finals.indexOf(it)
                        if (newFin != -1) return 1 to syllable(ini, med, newFin).toString()
                    }
                } else {
                    if (effectiveToInsert == "CYCLE") {
                        val iniChar = initials[ini]
                        cycleMap[iniChar]?.let {
                            val newIni = initials.indexOf(it[1])
                            if (newIni != -1) return 1 to syllable(newIni, med, 0).toString()
                        }
                    } else {
                        val medChar = medials[med]
                        val nextMed = if (effectiveToInsert == "STROKE_ADD") strokeMap[medChar] else doubleMap[medChar]
                        nextMed?.let {
                            val newMed = medials.indexOf(it)
                            if (newMed != -1) return 1 to syllable(ini, newMed, 0).toString()
                        }
                        
                        val iniChar = initials[ini]
                        val nextIni = if (effectiveToInsert == "STROKE_ADD") strokeMap[iniChar] else doubleMap[iniChar]
                        nextIni?.let {
                            val newIni = initials.indexOf(it)
                            if (newIni != -1) return 1 to syllable(newIni, med, 0).toString()
                        }
                    }
                }
            }
            return if (isFastRepeat) 0 to toInsert else 0 to ""
        }

        val c = toInsert.firstOrNull()
        if (precedingText.isEmpty() || c == null) {
            return 0 to toInsert
        }
        val lastChar = precedingText.last()
        
        // Cheonjiin vowel composition
        val combo = "$lastChar$c"
        cheonjiinVowels[combo]?.let { return 1 to it }
        
        if (lastChar.code in 44032..55203) {
            val ord = lastChar.code - 44032
            val ini = ord / 588
            val med = ord % 588 / 28
            val fin = ord % 28
            if (fin == 0) {
                val medChar = medials[med]
                cheonjiinVowels["$medChar$c"]?.let {
                    val newMed = medials.indexOf(it)
                    if (newMed != -1) return 1 to syllable(ini, newMed, 0).toString()
                }
            }
        }
        val lastOrd = lastChar.code

        if (lastChar in initials && c in medials) {
            return Pair(1, "${syllable(initials.indexOf(lastChar), medials.indexOf(c), 0)}")
        } else if (lastOrd in 44032..55203) { // syllable
            val (ini, med, fin) = syllableBlocks(lastOrd)

            // underscore is a sentinel in the "finals" string
            if (c == '_') {
                return 0 to toInsert
            }

            //  if there is no final and the new char is a final, merge
            if (fin == 0 && c in finals) {
                return 1 to "${syllable(ini, med, finals.indexOf(c))}"
            }

            // if there is already a final but it is mergeable with the new char into a composed final, merge
            if ((finals[fin] in finalComp) && c in finalComp[finals[fin]]!![0]) {
                val tple = finalComp[finals[fin]]
                return 1 to "${syllable(ini, med, finals.indexOf(tple!![1][tple[0].indexOf(c)]))}"
            }

            // if there is a simple final and the new char is a medial, split the old syllable
            if (fin != 0 && finals[fin] !in finalCompRev && c in medials)
                return 1 to "${syllable(ini, med, 0)}${syllable(initials.indexOf(finals[fin]), medials.indexOf(c), 0)}"

            // if there is a composed final and the new char is a medial, split the old final
            if (finals[fin] in finalCompRev && c in medials) {
                return 1 to "${syllable(ini, med, finals.indexOf(finalCompRev.getValue(finals[fin])[0]))}${syllable(initials.indexOf(finalCompRev.getValue(finals[fin])[1]), medials.indexOf(c), 0)}"
            }

            // if no final yet, and current medial can be composed with new char, merge
            if (medials[med] in medialComp && c in medialComp.getValue(medials[med])[0] && fin == 0) {
                val tple = medialComp[medials[med]]
                return 1 to "${syllable(ini, medials.indexOf(tple!![1][tple[0].indexOf(c)]), 0)}"
            }
        } else if (lastChar in medialComp.keys && medialComp[lastChar]?.get(0)?.contains(c) == true) { // medial+final
            return 1 to ""+ medialComp[lastChar]?.get(1)!![medialComp[lastChar]?.get(0)!!.indexOf(c)]
        } else if (lastChar in finalComp.keys && finalComp[lastChar]?.get(0)?.contains(c) == true) { // final+final
            return 1 to ""+ finalComp[lastChar]?.get(1)!![finalComp[lastChar]?.get(0)!!.indexOf(c)]
        }

        return 0 to toInsert
    }
}
