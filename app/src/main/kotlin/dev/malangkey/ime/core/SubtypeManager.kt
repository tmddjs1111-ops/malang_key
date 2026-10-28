/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.malangkey.ime.core

import android.content.Context
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.keyboard.CurrencySet
import dev.malangkey.ime.keyboard.WideScreenLayout
import dev.malangkey.ime.window.FoldState
import dev.malangkey.keyboardManager
import dev.malangkey.lib.FlorisLocale
import dev.malangkey.lib.devtools.flogDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.florisboard.lib.kotlin.collectLatestIn

val SubtypeJsonConfig = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Class which acts as a high level helper for the raw implementation of subtypes in the prefs. Additionally provides
 * helper methods for the in-keyboard language switch process.
 */
class SubtypeManager(context: Context) {
    private val prefs by FlorisPreferenceStore
    private val keyboardManager by context.keyboardManager()
    private val scope = CoroutineScope(Dispatchers.Default)

    val subtypesFlow: StateFlow<List<Subtype>>
        field = MutableStateFlow(listOf())
    inline var subtypes
        get() = subtypesFlow.value
        private set(v) { subtypesFlow.value = v }

    /**
     * 실제로 쓰는 서브타입. 펼친 화면·가로 화면 자판 설정이 켜져 있으면 [selectedSubtype]의
     * 글자 자판만 바꾼 것이 들어간다 (id는 같다).
     */
    val activeSubtypeFlow: StateFlow<Subtype>
        field = MutableStateFlow(Subtype.DEFAULT)
    inline val activeSubtype
        get() = activeSubtypeFlow.value

    /** 사용자가 고른 서브타입 (넓은 화면 자판 적용 전). */
    @Volatile
    private var selectedSubtype: Subtype = Subtype.DEFAULT
        set(v) {
            field = v
            publishActiveSubtype()
        }

    private fun publishActiveSubtype() {
        val isWide = WideScreenLayout.isWide(
            condition = prefs.keyboard.wideScreenCondition.get(),
            isUnfolded = FoldState.isUnfolded.value,
            isLandscape = FoldState.isLandscape.value,
        )
        activeSubtypeFlow.value = WideScreenLayout.apply(
            subtype = selectedSubtype,
            wideLayoutId = prefs.keyboard.wideScreenKoreanLayout.get(),
            isWide = isWide,
        )
    }

    init {
        scope.launch {
            combine(
                prefs.keyboard.wideScreenKoreanLayout.asFlow(),
                prefs.keyboard.wideScreenCondition.asFlow(),
                FoldState.isUnfolded,
                FoldState.isLandscape,
            ) { _, _, _, _ -> }.collect {
                publishActiveSubtype()
            }
        }

        prefs.localization.subtypes.asFlow().collectLatestIn(scope) { listRaw ->
            flogDebug { listRaw }
            val list = if (listRaw.isNotBlank()) {
                SubtypeJsonConfig.decodeFromString<List<Subtype>>(listRaw)
            } else {
                emptyList()
            }
            subtypes = list
            evaluateActiveSubtype(list)
        }
    }

    private fun persistNewSubtypeList(list: List<Subtype>) = scope.launch {
        val listRaw = SubtypeJsonConfig.encodeToString(list)
        prefs.localization.subtypes.set(listRaw)
    }

    /**
     * Gets the active subtype and returns it. If the activeSubtypeId points to a non-existent
     * subtype, this method tries to determine a new active subtype.
     *
     * @return The active subtype or null, if the subtype list is empty or no new active subtype
     *  could be determined.
     */
    private fun evaluateActiveSubtype(list: List<Subtype>) = scope.launch {
        val activeSubtypeId = prefs.localization.activeSubtypeId.get()
        val subtype = list.find { it.id == activeSubtypeId } ?: list.firstOrNull() ?: Subtype.DEFAULT
        if (subtype.id != activeSubtypeId) {
            prefs.localization.activeSubtypeId.set(subtype.id)
        }
        selectedSubtype = subtype
    }

    /**
     * Adds a given [subtype] to the subtype list, if it does not exist.
     *
     * @param subtype The subtype which should be added.
     * @return True if the subtype was added, false otherwise. A return value of false indicates
     *  that the subtype already exists.
     */
    fun addSubtype(subtype: Subtype): Boolean {
        val subtypeToAdd = subtype.copy(id = System.currentTimeMillis())
        val subtypeList = subtypes
        if (subtypeList.find { it.equalsExcludingId(subtype) } != null) {
            return false
        }
        val newSubtypeList = subtypeList + subtypeToAdd
        persistNewSubtypeList(newSubtypeList)
        return true
    }

    /**
     * Replaces the subtype order with [orderedSubtypes], which must contain exactly the current
     * subtypes. The language switch key and swipe gestures cycle through subtypes in this order.
     */
    fun reorderSubtypes(orderedSubtypes: List<Subtype>) {
        val current = subtypes
        if (orderedSubtypes.size != current.size ||
            orderedSubtypes.map { it.id }.toSet() != current.map { it.id }.toSet()
        ) {
            return
        }
        persistNewSubtypeList(orderedSubtypes)
    }

    fun addSubtypeAndActivate(subtype: Subtype): Boolean {
        val subtypeToAdd = subtype.copy(id = System.currentTimeMillis())
        val subtypeList = subtypes
        if (subtypeList.find { it.equalsExcludingId(subtype) } != null) {
            return false
        }
        val newSubtypeList = subtypeList + subtypeToAdd
        selectedSubtype = subtypeToAdd
        // 활성 id를 먼저 저장한 뒤 목록을 저장한다. 목록이 먼저 바뀌면 evaluateActiveSubtype이
        // 아직 예전 id를 읽어 방금 켠 자판 대신 이전 자판으로 되돌린다.
        scope.launch {
            prefs.localization.activeSubtypeId.set(subtypeToAdd.id)
            prefs.localization.subtypes.set(SubtypeJsonConfig.encodeToString(newSubtypeList))
        }
        return true
    }

    /**
     * Gets the currency set from the given subtype and returns it. Falls back to a default one if the subtype does not
     * exist.
     *
     * @return The currency set or a fallback.
     */
    fun getCurrencySet(subtypeToSearch: Subtype): CurrencySet {
        return keyboardManager.resources.currencySets.value[subtypeToSearch.currencySet] ?: CurrencySet.Fallback
    }

    /**
     * Gets a subtype by the given [id].
     *
     * @param id The id of the subtype you want to get.
     * @return The subtype or null, if no matching subtype could be found.
     */
    fun getSubtypeById(id: Long): Subtype? {
        val subtypeList = subtypes
        return subtypeList.find { it.id == id }
    }

    /**
     * Gets the default system subtype for a given [locale].
     *
     * @param locale The locale of the default system subtype to get.
     * @return The default system locale or null, if no matching default system subtype could be
     *  found.
     */
    fun getSubtypePresetForLocale(locale: FlorisLocale): SubtypePreset? {
        val presets = keyboardManager.resources.subtypePresets.value
        return presets.find { it.locale == locale } ?: presets.find { it.locale.language == locale.language }
    }

    /**
     * Modifies an existing subtype with the newly provided details. In order to determine which
     * subtype should be updated, the id must be the same.
     *
     * @param subtypeToModify The subtype with the new details but same id.
     */
    fun modifySubtypeWithSameId(subtypeToModify: Subtype) {
        val subtypeList = subtypes
        val index = subtypeList.indexOfFirst { subtypeToModify.id == it.id }
        if (index >= 0 && index < subtypeList.size) {
            val newSubtypeList = subtypeList.mapIndexed { n, subtype ->
                if (n == index) {
                    subtypeToModify
                } else {
                    subtype
                }
            }
            persistNewSubtypeList(newSubtypeList)
        }
    }

    /**
     * Removes a given [subtypeToRemove]. Nothing happens if the given [subtypeToRemove] does not
     * exist.
     *
     * @param subtypeToRemove The subtype which should be removed.
     */
    fun removeSubtype(subtypeToRemove: Subtype) {
        val subtypeList = subtypes
        val indexToRemove = subtypeList.indexOf(subtypeToRemove)
        if (indexToRemove in subtypeList.indices) {
            val newSubtypeList = subtypeList.mapIndexedNotNull { n, subtype ->
                if (n != indexToRemove) {
                    subtype
                } else {
                    null
                }
            }
            persistNewSubtypeList(newSubtypeList)
            evaluateActiveSubtype(newSubtypeList)
        }
    }

    /**
     * Switch to the previous subtype in the subtype list if possible.
     */
    fun switchToPrevSubtype() = scope.launch {
        val subtypeList = subtypes
        val cachedActiveSubtype = selectedSubtype
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype = Subtype.DEFAULT
        for (subtype in subtypeList.asReversed()) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype.id == cachedActiveSubtype.id) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.last()
        }
        prefs.localization.activeSubtypeId.set(newActiveSubtype.id)
        selectedSubtype = newActiveSubtype
    }

    /**
     * Switch to the next subtype in the subtype list if possible.
     */
    fun switchToNextSubtype() = scope.launch {
        val subtypeList = subtypes
        val cachedActiveSubtype = selectedSubtype
        var triggerNextSubtype = false
        var newActiveSubtype: Subtype = Subtype.DEFAULT
        for (subtype in subtypeList) {
            if (triggerNextSubtype) {
                triggerNextSubtype = false
                newActiveSubtype = subtype
            } else if (subtype.id == cachedActiveSubtype.id) {
                triggerNextSubtype = true
            }
        }
        if (triggerNextSubtype) {
            newActiveSubtype = subtypeList.first()
        }
        prefs.localization.activeSubtypeId.set(newActiveSubtype.id)
        selectedSubtype = newActiveSubtype
    }

    fun switchToSubtypeById(id: Long) = scope.launch {
        if (subtypes.any { it.id == id }) {
            selectedSubtype = getSubtypeById(id)!!
            prefs.localization.activeSubtypeId.set(id)
        }
    }
}
