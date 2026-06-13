/*
 * Copyright (C) 2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.input

/**
 * Enum for haptic vibration primitives available in VibrationEffect.Composition.
 * Using integer literals to avoid compile-time issues with different SDK versions.
 */
enum class HapticVibrationPrimitive(val androidId: Int) {
    CLICK(1),       // VibrationEffect.Composition.PRIMITIVE_CLICK
    QUICK_CLICK(2), // VibrationEffect.Composition.PRIMITIVE_QUICK_CLICK
    THUD(3),        // VibrationEffect.Composition.PRIMITIVE_THUD
    SPIN(4),        // VibrationEffect.Composition.PRIMITIVE_SPIN
    QUICK_RISE(5),  // VibrationEffect.Composition.PRIMITIVE_QUICK_RISE
    SLOW_RISE(6),   // VibrationEffect.Composition.PRIMITIVE_SLOW_RISE
    TICK(7),        // VibrationEffect.Composition.PRIMITIVE_TICK
    LOW_TICK(8);    // VibrationEffect.Composition.PRIMITIVE_LOW_TICK
}
