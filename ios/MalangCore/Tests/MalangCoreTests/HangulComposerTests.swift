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
    func testEveryLayoutHasFourRowsBackspaceAndGlobe() {
        for language in [KeyboardLanguage.korean, .english] {
            for korean in KoreanLayout.available {
                for mode in [KeyboardMode.letters, .symbols, .moreSymbols] {
                    let rows = KeyboardLayouts.rows(language: language, korean: korean, mode: mode, showsGlobe: true)
                    XCTAssertEqual(rows.count, 4, "\(korean) \(mode)")
                    XCTAssertTrue(rows.joined().contains { $0.action == .backspace }, "\(korean) \(mode)")
                    XCTAssertTrue(rows.joined().contains { $0.action == .nextKeyboard }, "\(korean) \(mode)")
                    XCTAssertTrue(rows.joined().contains { $0.action == .enter }, "\(korean) \(mode)")
                }
            }
        }
    }

    func testLongPressDigits() {
        let dubeolsik = KeyboardLayouts.dubeolsik(showsGlobe: false)[0].compactMap(\.longPress)
        XCTAssertEqual(dubeolsik.joined(), "1234567890")
        for korean in [KoreanLayout.cheonjiin, .sky, .naratgul] {
            let digits = KeyboardLayouts.grid(korean).joined().compactMap { $0.longPress }.filter { $0.first!.isNumber }
            XCTAssertEqual(digits.sorted().joined(), "0123456789", "\(korean)")
        }
    }

    func testTwelveKeyOutputsAreKnownToComposer() {
        // 연타형 자판의 키가 넣는 글자는 모두 조합기가 아는 글자여야 한다.
        let e = Editor(.sky)
        e.type("ㄱ", "ㅏ", "ㅇ")
        XCTAssertEqual(e.text, "강")
        let n = Editor(.naratgul)
        n.type("ㄱ", "ㅏ/ㅓ", "ㄴ", "획")
        XCTAssertEqual(n.text, "갇")
    }

    func testShiftedLabel() {
        let first = KeyboardLayouts.dubeolsik(showsGlobe: false)[0][0]
        XCTAssertEqual(first.action(shifted: true), .text("ㅃ"))
        XCTAssertEqual(first.action(shifted: false), .text("ㅂ"))
    }
}

final class DanmoeumTests: XCTestCase {
    func testDoublePressMakesDoubleLetters() {
        let e = Editor(.danmoeum)
        e.type("ㄱ")
        e.type("ㄱ", gap: 100)
        e.type("ㅏ")
        e.type("ㅏ", gap: 100)
        XCTAssertEqual(e.text, "꺄")
    }
}

final class ThemeTests: XCTestCase {
    func testResolve() {
        XCTAssertEqual(KeyboardTheme.resolve("blue", systemIsDark: true).id, "blue")
        XCTAssertEqual(KeyboardTheme.resolve("auto", systemIsDark: false).id, "vanilla")
        XCTAssertEqual(KeyboardTheme.resolve("auto", systemIsDark: true).id, "black")
        XCTAssertEqual(Set(KeyboardTheme.all.map(\.id)).count, KeyboardTheme.all.count)
    }
}

final class EmojiTests: XCTestCase {
    /// 안드로이드 앱의 ko.txt 를 그대로 읽는다.
    private func loadKorean() throws -> EmojiData {
        var url = URL(fileURLWithPath: #filePath)
        for _ in 0..<5 { url.deleteLastPathComponent() }   // ios/MalangCore/Tests/MalangCoreTests/파일 → 저장소 뿌리
        url.appendPathComponent("app/src/main/assets/ime/media/emoji/ko.txt")
        return EmojiData(text: try String(contentsOf: url, encoding: .utf8))
    }

    func testCategories() throws {
        let data = try loadKorean()
        XCTAssertEqual(data.categories.first?.id, "smileys_emotion")
        XCTAssertEqual(data.categories.count, 9)
        XCTAssertEqual(data.categories.first?.emojis.first?.value, "😀")
        // 피부색 변형은 빠진다
        XCTAssertFalse(data.categories.flatMap { $0.emojis }.contains { $0.value == "👋🏻" })
    }

    func testSuggestions() throws {
        let data = try loadKorean()
        XCTAssertFalse(data.suggestions(for: "행복").isEmpty)
        XCTAssertTrue(data.suggestions(for: "없는단어").isEmpty)
    }

    func testLastWord() {
        XCTAssertEqual(EmojiData.lastWord(in: "오늘 정말 행복"), "행복")
        XCTAssertEqual(EmojiData.lastWord(in: "사랑해."), "")
        XCTAssertEqual(EmojiData.lastWord(in: ""), "")
    }
}
