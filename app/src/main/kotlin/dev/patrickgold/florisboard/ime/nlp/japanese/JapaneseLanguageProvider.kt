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

package dev.patrickgold.florisboard.ime.nlp.japanese

import android.content.Context
import android.database.sqlite.SQLiteException
import dev.patrickgold.florisboard.extensionManager
import dev.patrickgold.florisboard.ime.core.Subtype
import dev.patrickgold.florisboard.ime.editor.EditorContent
import dev.patrickgold.florisboard.ime.editor.EditorRange
import dev.patrickgold.florisboard.ime.nlp.BreakIteratorGroup
import dev.patrickgold.florisboard.ime.nlp.LanguagePackComponent
import dev.patrickgold.florisboard.ime.nlp.LanguagePackExtension
import dev.patrickgold.florisboard.ime.nlp.SpellingProvider
import dev.patrickgold.florisboard.ime.nlp.SpellingResult
import dev.patrickgold.florisboard.ime.nlp.SuggestionCandidate
import dev.patrickgold.florisboard.ime.nlp.SuggestionProvider
import dev.patrickgold.florisboard.ime.nlp.WordSuggestionCandidate
import dev.patrickgold.florisboard.lib.devtools.flogDebug
import dev.patrickgold.florisboard.lib.devtools.flogError
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JapaneseLanguageProvider(val context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.japanese"
    }

    private val extensionManager by context.extensionManager()
    private val subtypeManager by context.subtypeManager()
    
    private val allLanguagePacks: List<LanguagePackExtension>
        get() = extensionManager.languagePacks.value
        
    private var __connectedActiveLanguagePacks: Set<LanguagePackExtension> = setOf()
    private var languagePackItems: Map<String, LanguagePackComponent> = mapOf()
    
    private val activeLanguagePacks
        get() = buildSet {
            val locales = subtypeManager.subtypes.map { it.primaryLocale.localeTag() }.toSet()
            for (languagePack in allLanguagePacks) {
                if (languagePack.items.any { it.locale.localeTag() in locales }) {
                    add(languagePack)
                }
            }
        }
        
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override val providerId = ProviderId

    private fun refreshLanguagePacks() {
        scope.launch { create() }
    }

    override suspend fun create() {
        languagePackItems = buildMap {
            for (languagePack in allLanguagePacks) {
                for (languagePackItem in languagePack.items) {
                    put(languagePackItem.locale.localeTag(), languagePackItem)
                    languagePackItem.parent = languagePack
                }
            }
        }.toMap()

        val activeLanguagePacks = activeLanguagePacks
        for (activeLanguagePack in activeLanguagePacks) {
            if (!activeLanguagePack.isLoaded()) {
                activeLanguagePack.load(context)
            }
        }
        __connectedActiveLanguagePacks = activeLanguagePacks
    }

    override suspend fun preload(subtype: Subtype) = withContext(Dispatchers.IO) {
        // Preload any necessary resources
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

    override suspend fun suggest(
        subtype: Subtype,
        content: EditorContent,
        maxCandidateCount: Int,
        allowPossiblyOffensive: Boolean,
        isPrivateSession: Boolean,
    ): List<SuggestionCandidate> {
        if (__connectedActiveLanguagePacks != activeLanguagePacks) {
            refreshLanguagePacks()
        }
        if (content.composingText.isEmpty()) {
            return emptyList()
        }
        
        val (_, languagePackExtension) = getLanguagePack(subtype) ?: return emptyList()
        
        try {
            val database = languagePackExtension.japaneseDictSQLiteDatabase
            if (!database.isOpen) {
                flogError { "Japanese Dictionary database is not open." }
                return emptyList()
            }
            
            // Expected schema: CREATE TABLE dictionary (id INTEGER PRIMARY KEY, reading TEXT NOT NULL, word TEXT NOT NULL, frequency INTEGER DEFAULT 0);
            val queryText = content.composingText.toString()
            val cur = database.query(
                "dictionary", 
                arrayOf("reading", "word"), 
                "reading LIKE ? || '%'", 
                arrayOf(queryText), 
                "", 
                "", 
                "reading ASC, frequency DESC", 
                "$maxCandidateCount"
            )
            
            cur.moveToFirst()
            val rowCount = cur.count
            flogDebug { "Japanese DB Query was '$queryText', found $rowCount rows." }
            
            val suggestions = buildList {
                for (n in 0 until rowCount) {
                    val reading = cur.getString(0)
                    val word = cur.getString(1)
                    cur.moveToNext()
                    
                    // Add the word (Kanji) as candidate, reading as secondary
                    add(WordSuggestionCandidate(
                        text = word,
                        secondaryText = reading,
                        confidence = 0.5,
                        isEligibleForAutoCommit = false, // Never auto-commit Kanji without user intent usually
                        sourceProvider = this@JapaneseLanguageProvider,
                    ))
                }
            }
            cur.close()
            return suggestions
        } catch (e: Exception) {
            flogError { "SQLiteException in Japanese Language Provider: composing=${content.composingText}, error='${e}'" }
            return emptyList()
        }
    }

    override suspend fun notifySuggestionAccepted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Accepted: $candidate" }
    }

    override suspend fun notifySuggestionReverted(subtype: Subtype, candidate: SuggestionCandidate) {
        flogDebug { "Reverted: $candidate" }
    }

    override suspend fun removeSuggestion(subtype: Subtype, candidate: SuggestionCandidate): Boolean {
        return false
    }

    fun getLanguagePack(subtype: Subtype): Pair<LanguagePackComponent, LanguagePackExtension>? {
        val languagePackItem = languagePackItems[subtype.primaryLocale.localeTag()]
        val languagePackExtension = languagePackItem?.parent
        if (languagePackItem == null || languagePackExtension == null) {
            return null
        }
        return Pair(languagePackItem, languagePackExtension)
    }

    override suspend fun getListOfWords(subtype: Subtype): List<String> {
        return emptyList()
    }

    override suspend fun getFrequencyForWord(subtype: Subtype, word: String): Double {
        return 0.0
    }

    override suspend fun destroy() {
    }

    override suspend fun determineLocalComposing(
        subtype: Subtype,
        textBeforeSelection: CharSequence,
        breakIterators: BreakIteratorGroup,
        localLastCommitPosition: Int
    ): EditorRange {
        // Simple fallback: composing text is exactly what FlorisBoard parsed normally.
        // Actually, for Japanese, we just return Unspecified and let FlorisBoard's default logic handle it,
        // or we could implement backward scanning for Hiragana. Let's rely on standard composer for now.
        return EditorRange.Unspecified
    }

    override val forcesSuggestionOn
        get() = true
}
