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
 * [timings]와 [amplitudes]로 그린 진동을 한 번 울린다. (VibrationEffect.createWaveform 형식)
 * 세기 조절이 안 되는 진동기에서는 0이 아닌 세기가 모두 최대로 울린다.
 */
fun Vibrator.vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
    try {
        if (AndroidVersion.ATLEAST_API26_O) {
            this.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            this.vibrate(timings.sum())
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error performing waveform haptic", e)
    }
}
