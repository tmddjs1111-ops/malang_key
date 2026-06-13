/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.app.apptheme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.R

// 폰트 패밀리 대규모 등록
val JuaFontFamily = FontFamily(Font(R.font.jua, FontWeight.Normal))
val GmarketSansFontFamily = FontFamily(Font(R.font.gmarket_sans, FontWeight.Normal))
val PretendardFontFamily = FontFamily(Font(R.font.pretendard, FontWeight.Normal))
val NotoSansFontFamily = FontFamily(Font(R.font.noto_sans, FontWeight.Normal))
val NanumGothicFontFamily = FontFamily(Font(R.font.nanum_gothic, FontWeight.Normal))
val NanumMyeongjoFontFamily = FontFamily(Font(R.font.nanum_myeongjo, FontWeight.Normal))
val HandwritingFontFamily = FontFamily(Font(R.font.handwriting, FontWeight.Normal))
val TuntunFontFamily = FontFamily(Font(R.font.tuntun, FontWeight.Normal))
val TmonFontFamily = FontFamily(Font(R.font.tmon, FontWeight.Normal))

fun getTypographyFor(fontFamilyId: String): Typography {
    val fontFamily = when (fontFamilyId) {
        "jua" -> JuaFontFamily
        "gmarket_sans" -> GmarketSansFontFamily
        "pretendard" -> PretendardFontFamily
        "noto_sans" -> NotoSansFontFamily
        "nanum_gothic" -> NanumGothicFontFamily
        "nanum_myeongjo" -> NanumMyeongjoFontFamily
        "handwriting" -> HandwritingFontFamily
        "tuntun" -> TuntunFontFamily
        "tmon" -> TmonFontFamily
        else -> FontFamily.Default
    }

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        )
    )
}

val AppTypography = getTypographyFor("system")
