package dev.malangkey.ime.nlp

object StyleAndEmoticonProvider {
    fun generateCandidates(text: String): List<SuggestionCandidate> {
        val candidates = mutableListOf<SuggestionCandidate>()
        
        if (text.isBlank()) {
            val defaults = EmoticonDatabase.allEmoticons.take(50)
            candidates.addAll(defaults.map {
                ReplaceWordSuggestionCandidate(text = it, originalWordLength = 0, secondaryText = "이모티콘")
            })
            return candidates
        }

        val emoticons = EmoticonDatabase.search(text).map {
            ReplaceWordSuggestionCandidate(text = it, originalWordLength = text.length, secondaryText = "이모티콘")
        }
        val styles = InstaStyleProvider.generateStyles(text).map { (styledText, badge) ->
            ReplaceWordSuggestionCandidate(text = styledText, originalWordLength = text.length, secondaryText = badge)
        }
        // 영문을 쳤으면 꾸밈 글씨를 찾는 경우가 많아 먼저 보여 준다. 한글은 뜻에 맞는 이모티콘이 먼저다.
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        candidates.addAll(if (hasLatin) styles + emoticons else emoticons + styles)

        return candidates
    }
}
