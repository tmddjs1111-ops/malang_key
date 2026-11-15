package dev.malangkey.ime.keyboard

import dev.malangkey.ime.core.Subtype
import dev.malangkey.ime.core.SubtypeLayoutMap
import dev.malangkey.lib.FlorisLocale
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WideScreenLayoutTest : FunSpec({
    val cheonjiin = Subtype.DEFAULT.copy(
        id = 7,
        layoutMap = SubtypeLayoutMap(characters = extCoreLayout("korean_cheonjiin_16")),
    )

    test("넓은 화면이면 한글 자판을 바꾸고 id는 그대로 둔다") {
        val wide = WideScreenLayout.apply(cheonjiin, "korean", isWide = true)
        wide.layoutMap.characters shouldBe extCoreLayout("korean")
        wide.id shouldBe 7
        wide.composer shouldBe cheonjiin.composer
    }

    test("좁은 화면이거나 설정이 비어 있으면 그대로") {
        WideScreenLayout.apply(cheonjiin, "korean", isWide = false) shouldBe cheonjiin
        WideScreenLayout.apply(cheonjiin, "", isWide = true) shouldBe cheonjiin
    }

    test("한글이 아닌 자판은 바꾸지 않는다") {
        val english = Subtype.DEFAULT.copy(
            primaryLocale = FlorisLocale.from("en", "US"),
            layoutMap = SubtypeLayoutMap(characters = extCoreLayout("qwerty")),
        )
        WideScreenLayout.apply(english, "korean", isWide = true) shouldBe english
    }

    test("조건별 넓은 화면 판단") {
        WideScreenLayout.isWide(WideScreenCondition.UNFOLDED_OR_LANDSCAPE, isUnfolded = false, isLandscape = true) shouldBe true
        WideScreenLayout.isWide(WideScreenCondition.UNFOLDED_ONLY, isUnfolded = false, isLandscape = true) shouldBe false
        WideScreenLayout.isWide(WideScreenCondition.LANDSCAPE_ONLY, isUnfolded = true, isLandscape = false) shouldBe false
        WideScreenLayout.isWide(WideScreenCondition.UNFOLDED_ONLY, isUnfolded = true, isLandscape = false) shouldBe true
    }
})
