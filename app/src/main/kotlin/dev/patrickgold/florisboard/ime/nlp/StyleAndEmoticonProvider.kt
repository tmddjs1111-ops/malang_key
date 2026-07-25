package dev.patrickgold.florisboard.ime.nlp

object StyleAndEmoticonProvider {
    
    // Simple basic emoticon mapping
    private val emoticonMap = mapOf(
        "안녕" to listOf("(❁´◡`❁)", "(●'◡'●)", "(✿◡‿◡)"),
        "ㅠㅠ" to listOf("(T_T)", "(╥_╥)", "(ಥ_ಥ)", "༼ つ ◕_◕ ༽つ"),
        "사랑" to listOf("❤️", "(´▽`ʃ♡ƪ)", "(* ￣3)(ε￣ *)"),
        "축하" to listOf("🎉", "🎊", "╰(*°▽°*)╯"),
        "화이팅" to listOf("(ง •_•)ง", "💪", "🔥"),
        "고마워" to listOf("🙏", "(´▽`ʃ♡ƪ)", "🥰"),
        "미안" to listOf("😥", "(；′⌒`)", "🙇‍♂️"),
        "최고" to listOf("👍", "😎", "✨"),
        "오케이" to listOf("👌", "🙆‍♂️", "✅")
    )

    fun generateCandidates(text: String): List<SuggestionCandidate> {
        val candidates = mutableListOf<SuggestionCandidate>()
        
        if (text.isBlank()) return candidates

        // 1. Emoticon Candidates
        val matchedEmoticons = emoticonMap.entries.firstOrNull { text.contains(it.key) }?.value
        if (matchedEmoticons != null) {
            candidates.addAll(matchedEmoticons.map {
                ReplaceWordSuggestionCandidate(text = it, originalWordLength = text.length, secondaryText = "이모티콘")
            })
        } else {
            // Default generic emoticons if no match
            val defaults = listOf("(❁´◡`❁)", "(T_T)", "❤️", "✨")
            candidates.addAll(defaults.map {
                ReplaceWordSuggestionCandidate(text = it, originalWordLength = text.length, secondaryText = "이모티콘")
            })
        }

        // 2. Insta-style font candidates (Mathematical Alphanumeric Symbols)
        // Convert Latin characters to bold serif / script / double-struck
        val doubleStruck = text.map { ch ->
            when (ch) {
                in 'A'..'Z' -> (ch - 'A' + 0x1D538).toChar().toString() // Note: C, H, N, P, Q, R, Z are exceptions in real unicode but we simplify for now or use full offset logic
                in 'a'..'z' -> (ch - 'a' + 0x1D552).toChar().toString()
                else -> ch.toString()
            }
        }.joinToString("")
        
        val script = text.map { ch ->
            when (ch) {
                in 'A'..'Z' -> (ch - 'A' + 0x1D49C).toChar().toString()
                in 'a'..'z' -> (ch - 'a' + 0x1D4B6).toChar().toString()
                else -> ch.toString()
            }
        }.joinToString("")

        if (text.any { it in 'A'..'Z' || it in 'a'..'z' }) {
            candidates.add(ReplaceWordSuggestionCandidate(text = doubleStruck, originalWordLength = text.length, secondaryText = "인스타체"))
            candidates.add(ReplaceWordSuggestionCandidate(text = script, originalWordLength = text.length, secondaryText = "인스타체"))
        } else {
            // For Korean, just append cute symbols
            candidates.add(ReplaceWordSuggestionCandidate(text = "✿$text✿", originalWordLength = text.length, secondaryText = "인스타체"))
            candidates.add(ReplaceWordSuggestionCandidate(text = "✧$text✧", originalWordLength = text.length, secondaryText = "인스타체"))
        }

        return candidates
    }
}
