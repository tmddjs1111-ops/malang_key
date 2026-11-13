import Foundation

/// 키보드 색. 안드로이드 `dev.malangkey.colors` 테마의 색을 그대로 옮겼다. 색은 0xRRGGBB.
public struct KeyboardTheme: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let isDark: Bool
    public let background: UInt32
    /// 글자 키
    public let key: UInt32
    public let keyText: UInt32
    /// 누른 키, 그리고 쉬프트·지우기 같은 기능 키 바탕
    public let keyPressed: UInt32
    /// 줄바꿈 키
    public let action: UInt32
    public let actionText: UInt32
    public let actionPressed: UInt32

    /// 시스템이 밝으면 바닐라, 어두우면 블랙. 테마 목록에는 "자동"으로 보인다.
    public static let automaticID = "auto"

    public static let vanilla = KeyboardTheme(id: "vanilla", title: "말랑키 바닐라", isDark: false, background: 0xFCF5D6,
        key: 0xFFFBEA, keyText: 0x311D18, keyPressed: 0xE8E1C3, action: 0x311D18, actionText: 0xFCF5D6, actionPressed: 0x4A2D25)
    public static let black = KeyboardTheme(id: "black", title: "블랙", isDark: true, background: 0x1C1B1D,
        key: 0x2E2D31, keyText: 0xF4F1EA, keyPressed: 0x45434A, action: 0xF4F1EA, actionText: 0x1C1B1D, actionPressed: 0xD6D2C8)

    public static let all: [KeyboardTheme] = [
        vanilla,
        KeyboardTheme(id: "white", title: "화이트", isDark: false, background: 0xFCF5D6,
            key: 0xFFFFFF, keyText: 0x311D18, keyPressed: 0xE8E1C3, action: 0x5D4037, actionText: 0xFFFFFF, actionPressed: 0x8D6E63),
        black,
        KeyboardTheme(id: "pink", title: "말랑 핑크", isDark: false, background: 0xF6EEEA,
            key: 0xFFFFFF, keyText: 0x3B2F33, keyPressed: 0xF3D6DC, action: 0xF28CA0, actionText: 0xFFFFFF, actionPressed: 0xE0778C),
        KeyboardTheme(id: "red", title: "레드", isDark: false, background: 0xFFEBEE,
            key: 0xFFCDD2, keyText: 0xB71C1C, keyPressed: 0xEF9A9A, action: 0xE53935, actionText: 0xFFFFFF, actionPressed: 0xC62828),
        KeyboardTheme(id: "orange", title: "오렌지", isDark: false, background: 0xFFF3E0,
            key: 0xFFE0B2, keyText: 0xE65100, keyPressed: 0xFFCC80, action: 0xF57C00, actionText: 0xFFFFFF, actionPressed: 0xEF6C00),
        KeyboardTheme(id: "yellow", title: "옐로우", isDark: false, background: 0xFFFDE7,
            key: 0xFFF9C4, keyText: 0xF57F17, keyPressed: 0xFFF59D, action: 0xFBC02D, actionText: 0x000000, actionPressed: 0xF9A825),
        KeyboardTheme(id: "green", title: "그린", isDark: false, background: 0xE8F5E9,
            key: 0xC8E6C9, keyText: 0x1B5E20, keyPressed: 0xA5D6A7, action: 0x43A047, actionText: 0xFFFFFF, actionPressed: 0x2E7D32),
        KeyboardTheme(id: "blue", title: "블루", isDark: false, background: 0xE3F2FD,
            key: 0xBBDEFB, keyText: 0x0D47A1, keyPressed: 0x90CAF9, action: 0x1E88E5, actionText: 0xFFFFFF, actionPressed: 0x1565C0),
        KeyboardTheme(id: "indigo", title: "네이비", isDark: false, background: 0xE8EAF6,
            key: 0xC5CAE9, keyText: 0x1A237E, keyPressed: 0x9FA8DA, action: 0x3949AB, actionText: 0xFFFFFF, actionPressed: 0x283593),
        KeyboardTheme(id: "purple", title: "퍼플", isDark: false, background: 0xF3E5F5,
            key: 0xE1BEE7, keyText: 0x4A148C, keyPressed: 0xCE93D8, action: 0x8E24AA, actionText: 0xFFFFFF, actionPressed: 0x6A1B9A),
    ]

    /// 저장된 테마 이름을 실제 색으로. "auto" 와 모르는 이름은 시스템 밝기를 따른다.
    public static func resolve(_ id: String, systemIsDark: Bool) -> KeyboardTheme {
        if let theme = all.first(where: { $0.id == id }) { return theme }
        return systemIsDark ? black : vanilla
    }
}
