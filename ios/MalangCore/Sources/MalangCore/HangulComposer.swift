import Foundation

/// 한글 자판 종류. 안드로이드 자판 이름(layoutId)과 맞춘다.
public enum KoreanLayout: String, CaseIterable, Sendable {
    case dubeolsik
    case cheonjiin
    case sky
    case danmoeum
    case naratgul

    /// 지금 iOS 키보드에 배치가 있는 자판.
    public static let available: [KoreanLayout] = [.dubeolsik, .cheonjiin]

    public var title: String {
        switch self {
        case .dubeolsik: return "두벌식"
        case .cheonjiin: return "천지인"
        case .sky: return "스카이"
        case .danmoeum: return "단모음"
        case .naratgul: return "획추가형"
        }
    }
}

/// 안드로이드 `HangulUnicode.kt` 를 옮긴 한글 조합기.
///
/// 커서 앞 글자(`precedingText`)를 보고 "몇 글자를 지우고 무엇을 넣을지"를 돌려준다.
/// iOS 에서는 `deleteBackward()` 를 그 수만큼 부르고 `insertText()` 하면 된다.
public final class HangulComposer {
    /// (지울 글자 수, 넣을 글자)
    public typealias Action = (delete: Int, insert: String)

    static let choseong = ["ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"]
    static let jungseong = ["ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"]
    static let jongseong = ["", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"]

    static let choMap = Dictionary(uniqueKeysWithValues: choseong.enumerated().map { ($1, $0) })
    static let jungMap = Dictionary(uniqueKeysWithValues: jungseong.enumerated().map { ($1, $0) })
    static let jongMap = Dictionary(uniqueKeysWithValues: jongseong.enumerated().filter { !$1.isEmpty }.map { ($1, $0) })

    static let doubleJung = ["ㅗㅏ": "ㅘ", "ㅗㅐ": "ㅙ", "ㅗㅣ": "ㅚ", "ㅜㅓ": "ㅝ", "ㅜㅔ": "ㅞ", "ㅜㅣ": "ㅟ", "ㅡㅣ": "ㅢ"]
    static let doubleJong = ["ㄱㅅ": "ㄳ", "ㄴㅈ": "ㄵ", "ㄴㅎ": "ㄶ", "ㄹㄱ": "ㄺ", "ㄹㅁ": "ㄻ", "ㄹㅂ": "ㄼ", "ㄹㅅ": "ㄽ", "ㄹㅌ": "ㄾ", "ㄹㅍ": "ㄿ", "ㄹㅎ": "ㅀ", "ㅂㅅ": "ㅄ"]

    /// 천지인의 '·' 와 '··' 는 아직 모음이 아니라서 중성 번호 대신 이 값을 쓴다.
    static let dot1 = 100
    static let dot2 = 101

    static let skyCycle: [String: [String]] = [
        "ㄱ": ["ㄱ", "ㅋ", "ㄲ"],
        "ㄴ": ["ㄴ", "ㄹ"],
        "ㄷ": ["ㄷ", "ㅌ", "ㄸ"],
        "ㅂ": ["ㅂ", "ㅍ", "ㅃ"],
        "ㅁ": ["ㅁ", "ㅅ", "ㅆ"],
        "ㅅ": ["ㅅ", "ㅎ", "ㅆ"],
        "ㅈ": ["ㅈ", "ㅊ", "ㅉ"],
        "ㅇ": ["ㅇ", "ㅎ", "ㅁ"],
        "ㅣ": ["ㅣ", "ㅡ", "ㅢ"],
        "ㅏ": ["ㅏ", "ㅑ"],
        "ㅓ": ["ㅓ", "ㅕ"],
        "ㅗ": ["ㅗ", "ㅛ"],
        "ㅜ": ["ㅜ", "ㅠ"],
    ]

    static let cheonjiinCycle: [String: [String]] = [
        "ㄱ": ["ㄱ", "ㅋ", "ㄲ"],
        "ㄴ": ["ㄴ", "ㄹ"],
        "ㄷ": ["ㄷ", "ㅌ", "ㄸ"],
        "ㅂ": ["ㅂ", "ㅍ", "ㅃ"],
        "ㅅ": ["ㅅ", "ㅎ", "ㅆ"],
        "ㅈ": ["ㅈ", "ㅊ", "ㅉ"],
        "ㅇ": ["ㅇ", "ㅁ"],
    ]

    static let danmoeumDouble = [
        "ㄱ": "ㄲ", "ㄷ": "ㄸ", "ㅂ": "ㅃ", "ㅅ": "ㅆ", "ㅈ": "ㅉ",
        "ㅏ": "ㅑ", "ㅓ": "ㅕ", "ㅗ": "ㅛ", "ㅜ": "ㅠ", "ㅐ": "ㅒ", "ㅔ": "ㅖ",
    ]

    static let naratgulStroke = [
        "ㄱ": "ㅋ", "ㅋ": "ㄱ",
        "ㄴ": "ㄷ", "ㄷ": "ㅌ", "ㅌ": "ㄴ",
        "ㅁ": "ㅂ", "ㅂ": "ㅍ", "ㅍ": "ㅁ",
        "ㅅ": "ㅈ", "ㅈ": "ㅊ", "ㅊ": "ㅅ",
        "ㅇ": "ㅎ", "ㅎ": "ㅇ",
        "ㅏ": "ㅑ", "ㅑ": "ㅏ",
        "ㅓ": "ㅕ", "ㅕ": "ㅓ",
        "ㅗ": "ㅛ", "ㅛ": "ㅗ",
        "ㅜ": "ㅠ", "ㅠ": "ㅜ",
    ]

    static let naratgulDouble = [
        "ㄱ": "ㄲ", "ㄲ": "ㄱ",
        "ㄷ": "ㄸ", "ㄸ": "ㄷ",
        "ㅂ": "ㅃ", "ㅃ": "ㅂ",
        "ㅅ": "ㅆ", "ㅆ": "ㅅ",
        "ㅈ": "ㅉ", "ㅉ": "ㅈ",
        "ㅇ": "ㅎ", "ㅎ": "ㅇ",
    ]

    /// 같은 키를 이 시간(밀리초) 안에 다시 누르면 연타로 본다.
    static let cycleWindow: Int64 = 800

    private var lastInputKey = ""
    private var lastInputTime: Int64 = 0
    private var escapedBySpace = false
    private let clock: () -> Int64

    /// - Parameter clock: 밀리초 단위 시계. 시험할 때 바꿔 끼운다.
    public init(clock: @escaping () -> Int64 = { Int64(ProcessInfo.processInfo.systemUptime * 1000) }) {
        self.clock = clock
    }

    /// 연타 상태를 잊는다. 줄바꿈·자판 전환처럼 조합과 상관없는 키를 누를 때 부른다.
    public func resetCycle() {
        lastInputKey = ""
        escapedBySpace = false
    }

    // MARK: - 글자 나누기

    static func code(_ c: Character) -> UInt32? {
        let scalars = c.unicodeScalars
        return scalars.count == 1 ? scalars.first!.value : nil
    }

    static func isSyllable(_ c: Character) -> Bool {
        guard let v = code(c) else { return false }
        return (0xAC00...0xD7A3).contains(v)
    }

    static func isConsonant(_ c: Character) -> Bool {
        guard let v = code(c) else { return false }
        return (0x3131...0x314E).contains(v)
    }

    static func isVowel(_ c: Character) -> Bool {
        guard let v = code(c) else { return false }
        return (0x314F...0x3163).contains(v)
    }

    /// 완성형 글자를 (초성, 중성, 종성) 번호로 나눈다. 종성 0 은 받침 없음.
    static func disassemble(_ c: Character) -> (cho: Int, jung: Int, jong: Int) {
        let base = Int(code(c)!) - 0xAC00
        return (base / (21 * 28), (base % (21 * 28)) / 28, base % 28)
    }

    /// 겹받침을 두 자음으로 나눈다. 겹받침이 아니면 nil.
    static func splitDoubleJong(_ j: String) -> (String, String)? {
        guard let key = doubleJong.first(where: { $0.value == j })?.key else { return nil }
        let parts = Array(key)
        return (String(parts[0]), String(parts[1]))
    }

    /// 겹모음을 두 모음으로 나눈다. 겹모음이 아니면 nil.
    static func splitDoubleJung(_ j: String) -> (String, String)? {
        guard let key = doubleJung.first(where: { $0.value == j })?.key else { return nil }
        let parts = Array(key)
        return (String(parts[0]), String(parts[1]))
    }

    /// 중성 번호를 글자로. 천지인의 점 상태도 다룬다.
    static func jungString(_ j: Int) -> String {
        if j == dot1 { return "·" }
        if j == dot2 { return "··" }
        return jungseong[j]
    }

    // MARK: - 조합 중인 글자

    final class EngineState {
        var cho: Int?
        var jung: Int?
        var jong: Int?
        var outNodes: [String] = []
        var extraDeleteCount = 0
        var precedingText: [Character] = []

        init(cho: Int? = nil, jung: Int? = nil, jong: Int? = nil) {
            self.cho = cho
            self.jung = jung
            self.jong = jong
        }

        func commitPreedit() {
            if cho != nil || jung != nil {
                outNodes.append(toPreedit())
            }
            cho = nil
            jung = nil
            jong = nil
        }

        func toPreedit() -> String {
            switch (cho, jung) {
            case (nil, nil):
                return ""
            case let (c?, nil):
                return HangulComposer.choseong[c]
            case let (nil, j?):
                return HangulComposer.jungString(j)
            case let (c?, j?):
                if j == HangulComposer.dot1 || j == HangulComposer.dot2 {
                    return HangulComposer.choseong[c] + HangulComposer.jungString(j)
                }
                let index = (c * 21 + j) * 28 + (jong ?? 0)
                return String(Character(Unicode.Scalar(UInt32(0xAC00 + index))!))
            }
        }

        /// 받침을 떼어 다음 글자의 초성으로 넘길 자음을 돌려준다. 겹받침이면 뒤 자음만 뗀다.
        func detachJong() -> String {
            let j = HangulComposer.jongseong[jong!]
            if case let (first, second)? = HangulComposer.splitDoubleJong(j) {
                jong = HangulComposer.jongMap[first]
                return second
            }
            jong = nil
            return j
        }
    }

    // MARK: - 입력

    /// 글자 키를 눌렀을 때.
    public func actions(precedingText: String, insert toInsert: String, layout: KoreanLayout) -> Action {
        let now = clock()
        if toInsert.isEmpty { return (0, toInsert) }

        let text = Array(precedingText)
        var cho: Int?
        var jung: Int?
        var jong: Int?
        var deleteCount = 0

        if let last = text.last, lastInputKey != "SPACE" {
            if Self.isSyllable(last) {
                let parts = Self.disassemble(last)
                cho = parts.cho
                jung = parts.jung
                jong = parts.jong > 0 ? parts.jong : nil
                deleteCount = 1
            } else if Self.isConsonant(last) {
                cho = Self.choMap[String(last)]
                if cho == nil {
                    jong = Self.jongMap[String(last)]
                }
                deleteCount = 1
            } else if Self.isVowel(last) {
                jung = Self.jungMap[String(last)]
                deleteCount = 1
            } else if last == "·" {
                deleteCount = 1
                jung = Self.dot1
                if text.count > 1 {
                    let prev = text[text.count - 2]
                    if prev == "·" {
                        deleteCount = 2
                        jung = Self.dot2
                        if text.count > 2, Self.isConsonant(text[text.count - 3]) {
                            cho = Self.choMap[String(text[text.count - 3])]
                            deleteCount = 3
                        }
                    } else if Self.isConsonant(prev) {
                        cho = Self.choMap[String(prev)]
                        deleteCount = 2
                    }
                }
            }
        }

        let state = EngineState(cho: cho, jung: jung, jong: jong)
        state.precedingText = text

        if lastInputKey != toInsert && lastInputKey != "SPACE" {
            escapedBySpace = false
        }

        switch layout {
        case .dubeolsik: handleQwerty(toInsert, state)
        case .cheonjiin: handleCheonjiin(toInsert, state, now)
        case .sky: handleSky(toInsert, state, now)
        case .danmoeum: handleDanmoeum(toInsert, state, now)
        case .naratgul: handleNaratgul(toInsert, state, now)
        }

        lastInputKey = toInsert
        lastInputTime = now

        return (deleteCount + state.extraDeleteCount, state.outNodes.joined() + state.toPreedit())
    }

    /// 지우기 키. 조합 중인 글자를 한 자모씩 푼다. nil 이면 그냥 한 글자 지우면 된다.
    public func actionsForBackspace(precedingText: String) -> Action? {
        lastInputKey = ""
        guard let last = precedingText.last, Self.isSyllable(last) else { return nil }
        let parts = Self.disassemble(last)

        if parts.jong > 0 {
            let j = Self.jongseong[parts.jong]
            let remaining = Self.splitDoubleJong(j).flatMap { Self.jongMap[$0.0] }
            return (1, EngineState(cho: parts.cho, jung: parts.jung, jong: remaining).toPreedit())
        }
        if case let (first, _)? = Self.splitDoubleJung(Self.jungseong[parts.jung]) {
            return (1, EngineState(cho: parts.cho, jung: Self.jungMap[first]).toPreedit())
        }
        return (1, EngineState(cho: parts.cho).toPreedit())
    }

    /// 스페이스. 연타형 자판에서는 첫 스페이스가 글자를 끝내기만 한다.
    /// - Returns: true 면 스페이스를 넣지 않는다.
    public func spacePressed(layout: KoreanLayout) -> Bool {
        guard layout == .sky || layout == .cheonjiin || layout == .naratgul else { return false }
        guard lastInputKey != "SPACE", !lastInputKey.isEmpty else { return false }
        let isHangulKey = Self.choseong.contains(lastInputKey) || Self.jungseong.contains(lastInputKey)
            || lastInputKey == "·" || lastInputKey == "ㆍ"
        guard isHangulKey else { return false }
        lastInputKey = "SPACE"
        escapedBySpace = true
        return true
    }

    // MARK: - 두벌식

    private func handleQwerty(_ key: String, _ s: EngineState) {
        if let keyJung = Self.jungMap[key] {
            if s.cho == nil {
                if let j = s.jung, let d = Self.doubleJung[Self.jungString(j) + key] {
                    s.jung = Self.jungMap[d]
                } else if s.jung == nil {
                    s.jung = keyJung
                } else {
                    s.commitPreedit()
                    s.jung = keyJung
                }
            } else if s.jung == nil {
                s.jung = keyJung
            } else if s.jong == nil {
                if let d = Self.doubleJung[Self.jungString(s.jung!) + key] {
                    s.jung = Self.jungMap[d]
                } else {
                    s.commitPreedit()
                    s.jung = keyJung
                }
            } else {
                let nextCho = s.detachJong()
                s.commitPreedit()
                s.cho = Self.choMap[nextCho] ?? Self.choMap["ㅇ"]
                s.jung = keyJung
            }
        } else if let keyCho = Self.choMap[key] {
            if s.cho == nil && s.jung == nil {
                s.cho = keyCho
            } else if s.jung == nil {
                s.commitPreedit()
                s.cho = keyCho
            } else if s.cho == nil {
                // 모음만 있을 때 자음이 오면 앞 모음을 끝내고 새 글자를 시작한다. (ㅏ+ㄱ ≠ 가)
                s.commitPreedit()
                s.cho = keyCho
            } else if s.jong == nil {
                if let keyJong = Self.jongMap[key] {
                    s.jong = keyJong
                } else {
                    s.commitPreedit()
                    s.cho = keyCho
                }
            } else {
                if let d = Self.doubleJong[Self.jongseong[s.jong!] + key] {
                    s.jong = Self.jongMap[d]
                } else {
                    s.commitPreedit()
                    s.cho = keyCho
                }
            }
        } else {
            s.commitPreedit()
            s.outNodes.append(key)
        }
    }

    // MARK: - 연타형 공통

    /// 연타 중인 자음이 앞 글자의 받침으로 붙을 수 있으면 붙인다. (예: "가"+ㄴ 연타 → "갈")
    private func tryMergeBack(_ s: EngineState, _ nextCho: String) -> Bool {
        if escapedBySpace { return false }

        func merge(_ syllable: Character) -> Bool {
            guard Self.isSyllable(syllable) else { return false }
            let prev = Self.disassemble(syllable)
            let newJong: Int?
            if prev.jong > 0 {
                newJong = Self.doubleJong[Self.jongseong[prev.jong] + nextCho].flatMap { Self.jongMap[$0] }
            } else {
                newJong = Self.jongMap[nextCho]
            }
            guard let newJong else { return false }
            s.cho = prev.cho
            s.jung = prev.jung
            s.jong = newJong
            return true
        }

        if let lastCommitted = s.outNodes.last {
            guard lastCommitted.count == 1, merge(lastCommitted.first!) else { return false }
            s.outNodes.removeLast()
            return true
        }
        let target = s.precedingText.count - 2 - s.extraDeleteCount
        guard target >= 0, merge(s.precedingText[target]) else { return false }
        s.extraDeleteCount += 1
        return true
    }

    private func cycleJongseong(_ s: EngineState, _ cycle: [String], isVowel: Bool) {
        let current = Self.jongseong[s.jong!]
        if let idx = cycle.firstIndex(of: current) {
            let next = cycle[(idx + 1) % cycle.count]
            if let nextJong = Self.jongMap[next] {
                s.jong = nextJong
            } else {
                s.jong = nil
                s.commitPreedit()
                s.cho = Self.choMap[next]
            }
            return
        }
        if case let (first, second)? = Self.splitDoubleJong(current), let idx = cycle.firstIndex(of: second) {
            let next = cycle[(idx + 1) % cycle.count]
            if let d = Self.doubleJong[first + next] {
                s.jong = Self.jongMap[d]
            } else {
                s.jong = Self.jongMap[first]
                s.commitPreedit()
                s.cho = Self.choMap[next]
            }
            return
        }
        s.commitPreedit()
        if isVowel { s.jung = Self.jungMap[cycle[0]] } else { s.cho = Self.choMap[cycle[0]] }
    }

    /// 받침 뒤에 같은 계열이 아닌 자음이 오면 겹받침을 만들거나 새 글자를 시작한다.
    private func addConsonant(_ s: EngineState, _ c: String) {
        if s.cho != nil && s.jung != nil && s.jong == nil, let j = Self.jongMap[c] {
            s.jong = j
        } else if s.cho != nil && s.jung != nil && s.jong != nil {
            if let d = Self.doubleJong[Self.jongseong[s.jong!] + c] {
                s.jong = Self.jongMap[d]
            } else {
                s.commitPreedit()
                s.cho = Self.choMap[c]
            }
        } else {
            s.commitPreedit()
            s.cho = Self.choMap[c]
        }
    }

    private func isCycle(_ key: String, _ now: Int64) -> Bool {
        lastInputKey == key && (now - lastInputTime) < Self.cycleWindow
    }

    // MARK: - 천지인

    private func handleCheonjiin(_ key: String, _ s: EngineState, _ now: Int64) {
        if let cycle = Self.cheonjiinCycle[key] {
            guard isCycle(key, now) else {
                addConsonant(s, cycle[0])
                return
            }
            if s.jong != nil {
                cycleJongseong(s, cycle, isVowel: false)
            } else if s.jung != nil && s.cho != nil {
                if let j = Self.jongMap[cycle[0]] {
                    s.jong = j
                } else {
                    s.commitPreedit()
                    s.cho = Self.choMap[cycle[0]]
                }
            } else if let c = s.cho, s.jung == nil {
                if let idx = cycle.firstIndex(of: Self.choseong[c]) {
                    let next = cycle[(idx + 1) % cycle.count]
                    if !tryMergeBack(s, next) {
                        s.cho = Self.choMap[next]
                    }
                } else {
                    s.commitPreedit()
                    s.cho = Self.choMap[cycle[0]]
                }
            } else {
                s.commitPreedit()
                s.cho = Self.choMap[cycle[0]]
            }
        } else if key == "ㅣ" || key == "·" || key == "ㅡ" {
            if s.cho != nil && s.jung != nil && s.jong != nil {
                let nextCho = s.detachJong()
                s.commitPreedit()
                s.cho = Self.choMap[nextCho] ?? Self.choMap["ㅇ"]
            }
            if s.cho == nil { s.cho = Self.choMap["ㅇ"] }

            guard let j = s.jung else {
                switch key {
                case "ㅣ": s.jung = Self.jungMap["ㅣ"]
                case "ㅡ": s.jung = Self.jungMap["ㅡ"]
                default: s.jung = Self.dot1
                }
                return
            }

            let next: String?
            switch (Self.jungString(j), key) {
            case ("ㅣ", "·"): next = "ㅏ"
            case ("ㅏ", "·"): next = "ㅑ"
            case ("ㅏ", "ㅣ"): next = "ㅐ"
            case ("ㅑ", "ㅣ"): next = "ㅒ"
            case ("ㅡ", "·"): next = "ㅜ"
            case ("ㅜ", "·"): next = "ㅠ"
            case ("ㅜ", "ㅣ"): next = "ㅟ"
            case ("ㅟ", "·"): next = "ㅝ"
            case ("ㅠ", "ㅣ"): next = "ㅝ"
            case ("·", "ㅣ"): next = "ㅓ"
            case ("·", "ㅡ"): next = "ㅗ"
            case ("·", "·"): next = "··"
            case ("··", "ㅣ"): next = "ㅕ"
            case ("··", "ㅡ"): next = "ㅛ"
            case ("ㅓ", "·"): next = "ㅕ"
            case ("ㅓ", "ㅣ"): next = "ㅔ"
            case ("ㅕ", "ㅣ"): next = "ㅖ"
            case ("ㅗ", "·"): next = "ㅛ"
            case ("ㅗ", "ㅣ"): next = "ㅚ"
            case ("ㅚ", "·"): next = "ㅘ"
            default: next = nil
            }

            if next == "··" {
                s.jung = Self.dot2
            } else if let next {
                s.jung = Self.jungMap[next]
            } else {
                s.commitPreedit()
                handleCheonjiin(key, s, now)
            }
        } else if key != " " {
            handleQwerty(key, s)
        } else {
            s.commitPreedit()
        }
    }

    // MARK: - 스카이

    private func handleSky(_ key: String, _ s: EngineState, _ now: Int64) {
        let cycling = isCycle(key, now)

        if key == " " && cycling {
            s.outNodes.append(" ")
            return
        }

        guard let cycle = Self.skyCycle[key] else {
            if key == "·" {
                s.commitPreedit()
                s.outNodes.append("·")
            } else if key != " " {
                handleQwerty(key, s)
            } else {
                s.commitPreedit()
            }
            return
        }
        let isVowel = Self.jungMap[cycle[0]] != nil

        if cycling {
            if s.jong != nil {
                cycleJongseong(s, cycle, isVowel: isVowel)
            } else if s.jung != nil && s.cho != nil {
                if isVowel {
                    if let idx = cycle.firstIndex(of: Self.jungString(s.jung!)) {
                        s.jung = Self.jungMap[cycle[(idx + 1) % cycle.count]]
                    } else {
                        s.commitPreedit()
                        s.jung = Self.jungMap[cycle[0]]
                    }
                } else if let j = Self.jongMap[cycle[0]] {
                    s.jong = j
                } else {
                    s.commitPreedit()
                    s.cho = Self.choMap[cycle[0]]
                }
            } else if let c = s.cho, s.jung == nil {
                if isVowel {
                    s.jung = Self.jungMap[cycle[0]]
                } else if let idx = cycle.firstIndex(of: Self.choseong[c]) {
                    let next = cycle[(idx + 1) % cycle.count]
                    if !tryMergeBack(s, next) {
                        s.cho = Self.choMap[next]
                    }
                } else {
                    s.commitPreedit()
                    s.cho = Self.choMap[cycle[0]]
                }
            } else if let j = s.jung, s.cho == nil {
                if isVowel {
                    if let idx = cycle.firstIndex(of: Self.jungString(j)) {
                        s.jung = Self.jungMap[cycle[(idx + 1) % cycle.count]]
                    } else {
                        s.commitPreedit()
                        s.jung = Self.jungMap[cycle[0]]
                    }
                } else {
                    s.commitPreedit()
                    s.cho = Self.choMap[cycle[0]]
                }
            } else {
                if isVowel { s.jung = Self.jungMap[cycle[0]] } else { s.cho = Self.choMap[cycle[0]] }
            }
            return
        }

        guard isVowel else {
            addConsonant(s, cycle[0])
            return
        }
        let input = cycle[0]
        if s.jung == nil {
            s.jung = Self.jungMap[input]
        } else if s.jong == nil {
            let combined: [String: String] = [
                "ㅏㅣ": "ㅐ", "ㅑㅣ": "ㅒ", "ㅓㅣ": "ㅔ", "ㅕㅣ": "ㅖ",
                "ㅗㅏ": "ㅘ", "ㅗㅣ": "ㅚ", "ㅜㅓ": "ㅝ", "ㅜㅣ": "ㅟ",
                "ㅡㅣ": "ㅢ", "ㅘㅣ": "ㅙ", "ㅝㅣ": "ㅞ",
            ]
            if let next = combined[Self.jungString(s.jung!) + input] {
                s.jung = Self.jungMap[next]
            } else {
                s.commitPreedit()
                s.jung = Self.jungMap[input]
            }
        } else {
            let nextCho = s.detachJong()
            s.commitPreedit()
            s.cho = Self.choMap[nextCho] ?? Self.choMap["ㅇ"]
            s.jung = Self.jungMap[input]
        }
    }

    // MARK: - 단모음

    private func handleDanmoeum(_ key: String, _ s: EngineState, _ now: Int64) {
        if isCycle(key, now), let doubled = Self.danmoeumDouble[key] {
            handleQwerty(doubled, s)
        } else {
            handleQwerty(key, s)
        }
    }

    // MARK: - 획추가형(나랏글)

    private func handleNaratgul(_ key: String, _ s: EngineState, _ now: Int64) {
        let cycling = isCycle(key, now)

        func toggleVowel(_ a: String, _ b: String) {
            if cycling && s.jung == Self.jungMap[a] {
                s.jung = Self.jungMap[b]
            } else if cycling && s.jung == Self.jungMap[b] {
                s.jung = Self.jungMap[a]
            } else {
                handleQwerty(a, s)
            }
        }

        func transform(_ table: [String: String], consonantOnlyWithoutVowel: Bool) {
            if let jong = s.jong {
                let current = Self.jongseong[jong]
                if case let (first, second)? = Self.splitDoubleJong(current) {
                    if let t = table[second], let d = Self.doubleJong[first + t] {
                        s.jong = Self.jongMap[d]
                    }
                } else if let t = table[current], let j = Self.jongMap[t] {
                    s.jong = j
                }
            } else if !consonantOnlyWithoutVowel, let j = s.jung {
                if let t = table[Self.jungString(j)] {
                    s.jung = Self.jungMap[t]
                }
            } else if let c = s.cho, s.jung == nil || !consonantOnlyWithoutVowel {
                if let next = table[Self.choseong[c]] {
                    if s.jung != nil || !tryMergeBack(s, next) {
                        s.cho = Self.choMap[next]
                    }
                }
            }
        }

        switch key {
        case "ㅏ/ㅓ": toggleVowel("ㅏ", "ㅓ")
        case "ㅗ/ㅜ": toggleVowel("ㅗ", "ㅜ")
        case "ㅣ/ㅡ": toggleVowel("ㅣ", "ㅡ")
        case "획추가", "획": transform(Self.naratgulStroke, consonantOnlyWithoutVowel: false)
        case "쌍자음", "쌍": transform(Self.naratgulDouble, consonantOnlyWithoutVowel: true)
        case " ": s.commitPreedit()
        default: handleQwerty(key, s)
        }
    }
}
