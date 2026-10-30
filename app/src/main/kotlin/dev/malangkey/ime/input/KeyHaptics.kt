package dev.malangkey.ime.input

import android.os.Vibrator
import org.florisboard.lib.android.vibratePredefined
import org.florisboard.lib.android.vibrateWaveform
import kotlin.math.roundToInt

/** 말랑 진동의 느낌. */
enum class HapticFeel(val label: String, val description: String) {
    /** 기기 제조사가 다듬어 둔 클릭 진동. 세기 설정은 따르지 않는다. */
    SYSTEM("시스템", "휴대폰 기본 클릭 진동 — 기본 (세기 조절 안 됨)"),
    CRISP("또각", "짧고 또렷하게 한 번"),
    SOFT("톡", "가볍고 부드럽게"),
    THOCK("묵직", "조금 더 꽉 차게, 그래도 짧게");
}

/** 어떤 상황의 진동인지. */
enum class HapticKind {
    KEY,
    /** 길게 누르기. 일반 키보다 세지 않게 살짝만. */
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
 * 안드로이드의 CLICK·TICK 진동은 세기를 줄이면 손끝에 거의 안 와서, 진동 크기와 길이를 직접 그린다.
 * 타건 중에 진동이 겹치지 않도록 길이는 느낌마다 고정이고, 세기는 진동 크기만 바꾼다.
 */
object KeyHaptics {
    /** 길게 누르기는 일반 키의 이 비율 세기로 울린다. */
    const val LONG_PRESS_SCALE = 0.6f
    /** 틱은 일반 키의 이 비율 세기로 울린다. */
    const val TICK_SCALE = 0.5f

    /** 진동 크기 배율 (0~1). 0%에서 0.4, 50%에서 0.7, 100%에서 1. */
    fun amplitudeScale(strength: Float): Float = 0.4f + strength.coerceIn(0f, 1f) * 0.6f

    private fun kindScale(kind: HapticKind) = when (kind) {
        HapticKind.KEY -> 1f
        HapticKind.LONG_PRESS -> LONG_PRESS_SCALE
        HapticKind.TICK -> TICK_SCALE
    }

    fun pattern(feel: HapticFeel, strength: Float, kind: HapticKind): HapticPattern {
        val a = amplitudeScale(strength) * kindScale(kind)
        fun amp(x: Float) = (255 * a * x).roundToInt().coerceIn(1, 255)
        if (kind == HapticKind.TICK) return HapticPattern(longArrayOf(0, 6), intArrayOf(0, amp(1f)))
        return when (feel) {
            HapticFeel.CRISP, HapticFeel.SYSTEM -> HapticPattern(longArrayOf(0, 12), intArrayOf(0, amp(1f)))
            HapticFeel.SOFT -> HapticPattern(longArrayOf(0, 9), intArrayOf(0, amp(0.8f)))
            HapticFeel.THOCK -> HapticPattern(longArrayOf(0, 14), intArrayOf(0, amp(1f)))
        }
    }
}

/** VibrationEffect.EFFECT_* 번호. SDK 버전과 무관하게 컴파일되도록 숫자로 둔다. */
private const val EFFECT_CLICK = 0
private const val EFFECT_TICK = 2

/** [feel] 진동을 울린다. 시스템 느낌은 기기의 클릭 진동을, 나머지는 [KeyHaptics.pattern] 모양을 울린다. */
fun Vibrator.playKeyHaptic(feel: HapticFeel, strength: Float, kind: HapticKind) {
    if (feel == HapticFeel.SYSTEM && vibratePredefined(if (kind == HapticKind.KEY) EFFECT_CLICK else EFFECT_TICK)) return
    val pattern = KeyHaptics.pattern(feel, strength, kind)
    vibrateWaveform(pattern.timings, pattern.amplitudes)
}
