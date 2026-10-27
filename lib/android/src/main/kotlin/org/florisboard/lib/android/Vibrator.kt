/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package org.florisboard.lib.android

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

private const val TAG = "FlorisVibrator"

fun Context.systemVibratorOrNull(): Vibrator? {
    return try {
        val vibrator = if (AndroidVersion.ATLEAST_API31_S) {
            this.systemServiceOrNull(VibratorManager::class)?.defaultVibrator
        } else {
            this.systemServiceOrNull(Vibrator::class)
        }
        vibrator?.takeIf { it.hasVibrator() }.also {
            if (it == null) Log.w(TAG, "No vibrator found on this device")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting system vibrator", e)
        null
    }
}

fun Vibrator.vibrate(duration: Int, strength: Int, factor: Double = 1.0) {
    if (duration <= 0 || strength <= 0) {
        Log.d(TAG, "Skipping vibration: duration=$duration, strength=$strength")
        return
    }
    val effectiveDuration = (duration * factor).toLong().coerceAtLeast(10L)
    
    Log.d(TAG, "Perform vibration: duration=$effectiveDuration, strength=$strength, factor=$factor")
    
    try {
        if (AndroidVersion.ATLEAST_API26_O) {
            val effectiveStrength = when {
                this.hasAmplitudeControl() -> (255.0 * ((strength * factor) / 100.0)).toInt().coerceIn(10, 255)
                else -> VibrationEffect.DEFAULT_AMPLITUDE
            }
            val effect = VibrationEffect.createOneShot(effectiveDuration, effectiveStrength)
            this.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            this.vibrate(effectiveDuration)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error performing vibration", e)
        // Ultimate fallback
        try {
            @Suppress("DEPRECATION")
            this.vibrate(effectiveDuration)
        } catch (e2: Exception) {
            Log.e(TAG, "Ultimate fallback failed", e2)
        }
    }
}

/**
 * 키보드용 짧은 진동. 기기가 지원하는 가장 또렷한 방법부터 쓴다.
 *
 * 1. 진동 프리미티브 (API 30+, 기기 지원 시): 세기([scale])까지 조절된다.
 * 2. 미리 정의된 효과 (API 29+): 제조사가 맞춘 클릭·틱. 세기는 기기 설정을 따른다.
 * 3. 아주 짧은 단발 진동: 오래된 기기용.
 *
 * @param primitiveId VibrationEffect.Composition.PRIMITIVE_* 값.
 * @param scale 세기 (0.0 ~ 1.0).
 * @param predefinedEffectId 프리미티브를 못 쓸 때의 VibrationEffect.EFFECT_* 값.
 */
fun Vibrator.vibrateKeyHaptic(primitiveId: Int, scale: Float, predefinedEffectId: Int) {
    val effectiveScale = scale.coerceIn(0f, 1f)
    if (effectiveScale <= 0f) return
    try {
        if (AndroidVersion.ATLEAST_API30_R && this.areAllPrimitivesSupported(primitiveId)) {
            this.vibrate(VibrationEffect.startComposition().addPrimitive(primitiveId, effectiveScale).compose())
        } else if (AndroidVersion.ATLEAST_API29_Q) {
            this.vibrate(VibrationEffect.createPredefined(predefinedEffectId))
        } else if (AndroidVersion.ATLEAST_API26_O) {
            val amplitude = if (this.hasAmplitudeControl()) {
                (255 * effectiveScale).toInt().coerceIn(1, 255)
            } else {
                VibrationEffect.DEFAULT_AMPLITUDE
            }
            this.vibrate(VibrationEffect.createOneShot(12L, amplitude))
        } else {
            @Suppress("DEPRECATION")
            this.vibrate(12L)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error performing key haptic", e)
    }
}
