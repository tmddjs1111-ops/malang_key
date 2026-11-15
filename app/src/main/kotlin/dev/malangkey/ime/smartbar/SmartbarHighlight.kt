package dev.malangkey.ime.smartbar

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

/** 반짝이는 테두리 색. 어떤 테마 위에서도 눈에 띄는 주황. */
internal val SmartbarHighlightColor = Color(0xFFFF7A3D)

/**
 * 간단 설정에서 지금 눌러 볼 스마트 바 버튼을 반짝이게 한다. 키보드와 설정 화면은 같은 프로세스라
 * 이 값을 같이 본다. 비어 있으면 아무것도 반짝이지 않는다.
 */
object SmartbarHighlight {
    val keyCodes = MutableStateFlow<Set<Int>>(emptySet())
}
