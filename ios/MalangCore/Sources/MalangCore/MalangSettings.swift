import Foundation

/// 키보드 높이.
public enum KeyboardHeight: String, CaseIterable, Sendable {
    case small, normal, large

    public var title: String {
        switch self {
        case .small: return "낮게"
        case .normal: return "보통"
        case .large: return "높게"
        }
    }

    /// 글자 키 한 줄의 높이(pt)
    public var rowHeight: Double {
        switch self {
        case .small: return 46
        case .normal: return 52
        case .large: return 58
        }
    }
}

/// 앱과 키보드 확장이 함께 보는 설정.
///
/// 앱은 App Group 에 쓴다. 키보드는 "전체 접근 허용"이 꺼져 있으면 App Group 을 못 읽을 수 있어서,
/// 키보드 안에서 바꾼 값은 키보드 자신의 저장소에도 함께 쓰고, 읽을 때는 App Group → 자기 저장소 순으로 본다.
public enum MalangSettings {
    public static let appGroup = "group.dev.malangkey"

    public enum Keys {
        public static let koreanLayout = "koreanLayout"
        public static let theme = "theme"
        public static let height = "keyboardHeight"
        public static let sound = "keySound"
        public static let numberHints = "numberHints"
        public static let phrases = "phrases"
        public static let recentEmoji = "recentEmoji"
        public static let emojiSuggestions = "emojiSuggestions"
    }

    public static var defaults: UserDefaults {
        UserDefaults(suiteName: appGroup) ?? .standard
    }

    private static func object(_ key: String) -> Any? {
        UserDefaults(suiteName: appGroup)?.object(forKey: key) ?? UserDefaults.standard.object(forKey: key)
    }

    private static func set(_ value: Any?, _ key: String) {
        UserDefaults(suiteName: appGroup)?.set(value, forKey: key)
        UserDefaults.standard.set(value, forKey: key)
    }

    public static var koreanLayout: KoreanLayout {
        get { (object(Keys.koreanLayout) as? String).flatMap(KoreanLayout.init(rawValue:)) ?? .dubeolsik }
        set { set(newValue.rawValue, Keys.koreanLayout) }
    }

    public static var themeID: String {
        get { object(Keys.theme) as? String ?? KeyboardTheme.automaticID }
        set { set(newValue, Keys.theme) }
    }

    public static var height: KeyboardHeight {
        get { (object(Keys.height) as? String).flatMap(KeyboardHeight.init(rawValue:)) ?? .normal }
        set { set(newValue.rawValue, Keys.height) }
    }

    public static var sound: Bool {
        get { object(Keys.sound) as? Bool ?? true }
        set { set(newValue, Keys.sound) }
    }

    public static var numberHints: Bool {
        get { object(Keys.numberHints) as? Bool ?? true }
        set { set(newValue, Keys.numberHints) }
    }

    public static var emojiSuggestions: Bool {
        get { object(Keys.emojiSuggestions) as? Bool ?? true }
        set { set(newValue, Keys.emojiSuggestions) }
    }

    public static let defaultPhrases = ["감사합니다 😊", "지금 가는 중이에요!", "잠시만 기다려 주세요", "좋은 하루 보내세요 ☀️"]

    /// 상용구
    public static var phrases: [String] {
        get { object(Keys.phrases) as? [String] ?? defaultPhrases }
        set { set(newValue, Keys.phrases) }
    }

    /// 최근 쓴 이모지 (최신이 앞)
    public static var recentEmoji: [String] {
        get { object(Keys.recentEmoji) as? [String] ?? [] }
        set { set(Array(newValue.prefix(32)), Keys.recentEmoji) }
    }
}
