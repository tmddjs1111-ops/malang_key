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
import com.google.android.apps.inputmethod.libs.mozc.session.MozcJni
import dev.malangkey.lib.devtools.flogDebug
import dev.malangkey.lib.devtools.flogError
import dev.malangkey.lib.devtools.flogInfo
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Command
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Input
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.KeyEvent
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Output
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.Request
import org.mozc.android.inputmethod.japanese.protobuf.ProtoCommands.SessionCommand

/**
 * Talks to the Mozc engine in libmozc.so: hiragana reading in, conversion candidates out.
 *
 * Romaji / flick composition stays in the keyboard. Mozc is only used as the
 * "reading -> kanji candidates" converter, so every query starts from an empty composition.
 *
 * The native session is not thread-safe, so every call goes through [mutex] on a single thread.
 */
class MozcSession(private val context: Context) {
    data class Candidate(
        val value: String,
        val reading: String,
        val id: Int,
    )

    companion object {
        /** mozc.data inside the APK. Must come from the same Mozc build as libmozc.so. */
        const val AssetDataPath = "mozc/mozc.data"
        private const val InstalledDataFileName = "mozc.data"
        private const val InstalledDataStampFileName = "mozc.data.stamp"
        private const val ProfileDirName = "mozc_profile"

        private val nativeLock = Any()

        @Volatile
        private var nativeReady: Boolean? = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val mutex = Mutex()
    private var sessionId: Long? = null

    /** Whether composition via UPDATE_COMPOSITION produced a preedit; null until first tried. */
    private var updateCompositionWorks: Boolean? = null

    val isOpen: Boolean
        get() = sessionId != null

    /** Loads the engine and creates a session. Safe to call repeatedly. */
    suspend fun open(): Boolean = withContext(dispatcher) {
        mutex.withLock { openLocked() }
    }

    /** Returns conversion / prediction candidates for [reading] (hiragana), best first. */
    suspend fun convert(reading: String, maxCount: Int): List<Candidate> = withContext(dispatcher) {
        mutex.withLock {
            if (reading.isEmpty() || maxCount <= 0 || !openLocked()) return@withLock emptyList()
            try {
                val output = composeLocked(reading) ?: return@withLock emptyList()
                extractCandidates(output, reading, maxCount)
            } catch (e: Throwable) {
                flogError { "Mozc convert failed for '$reading': $e" }
                emptyList()
            }
        }
    }

    /**
     * Tells Mozc the user picked [value] for [reading], so its user history learns it.
     * Rebuilds the composition first because other queries may have run in between.
     */
    suspend fun learn(reading: String, value: String) = withContext(dispatcher) {
        mutex.withLock {
            if (reading.isEmpty() || !openLocked()) return@withLock
            try {
                val output = composeLocked(reading) ?: return@withLock
                val candidate = extractCandidates(output, reading, Int.MAX_VALUE)
                    .firstOrNull { it.value == value } ?: run {
                        sendSessionCommandLocked(SessionCommand.CommandType.REVERT)
                        return@withLock
                    }
                sendSessionCommandLocked(SessionCommand.CommandType.SUBMIT_CANDIDATE) { setId(candidate.id) }
            } catch (e: Throwable) {
                flogError { "Mozc learn failed for '$reading' -> '$value': $e" }
            }
        }
    }

    suspend fun close() = withContext(dispatcher) {
        mutex.withLock {
            val id = sessionId ?: return@withLock
            try {
                eval(Input.newBuilder().setType(Input.CommandType.DELETE_SESSION).setId(id))
            } catch (e: Throwable) {
                flogError { "Mozc DELETE_SESSION failed: $e" }
            }
            sessionId = null
        }
    }

    private fun openLocked(): Boolean {
        if (sessionId != null) return true
        if (!ensureNativeReady()) return false
        return try {
            val output = eval(Input.newBuilder().setType(Input.CommandType.CREATE_SESSION))
            if (!output.hasId() || output.errorCode != Output.ErrorCode.SESSION_SUCCESS) {
                flogError { "Mozc CREATE_SESSION failed: ${output.errorCode}" }
                return false
            }
            sessionId = output.id
            eval(
                Input.newBuilder()
                    .setType(Input.CommandType.SET_REQUEST)
                    .setId(output.id)
                    .setRequest(mobileRequest())
            )
            flogInfo { "Mozc session ${output.id} created (data ${MozcJni.getDataVersion()})" }
            true
        } catch (e: Throwable) {
            flogError { "Mozc session setup failed: $e" }
            sessionId = null
            false
        }
    }

    /** Same request settings the official Mozc Android client uses for software keyboards. */
    private fun mobileRequest(): Request = Request.newBuilder()
        .setZeroQuerySuggestion(true)
        .setMixedConversion(true)
        .setKanaModifierInsensitiveConversion(true)
        .setAutoPartialSuggestion(true)
        .setSpecialRomanjiTable(Request.SpecialRomanjiTable.QWERTY_MOBILE_TO_HIRAGANA)
        .setSpaceOnAlphanumeric(Request.SpaceOnAlphanumeric.COMMIT)
        .setCandidatePageSize(32)
        .build()

    /**
     * Replaces the current composition with [reading] and returns the resulting output.
     *
     * Tries UPDATE_COMPOSITION first (sets the whole string at once). If that does not produce
     * a preedit on this engine build, falls back to sending the reading one character at a time.
     */
    private fun composeLocked(reading: String): Output? {
        sendSessionCommandLocked(SessionCommand.CommandType.REVERT)

        if (updateCompositionWorks != false) {
            val output = sendSessionCommandLocked(SessionCommand.CommandType.UPDATE_COMPOSITION) {
                addCompositionEvents(
                    SessionCommand.CompositionEvent.newBuilder()
                        .setCompositionString(reading)
                        .setProbability(1.0)
                )
            }
            val works = output?.hasPreedit() == true
            if (updateCompositionWorks == null) {
                updateCompositionWorks = works
                flogInfo { "Mozc UPDATE_COMPOSITION supported: $works" }
            }
            if (works) return output
            sendSessionCommandLocked(SessionCommand.CommandType.REVERT)
        }

        var output: Output? = null
        var offset = 0
        while (offset < reading.length) {
            val codePoint = reading.codePointAt(offset)
            val key = KeyEvent.newBuilder()
                .setKeyCode(codePoint)
                .setKeyString(String(Character.toChars(codePoint)))
                .setInputStyle(KeyEvent.InputStyle.AS_IS)
            output = eval(
                Input.newBuilder()
                    .setType(Input.CommandType.SEND_KEY)
                    .setId(sessionId!!)
                    .setKey(key)
            )
            offset += Character.charCount(codePoint)
        }
        return output
    }

    private fun extractCandidates(output: Output, reading: String, maxCount: Int): List<Candidate> {
        val result = LinkedHashMap<String, Candidate>()
        if (output.hasAllCandidateWords()) {
            for (word in output.allCandidateWords.candidatesList) {
                if (result.size >= maxCount) break
                val value = word.value
                if (value.isEmpty() || value in result) continue
                result[value] = Candidate(
                    value = value,
                    reading = if (word.hasKey()) word.key else reading,
                    id = word.id,
                )
            }
        }
        if (result.isEmpty() && output.hasCandidateWindow()) {
            for (candidate in output.candidateWindow.candidateList) {
                if (result.size >= maxCount) break
                val value = candidate.value
                if (value.isEmpty() || value in result) continue
                result[value] = Candidate(value = value, reading = reading, id = candidate.id)
            }
        }
        flogDebug { "Mozc '$reading' -> ${result.keys.take(10)}" }
        return result.values.toList()
    }

    private inline fun sendSessionCommandLocked(
        type: SessionCommand.CommandType,
        configure: SessionCommand.Builder.() -> Unit = {},
    ): Output? {
        val id = sessionId ?: return null
        val command = SessionCommand.newBuilder().setType(type).apply(configure)
        return eval(
            Input.newBuilder()
                .setType(Input.CommandType.SEND_COMMAND)
                .setId(id)
                .setCommand(command)
        )
    }

    private fun eval(input: Input.Builder): Output {
        val request = Command.newBuilder().setInput(input).build().toByteArray()
        val response = MozcJni.evalCommand(request)
            ?: throw IllegalStateException("evalCommand returned null")
        return Command.parseFrom(response).output
    }

    /** Loads libmozc once per process: register natives, then point it at the data file. */
    private fun ensureNativeReady(): Boolean {
        nativeReady?.let { return it }
        synchronized(nativeLock) {
            nativeReady?.let { return it }
            val ready = try {
                if (!MozcJni.isLibraryLoaded()) {
                    flogError { "libmozc.so could not be loaded" }
                    false
                } else {
                    val dataFile = installDataFile()
                    if (dataFile == null) {
                        false
                    } else {
                        val profileDir = File(context.filesDir, ProfileDirName).apply { mkdirs() }
                        MozcJni.initialize()
                        MozcJni.onPostLoad(profileDir.absolutePath, dataFile.absolutePath).also {
                            if (!it) flogError { "Mozc onPostLoad returned false" }
                        }
                    }
                }
            } catch (e: Throwable) {
                flogError { "Mozc native init failed: $e" }
                false
            }
            nativeReady = ready
            return ready
        }
    }

    /**
     * Copies mozc.data out of the APK so the engine can open it by path.
     * Re-copies whenever the app is updated, so a new dictionary replaces the old one.
     */
    private fun installDataFile(): File? {
        val destination = File(context.filesDir, InstalledDataFileName)
        val stampFile = File(context.filesDir, InstalledDataStampFileName)
        val stamp = try {
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
        } catch (e: Exception) {
            "unknown"
        }
        if (destination.exists() && stampFile.exists() && stampFile.readText() == stamp) {
            return destination
        }
        return try {
            val temporaryFile = File(context.filesDir, "$InstalledDataFileName.tmp")
            context.assets.open(AssetDataPath).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            destination.delete()
            if (!temporaryFile.renameTo(destination)) {
                temporaryFile.copyTo(destination, overwrite = true)
                temporaryFile.delete()
            }
            stampFile.writeText(stamp)
            destination
        } catch (e: Exception) {
            flogError { "Failed to install $AssetDataPath (is it in assets?): $e" }
            null
        }
    }
}
