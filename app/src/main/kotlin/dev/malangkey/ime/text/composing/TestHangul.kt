import dev.malangkey.ime.text.composing.HangulUnicode

fun main() {
    var precedingText = "넉"
    val result = HangulUnicode.getActions(precedingText, "ㅅ", "korean")
    println(result)
}
