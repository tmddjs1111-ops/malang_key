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

    public init(_ label: String, shifted: String? = nil, symbol: String? = nil, action: KeyAction? = nil,
                width: Double = 1, style: Style = .letter) {
        self.label = label
        self.shifted = shifted
        self.symbol = symbol
        self.action = action ?? .text(label)
        self.width = width
        self.style = style
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
        case .cheonjiin: return cheonjiin(showsGlobe: showsGlobe)
        default: return dubeolsik(showsGlobe: showsGlobe)
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

    static func letterRow(_ lower: String, _ upper: String? = nil, width: Double = 1) -> [Key] {
        let upperChars = upper.map { Array($0).map(String.init) }
        return Array(lower).enumerated().map { i, c in
            Key(String(c), shifted: upperChars?[i], width: width)
        }
    }

    public static func dubeolsik(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ", "ㅃㅉㄸㄲㅆㅛㅕㅑㅒㅖ"),
            [Key.spacer(0.5)] + letterRow("ㅁㄴㅇㄹㅎㅗㅓㅏㅣ") + [Key.spacer(0.5)],
            [shiftKey] + letterRow("ㅋㅌㅊㅍㅠㅜㅡ") + [backspaceKey],
            bottomRow(modeKey: Key("123", action: .symbols, width: 1.4, style: .function), showsGlobe: showsGlobe),
        ]
    }

    public static func qwerty(showsGlobe: Bool) -> [[Key]] {
        [
            letterRow("qwertyuiop", "QWERTYUIOP"),
            [Key.spacer(0.5)] + letterRow("asdfghjkl", "ASDFGHJKL") + [Key.spacer(0.5)],
            [shiftKey] + letterRow("zxcvbnm", "ZXCVBNM") + [backspaceKey],
            bottomRow(modeKey: Key("123", action: .symbols, width: 1.4, style: .function), showsGlobe: showsGlobe),
        ]
    }

    /// 천지인. 안드로이드 korean_cheonjiin_16 과 같은 3×4 배치.
    public static func cheonjiin(showsGlobe: Bool) -> [[Key]] {
        func jamo(_ label: String, _ output: String) -> Key { Key(label, action: .text(output)) }
        var bottom: [Key] = [Key("123", action: .symbols, style: .function)]
        if showsGlobe { bottom.append(Key("", symbol: "globe", action: .nextKeyboard, style: .function)) }
        bottom += [
            Key("한/영", action: .toggleLanguage, style: .function),
            jamo("ㅇㅁ", "ㅇ"),
            Key(".", shifted: nil),
            Key("", symbol: "return.left", action: .enter, style: .function),
        ]
        return [
            [jamo("ㅣ", "ㅣ"), jamo("·", "·"), jamo("ㅡ", "ㅡ"), Key("", symbol: "delete.left", action: .backspace, style: .function)],
            [jamo("ㄱㅋ", "ㄱ"), jamo("ㄴㄹ", "ㄴ"), jamo("ㄷㅌ", "ㄷ"), Key("", symbol: "space", action: .space, style: .function)],
            [jamo("ㅂㅍ", "ㅂ"), jamo("ㅅㅎ", "ㅅ"), jamo("ㅈㅊ", "ㅈ"), Key(",")],
            bottom,
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

/// 앱과 키보드 확장이 App Group 으로 함께 보는 설정.
public enum MalangSettings {
    public static let appGroup = "group.dev.malangkey"

    public enum Keys {
        public static let koreanLayout = "koreanLayout"
    }

    public static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroup) ?? .standard
    }

    public static var koreanLayout: KoreanLayout {
        get { defaults.string(forKey: Keys.koreanLayout).flatMap(KoreanLayout.init(rawValue:)) ?? .dubeolsik }
        set { defaults.set(newValue.rawValue, forKey: Keys.koreanLayout) }
    }
}
