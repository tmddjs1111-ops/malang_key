/*
 * Copyright (C) 2026 The MalangKey Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.ime.text.keyboard

/** Tracks repeated stationary taps for a Japanese 20-key kana key. */
internal class JapaneseMultiTapState(
    private val timeoutMillis: Long = DefaultTimeoutMillis,
) {
    data class Result(
        val index: Int,
        val replacePrevious: Boolean,
    )

    private var lastBaseCode: Int? = null
    private var lastOutputCode: Int? = null
    private var lastIndex: Int = 0
    private var lastTapAtMillis: Long = 0L

    fun next(
        baseCode: Int,
        sequenceCodes: List<Int>,
        precedingCode: Int?,
        nowMillis: Long,
    ): Result {
        require(sequenceCodes.isNotEmpty())

        val canCycle = lastBaseCode == baseCode &&
            nowMillis >= lastTapAtMillis &&
            nowMillis - lastTapAtMillis <= timeoutMillis &&
            precedingCode == lastOutputCode
        val nextIndex = if (canCycle) {
            (lastIndex + 1) % sequenceCodes.size
        } else {
            0
        }

        lastBaseCode = baseCode
        lastOutputCode = sequenceCodes[nextIndex]
        lastIndex = nextIndex
        lastTapAtMillis = nowMillis

        return Result(index = nextIndex, replacePrevious = canCycle)
    }

    fun reset() {
        lastBaseCode = null
        lastOutputCode = null
        lastIndex = 0
        lastTapAtMillis = 0L
    }

    companion object {
        const val DefaultTimeoutMillis: Long = 700L
    }
}
