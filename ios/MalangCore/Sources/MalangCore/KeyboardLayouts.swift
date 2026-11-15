import Foundation

/// 키를 눌렀을 때 하는 일.
public enum KeyAction: Hashable, Sendable {
    case text(String)
    case backspace
    case space
    case enter
    case shift
    /// 시스템의 다음 키보드로 (지구본 키)
    case nextKeyboard
    /// 한/영 전환
    case toggleLanguage
    case symbols
    case moreSymbols
    case letters
    /// 조합하지 않고 그대로 넣는다 (이모지, 상용구, 추천)
    case commit(String)
    /// 키보드 내리기
    case dismiss
    /// 줄 맞춤용 빈 칸
    case noop
}

public struct Key: Hashable, Sendable {
    public enum Style: Hashable, Sendable { case letter, function, spacer }

    public var label: String
    /// 쉬프트를 눌렀을 때 보이는 글자와 넣는 글자. nil 이면 바뀌지 않는다.
    public var shifted: String?
    /// SF Symbol 이름. 있으면 글자 대신 그린다.
    public var symbol: String?
    public var action: KeyAction
    /// 한 줄 안에서 차지하는 비율.
    public var width: Double
    public var style: Style
    /// 길게 누르면 넣는 글자. 키 오른쪽 위에 작게 보인다.
    public var longPress: String?

    public init(_ label: String, shifted: String? = nil, symbol: String? = nil, action: KeyAction? = nil,
                width: Double = 1, style: Style = .letter, longPress: String? = nil) {
        self.label = label
        self.shifted = shifted
        self.symbol = symbol
        self.action = action ?? .text(label)
        self.width = width
        self.style = style
        self.longPress = longPress
    }

    public func action(shifted isShifted: Bool) -> KeyAction {
        if isShifted, let shifted, case .text = action { return .text(shifted) }
        return action
    }

    public func label(shifted isShifted: Bool) -> String {
        isShifted ? (shifted ?? label) : label
    }

    static func spacer(_ width: Double) -> Key { Key("", action: KeyAction.noop, width: width, style: .spacer) }
}

public enum KeyboardLanguage: String, Sendable { case korean, english }
public enum KeyboardMode: Sendable { case letters, symbols, moreSymbols }

public enum KeyboardLayouts {
    /// 지금 상태에 맞는 키 배치.
    /// - Parameter showsGlobe: 시스템이 지구본 키를 요구할 때(홈 버튼 없는 기기는 보통 필요 없다)
    public static func rows(language: KeyboardLanguage, korean: KoreanLayout, mode: KeyboardMode,
                            showsGlobe: Bool) -> [[Key]] {
        switch mode {
        case .symbols: return symbols(showsGlobe: showsGlobe)
        case .moreSymbols: return moreSymbols(showsGlobe: showsGlobe)
        case .letters: break
        }
        if language == .english { return qwerty(showsGlobe: showsGlobe) }
        switch korean {
        case .cheonjiin, .sky, .naratgul: return twelveKey(korean, showsGlobe: showsGlobe)
        case .danmoeum: return danmoeum(showsGlobe: showsGlobe)
        case .dubeolsik: return dubeolsik(showsGlobe: showsGlobe)
        }
    }

    static let shiftKey = Key("", symbol: "shift", action: .shift, width: 1.4, style: .function)
    static let backspaceKey = Key("", symbol: "delete.left", action: .backspace, width: 1.4, style: .function)

    static func bottomRow(modeKey: Key, showsGlobe: Bool, languageKey: Bool = true) -> [Key] {
        var row = [modeKey]
        if showsGlobe { row.append(Key("", symbol: "globe", action: .nextKeyboard, width: 1.2, style: .function)) }
        if languageKey { row.append(Key("한/영", action: .toggleLanguage, width: 1.2, style: .function)) }
        row.append(Key("스페이스", action: .space, width: 4.6))
        row.append(Key("", symbol: "return.left", action: .enter, width: 2, style: .function))
        return row
    }

    static func letterRow(_ lower: String, _ upper: String? = nil, longPress: String? = nil, width: Double = 1) -> [Key] {
        let upperChars = upper.map { Array($0).map(String.init) }
        let longChars = longPress.map { Array($0).map(String.init) }
        return Array(lower).enumerated().map { i, c in
            Key(String(c), shifted: upperChars?[i], width: width, longPress: longChars?[i])
        }
    }

    public static func dubeolsik(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ", longPress: "1234567890"),
            [Key.spacer(0.5)] + letterRow("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ", longPress: "~@#₩%^&*(") + [Key.spacer(0.5)],
            [shiftKey] + letterRow("ㅋㅌㅊㅍㅠㅜㅡ", longPress: "-_\"'?!/") + [backspaceKey],
            bottomRow(modeKey: Key("123", action: .symbols, width: 1.4, style: .function), showsGlobe: showsGlobe),
        ]
    }

    public static func qwerty(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("qwertyuiop", "QWERTYUIOP", longPress: "1234567890"),
            [Key.spacer(0.5)] + letterRow("asdfghjkl", "ASDFGHJKL", longPress: "~@#$%^&*(") + [Key.spacer(0.5)],
            [shiftKey] + letterRow("zxcvbnm", "ZXCVBNM", longPress: "-_\"'?!/") + [backspaceKey],
            bottomRow(modeKey: Key("123", action: .symbols, width: 1.4, style: .function), showsGlobe: showsGlobe),
        ]
    }

    /// 연타형 자판(천지인·스카이·획추가형)의 가운데 3×4 칸. (보이는 글자, 넣는 글자, 길게 눌러 넣는 글자)
    typealias GridKey = (label: String, output: String, longPress: String?)

    static func grid(_ korean: KoreanLayout) -> [[GridKey]] {
        switch korean {
        case .sky:
            return [
                [("ㄱㅋ", "ㄱ", "1"), ("ㅣㅡ", "ㅣ", "2"), ("ㅏㅑ", "ㅏ", "3")],
                [("ㄷㅌ", "ㄷ", "4"), ("ㄴㄹ", "ㄴ", "5"), ("ㅓㅕ", "ㅓ", "6")],
                [("ㅁㅅ", "ㅁ", "7"), ("ㅂㅍ", "ㅂ", "8"), ("ㅗㅛ", "ㅗ", "9")],
                [("ㅈㅊ", "ㅈ", "*"), ("ㅇㅎ", "ㅇ", "0"), ("ㅜㅠ", "ㅜ", "#")],
            ]
        case .naratgul:
            return [
                [("ㄱ", "ㄱ", "1"), ("ㄴ", "ㄴ", "2"), ("ㅏㅓ", "ㅏ/ㅓ", "3")],
                [("ㄹ", "ㄹ", "4"), ("ㅁ", "ㅁ", "5"), ("ㅗㅜ", "ㅗ/ㅜ", "6")],
                [("ㅅ", "ㅅ", "7"), ("ㅇ", "ㅇ", "8"), ("ㅣㅡ", "ㅣ/ㅡ", "9")],
                [("획", "획", "*"), ("ㅈ", "ㅈ", "0"), ("쌍", "쌍", "#")],
            ]
        default:
            // 천지인. 안드로이드 korean_cheonjiin_16 과 같은 자리, 같은 숫자
            return [
                [("ㅣ", "ㅣ", "1"), ("·", "·", "2"), ("ㅡ", "ㅡ", "3")],
                [("ㄱㅋ", "ㄱ", "4"), ("ㄴㄹ", "ㄴ", "5"), ("ㄷㅌ", "ㄷ", "6")],
                [("ㅂㅍ", "ㅂ", "7"), ("ㅅㅎ", "ㅅ", "8"), ("ㅈㅊ", "ㅈ", "9")],
                [("?", "?", "!"), ("ㅇㅁ", "ㅇ", "0"), ("~", "~", "^")],
            ]
        }
    }

    /// 천지인·스카이·획추가형. 가운데 3×4 자모 칸, 왼쪽에 전환 키, 오른쪽에 편집 키.
    public static func twelveKey(_ korean: KoreanLayout, showsGlobe: Bool) -> [[Key]] {
        let side = 0.9
        let left: [Key] = [
            Key("한/영", action: .toggleLanguage, width: side, style: .function),
            Key("123", action: .symbols, width: side, style: .function),
            Key(",", width: side, style: .function, longPress: ";"),
            showsGlobe
                ? Key("", symbol: "globe", action: .nextKeyboard, width: side, style: .function)
                : Key(".", width: side, style: .function, longPress: ":"),
        ]
        let right: [Key] = [
            Key("", symbol: "delete.left", action: .backspace, width: side, style: .function),
            Key("", symbol: "space", action: .space, width: side, style: .function),
            Key("", symbol: "return.left", action: .enter, width: side, style: .function),
            showsGlobe
                ? Key(".", width: side, style: .function, longPress: ":")
                : Key("'", width: side, style: .function, longPress: "\""),
        ]
        return grid(korean).enumerated().map { i, row in
            [left[i]] + row.map { Key($0.label, action: .text($0.output), width: 1.15, longPress: $0.longPress) } + [right[i]]
        }
    }

    /// 단모음. 두벌식에서 쌍자음·겹모음 자리를 빼고, 같은 키를 빨리 두 번 눌러 만든다.
    public static func danmoeum(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("ㅂㅈㄷㄱㅅㅗㅐㅔ", "ㅃㅉㄸㄲㅆㅗㅒㅖ", longPress: "12345678"),
            letterRow("ㅁㄴㅇㄹㅎㅓㅏㅣ", longPress: "90~@#₩%^"),
            [shiftKey] + letterRow("ㅋㅌㅊㅍㅜㅡ", longPress: "-_\"'?!") + [backspaceKey],
            bottomRow(modeKey: Key("123", action: .symbols, width: 1.4, style: .function), showsGlobe: showsGlobe),
        ]
    }

    public static func symbols(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("1234567890"),
            letterRow("-/:;()₩&@\""),
            [Key("#+=", action: .moreSymbols, width: 1.4, style: .function)] + letterRow(".,?!'", width: 1.4) + [backspaceKey],
            bottomRow(modeKey: Key("가", action: .letters, width: 1.4, style: .function), showsGlobe: showsGlobe, languageKey: false),
        ]
    }

    public static func moreSymbols(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("[]{}#%^*+="),
            letterRow("_\\|~<>$€£•"),
            [Key("123", action: .symbols, width: 1.4, style: .function)] + letterRow(".,?!'", width: 1.4) + [backspaceKey],
            bottomRow(modeKey: Key("가", action: .letters, width: 1.4, style: .function), showsGlobe: showsGlobe, languageKey: false),
        ]
    }
}
