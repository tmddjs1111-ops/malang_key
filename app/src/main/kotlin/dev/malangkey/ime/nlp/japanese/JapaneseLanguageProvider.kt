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

package dev.malangkey.ime.nlp.japanese

import android.content.Context
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.editor.EditorContent
import dev.malangkey.ime.editor.EditorRange
import dev.malangkey.ime.nlp.BreakIteratorGroup
import dev.malangkey.ime.nlp.SpellingProvider
import dev.malangkey.ime.nlp.SpellingResult
import dev.malangkey.ime.nlp.SuggestionCandidate
import dev.malangkey.ime.nlp.SuggestionProvider
import dev.malangkey.ime.nlp.WordSuggestionCandidate
import dev.malangkey.lib.devtools.flogDebug
import dev.malangkey.lib.devtools.flogError
import java.io.File
import java.text.Normalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private fun Char.isJapaneseComposingCharacter(): Boolean {
    return this in '぀'..'ゟ' ||
        this in '゠'..'ヿ' ||
        this in 'ｦ'..'ﾟ'
}

internal fun determineJapaneseComposingRange(
    textBeforeSelection: CharSequence,
    localLastCommitPosition: Int,
): EditorRange {
    val end = textBeforeSelection.length
    val lowerBound = localLastCommitPosition.coerceIn(0, end)
    var start = end
    while (start > lowerBound && textBeforeSelection[start - 1].isJapaneseComposingCharacter()) {
        start--
    }
    return if (start < end) EditorRange(start, end) else EditorRange.Unspecified
}

/**
 * Kana-to-kanji conversion backed by Mozc ([MozcSession]).
 *
 * On top of Mozc's own learning, a small per-reading history keeps the user's recent picks first.
 * If Mozc cannot be loaded, a tiny built-in dictionary keeps basic conversion working.
 */
class JapaneseLanguageProvider(val context: Context) : SpellingProvider, SuggestionProvider {
    private val historyFile by lazy { File(context.filesDir, "japanese_history.json") }

    class JapaneseHistoryEntry(
        val word: String,
        var count: Int,
        var lastUsed: Long
    )

    private val userHistory = mutableMapOf<String, MutableList<JapaneseHistoryEntry>>()

    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.japanese"

        /** Copied out of the APK by versions before Mozc; deleted on startup to free ~26 MB. */
        private const val LegacyDatabaseFileName = "japanese_dict_v1.sqlite3"

        private val builtInDict = mapOf(
            "にほん" to listOf("日本"),
            "かたな" to listOf("刀"),
            "ありがとう" to listOf("有難う", "ありがとう"),
            "たべる" to listOf("食べる", "たべる"),
            "さくら" to listOf("桜", "さくら"),
            "やま" to listOf("山"),
            "かわ" to listOf("川"),
            "そら" to listOf("空"),
            "うみ" to listOf("海"),
            "ひと" to listOf("人"),
            "きょう" to listOf("今日"),
            "あした" to listOf("明日"),
            "きのう" to listOf("昨日"),
            "ねこ" to listOf("猫"),
            "いぬ" to listOf("犬"),
            "わたし" to listOf("私"),
            "あなた" to listOf("貴方", "あなた"),
            "ともだち" to listOf("友達"),
            "せんせい" to listOf("先生"),
            "がっこう" to listOf("学校"),
            "すき" to listOf("好き"),
            "たのしい" to listOf("楽しい"),
            "うれしい" to listOf("嬉しい"),
            "たべもの" to listOf("食べ物"),
            "のみもの" to listOf("飲み物"),
            "ほん" to listOf("本"),
            "くるま" to listOf("車"),
            "でんしゃ" to listOf("電車"),
            "あさ" to listOf("朝"),
            "ひる" to listOf("昼"),
            "よる" to listOf("夜"),
            "あめ" to listOf("雨"),
            "ゆき" to listOf("雪"),
            "はな" to listOf("花", "鼻"),
            "き" to listOf("木", "気"),
            "もり" to listOf("森"),
            "つき" to listOf("月"),
            "ひ" to listOf("日", "火"),
            "ほし" to listOf("星"),
            "みず" to listOf("水"),
            "てんき" to listOf("天気"),
            "かみ" to listOf("神", "紙", "髪"),
            "こころ" to listOf("心"),
            "め" to listOf("目"),
            "くち" to listOf("口"),
            "みみ" to listOf("耳"),
            "て" to listOf("手"),
            "あし" to listOf("足"),
            "かお" to listOf("顔"),
            "あい" to listOf("愛"),
            "あお" to listOf("青"),
            "あか" to listOf("赤"),
            "しろ" to listOf("白"),
            "くろ" to listOf("黒"),
            "くに" to listOf("国"),
            "せかい" to listOf("世界"),
            "じかん" to listOf("時間"),
            "しごと" to listOf("仕事"),
            "かぞく" to listOf("家族"),
            "おとこ" to listOf("男"),
            "おんな" to listOf("女"),
            "こども" to listOf("子供")
        )
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mozc = MozcSession(context)

    /** Whether the most recent suggest() call came from an incognito / private field. */
    @Volatile
    private var isPrivateSession = false

    override val providerId = ProviderId

    private fun loadHistory() {
        try {
            if (historyFile.exists()) {
                val json = org.json.JSONObject(historyFile.readText())
                json.keys().forEach { reading ->
                    val array = json.getJSONArray(reading)
                    val entries = mutableListOf<JapaneseHistoryEntry>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        entries.add(JapaneseHistoryEntry(
                            word = obj.getString("word"),
                            count = obj.getInt("count"),
                            lastUsed = obj.getLong("lastUsed")
                        ))
                    }
                    userHistory[reading] = entries
                }
            }
        } catch (e: Exception) {
            flogError { "Failed to load Japanese history: $e" }
        }
    }

    private fun saveHistory() {
        try {
            val root = org.json.JSONObject()
            for ((reading, entries) in userHistory) {
                val array = org.json.JSONArray()
                for (entry in entries) {
                    val obj = org.json.JSONObject()
                    obj.put("word", entry.word)
                    obj.put("count", entry.count)
                    obj.put("lastUsed", entry.lastUsed)
                    array.put(obj)
                }
                root.put(reading, array)
            }
            historyFile.writeText(root.toString())
        } catch (e: Exception) {
            flogError { "Failed to save Japanese history: $e" }
        }
    }

    override suspend fun create() {
        loadHistory()
        scope.launch {
            File(context.filesDir, LegacyDatabaseFileName).delete()
            // First launch copies mozc.data out of the APK, so don't block create() on it.
            mozc.open()
        }
    }

    override suspend fun preload(subtype: Subtype) {
        // Mozc is opened in create(); nothing per-subtype to load.
    }

    override suspend fun spell(
        subtype: Subtype,
        word: String,
        precedingWords: List<String>,
        followingWords: List<String>,
        maxSuggestionCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): SpellingResult {
        return SpellingResult.validWord()
    }

    private fun normalizeReading(text: String): String {
        val normalizedText = Normalizer.normalize(text, Normalizer.Form.NFKC)
        return buildString(normalizedText.length) {
            for (character in normalizedText) {
                if (character in 'ァ'..'ヶ') {
                    append((character.code - 0x60).toChar())
                } else {
                    append(character)
                }
            }
        }
    }

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        this.isPrivateSession = isPrivateSession
        if (content.composingText.isEmpty() || maxCandidateCount <= 0) {
            return emptyList()
        }

        val composingText = content.composingText
        val queryText = normalizeReading(composingText)
        val suggestions = mutableListOf<SuggestionCandidate>()

        fun add(word: String, confidence: Double) {
            if (suggestions.size >= maxCandidateCount) return
            // The raw reading is appended last by withRawReadingCandidate(), so the first
            // candidate (used by the convert key) is always an actual conversion.
            if (word == queryText || word == composingText) return
            if (suggestions.any { (it as? WordSuggestionCandidate)?.text == word }) return
            suggestions.add(WordSuggestionCandidate(
                text = word,
                secondaryText = queryText,
                confidence = confidence,
                isEligibleForAutoCommit = false,
                sourceProvider = this@JapaneseLanguageProvider,
            ))
        }

        if (!isPrivateSession) {
            userHistory[queryText]?.forEach { add(it.word, confidence = 0.98) }
        }

        val mozcCandidates = mozc.convert(queryText, maxCandidateCount)
        mozcCandidates.forEach { add(it.value, confidence = 0.9) }

        if (mozcCandidates.isEmpty()) {
            // Mozc unavailable: exact matches first, then readings that start with the input.
            builtInDict[queryText]?.forEach { add(it, confidence = 0.8) }
            for ((reading, words) in builtInDict) {
                if (reading != queryText && reading.startsWith(queryText)) {
                    words.forEach { add(it, confidence = 0.6) }
                }
            }
        }

        flogDebug { "Japanese '$queryText' -> ${suggestions.size} candidates (mozc=${mozcCandidates.size})" }
        return withRawReadingCandidate(suggestions, composingText, maxCandidateCount)
    }

    /** Always offers the unconverted reading as the last candidate. */
    private fun withRawReadingCandidate(
        suggestions: List<SuggestionCandidate>,
        composingText: CharSequence,
        maxCandidateCount: Int,
    ): List<SuggestionCandidate> {
        val rawReadingCandidate = WordSuggestionCandidate(
            text = composingText,
            confidence = 0.5,
            isEligibleForAutoCommit = false,
            isEligibleForUserRemoval = false,
            sourceProvider = this@JapaneseLanguageProvider,
        )
        return if (maxCandidateCount == 1) {
            listOf(rawReadingCandidate)
        } else {
            suggestions.take(maxCandidateCount - 1) + rawReadingCandidate
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Accepted: $candidate" }
        // Nothing typed in a private field is remembered, neither here nor in Mozc.
        if (isPrivateSession) return
        if (candidate is WordSuggestionCandidate && candidate.secondaryText != null) {
            val reading = candidate.secondaryText!!.toString()
            val word = candidate.text.toString()

            val entries = userHistory.getOrPut(reading) { mutableListOf() }
            val entry = entries.find { it.word == word }
            if (entry != null) {
                entry.count++
                entry.lastUsed = System.currentTimeMillis()
            } else {
                entries.add(JapaneseHistoryEntry(word, 1, System.currentTimeMillis()))
            }
            entries.sortByDescending { (it.count.toLong() * 10000L) + it.lastUsed }
            if (entries.size > 10) {
                entries.removeAt(entries.lastIndex)
            }
            saveHistory()
            scope.launch { mozc.learn(reading, word) }
        }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Reverted: $candidate" }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        return false
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
    }

    override suspend fun destroy() {
        mozc.close()
        scope.cancel()
    }

    override suspend fun determineLocalComposing(
        subtype: Subtype,
        textBeforeSelection: CharSequence,
        breakIterators: BreakIteratorGroup,
        localLastCommitPosition: Int
    ): EditorRange {
        return determineJapaneseComposingRange(textBeforeSelection, localLastCommitPosition)
    }

    override val forcesSuggestionOn
        get() = true
}
