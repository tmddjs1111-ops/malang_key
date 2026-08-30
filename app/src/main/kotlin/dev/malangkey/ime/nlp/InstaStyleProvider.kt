package dev.malangkey.ime.nlp

object InstaStyleProvider {

    private val BOLD_SERIF_UP = listOf("𝐀", "𝐁", "𝐂", "𝐃", "𝐄", "𝐅", "𝐆", "𝐇", "𝐈", "𝐉", "𝐊", "𝐋", "𝐌", "𝐍", "𝐎", "𝐏", "𝐐", "𝐑", "𝐒", "𝐓", "𝐔", "𝐕", "𝐖", "𝐗", "𝐘", "𝐙")
    private val BOLD_SERIF_LOW = listOf("𝐚", "𝐛", "𝐜", "𝐝", "𝐞", "𝐟", "𝐠", "𝐡", "𝐢", "𝐣", "𝐤", "𝐥", "𝐦", "𝐧", "𝐨", "𝐩", "𝐪", "𝐫", "𝐬", "𝐭", "𝐮", "𝐯", "𝐰", "𝐱", "𝐲", "𝐳")

    private val BOLD_SANS_UP = listOf("𝗔", "𝗕", "𝗖", "𝗗", "𝗘", "𝗙", "𝗚", "𝗛", "𝗜", "𝗝", "𝗞", "𝗟", "𝗠", "𝗡", "𝗢", "𝗣", "𝗤", "𝗥", "𝗦", "𝗧", "𝗨", "𝗩", "𝗪", "𝗫", "𝗬", "𝗭")
    private val BOLD_SANS_LOW = listOf("𝗮", "𝗯", "𝗰", "𝗱", "𝗲", "𝗳", "𝗴", "𝗵", "𝗶", "𝗷", "𝗸", "𝗹", "𝗺", "𝗻", "𝗼", "𝗽", "𝗾", "𝗿", "𝘀", "𝘁", "𝘂", "𝘃", "𝘄", "𝘅", "𝘆", "𝘇")

    private val SCRIPT_UP = listOf("𝒜", "ℬ", "𝒞", "𝒟", "ℰ", "ℱ", "𝒢", "ℋ", "ℐ", "𝒥", "𝒦", "ℒ", "ℳ", "𝒩", "𝒪", "𝒫", "𝒬", "ℛ", "𝒮", "𝒯", "𝒰", "𝒱", "𝒲", "𝒳", "𝒴", "𝒵")
    private val SCRIPT_LOW = listOf("𝒶", "𝒷", "𝒸", "𝒹", "ℯ", "𝒻", "ℊ", "𝒽", "𝒾", "𝒿", "𝓀", "𝓁", "𝓂", "𝓃", "ℴ", "𝓅", "𝓆", "𝓇", "𝓈", "𝓉", "𝓊", "𝓋", "𝓌", "𝓍", "𝓎", "𝓏")

    private val BOLD_SCRIPT_UP = listOf("𝓐", "𝓑", "𝓒", "𝓓", "𝓔", "𝓕", "𝓖", "𝓗", "𝓘", "𝓙", "𝓚", "𝓛", "𝓜", "𝓝", "𝓞", "𝓟", "𝓠", "𝓡", "𝓢", "𝓣", "𝓤", "𝓥", "𝓦", "𝓧", "𝓨", "𝓩")
    private val BOLD_SCRIPT_LOW = listOf("𝓪", "𝓫", "𝓬", "𝓭", "𝓮", "𝓯", "𝓰", "𝓱", "𝓲", "𝓳", "𝓴", "𝓵", "𝓶", "𝓷", "𝓸", "𝓹", "𝓺", "𝓻", "𝓼", "𝓽", "𝓾", "𝓿", "𝓦", "𝔁", "𝔂", "𝔃")

    private val FRAKTUR_UP = listOf("𝔄", "𝔅", "ℭ", "𝔇", "𝔈", "𝔉", "𝔊", "ℌ", "ℑ", "𝔍", "𝔎", "𝔏", "𝔐", "𝔑", "𝔒", "𝔓", "𝔔", "ℜ", "𝔖", "𝔗", "𝔘", "𝔙", "𝔚", "𝔛", "Ყ", "ℨ")
    private val FRAKTUR_LOW = listOf("𝔞", "𝔟", "𝔠", "𝔡", "𝔢", "𝔣", "𝔤", "𝔥", "𝔦", "𝔧", "𝔨", "𝔩", "𝔪", "𝔫", "𝔬", "𝔭", "𝔮", "𝔯", "𝔰", "𝔱", "𝔲", "𝔳", "𝔴", "𝔵", "𝔶", "𝔷")

    private val DOUBLE_STRUCK_UP = listOf("𝔸", "𝔹", "ℂ", "𝔻", "𝔼", "𝔽", "𝔾", "ℍ", "𝕀", "𝕁", "𝕂", "𝕃", "𝕄", "ℕ", "𝕆", "ℙ", "ℚ", "ℝ", "𝕊", "𝕋", "𝕌", "𝕍", "𝕎", "𝕏", "𝕐", "ℤ")
    private val DOUBLE_STRUCK_LOW = listOf("𝕒", "𝕓", "𝕔", "𝕕", "𝕖", "𝕗", "𝕘", "𝕙", "𝕚", "𝕛", "𝕜", "𝕝", "𝕞", "𝕟", "𝕠", "𝕡", "𝕢", "𝕣", "𝕤", "𝕥", "𝕦", "𝕧", "𝕨", "𝕩", "𝕪", "𝕫")

    private val MONOSPACE_UP = listOf("𝙰", "𝙱", "𝙲", "𝙳", "𝙴", "𝙵", "𝙶", "𝙷", "𝙸", "𝙹", "𝙺", "𝙻", "𝙼", "𝙽", "𝙾", "𝙿", "𝚀", "𝚁", "𝚂", "𝚃", "𝚄", "𝚅", "𝚆", "𝚇", "𝚈", "𝚉")
    private val MONOSPACE_LOW = listOf("𝚊", "𝚋", "𝚌", "𝚍", "𝚎", "𝚏", "𝚐", "𝚑", "𝚒", "𝚓", "𝚔", "𝚕", "𝚖", "𝚗", "𝚘", "𝚙", "𝚚", "𝚛", "𝚜", "𝚝", "𝚞", "𝚟", "𝚠", "𝚡", "𝚢", "𝚣")

    private val BUBBLE_UP = listOf("Ⓐ", "Ⓑ", "Ⓒ", "Ⓓ", "Ⓔ", "Ⓕ", "Ⓖ", "Ⓗ", "Ⓘ", "Ⓙ", "Ⓚ", "Ⓛ", "Ⓜ", "Ⓝ", "Ⓩ", "Ⓟ", "Ⓠ", "Ⓡ", "Ⓢ", "Ⓣ", "Ⓤ", "Ⓥ", "Ⓦ", "Ⓧ", "Ⓨ", "Ⓩ")
    private val BUBBLE_LOW = listOf("ⓐ", "ⓑ", "ⓒ", "ⓓ", "ⓔ", "ⓕ", "ⓖ", "ⓗ", "ⓘ", "ⓙ", "ⓚ", "ⓛ", "ⓜ", "ⓝ", "ⓞ", "ⓟ", "ⓠ", "ⓡ", "ⓢ", "ⓣ", "ⓤ", "ⓥ", "ⓦ", "ⓧ", "ⓨ", "ⓩ")

    private val SMALL_CAPS_LOW = listOf("ᴀ", "ʙ", "ᴄ", "ᴅ", "ᴇ", "ғ", "ɢ", "ʜ", "ɪ", "ᴊ", "ᴋ", "ʟ", "ᴍ", "ɴ", "ᴏ", "ᴘ", "ǫ", "ʀ", "s", "ᴛ", "ᴜ", "ᴠ", "ᴡ", "x", "ʏ", "ᴢ")

    private fun convertLatin(text: String, upList: List<String>, lowList: List<String>): String {
        val sb = StringBuilder()
        for (ch in text) {
            when (ch) {
                in 'A'..'Z' -> sb.append(upList[ch - 'A'])
                in 'a'..'z' -> sb.append(lowList[ch - 'a'])
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun generateStyles(text: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        if (text.isBlank()) return results

        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }

        // 1. Decorative / Aesthetic Frames (Works beautifully for Korean and English)
        results.add(Pair("✿ $text ✿", "인스타: 꽃송이"))
        results.add(Pair("✨ $text ✨", "인스타: 반짝이"))
        results.add(Pair("★·.·´¯`·.·★ $text ★·.·´¯`·.·★", "인스타: 스타"))
        results.add(Pair("( ˘ ³˘)♥ $text ♥", "인스타: 러블리"))
        results.add(Pair("⋆｡˚ ☁︎ $text ☁︎ ˚｡⋆", "인스타: 몽환구름"))
        results.add(Pair("꒰ $text ꒱", "인스타: 귀요미"))
        results.add(Pair("[ $text ]", "인스타: 힙레트로"))
        results.add(Pair("ฅ^•ﻌ•^ฅ $text ฅ^•ﻌ•^ฅ", "인스타: 냥이"))
        results.add(Pair("『 $text 』", "인스타: 인용문"))
        results.add(Pair("« $text »", "인스타: 강조"))
        results.add(Pair("ʕ•ᴥ•ʔ $text ʕ•ᴥ•ʔ", "인스타: 곰돌이"))
        results.add(Pair(".°ʚ $text ɞ°.", "인스타: 천사"))
        results.add(Pair("🎀 $text 🎀", "인스타: 리본"))
        results.add(Pair("🫧 $text 🫧", "인스타: 물방울"))
        results.add(Pair("💓 $text 💓", "인스타: 하트비트"))

        // 2. Mathematical Alphanumeric Font Styles (If text contains Latin characters)
        if (hasLatin) {
            results.add(Pair(convertLatin(text, BOLD_SERIF_UP, BOLD_SERIF_LOW), "인스타: 굵은명조"))
            results.add(Pair(convertLatin(text, BOLD_SANS_UP, BOLD_SANS_LOW), "인스타: 굵은고딕"))
            results.add(Pair(convertLatin(text, SCRIPT_UP, SCRIPT_LOW), "인스타: 필기체"))
            results.add(Pair(convertLatin(text, BOLD_SCRIPT_UP, BOLD_SCRIPT_LOW), "인스타: 굵은필기체"))
            results.add(Pair(convertLatin(text, DOUBLE_STRUCK_UP, DOUBLE_STRUCK_LOW), "인스타: 이중선체"))
            results.add(Pair(convertLatin(text, FRAKTUR_UP, FRAKTUR_LOW), "인스타: 중세고딕"))
            results.add(Pair(convertLatin(text, MONOSPACE_UP, MONOSPACE_LOW), "인스타: 타자기"))
            results.add(Pair(convertLatin(text, BUBBLE_UP, BUBBLE_LOW), "인스타: 동그라미"))
            
            val smallCaps = text.map { ch ->
                if (ch in 'a'..'z') SMALL_CAPS_LOW[ch - 'a'] else ch.toString()
            }.joinToString("")
            results.add(Pair(smallCaps, "인스타: 소문자대문자화"))
        }

        return results
    }
}
