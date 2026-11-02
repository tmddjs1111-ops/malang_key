import XCTest
@testable import MalangCore

/// 입력창을 흉내 낸다. 키보드 확장이 textDocumentProxy 에 하는 일과 같다.
private final class Editor {
    var text = ""
    var now: Int64 = 0
    let layout: KoreanLayout
    lazy var composer = HangulComposer(clock: { [unowned self] in self.now })

    init(_ layout: KoreanLayout) { self.layout = layout }

    /// 키를 누른다. `gap` 은 앞 키와의 시간 간격(밀리초).
    func type(_ keys: String..., gap: Int64 = 1000) {
        for key in keys {
            now += gap
            let action = composer.actions(precedingText: text, insert: key, layout: layout)
            text.removeLast(action.delete)
            text += action.insert
        }
    }

    func backspace() {
        if let action = composer.actionsForBackspace(precedingText: text) {
            text.removeLast(action.delete)
            text += action.insert
        } else if !text.isEmpty {
            text.removeLast()
        }
    }

    func space() {
        now += 1000
        if !composer.spacePressed(layout: layout) { text += " " }
    }
}

final class DubeolsikTests: XCTestCase {
    func testSyllables() {
        let e = Editor(.dubeolsik)
        e.type("ㅎ", "ㅏ", "ㄴ", "ㄱ", "ㅡ", "ㄹ")
        XCTAssertEqual(e.text, "한글")
    }

    func testDoubleJongMovesToNextSyllable() {
        let e = Editor(.dubeolsik)
        e.type("ㄷ", "ㅏ", "ㄹ", "ㄱ")
        XCTAssertEqual(e.text, "닭")
        e.type("ㅏ")
        XCTAssertEqual(e.text, "달가")
    }

    func testDoubleVowel() {
        let e = Editor(.dubeolsik)
        e.type("ㅇ", "ㅗ", "ㅏ")
        XCTAssertEqual(e.text, "와")
    }

    func testShiftedConsonants() {
        let e = Editor(.dubeolsik)
        e.type("ㄲ", "ㅗ", "ㅊ")
        XCTAssertEqual(e.text, "꽃")
    }

    func testVowelThenConsonantDoesNotJoin() {
        let e = Editor(.dubeolsik)
        e.type("ㅏ", "ㄱ")
        XCTAssertEqual(e.text, "ㅏㄱ")
    }

    func testBackspaceDecomposes() {
        let e = Editor(.dubeolsik)
        e.type("ㄷ", "ㅏ", "ㄹ", "ㄱ")
        e.backspace()
        XCTAssertEqual(e.text, "달")
        e.backspace()
        XCTAssertEqual(e.text, "다")
        e.backspace()
        XCTAssertEqual(e.text, "ㄷ")
        e.backspace()
        XCTAssertEqual(e.text, "")
    }

    func testBackspaceSplitsDoubleVowel() {
        let e = Editor(.dubeolsik)
        e.type("ㄱ", "ㅜ", "ㅓ")
        XCTAssertEqual(e.text, "궈")
        e.backspace()
        XCTAssertEqual(e.text, "구")
    }

    func testNonHangulPassesThrough() {
        let e = Editor(.dubeolsik)
        e.type("ㄱ", "ㅏ", "!")
        XCTAssertEqual(e.text, "가!")
    }
}

final class CheonjiinTests: XCTestCase {
    func testFastRepeatCycles() {
        let e = Editor(.cheonjiin)
        e.type("ㄱ")
        e.type("ㄱ", gap: 100)
        XCTAssertEqual(e.text, "ㅋ")
        e.type("ㄱ", gap: 100)
        XCTAssertEqual(e.text, "ㄲ")
    }

    func testSlowRepeatStartsNewLetter() {
        let e = Editor(.cheonjiin)
        e.type("ㄱ", "ㄱ")
        XCTAssertEqual(e.text, "ㄱㄱ")
    }

    func testHan() {
        let e = Editor(.cheonjiin)
        e.type("ㅅ")
        e.type("ㅅ", gap: 100)
        e.type("ㅣ", "·", "ㄴ")
        XCTAssertEqual(e.text, "한")
    }

    func testDotVowels() {
        let e = Editor(.cheonjiin)
        e.type("ㄱ", "·", "ㅣ")
        XCTAssertEqual(e.text, "거")
        e.type("ㄴ", "·", "ㅡ")
        XCTAssertEqual(e.text, "거노")
    }

    func testVowelAloneGetsIeung() {
        let e = Editor(.cheonjiin)
        e.type("ㅣ")
        XCTAssertEqual(e.text, "이")
    }

    func testSpaceEndsCycle() {
        let e = Editor(.cheonjiin)
        e.type("ㄱ")
        e.space()
        XCTAssertEqual(e.text, "ㄱ")
        e.type("ㄱ", gap: 100)
        XCTAssertEqual(e.text, "ㄱㄱ")
        e.space()
        e.space()
        XCTAssertEqual(e.text, "ㄱㄱ ")
    }

    func testRepeatMergesIntoPreviousJong() {
        // "가" 다음 ㄴ 을 두 번 빨리 누르면 ㄹ 이 앞 글자 받침으로 붙어 "갈"
        let e = Editor(.cheonjiin)
        e.type("ㄱ", "ㅣ", "·")
        XCTAssertEqual(e.text, "가")
        e.type("ㄴ")
        XCTAssertEqual(e.text, "간")
        e.type("ㄴ", gap: 100)
        XCTAssertEqual(e.text, "갈")
    }
}

final class LayoutTests: XCTestCase {
    func testEveryLayoutHasFourRowsAndBackspace() {
        for language in [KeyboardLanguage.korean, .english] {
            for korean in KoreanLayout.available {
                for mode in [KeyboardMode.letters, .symbols, .moreSymbols] {
                    let rows = KeyboardLayouts.rows(language: language, korean: korean, mode: mode, showsGlobe: true)
                    XCTAssertEqual(rows.count, 4)
                    XCTAssertTrue(rows.joined().contains { $0.action == .backspace })
                    XCTAssertTrue(rows.joined().contains { $0.action == .nextKeyboard })
                }
            }
        }
    }

    func testLongPressDigits() {
        let dubeolsik = KeyboardLayouts.dubeolsik(showsGlobe: false)[0].compactMap(\.longPress)
        XCTAssertEqual(dubeolsik.joined(), "1234567890")
        let cheonjiin = KeyboardLayouts.cheonjiin(showsGlobe: false).joined()
            .filter { $0.style == .letter && $0.label.count > 0 && $0.label != "." && $0.label != "," }
            .compactMap(\.longPress)
        XCTAssertEqual(cheonjiin.sorted().joined(), "0123456789")
    }

    func testShiftedLabel() {
        let first = KeyboardLayouts.dubeolsik(showsGlobe: false)[0][0]
        XCTAssertEqual(first.action(shifted: true), .text("ㅃ"))
        XCTAssertEqual(first.action(shifted: false), .text("ㅂ"))
    }
}
