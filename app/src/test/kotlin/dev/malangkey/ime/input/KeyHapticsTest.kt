package dev.malangkey.ime.input

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.longs.shouldBeLessThanOrEqual
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

    test("타건이 겹치지 않도록 진동은 15ms 이하이고, 세기를 바꿔도 길이는 그대로다") {
        for (feel in HapticFeel.entries) {
            for (kind in HapticKind.entries) {
                val weak = KeyHaptics.pattern(feel, 0f, kind)
                val strong = KeyHaptics.pattern(feel, 1f, kind)
                strong.totalMillis shouldBeLessThanOrEqual 15L
                strong.totalMillis shouldBe weak.totalMillis
            }
        }
    }

    test("세기를 올리면 크기가 커지고, 100%는 최대 크기다") {
        KeyHaptics.pattern(HapticFeel.CRISP, 1f, HapticKind.KEY).amplitudes.max() shouldBe 255
        for (feel in HapticFeel.entries) {
            KeyHaptics.pattern(feel, 1f, HapticKind.KEY).amplitudes.max() shouldBeGreaterThan
                KeyHaptics.pattern(feel, 0.3f, HapticKind.KEY).amplitudes.max()
        }
    }

    test("길게 누르기는 일반 키보다 약하다") {
        for (feel in HapticFeel.entries) {
            KeyHaptics.pattern(feel, 1f, HapticKind.LONG_PRESS).amplitudes.max() shouldBeLessThan
                KeyHaptics.pattern(feel, 1f, HapticKind.KEY).amplitudes.max()
        }
    }
})
