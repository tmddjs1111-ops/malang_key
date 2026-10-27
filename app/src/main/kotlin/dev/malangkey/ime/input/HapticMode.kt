package dev.malangkey.ime.input

/** 진동 방식. */
enum class HapticMode {
    /** 휴대폰의 키보드 진동(HapticFeedbackConstants)을 쓴다. 기기 제조사가 맞춰 둔 느낌이 난다. */
    SYSTEM,
    /** 느낌과 세기를 직접 고른다. */
    CUSTOM;
}

/**
 * 직접 조절 모드의 키 진동 느낌.
 *
 * [primitiveId]는 VibrationEffect.Composition.PRIMITIVE_* 값, [predefinedEffectId]는 프리미티브를
 * 지원하지 않는 기기에서 대신 쓰는 VibrationEffect.EFFECT_* 값이다. SDK 버전과 무관하게
 * 컴파일되도록 숫자로 둔다.
 */
enum class HapticFeel(val primitiveId: Int, val predefinedEffectId: Int) {
    CLICK(1, 0),    // PRIMITIVE_CLICK, EFFECT_CLICK
    TICK(7, 2),     // PRIMITIVE_TICK, EFFECT_TICK
    LOW_TICK(8, 2); // PRIMITIVE_LOW_TICK, EFFECT_TICK
}
