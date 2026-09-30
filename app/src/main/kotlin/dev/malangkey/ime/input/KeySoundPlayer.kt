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
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 키 소리를 앱 안에서 직접 낸다.
 *
 * 시스템 효과음(AudioManager.playSoundEffect)은 매번 시스템 서비스를 거쳐 재생돼서 눌렀다 뗀 뒤에야
 * 들릴 만큼 늦고, 기기마다 소리가 비어 있기도 하다. 여기서는 짧은 클릭음을 한 번 만들어 SoundPool에
 * 미리 올려 두고, 누르는 순간 바로 재생한다.
 */
class KeySoundPlayer(context: Context) {
    enum class Sound(val fileName: String) {
        STANDARD("standard"),
        DELETE("delete"),
        SPACE("space"),
        ENTER("enter"),
        MALANG("malang"),
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

    @Volatile
    private var soundIds = IntArray(Sound.entries.size)

    private val cacheDir = File(context.cacheDir, "key_sounds")

    /** WAV를 만들고 SoundPool에 올린다. 파일을 쓰므로 메인 스레드에서 부르지 않는다. */
    fun load() {
        cacheDir.mkdirs()
        val ids = IntArray(Sound.entries.size)
        for (sound in Sound.entries) {
            val file = File(cacheDir, "${sound.fileName}_v$SoundVersion.wav")
            if (!file.exists()) writeWav(file, synthesize(sound))
            ids[sound.ordinal] = soundPool.load(file.absolutePath, 1)
        }
        soundIds = ids
    }

    /** 소리가 준비됐으면 바로 재생하고 true, 아직이면 false를 돌려준다. */
    fun play(sound: Sound, volume: Float): Boolean {
        val id = soundIds[sound.ordinal]
        if (id == 0) return false
        val v = volume.coerceIn(0f, 1f)
        // 아직 디코딩 중인 소리면 play()가 0을 돌려준다.
        val streamId = soundPool.play(id, v, v, 1, 0, 1f)
        flogDebug { "Key sound $sound volume=$v stream=$streamId" }
        return streamId != 0
    }

    private fun synthesize(sound: Sound): ShortArray = when (sound) {
        Sound.STANDARD -> click(toneHz = 1900.0, toneDecayMs = 5.0, noiseDecayMs = 2.5, lengthMs = 28)
        Sound.DELETE -> click(toneHz = 1300.0, toneDecayMs = 6.0, noiseDecayMs = 2.5, lengthMs = 32)
        Sound.SPACE -> click(toneHz = 750.0, toneDecayMs = 9.0, noiseDecayMs = 4.0, lengthMs = 45)
        Sound.ENTER -> click(toneHz = 950.0, toneDecayMs = 12.0, noiseDecayMs = 4.0, lengthMs = 55)
        Sound.MALANG -> pop()
    }

    /** 짧은 잡음과 감쇠하는 사인파를 섞은 기계식 키 느낌의 클릭음. */
    private fun click(toneHz: Double, toneDecayMs: Double, noiseDecayMs: Double, lengthMs: Int): ShortArray {
        val n = SampleRate * lengthMs / 1000
        val random = Random(toneHz.toInt())
        var lowPassed = 0.0
        return ShortArray(n) { i ->
            val t = i.toDouble() / SampleRate
            val ms = t * 1000.0
            lowPassed += (random.nextDouble(-1.0, 1.0) - lowPassed) * 0.45
            val noise = lowPassed * exp(-ms / noiseDecayMs)
            val tone = sin(2 * PI * toneHz * t) * exp(-ms / toneDecayMs)
            toSample((noise * 0.55 + tone * 0.45) * fadeOut(i, n))
        }
    }

    /** 위로 살짝 미끄러지는 물방울 같은 '말랑' 소리. */
    private fun pop(): ShortArray {
        val lengthMs = 60
        val n = SampleRate * lengthMs / 1000
        var phase = 0.0
        return ShortArray(n) { i ->
            val ms = i * 1000.0 / SampleRate
            val freq = 380.0 + 620.0 * (1 - exp(-ms / 12.0))
            phase += 2 * PI * freq / SampleRate
            val attack = (ms / 2.0).coerceAtMost(1.0)
            toSample(sin(phase) * attack * exp(-ms / 16.0) * 0.8 * fadeOut(i, n))
        }
    }

    /** 끝에서 뚝 끊기는 잡음이 나지 않도록 마지막 몇 ms를 줄인다. */
    private fun fadeOut(i: Int, n: Int): Double {
        val tail = SampleRate * 3 / 1000
        return if (i < n - tail) 1.0 else (n - i).toDouble() / tail
    }

    private fun toSample(value: Double): Short =
        (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.9).toInt().toShort()

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
        const val SoundVersion = 1
    }
}
