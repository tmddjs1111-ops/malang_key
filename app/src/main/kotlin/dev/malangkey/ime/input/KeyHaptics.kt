package dev.malangkey.ime.input

import android.os.Vibrator
import org.florisboard.lib.android.vibratePrimitives
import org.florisboard.lib.android.vibrateWaveform
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** 말랑 진동의 느낌. */
enum class HapticFeel(val label: String, val description: String) {
    /** 1.58까지의 기본 진동. 안드로이드의 THUD 진동이라 낮고 둥글게 울린다. */
    THUD("쿵", "낮고 둥글게 쿵 — 예전 기본 진동"),
    CRISP("또각", "짧고 단단하게 한 번"),
    SOFT("톡", "살짝 차오르며 부드럽게"),
    MECHANICAL("기계식", "눌릴 때 딸깍, 올라올 때 톡 — 기계식 키보드처럼"),
    THOCK("묵직", "깊게 누르고 여운이 남게");
}

/** 어떤 상황의 진동인지. 같은 느낌이라도 상황마다 모양이 다르다. */
enum class HapticKind {
    KEY,
    /** 스페이스·엔터·지우기처럼 큰 키. 실제 키보드처럼 조금 더 묵직하게. */
    BIG_KEY,
    LONG_PRESS,
    /** 지우기 반복·삭제 키 밀기처럼 잇달아 나는 가벼운 진동. */
    TICK,
}

/**
 * 진동 한 번의 모양. [timings]는 구간별 길이(ms), [amplitudes]는 같은 자리 구간의 세기(0~255)다.
 * 첫 구간은 세기 0인 대기 시간이다. (VibrationEffect.createWaveform 형식)
 */
class HapticPattern(val timings: LongArray, val amplitudes: IntArray) {
    val totalMillis: Long get() = timings.sum()
}

/**
 * 말랑 진동의 모양을 만든다.
 *
 * 안드로이드의 미리 정의된 클릭 효과는 길이가 정해져 있고 약해서 세기를 올려도 손끝 차이가 거의 없었다.
 * 그래서 진동 크기(0~255)와 길이를 직접 정한다. 세기를 올리면 크기와 함께 길이도 조금 늘어나서
 * 100%에서는 확실히 세게 느껴진다.
 */
object KeyHaptics {
    fun pattern(feel: HapticFeel, strength: Float, kind: HapticKind): HapticPattern {
        val s = strength.coerceIn(0.05f, 1f)
        fun ms(base: Int) = (base * (0.7f + 0.6f * s)).roundToLong().coerceAtLeast(4L)
        fun amp(x: Float) = (255 * s * x).roundToInt().coerceIn(1, 255)
        return when (kind) {
            HapticKind.TICK -> HapticPattern(longArrayOf(0, ms(7)), intArrayOf(0, amp(0.6f)))
            HapticKind.LONG_PRESS -> HapticPattern(longArrayOf(0, ms(28)), intArrayOf(0, amp(1f)))
            HapticKind.KEY, HapticKind.BIG_KEY -> {
                val big = kind == HapticKind.BIG_KEY
                when (feel) {
                    // 쿵 진동(THUD)을 못 내는 기기에서 대신 쓰는 모양: 세게 치고 길게 줄어든다.
                    HapticFeel.THUD -> HapticPattern(
                        longArrayOf(0, ms(if (big) 16 else 12), ms(if (big) 26 else 18)),
                        intArrayOf(0, amp(1f), amp(0.4f)),
                    )
                    HapticFeel.CRISP -> HapticPattern(
                        longArrayOf(0, ms(if (big) 18 else 12)),
                        intArrayOf(0, amp(1f)),
                    )
                    HapticFeel.SOFT -> HapticPattern(
                        longArrayOf(0, ms(if (big) 8 else 6), ms(if (big) 14 else 10)),
                        intArrayOf(0, amp(0.35f), amp(0.75f)),
                    )
                    HapticFeel.MECHANICAL -> HapticPattern(
                        // 누를 때 딸깍 → 잠깐 쉬고 → 키가 올라오는 톡
                        longArrayOf(0, ms(if (big) 14 else 10), if (big) 32L else 28L, ms(if (big) 9 else 7)),
                        intArrayOf(0, amp(1f), 0, amp(if (big) 0.5f else 0.4f)),
                    )
                    HapticFeel.THOCK -> HapticPattern(
                        longArrayOf(0, ms(if (big) 12 else 8), ms(if (big) 24 else 16)),
                        intArrayOf(0, amp(1f), amp(0.45f)),
                    )
                }
            }
        }
    }
}

/** VibrationEffect.Composition 의 진동 번호. SDK 버전과 무관하게 컴파일되도록 숫자로 둔다. */
private const val PRIMITIVE_THUD = 2
private const val PRIMITIVE_LOW_TICK = 8

/**
 * [feel] 진동을 울린다. 쿵은 기기가 지원하면 안드로이드의 THUD 진동을 그대로 쓰고,
 * 나머지는 [KeyHaptics.pattern]으로 그린 모양을 울린다.
 */
fun Vibrator.playKeyHaptic(feel: HapticFeel, strength: Float, kind: HapticKind) {
    if (feel == HapticFeel.THUD && (kind == HapticKind.KEY || kind == HapticKind.BIG_KEY)) {
        val s = strength.coerceIn(0.05f, 1f)
        val played = if (kind == HapticKind.BIG_KEY) {
            // 큰 키는 쿵 뒤에 낮은 틱을 하나 더해 더 깊게 들어간 느낌을 준다.
            vibratePrimitives(PRIMITIVE_THUD to s, PRIMITIVE_LOW_TICK to s * 0.7f)
        } else {
            vibratePrimitives(PRIMITIVE_THUD to s)
        }
        if (played) return
    }
    val pattern = KeyHaptics.pattern(feel, strength, kind)
    vibrateWaveform(pattern.timings, pattern.amplitudes)
}
