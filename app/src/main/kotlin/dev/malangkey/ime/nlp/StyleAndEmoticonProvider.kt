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

        // 1. Emoticon Candidates from Database (1000+ emoticons)
        val matchedEmoticons = EmoticonDatabase.search(text)
        candidates.addAll(matchedEmoticons.map {
            ReplaceWordSuggestionCandidate(text = it, originalWordLength = text.length, secondaryText = "이모티콘")
        })

        // 2. Insta-style font & aesthetic frame candidates (25+ styles)
        val stylePairs = InstaStyleProvider.generateStyles(text)
        candidates.addAll(stylePairs.map { (styledText, badge) ->
            ReplaceWordSuggestionCandidate(text = styledText, originalWordLength = text.length, secondaryText = badge)
        })

        return candidates
    }
}
