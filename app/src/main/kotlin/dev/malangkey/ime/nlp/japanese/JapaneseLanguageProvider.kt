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
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import dev.malangkey.extensionManager
import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.editor.EditorContent
import dev.malangkey.ime.editor.EditorRange
import dev.malangkey.ime.nlp.BreakIteratorGroup
import dev.malangkey.ime.nlp.LanguagePackComponent
import dev.malangkey.ime.nlp.LanguagePackExtension
import dev.malangkey.ime.nlp.SpellingProvider
import dev.malangkey.ime.nlp.SpellingResult
import dev.malangkey.ime.nlp.SuggestionCandidate
import dev.malangkey.ime.nlp.SuggestionProvider
import dev.malangkey.ime.nlp.WordSuggestionCandidate
import dev.malangkey.lib.devtools.flogDebug
import dev.malangkey.lib.devtools.flogError
import dev.malangkey.subtypeManager
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JapaneseLanguageProvider(val context: Context) : SpellingProvider, SuggestionProvider {
    companion object {
        const val ProviderId = "org.florisboard.nlp.providers.japanese"

        private const val AssetDatabasePath =
            "ime/languagepack/org.florisboard.japanesepack/japanese_dict.sqlite3"
        private const val InstalledDatabaseFileName = "japanese_dict_v1.sqlite3"

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

    @Transient
    private var assetDatabase: SQLiteDatabase? = null

    @Synchronized
    private fun getOrOpenAssetDatabase(): SQLiteDatabase? {
        if (assetDatabase?.isOpen == true) return assetDatabase

        val dbFile = File(context.filesDir, InstalledDatabaseFileName)
        repeat(2) { attempt ->
            try {
                if (!dbFile.exists()) {
                    installAssetDatabase(dbFile)
                }
                assetDatabase = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                return assetDatabase
            } catch (e: SQLiteException) {
                if (attempt == 0) {
                    dbFile.delete()
                } else {
                    flogError { "Failed to open bundled Japanese DB: $e" }
                }
            } catch (e: Exception) {
                flogError { "Failed to install bundled Japanese DB: $e" }
                return null
            }
        }
        return null
    }

    private fun installAssetDatabase(destination: File) {
        val temporaryFile = File(destination.parentFile, "${destination.name}.tmp")
        temporaryFile.delete()
        context.assets.open(AssetDatabasePath).use { input ->
            FileOutputStream(temporaryFile).use { output ->
                input.copyTo(output)
                output.fd.sync()
            }
        }
        if (!temporaryFile.renameTo(destination)) {
            temporaryFile.copyTo(destination, overwrite = true)
            temporaryFile.delete()
        }
    }

    private fun normalizeReading(text: String): String = buildString(text.length) {
        for (character in text) {
            if (character in '\u30A1'..'\u30F6') {
                append((character.code - 0x60).toChar())
            } else {
                append(character)
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
        if (__connectedActiveLanguagePacks != activeLanguagePacks) {
            refreshLanguagePacks()
        }
        if (content.composingText.isEmpty()) {
            return emptyList()
        }
        
        if (maxCandidateCount <= 0) {
            return emptyList()
        }

        val composingText = content.composingText
        val queryText = normalizeReading(composingText)
        val suggestions = mutableListOf<SuggestionCandidate>()

        // Preserve a small set of hand-ranked everyday words where JMdict priority markers tie.
        builtInDict[queryText]?.forEach { word ->
            if (suggestions.size < maxCandidateCount) {
                suggestions.add(WordSuggestionCandidate(
                    text = word,
                    secondaryText = queryText,
                    confidence = 0.95,
                    isEligibleForAutoCommit = false,
                    sourceProvider = this@JapaneseLanguageProvider,
                ))
            }
        }

        // Prefer the versioned database bundled in this APK. The language-pack database remains
        // available as a compatibility fallback for installations created before this bundle.
        val languagePackExt = getLanguagePack(subtype)?.second
        val database = getOrOpenAssetDatabase()
            ?: languagePackExt?.japaneseDictSQLiteDatabase?.takeIf { it.isOpen }

        if (database != null && database.isOpen) {
            try {
                fun appendCandidates(selection: String, selectionArgs: Array<String>, confidence: Double) {
                    val remainingCount = maxCandidateCount - suggestions.size
                    if (remainingCount <= 0) return

                    database.query(
                        "dictionary",
                        arrayOf("reading", "word"),
                        selection,
                        selectionArgs,
                        null,
                        null,
                        "frequency DESC, reading ASC, word ASC",
                        maxCandidateCount.toString(),
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            if (suggestions.size >= maxCandidateCount) break
                            val reading = cursor.getString(0)
                            val word = cursor.getString(1)
                            if (suggestions.any { (it as? WordSuggestionCandidate)?.text == word }) continue

                            suggestions.add(WordSuggestionCandidate(
                                text = word,
                                secondaryText = reading,
                                confidence = confidence,
                                isEligibleForAutoCommit = false,
                                sourceProvider = this@JapaneseLanguageProvider,
                            ))
                        }
                    }
                }

                appendCandidates("reading = ?", arrayOf(queryText), confidence = 0.9)
                if (suggestions.size < maxCandidateCount) {
                    appendCandidates(
                        "reading >= ? AND reading < ? AND reading <> ?",
                        arrayOf(queryText, "$queryText\uFFFF", queryText),
                        confidence = 0.65,
                    )
                }
                flogDebug { "Japanese DB query '$queryText' returned ${suggestions.size} candidates." }
            } catch (e: Exception) {
                flogError { "SQLiteException in Japanese Language Provider: composing=${content.composingText}, error='$e'" }
            }
        }

        // Keep a tiny emergency dictionary so conversion still works if the asset cannot be opened.
        if (database == null || !database.isOpen) {
            for ((reading, words) in builtInDict) {
                if (suggestions.size >= maxCandidateCount) break
                if (reading != queryText && reading.startsWith(queryText)) {
                    for (word in words) {
                        if (suggestions.size >= maxCandidateCount) break
                        if (suggestions.none { (it as? WordSuggestionCandidate)?.text == word }) {
                            suggestions.add(WordSuggestionCandidate(
                                text = word,
                                secondaryText = reading,
                                confidence = 0.6,
                                isEligibleForAutoCommit = false,
                                sourceProvider = this@JapaneseLanguageProvider,
                            ))
                        }
                    }
                }
            }
        }

        return suggestions.take(maxCandidateCount)
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
        assetDatabase?.close()
        assetDatabase = null
        scope.cancel()
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
