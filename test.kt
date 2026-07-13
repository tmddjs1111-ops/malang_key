import java.io.File

fun main() {
    val CHOSEONG = listOf("ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")
    val JUNGSEONG = listOf("ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ")
    val JONGSEONG = listOf("", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ")

    val choseongMap = CHOSEONG.mapIndexed { index, s -> s to index }.toMap()
    val jungMap = JUNGSEONG.mapIndexed { index, s -> s to index }.toMap()
    val jongMap = JONGSEONG.mapIndexed { index, s -> s to index }.filter { it.first.isNotEmpty() }.toMap()

    val doubleJung = mapOf("ㅗㅏ" to "ㅘ", "ㅗㅐ" to "ㅙ", "ㅗㅣ" to "ㅚ", "ㅜㅓ" to "ㅝ", "ㅜㅔ" to "ㅞ", "ㅜㅣ" to "ㅟ", "ㅡㅣ" to "ㅢ")
    val doubleJong = mapOf("ㄱㅅ" to "ㄳ", "ㄴㅈ" to "ㄵ", "ㄴㅎ" to "ㄶ", "ㄹㄱ" to "ㄺ", "ㄹㅁ" to "ㄻ", "ㄹㅂ" to "ㄼ", "ㄹㅅ" to "ㄽ", "ㄹㅌ" to "ㄾ", "ㄹㅍ" to "ㄿ", "ㄹㅎ" to "ㅀ", "ㅂㅅ" to "ㅄ")

    fun disassemble(c: Char): List<Int> {
        val base = c.code - 0xAC00
        val initial = base / (21 * 28)
        val medial = (base % (21 * 28)) / 28
        val fin = base % 28
        return listOf(initial, medial, fin)
    }

    var cho: Int? = null
    var jung: Int? = null
    var jong: Int? = null
    var deleteCount = 0

    val precedingText = "이"
    val lastChar = precedingText.lastOrNull()
    if (lastChar != null) {
        val code = lastChar.code
        if (code in 0xAC00..0xD7A3) {
            val parts = disassemble(lastChar)
            cho = parts[0]
            jung = parts[1]
            jong = if (parts[2] > 0) parts[2] else null
            deleteCount = 1
        }
    }
    
    var key = "ㄹ"
    if (choseongMap.containsKey(key)) {
        if (cho == null) {
            cho = choseongMap[key]
        } else if (jung == null) {
            cho = choseongMap[key]
        } else if (jong == null) {
            if (jongMap.containsKey(key)) {
                jong = jongMap[key]
            } else {
                cho = choseongMap[key]
            }
        } else {
            val combined = JONGSEONG[jong!!] + key
            if (doubleJong.containsKey(combined)) {
                jong = jongMap[doubleJong[combined]]
            } else {
                cho = choseongMap[key]
            }
        }
    }
    
    val sIndex = (cho!! * 21 + jung!!) * 28 + (jong ?: 0)
    val res = (0xAC00 + sIndex).toChar().toString()
    println("Result: " + res)
}
