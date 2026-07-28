package dev.patrickgold.florisboard.ime.text.composing

import android.os.SystemClock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("hangul-unicode")
object HangulUnicode : Composer {
    override val id: String = "hangul-unicode"
    override val label: String = "Hangul Unicode"
    override val toRead: Int = 3

    private val CHOSEONG = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    private val JUNGSEONG = listOf("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ")
    private val JONGSEONG = listOf("", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

    private val choseongMap = CHOSEONG.mapIndexed { index, s -> s to index }.toMap()
    private val jungMap = JUNGSEONG.mapIndexed { index, s -> s to index }.toMap()
    private val jongMap = JONGSEONG.mapIndexed { index, s -> s to index }.filter { it.first.isNotEmpty() }.toMap()

    private val doubleJung = mapOf("ㅗㅏ" to "ㅘ", "ㅗㅐ" to "ㅙ", "ㅗㅣ" to "ㅚ", "ㅜㅓ" to "ㅝ", "ㅜㅔ" to "ㅞ", "ㅜㅣ" to "ㅟ", "ㅡㅣ" to "ㅢ")
    private val doubleJong = mapOf("ㄱㅅ" to "ㄳ", "ㄴㅈ" to "ㄵ", "ㄴㅎ" to "ㄶ", "ㄹㄱ" to "ㄺ", "ㄹㅁ" to "ㄻ", "ㄹㅂ" to "ㄼ", "ㄹㅅ" to "ㄽ", "ㄹㅌ" to "ㄾ", "ㄹㅍ" to "ㄿ", "ㄹㅎ" to "ㅀ", "ㅂㅅ" to "ㅄ")

    private val skyCycleStr = mapOf(
        "ㄱ" to listOf("ㄱ", "ㅋ", "ㄲ"),
        "ㄴ" to listOf("ㄴ", "ㄹ"),
        "ㄷ" to listOf("ㄷ", "ㅌ", "ㄸ"),
        "ㅂ" to listOf("ㅂ", "ㅍ", "ㅃ"),
        "ㅁ" to listOf("ㅁ", "ㅅ", "ㅆ"), // For Sky, this key is ㅁㅅ
        "ㅈ" to listOf("ㅈ", "ㅊ", "ㅉ"),
        "ㅇ" to listOf("ㅇ", "ㅎ"),
        "ㅣ" to listOf("ㅣ", "ㅡ", "ㅢ"),
        "ㅏ" to listOf("ㅏ", "ㅑ"),
        "ㅓ" to listOf("ㅓ", "ㅕ"),
        "ㅗ" to listOf("ㅗ", "ㅛ"),
        "ㅜ" to listOf("ㅜ", "ㅠ")
    )

    private val cheonjiinCycleStr = mapOf(
        "ㄱ" to listOf("ㄱ", "ㅋ", "ㄲ"),
        "ㄴ" to listOf("ㄴ", "ㄹ"),
        "ㄷ" to listOf("ㄷ", "ㅌ", "ㄸ"),
        "ㅂ" to listOf("ㅂ", "ㅍ", "ㅃ"),
        "ㅅ" to listOf("ㅅ", "ㅎ", "ㅆ"),
        "ㅈ" to listOf("ㅈ", "ㅊ", "ㅉ"),
        "ㅇ" to listOf("ㅇ", "ㅁ")
    )

    private val danmoeumDouble = mapOf(
        "ㄱ" to "ㄲ", "ㄷ" to "ㄸ", "ㅂ" to "ㅃ", "ㅅ" to "ㅆ", "ㅈ" to "ㅉ",
        "ㅏ" to "ㅑ", "ㅓ" to "ㅕ", "ㅗ" to "ㅛ", "ㅜ" to "ㅠ", "ㅐ" to "ㅒ", "ㅔ" to "ㅖ"
    )

    private val naratgulStroke = mapOf(
        "ㄱ" to "ㅋ", "ㅋ" to "ㄱ",
        "ㄴ" to "ㄷ", "ㄷ" to "ㅌ", "ㅌ" to "ㄴ",
        "ㅁ" to "ㅂ", "ㅂ" to "ㅍ", "ㅍ" to "ㅁ",
        "ㅅ" to "ㅈ", "ㅈ" to "ㅊ", "ㅊ" to "ㅅ",
        "ㅇ" to "ㅎ", "ㅎ" to "ㅇ",
        "ㅏ" to "ㅑ", "ㅑ" to "ㅏ",
        "ㅓ" to "ㅕ", "ㅕ" to "ㅓ",
        "ㅗ" to "ㅛ", "ㅛ" to "ㅗ",
        "ㅜ" to "ㅠ", "ㅠ" to "ㅜ"
    )
    private val naratgulDouble = mapOf(
        "ㄱ" to "ㄲ", "ㄲ" to "ㄱ",
        "ㄷ" to "ㄸ", "ㄸ" to "ㄷ",
        "ㅂ" to "ㅃ", "ㅃ" to "ㅂ",
        "ㅅ" to "ㅆ", "ㅆ" to "ㅅ",
        "ㅈ" to "ㅉ", "ㅉ" to "ㅈ",
        "ㅇ" to "ㅎ", "ㅎ" to "ㅇ"
    )

    private val qwertyEnglishToKoreanMap = mapOf(
        "q" to "ㅂ", "w" to "ㅈ", "e" to "ㄷ", "r" to "ㄱ", "t" to "ㅅ", "y" to "ㅛ", "u" to "ㅕ", "i" to "ㅑ", "o" to "ㅐ", "p" to "ㅔ",
        "a" to "ㅁ", "s" to "ㄴ", "d" to "ㅇ", "f" to "ㄹ", "g" to "ㅎ", "h" to "ㅗ", "j" to "ㅓ", "k" to "ㅏ", "l" to "ㅣ",
        "z" to "ㅋ", "x" to "ㅌ", "c" to "ㅊ", "v" to "ㅍ", "b" to "ㅠ", "n" to "ㅜ", "m" to "ㅡ",
        "Q" to "ㅃ", "W" to "ㅉ", "E" to "ㄸ", "R" to "ㄲ", "T" to "ㅆ", "Y" to "ㅛ", "U" to "ㅕ", "I" to "ㅑ", "O" to "ㅒ", "P" to "ㅖ",
        "A" to "ㅁ", "S" to "ㄴ", "D" to "ㅇ", "F" to "ㄹ", "G" to "ㅎ", "H" to "ㅗ", "J" to "ㅓ", "K" to "ㅏ", "L" to "ㅣ",
        "Z" to "ㅋ", "X" to "ㅌ", "C" to "ㅊ", "V" to "ㅍ", "B" to "ㅠ", "N" to "ㅜ", "M" to "ㅡ"
    )

    private var lastInputKey: String = ""
    private var lastInputTime: Long = 0L
    private var escapedBySpace: Boolean = false

    private fun disassemble(c: Char): List<Int> {
        val base = c.code - 0xAC00
        val initial = base / (21 * 28)
        val medial = (base % (21 * 28)) / 28
        val fin = base % 28
        return listOf(initial, medial, fin)
    }

    private class EngineState(var cho: Int? = null, var jung: Int? = null, var jong: Int? = null) {
        val outNodes = mutableListOf<String>()
        var extraDeleteCount = 0
        var precedingText = ""

        fun commitPreedit() {
            if (cho != null || jung != null) {
                outNodes.add(toPreedit())
            }
            cho = null
            jung = null
            jong = null
        }

        fun toPreedit(): String {
            if (cho == null && jung == null) return ""
            if (cho != null && jung == null) return CHOSEONG[cho!!]
            if (cho == null && jung != null) {
                if (jung == 100) return "·"
                if (jung == 101) return "··"
                return JUNGSEONG[jung!!]
            }
            if (jung == 100) return CHOSEONG[cho!!] + "·"
            if (jung == 101) return CHOSEONG[cho!!] + "··"
            val sIndex = (cho!! * 21 + jung!!) * 28 + (jong ?: 0)
            return (0xAC00 + sIndex).toChar().toString()
        }
    }

    override fun getActions(precedingText: String, toInsert: String, layoutId: String?): Pair<Int, String> {
        val now = SystemClock.uptimeMillis()
        if (toInsert.isEmpty()) return 0 to toInsert
        val inputStr = toInsert

        var cho: Int? = null
        var jung: Int? = null
        var jong: Int? = null
        var deleteCount = 0

        val lastChar = precedingText.lastOrNull()
        if (lastChar != null && lastInputKey != "SPACE") {
            val code = lastChar.code
            if (code in 0xAC00..0xD7A3) {
                val parts = disassemble(lastChar)
                cho = parts[0]
                jung = parts[1]
                jong = if (parts[2] > 0) parts[2] else null
                deleteCount = 1
            } else if (code in 0x3131..0x314E) {
                cho = choseongMap[lastChar.toString()]
                if (cho == null) {
                    jong = jongMap[lastChar.toString()]
                }
                deleteCount = 1
            } else if (code in 0x314F..0x3163) {
                jung = jungMap[lastChar.toString()]
                deleteCount = 1
            } else if (lastChar == '·') {
                deleteCount = 1
                if (precedingText.length > 1) {
                    val prevPrev = precedingText[precedingText.length - 2]
                    if (prevPrev == '·') {
                        deleteCount = 2
                        if (precedingText.length > 2) {
                            val p3 = precedingText[precedingText.length - 3]
                            if (p3.code in 0x3131..0x314E) {
                                cho = choseongMap[p3.toString()]
                                deleteCount = 3
                            }
                        }
                        jung = 101
                    } else if (prevPrev.code in 0x3131..0x314E) {
                        cho = choseongMap[prevPrev.toString()]
                        deleteCount = 2
                        jung = 100
                    } else {
                        jung = 100
                    }
                } else {
                    jung = 100
                }
            }
        }

        val state = EngineState(cho, jung, jong)
        state.precedingText = precedingText
        val lId = layoutId ?: "korean"
        
        var mappedInputStr = inputStr
        if (lId == "korean" || lId.startsWith("korean_") || lId.startsWith("qwerty")) {
            mappedInputStr = qwertyEnglishToKoreanMap[inputStr] ?: inputStr
        }

        val isSameCycle = lastInputKey == mappedInputStr
        if (!isSameCycle && lastInputKey != "SPACE") {
            escapedBySpace = false
        }

        when {
            lId.startsWith("korean_sky") -> handleSky(mappedInputStr, state, now)
            lId.startsWith("korean_cheonjiin") -> handleCheonjiin(mappedInputStr, state, now)
            lId.startsWith("korean_danmoeum") -> handleDanmoeum(mappedInputStr, state, now)
            lId.startsWith("korean_naratgul") -> handleNaratgul(mappedInputStr, state, now)
            else -> handleQwerty(mappedInputStr, state)
        }

        val tempText = state.outNodes.joinToString("") + state.toPreedit()
        
        lastInputKey = inputStr
        lastInputTime = now

        return (deleteCount + state.extraDeleteCount) to tempText
    }

    override fun getActionsForBackspace(precedingText: String, layoutId: String?): Pair<Int, String>? {
        lastInputKey = ""
        val lastChar = precedingText.lastOrNull() ?: return null
        val code = lastChar.code
        
        if (code in 0xAC00..0xD7A3) {
            val parts = disassemble(lastChar)
            val choIdx = parts[0]
            val jungIdx = parts[1]
            val jongIdx = if (parts[2] > 0) parts[2] else null

            if (jongIdx != null) {
                val j = JONGSEONG[jongIdx]
                var origin = ""
                for ((k, v) in doubleJong) {
                    if (v == j) origin = k
                }
                if (origin.isNotEmpty()) {
                    val state = EngineState(choIdx, jungIdx, jongMap[origin[0].toString()])
                    return 1 to state.toPreedit()
                } else {
                    val state = EngineState(choIdx, jungIdx, null)
                    return 1 to state.toPreedit()
                }
            } else {
                val j = JUNGSEONG[jungIdx]
                var origin = ""
                for ((k, v) in doubleJung) {
                    if (v == j) origin = k
                }
                if (origin.isNotEmpty()) {
                    val state = EngineState(choIdx, jungMap[origin[0].toString()], null)
                    return 1 to state.toPreedit()
                } else {
                    val state = EngineState(choIdx, null, null)
                    return 1 to state.toPreedit()
                }
            }
        }
        return null
    }

    override fun onSpacePressed(precedingText: String, layoutId: String?): Boolean {
        val now = SystemClock.uptimeMillis()
        val lId = layoutId ?: "korean"
        if (lId.contains("sky") || lId.contains("cheonjiin") || lId.contains("naratgul")) {
            // If the last key was a character, the first space always just finalizes.
            if (lastInputKey != "SPACE" && lastInputKey.isNotEmpty()) {
                val isHangulKey = lastInputKey in CHOSEONG || lastInputKey in JUNGSEONG || lastInputKey == "·" || lastInputKey == "ㆍ"
                if (isHangulKey) {
                    lastInputKey = "SPACE"
                    escapedBySpace = true
                    return true
                }
            }
        }
        return false
    }

    private fun handleQwerty(key: String, state: EngineState) {
        if (jungMap.containsKey(key)) {
            if (state.cho == null) {
                if (state.jung == null) {
                    state.jung = jungMap[key]
                } else {
                    val combined = JUNGSEONG[state.jung!!] + key
                    if (doubleJung.containsKey(combined)) {
                        state.jung = jungMap[doubleJung[combined]]
                    } else {
                        state.commitPreedit()
                        state.jung = jungMap[key]
                    }
                }
            } else if (state.jung == null) {
                state.jung = jungMap[key]
            } else if (state.jong == null) {
                val combined = JUNGSEONG[state.jung!!] + key
                if (doubleJung.containsKey(combined)) {
                    state.jung = jungMap[doubleJung[combined]]
                } else {
                    state.commitPreedit()
                    state.jung = jungMap[key]
                }
            } else {
                val j = JONGSEONG[state.jong!!]
                var origin = ""
                for ((k, v) in doubleJong) {
                    if (v == j) origin = k
                }
                var nextCho = ""
                if (origin.isNotEmpty()) {
                    state.jong = jongMap[origin[0].toString()]
                    nextCho = origin[1].toString()
                } else {
                    nextCho = j
                    state.jong = null
                }
                state.commitPreedit()
                state.cho = choseongMap[nextCho] ?: choseongMap["ㅇ"] // fallback
                state.jung = jungMap[key]
            }
        } else if (choseongMap.containsKey(key)) {
            if (state.cho == null) {
                state.cho = choseongMap[key]
            } else if (state.jung == null) {
                state.commitPreedit()
                state.cho = choseongMap[key]
            } else if (state.jong == null) {
                if (jongMap.containsKey(key)) {
                    state.jong = jongMap[key]
                } else {
                    state.commitPreedit()
                    state.cho = choseongMap[key]
                }
            } else {
                val combined = JONGSEONG[state.jong!!] + key
                if (doubleJong.containsKey(combined)) {
                    state.jong = jongMap[doubleJong[combined]]
                } else {
                    state.commitPreedit()
                    state.cho = choseongMap[key]
                }
            }
        } else {
            state.commitPreedit()
            state.outNodes.add(key)
        }
    }

    private fun tryMergeBack(state: EngineState, nextChoStr: String): Boolean {
        if (escapedBySpace) return false
        if (state.outNodes.isNotEmpty()) {
            val lastCommitted = state.outNodes.last()
            if (lastCommitted.length == 1) {
                val code = lastCommitted[0].code
                if (code in 0xAC00..0xD7A3) {
                    val parts = disassemble(lastCommitted[0])
                    val prevCho = parts[0]
                    val prevJung = parts[1]
                    val prevJong = parts[2]
                    if (prevJong > 0) {
                        val prevJongStr = JONGSEONG[prevJong]
                        val combined = prevJongStr + nextChoStr
                        if (doubleJong.containsKey(combined)) {
                            state.outNodes.removeAt(state.outNodes.size - 1)
                            state.cho = prevCho
                            state.jung = prevJung
                            state.jong = jongMap[doubleJong[combined]]
                            return true
                        }
                    } else {
                        if (jongMap.containsKey(nextChoStr)) {
                            state.outNodes.removeAt(state.outNodes.size - 1)
                            state.cho = prevCho
                            state.jung = prevJung
                            state.jong = jongMap[nextChoStr]
                            return true
                        }
                    }
                }
            }
        } else {
            val pt = state.precedingText
            val targetIdx = pt.length - 2 - state.extraDeleteCount
            if (targetIdx >= 0) {
                val prevChar = pt[targetIdx]
                val code = prevChar.code
                if (code in 0xAC00..0xD7A3) {
                    val parts = disassemble(prevChar)
                    val prevCho = parts[0]
                    val prevJung = parts[1]
                    val prevJong = parts[2]
                    if (prevJong > 0) {
                        val prevJongStr = JONGSEONG[prevJong]
                        val combined = prevJongStr + nextChoStr
                        if (doubleJong.containsKey(combined)) {
                            state.extraDeleteCount++
                            state.cho = prevCho
                            state.jung = prevJung
                            state.jong = jongMap[doubleJong[combined]]
                            return true
                        }
                    } else {
                        if (jongMap.containsKey(nextChoStr)) {
                            state.extraDeleteCount++
                            state.cho = prevCho
                            state.jung = prevJung
                            state.jong = jongMap[nextChoStr]
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun cycleJongseong(state: EngineState, cycle: List<String>, isVowel: Boolean) {
        val idx = cycle.indexOf(JONGSEONG[state.jong!!])
        if (idx != -1) {
            val nextCycleChar = cycle[(idx + 1) % cycle.size]
            if (jongMap.containsKey(nextCycleChar)) {
                state.jong = jongMap[nextCycleChar]
            } else {
                state.jong = null
                state.commitPreedit()
                state.cho = choseongMap[nextCycleChar]
            }
        } else {
            val j = JONGSEONG[state.jong!!]
            var origin = ""
            for ((k, v) in doubleJong) {
                if (v == j) origin = k
            }
            if (origin.isNotEmpty()) {
                val secondJong = origin[1].toString()
                val secondIdx = cycle.indexOf(secondJong)
                if (secondIdx != -1) {
                    val nextCycleChar = cycle[(secondIdx + 1) % cycle.size]
                    val newCombined = origin[0].toString() + nextCycleChar
                    if (doubleJong.containsKey(newCombined)) {
                        state.jong = jongMap[doubleJong[newCombined]]
                    } else {
                        state.jong = jongMap[origin[0].toString()]
                        state.commitPreedit()
                        state.cho = choseongMap[nextCycleChar]
                    }
                } else {
                    state.commitPreedit()
                    if (isVowel) state.jung = jungMap[cycle[0]] else state.cho = choseongMap[cycle[0]]
                }
            } else {
                state.commitPreedit()
                if (isVowel) state.jung = jungMap[cycle[0]] else state.cho = choseongMap[cycle[0]]
            }
        }
    }

    private fun handleSky(key: String, state: EngineState, now: Long) {
        val isSameCycle = lastInputKey == key
        val isWithinTime = (now - lastInputTime) < 800

        // Handle space conversion (two fast presses on space = space, 1 = exit)
        if (key == " " && isSameCycle && isWithinTime) {
            state.outNodes.add(" ")
            return
        }

        if (skyCycleStr.containsKey(key)) {
            val cycle = skyCycleStr[key]!!
            val isVowel = jungMap.containsKey(cycle[0])

            if (isSameCycle && isWithinTime) {
                if (state.jong != null) {
                    cycleJongseong(state, cycle, isVowel)
                } else if (state.jung != null && state.cho != null) {
                    if (isVowel) {
                        val idx = cycle.indexOf(JUNGSEONG[state.jung!!])
                        if (idx != -1) {
                            state.jung = jungMap[cycle[(idx + 1) % cycle.size]]
                        } else {
                            state.commitPreedit()
                            state.jung = jungMap[cycle[0]]
                        }
                    } else {
                        if (jongMap.containsKey(cycle[0])) {
                            state.jong = jongMap[cycle[0]]
                        } else {
                            state.commitPreedit()
                            state.cho = choseongMap[cycle[0]]
                        }
                    }
                } else if (state.cho != null && state.jung == null) {
                    if (isVowel) {
                        state.jung = jungMap[cycle[0]]
                    } else {
                        val idx = cycle.indexOf(CHOSEONG[state.cho!!])
                        if (idx != -1) {
                            val nextChoStr = cycle[(idx + 1) % cycle.size]
                            if (!tryMergeBack(state, nextChoStr)) {
                                state.cho = choseongMap[nextChoStr]
                            }
                        } else {
                            state.commitPreedit()
                            state.cho = choseongMap[cycle[0]]
                        }
                    }
                } else if (state.cho == null && state.jung != null) {
                    if (isVowel) {
                        val idx = cycle.indexOf(JUNGSEONG[state.jung!!])
                        if (idx != -1) {
                            state.jung = jungMap[cycle[(idx + 1) % cycle.size]]
                        } else {
                            state.commitPreedit()
                            state.jung = jungMap[cycle[0]]
                        }
                    } else {
                        state.commitPreedit()
                        state.cho = choseongMap[cycle[0]]
                    }
                } else {
                    if (isVowel) state.jung = jungMap[cycle[0]] else state.cho = choseongMap[cycle[0]]
                }
            } else {
                if (isVowel) {
                    if (state.cho == null && state.jung == null) {
                        state.jung = jungMap[cycle[0]]
                    } else if (state.cho != null && state.jung == null) {
                        state.jung = jungMap[cycle[0]]
                    } else if (state.jung != null && state.jong == null) {
                        val curr = JUNGSEONG[state.jung!!]
                        val input = cycle[0]
                        var next = ""
                        when {
                            curr == "ㅏ" && input == "ㅣ" -> next = "ㅐ"
                            curr == "ㅑ" && input == "ㅣ" -> next = "ㅒ"
                            curr == "ㅓ" && input == "ㅣ" -> next = "ㅔ"
                            curr == "ㅕ" && input == "ㅣ" -> next = "ㅖ"
                            curr == "ㅗ" && input == "ㅏ" -> next = "ㅘ"
                            curr == "ㅗ" && input == "ㅣ" -> next = "ㅚ"
                            curr == "ㅜ" && input == "ㅓ" -> next = "ㅝ"
                            curr == "ㅜ" && input == "ㅣ" -> next = "ㅟ"
                            curr == "ㅡ" && input == "ㅣ" -> next = "ㅢ"
                            curr == "ㅘ" && input == "ㅣ" -> next = "ㅙ"
                            curr == "ㅝ" && input == "ㅣ" -> next = "ㅞ"
                        }
                        if (next.isNotEmpty()) {
                            state.jung = jungMap[next]
                        } else {
                            state.commitPreedit()
                            state.jung = jungMap[cycle[0]]
                        }
                    } else {
                        val j = JONGSEONG[state.jong!!]
                        var origin = ""
                        for ((k, v) in doubleJong) {
                            if (v == j) origin = k
                        }
                        var nextCho = ""
                        if (origin.isNotEmpty()) {
                            state.jong = jongMap[origin[0].toString()]
                            nextCho = origin[1].toString()
                        } else {
                            nextCho = j
                            state.jong = null
                        }
                        state.commitPreedit()
                        state.cho = choseongMap[nextCho] ?: choseongMap["ㅇ"]
                        state.jung = jungMap[cycle[0]]
                    }
                } else {
                    if (state.cho != null && state.jung != null && state.jong == null && jongMap.containsKey(cycle[0])) {
                        state.jong = jongMap[cycle[0]]
                    } else if (state.cho != null && state.jung != null && state.jong != null) {
                        val curr = JONGSEONG[state.jong!!]
                        val combined = curr + cycle[0]
                        if (doubleJong.containsKey(combined)) {
                            state.jong = jongMap[doubleJong[combined]]
                        } else {
                            state.commitPreedit()
                            state.cho = choseongMap[cycle[0]]
                        }
                    } else {
                        state.commitPreedit()
                        state.cho = choseongMap[cycle[0]]
                    }
                }
            }
        } else {
            if (key == "·") {
                // handle dot if it isn't part of sky cycle
                state.commitPreedit()
                state.outNodes.add("·")
            } else if (key != " ") {
                handleQwerty(key, state)
            } else {
                state.commitPreedit()
            }
        }
    }

    private fun handleCheonjiin(key: String, state: EngineState, now: Long) {
        val isSameCycle = lastInputKey == key
        val isWithinTime = (now - lastInputTime) < 800

        if (cheonjiinCycleStr.containsKey(key)) {
            val cycle = cheonjiinCycleStr[key]!!
            if (isSameCycle && isWithinTime) {
                if (state.jong != null) {
                    cycleJongseong(state, cycle, false)
                } else if (state.jung != null && state.cho != null) {
                    if (jongMap.containsKey(cycle[0])) {
                        state.jong = jongMap[cycle[0]]
                    } else {
                        state.commitPreedit()
                        state.cho = choseongMap[cycle[0]]
                    }
                } else if (state.cho != null && state.jung == null) {
                    val idx = cycle.indexOf(CHOSEONG[state.cho!!])
                    if (idx != -1) {
                        val nextChoStr = cycle[(idx + 1) % cycle.size]
                        if (!tryMergeBack(state, nextChoStr)) {
                            state.cho = choseongMap[nextChoStr]
                        }
                    } else {
                        state.commitPreedit()
                        state.cho = choseongMap[cycle[0]]
                    }
                } else {
                    state.commitPreedit()
                    state.cho = choseongMap[cycle[0]]
                }
            } else {
                if (state.cho != null && state.jung != null && state.jong == null && jongMap.containsKey(cycle[0])) {
                    state.jong = jongMap[cycle[0]]
                } else if (state.cho != null && state.jung != null && state.jong != null) {
                    val curr = JONGSEONG[state.jong!!]
                    val combined = curr + cycle[0]
                    if (doubleJong.containsKey(combined)) {
                        state.jong = jongMap[doubleJong[combined]]
                    } else {
                        state.commitPreedit()
                        state.cho = choseongMap[cycle[0]]
                    }
                } else {
                    state.commitPreedit()
                    state.cho = choseongMap[cycle[0]]
                }
            }
        } else if (key == "ㅣ" || key == "·" || key == "ㅡ") {
            // Cheonjiin Vowel Creation
            if (state.cho != null && state.jung != null && state.jong != null) {
                val j = JONGSEONG[state.jong!!]
                var origin = ""
                for ((k, v) in doubleJong) {
                    if (v == j) origin = k
                }
                var nextCho = ""
                if (origin.isNotEmpty()) {
                    state.jong = jongMap[origin[0].toString()]
                    nextCho = origin[1].toString()
                } else {
                    nextCho = j
                    state.jong = null
                }
                state.commitPreedit()
                state.cho = choseongMap[nextCho] ?: choseongMap["ㅇ"]
            }

            if (state.cho == null) state.cho = choseongMap["ㅇ"]

            if (state.jung == null) {
                if (key == "ㅣ") state.jung = jungMap["ㅣ"]
                else if (key == "ㅡ") state.jung = jungMap["ㅡ"]
                else if (key == "·") state.jung = 100 // 1 dot
            } else {
                val curr = if (state.jung == 100) "·" else if (state.jung == 101) "··" else JUNGSEONG[state.jung!!]
                var next = ""
                when (curr) {
                    "ㅣ" -> if (key == "·") next = "ㅏ"
                    "ㅏ" -> if (key == "·") next = "ㅑ" else if (key == "ㅣ") next = "ㅐ"
                    "ㅑ" -> if (key == "ㅣ") next = "ㅒ"
                    "ㅡ" -> if (key == "·") next = "ㅜ"
                    "ㅜ" -> if (key == "·") next = "ㅠ" else if (key == "ㅣ") next = "ㅟ"
                    "ㅟ" -> if (key == "·") next = "ㅝ"
                    "ㅠ" -> if (key == "ㅣ") next = "ㅝ"
                    "·" -> if (key == "ㅣ") next = "ㅓ" else if (key == "ㅡ") next = "ㅗ" else if (key == "·") next = "··"
                    "··" -> if (key == "ㅣ") next = "ㅕ" else if (key == "ㅡ") next = "ㅛ"
                    "ㅓ" -> if (key == "·") next = "ㅕ" else if (key == "ㅣ") next = "ㅔ"
                    "ㅕ" -> if (key == "ㅣ") next = "ㅖ"
                    "ㅗ" -> if (key == "·") next = "ㅛ" else if (key == "ㅣ") next = "ㅚ"
                    "ㅚ" -> if (key == "·") next = "ㅘ"
                }

                if (next == "·") {
                    state.jung = 100
                } else if (next == "··") {
                    state.jung = 101
                } else if (next.isNotEmpty()) {
                    state.jung = jungMap[next]
                } else {
                    state.commitPreedit()
                    handleCheonjiin(key, state, now)
                }
            }
        } else {
            if (key != " ") {
                handleQwerty(key, state)
            } else {
                state.commitPreedit()
            }
        }
    }

    private fun handleDanmoeum(key: String, state: EngineState, now: Long) {
        val isSameCycle = lastInputKey == key
        val isWithinTime = (now - lastInputTime) < 800

        if (isSameCycle && isWithinTime && danmoeumDouble.containsKey(key)) {
            val doubleChar = danmoeumDouble[key]!!
            handleQwerty(doubleChar, state)
        } else {
            handleQwerty(key, state)
        }
    }

    private fun handleNaratgul(key: String, state: EngineState, now: Long) {
        val isSameCycle = lastInputKey == key
        val isWithinTime = (now - lastInputTime) < 800

        when (key) {
            "ㅏ/ㅓ" -> {
                if (isSameCycle && isWithinTime && state.jung == jungMap["ㅏ"]) {
                    state.jung = jungMap["ㅓ"]
                } else if (isSameCycle && isWithinTime && state.jung == jungMap["ㅓ"]) {
                    state.jung = jungMap["ㅏ"]
                } else {
                    handleQwerty("ㅏ", state)
                }
            }
            "ㅗ/ㅜ" -> {
                if (isSameCycle && isWithinTime && state.jung == jungMap["ㅗ"]) {
                    state.jung = jungMap["ㅜ"]
                } else if (isSameCycle && isWithinTime && state.jung == jungMap["ㅜ"]) {
                    state.jung = jungMap["ㅗ"]
                } else {
                    handleQwerty("ㅗ", state)
                }
            }
            "ㅣ/ㅡ" -> {
                if (isSameCycle && isWithinTime && state.jung == jungMap["ㅣ"]) {
                    state.jung = jungMap["ㅡ"]
                } else if (isSameCycle && isWithinTime && state.jung == jungMap["ㅡ"]) {
                    state.jung = jungMap["ㅣ"]
                } else {
                    handleQwerty("ㅣ", state)
                }
            }
            "획추가", "획" -> {
                if (state.jong != null) {
                    val currentJong = JONGSEONG[state.jong!!]
                    var origin = ""
                    for ((k, v) in doubleJong) {
                        if (v == currentJong) origin = k
                    }
                    if (origin.isNotEmpty()) {
                        val p2 = origin[1].toString()
                        if (naratgulStroke.containsKey(p2)) {
                            val nextJong = origin[0].toString() + naratgulStroke[p2]!!
                            if (doubleJong.containsKey(nextJong)) {
                                state.jong = jongMap[doubleJong[nextJong]!!]
                            }
                        }
                    } else if (naratgulStroke.containsKey(currentJong)) {
                        state.jong = jongMap[naratgulStroke[currentJong]!!]
                    }
                } else if (state.jung != null) {
                    val currentJung = JUNGSEONG[state.jung!!]
                    if (naratgulStroke.containsKey(currentJung)) {
                        state.jung = jungMap[naratgulStroke[currentJung]!!]
                    }
                } else if (state.cho != null) {
                    val currentCho = CHOSEONG[state.cho!!]
                    if (naratgulStroke.containsKey(currentCho)) {
                        val nextCho = naratgulStroke[currentCho]!!
                        if (state.jung == null) {
                            if (!tryMergeBack(state, nextCho)) {
                                state.cho = choseongMap[nextCho]
                            }
                        } else {
                            state.cho = choseongMap[nextCho]
                        }
                    }
                }
            }
            "쌍자음", "쌍" -> {
                if (state.jong != null) {
                    val currentJong = JONGSEONG[state.jong!!]
                    var origin = ""
                    for ((k, v) in doubleJong) {
                        if (v == currentJong) origin = k
                    }
                    if (origin.isNotEmpty()) {
                        val p2 = origin[1].toString()
                        if (naratgulDouble.containsKey(p2)) {
                            val nextJong = origin[0].toString() + naratgulDouble[p2]!!
                            if (doubleJong.containsKey(nextJong)) {
                                state.jong = jongMap[doubleJong[nextJong]!!]
                            }
                        }
                    } else if (naratgulDouble.containsKey(currentJong)) {
                        state.jong = jongMap[naratgulDouble[currentJong]!!]
                    }
                } else if (state.cho != null && state.jung == null) {
                    val currentCho = CHOSEONG[state.cho!!]
                    if (naratgulDouble.containsKey(currentCho)) {
                        val nextCho = naratgulDouble[currentCho]!!
                        if (!tryMergeBack(state, nextCho)) {
                            state.cho = choseongMap[nextCho]
                        }
                    }
                }
            }
            else -> {
                if (key != " ") {
                    handleQwerty(key, state)
                } else {
                    state.commitPreedit()
                }
            }
        }
    }
}
