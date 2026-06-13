import dev.patrickgold.florisboard.ime.text.composing.HangulUnicode

fun main() {
    val h = HangulUnicode
    val testWords = listOf("몫", "많다", "핥다", "닳다", "값")
    
    // Qwerty tests
    val qwertyInputs = mapOf(
        "몫" to listOf("ㅁ", "ㅗ", "ㄱ", "ㅅ"),
        "많다" to listOf("ㅁ", "ㅏ", "ㄴ", "ㅎ", "ㄷ", "ㅏ"),
        "핥다" to listOf("ㅎ", "ㅏ", "ㄹ", "ㅌ", "ㄷ", "ㅏ"),
        "닳다" to listOf("ㄷ", "ㅏ", "ㄹ", "ㅎ", "ㄷ", "ㅏ"),
        "값" to listOf("ㄱ", "ㅏ", "ㅂ", "ㅅ")
    )

    for ((word, inputs) in qwertyInputs) {
        var text = ""
        for (i in inputs) {
            val (del, add) = h.getActions(text, i, "korean_qwerty")
            text = text.dropLast(del) + add
        }
        println("Qwerty $word: expected=$word, actual=$text, match=${word == text}")
    }

    // Cheonjiin tests
    val cheonjiinInputs = mapOf(
        "몫" to listOf("ㅁ", "ㅗ", "ㄱ", "ㅅ"), // ㅅ key
        "많다" to listOf("ㅁ", "ㅏ", "ㄴ", "ㅅ", "ㅅ", "ㄷ", "ㅏ"), // ㅎ is second cycle of ㅅ
        "값" to listOf("ㄱ", "ㅏ", "ㅂ", "ㅅ") // ㅅ key
    )
    for ((word, inputs) in cheonjiinInputs) {
        var text = ""
        var time = 0L
        for (i in inputs) {
            val (del, add) = h.getActions(text, i, "korean_cheonjiin", now = time)
            text = text.dropLast(del) + add
            time += 100 // Simulate 100ms between keypresses
        }
        println("Cheonjiin $word: actual=$text")
    }
}
