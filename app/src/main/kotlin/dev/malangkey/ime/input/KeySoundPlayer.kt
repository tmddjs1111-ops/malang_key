/*
 * Copyright (C) 2026 The MalangKey Contributors
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

package dev.malangkey.ime.input

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dev.malangkey.lib.devtools.flogDebug
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 키 소리를 앱 안에서 직접 낸다.
 *
 * 시스템 효과음(AudioManager.playSoundEffect)은 매번 시스템 서비스를 거쳐 재생돼서 눌렀다 뗀 뒤에야
 * 들릴 만큼 늦고, 기기마다 소리가 비어 있기도 하다. 여기서는 [KeySoundStyle]마다 짧은 소리를 한 번 만들어
 * SoundPool에 올려 두고, 누르는 순간 바로 재생한다. 종류마다 일반 키·지우기·스페이스·엔터 소리가 따로 있다.
 */
class KeySoundPlayer(context: Context) {
    /** 어떤 키를 눌렀는지에 따라 같은 종류 안에서 소리를 조금씩 달리한다. */
    enum class Kind(val pitch: Double, val length: Double) {
        STANDARD(1.0, 1.0),
        DELETE(0.82, 1.0),
        SPACE(0.62, 1.3),
        ENTER(0.7, 1.6),
    }

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val cacheDir = File(context.cacheDir, "key_sounds")
    /** 종류별 SoundPool 샘플 id (Kind 순서). 다 올라간 종류만 들어 있다. */
    private val loadedIds = ConcurrentHashMap<KeySoundStyle, IntArray>()
    private val loading = ConcurrentHashMap<KeySoundStyle, CompletableDeferred<Unit>>()
    /** 올리는 중인 샘플. load()와 완료 콜백이 같은 잠금을 써서, 콜백이 먼저 와도 놓치지 않는다. */
    private val pendingSamples = HashMap<Int, CompletableDeferred<Unit>>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, _ ->
            synchronized(pendingSamples) { pendingSamples.remove(sampleId) }?.complete(Unit)
        }
    }

    fun isLoaded(style: KeySoundStyle) = loadedIds.containsKey(style)

    /** [style]의 소리를 만들어 올리고, 재생할 수 있을 때까지 기다린다. 파일을 쓰므로 메인 스레드에서 부르지 않는다. */
    suspend fun load(style: KeySoundStyle) {
        if (isLoaded(style)) return
        val mine = CompletableDeferred<Unit>()
        val existing = loading.putIfAbsent(style, mine)
        if (existing != null) {
            existing.await()
            return
        }
        try {
            cacheDir.mkdirs()
            val ids = IntArray(Kind.entries.size)
            val waits = mutableListOf<CompletableDeferred<Unit>>()
            for (kind in Kind.entries) {
                val file = File(cacheDir, "${style.name.lowercase()}_${kind.name.lowercase()}_v$SoundVersion.wav")
                if (!file.exists()) writeWav(file, normalize(synthesize(style, kind)))
                val done = CompletableDeferred<Unit>()
                ids[kind.ordinal] = synchronized(pendingSamples) {
                    soundPool.load(file.absolutePath, 1).also { pendingSamples[it] = done }
                }
                waits += done
            }
            waits.forEach { it.await() }
            loadedIds[style] = ids
        } finally {
            mine.complete(Unit)
            loading.remove(style)
        }
    }

    /** 소리가 준비됐으면 바로 재생하고 true, 아직이면 false를 돌려준다. */
    fun play(style: KeySoundStyle, kind: Kind, volume: Float): Boolean {
        val id = loadedIds[style]?.get(kind.ordinal) ?: return false
        val v = volume.coerceIn(0f, 1f)
        val streamId = soundPool.play(id, v, v, 1, 0, 1f)
        flogDebug { "Key sound $style/$kind volume=$v stream=$streamId" }
        return streamId != 0
    }

    fun release() {
        soundPool.release()
    }

    // ---- 소리 만들기 ----

    private fun synthesize(style: KeySoundStyle, kind: Kind): DoubleArray {
        val p = kind.pitch
        val l = kind.length
        return when (style) {
            KeySoundStyle.CLICK -> click(toneHz = 1900.0 * p, toneDecayMs = 5.0 * l, noiseDecayMs = 2.5 * l, lengthMs = 30 * l)
            KeySoundStyle.MALANG -> sweep(fromHz = 380.0 * p, toHz = 1000.0 * p, riseMs = 12.0, decayMs = 16.0 * l, lengthMs = 60 * l)
            KeySoundStyle.TYPEWRITER -> typewriter(p, l, withBell = kind == Kind.ENTER)
            KeySoundStyle.MECHANICAL -> mechanical(p, l)
            KeySoundStyle.SOFT -> soft(p, l)
            KeySoundStyle.DROP -> sweep(fromHz = 500.0 * p, toHz = 1700.0 * p, riseMs = 6.0, decayMs = 20.0 * l, lengthMs = 55 * l)
            KeySoundStyle.WOOD -> wood(p, l)
            KeySoundStyle.MARIMBA -> marimba(kind)
            KeySoundStyle.RETRO -> retro(p, l, kind)
            KeySoundStyle.TICK -> tick(p)
        }
    }

    private fun buffer(lengthMs: Double) = DoubleArray((SampleRate * lengthMs / 1000).toInt().coerceAtLeast(1))

    private fun msOf(i: Int) = i * 1000.0 / SampleRate

    /** 짧은 잡음과 감쇠하는 사인파를 섞은 기본 클릭음. */
    private fun click(toneHz: Double, toneDecayMs: Double, noiseDecayMs: Double, lengthMs: Double): DoubleArray {
        val out = buffer(lengthMs)
        val random = Random(toneHz.toInt())
        var lp = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            lp += (random.nextDouble(-1.0, 1.0) - lp) * 0.45
            out[i] = lp * exp(-ms / noiseDecayMs) * 0.55 + sin(2 * PI * toneHz * ms / 1000) * exp(-ms / toneDecayMs) * 0.45
        }
        return out
    }

    /** 음높이가 위로 미끄러지는 사인파 (말랑 뽁, 물방울). */
    private fun sweep(fromHz: Double, toHz: Double, riseMs: Double, decayMs: Double, lengthMs: Double): DoubleArray {
        val out = buffer(lengthMs)
        var phase = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            val freq = fromHz + (toHz - fromHz) * (1 - exp(-ms / riseMs))
            phase += 2 * PI * freq / SampleRate
            out[i] = sin(phase) * (ms / 1.5).coerceAtMost(1.0) * exp(-ms / decayMs)
        }
        return out
    }

    /** 쇠막대가 종이를 때리는 날카로운 소리. 엔터는 줄 끝 종소리를 더한다. */
    private fun typewriter(p: Double, l: Double, withBell: Boolean): DoubleArray {
        val out = buffer(if (withBell) 380.0 else 45.0 * l)
        val random = Random(7)
        var lp = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            val n = random.nextDouble(-1.0, 1.0)
            lp += (n - lp) * 0.3
            val hiss = (n - lp) * exp(-ms / 1.5)
            val ring = sin(2 * PI * 3200 * p * ms / 1000) * exp(-ms / 4.0) * 0.3
            val body = sin(2 * PI * 220 * p * ms / 1000) * exp(-ms / (12.0 * l)) * 0.5
            var v = hiss * 0.8 + ring + body
            if (withBell && ms > 40) {
                val b = ms - 40
                v += (sin(2 * PI * 2093 * b / 1000) + sin(2 * PI * 3135 * b / 1000) * 0.5) * exp(-b / 110) * 0.35
            }
            out[i] = v
        }
        return out
    }

    /** 걸쇠가 두 번 걸리는 '찰칵' 클릭과 바닥에 닿는 낮은 소리. */
    private fun mechanical(p: Double, l: Double): DoubleArray {
        val out = buffer(50.0 * l)
        val random = Random(11)
        for (i in out.indices) {
            val ms = msOf(i)
            var v = sin(2 * PI * 170 * p * ms / 1000) * exp(-ms / (18.0 * l)) * 0.6
            for (start in doubleArrayOf(0.0, 14.0 * l)) {
                if (ms >= start) {
                    val t = ms - start
                    v += random.nextDouble(-1.0, 1.0) * exp(-t / 1.2) * 0.6
                    v += sin(2 * PI * 2600 * p * t / 1000) * exp(-t / 2.5) * 0.4
                }
            }
            out[i] = v
        }
        return out
    }

    /** 고무 키처럼 둥글고 작은 소리. */
    private fun soft(p: Double, l: Double): DoubleArray {
        val out = buffer(40.0 * l)
        val random = Random(3)
        var lp = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            lp += (random.nextDouble(-1.0, 1.0) - lp) * 0.15
            out[i] = (lp * exp(-ms / (5.0 * l)) * 1.4 + sin(2 * PI * 420 * p * ms / 1000) * exp(-ms / 14.0) * 0.5) * 0.6
        }
        return out
    }

    /** 속이 빈 나무를 두드리는 소리. */
    private fun wood(p: Double, l: Double): DoubleArray {
        val out = buffer(60.0 * l)
        val random = Random(5)
        for (i in out.indices) {
            val ms = msOf(i)
            out[i] = sin(2 * PI * 780 * p * ms / 1000) * exp(-ms / (30.0 * l)) * 0.7 +
                sin(2 * PI * 2150 * p * ms / 1000) * exp(-ms / 9.0) * 0.35 +
                random.nextDouble(-1.0, 1.0) * exp(-ms / 1.0) * 0.4
        }
        return out
    }

    /** 키마다 다른 음을 내는 마림바. 같은 화음(도·미·솔)이라 이어 쳐도 어울린다. */
    private fun marimba(kind: Kind): DoubleArray {
        val f0 = when (kind) {
            Kind.STANDARD -> 784.0 // 솔
            Kind.DELETE -> 659.3 // 미
            Kind.SPACE -> 523.3 // 도
            Kind.ENTER -> 1046.5 // 높은 도
        }
        val out = buffer(220.0)
        for (i in out.indices) {
            val ms = msOf(i)
            val attack = (ms / 2.0).coerceAtMost(1.0)
            out[i] = attack * (sin(2 * PI * f0 * ms / 1000) * exp(-ms / 150) +
                sin(2 * PI * f0 * 3.9 * ms / 1000) * exp(-ms / 25) * 0.35)
        }
        return out
    }

    /** 옛날 게임기 같은 네모파 '삑'. 엔터는 세 음이 올라간다. */
    private fun retro(p: Double, l: Double, kind: Kind): DoubleArray {
        val notes = if (kind == Kind.ENTER) doubleArrayOf(659.0, 784.0, 1047.0) else doubleArrayOf(988.0 * p, 740.0 * p)
        val noteMs = if (kind == Kind.ENTER) 35.0 else 25.0 * l
        val out = buffer(noteMs * notes.size + 10)
        var phase = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            val freq = notes[(ms / noteMs).toInt().coerceAtMost(notes.lastIndex)]
            phase += freq / SampleRate
            val square = if ((phase % 1.0) < 0.5) 1.0 else -1.0
            out[i] = square * 0.35 * exp(-ms / (60.0 * l))
        }
        return out
    }

    /** 아주 짧고 가벼운 '톡'. */
    private fun tick(p: Double): DoubleArray {
        val out = buffer(15.0)
        val random = Random(13)
        var lp = 0.0
        for (i in out.indices) {
            val ms = msOf(i)
            val n = random.nextDouble(-1.0, 1.0)
            lp += (n - lp) * 0.5
            out[i] = sin(2 * PI * 3000 * p * ms / 1000) * exp(-ms / 1.8) * 0.7 + (n - lp) * exp(-ms / 0.8) * 0.4
        }
        return out
    }

    /** 최대 크기를 맞추고, 끝에서 뚝 끊기는 잡음이 나지 않도록 마지막 몇 ms를 줄인다. */
    private fun normalize(samples: DoubleArray): ShortArray {
        val peak = samples.maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
        val tail = SampleRate * 3 / 1000
        return ShortArray(samples.size) { i ->
            val fade = if (i < samples.size - tail) 1.0 else (samples.size - i).toDouble() / tail
            (samples[i] / peak * 0.9 * fade * Short.MAX_VALUE).toInt().toShort()
        }
    }

    private fun writeWav(file: File, samples: ShortArray) {
        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(SampleRate).putInt(SampleRate * 2).putShort(2).putShort(16)
        buffer.put("data".toByteArray()).putInt(dataSize)
        for (s in samples) buffer.putShort(s)
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { it.write(buffer.array()) }
        tmp.renameTo(file)
    }

    private companion object {
        const val SampleRate = 44100
        /** 소리 모양을 바꾸면 올려서 캐시에 남은 옛 파일을 쓰지 않게 한다. */
        const val SoundVersion = 2
    }
}
