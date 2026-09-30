package dev.malangkey.app.game

import kotlin.math.max
import kotlin.math.roundToInt

/** 애국가 가사 (저작권 없음). 1절 도전은 1절과 후렴, 전체 도전은 1~4절과 후렴. */
object AnthemLyrics {
    private val chorus = listOf(
        "무궁화 삼천리 화려 강산",
        "대한 사람 대한으로 길이 보전하세",
    )
    private val verses = listOf(
        listOf("동해 물과 백두산이 마르고 닳도록", "하느님이 보우하사 우리나라 만세"),
        listOf("남산 위에 저 소나무 철갑을 두른 듯", "바람 서리 불변함은 우리 기상일세"),
        listOf("가을 하늘 공활한데 높고 구름 없이", "밝은 달은 우리 가슴 일편단심일세"),
        listOf("이 기상과 이 맘으로 충성을 다하여", "괴로우나 즐거우나 나라 사랑하세"),
    )

    fun lines(full: Boolean): List<String> =
        (if (full) verses else verses.take(1)).flatMap { it + chorus }
}

/**
 * 타자 게임 점수 계산. 속도는 한 분에 친 타수(자모 기준), 정확도는 목표 글자 대비 오타 비율이다.
 * 점수 = 타수 × 정확도² — 오타가 속도보다 점수를 더 크게 깎는다.
 */
object TypingScore {
    /** 한 글자를 치는 데 드는 대략의 타수 (두벌식 기준: 초성·중성·종성, 겹모음·겹받침은 2타). */
    fun strokes(c: Char): Int {
        val index = c.code - 0xAC00
        if (index !in 0 until 11172) return 1
        val jung = (index % 588) / 28
        val jong = index % 28
        val jungStrokes = if (jung in CompoundVowels) 2 else 1
        val jongStrokes = when {
            jong == 0 -> 0
            jong in CompoundFinals -> 2
            else -> 1
        }
        return 1 + jungStrokes + jongStrokes
    }

    fun strokes(text: String): Int = text.sumOf { strokes(it) }

    /** 두 글자열이 몇 글자 다른지 (빠진 글자·더 친 글자·틀린 글자를 모두 한 번씩 센다). */
    fun errors(typed: String, target: String): Int {
        var prev = IntArray(target.length + 1) { it }
        for (i in 1..typed.length) {
            val cur = IntArray(target.length + 1)
            cur[0] = i
            for (j in 1..target.length) {
                val cost = if (typed[i - 1] == target[j - 1]) 0 else 1
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
            }
            prev = cur
        }
        return prev[target.length]
    }

    data class Result(
        val strokesPerMinute: Int,
        val accuracy: Float,
        val errorCount: Int,
        val elapsedMillis: Long,
        val score: Int,
    ) {
        val grade: String get() = gradeOf(score)
        val gradeTitle: String get() = gradeTitleOf(score)
    }

    fun result(typedLines: List<String>, targetLines: List<String>, elapsedMillis: Long): Result {
        val totalChars = targetLines.sumOf { it.length }.coerceAtLeast(1)
        val errors = typedLines.zip(targetLines).sumOf { (typed, target) -> errors(typed, target) }
        val accuracy = max(0f, 1f - errors.toFloat() / totalChars)
        val minutes = (elapsedMillis / 60_000f).coerceAtLeast(1f / 60f)
        val speed = (typedLines.sumOf { strokes(it) } / minutes).roundToInt()
        val score = (speed * accuracy * accuracy).roundToInt()
        return Result(speed, accuracy, errors, elapsedMillis, score)
    }

    fun gradeOf(score: Int): String = when {
        score >= 450 -> "S"
        score >= 350 -> "A"
        score >= 250 -> "B"
        score >= 150 -> "C"
        else -> "D"
    }

    fun gradeTitleOf(score: Int): String = when {
        score >= 450 -> "타자의 신"
        score >= 350 -> "손가락 달인"
        score >= 250 -> "날쌘 타자수"
        score >= 150 -> "성실한 타자수"
        else -> "연습이 필요해요"
    }

    private val CompoundVowels = setOf(9, 10, 11, 14, 15, 16, 19) // ㅘ ㅙ ㅚ ㅝ ㅞ ㅟ ㅢ
    private val CompoundFinals = setOf(3, 5, 6, 9, 10, 11, 12, 13, 14, 15, 18) // ㄳ ㄵ ㄶ ㄺ ㄻ ㄼ ㄽ ㄾ ㄿ ㅀ ㅄ
}
