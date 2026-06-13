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
 * Performs a sharp click haptic feedback using VibrationEffect.Composition if supported.
 * Falls back to a standard vibration if not supported.
 *
 * @param primitiveId The ID of the primitive to use (from [VibrationEffect.Composition]).
 * @param factor The intensity factor (0.0 to 1.0).
 */
fun Vibrator.vibrateClick(primitiveId: Int = -1, factor: Float = 1.0f) {
    val id = if (primitiveId != -1) primitiveId else 1 // 1 is PRIMITIVE_CLICK
    
    Log.d(TAG, "Perform vibrateClick: primitiveId=$id, factor=$factor")
    
    if (AndroidVersion.ATLEAST_API30_R && this.areAllPrimitivesSupported(id)) {
        try {
            val composition = VibrationEffect.startComposition()
            composition.addPrimitive(id, factor.coerceIn(0.0f, 1.0f))
            this.vibrate(composition.compose())
            Log.d(TAG, "VibrationEffect.Composition used successfully")
            return
        } catch (e: Exception) {
            Log.e(TAG, "Composition failed, falling back to legacy", e)
        }
    }
    
    // Fallback to legacy vibration
    vibrate(duration = 30, strength = 80, factor = factor.toDouble())
}
