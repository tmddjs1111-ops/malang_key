package dev.malangkey.ime.keyboard

import dev.malangkey.ime.core.Subtype

/** 넓은 화면 자판을 언제 쓸지. */
enum class WideScreenCondition {
    UNFOLDED_OR_LANDSCAPE,
    UNFOLDED_ONLY,
    LANDSCAPE_ONLY;
}

/**
 * 폴더블을 펼치거나 가로로 돌렸을 때 한글 자판을 다른 것으로 바꿔 쓴다.
 * 예: 접었을 때는 천지인, 펼쳤을 때는 두벌식.
 *
 * 한글 자판은 모두 같은 hangul-unicode 조합기를 쓰므로 글자 자판만 바꾸면 된다.
 * 서브타입 id는 그대로라 언어 전환 순서나 저장된 설정에는 영향이 없다.
 */
object WideScreenLayout {
    private const val CORE_LAYOUTS_EXTENSION_ID = "org.florisboard.layouts"

    /** 서로 바꿔 쓸 수 있는 한글 자판. */
    val KoreanLayoutIds = setOf(
        "korean",
        "korean_cheonjiin_16",
        "korean_sky_vega",
        "korean_naratgul",
    )

    fun isWide(condition: WideScreenCondition, isUnfolded: Boolean, isLandscape: Boolean): Boolean {
        return when (condition) {
            WideScreenCondition.UNFOLDED_OR_LANDSCAPE -> isUnfolded || isLandscape
            WideScreenCondition.UNFOLDED_ONLY -> isUnfolded
            WideScreenCondition.LANDSCAPE_ONLY -> isLandscape
        }
    }

    /**
     * [isWide]일 때 [subtype]이 한글 자판이면 글자 자판을 [wideLayoutId]로 바꾼 서브타입을 돌려준다.
     * [wideLayoutId]가 비어 있거나 한글 자판이 아니면 [subtype]을 그대로 돌려준다.
     */
    fun apply(subtype: Subtype, wideLayoutId: String, isWide: Boolean): Subtype {
        if (!isWide || wideLayoutId !in KoreanLayoutIds) return subtype
        if (subtype.primaryLocale.language != "ko") return subtype
        val characters = subtype.layoutMap.characters
        if (characters.extensionId != CORE_LAYOUTS_EXTENSION_ID || characters.componentId !in KoreanLayoutIds) {
            return subtype
        }
        if (characters.componentId == wideLayoutId) return subtype
        return subtype.copy(layoutMap = subtype.layoutMap.copy(characters = extCoreLayout(wideLayoutId)))
    }
}
