import Foundation

/// 안드로이드와 같은 CLDR 이모지 목록(`ime/media/emoji/ko.txt`)을 읽는다.
///
/// 파일 형식: `[분류]` 줄 다음에 `이모지;이름;키워드|키워드…` 줄이 이어진다.
/// 탭으로 시작하는 줄은 바로 위 이모지의 피부색 변형이라 건너뛴다.
public struct EmojiData: Sendable {
    public struct Emoji: Hashable, Sendable {
        public let value: String
        public let name: String
        public let keywords: [String]
    }

    public struct Category: Identifiable, Sendable {
        public let id: String
        public let emojis: [Emoji]

        public var title: String {
            switch id {
            case "smileys_emotion": return "표정"
            case "people_body": return "사람"
            case "animals_nature": return "동물"
            case "food_drink": return "음식"
            case "travel_places": return "여행"
            case "activities": return "활동"
            case "objects": return "사물"
            case "symbols": return "기호"
            case "flags": return "깃발"
            default: return id
            }
        }

        /// 분류 탭에 보일 대표 이모지
        public var icon: String {
            switch id {
            case "smileys_emotion": return "😀"
            case "people_body": return "👋"
            case "animals_nature": return "🐶"
            case "food_drink": return "🍎"
            case "travel_places": return "🚗"
            case "activities": return "⚽"
            case "objects": return "💡"
            case "symbols": return "❤️"
            case "flags": return "🏳️"
            default: return emojis.first?.value ?? "?"
            }
        }
    }

    public let categories: [Category]
    private let index: [String: [String]]

    public init(text: String, isSupported: (String) -> Bool = { _ in true }) {
        var categories: [Category] = []
        var currentID: String?
        var current: [Emoji] = []
        var index: [String: [String]] = [:]

        func flush() {
            if let currentID, !current.isEmpty { categories.append(Category(id: currentID, emojis: current)) }
            current = []
        }

        for line in text.split(whereSeparator: \.isNewline) {
            if line.isEmpty || line.hasPrefix("#") || line.hasPrefix("\t") { continue }
            if line.hasPrefix("[") {
                flush()
                currentID = String(line.dropFirst().dropLast())
                continue
            }
            let parts = line.split(separator: ";", maxSplits: 2, omittingEmptySubsequences: false)
            guard parts.count >= 2 else { continue }
            let value = String(parts[0])
            guard isSupported(value) else { continue }
            let keywords = parts.count > 2 ? parts[2].split(separator: "|").map(String.init) : []
            current.append(Emoji(value: value, name: String(parts[1]), keywords: keywords))
            for word in Set(keywords + [String(parts[1])]) {
                index[word, default: []].append(value)
            }
        }
        flush()
        self.categories = categories
        self.index = index
    }

    /// 마지막으로 친 단어에 어울리는 이모지. 키워드가 정확히 같은 것만 고른다. (예: "행복" → 😀 😄 …)
    public func suggestions(for word: String, limit: Int = 12) -> [String] {
        let word = word.trimmingCharacters(in: .whitespaces)
        guard !word.isEmpty, let found = index[word] else { return [] }
        var seen = Set<String>()
        return Array(found.filter { seen.insert($0).inserted }.prefix(limit))
    }

    /// 커서 앞 글자에서 마지막 단어를 뽑는다.
    public static func lastWord(in text: String) -> String {
        String(text.reversed().prefix { !$0.isWhitespace && !$0.isPunctuation }.reversed())
    }
}
