package dev.malangkey.ime.input

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class KeyHapticsTest : FunSpec({
    test("모든 모양은 대기 구간으로 시작하고 세기는 0~255 안에 있다") {
        for (feel in HapticFeel.entries) {
            for (kind in HapticKind.entries) {
                for (strength in listOf(0f, 0.01f, 0.5f, 1f, 2f)) {
                    val p = KeyHaptics.pattern(feel, strength, kind)
                    p.timings.size shouldBe p.amplitudes.size
                    p.amplitudes[0] shouldBe 0
                    p.amplitudes.forEach { it shouldBeInRange 0..255 }
                }
            }
        }
    }

    test("100%는 최대 세기로 울린다") {
        for (feel in HapticFeel.entries) {
            KeyHaptics.pattern(feel, 1f, HapticKind.KEY).amplitudes.max() shouldBe 255
        }
    }

    test("세기를 올리면 크기와 길이가 함께 커진다") {
        for (feel in HapticFeel.entries) {
            val weak = KeyHaptics.pattern(feel, 0.3f, HapticKind.KEY)
            val strong = KeyHaptics.pattern(feel, 1f, HapticKind.KEY)
            strong.amplitudes.max() shouldBeGreaterThan weak.amplitudes.max()
            strong.totalMillis shouldBeGreaterThan weak.totalMillis
        }
    }

    test("큰 키는 일반 키보다 길게 울린다") {
        for (feel in HapticFeel.entries) {
            KeyHaptics.pattern(feel, 0.7f, HapticKind.BIG_KEY).totalMillis shouldBeGreaterThan
                KeyHaptics.pattern(feel, 0.7f, HapticKind.KEY).totalMillis
        }
    }
})
