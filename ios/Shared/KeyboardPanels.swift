import CoreText
import MalangCore
import SwiftUI

/// 이모지 목록. 처음 열 때 한 번만 읽는다.
enum EmojiStore {
    static let shared: EmojiData = {
        guard let url = Bundle.main.url(forResource: "ko", withExtension: "txt"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return EmojiData(text: "") }
        let font = CTFontCreateWithName("AppleColorEmoji" as CFString, 20, nil)
        // 이 기기의 글꼴에 없는(더 새로운) 이모지는 빈 네모로 나오니 뺀다.
        return EmojiData(text: text) { emoji in
            let utf16 = Array(emoji.utf16)
            var glyphs = [CGGlyph](repeating: 0, count: utf16.count)
            return CTFontGetGlyphsForCharacters(font, utf16, &glyphs, utf16.count)
        }
    }()
}

/// 패널 아래쪽 줄: 글자판으로 돌아가기, 지우기
private struct PanelBottomBar<Middle: View>: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors
    @ViewBuilder var middle: Middle

    var body: some View {
        HStack(spacing: 6) {
            Button { model.panel = .none } label: {
                Text(model.language == .korean ? "가" : "ABC")
                    .font(.system(size: 15, weight: .medium, design: .rounded))
                    .foregroundColor(colors.keyText)
                    .frame(width: 52, height: 36)
                    .background(RoundedRectangle(cornerRadius: 9, style: .continuous).fill(colors.function))
            }
            middle.frame(maxWidth: .infinity)
            Button { model.onAction?(.backspace) } label: {
                Image(systemName: "delete.left")
                    .font(.system(size: 17, weight: .medium))
                    .foregroundColor(colors.keyText)
                    .frame(width: 52, height: 36)
                    .background(RoundedRectangle(cornerRadius: 9, style: .continuous).fill(colors.function))
            }
        }
        .padding(.horizontal, 6)
        .padding(.bottom, 6)
    }
}

struct EmojiPanel: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors
    @State private var categoryID = "recent"

    private var data: EmojiData { EmojiStore.shared }

    private var emojis: [String] {
        if categoryID == "recent" { return model.recentEmoji }
        return data.categories.first { $0.id == categoryID }?.emojis.map(\.value) ?? []
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                if emojis.isEmpty {
                    Text("최근 쓴 이모지가 여기에 모여요")
                        .font(.system(size: 13, design: .rounded))
                        .foregroundColor(colors.keyText.opacity(0.5))
                        .padding(.top, 50)
                } else {
                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 8), spacing: 2) {
                        ForEach(emojis, id: \.self) { emoji in
                            Button { pick(emoji) } label: {
                                Text(emoji).font(.system(size: 28)).frame(height: 40)
                            }
                        }
                    }
                    .padding(.horizontal, 4)
                }
            }
            .id(categoryID)

            PanelBottomBar(model: model, colors: colors) {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 2) {
                        tab("recent", "🕘")
                        ForEach(data.categories) { category in tab(category.id, category.icon) }
                    }
                }
            }
        }
        .onAppear {
            if model.recentEmoji.isEmpty { categoryID = data.categories.first?.id ?? "recent" }
        }
    }

    private func tab(_ id: String, _ icon: String) -> some View {
        Button { categoryID = id } label: {
            Text(icon)
                .font(.system(size: 20))
                .frame(width: 34, height: 34)
                .background(Circle().fill(categoryID == id ? colors.keyPressed : Color.clear))
        }
    }

    private func pick(_ emoji: String) {
        model.onAction?(.commit(emoji))
        var recent = model.recentEmoji.filter { $0 != emoji }
        recent.insert(emoji, at: 0)
        model.recentEmoji = Array(recent.prefix(32))
        MalangSettings.recentEmoji = model.recentEmoji
    }
}

struct PhrasesPanel: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 6) {
                    ForEach(model.phrases, id: \.self) { phrase in
                        Button { model.onAction?(.commit(phrase)) } label: {
                            Text(phrase)
                                .font(.system(size: 15, design: .rounded))
                                .foregroundColor(colors.keyText)
                                .lineLimit(2)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 10)
                                .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(colors.key))
                        }
                    }
                }
                .padding(8)
            }
            PanelBottomBar(model: model, colors: colors) {
                Text("상용구는 말랑키 앱에서 고칠 수 있어요")
                    .font(.system(size: 11, design: .rounded))
                    .foregroundColor(colors.keyText.opacity(0.55))
            }
        }
    }
}

struct ThemePanel: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 8) {
                    swatch(id: KeyboardTheme.automaticID, title: "자동", theme: nil)
                    ForEach(KeyboardTheme.all) { theme in swatch(id: theme.id, title: theme.title, theme: theme) }
                }
                .padding(8)
            }
            PanelBottomBar(model: model, colors: colors) { EmptyView() }
        }
    }

    private func swatch(id: String, title: String, theme: KeyboardTheme?) -> some View {
        Button {
            model.themeID = id
            MalangSettings.themeID = id
        } label: {
            VStack(spacing: 4) {
                ThemeSwatch(theme: theme)
                    .frame(height: 44)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .stroke(model.themeID == id ? colors.action : Color.clear, lineWidth: 3)
                    )
                Text(title)
                    .font(.system(size: 11, design: .rounded))
                    .foregroundColor(colors.keyText)
                    .lineLimit(1)
            }
        }
    }
}

/// 테마 미리보기 조각. theme 이 nil 이면 '자동'(바닐라/블랙 반반).
struct ThemeSwatch: View {
    let theme: KeyboardTheme?

    var body: some View {
        if let theme {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color(hex: theme.background))
                HStack(spacing: 3) {
                    RoundedRectangle(cornerRadius: 4).fill(Color(hex: theme.key))
                    RoundedRectangle(cornerRadius: 4).fill(Color(hex: theme.key))
                    RoundedRectangle(cornerRadius: 4).fill(Color(hex: theme.action))
                }
                .padding(7)
            }
        } else {
            HStack(spacing: 0) {
                ThemeSwatch(theme: .vanilla)
                ThemeSwatch(theme: .black)
            }
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }
}

struct SettingsPanel: View {
    @ObservedObject var model: KeyboardModel
    let colors: ThemeColors

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    label("한글 자판")
                    chips(KoreanLayout.available, selected: model.koreanLayout, title: \.title) { layout in
                        model.koreanLayout = layout
                        MalangSettings.koreanLayout = layout
                    }
                    label("키보드 높이")
                    chips(KeyboardHeight.allCases, selected: model.height, title: \.title) { height in
                        model.height = height
                        MalangSettings.height = height
                    }
                    label("숫자 힌트 (길게 누르기)")
                    chips([true, false], selected: model.numberHints, title: { $0 ? "보이기" : "숨기기" }) { on in
                        model.numberHints = on
                        MalangSettings.numberHints = on
                    }
                }
                .padding(10)
            }
            PanelBottomBar(model: model, colors: colors) { EmptyView() }
        }
    }

    private func label(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold, design: .rounded))
            .foregroundColor(colors.keyText.opacity(0.6))
    }

    private func chips<T: Hashable>(_ items: [T], selected: T, title: @escaping (T) -> String,
                                    onPick: @escaping (T) -> Void) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 6) {
                ForEach(items, id: \.self) { item in
                    Button { onPick(item) } label: {
                        Text(title(item))
                            .font(.system(size: 14, weight: .medium, design: .rounded))
                            .foregroundColor(item == selected ? colors.actionText : colors.keyText)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(Capsule().fill(item == selected ? colors.action : colors.key))
                    }
                }
            }
        }
    }
}
